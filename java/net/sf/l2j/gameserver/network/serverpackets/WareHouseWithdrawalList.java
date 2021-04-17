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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * 0x42 WarehouseWithdrawalList  dh (h dddhh dhhh d)
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/29 23:15:10 $
 */
public final class WareHouseWithdrawalList extends L2GameServerPacket
{
public static final int PRIVATE = 1;
public static final int CLAN = 4;
public static final int CASTLE = 3; //not sure
public static final int FREIGHT = 1;
private static Logger _log = Logger.getLogger(WareHouseWithdrawalList.class.getName());
private static final String _S__54_WAREHOUSEWITHDRAWALLIST = "[S] 42 WareHouseWithdrawalList";
private L2PcInstance _activeChar;
private long _playerAdena;
private L2ItemInstance[] _items;
private int _whType;

public WareHouseWithdrawalList(L2PcInstance player, int type)
{
	_activeChar = player;
	_whType = type;
	
	_playerAdena = _activeChar.getAdena();
	if (_activeChar.getActiveWarehouse() == null)
	{
		// Something went wrong!
		_log.warning("error while sending withdraw request to: " + _activeChar.getName());
		return;
	}
	else
		_items = _activeChar.getActiveWarehouse().getItems();
	
	if (Config.DEBUG)
		for (L2ItemInstance item : _items)
			_log.fine("item:" + item.getItem().getName() + " type1:" + item.getItem().getType1() + " type2:" + item.getItem().getType2());
}

@Override
protected final void writeImpl()
{
	writeC(0x42);
	/* 0x01-Private Warehouse
	 * 0x02-Clan Warehouse
	 * 0x03-Castle Warehouse
	 * 0x04-Warehouse */
	writeH(_whType);
	writeQ(_playerAdena);
	writeH(_items.length);
	
	for (L2ItemInstance item : _items)
	{
		writeH(item.getItem().getType1());
		writeD(item.getObjectId());
		writeD(item.getItemId());
		writeQ(item.getCount());
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeH(type2);
		writeH(item.getCustomType1());
		writeD(bodyPart);
		writeH(item.getEnchantLevel());
		writeH(0x00);
		writeH(item.getCustomType2());
		writeD(item.getObjectId());
		if (item.isAugmented())
		{
			writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
			writeD(item.getAugmentation().getAugmentationId() >> 16);
		}
		else
			writeQ(0x00);
		
		writeH(item.getAttackElementType());
		writeH(item.getAttackElementPower());
		for (byte i = 0; i < 6; i++)
		{
			writeH(item.getElementDefAttr(i));
		}
		
		writeD(item.getMana());
		// T2
		writeD(item.isTimeLimitedItem() ? (int) (item.getRemainingTime()/1000) : -1);
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__54_WAREHOUSEWITHDRAWALLIST;
}
}
