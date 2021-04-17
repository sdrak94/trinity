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
package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 * CPU Disasm
 * Packets: ddhhQQ cddb
 */
public final class SetPrivateStoreListBuy extends L2GameClientPacket
{
private static final String _C__91_SETPRIVATESTORELISTBUY = "[C] 91 SetPrivateStoreListBuy";

private static final int BATCH_LENGTH = 40; // length of the one item

private Item[] _items = null;

@Override
protected void readImpl()
{
	int count = readD();
	if (count < 0
			|| count > Config.MAX_ITEM_IN_PACKET
			|| count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	
	_items = new Item[count];
	for (int i = 0; i < count; i++)
	{
		int itemId = readD();
		
		if (itemId == 4355)
		{
			getClient().getActiveChar().sendMessage("You cannot put Blue Evas into your private store");
			getClient().getActiveChar().sendPacket(new PrivateStoreManageListBuy(getClient().getActiveChar()));
			
			return;
		}
		
		readH();//TODO analyse this
		readH();//TODO analyse this
		long cnt = readQ();
		long price = readQ();
		
		if (itemId < 1 || cnt < 1 || price < 0)
		{
			_items = null;
			return;
		}
		readC(); // FE
		readD(); // FF 00 00 00
		readD(); // 00 00 00 00
		readB(new byte[7]); // Completely Unknown
		
		_items[i] = new Item(itemId, cnt, price);
	}
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if (_items == null)
	{
		player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
		player.broadcastUserInfo();
		return;
	}
	
	if (!player.getAccessLevel().allowTransaction())
	{
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
		return;
	}
	
	if (player.isInsideZone(L2Character.ZONE_NOSTORE) || player.isInAZoneThatDoesntAllowShops())
	{
		player.sendPacket(new PrivateStoreManageListBuy(player));
		player.sendPacket(new SystemMessage(SystemMessageId.NO_PRIVATE_STORE_HERE));
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	TradeList tradeList = player.getBuyList();
	tradeList.clear();
	
	// Check maximum number of allowed slots for pvt shops
	if (_items.length > player.getPrivateBuyStoreLimit())
	{
		player.sendPacket(new PrivateStoreManageListBuy(player));
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
		return;
	}
	
	int totalCost = 0;
	for (Item i : _items)
	{
		if (!i.addToTradeList(tradeList))
		{
			/*			Util.handleIllegalPlayerAction(player, "Warning!! Character "
					+ player.getName() + " of account "
					+ player.getAccountName() + " tried to set price more than "
					+ MAX_ADENA + " adena in Private Store - Buy.",
					Config.DEFAULT_PUNISH);*/
			player.sendPacket(new PrivateStoreManageListBuy(player));
			return;
		}
		
		totalCost += i.getCost();
		if (totalCost > MAX_ADENA)
		{
			/*				Util.handleIllegalPlayerAction(player, "Warning!! Character "
						+ player.getName() + " of account "
						+ player.getAccountName() + " tried to set total price more than "
						+ MAX_ADENA + " adena in Private Store - Buy.",
						Config.DEFAULT_PUNISH);*/
			player.sendPacket(new PrivateStoreManageListBuy(player));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY));
			return;
		}
	}
	
	// Check for available funds
	if (totalCost > player.getBlueEva())
	{
		player.sendPacket(new PrivateStoreManageListBuy(player));
		player.sendPacket(new SystemMessage(SystemMessageId.THE_PURCHASE_PRICE_IS_HIGHER_THAN_MONEY));
		return;
	}
	
	player.sitDown();
	player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY);
	player.broadcastUserInfo();
	player.broadcastPacket(new PrivateStoreMsgBuy(player));
}

private class Item
{
private final int _itemId;
private final long _count;
private final long _price;

public Item(int id, long num, long pri)
{
	_itemId = id;
	_count = num;
	_price = pri;
}

public boolean addToTradeList(TradeList list)
{
	if ((MAX_ADENA / _count) < _price)
		return false;
	if (_itemId == 4355)
		return false;
	
	return list.addItemByItemId(_itemId, _count, _price) != null;
}

public long getCost()
{
	return _count * _price;
}
}

@Override
public String getType()
{
	return _C__91_SETPRIVATESTORELISTBUY;
}
}
