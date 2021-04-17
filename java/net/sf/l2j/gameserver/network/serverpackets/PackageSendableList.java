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

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 *
 *
 * @author  -Wooden-
 */
public class PackageSendableList extends L2GameServerPacket
{
private static final String _S__C3_PACKAGESENDABLELIST = "[S] d2 PackageSendableList";
private final int _targetPlayerObjId;
private final long _playerAdena;
private final FastList<L2ItemInstance> _items;

public PackageSendableList(L2PcInstance player, int targetPlayerObjId)
{
	_targetPlayerObjId = targetPlayerObjId;
	_playerAdena = player.getAdena();
	
	_items = new FastList<L2ItemInstance>();
	for (L2ItemInstance temp : player.getInventory().getAvailableItemsFreight())
	{
		if (temp != null && temp.isDepositableFreight())
			_items.add(temp);
	}
}

/**
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
 */
@Override
protected
void writeImpl()
{
	writeC(0xd2);
	
	writeD(_targetPlayerObjId);
	writeQ(_playerAdena);
	writeD(_items.size());
	for(L2ItemInstance item : _items) // format inside the for taken from SellList part use should be about the same
	{
		writeH(item.getItem().getType1());
		writeD(item.getObjectId());
		writeD(item.getItemId());
		writeQ(item.getCount());
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItem().getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeH(type2);
		writeH(item.getCustomType1());
		writeD(bodyPart);
		writeH(item.getEnchantLevel());
		writeH(0x00);
		writeH(item.getCustomType2());
		writeD(item.getObjectId()); // some item identifier later used by client to answer (see RequestPackageSend) not item id nor object id maybe some freight system id??
		//T1
		writeH(item.getAttackElementType());
		writeH(item.getAttackElementPower());
		writeH(item.getElementDefAttr((byte)0));
		writeH(item.getElementDefAttr((byte)1));
		writeH(item.getElementDefAttr((byte)2));
		writeH(item.getElementDefAttr((byte)3));
		writeH(item.getElementDefAttr((byte)4));
		writeH(item.getElementDefAttr((byte)5));
	}
	_items.clear();
}

/**
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _S__C3_PACKAGESENDABLELIST;
}
}
