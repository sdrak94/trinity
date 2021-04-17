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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WarehouseItem;

/**
 * 0x42 WarehouseWithdrawalList  dh (h dddhh dhhh d)
 * 
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/29 23:15:10 $
 */

public class SortedWareHouseWithdrawalList extends L2GameServerPacket
{
public static final int PRIVATE = 1;
public static final int CLAN = 2;
public static final int CASTLE = 3; //not sure
public static final int FREIGHT = 4; //not sure
private static Logger _log = Logger.getLogger(SortedWareHouseWithdrawalList.class.getName());
private static final String _S__54_SORTEDWAREHOUSEWITHDRAWALLIST = "[S] 42 SortedWareHouseWithdrawalList";
private L2PcInstance _activeChar;
private long _playerAdena;
private List<L2WarehouseItem> _objects = new FastList<L2WarehouseItem>();
private int _whType;
private byte _sortorder;
private WarehouseListType _itemtype;

public static enum WarehouseListType
{
WEAPON,
ARMOR,
ETCITEM,
MATERIAL,
RECIPE,
AMULETT,
SPELLBOOK,
SHOT,
SCROLL,
CONSUMABLE,
SEED,
POTION,
QUEST,
PET,
OTHER,
ALL
}

/** sort order A..Z */
public static final byte A2Z = 1;
/** sort order Z..A */
public static final byte Z2A = -1;
/** sort order Grade non..S */
public static final byte GRADE = 2;
/** sort order Recipe Level 1..9 */
public static final byte LEVEL = 3;
/** sort order type */
public static final byte TYPE = 4;
/** sort order body part (wearing) */
public static final byte WEAR = 5;
/** Maximum Items to put into list */
public static final int MAX_SORT_LIST_ITEMS = 300;

/**
 * This will instantiate the Warehouselist the Player asked for
 * @param player who calls for the itemlist
 * @param type is the Warehouse Type
 * @param itemtype is the Itemtype to sort for
 * @param sortorder is the integer Sortorder like 1 for A..Z (use public constant)
 */
public SortedWareHouseWithdrawalList(L2PcInstance player, int type, WarehouseListType itemtype, byte sortorder)
{
	_activeChar = player;
	_whType = type;
	_itemtype = itemtype;
	_sortorder = sortorder;
	
	_playerAdena = _activeChar.getAdena();
	if (_activeChar.getActiveWarehouse() == null)
	{
		// Something went wrong!
		_log.warning("error while sending withdraw request to: " + _activeChar.getName());
		return;
	}
	
	switch (_itemtype)
	{
	case WEAPON:
		_objects = createWeaponList(_activeChar.getActiveWarehouse().getItems());
		break;
	case ARMOR:
		_objects = createArmorList(_activeChar.getActiveWarehouse().getItems());
		break;
	case ETCITEM:
		_objects = createEtcItemList(_activeChar.getActiveWarehouse().getItems());
		break;
	case MATERIAL:
		_objects = createMatList(_activeChar.getActiveWarehouse().getItems());
		break;
	case RECIPE:
		_objects = createRecipeList(_activeChar.getActiveWarehouse().getItems());
		break;
	case AMULETT:
		_objects = createAmulettList(_activeChar.getActiveWarehouse().getItems());
		break;
	case SPELLBOOK:
		_objects = createSpellbookList(_activeChar.getActiveWarehouse().getItems());
		break;
	case CONSUMABLE:
		_objects = createConsumableList(_activeChar.getActiveWarehouse().getItems());
		break;
	case SHOT:
		_objects = createShotList(_activeChar.getActiveWarehouse().getItems());
		break;
	case SCROLL:
		_objects = createScrollList(_activeChar.getActiveWarehouse().getItems());
		break;
	case SEED:
		_objects = createSeedList(_activeChar.getActiveWarehouse().getItems());
		break;
	case OTHER:
		_objects = createOtherList(_activeChar.getActiveWarehouse().getItems());
		break;
	case ALL:
	default:
		_objects = createAllList(_activeChar.getActiveWarehouse().getItems());
		break;
	}
	
	try
	{
		switch (_sortorder)
		{
		case A2Z:
		case Z2A:
			Collections.sort(_objects, new WarehouseItemNameComparator(_sortorder));
			break;
		case GRADE:
			if (_itemtype == WarehouseListType.ARMOR || _itemtype == WarehouseListType.WEAPON)
			{
				Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
				Collections.sort(_objects, new WarehouseItemGradeComparator(A2Z));
			}
			break;
		case LEVEL:
			if (_itemtype == WarehouseListType.RECIPE)
			{
				Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
				Collections.sort(_objects, new WarehouseItemRecipeComparator(A2Z));
			}
			break;
		case TYPE:
			if (_itemtype == WarehouseListType.MATERIAL)
			{
				Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
				Collections.sort(_objects, new WarehouseItemTypeComparator(A2Z));
			}
			break;
		case WEAR:
			if (_itemtype == WarehouseListType.ARMOR)
			{
				Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
				Collections.sort(_objects, new WarehouseItemBodypartComparator(A2Z));
			}
			break;
		}
	}
	catch (Exception e)
	{
	}
}

/**
 * This public method return the integer of the Sortorder by its name.
 * If you want to have another, add the Comparator and the Constant.
 * @param order
 * @return the integer of the sortorder or 1 as default value
 */
public static byte getOrder(String order)
{
	if (order == null)
		return A2Z;
	else if (order.startsWith("A2Z"))
		return A2Z;
	else if (order.startsWith("Z2A"))
		return Z2A;
	else if (order.startsWith("GRADE"))
		return GRADE;
	else if (order.startsWith("TYPE"))
		return TYPE;
	else if (order.startsWith("WEAR"))
		return WEAR;
	else
	{
		try
		{
			return Byte.parseByte(order);
		}
		catch (NumberFormatException ex)
		{
			return A2Z;
		}
	}
}

/**
 * This is the common Comparator to sort the items by Name
 */
private static class WarehouseItemNameComparator implements Comparator<L2WarehouseItem>
{
private byte order = 0;

protected WarehouseItemNameComparator(byte sortOrder)
{
	order = sortOrder;
}

public int compare(L2WarehouseItem o1, L2WarehouseItem o2)
{
	if (o1.getType2() == L2Item.TYPE2_MONEY && o2.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? Z2A : A2Z);
	if (o2.getType2() == L2Item.TYPE2_MONEY && o1.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? A2Z : Z2A);
	String s1 = o1.getItemName();
	String s2 = o2.getItemName();
	return (order == A2Z ? s1.compareTo(s2) : s2.compareTo(s1));
}
}

/**
 * This Comparator is used to sort by Recipe Level
 */
private static class WarehouseItemRecipeComparator implements Comparator<L2WarehouseItem>
{
private int order = 0;

private RecipeController rc = null;

protected WarehouseItemRecipeComparator(int sortOrder)
{
	order = sortOrder;
	rc = RecipeController.getInstance();
}

public int compare(L2WarehouseItem o1, L2WarehouseItem o2)
{
	if (o1.getType2() == L2Item.TYPE2_MONEY && o2.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? Z2A : A2Z);
	if (o2.getType2() == L2Item.TYPE2_MONEY && o1.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? A2Z : Z2A);
	if ((o1.isEtcItem() && o1.getItemType() == L2EtcItemType.RECEIPE) && (o2.isEtcItem() && o2.getItemType() == L2EtcItemType.RECEIPE))
	{
		try
		{
			L2RecipeList rp1 = rc.getRecipeByItemId(o1.getItemId());
			L2RecipeList rp2 = rc.getRecipeByItemId(o2.getItemId());
			
			if (rp1 == null)
				return (order == A2Z ? A2Z : Z2A);
			if (rp2 == null)
				return (order == A2Z ? Z2A : A2Z);
			
			Integer i1 = rp1.getLevel();
			Integer i2 = rp2.getLevel();
			
			return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	else
	{
		String s1 = o1.getItemName();
		String s2 = o2.getItemName();
		return (order == A2Z ? s1.compareTo(s2) : s2.compareTo(s1));
	}
}
}

/**
 * This Comparator is used to sort the Items by BodyPart
 */
private static class WarehouseItemBodypartComparator implements Comparator<L2WarehouseItem>
{
private byte order = 0;

protected WarehouseItemBodypartComparator(byte sortOrder)
{
	order = sortOrder;
}

public int compare(L2WarehouseItem o1, L2WarehouseItem o2)
{
	if (o1.getType2() == L2Item.TYPE2_MONEY && o2.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? Z2A : A2Z);
	if (o2.getType2() == L2Item.TYPE2_MONEY && o1.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? A2Z : Z2A);
	Integer i1 = o1.getBodyPart();
	Integer i2 = o2.getBodyPart();
	return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
}
}

/**
 * This Comparator is used to sort by the Item Grade (e.g. Non..S-Grade)
 */
private static class WarehouseItemGradeComparator implements Comparator<L2WarehouseItem>
{
byte order = 0;

protected WarehouseItemGradeComparator(byte sortOrder)
{
	order = sortOrder;
}

public int compare(L2WarehouseItem o1, L2WarehouseItem o2)
{
	if (o1.getType2() == L2Item.TYPE2_MONEY && o2.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? Z2A : A2Z);
	if (o2.getType2() == L2Item.TYPE2_MONEY && o1.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? A2Z : Z2A);
	Integer i1 = o1.getItemGrade();
	Integer i2 = o2.getItemGrade();
	return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
}
}

/**
 * This Comparator will sort by Item Type. Unfortunatly this will only have a
 * good result if the Database Table for the ETCITEM.TYPE column is fixed!
 */
private static class WarehouseItemTypeComparator implements Comparator<L2WarehouseItem>
{
byte order = 0;

protected WarehouseItemTypeComparator(byte sortOrder)
{
	order = sortOrder;
}

public int compare(L2WarehouseItem o1, L2WarehouseItem o2)
{
	if (o1.getType2() == L2Item.TYPE2_MONEY && o2.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? Z2A : A2Z);
	if (o2.getType2() == L2Item.TYPE2_MONEY && o1.getType2() != L2Item.TYPE2_MONEY)
		return (order == A2Z ? A2Z : Z2A);
	try
	{
		Integer i1 = o1.getItem().getMaterialType();
		Integer i2 = o2.getItem().getMaterialType();
		return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
	}
	catch (Exception e)
	{
		return 0;
	}
}
}

// ========================================================================

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Weapon</li>
 * <li>Arrow</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createWeaponList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isWeapon() || item.getItem().getType2() == L2Item.TYPE2_WEAPON || (item.isEtcItem() && item.getItemType() == L2EtcItemType.ARROW) || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Armor</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createArmorList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isArmor() || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Everything which is no Weapon/Armor</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createEtcItemList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Materials</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createMatList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && item.getEtcItem().getItemType() == L2EtcItemType.MATERIAL || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Recipes</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createRecipeList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && item.getEtcItem().getItemType() == L2EtcItemType.RECEIPE || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Amulett</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createAmulettList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && (item.getEtcItem().getItemType() == L2EtcItemType.SPELLBOOK && item.getItemName().toUpperCase().startsWith("AMULET")) || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Spellbook & Dwarven Drafts</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createSpellbookList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && (item.getEtcItem().getItemType() == L2EtcItemType.SPELLBOOK && !item.getItemName().toUpperCase().startsWith("AMULET")) || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Consumables (Potions, Shots, ...)</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createConsumableList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && (item.getEtcItem().getItemType() == L2EtcItemType.SCROLL || item.getEtcItem().getItemType() == L2EtcItemType.SHOT) || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Shots</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createShotList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && item.getEtcItem().getItemType() == L2EtcItemType.SHOT || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Scrolls/Potions</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createScrollList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && item.getEtcItem().getItemType() == L2EtcItemType.SCROLL || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Seeds</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createSeedList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem() && item.getEtcItem().getItemType() == L2EtcItemType.SEED || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>Everything which is no Weapon/Armor, Material, Recipe, Spellbook, Scroll or Shot</li>
 * <li>Money</li>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createOtherList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (item.isEtcItem()
				&& (item.getEtcItem().getItemType() != L2EtcItemType.MATERIAL && item.getEtcItem().getItemType() != L2EtcItemType.RECEIPE && item.getEtcItem().getItemType() != L2EtcItemType.SPELLBOOK
						&& item.getEtcItem().getItemType() != L2EtcItemType.SCROLL && item.getEtcItem().getItemType() != L2EtcItemType.SHOT) || item.getItem().getType2() == L2Item.TYPE2_MONEY)
		{
			if (_list.size() < MAX_SORT_LIST_ITEMS)
				_list.add(new L2WarehouseItem(item));
			else
				continue;
		}
	}
	return _list;
}

/**
 * This method is used to limit the given Warehouse List to:
 * <li>no limit</li>
 * This may sound strange but we return the given Array as a List<L2WarehouseItem>
 * @param _items complete Warehouse List
 * @return limited Item List
 */
private List<L2WarehouseItem> createAllList(L2ItemInstance[] _items)
{
	List<L2WarehouseItem> _list = new FastList<L2WarehouseItem>();
	for (L2ItemInstance item : _items)
	{
		if (_list.size() < MAX_SORT_LIST_ITEMS)
			_list.add(new L2WarehouseItem(item));
		else
			continue;
	}
	return _list;
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
	writeH(_objects.size());
	
	for (L2WarehouseItem item : _objects)
	{
		writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
		writeD(item.getObjectId());
		writeD(item.getItemId()); //unconfirmed, works
		writeQ(item.getCount()); //unconfirmed, works
		
		int type2 = item.getItem().getType2();
		int bodyPart = item.getItem().getBodyPart();
		
		if (bodyPart == L2Item.SLOT_BELT)
			type2 = L2Item.TYPE2_ACCESSORY;
		else if (item.getItemId() == 20325) //plastic hair
			type2 = L2Item.TYPE2_ACCESSORY;
		
		writeH(type2); // item type2 //unconfirmed, works
		writeH(item.getCustomType1()); // ?
		writeD(bodyPart); // ?
		writeH(item.getEnchantLevel()); // enchant level -confirmed
		writeH(item.getCustomType2()); // ?
		writeH(0x00); // ?
		writeD(item.getObjectId()); // item id - confimed
		if (item.isAugmented())
		{
			writeD(0x0000FFFF & item.getAugmentationId());
			writeD(item.getAugmentationId() >> 16);
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
		writeD(item.getTime());
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__54_SORTEDWAREHOUSEWITHDRAWALLIST;
}
}
