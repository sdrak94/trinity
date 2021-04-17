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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.2.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PrivateStoreListBuy extends L2GameServerPacket
{
//	private static final String _S__D1_PRIVATEBUYLISTBUY = "[S] b8 PrivateBuyListBuy";
private static final String _S__D1_PRIVATESTORELISTBUY = "[S] be PrivateStoreListBuy";
private int _objId;
private long _playerAdena;
private TradeList.TradeItem[] _items;

public PrivateStoreListBuy(L2PcInstance player, L2PcInstance storePlayer)
{
	_objId = storePlayer.getObjectId();
	_playerAdena = player.getBlueEva();
	storePlayer.getSellList().updateItems(); // Update SellList for case inventory content has changed
	_items = storePlayer.getBuyList().getAvailableItems(player.getInventory());
}

@Override
protected final void writeImpl()
{
	writeC(0xbe);
	writeD(_objId);
	writeQ(_playerAdena);
	
	writeD(_items.length);
	
	for (TradeList.TradeItem item : _items)
	{
		writeD(item.getObjectId());
		writeD(item.getItem().getItemId());
		writeH(item.getEnchant());
		writeQ(item.getCount()); //give max possible sell amount
		
		writeQ(item.getItem().getReferencePrice());
		writeH(0);
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItem().getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeD(bodyPart);
		writeH(type2);
		writeQ(item.getPrice());//buyers price
		
		writeQ(item.getStoreCount());  // maximum possible tradecount
		
		// T1
		writeH(item.getAttackElementType());
		writeH(item.getAttackElementPower());
		for (byte i = 0; i < 6; i++)
		{
			writeH(item.getElementDefAttr(i));
		}
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__D1_PRIVATESTORELISTBUY;
}
}
