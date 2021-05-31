package net.sf.l2j.gameserver.network.clientpackets;
import java.util.Arrays;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public abstract class old_AbstractEnchantPacket extends L2GameClientPacket
{
public static final Map<Integer, EnchantScroll> _scrolls = new FastMap<Integer, EnchantScroll>();
public static final Map<Integer, EnchantItem> _supports = new FastMap<Integer, EnchantItem>();

public static final int ITEM_DESTROYED = 0;
public static final int ENCHANT_TO_4_OR_0 = 1;
public static final int ENCHANT_MINUS_ONE_OR_NEXT_LEVEL = 2;
public static final int REMAIN_SAME_ENCHANT = 3;
public static final int ENCHANT_TO_10_OR_6_OR_3_OR_0 = 4;
public static final int RETURNS_TO_0 = 5;
public static final int ENCHANT_TO_7_OR_3_OR_0 = 6;

public static class EnchantItem
{
protected final boolean _isWeapon;
protected final int _grade;
protected int _maxEnchantLevel = 20;
protected final int _chanceAdd;
protected final int[] _itemIds;

public EnchantItem(boolean wep, int type, int level, int chanceAdd, int[] items)
{
	_isWeapon = wep;
	_grade = type;
	_maxEnchantLevel = level;
	_chanceAdd = chanceAdd;
	_itemIds = items;
}

/*
 * Return true if support item can be used for this item
 */
public final boolean isValid(L2ItemInstance targetItem)
{
	if (targetItem == null)
		return false;
	
	final int type2 = targetItem.getItem().getType2();
	
	// checking scroll type and configured maximum enchant level
	switch (type2)
	{
	// weapon scrolls can enchant only weapons
	case L2Item.TYPE2_WEAPON:
		if (!_isWeapon /*|| (Config.ENCHANT_MAX_WEAPON > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_WEAPON)*/)
			return false;
		break;
		// armor scrolls can enchant only accessory and armors
	case L2Item.TYPE2_SHIELD_ARMOR:
		if (targetItem.getItemType() == L2ArmorType.SIGIL)
		{
			if (!_isWeapon)
				return false;
		}
		else if (_isWeapon /*|| (Config.ENCHANT_MAX_ARMOR > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_ARMOR)*/)
			return false;
		break;
	case L2Item.TYPE2_ACCESSORY:
		if (_isWeapon /*|| (Config.ENCHANT_MAX_JEWELRY > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_JEWELRY)*/)
			return false;
		break;
	default:
		return false;
	}
	
	// check for crystal types
	if (_grade != targetItem.getItem().getItemGradeSPlus())
		return false;
	
	if (targetItem.isTimeLimitedItem())
	{
		int max = 99;
		
		switch ((int)targetItem.getItem().getUniqueness())
		{
		case 0:
			break;
		case 1:
			max = 11;
			break;
		case 2:
			max = 8;
			break;
		case 3:
			max = 4;
			break;
		default:
			max = 0;
		}
		
		if (targetItem.getEnchantLevel() >= max)
			return false;
	}
	
	if (targetItem.isStarterItem())
	{
		int max = 15;
		
		switch ((int)targetItem.getItem().getUniqueness())
		{
		case 0:
			break;
		case 1:
			max = 14;
			break;
		case 2:
			max = 12;
			break;
		case 3:
			max = 11;
			break;
		case 4:
			if (targetItem.getItem().getUniqueness() == 4.5)
				max = 3;
			else
				max = 8;
			break;
		case 5:
			max = 0;
			break;
		}
		
		if (targetItem.getEnchantLevel() >= max)
		{
			targetItem.getActingPlayer().sendMessage("Your item is a starter item, and thus have reached its enchant limit");
			return false;
		}
	}
	
	// check for maximum enchant level
	if (_maxEnchantLevel != 0 && targetItem.getEnchantLevel() >= _maxEnchantLevel)
		return false;
	
	if(_itemIds != null && Arrays.binarySearch(_itemIds, targetItem.getItemId()) < 0)
		return false;
	
	return true;
}

public final int getChanceAdd()
{
	return _chanceAdd;
}
}

public static final class EnchantScroll extends EnchantItem
{
private final boolean _isForbidden;
private final boolean _isLegendary;
private final boolean _isBlessed;
private final boolean _isCrystal;
private final boolean _isSafe;
private final int _scrollLvl;

public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, int type, int level, int chance, int[] items)
{
	super(wep, type, level, chance, items);
	
	_isBlessed = bless;
	_isCrystal = crystal;
	_isSafe = safe;
	_isForbidden = false;
	_isLegendary = false;
	
	if (_isForbidden)
		_scrollLvl = 4;
	else if (_isLegendary)
		_scrollLvl = 3;
	else if (_isBlessed)
		_scrollLvl = 2;
	else if (_isCrystal)
		_scrollLvl = 1;
	else
		_scrollLvl = 0;
}

public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, boolean forbidden, boolean legendary, int type, int level, int chance, int[] items)
{
	super(wep, type, level, chance, items);
	
	_isBlessed = bless;
	_isCrystal = crystal;
	_isSafe = safe;
	_isForbidden = forbidden;
	_isLegendary = legendary;
	
	if (_isForbidden)
		_scrollLvl = 4;
	else if (_isLegendary)
		_scrollLvl = 3;
	else if (_isBlessed)
		_scrollLvl = 2;
	else if (_isCrystal)
		_scrollLvl = 1;
	else
		_scrollLvl = 0;
}

