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

import java.util.Collection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.L2TradeList.L2TradeItem;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class ShopPreviewList extends L2GameServerPacket
{
	private static final String _S__EF_WEARLIST = "[S] f5 WearList";
	private int _listId;
    private Collection<L2TradeItem> _list;
	private long _money;
	private int _expertise;

	public ShopPreviewList(L2TradeList list, long currentMoney, int expertiseIndex)
	{
		_listId = list.getListId();
		_list = list.getItems();
		_money = currentMoney;
		_expertise = expertiseIndex;
	}

	public ShopPreviewList(Collection<L2TradeItem> lst, int listId, long currentMoney)
	{
		_listId = listId;
		_list = lst;
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf5);
		writeC(0xc0);	// ?
		writeC(0x13);	// ?
		writeC(0x00);	// ?
		writeC(0x00);	// ?
		writeQ(_money);		// current money
		writeD(_listId);

		int newlength = 0;
		for (L2TradeItem item : _list)
        {
		    if (item.getTemplate().getCrystalType() <= _expertise && item.getTemplate().isEquipable())
			newlength++;
		}
		writeH(newlength);

		for (L2TradeItem item : _list)
		{
		    if (item.getTemplate().getCrystalType() <= _expertise && item.getTemplate().isEquipable())
		    {
		        writeD(item.getItemId());
		        writeH(item.getTemplate().getType2());	// item type2
		        
		        if (item.getTemplate().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
		        {
		            writeH(item.getTemplate().getBodyPart());	// rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
		        }
		        else
		        {
		            writeH(0x00);	// rev 415  slot    0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
		        }
		        
		        writeQ(Config.WEAR_PRICE);
		    }
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__EF_WEARLIST;
	}
}
