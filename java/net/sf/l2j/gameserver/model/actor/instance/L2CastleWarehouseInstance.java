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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseDepositList;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseWithdrawalList;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author l3x
 */
public class L2CastleWarehouseInstance extends L2NpcInstance
{
protected static final int COND_ALL_FALSE = 0;
protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
protected static final int COND_OWNER = 2;

/**
 * @param template
 */
public L2CastleWarehouseInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public boolean isWarehouse()
{
	return true;
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
}

private void showDepositWindow(L2PcInstance player)
{
	player.sendPacket(ActionFailed.STATIC_PACKET);
	player.setActiveWarehouse(player.getWarehouse());
	player.tempInventoryDisable();
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
			if (!Config.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH && (player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE));
			
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.tempInventoryDisable();
			WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
			player.sendPacket(dl);
		}
	}
}

private void showWithdrawWindowClan(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
{
	player.sendPacket(ActionFailed.STATIC_PACKET);
	
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
	else if (command.equals("WithdrawC"))
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
	else if (command.startsWith("Chat"))
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(command.substring(5));
		}
		catch (IndexOutOfBoundsException ioobe){}
		catch (NumberFormatException nfe){}
		showChatWindow(player, val);
	}
	else
		super.onBypassFeedback(player, command);
}

@Override
public void showChatWindow(L2PcInstance player, int val)
{
	player.sendPacket( ActionFailed.STATIC_PACKET );
	String filename = "data/html/castlewarehouse/castlewarehouse-no.htm";
	
	int condition = validateCondition(player);
	
	if (condition > COND_ALL_FALSE)
	{
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/castlewarehouse/castlewarehouse-busy.htm";
		else if (condition == COND_OWNER)
		{
			if (val == 0)
				filename = "data/html/castlewarehouse/castlewarehouse.htm";
			else
				filename = "data/html/castlewarehouse/castlewarehouse-" + val + ".htm";
		}
	}
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	html.setFile(filename);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcname%", getName());
	player.sendPacket(html);
}

protected int validateCondition(L2PcInstance player)
{
	if (player.isGM())
		return COND_OWNER;
	
	if (getCastle() != null && getCastle().getCastleId() > 0)
	{
		if (player.getClan() != null)
		{
			if (getCastle().getSiege().getIsInProgress())
				return COND_BUSY_BECAUSE_OF_SIEGE;
			else if (getCastle().getOwnerId() == player.getClanId())
				return COND_OWNER;
		}
	}
	return COND_ALL_FALSE;
}
}