public final boolean isForbidden()
{
	return _isForbidden;
}

public final boolean isLegendary()
{
	return _isLegendary;
}

public final boolean isBlessed()
{
	return _isBlessed;
}

public final boolean isCrystal()
{
	return _isCrystal;
}

/*
 * Return true for safe-enchant scrolls (enchant level will remain on failure)
 */
public final boolean isSafe()
{
	return _isSafe;
}

public final int getMaxEnchantLevel(final L2ItemInstance enchantItem)
{
	return _maxEnchantLevel;
}

public final boolean isValid(L2ItemInstance enchantItem, EnchantItem supportItem)
{
	// blessed scrolls can't use support items
	if (supportItem != null && (!supportItem.isValid(enchantItem) || isBlessed()))
		return false;
	
	return isValid(enchantItem);
}

public final int getChance(final L2ItemInstance enchantItem, final EnchantItem supportItem)
{
	if (isBlessed() || isForbidden() || isLegendary())
	{
		if (supportItem != null)// blessed scrolls does not use support items
			return -200;
	}
	
	if (!isValid(enchantItem, supportItem))
		return -200;
	
	if (!enchantItem.isStandardShopItem() && enchantItem.isWeapon())
	{
		if (!enchantItem.isRaidbossItem()) //rares, epics
		{
			int chance = 40;
			
			switch (_scrollLvl)
			{
			case 0: //normal scrolls
				break;
			case 1: //crystal scrolls
				chance = 54;
				break;
			case 2: //blessed scrolls
				chance = 70;
				break;
			case 3: //legendary scrolls
				chance = 75;
				break;
			case 4: //forbidden
				chance = 80;
				break;
			default:
				System.out.println("LOL WTF get enchant chance has a 'default' scroll wtf lol");
				return -1;
			}
			
			switch (enchantItem.getEnchantLevel())
			{
			case 10:
				chance -= 3;
				break;
			case 11:
				chance -= 7;
				break;
			case 12:
				chance -= 10;
				break;
			case 13:
				chance -= 14;
				break;
			case 14:
				chance -= 17;
				break;
			case 15:
				chance -= 21;
				break;
			case 16:
				chance -= 25;
				break;
			case 17:
				chance -= 29;
				break;
			case 18:
				chance -= 34;
				break;
			case 19:
				chance -= 40;
				break;
			case 20:
				chance -= 50;
				break;
			}
			
			if (enchantItem.isAtOrOverMustBreakEnchantLevel())
			{
				chance -= 7;
				chance /= 2.5;
			}
			
			return chance;
		}
	}
	
	int chance = 0;
	
	switch (enchantItem.getEnchantLevel())
	{
	case 0:
	case 1:
	case 2:
		chance = 100;
		break;
	case 3:
		chance = 95;
		break;
	case 4:
		chance = 90;
		break;
	case 5:
		chance = 85;
		break;
	case 6:
		chance = 80;
		break;
	case 7:
		chance = 75;
		break;
	case 8:
		chance = 70;
		break;
	case 9:
		chance = 65;
		break;
	case 10:
		chance = 60;
		break;
	case 11:
		chance = 55;
		break;
	case 12:
		chance = 50;
		break;
	case 13:
		chance = 45;
		break;
	case 14:
		chance = 40;
		break;
	case 15:
		chance = 35;
		break;
	case 16:
		chance = 30;
		break;
	case 17:
		chance = 25;
		break;
	case 18:
		chance = 20;
		break;
	case 19:
		chance = 15;
		break;
	default:
		chance = 10;
	}
	
	if (enchantItem.getEnchantLevel() < 10 && enchantItem.getUniqueness() <= 4)
	{
		chance += 15;
	}
	else if (enchantItem.getEnchantLevel() < 12 && enchantItem.getUniqueness() <= 3) //vesper boost
	{
		chance += 10;
	}
	 if (enchantItem.getUniqueness() >= 4.5)
	{
		if (enchantItem.isStandardShopItem())
		{
			if (enchantItem.getEnchantLevel() >= 10)
				chance -= (enchantItem.getEnchantLevel()-9)*5;
		}
		else
		{
			if (enchantItem.getEnchantLevel() >= 10)
				chance -= (enchantItem.getEnchantLevel()-9)*5;
		}
	}
	else if (enchantItem.getUniqueness() >= 4)
	{
		if (enchantItem.isStandardShopItem())
		{
			if (enchantItem.getEnchantLevel() >= 11)
				chance -= 5;
		}
		else
		{
			if (enchantItem.getEnchantLevel() >= 10)
				chance -= 5;
		}
	}
	else if (enchantItem.getUniqueness() >= 3.5)
	{
		if (enchantItem.isStandardShopItem())
		{
			if (enchantItem.getEnchantLevel() >= 14)
				chance -= 5;
		}
		else
		{
			if (enchantItem.getEnchantLevel() >= 15)
				chance -= 10;
			else if (enchantItem.getEnchantLevel() >= 14)
				chance -= 5;
		}
	}
	else if (enchantItem.getUniqueness() >= 3)
	{
		if (enchantItem.isStandardShopItem())
		{
			if (enchantItem.getEnchantLevel() >= 18)
				chance -= 5;
		}
		else
		{
			if (enchantItem.getEnchantLevel() >= 16)
				chance -= 5;
		}
	}
	
	chance += _chanceAdd;
	
	if (supportItem != null)
		chance += supportItem.getChanceAdd();
	
	chance += enchantItem.getItem().getWeight();
	
	switch (_scrollLvl)
	{
	case 0: //normal scrolls
		chance += 10;
		if (enchantItem.getItem().getUniqueness() >= 3.5)
			chance -= 5;
		break;
	case 1: //crystal scrolls
		if (((enchantItem.getItem().getUniqueness() >= 3 && enchantItem.getEnchantLevel() >= 10) || enchantItem.getItem().getUniqueness() >= 4) && !enchantItem.isStandardShopItem())
			chance -= 5;
		break;
	case 2: //blessed scrolls
		chance += 30;
		break;
	case 3: //legendary scrolls
		chance += 40;
		if (enchantItem.isAtOrOverMustBreakEnchantLevel())
			chance -= 5;
		else if (enchantItem.getItem().getUniqueness() >= 4)
			chance += 5;
		break;
	case 4: //forbidden
		chance += 50;
		if (enchantItem.isAtOrOverMustBreakEnchantLevel())
			chance -= 5;
		else if (enchantItem.getItem().getUniqueness() >= 4)
			chance += 5;
		break;
	default :
		System.out.println("LOL WTF get enchant chance has a 'default' scroll wtf lol");
		return -1;
	}
	
	if (enchantItem.isAtOrOverMustBreakEnchantLevel())
	{
		chance -= 3;
		chance /= 2;
	}
	
	return chance;
}

