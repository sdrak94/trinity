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
 * @version $Revision: 1.2.2.3.2.6 $ $Date: 2005/03/27 15:29:57 $
 */
public class PrivateStoreListSell extends L2GameServerPacket
{
private static final String _S__B4_PRIVATESTORELISTSELL = "[S] a1 PrivateStoreListSell";
private int _objId;
private long _playerAdena;
private boolean _packageSale;
private TradeList.TradeItem[] _items;

// player's private shop
public PrivateStoreListSell(L2PcInstance player, L2PcInstance storePlayer)
{
	_objId = storePlayer.getObjectId();
	_playerAdena = player.getBlueEva();
	_items = storePlayer.getSellList().getItems();
	_packageSale = storePlayer.getSellList().isPackaged();
}

@Override
protected final void writeImpl()
{
	writeC(0xa1);
	writeD(_objId);
	writeD(_packageSale ? 1 : 0);
	writeQ(_playerAdena);
	writeD(_items.length);
	for (TradeList.TradeItem item : _items)
	{
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItem().getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeD(type2);
		writeD(item.getObjectId());
		writeD(item.getItem().getItemId());
		writeQ(item.getCount());
		writeH(0x00);
		writeH(item.getEnchant());
		writeH(item.getCustomType2());
		writeD(bodyPart);
		writeQ(item.getPrice()); //your price
		writeQ(item.getItem().getReferencePrice()); //store price
		
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
	return _S__B4_PRIVATESTORELISTSELL;
}
}
