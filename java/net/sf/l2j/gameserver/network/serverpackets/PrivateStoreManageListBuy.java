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

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:40 $
 */
public class PrivateStoreManageListBuy extends L2GameServerPacket
{
private static final String _S__D0_PRIVATESELLLISTBUY = "[S] bd PrivateStoreManageListBuy";
private int _objId;
private long _playerAdena;
private L2ItemInstance[] _itemList;
private TradeList.TradeItem[] _buyList;

public PrivateStoreManageListBuy(L2PcInstance player)
{
	_objId = player.getObjectId();
	_playerAdena = player.getBlueEva();
	_itemList = player.getInventory().getUniqueItems(false,true);
	_buyList = player.getBuyList().getItems();
}

@Override
protected final void writeImpl()
{
	writeC(0xbd);
	//section 1
	writeD(_objId);
	writeQ(_playerAdena);
	
	//section2
	writeD(_itemList.length); // inventory items for potential buy
	for (L2ItemInstance item : _itemList)
	{
		writeD(item.getItemId());
		writeH(0); //show enchant lvl as 0, as you can't buy enchanted weapons
		writeQ(item.getCount());
		writeQ(item.getReferencePrice());
		writeH(0x00);
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItem().getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeD(bodyPart);
		writeH(type2);
		
		// T1
		writeH(item.getAttackElementType());
		writeH(item.getAttackElementPower());
		for (byte i = 0; i < 6; i++)
		{
			writeH(item.getElementDefAttr(i));
		}
	}
	
	//section 3
	writeD(_buyList.length); //count for all items already added for buy
	for (TradeList.TradeItem item : _buyList)
	{
		writeD(item.getItem().getItemId());
		writeH(0);
		writeQ(item.getCount());
		writeQ(item.getItem().getReferencePrice());
		writeH(0x00);
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItem().getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeD(bodyPart);
		writeH(type2);
		writeQ(item.getPrice());//your price
		writeQ(item.getItem().getReferencePrice());//fixed store price
		
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
	return _S__D0_PRIVATESELLLISTBUY;
}
}
