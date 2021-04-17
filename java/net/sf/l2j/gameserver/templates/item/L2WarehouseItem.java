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
package net.sf.l2j.gameserver.templates.item;

import net.sf.l2j.gameserver.model.L2ItemInstance;

/**
 * This class contains L2ItemInstance<BR>
 * Use to sort L2ItemInstance of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI> 
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public class L2WarehouseItem
{
	private L2Item _item;
	private int _object;
	private long _count;
	private int _owner;
	private int _enchant;
	private int _grade;
	private boolean _isAugmented;
	private int _augmentationId;
	private int _customType1;
	private int _customType2;
	private int _mana;
	
	private int _elemAtkType = -2;
	private int _elemAtkPower = 0;
	private int[] _elemDefAttr =
	{
		0, 0, 0, 0, 0, 0
	};
	private int _time;
	
	public L2WarehouseItem(L2ItemInstance item)
	{
		_item = item.getItem();
		_object = item.getObjectId();
		_count = item.getCount();
		_owner = item.getOwnerId();
		_enchant = item.getEnchantLevel();
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
		_grade = item.getItem().getItemGrade();
		if (item.isAugmented())
		{
			_isAugmented = true;
			_augmentationId = item.getAugmentation().getAugmentationId();
		}
		else
			_isAugmented = false;
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime()/1000) : -1;
		
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
			_elemDefAttr[i] = item.getElementDefAttr(i);
	}
	
	/**
	* Returns the item.
	* @return L2Item
	*/
	public L2Item getItem()
	{
		return _item;
	}
	
	/**
	 * Returns the unique objectId
	 * @return int
	 */
	public final int getObjectId()
	{
		return _object;
	}
	
	/**
	 * Returns the owner
	 * @return int
	 */
	public final int getOwnerId()
	{
		return _owner;
	}
	
	/**
	 * Returns the count
	 * @return int
	 */
	public final long getCount()
	{
		return _count;
	}
	
	/**
	 * Returns the first type
	 * @return int
	 */
	public final int getType1()
	{
		return _item.getType1();
	}
	
	/**
	 * Returns the second type
	 * @return int
	 */
	public final int getType2()
	{
		return _item.getType2();
	}
	
	/**
	 * Returns the second type
	 * @return int
	 */

	@SuppressWarnings("rawtypes")
	public final Enum getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * Returns the ItemId
	 * @return int
	 */
	public final int getItemId()
	{
		return _item.getItemId();
	}
	
	/**
	 * Returns the part of body used with this item
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _item.getBodyPart();
	}
	
	/**
	 * Returns the enchant level
	 * @return int
	 */
	public final int getEnchantLevel()
	{
		return _enchant;
	}
	
	/**
	 * Returns the item grade
	 * @return int
	 */
	public final int getItemGrade()
	{
		return _grade;
	}
	
	/**
	 * Returns true if it is a weapon
	 * @return boolean
	 */
	public final boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}
	
	/**
	 * Returns true if it is an armor
	 * @return boolean
	 */
	public final boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}
	
	/**
	 * Returns true if it is an EtcItem
	 * @return boolean
	 */
	public final boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}
	
	/**
	 * Returns the name of the item
	 * @return String
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	public boolean isAugmented()
	{
		return _isAugmented;
	}
	
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	/**
	 * Returns the name of the item
	 * @return String
	 * @deprecated beware to use getItemName() instead because getName() is final in L2Object and could not be overridden! Allover L2Object.getName() may return null!
	 */
	public String getName()
	{
		return _item.getName();
	}
	
	public final int getCustomType1()
	{
		return _customType1;
	}
	
	public final int getCustomType2()
	{
		return _customType2;
	}
	
	public final int getMana()
	{
		return _mana;
	}
	
	public int getAttackElementType()
	{
		return _elemAtkType;
	}
	
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	public int getElementDefAttr(byte i)
	{
		return _elemDefAttr[i];
	}
	
	public int getTime()
	{
		return _time;
	}
	/**
	 * Returns the name of the item
	 * @return String
	 */
	public String toString()
	{
		return _item.toString();
	}
}
