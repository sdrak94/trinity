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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.CrownTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowCropInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowCropSetting;
import net.sf.l2j.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowSeedInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowSeedSetting;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.gameserver.util.Util;

/**
 * Castle Chamberlains implementation used for: - tax rate control - regional
 * manor system control - castle treasure control - ...
 */
public class L2CastleChamberlainInstance extends L2MerchantInstance
{
// private static Logger _log = Logger.getLogger(L2CastleChamberlainInstance.class.getName());

protected static final int COND_ALL_FALSE = 0;
protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
protected static final int COND_OWNER = 2;
@SuppressWarnings("unused")
private int _preDay;
@SuppressWarnings("unused")
private int _preHour;

public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
{
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcId%", String.valueOf(getNpcId()));
	player.sendPacket(html);
}

@Override
public void onAction(L2PcInstance player)
{
	if (!canTarget(player))
		return;
	
	player.setLastFolkNPC(this);
	
	// Check if the L2PcInstance already target the L2NpcInstance
	if (this != player.getTarget())
	{
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		player.sendPacket(my);
		
		// Send a Server->Client packet ValidateLocation to correct the zL2NpcInstance position and heading on the client
		player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		// Calculate the distance between the L2PcInstance and the L2NpcInstance
		if (!canInteract(player))
		{
			// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
		}
		else
		{
			showMessageWindow(player);
		}
	}
	// Send a Server->Client ActionFailed to the L2PcInstance in order to
	// avoid that the client wait another packet
	player.sendPacket(ActionFailed.STATIC_PACKET);
}

@Override
public void onBypassFeedback(L2PcInstance player, String command)
{
	// BypassValidation Exploit plug.
	if (player.getLastFolkNPC().getObjectId() != getObjectId())
		return;
	SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	int condition = validateCondition(player);
	if (condition <= COND_ALL_FALSE)
		return;
	
	if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		return;
	else if (condition == COND_OWNER)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		String val = "";
		if (st.countTokens() >= 1)
		{
			val = st.nextToken();
		}
		
		if (actualCommand.equalsIgnoreCase("banish_foreigner"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS)) return;
			if (siegeBlocksFunction(player)) return;
			getCastle().banishForeigners(); // Move non-clan members off castle area
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/chamberlain-banishafter.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("banish_foreigner_show"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS)) return;
			if (siegeBlocksFunction(player)) return;
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/chamberlain-banishfore.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) == L2Clan.CP_CS_MANAGE_SIEGE)
			{
				getCastle().getSiege().listRegisterClan(player); // List current register clan
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("receive_report"))
		{
			if (player.isClanLeader())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-report.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
				html.replace("%clanname%", clan.getName());
				html.replace("%clanleadername%", clan.getLeaderName());
				html.replace("%castlename%", getCastle().getName());
				
				int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
				switch (currentPeriod)
				{
				case SevenSigns.PERIOD_COMP_RECRUITING:
					html.replace("%ss_event%", "Quest Event Initialization");
					break;
				case SevenSigns.PERIOD_COMPETITION:
					html.replace("%ss_event%", "Competition (Quest Event)");
					break;
				case SevenSigns.PERIOD_COMP_RESULTS:
					html.replace("%ss_event%", "Quest Event Results");
					break;
				case SevenSigns.PERIOD_SEAL_VALIDATION:
					html.replace("%ss_event%", "Seal Validation");
					break;
				}
				int sealOwner1 = SevenSigns.getInstance().getSealOwner(1);
				switch (sealOwner1)
				{
				case SevenSigns.CABAL_NULL:
					html.replace("%ss_avarice%", "Not in Possession");
					break;
				case SevenSigns.CABAL_DAWN:
					html.replace("%ss_avarice%", "Lords of Dawn");
					break;
				case SevenSigns.CABAL_DUSK:
					html.replace("%ss_avarice%", "Revolutionaries of Dusk");
					break;
				}
				int sealOwner2 = SevenSigns.getInstance().getSealOwner(2);
				switch (sealOwner2)
				{
				case SevenSigns.CABAL_NULL:
					html.replace("%ss_gnosis%", "Not in Possession");
					break;
				case SevenSigns.CABAL_DAWN:
					html.replace("%ss_gnosis%", "Lords of Dawn");
					break;
				case SevenSigns.CABAL_DUSK:
					html.replace("%ss_gnosis%", "Revolutionaries of Dusk");
					break;
				}
				int sealOwner3 = SevenSigns.getInstance().getSealOwner(3);
				switch (sealOwner3)
				{
				case SevenSigns.CABAL_NULL:
					html.replace("%ss_strife%", "Not in Possession");
					break;
				case SevenSigns.CABAL_DAWN:
					html.replace("%ss_strife%", "Lords of Dawn");
					break;
				case SevenSigns.CABAL_DUSK:
					html.replace("%ss_strife%", "Revolutionaries of Dusk");
					break;
				}
				player.sendPacket(html);
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("items"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) == L2Clan.CP_CS_USE_FUNCTIONS)
			{
				if (val.isEmpty())
					return;
				player.tempInventoryDisable();
				
				if (Config.DEBUG)
					_log.fine("Showing chamberlain buylist");
				
				showBuyWindow(player,Integer.parseInt(val + "1"));
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("manage_siege_defender"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) == L2Clan.CP_CS_MANAGE_SIEGE)
			{
				getCastle().getSiege().listRegisterClan(player);
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("manage_vault"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
			{
				String filename = "data/html/chamberlain/chamberlain-vault.htm";
				long amount = 0;
				if (val.equalsIgnoreCase("deposit"))
				{
					try
					{
						amount = Long.parseLong(st.nextToken());
					}
					catch (NoSuchElementException e)
					{
					}
					if (amount > 0 && getCastle().getTreasury() + amount < PcInventory.MAX_ADENA)
					{
						if (player.reduceAdena("Castle", amount, this, true))
							getCastle().addToTreasuryNoTax(amount);
						else
							sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
					}
				}
				else if (val.equalsIgnoreCase("withdraw"))
				{
					try
					{
						amount = Long.parseLong(st.nextToken());
					}
					catch (NoSuchElementException e)
					{
					}
					if (amount > 0)
					{
						if (getCastle().getTreasury() < amount)
							filename = "data/html/chamberlain/chamberlain-vault-no.htm";
						else
						{
							if (getCastle().addToTreasuryNoTax((-1) * amount))
								player.addAdena("Castle", amount, this, true);
						}
					}
				}
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				html.replace("%tax_income%", Util.formatAdena(getCastle().getTreasury()));
				html.replace("%withdraw_amount%", Util.formatAdena(amount));
				player.sendPacket(html);
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("manor"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) == L2Clan.CP_CS_MANOR_ADMIN)
			{
				String filename = "";
				if (CastleManorManager.getInstance().isDisabled())
					filename = "data/html/npcdefault.htm";
				else
				{
					int cmd = Integer.parseInt(val);
					switch (cmd)
					{
					case 0:
						filename = "data/html/chamberlain/manor/manor.htm";
						break;
						// TODO: correct in html's to 1
					case 4:
						filename = "data/html/chamberlain/manor/manor_help00"
								+ st.nextToken() + ".htm";
						break;
					default:
						filename = "data/html/chamberlain/chamberlain-no.htm";
						break;
					}
				}
				
				if (filename.length() != 0)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (command.startsWith("manor_menu_select"))
		{// input string format:
			// manor_menu_select?ask=X&state=Y&time=X
			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
				return;
			}
			
			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer str = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(str.nextToken().split("=")[1]);
			int state = Integer.parseInt(str.nextToken().split("=")[1]);
			int time = Integer.parseInt(str.nextToken().split("=")[1]);
			
			int castleId;
			if (state == -1) // info for current manor
				castleId = getCastle().getCastleId();
			else
				// info for requested manor
				castleId = state;
			
			switch (ask)
			{ // Main action
			case 3: // Current seeds (Manor info)
				if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					player.sendPacket(new ExShowSeedInfo(castleId, null));
				else
					player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
				break;
			case 4: // Current crops (Manor info)
				if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					player.sendPacket(new ExShowCropInfo(castleId, null));
				else
					player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
				break;
			case 5: // Basic info (Manor info)
				player.sendPacket(new ExShowManorDefaultInfo());
				break;
			case 7: // Edit seed setup
				if (getCastle().isNextPeriodApproved())
					player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
				else
					player.sendPacket(new ExShowSeedSetting(getCastle().getCastleId()));
				break;
			case 8: // Edit crop setup
				if (getCastle().isNextPeriodApproved())
					player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
				else
					player.sendPacket(new ExShowCropSetting(getCastle().getCastleId()));
				break;
			}
		}
		else if (actualCommand.equalsIgnoreCase("operate_door")) // door
			// control
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
			{
				if (!val.isEmpty())
				{
					boolean open = (Integer.parseInt(val) == 1);
					while (st.hasMoreTokens())
					{
						getCastle().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
					}
					
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					String file = "data/html/chamberlain/doors-close.htm";
					if (open)
						file = "data/html/chamberlain/doors-open.htm";
					html.setFile(file);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/" + getTemplate().npcId	+ "-d.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("tax_set")) // tax rates
			// control
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_TAXES) == L2Clan.CP_CS_TAXES)
			{
				if (!val.isEmpty())
					getCastle().setTaxPercent(player, Integer.parseInt(val));
				
				final String msg = StringUtil.concat(
						"<html><body>",
						getName(),
						":<br>" +
								"Current tax rate: ",
								String.valueOf(getCastle().getTaxPercent()),
								"%<br>" +
										"<table>" +
										"<tr>" +
										"<td>Change tax rate to:</td>" +
										"<td><edit var=\"value\" width=40><br>" +
										"<button value=\"Adjust\" action=\"bypass -h npc_%objectId%_tax_set $value\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
										"</tr>" +
										"</table>" +
										"</center>" +
										"</body></html>"
						);
				sendHtmlMessage(player, msg);
				return;
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/chamberlain-tax.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%tax%", String.valueOf(getCastle().getTaxPercent()));
				player.sendPacket(html);
				return;
			}
		}
		else if (actualCommand.equalsIgnoreCase("manage_functions"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/chamberlain-manage.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("products"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/chamberlain-products.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcId%", String.valueOf(getNpcId()));
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("functions"))
		{
			if (val.equalsIgnoreCase("tele"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				if (getCastle().getFunction(Castle.FUNC_TELEPORT) == null)
					html.setFile("data/html/chamberlain/chamberlain-nac.htm");
				else
					html.setFile("data/html/chamberlain/"+ getNpcId()+ "-t"
							+ getCastle().getFunction(Castle.FUNC_TELEPORT).getLvl()+ ".htm");
				sendHtmlMessage(player, html);
			}
			else if (val.equalsIgnoreCase("support"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				if (getCastle().getFunction(Castle.FUNC_SUPPORT) == null)
					html.setFile("data/html/chamberlain/chamberlain-nac.htm");
				else
				{
					html.setFile("data/html/chamberlain/support"
							+ getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl()+ ".htm");
					html.replace("%mp%", String.valueOf((int)getCurrentMp()));
				}
				sendHtmlMessage(player, html);
			}
			else if (val.equalsIgnoreCase("back"))
				showMessageWindow(player);
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/chamberlain/chamberlain-functions.htm");
				if (getCastle().getFunction(Castle.FUNC_RESTORE_EXP) != null)
					html.replace("%xp_regen%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_EXP).getLvl()));
				else
					html.replace("%xp_regen%", "0");
				if (getCastle().getFunction(Castle.FUNC_RESTORE_HP) != null)
					html.replace("%hp_regen%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_HP).getLvl()));
				else
					html.replace("%hp_regen%", "0");
				if (getCastle().getFunction(Castle.FUNC_RESTORE_MP) != null)
					html.replace("%mp_regen%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_MP).getLvl()));
				else
					html.replace("%mp_regen%", "0");
				sendHtmlMessage(player, html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("manage"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_SET_FUNCTIONS) == L2Clan.CP_CS_SET_FUNCTIONS)
			{
				if (val.equalsIgnoreCase("recovery"))
				{
					if (st.countTokens() >= 1)
					{
						if (getCastle().getOwnerId() == 0)
						{
							player.sendMessage("This castle have no owner, you cannot change configuration");
							return;
						}
						val = st.nextToken();
						if (val.equalsIgnoreCase("hp_cancel"))
						{
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-cancel.htm");
							html.replace("%apply%", "recovery hp 0");
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("mp_cancel"))
						{
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-cancel.htm");
							html.replace("%apply%", "recovery mp 0");
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("exp_cancel"))
						{
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-cancel.htm");
							html.replace("%apply%", "recovery exp 0");
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("edit_hp"))
						{
							val = st.nextToken();
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-apply.htm");
							html.replace("%name%", "Fireplace (HP Recovery Device)");
							int percent = Integer.parseInt(val);
							int cost;
							switch (percent)
							{
							case 80:
								cost = Config.CS_HPREG1_FEE;
								break;
							case 120:
								cost = Config.CS_HPREG2_FEE;
								break;
							case 180:
								cost = Config.CS_HPREG3_FEE;
								break;
							case 240:
								cost = Config.CS_HPREG4_FEE;
								break;
							default: // 300
							cost = Config.CS_HPREG5_FEE;
							break;
							}
							
							html.replace("%cost%", String.valueOf(cost)
									+ "</font>Adena /"
									+ String.valueOf(Config.CS_HPREG_FEE_RATIO
											/ 1000 / 60 / 60 / 24)
											+ " Day</font>)");
							html.replace("%use%", "Provides additional HP recovery for clan members in the castle.<font color=\"00FFFF\">"
									+ String.valueOf(percent) + "%</font>");
							html.replace("%apply%", "recovery hp "
									+ String.valueOf(percent));
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("edit_mp"))
						{
							val = st.nextToken();
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-apply.htm");
							html.replace("%name%", "Carpet (MP Recovery)");
							int percent = Integer.parseInt(val);
							int cost;
							switch (percent)
							{
							case 5:
								cost = Config.CS_MPREG1_FEE;
								break;
							case 15:
								cost = Config.CS_MPREG2_FEE;
								break;
							case 30:
								cost = Config.CS_MPREG3_FEE;
								break;
							default: // 40
							cost = Config.CS_MPREG4_FEE;
							break;
							}
							html.replace("%cost%", String.valueOf(cost)
									+ "</font>Adena /"
									+ String.valueOf(Config.CS_MPREG_FEE_RATIO
											/ 1000 / 60 / 60 / 24)
											+ " Day</font>)");
							html.replace("%use%", "Provides additional MP recovery for clan members in the castle.<font color=\"00FFFF\">"
									+ String.valueOf(percent) + "%</font>");
							html.replace("%apply%", "recovery mp "
									+ String.valueOf(percent));
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("edit_exp"))
						{
							val = st.nextToken();
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-apply.htm");
							html.replace("%name%", "Chandelier (EXP Recovery Device)");
							int percent = Integer.parseInt(val);
							int cost;
							switch (percent)
							{
							case 15:
								cost = Config.CS_EXPREG1_FEE;
								break;
							case 25:
								cost = Config.CS_EXPREG2_FEE;
								break;
							case 35:
								cost = Config.CS_EXPREG3_FEE;
								break;
							default: // 50
							cost = Config.CS_EXPREG4_FEE;
							break;
							}
							html.replace("%cost%", String.valueOf(cost)
									+ "</font>Adena /"
									+ String.valueOf(Config.CS_EXPREG_FEE_RATIO
											/ 1000 / 60 / 60 / 24)
											+ " Day</font>)");
							html.replace("%use%", "Restores the Exp of any clan member who is resurrected in the castle.<font color=\"00FFFF\">"
									+ String.valueOf(percent) + "%</font>");
							html.replace("%apply%", "recovery exp "
									+ String.valueOf(percent));
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("hp"))
						{
							if (st.countTokens() >= 1)
							{
								int fee;
								if (Config.DEBUG)
									_log.warning("Mp editing invoked");
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/chamberlain/functions-apply_confirmed.htm");
								if (getCastle().getFunction(Castle.FUNC_RESTORE_HP) != null)
								{
									if (getCastle().getFunction(Castle.FUNC_RESTORE_HP).getLvl() == Integer.parseInt(val))
									{
										html.setFile("data/html/chamberlain/functions-used.htm");
										html.replace("%val%", String.valueOf(val)
												+ "%");
										sendHtmlMessage(player, html);
										return;
									}
								}
								int percent = Integer.parseInt(val);
								switch (percent)
								{
								case 0:
									fee = 0;
									html.setFile("data/html/chamberlain/functions-cancel_confirmed.htm");
									break;
								case 80:
									fee = Config.CS_HPREG1_FEE;
									break;
								case 120:
									fee = Config.CS_HPREG2_FEE;
									break;
								case 180:
									fee = Config.CS_HPREG3_FEE;
									break;
								case 240:
									fee = Config.CS_HPREG4_FEE;
									break;
								default: // 300
									fee = Config.CS_HPREG5_FEE;
									break;
								}
								if (!getCastle().updateFunctions(player, Castle.FUNC_RESTORE_HP, percent, fee, Config.CS_HPREG_FEE_RATIO, (getCastle().getFunction(Castle.FUNC_RESTORE_HP) == null)))
								{
									html.setFile("data/html/chamberlain/low_adena.htm");
									sendHtmlMessage(player, html);
								}
								sendHtmlMessage(player, html);
							}
							return;
						}
						else if (val.equalsIgnoreCase("mp"))
						{
							if (st.countTokens() >= 1)
							{
								int fee;
								if (Config.DEBUG)
									_log.warning("Mp editing invoked");
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/chamberlain/functions-apply_confirmed.htm");
								if (getCastle().getFunction(Castle.FUNC_RESTORE_MP) != null)
								{
									if (getCastle().getFunction(Castle.FUNC_RESTORE_MP).getLvl() == Integer.parseInt(val))
									{
										html.setFile("data/html/chamberlain/functions-used.htm");
										html.replace("%val%", String.valueOf(val)
												+ "%");
										sendHtmlMessage(player, html);
										return;
									}
								}
								int percent = Integer.parseInt(val);
								switch (percent)
								{
								case 0:
									fee = 0;
									html.setFile("data/html/chamberlain/functions-cancel_confirmed.htm");
									break;
								case 5:
									fee = Config.CS_MPREG1_FEE;
									break;
								case 15:
									fee = Config.CS_MPREG2_FEE;
									break;
								case 30:
									fee = Config.CS_MPREG3_FEE;
									break;
								default: // 40
									fee = Config.CS_MPREG4_FEE;
									break;
								}
								if (!getCastle().updateFunctions(player, Castle.FUNC_RESTORE_MP, percent, fee, Config.CS_MPREG_FEE_RATIO, (getCastle().getFunction(Castle.FUNC_RESTORE_MP) == null)))
								{
									html.setFile("data/html/chamberlain/low_adena.htm");
									sendHtmlMessage(player, html);
								}
								sendHtmlMessage(player, html);
							}
							return;
						}
						else if (val.equalsIgnoreCase("exp"))
						{
							if (st.countTokens() >= 1)
							{
								int fee;
								if (Config.DEBUG)
									_log.warning("Exp editing invoked");
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/chamberlain/functions-apply_confirmed.htm");
								if (getCastle().getFunction(Castle.FUNC_RESTORE_EXP) != null)
								{
									if (getCastle().getFunction(Castle.FUNC_RESTORE_EXP).getLvl() == Integer.parseInt(val))
									{
										html.setFile("data/html/chamberlain/functions-used.htm");
										html.replace("%val%", String.valueOf(val)
												+ "%");
										sendHtmlMessage(player, html);
										return;
									}
								}
								int percent = Integer.parseInt(val);
								switch (percent)
								{
								case 0:
									fee = 0;
									html.setFile("data/html/chamberlain/functions-cancel_confirmed.htm");
									break;
								case 15:
									fee = Config.CS_EXPREG1_FEE;
									break;
								case 25:
									fee = Config.CS_EXPREG2_FEE;
									break;
								case 35:
									fee = Config.CS_EXPREG3_FEE;
									break;
								default: // 50
									fee = Config.CS_EXPREG4_FEE;
									break;
								}
								if (!getCastle().updateFunctions(player, Castle.FUNC_RESTORE_EXP, percent, fee, Config.CS_EXPREG_FEE_RATIO, (getCastle().getFunction(Castle.FUNC_RESTORE_EXP) == null)))
								{
									html.setFile("data/html/chamberlain/low_adena.htm");
									sendHtmlMessage(player, html);
								}
								sendHtmlMessage(player, html);
							}
							return;
						}
					}
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/edit_recovery.htm");
					String hp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 120\">120%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 180\">180%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 240\">240%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]";
					String exp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 35\">35%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
					String mp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]";
					if (getCastle().getFunction(Castle.FUNC_RESTORE_HP) != null)
					{
						html.replace("%hp_recovery%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_HP).getLvl())
								+ "%</font> (<font color=\"FFAABB\">"
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_HP).getLease())
								+ "</font>Adena /"
								+ String.valueOf(Config.CS_HPREG_FEE_RATIO
										/ 1000 / 60 / 60 / 24) + " Day)");
						html.replace("%hp_period%", "Withdraw the fee for the next time at "
								+ format.format(getCastle().getFunction(Castle.FUNC_RESTORE_HP).getEndTime()));
						html.replace("%change_hp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>]"
								+ hp);
					}
					else
					{
						html.replace("%hp_recovery%", "none");
						html.replace("%hp_period%", "none");
						html.replace("%change_hp%", hp);
					}
					if (getCastle().getFunction(Castle.FUNC_RESTORE_EXP) != null)
					{
						html.replace("%exp_recovery%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_EXP).getLvl())
								+ "%</font> (<font color=\"FFAABB\">"
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_EXP).getLease())
								+ "</font>Adena /"
								+ String.valueOf(Config.CS_EXPREG_FEE_RATIO
										/ 1000 / 60 / 60 / 24) + " Day)");
						html.replace("%exp_period%", "Withdraw the fee for the next time at "
								+ format.format(getCastle().getFunction(Castle.FUNC_RESTORE_EXP).getEndTime()));
						html.replace("%change_exp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>]"
								+ exp);
					}
					else
					{
						html.replace("%exp_recovery%", "none");
						html.replace("%exp_period%", "none");
						html.replace("%change_exp%", exp);
					}
					if (getCastle().getFunction(Castle.FUNC_RESTORE_MP) != null)
					{
						html.replace("%mp_recovery%", String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_MP).getLvl())
								+ "%</font> (<font color=\"FFAABB\">"
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_RESTORE_MP).getLease())
								+ "</font>Adena /"
								+ String.valueOf(Config.CS_MPREG_FEE_RATIO
										/ 1000 / 60 / 60 / 24) + " Day)");
						html.replace("%mp_period%", "Withdraw the fee for the next time at "
								+ format.format(getCastle().getFunction(Castle.FUNC_RESTORE_MP).getEndTime()));
						html.replace("%change_mp%", "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>]"
								+ mp);
					}
					else
					{
						html.replace("%mp_recovery%", "none");
						html.replace("%mp_period%", "none");
						html.replace("%change_mp%", mp);
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("other"))
				{
					if (st.countTokens() >= 1)
					{
						if (getCastle().getOwnerId() == 0)
						{
							player.sendMessage("This castle have no owner, you cannot change configuration");
							return;
						}
						val = st.nextToken();
						if (val.equalsIgnoreCase("tele_cancel"))
						{
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-cancel.htm");
							html.replace("%apply%", "other tele 0");
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("support_cancel"))
						{
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-cancel.htm");
							html.replace("%apply%", "other support 0");
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("edit_support"))
						{
							val = st.nextToken();
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-apply.htm");
							html.replace("%name%", "Insignia (Supplementary Magic)");
							int stage = Integer.parseInt(val);
							int cost;
							switch (stage)
							{
							case 1:
								cost = Config.CS_SUPPORT1_FEE;
								break;
							case 2:
								cost = Config.CS_SUPPORT2_FEE;
								break;
							case 3:
								cost = Config.CS_SUPPORT3_FEE;
								break;
							default:
								cost = Config.CS_SUPPORT4_FEE;
								break;
							}
							html.replace("%cost%", String.valueOf(cost)
									+ "</font>Adena /"
									+ String.valueOf(Config.CS_SUPPORT_FEE_RATIO
											/ 1000 / 60 / 60 / 24)
											+ " Day</font>)");
							html.replace("%use%", "Enables the use of supplementary magic.");
							html.replace("%apply%", "other support "
									+ String.valueOf(stage));
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("edit_tele"))
						{
							val = st.nextToken();
							NpcHtmlMessage html = new NpcHtmlMessage(1);
							html.setFile("data/html/chamberlain/functions-apply.htm");
							html.replace("%name%", "Mirror (Teleportation Device)");
							int stage = Integer.parseInt(val);
							int cost;
							switch (stage)
							{
							case 1:
								cost = Config.CS_TELE1_FEE;
								break;
							default:
								cost = Config.CS_TELE2_FEE;
								break;
							}
							html.replace("%cost%", String.valueOf(cost)
									+ "</font>Adena /"
									+ String.valueOf(Config.CS_TELE_FEE_RATIO
											/ 1000 / 60 / 60 / 24)
											+ " Day</font>)");
							html.replace("%use%", "Teleports clan members in a castle to the target <font color=\"00FFFF\">Stage "
									+ String.valueOf(stage)
									+ "</font> staging area");
							html.replace("%apply%", "other tele "+ String.valueOf(stage));
							sendHtmlMessage(player, html);
							return;
						}
						else if (val.equalsIgnoreCase("tele"))
						{
							if (st.countTokens() >= 1)
							{
								int fee;
								if (Config.DEBUG)
									_log.warning("Tele editing invoked");
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/chamberlain/functions-apply_confirmed.htm");
								if (getCastle().getFunction(Castle.FUNC_TELEPORT) != null)
								{
									if (getCastle().getFunction(Castle.FUNC_TELEPORT).getLvl() == Integer.parseInt(val))
									{
										html.setFile("data/html/chamberlain/functions-used.htm");
										html.replace("%val%", "Stage "	+ String.valueOf(val));
										sendHtmlMessage(player, html);
										return;
									}
								}
								int lvl = Integer.parseInt(val);
								switch (lvl)
								{
								case 0:
									fee = 0;
									html.setFile("data/html/chamberlain/functions-cancel_confirmed.htm");
									break;
								case 1:
									fee = Config.CS_TELE1_FEE;
									break;
								default:
									fee = Config.CS_TELE2_FEE;
									break;
								}
								if (!getCastle().updateFunctions(player, Castle.FUNC_TELEPORT, lvl, fee, Config.CS_TELE_FEE_RATIO, (getCastle().getFunction(Castle.FUNC_TELEPORT) == null)))
								{
									html.setFile("data/html/chamberlain/low_adena.htm");
									sendHtmlMessage(player, html);
								}
								sendHtmlMessage(player, html);
							}
							return;
						}
						else if (val.equalsIgnoreCase("support"))
						{
							if (st.countTokens() >= 1)
							{
								int fee;
								if (Config.DEBUG)
									_log.warning("Support editing invoked");
								val = st.nextToken();
								NpcHtmlMessage html = new NpcHtmlMessage(1);
								html.setFile("data/html/chamberlain/functions-apply_confirmed.htm");
								if (getCastle().getFunction(Castle.FUNC_SUPPORT) != null)
								{
									if (getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl() == Integer.parseInt(val))
									{
										html.setFile("data/html/chamberlain/functions-used.htm");
										html.replace("%val%", "Stage "
												+ String.valueOf(val));
										sendHtmlMessage(player, html);
										return;
									}
								}
								int lvl = Integer.parseInt(val);
								switch (lvl)
								{
								case 0:
									fee = 0;
									html.setFile("data/html/chamberlain/functions-cancel_confirmed.htm");
									break;
								case 1:
									fee = Config.CS_SUPPORT1_FEE;
									break;
								case 2:
									fee = Config.CS_SUPPORT2_FEE;
									break;
								case 3:
									fee = Config.CS_SUPPORT3_FEE;
									break;
								default:
									fee = Config.CS_SUPPORT4_FEE;
									break;
								}
								if (!getCastle().updateFunctions(player, Castle.FUNC_SUPPORT, lvl, fee, Config.CS_SUPPORT_FEE_RATIO, (getCastle().getFunction(Castle.FUNC_SUPPORT) == null)))
								{
									html.setFile("data/html/chamberlain/low_adena.htm");
									sendHtmlMessage(player, html);
								}
								else
									sendHtmlMessage(player, html);
							}
							return;
						}
					}
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/edit_other.htm");
					String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]";
					String support = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>]";
					if (getCastle().getFunction(Castle.FUNC_TELEPORT) != null)
					{
						html.replace("%tele%", "Stage "
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_TELEPORT).getLvl())
								+ "</font> (<font color=\"FFAABB\">"
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_TELEPORT).getLease())
								+ "</font>Adena /"
								+ String.valueOf(Config.CS_TELE_FEE_RATIO
										/ 1000 / 60 / 60 / 24) + " Day)");
						html.replace("%tele_period%", "Withdraw the fee for the next time at "
								+ format.format(getCastle().getFunction(Castle.FUNC_TELEPORT).getEndTime()));
						html.replace("%change_tele%", "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Deactivate</a>]"
								+ tele);
					}
					else
					{
						html.replace("%tele%", "none");
						html.replace("%tele_period%", "none");
						html.replace("%change_tele%", tele);
					}
					if (getCastle().getFunction(Castle.FUNC_SUPPORT) != null)
					{
						html.replace("%support%", "Stage "
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl())
								+ "</font> (<font color=\"FFAABB\">"
								+ String.valueOf(getCastle().getFunction(Castle.FUNC_SUPPORT).getLease())
								+ "</font>Adena /"
								+ String.valueOf(Config.CS_SUPPORT_FEE_RATIO
										/ 1000 / 60 / 60 / 24) + " Day)");
						html.replace("%support_period%", "Withdraw the fee for the next time at "
								+ format.format(getCastle().getFunction(Castle.FUNC_SUPPORT).getEndTime()));
						html.replace("%change_support%", "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>]"
								+ support);
					}
					else
					{
						html.replace("%support%", "none");
						html.replace("%support_period%", "none");
						html.replace("%change_support%", support);
					}
					sendHtmlMessage(player, html);
				}
				else if (val.equalsIgnoreCase("back"))
					showMessageWindow(player);
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/manage.htm");
					sendHtmlMessage(player, html);
				}
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				sendHtmlMessage(player, html);
			}
			return;
		}
		else if (actualCommand.equalsIgnoreCase("support"))
		{
			setTarget(player);
			L2Skill skill;
			if (val.isEmpty())
				return;
			
			try
			{
				int skill_id = Integer.parseInt(val);
				try
				{
					if (getCastle().getFunction(Castle.FUNC_SUPPORT) == null)
						return;
					if (getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl() == 0)
						return;
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					int skill_lvl = 0;
					if (st.countTokens() >= 1)
						skill_lvl = Integer.parseInt(st.nextToken());
					skill = SkillTable.getInstance().getInfo(skill_id, skill_lvl);
					if (skill.getSkillType() == L2SkillType.SUMMON)
						player.doSimultaneousCast(skill);
					else
					{
						if (!((skill.getMpConsume() + skill.getMpInitialConsume()) > getCurrentMp()))
							doCast(skill);
						else
						{
							html.setFile("data/html/chamberlain/support-no_mana.htm");
							html.replace("%mp%", String.valueOf((int)getCurrentMp()));
							sendHtmlMessage(player, html);
							return;
						}
					}
					html.setFile("data/html/chamberlain/support-done.htm");
					html.replace("%mp%", String.valueOf((int)getCurrentMp()));
					sendHtmlMessage(player, html);
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid skill level, contact your admin!");
				}
			}
			catch (Exception e)
			{
				player.sendMessage("Invalid skill level, contact your admin!");
			}
			return;
		}
		else if (actualCommand.equalsIgnoreCase("support_back"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			if (getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl() == 0)
				return;
			html.setFile("data/html/chamberlain/support"
					+ getCastle().getFunction(Castle.FUNC_SUPPORT).getLvl()	+ ".htm");
			html.replace("%mp%", String.valueOf((int)getStatus().getCurrentMp()));
			sendHtmlMessage(player, html);
		}
		else if (actualCommand.equalsIgnoreCase("goto"))
		{
			int whereTo = Integer.parseInt(val);
			doTeleport(player, whereTo);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("siege_change")) // siege day set
		{
			if (Config.CL_SET_SIEGE_TIME_LIST.isEmpty())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/chamberlain/chamberlain-noadmin.htm");
				sendHtmlMessage(player, html);
			}
			else if (player.isClanLeader())
			{
				if (getCastle().getSiege().getTimeRegistrationOverDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/siegetime1.htm");
					sendHtmlMessage(player, html);
				}
				else if (getCastle().getSiege().getIsTimeRegistrationOver())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/siegetime2.htm");
					sendHtmlMessage(player, html);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/siegetime3.htm");
					html.replace("%time%", String.valueOf(getCastle().getSiegeDate().getTime()));
					sendHtmlMessage(player, html);
				}
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				sendHtmlMessage(player, html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("siege_time_set")) // set preDay
		{
			/*boolean isAfternoon = Config.SIEGE_HOUR_LIST_MORNING.isEmpty();
				switch (Integer.parseInt(val))
				{
					case 0:
					case 4:
						break;
					case 1:
						_preDay = Integer.parseInt(st.nextToken());
						break;
					case 2:
						isAfternoon = Boolean.parseBoolean(st.nextToken());
						break;
					case 3:
						_preHour = Integer.parseInt(st.nextToken());
						break;
					default:
						break;
				}
				NpcHtmlMessage html = getNextSiegeTimePage(Integer.parseInt(val),isAfternoon);
				
				if (html == null)
				{
					if (Config.CL_SET_SIEGE_TIME_LIST.contains("day"))
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, _preDay);
					else
						getCastle().getSiegeDate().set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
					if (Config.CL_SET_SIEGE_TIME_LIST.contains("hour"))
						getCastle().getSiegeDate().set(Calendar.HOUR_OF_DAY, _preHour);
					if (Config.CL_SET_SIEGE_TIME_LIST.contains("minute"))
						getCastle().getSiegeDate().set(Calendar.MINUTE, Integer.parseInt(st.nextToken()));
					// now store the changed time and finished next Siege Time registration
					getCastle().getSiege().endTimeRegistration(false);
					
					html = new NpcHtmlMessage(1);
					html.setFile("data/html/chamberlain/siegetime8.htm");
					html.replace("%time%", String.valueOf(getCastle().getSiegeDate().getTime()));
				}
				sendHtmlMessage(player, html);*/
			player.sendMessage("Players cannot change the siege time anymore");
		}
		else if (actualCommand.equals("give_crown"))
		{
			if (siegeBlocksFunction(player))
				return;
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (player.isClanLeader())
			{
				if (player.getInventory().getItemByItemId(6841) == null)
				{
					player.addItem("Chamberlain - Crown", 6841, 1, this, true);
					
					html.setFile("data/html/chamberlain/chamberlain-gavecrown.htm");
					html.replace("%CharName%", String.valueOf(player.getName()));
					html.replace("%FeudName%", String.valueOf(getCastle().getName()));
				}
				else
				{
					html.setFile("data/html/chamberlain/chamberlain-hascrown.htm");
				}
			}
			else
			{
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
			}
			
			player.sendPacket(html);
		}
		else if (actualCommand.equals("give_cloak"))
		{
			final int cloakId = CrownTable.getCloakId(getCastle().getCastleId(), (player.isClanLeader() ||
					(player.isThisCharacterMarried() && player.getPartnerId() == player.getClan().getLeaderId())));
			
			if (cloakId != -1)
			{
				if (player.getInventory().getItemByItemId(cloakId) == null)
				{
					player.addItem("Chamberlain - Cloak", cloakId, 1, this, true);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain.htm");
					html.replace("%CharName%", String.valueOf(player.getName()));
					html.replace("%FeudName%", String.valueOf(getCastle().getName()));
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/chamberlain/chamberlain-hascrown.htm");
					player.sendPacket(html);
				}
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
				sendHtmlMessage(player, html);
				player.sendMessage("The free castle cloak is only available at Aden and Rune castles.");
			}
		}
		
		super.onBypassFeedback(player, command);
	}
}

