/*
 * $Header: MultiSellList.java, 2/08/2005 14:21:01 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 2/08/2005 14:21:01 $
 * $Revision: 1 $
 * $Log: MultiSellList.java,v $
 * Revision 1  2/08/2005 14:21:01  luisantonioa
 * Added copyright notice
 *
 *
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

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellEntry;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellIngredient;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellListContainer;
import net.sf.l2j.gameserver.templates.item.L2Item;


/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public final class MultiSellList extends L2GameServerPacket
{
private static final String _S__D0_MULTISELLLIST = "[S] d0 MultiSellList";

protected int _listId, _page, _finished;
protected MultiSellListContainer _list;

public MultiSellList(MultiSellListContainer list, int page, int finished)
{
	_list = list;
	_listId = list.getListId();
	_page = page;
	_finished = finished;
}

@Override
protected void writeImpl()
{
	// [ddddd] [dchh] [hdhdh] [hhdh]
	
	writeC(0xd0);
	writeD(_listId);    // list id
	writeD(_page);		// page
	writeD(_finished);	// finished
	writeD(0x28);	// size of pages
	writeD(_list == null ? 0 : _list.getEntries().size()); //list length
	
	if(_list != null)
	{
		for(MultiSellEntry ent : _list.getEntries())
		{
			writeD(ent.getEntryId());
			writeC(1);
			writeH(0x00); // C6
			writeD(0x00); // C6
			writeD(0x00); // T1
			writeH(65534); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(0x00); // T1
			writeH(ent.getProducts().size());
			writeH(ent.getIngredients().size());
			
			for(MultiSellIngredient i: ent.getProducts())
			{
				int item = i.getItemId();
				int bodyPart = 0;
				int type2 = 65535;
				
				if (item > 0)
				{
					L2Item template = ItemTable.getInstance().getTemplate(item);
					if (template != null)
					{
						bodyPart = template.getBodyPart();
						type2 = template.getType2();
					}
				}
				
				writeD(item);
				writeD(bodyPart);
				writeH(type2);
				writeQ(i.getItemCount());
				writeH(i.getEnchantmentLevel()); //enchtant lvl
				writeD(i.getAugmentId()); // C6
				writeD(0x00); // mana
				writeH(i.getElementId()); // T1 element id
				writeH(i.getElementVal()); // T1 element power
				writeH(i.getFireVal()); // T1 fire
				writeH(i.getWaterVal()); // T1 water
				writeH(i.getWindVal()); // T1 wind
				writeH(i.getEarthVal()); // T1 earth
				writeH(i.getHolyVal()); // T1 holy
				writeH(i.getDarkVal()); // T1 dark
			}
			
			for(MultiSellIngredient i : ent.getIngredients())
			{
				int item = i.getItemId();
				int typeE = 65535;
				if (item > 0)
					typeE = ItemTable.getInstance().getTemplate(item).getType2();
				writeD(item);      //ID
				writeH(typeE);
				writeQ(i.getItemCount());	//Count
				writeH(i.getEnchantmentLevel()); //Enchant Level
				writeD(i.getAugmentId()); // C6
				writeD(0x00); // C6
				writeH(i.getElementId()); // T1
				writeH(i.getElementVal()); // T1
				writeH(i.getFireVal()); // T1
				writeH(i.getWaterVal()); // T1
				writeH(i.getWindVal()); // T1
				writeH(i.getEarthVal()); // T1
				writeH(i.getHolyVal()); // T1
				writeH(i.getDarkVal()); // T1
			}
		}
	}
}

@Override
public String getType()
{
	return _S__D0_MULTISELLLIST;
}

}
