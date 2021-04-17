package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class GMViewItemList extends L2GameServerPacket
{
//private static Logger _log = Logger.getLogger(GMViewItemList.class.getName());
private static final String _S__AD_GMVIEWITEMLIST = "[S] 9a GMViewItemList";
private final L2ItemInstance[] _items;
private final L2PcInstance _cha;
private final String _playerName;

public GMViewItemList(L2PcInstance cha)
{
	_items = cha.getInventory().getItems();
	_playerName = cha.getName();
	_cha = cha;
}

@Override
protected final void writeImpl()
{
	writeC(0x9a);
	writeS(_playerName);
	writeD(_cha.getInventoryLimit()); // inventory limit
	writeH(0x01); // show window ??
	writeH(_items.length);
	
	for (L2ItemInstance temp : _items)
	{
		if (temp == null || temp.getItem() == null)
			continue;
		
		writeH(temp.getItem().getType1());
		
		writeD(temp.getObjectId());
		writeD(temp.getItemId());
		writeD(temp.getLocationSlot()); // T1
		writeQ(temp.getCount());
		
		int type2 = temp.getItem().getType2();
		int bodyPart = temp.getItem().getBodyPart();
		
		if (temp.isEquipped() && bodyPart == (L2Item.SLOT_HAIR | L2Item.SLOT_HAIR2))
			bodyPart = _cha.getInventory().getSlotFromItem(temp);
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (temp.getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeH(type2);
		writeH(temp.getCustomType1());
		writeH(temp.isEquipped() ? 0x01 : 0x00);
		writeD(bodyPart);
		writeH(temp.getEnchantLevel());
		writeH(temp.getCustomType2());
		if (temp.isAugmented())
			writeD(temp.getAugmentation().getAugmentationId());
		else
			writeD(0x00);
		writeD(temp.getMana());
		
		// T1
		writeH(temp.getAttackElementType());
		writeH(temp.getAttackElementPower());
		for (byte i = 0; i < 6; i++)
		{
			writeH(temp.getElementDefAttr(i));
		}
		// T2
		writeD(temp.isTimeLimitedItem() ? (int) (temp.getRemainingTime()/1000) : -1);
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__AD_GMVIEWITEMLIST;
}
}
