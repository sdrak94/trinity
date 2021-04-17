/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.datatables.MerchantPriceConfigTable;
import net.sf.l2j.gameserver.datatables.MerchantPriceConfigTable.MerchantPriceConfig;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShopPreviewList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class ...
 *
 * @version $Revision: 1.10.4.9 $ $Date: 2005/04/11 10:06:08 $
 */
public class L2MerchantInstance extends L2NpcInstance
{
	//private static Logger _log = Logger.getLogger(L2MerchantInstance.class.getName());
	
	protected MerchantPriceConfig _mpc;
	
	/**
	 * @param template
	 */
	public L2MerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0) pom = "" + npcId;
		else pom = npcId + "-" + val;
		
		return "data/html/merchant/" + pom + ".htm";
	}
	
	/**
	 * @return Returns the mpc.
	 */
	public MerchantPriceConfig getMpc()
	{
		return _mpc;
	}
	
	private final void showWearWindow(L2PcInstance player, int val)
	{
		player.tempInventoryDisable();
		
		if (Config.DEBUG) _log.fine("Showing wearlist");
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null)
		{
			ShopPreviewList bl = new ShopPreviewList(list, player.getAdena(), player.getExpertiseIndex());
			player.sendPacket(bl);
		}
		else
		{
			_log.warning("no buylist with id:" + val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	protected final void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;
		
		taxRate = getMpc().getTotalTaxRate();
		
		player.tempInventoryDisable();
		
		if (Config.DEBUG)
		{
			_log.fine("Showing buylist");
		}
		
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		
		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			_log.warning("possible client hacker: "+player.getName()+" attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	protected final void showSellWindow(L2PcInstance player)
	{
		if (Config.DEBUG) _log.fine("Showing selllist");
		
		player.sendPacket(new SellList(player));
		
		if (Config.DEBUG) _log.fine("Showing sell window");
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1) return;
			
			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else if (actualCommand.equalsIgnoreCase("RentPet"))
		{
			if (Config.ALLOW_RENTPET)
			{
				if (st.countTokens() < 1)
				{
					showRentPetWindow(player);
				}
				else
				{
					int val = Integer.parseInt(st.nextToken());
					tryRentPet(player, val);
				}
			}
		}
		else if (actualCommand.equalsIgnoreCase("Wear") && Config.ALLOW_WEAR)
		{
			if (st.countTokens() < 1) return;
			
			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Multisell"))
		{
			if (st.countTokens() < 1) return;
			
			int val = Integer.parseInt(st.nextToken());
			if (player.isGM())
				player.sendMessage("Browsing multisell: " + val);
			L2Multisell.getInstance().separateAndSend(val, player, getNpcId(), false, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if (st.countTokens() < 1) return;
			
			int val = Integer.parseInt(st.nextToken());
			if (player.isGM())
				player.sendMessage("Browsing multisell: " + val);
			L2Multisell.getInstance().separateAndSend(val, player, getNpcId(), true, getCastle().getTaxRate());
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			
			super.onBypassFeedback(player, command);
		}
	}
	
	public final void showRentPetWindow(L2PcInstance player)
	{
		if (!Config.LIST_PET_RENT_NPC.contains(getTemplate().npcId)) return;
		
		insertObjectIdAndShowChatWindow(player,
				"<html><body>Pet Manager:<br>" +
				"You can rent a wyvern or strider for adena.<br>My prices:<br1>" +
				"<table border=0><tr><td>Ride</td></tr>" +
				"<tr><td>Wyvern</td><td>Strider</td></tr>" +
				"<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr>" +
				"<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr>" +
				"<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr>" +
				"<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr>" +
				"</table>" +
				"</body></html>"
		);
	}
	
	public final void tryRentPet(L2PcInstance player, int val)
	{
		if (player == null || player.getPet() != null || player.isMounted() || player.isRentedPet() || player.isTransformed() || player.isCursedWeaponEquipped())
			return;
		if(!player.disarmWeapons()) return;
		
		int petId;
		double price = 1;
		int cost[] = {1800, 7200, 720000, 6480000};
		int ridetime[] = {30, 60, 600, 1800};
		
		if (val > 10)
		{
			petId = 12526;
			val -= 10;
			price /= 2;
		}
		else
		{
			petId = 12621;
		}
		
		if (val < 1 || val > 4) return;
		
		price *= cost[val - 1];
		int time = ridetime[val - 1];
		
		if (!player.reduceAdena("Rent", (long) price, player.getLastFolkNPC(), true)) return;
		
		player.mount(petId, 0, false);
		SetupGauge sg = new SetupGauge(3, time*1000);
		player.sendPacket(sg);
		player.startRentPet(time);
	}
	
	@Override
	public final void onActionShift(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		
		if (player == null) return;
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (player.isGM())
		{
			player.setTarget(this);
			
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final StringBuilder html1 = StringUtil.startAppend(2000,
					"<html><body><table border=0>" +
					"<tr><td>Current Target:</td></tr>" +
					"<tr><td><br></td></tr>" +
					"<tr><td>Object ID: ",
					String.valueOf(getObjectId()),
					"</td></tr>" +
					"<tr><td>Template ID: ",
					String.valueOf(getTemplate().npcId),
					"</td></tr>" +
					"<tr><td><br></td></tr>" +
					"<tr><td>HP: ",
					String.valueOf(getCurrentHp()),
					"</td></tr>" +
					"<tr><td>MP: ",
					String.valueOf(getCurrentMp()),
					"</td></tr>" +
					"<tr><td>Level: ",
					String.valueOf(getLevel()),
					"</td></tr>" +
					"<tr><td><br></td></tr>" +
					"<tr><td>Class: ",
					getClass().getName(),
					"</td></tr>" +
					"<tr><td><br></td></tr>" +
					//changed by terry 2005-02-22 21:45
					"</table><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
					"<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"</table>"
			);
			
			if (player.isGM()) {
				StringUtil.append(html1,
						"<button value=\"View Shop\" action=\"bypass -h admin_showShop ",
						String.valueOf(getTemplate().npcId),
						"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></br>" +
						"<button value=\"Lease next week\" action=\"bypass -h npc_",
						String.valueOf(getObjectId()),
						"_Lease\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
						"<button value=\"Abort current leasing\" action=\"bypass -h npc_",
						String.valueOf(getObjectId()),
						"_Lease next\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
						"<button value=\"Manage items\" action=\"bypass -h npc_",
						String.valueOf(getObjectId()),
						"_Lease manage\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
				);
			}
			
			html1.append("</body></html>");
			
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		{
			if (!canTarget(player))
				return;
			
			// Check if the L2PcInstance is confused
			if (player.isOutOfControl())
				return;
			
			// Aggression target lock effect
			if (player.isLockedTarget() && player.getLockedTarget() != this)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
				return;
			}
			
			// Check if the L2PcInstance already target the L2NpcInstance
			if (this != player.getTarget())
			{
				if (Config.DEBUG)
					_log.fine("new target selected:" + getObjectId());
				
				// Set the target of the L2PcInstance player
				player.setTarget(this);
				
				// Check if the player is attackable (without a forced attack)
				if (isAutoAttackable(player))
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					// The player.getLevel() - getLevel() permit to display the correct color in the select window
					MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
					player.sendPacket(my);
					
					// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				else
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
					player.sendPacket(my);
				}
				
				// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
				player.sendPacket(new ValidateLocation(this));
			}
			else
			{
				player.sendPacket(new ValidateLocation(this));
			}
		}
	}
}