/**
 * @param enchantItem - item to be enchanted
 * @return
 * 3 = item and enchant stay the same
 * 2 = enchant is subtracted by 1
 * 1 = enchant is set to 0
 * 0 = item is destroyed
 */
public final byte determineFateOfItemIfFail(final L2ItemInstance enchantItem)
{
	if (_isForbidden)
		return REMAIN_SAME_ENCHANT;
	
	/*	if (_isSafe)
		return REMAIN_SAME_ENCHANT;*/
	
	final float uniqueness = enchantItem.getItem().getUniqueness();
	
	if (_isLegendary)
	{
		switch ((int)uniqueness)
		{
		case 0:
			return REMAIN_SAME_ENCHANT;
		case 1:
			return REMAIN_SAME_ENCHANT;
		case 2:
			return REMAIN_SAME_ENCHANT;
		case 3:
			return REMAIN_SAME_ENCHANT;
		case 4:
			if (!enchantItem.isStandardShopItem())
				return ENCHANT_TO_10_OR_6_OR_3_OR_0;
			return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
		case 5:
			if (!enchantItem.isStandardShopItem())
				return ENCHANT_TO_10_OR_6_OR_3_OR_0;
			return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
		}
	}
	else if (_isBlessed)
	{
		if (!enchantItem.isStandardShopItem())
		{
			switch ((int)uniqueness)
			{
			case 5:
			case 4:
				if (enchantItem.getEnchantLevel() >= 6)
					return ITEM_DESTROYED;
				break;
			case 3:
				if (enchantItem.isRaidbossItem() && enchantItem.getEnchantLevel() >= 13)
					return ITEM_DESTROYED;
				else if (enchantItem.getEnchantLevel() >= 16)
					return ITEM_DESTROYED;
				break;
			default:
				if (enchantItem.getEnchantLevel() >= 15)
					return ITEM_DESTROYED;
			}
		}
		
		switch ((int)uniqueness)
		{
		case 0:
			return REMAIN_SAME_ENCHANT;
		case 1:
			return REMAIN_SAME_ENCHANT;
		case 2:
			return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
		case 3:
			return ENCHANT_TO_10_OR_6_OR_3_OR_0;
		case 4:
			if (uniqueness > 4 && enchantItem.getEnchantLevel() >= 11)
				return ITEM_DESTROYED;
			else if (uniqueness >= 4 && enchantItem.getEnchantLevel() >= 13)
				return ITEM_DESTROYED;
			else if (uniqueness == 4)
				return ENCHANT_TO_7_OR_3_OR_0;
			else
				return ENCHANT_TO_4_OR_0;
		case 5:
			return RETURNS_TO_0;
		}
	}
	else if (_isCrystal)
	{
		if (!enchantItem.isStandardShopItem())
		{
			if (uniqueness >= 4)
			{
				if (enchantItem.getEnchantLevel() >= 3)
					return ITEM_DESTROYED;
			}
			else if (enchantItem.getEnchantLevel() >= 9)
				return ITEM_DESTROYED;
		}
		
		switch ((int)uniqueness)
		{
		case 0:
			return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
		case 1:
			return ENCHANT_MINUS_ONE_OR_NEXT_LEVEL;
		case 2:
			if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel())
				return ITEM_DESTROYED;
			else
				return ENCHANT_TO_10_OR_6_OR_3_OR_0;
		case 3:
			if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel()-1)
				return ITEM_DESTROYED;
			else
				return ENCHANT_TO_4_OR_0;
		case 4:
			if (uniqueness > 4 && enchantItem.getEnchantLevel() >= 6)
				return ITEM_DESTROYED;
			else if (uniqueness >= 4 && enchantItem.getEnchantLevel() >= 12)
				return ITEM_DESTROYED;
			else
				return RETURNS_TO_0;
		case 5:
			return ITEM_DESTROYED;
		}
	}
	else //normal scrolls
	{
		if (!enchantItem.isStandardShopItem())
			return ITEM_DESTROYED;
		
		switch ((int)uniqueness)
		{
		case 0:
			return ENCHANT_TO_10_OR_6_OR_3_OR_0;
		case 1:
			return ENCHANT_TO_4_OR_0;
		case 2:
			return ITEM_DESTROYED;
		case 3:
			return ITEM_DESTROYED;
		case 4:
			return ITEM_DESTROYED;
		case 5:
			return ITEM_DESTROYED;
		}
	}
	
	return ITEM_DESTROYED;
}
}

