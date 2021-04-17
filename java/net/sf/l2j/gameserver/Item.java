/*
 * $Header: Item.java, 2/08/2005 00:49:12 luisantonioa Exp $
 * 
 * $Author: luisantonioa $ $Date: 2/08/2005 00:49:12 $ $Revision: 1 $ $Log:
 * Item.java,v $ Revision 1 2/08/2005 00:49:12 luisantonioa Added copyright
 * notice
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
package net.sf.l2j.gameserver;

import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class Item
{
	public int id;
	
    @SuppressWarnings("rawtypes")
	public Enum type;
	
	public String name;
	
	public StatsSet set;
	
	public int currentLevel;
	
	public L2Item item;
}
