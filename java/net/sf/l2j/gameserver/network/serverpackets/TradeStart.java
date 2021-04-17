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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public final class TradeStart extends L2GameServerPacket
{
private static final String _S__2E_TRADESTART = "[S] 14 TradeStart";
private L2PcInstance _activeChar;
private L2ItemInstance[] _itemList;

public TradeStart (L2PcInstance player)
{
	_activeChar = player;
	
	boolean allow = _activeChar.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS;
	
	if (!allow)
	{
		try
		{
			if (_activeChar.isThisCharacterMarried())
			{
				final long marriageDate = CoupleManager.getInstance().getCouple(_activeChar.getCoupleId()).getWeddingDate().getTimeInMillis();
				final long difference = System.currentTimeMillis() - marriageDate;
				
				if (difference > 5529600000L) // 128 days
				{
					if (_activeChar.getActiveTradeList().getPartner().getObjectId() == _activeChar.getPartnerId())
						allow = true;
				}
			}
		}
		catch (Exception e)
		{
			allow = false;
		}
	}
	
	_itemList = _activeChar.getInventory().getAvailableItems(true, allow);
}

@Override
protected final void writeImpl()
{//0x2e TradeStart   d h (h dddhh dhhh)
	if (_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null)
		return;
	
	writeC(0x14);
	writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());
	//writeD((_activeChar != null || _activeChar.getTransactionRequester() != null)? _activeChar.getTransactionRequester().getObjectId() : 0);
	
	writeH(_itemList.length);
	for (L2ItemInstance item : _itemList)
	{
		writeH(item.getItem().getType1()); // item type1
		writeD(item.getObjectId());
		writeD(item.getItemId());
		writeQ(item.getCount());
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeH(type2);	// item type2
		writeH(0x00);	// ?
		
		writeD(bodyPart);	// rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
		writeH(item.getEnchantLevel());	// enchant level
		writeH(0x00);
		writeH(item.getCustomType2());
		
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
	return _S__2E_TRADESTART;
}
}
