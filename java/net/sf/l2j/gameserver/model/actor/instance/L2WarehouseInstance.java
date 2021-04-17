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

import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PackageToList;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseDepositList;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseWithdrawalList;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public final class L2WarehouseInstance extends L2NpcInstance
{
	/**
	 * @param template
	 */
	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isWarehouse()
	{
		return true;
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/warehouse/" + pom + ".htm";
	}

	private void showRetrieveWindow(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
			return;
		}

		if (itemtype != null)
			player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE, itemtype, sortorder));
		else
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));

		if (Config.DEBUG)
			_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showRetrieveWindow; Message: Showing stored items.");
	}

	private void showDepositWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());
		player.tempInventoryDisable();

		if (Config.DEBUG) _log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showDepositWindow; Message: Showing items to deposit.");
			player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
	}

	private void showDepositWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		if (player.getClan() != null)
		{
			if (player.getClan().getLevel() == 0)
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.tempInventoryDisable();
				WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
				player.sendPacket(dl);

				if (Config.DEBUG)
					_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showDepositWindowClan; Message: Showing items to deposit.");
			}
		}
	}

	private void showWithdrawWindowClan(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		if (Config.DEBUG)
			_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showWithdrawWindowClan; Message: Showing stored items.");

		if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
			return;
		}
		else
		{
			if (player.getClan().getLevel() == 0)
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());

				if (itemtype != null)
					player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
				else
					player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
			}
		}
	}

	private void showWithdrawWindowFreight(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		if (Config.DEBUG)
			_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showWithdrawWindowFreight; Message: Showing freighted items.");

		PcFreight freight = player.getFreight();

		if (freight != null)
		{
			if (freight.getSize() > 0)
			{
				if (Config.ALT_GAME_FREIGHTS)
					freight.setActiveLocation(0);
				else
					freight.setActiveLocation(getWorldRegion().hashCode());

				player.setActiveWarehouse(freight);

				if (itemtype != null)
					player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT, itemtype, sortorder));
				else
					player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
			}
			else
				player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
		}
		else if (Config.DEBUG)
			_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showWithdrawWindowFreight; Message: No items freighted.");
	}

	private void showDepositWindowFreight(L2PcInstance player)
	{
		if (player.getAccountChars().isEmpty())
			player.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
		else
		{
			Map<Integer, String> chars = player.getAccountChars();

			if (chars.isEmpty())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			player.sendPacket(new PackageToList(chars));

			if (Config.DEBUG)
				_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showDepositWindowFreight; Message: Showing freight destination characters.");
		}
	}

	private void showDepositWindowFreight(L2PcInstance player, int obj_Id)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		PcFreight freight = player.getDepositedFreight(obj_Id);

		if (Config.ALT_GAME_FREIGHTS)
			freight.setActiveLocation(0);
		else
			freight.setActiveLocation(getWorldRegion().hashCode());

		player.setActiveWarehouse(freight);
		player.tempInventoryDisable();

		if (Config.DEBUG)
			_log.fine("Source: L2WarehouseInstance.java; Player: "+player.getName()+"; Command: showDepositWindowFreight; Message: Showing items to freight.");

		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player.getActiveEnchantItem() != null)
		{
			// Retail (April 17, 2009 - Gracia CT2) - If a player has an enchant window open and attempts to access a warehouse, the enchant window stays up and the warehouse does not open.
			return;
		}

		String param[] = command.split("_");

		if (command.startsWith("WithdrawP"))
		{
			if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE)
			{
				String htmFile = "data/html/mods/WhSortedP.htm";
				String htmContent = HtmCache.getInstance().getHtm(htmFile);

				if (htmContent != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
				else
				{
					_log.warning("Missing htm: " + htmFile + "!");
					player.sendMessage("This NPC's html is missing. Please inform the server admin.");
				}
			}
			else
				showRetrieveWindow(player, null, (byte) 0);
		}
		else if (command.startsWith("WithdrawSortedP"))
		{
			if (param.length > 2)
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			else if (param.length > 1)
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			else
				showRetrieveWindow(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
		}
		else if (command.equals("DepositP"))
			showDepositWindow(player);

		else if (command.startsWith("WithdrawC"))
		{
			if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_CLAN)
			{
				String htmFile = "data/html/mods/WhSortedC.htm";
				String htmContent = HtmCache.getInstance().getHtm(htmFile);

				if (htmContent != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
				else
				{
					_log.warning("Missing htm: " + htmFile + "!");
					player.sendMessage("This NPC's html is missing. Please inform the server admin.");
				}
			}
			else
				showWithdrawWindowClan(player, null, (byte) 0);
		}
		else if (command.startsWith("WithdrawSortedC"))
		{
			if (param.length > 2)
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			else if (param.length > 1)
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			else
				showWithdrawWindowClan(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
		}
		else if (command.equals("DepositC"))
			showDepositWindowClan(player);
		else if (command.startsWith("WithdrawF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT)
				{
					String htmFile = "data/html/mods/WhSortedF.htm";
					String htmContent = HtmCache.getInstance().getHtm(htmFile);

					if (htmContent != null)
					{
						NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
						npcHtmlMessage.setHtml(htmContent);
						npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(npcHtmlMessage);
					}
					else
					{
						_log.warning("Missing htm: " + htmFile + "!");
						player.sendMessage("This NPC's html is missing. Please inform the server admin.");
					}
				}
				else
					showWithdrawWindowFreight(player, null, (byte) 0);
			}
		}
		else if (command.startsWith("WithdrawSortedF"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				if (param.length > 2) 
					showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
				else if (param.length > 1)
					showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
				else
					showWithdrawWindowFreight(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
			}
		}
		else if (command.startsWith("DepositF"))
		{
			if (Config.ALLOW_FREIGHT)
				showDepositWindowFreight(player);
		}
		else if (command.startsWith("FreightChar"))
		{
			if (Config.ALLOW_FREIGHT)
			{
				int startOfId = command.lastIndexOf("_") + 1;
				String id = command.substring(startOfId);
				showDepositWindowFreight(player, Integer.parseInt(id));
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