static
{
	// itemId, (isWeapon, isBlessed, isCrystal, isSafe, grade, max enchant level, chance increase, allowed item IDs)
	// allowed items list must be sorted by ascending order
	
	// Titanium enchant scrolls
	_scrolls.put(99950, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_S, 11, 100, new int[]{90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90031,90032,90033,90034,90035}));
	_scrolls.put(99951, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_S, 11, 100, new int[]{13140,13141,13142,13435,13436,13437,13448,13449,13450,13451,13452,13453,13454,13455,13456,74000,74001
			,74002,74003,74004,74010,74011,74012,74013,74014,74020,74021,74022,74023,74024}));
	
	_scrolls.put(99952, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_S, 14, 100, new int[]{90018,90019,90020,90021,90022,90023,90024,90025,90026,90027,90028,90029,90030,90031,90032,90033,90034,90035}));
	_scrolls.put(99953, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_S, 14, 100, new int[]{13140,13141,13142,13435,13436,13437,13448,13449,13450,13451,13452,13453,13454,13455,13456,74000,74001
			,74002,74003,74004,74010,74011,74012,74013,74014,74020,74021,74022,74023,74024}));
	
	// Dread enchant scrolls
	_scrolls.put(99954, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_S, 10, 100, new int[]{90000,90001,90002,90003,90004,90005,90006,90007,90008,90009,90010,90011,90012,90013,90014,90015,90016,90017}));
	_scrolls.put(99955, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_S, 10, 100, new int[]{71000,71001,71002,71003,71004,71010,71011,71012,71013,71014,71020,71021,71022,71023,71024,
			72000,72001,72002,72003,72004,72010,72011,72012,72013,72014,72020,72021,72022,72023,72024}));
	
	_scrolls.put(729, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(730, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(731, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(732, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(947, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(948, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(949, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(950, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(951, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(952, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(953, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(954, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(955, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(956, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(957, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(958, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_D, 20, 0, null));
	
	_scrolls.put(959, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_S, 20, 0, null));
	_scrolls.put(960, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_S, 20, 0, null));
	_scrolls.put(961, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_S, 20, 0, null));
	_scrolls.put(962, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_S, 20, 0, null));
	
	_scrolls.put(6569, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(6570, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(6571, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(6572, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(6573, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(6574, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(6575, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(6576, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	
	_scrolls.put(6577, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_S, 20, 0, null));
	_scrolls.put(6578, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_S, 20, 0, null));
	
	_scrolls.put(22006, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(22007, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(22008, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(22009, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(22010, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_D, 20, 0, null));
	_scrolls.put(22011, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_C, 20, 0, null));
	_scrolls.put(22012, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_B, 20, 0, null));
	_scrolls.put(22013, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(22014, new EnchantScroll(true, false, false, false, false, true, L2Item.CRYSTAL_S, 25, 0, null));
	_scrolls.put(22015, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(22016, new EnchantScroll(false, false, false, false, false, true, L2Item.CRYSTAL_S, 25, 0, null));
	_scrolls.put(22017, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(22018, new EnchantScroll(true, false, false, false, true, false, L2Item.CRYSTAL_S, 25, 0, null));
	_scrolls.put(22019, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	_scrolls.put(22020, new EnchantScroll(false, false, false, false, true, false, L2Item.CRYSTAL_S, 25, 0, null));
	_scrolls.put(22021, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 20, 0, null));
	
	// Master Yogi's Scroll Enchant Weapon (event)
	_scrolls.put(13540, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_NONE, 0, 0, new int[]{ 13539 }));
	
	// itemId, (isWeapon, grade, max enchant level, chance increase)
	_supports.put(12362, new EnchantItem(true, L2Item.CRYSTAL_D, 15, 20, null));
	_supports.put(12363, new EnchantItem(true, L2Item.CRYSTAL_C, 15, 18, null));
	_supports.put(12364, new EnchantItem(true, L2Item.CRYSTAL_B, 15, 15, null));
	_supports.put(12365, new EnchantItem(true, L2Item.CRYSTAL_A, 15, 12, null));
	_supports.put(12366, new EnchantItem(true, L2Item.CRYSTAL_S, 15, 10, null));
	_supports.put(12367, new EnchantItem(false, L2Item.CRYSTAL_D, 15, 35, null));
	_supports.put(12368, new EnchantItem(false, L2Item.CRYSTAL_C, 15, 27, null));
	_supports.put(12369, new EnchantItem(false, L2Item.CRYSTAL_B, 15, 23, null));
	_supports.put(12370, new EnchantItem(false, L2Item.CRYSTAL_A, 15, 18, null));
	_supports.put(12371, new EnchantItem(false, L2Item.CRYSTAL_S, 15, 15, null));
	_supports.put(14702, new EnchantItem(true, L2Item.CRYSTAL_D, 15, 20, null));
	_supports.put(14703, new EnchantItem(true, L2Item.CRYSTAL_C, 15, 18, null));
	_supports.put(14704, new EnchantItem(true, L2Item.CRYSTAL_B, 15, 15, null));
	_supports.put(14705, new EnchantItem(true, L2Item.CRYSTAL_A, 15, 12, null));
	_supports.put(14706, new EnchantItem(true, L2Item.CRYSTAL_S, 15, 10, null));
	_supports.put(14707, new EnchantItem(false, L2Item.CRYSTAL_D, 15, 35, null));
	_supports.put(14708, new EnchantItem(false, L2Item.CRYSTAL_C, 15, 27, null));
	_supports.put(14709, new EnchantItem(false, L2Item.CRYSTAL_B, 15, 23, null));
	_supports.put(14710, new EnchantItem(false, L2Item.CRYSTAL_A, 15, 18, null));
	_supports.put(14711, new EnchantItem(false, L2Item.CRYSTAL_S, 15, 15, null));
	
	
}

/**
 * Return enchant template for scroll
 */
protected static final EnchantScroll getEnchantScroll(L2ItemInstance scroll)
{
	return _scrolls.get(scroll.getItemId());
}

/**
 * Return enchant template for support item
 */
protected static final EnchantItem getSupportItem(L2ItemInstance item)
{
	return _supports.get(item.getItemId());
}

/**
 * Return true if item can be enchanted
 */
protected static final boolean isEnchantable(L2ItemInstance item)
{
	if (item.isHeroItem())
		return false;
	if (item.isShadowItem())
		return false;
	if (item.isCommonItem())
		return false;
	if (item.isEtcItem())
		return false;
	if (item.isTimeLimitedItem() && item.getEnchantLevel() >= 18)
		return false;
	if (item.isWear())
		return false;
	// rods
	if (item.getItem().getItemType() == L2WeaponType.ROD)
		return false;
	// apprentice and travelers weapons
	if (item.getItemId() >= 7816 && item.getItemId() <= 7831)
		return false;
	// bracelets
	if (item.getItem().getBodyPart() == L2Item.SLOT_L_BRACELET)
		return false;
	if (item.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET)
		return false;
	if (item.getItem().getBodyPart() == L2Item.SLOT_BACK)
		return false;
	// only items in inventory and equipped can be enchanted
	if (item.getItemLocation() != L2ItemInstance.ItemLocation.INVENTORY && item.getItemLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
		return false;
/*	if (item.getItem().getType2() == L2Item.TYPE2_WEAPON && item.getEnchantLevel() >=13 && item.getUniqueness() == 3.5) //Uniques CAP +13
		return false;
	if (item.getItem().getType2() == L2Item.TYPE2_WEAPON && item.getEnchantLevel() >=12 && item.getUniqueness() == 3) //Titanium CAP +12
		return false;
	if (item.getItem().getType2() == L2Item.TYPE2_WEAPON && item.getEnchantLevel() >=10 && item.getUniqueness() >= 4) //Dread CAP +10
		return false;*/
	return true;
}
}