@SuppressWarnings("unused")
private NpcHtmlMessage getNextSiegeTimePage(int now, boolean isAfternoon)
{
	NpcHtmlMessage ret = new NpcHtmlMessage(1);
	if (now == 0 && Config.CL_SET_SIEGE_TIME_LIST.contains("day"))
	{
		ret.setFile("data/html/chamberlain/siegetime4.htm");
		return ret;
	}
	if (now < 3 && Config.CL_SET_SIEGE_TIME_LIST.contains("hour"))
	{
		switch (now)
		{
		case 0:
		case 1:
			if (!Config.SIEGE_HOUR_LIST_MORNING.isEmpty() && !Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty())
			{
				ret.setFile("data/html/chamberlain/siegetime5.htm");
				return ret;
			}
		case 2:
			ret.setFile("data/html/chamberlain/siegetime6.htm");
			List<Integer> list;
			int inc = 0;
			String ampm = "";
			
			if (!isAfternoon)
			{
				if (Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty()) ampm = "AM";
				list = Config.SIEGE_HOUR_LIST_MORNING;
			}
			else
			{
				if (Config.SIEGE_HOUR_LIST_MORNING.isEmpty()) ampm = "PM";
				inc = 12;
				list = Config.SIEGE_HOUR_LIST_AFTERNOON;
			}
			
			final StringBuilder tList = new StringBuilder(list.size() * 50);
			for (Integer hour : list)
			{
				if (hour == 0)
				{
					StringUtil.append(tList,
							"<a action=\"bypass -h npc_%objectId%_siege_time_set 3 ",
							String.valueOf(hour + inc),
							"\">",
							String.valueOf(hour + 12),
							":00 ",
							ampm,
							"</a><br>"
							);
				}
				else
				{
					StringUtil.append(tList,
							"<a action=\"bypass -h npc_%objectId%_siege_time_set 3 ",
							String.valueOf(hour + inc),
							"\">",
							String.valueOf(hour),
							":00 ",
							ampm,
							"</a><br>"
							);
				}
			}
			ret.replace("%links%", tList.toString());
		}
		return ret;
	}
	if (now < 4 && Config.CL_SET_SIEGE_TIME_LIST.contains("minute"))
	{
		ret.setFile("data/html/chamberlain/siegetime7.htm");
		return ret;
	}
	
	return null;
}

private void sendHtmlMessage(L2PcInstance player, String htmlMessage)
{
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	html.setHtml(htmlMessage);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcname%", getName());
	player.sendPacket(html);
}

private void showMessageWindow(L2PcInstance player)
{
	player.sendPacket(ActionFailed.STATIC_PACKET);
	String filename = "data/html/chamberlain/chamberlain-no.htm";
	
	int condition = validateCondition(player);
	if (condition > COND_ALL_FALSE)
	{
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/chamberlain/chamberlain-busy.htm"; // Busy because of siege
		else if (condition == COND_OWNER) // Clan owns castle
			filename = "data/html/chamberlain/chamberlain.htm"; // Owner message window
	}
	
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	html.setFile(filename);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcname%", getName());
	player.sendPacket(html);
}

private void doTeleport(L2PcInstance player, int val)
{
	if (Config.DEBUG)
		_log.warning("doTeleport(L2PcInstance player, int val) is called");
	L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
	if (list != null)
	{
		if (player.reduceAdena("Teleport", list.getPrice(), this, true))
		{
			if (Config.DEBUG)
				_log.warning("Teleporting player " + player.getName()
						+ " for Castle to new location: " + list.getLocX()
						+ ":" + list.getLocY() + ":" + list.getLocZ());
			player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ());
		}
	}
	else
		_log.warning("No teleport destination with id:" + val);
	player.sendPacket(ActionFailed.STATIC_PACKET);
}

protected int validateCondition(L2PcInstance player)
{
	if (getCastle() != null && getCastle().getCastleId() > 0)
	{
		if (player.getClan() != null)
		{
			if (getCastle().getSiege().getIsInProgress())
				return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
			else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
				return COND_OWNER; // Owner
		}
	}
	return COND_ALL_FALSE;
}

private boolean validatePrivileges(L2PcInstance player, int privilege)
{
	if ((player.getClanPrivileges() & privilege) != privilege)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/chamberlain/chamberlain-noprivs.htm");
		player.sendPacket(html);
		return false;
	}
	return true;
}

private boolean siegeBlocksFunction(L2PcInstance player)
{
	if (getCastle().getSiege().getIsInProgress())
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/chamberlain/chamberlain-busy.htm");
		html.replace("%npcname%", String.valueOf(getName()));
		player.sendPacket(html);
		return true;
	}
	
	return false;
}
}
