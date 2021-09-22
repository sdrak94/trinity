package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public abstract class AbstractEnchantPacket extends L2GameClientPacket
{
	public static final Map<Integer, EnchantScroll>	_scrolls						= new HashMap<>();
	public static final Map<Integer, EnchantItem>	_supports						= new HashMap<>();
	public static final int							ITEM_DESTROYED					= 0;
	public static final int							ENCHANT_TO_4_OR_0				= 1;
	public static final int							ENCHANT_MINUS_ONE_OR_NEXT_LEVEL	= 2;
	public static final int							REMAIN_SAME_ENCHANT				= 3;
	public static final int							ENCHANT_TO_10_OR_6_OR_3_OR_0	= 4;
	public static final int							RETURNS_TO_0					= 5;
	public static final int							ENCHANT_TO_7_OR_3_OR_0			= 6;
	
	public static class EnchantItem
	{
		protected final boolean	_isWeapon;
		protected final int		_grade;
		protected int			_maxEnchantLevel	= 20;
		protected final int		_chanceAdd;
		protected final int[]	_itemIds;
		
		public EnchantItem(boolean wep, int type, int level, int chanceAdd, int[] items)
		{
			_isWeapon = wep;
			_grade = type;
			_maxEnchantLevel = level;
			_chanceAdd = chanceAdd;
			_itemIds = items;
		}
		
		public final boolean isValid(L2ItemInstance targetItem)
		{
			if (targetItem == null)
			{
				return false;
			}
			int type2 = targetItem.getItem().getType2();
			switch (type2)
			{
				case 0:
					if (!_isWeapon)
					{
						return false;
					}
					break;
				case 1:
					if (targetItem.getItemType() == L2ArmorType.SIGIL || targetItem.getItemId() == 83034)
					{
						if (!_isWeapon)
							return false;
						break;
					}
					if (_isWeapon)
						return false;
					break;
				case 2:
					if (_isWeapon)
						return false;
					break;
				default:
					return false;
			}
			if (_grade != targetItem.getItem().getItemGradeSPlus())
			{
				return false;
			}
			if (targetItem.isTimeLimitedItem())
			{
				int max = 99;
				switch ((int) targetItem.getItem().getUniqueness())
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
						break;
				}
				if (targetItem.getEnchantLevel() >= max)
				{
					return false;
				}
			}
			if (targetItem.isStarterItem())
			{
				int max = 15;
				switch ((int) targetItem.getItem().getUniqueness())
				{
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
						if (targetItem.getItem().getUniqueness() == 4.5D)
						{
							max = 3;
							break;
						}
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
			if (_maxEnchantLevel != 0 && targetItem.getEnchantLevel() >= _maxEnchantLevel)
			{
				return false;
			}
			if (_itemIds != null && Arrays.binarySearch(_itemIds, targetItem.getItemId()) < 0)
			{
				return false;
			}
			return true;
		}
		
		public final int getChanceAdd()
		{
			return _chanceAdd;
		}
	}
	
	public static final class EnchantScroll extends EnchantItem
	{
		private final boolean	_isForbidden;
		private final boolean	_isLegendary;
		private final boolean	_isBlessed;
		private final boolean	_isCrystal;
		private final boolean	_isSafe;
		private final int		_scrollLvl;
		
		public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, int type, int level, int chance, int[] items)
		{
			super(wep, type, level, chance, items);
			_isBlessed = bless;
			_isCrystal = crystal;
			_isSafe = safe;
			_isForbidden = false;
			_isLegendary = false;
			if (_isForbidden)
			{
				_scrollLvl = 4;
			}
			else if (_isLegendary)
			{
				_scrollLvl = 3;
			}
			else if (_isBlessed)
			{
				_scrollLvl = 2;
			}
			else if (_isCrystal)
			{
				_scrollLvl = 1;
			}
			else
			{
				_scrollLvl = 0;
			}
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
			{
				_scrollLvl = 4;
			}
			else if (_isLegendary)
			{
				_scrollLvl = 3;
			}
			else if (_isBlessed)
			{
				_scrollLvl = 2;
			}
			else if (_isCrystal)
			{
				_scrollLvl = 1;
			}
			else
			{
				_scrollLvl = 0;
			}
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
		
		public final boolean isSafe()
		{
			return _isSafe;
		}
		
		public final int getMaxEnchantLevel(L2ItemInstance enchantItem)
		{
			return _maxEnchantLevel;
		}
		
		public final boolean isValid(L2ItemInstance enchantItem, AbstractEnchantPacket.EnchantItem supportItem)
		{
			if (supportItem != null && (!supportItem.isValid(enchantItem) || isBlessed()))
			{
				return false;
			}
			return isValid(enchantItem);
		}
		
		public final int getChance(L2ItemInstance enchantItem, AbstractEnchantPacket.EnchantItem supportItem)
		{
			if (isBlessed() || isForbidden() || isLegendary())
			{
				if (supportItem != null)
				{
					return -200;
				}
			}
			if (!isValid(enchantItem, supportItem))
			{
				return -200;
			}
			if (!enchantItem.isStandardShopItem() && enchantItem.isWeapon())
			{
				if (!enchantItem.isRaidbossItem())
				{
					int chance = 40;
					switch (_scrollLvl)
					{
						case 0:
							break;
						case 1:
							chance = 54;
							break;
						case 2:
							chance = 70;
							break;
						case 3:
							chance = 75;
							break;
						case 4:
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
					if(enchantItem.getWeaponItem().isPvpWeapon())
					{
						if (enchantItem.getEnchantLevel() >= 18)
						{
							chance -= 25;
						}
						else if (enchantItem.getEnchantLevel() >= 16)
						{
							chance -= 15;
						}
					}
					if (enchantItem.isAtOrOverMustBreakEnchantLevel())
					{
						chance -= 7;
						chance = (int) (chance / 2.5D);
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
					break;
			}
			if (enchantItem.getEnchantLevel() < 10 && enchantItem.getUniqueness() <= 4.0F)
			{
				chance += 15;
			}
			else if (enchantItem.getEnchantLevel() < 12 && enchantItem.getUniqueness() <= 3.0F)
			{
				chance += 10;
			}
			else if (enchantItem.getUniqueness() >= 4.5D)
			{
				if (enchantItem.isStandardShopItem())
				{
					if (enchantItem.getEnchantLevel() >= 10)
					{
						chance -= (enchantItem.getEnchantLevel() - 9) * 5;
					}
				}
				else if (enchantItem.getEnchantLevel() >= 10)
				{
					chance -= (enchantItem.getEnchantLevel() - 9) * 5;
				}
			}
			else if (enchantItem.getUniqueness() >= 4.0F)
			{
				if (enchantItem.isStandardShopItem())
				{
					if (enchantItem.getEnchantLevel() >= 11)
					{
						chance -= 5;
					}
				}
				else if (enchantItem.getEnchantLevel() >= 10)
				{
					chance -= 5;
				}
			}
			else if (enchantItem.getUniqueness() >= 3.5D)
			{
				if (enchantItem.isStandardShopItem())
				{
					if (enchantItem.getEnchantLevel() >= 14)
					{
						chance -= 5;
					}
				}
				else if (enchantItem.getEnchantLevel() >= 15)
				{
					chance -= 10;
				}
				else if (enchantItem.getEnchantLevel() >= 14)
				{
					chance -= 5;
				}
			}
			else if (enchantItem.getUniqueness() >= 3.0F)
			{
				if (enchantItem.isStandardShopItem())
				{
					if (enchantItem.getEnchantLevel() >= 18)
					{
						chance -= 5;
					}
				}
				else if (enchantItem.getEnchantLevel() >= 16)
				{
					chance -= 5;
				}
			}
			chance += _chanceAdd;
			if (supportItem != null)
			{
				chance += supportItem.getChanceAdd();
			}
			chance += enchantItem.getItem().getWeight();
			switch (_scrollLvl)
			{
				case 0:
					chance += 10;
					if (enchantItem.getItem().getUniqueness() >= 3.5D)
						chance -= 5;
					break;
				case 1:
					if (((enchantItem.getItem().getUniqueness() >= 3.0F && enchantItem.getEnchantLevel() >= 10) || enchantItem.getItem().getUniqueness() >= 4.0F) && !enchantItem.isStandardShopItem())
						chance -= 5;
					break;
				case 2:
					chance += 30;
					break;
				case 3:
					chance += 40;
					if (enchantItem.isAtOrOverMustBreakEnchantLevel())
					{
						chance -= 5;
						break;
					}
					if (enchantItem.getItem().getUniqueness() >= 4.0F)
						chance += 5;
					break;
				case 4:
					chance += 50;
					if (enchantItem.isAtOrOverMustBreakEnchantLevel())
					{
						chance -= 5;
						break;
					}
					if (enchantItem.getItem().getUniqueness() >= 4.0F)
						chance += 5;
					break;
				default:
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
		
		public final byte determineFateOfItemIfFail(L2ItemInstance enchantItem)
		{
			if (_isForbidden)
			{
				return 3;
			}
			if (_isSafe)
			{
				return 3;
			}
			float uniqueness = enchantItem.getItem().getUniqueness();
			if (_isLegendary)
			{
				switch ((int) uniqueness)
				{
					case 0:
						return 3;
					case 1:
						return 3;
					case 2:
						return 3;
					case 3:
						return 3;
					case 4:
						if (!enchantItem.isStandardShopItem())
							return 4;
						return 2;
					case 5:
						if (!enchantItem.isStandardShopItem())
							return 4;
						return 2;
				}
			}
			else if (_isBlessed)
			{
				if (!enchantItem.isStandardShopItem())
				{
					switch ((int) uniqueness)
					{
						case 4:
						case 5:
							if (enchantItem.getEnchantLevel() >= 6)
								return 0;
							break;
						case 3:
							if (enchantItem.isRaidbossItem() && enchantItem.getEnchantLevel() >= 13)
								return 0;
							if (enchantItem.getEnchantLevel() >= 16)
								return 0;
							break;
						default:
							if (enchantItem.getEnchantLevel() >= 15)
								return 0;
							break;
					}
				}
				switch ((int) uniqueness)
				{
					case 0:
						return 3;
					case 1:
						return 3;
					case 2:
						return 2;
					case 3:
						return 4;
					case 4:
						if (uniqueness > 4.0F && enchantItem.getEnchantLevel() >= 11)
							return 0;
						if (uniqueness >= 4.0F && enchantItem.getEnchantLevel() >= 13)
							return 0;
						if (uniqueness == 4.0F)
						{
							return 6;
						}
						return 1;
					case 5:
						return 5;
				}
			}
			else if (_isCrystal)
			{
				if (!enchantItem.isStandardShopItem())
				{
					if (uniqueness >= 4.0F)
					{
						if (enchantItem.getEnchantLevel() >= 3)
						{
							return 0;
						}
					}
					else if (enchantItem.getEnchantLevel() >= 9)
					{
						return 0;
					}
				}
				switch ((int) uniqueness)
				{
					case 0:
						return 2;
					case 1:
						return 2;
					case 2:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel())
						{
							return 0;
						}
						return 4;
					case 3:
						if (enchantItem.getEnchantLevel() >= enchantItem.getItem().getClutchEnchantLevel() - 1)
						{
							return 0;
						}
						return 1;
					case 4:
						if (uniqueness > 4.0F && enchantItem.getEnchantLevel() >= 6)
							return 0;
						if (uniqueness >= 4.0F && enchantItem.getEnchantLevel() >= 12)
						{
							return 0;
						}
						return 5;
					case 5:
						return 0;
				}
			}
			else
			{
				if (!enchantItem.isStandardShopItem())
				{
					return 0;
				}
				switch ((int) uniqueness)
				{
					case 0:
						return 4;
					case 1:
						return 1;
					case 2:
						return 0;
					case 3:
						return 0;
					case 4:
						return 0;
					case 5:
						return 0;
				}
			}
			return 0;
		}
	}
	
	static
	{
		_scrolls.put(Integer.valueOf(99950), new EnchantScroll(true, false, false, true, 5, 11, 100, new int[]
		{
			90018, 90019, 90020, 90021, 90022, 90023, 90024, 90025, 90026, 90027, 90028, 90029, 90030, 90031, 90032, 90033, 90034, 90035
		}));
		_scrolls.put(Integer.valueOf(99951), new EnchantScroll(false, false, false, true, 5, 11, 100, new int[]
		{
			13140, 13141, 13142, 13435, 13436, 13437, 13448, 13449, 13450, 13451, 13452, 13453, 13454, 13455, 13456, 74000, 74001, 74002, 74003, 74004, 74010, 74011, 74012, 74013, 74014, 74020, 74021, 74022, 74023, 74024, 90035
		}));
		_scrolls.put(Integer.valueOf(99952), new EnchantScroll(true, false, false, true, 5, 14, 100, new int[]
		{
			90018, 90019, 90020, 90021, 90022, 90023, 90024, 90025, 90026, 90027, 90028, 90029, 90030, 90031, 90032, 90033, 90034, 90035
		}));
		_scrolls.put(Integer.valueOf(99953), new EnchantScroll(false, false, false, true, 5, 14, 100, new int[]
		{
			13140, 13141, 13142, 13435, 13436, 13437, 13448, 13449, 13450, 13451, 13452, 13453, 13454, 13455, 13456, 74000, 74001, 74002, 74003, 74004, 74010, 74011, 74012, 74013, 74014, 74020, 74021, 74022, 74023, 74024, 90035
		}));
		_scrolls.put(Integer.valueOf(99954), new EnchantScroll(true, false, false, true, 5, 10, 100, new int[]
		{
			90000, 90001, 90002, 90003, 90004, 90005, 90006, 90007, 90008, 90009, 90010, 90011, 90012, 90013, 90014, 90015, 90016, 90017, 999111
		}));
		_scrolls.put(Integer.valueOf(99955), new EnchantScroll(false, false, false, true, 5, 10, 100, new int[]
		{
			71000, 71001, 71002, 71003, 71004, 71010, 71011, 71012, 71013, 71014, 71020, 71021, 71022, 71023, 71024, 72000, 72001, 72002, 72003, 72004, 72010, 72011, 72012, 72013, 72014, 72020, 72021, 72022, 72023, 72024
		}));
		_scrolls.put(Integer.valueOf(99956), new EnchantScroll(true, false, false, true, 5, 20, 100, new int[]
		{
			83020, 83021, 83022, 83023, 83024, 83025, 83026, 83027, 83028, 83029, 83030, 83031, 83032, 83033, 83034, 83035, 83036, 83037
		}));
		_scrolls.put(Integer.valueOf(800300), new EnchantScroll(true, false, false, true, 5, 15, 100, new int[]
		{
		 	8206, 91000, 91001, 91002, 91003, 91004, 91005, 91006, 91007, 91008, 91009, 91010, 91011, 91012, 91013, 91014, 91015, 91016, 91017, 91019, 91023, 91024, 91027, 91028, 91029
		}));
		_scrolls.put(Integer.valueOf(729), new EnchantScroll(true, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(730), new EnchantScroll(false, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(731), new EnchantScroll(true, false, true, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(732), new EnchantScroll(false, false, true, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(947), new EnchantScroll(true, false, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(948), new EnchantScroll(false, false, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(949), new EnchantScroll(true, false, true, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(950), new EnchantScroll(false, false, true, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(951), new EnchantScroll(true, false, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(952), new EnchantScroll(false, false, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(953), new EnchantScroll(true, false, true, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(954), new EnchantScroll(false, false, true, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(955), new EnchantScroll(true, false, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(956), new EnchantScroll(false, false, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(957), new EnchantScroll(true, false, true, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(958), new EnchantScroll(false, false, true, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(959), new EnchantScroll(true, false, false, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(960), new EnchantScroll(false, false, false, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(961), new EnchantScroll(true, false, true, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(962), new EnchantScroll(false, false, true, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(6569), new EnchantScroll(true, true, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(6570), new EnchantScroll(false, true, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(6571), new EnchantScroll(true, true, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(6572), new EnchantScroll(false, true, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(6573), new EnchantScroll(true, true, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(6574), new EnchantScroll(false, true, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(6575), new EnchantScroll(true, true, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(6576), new EnchantScroll(false, true, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(6577), new EnchantScroll(true, true, false, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(6578), new EnchantScroll(false, true, false, false, 5, 20, 0, null));
		_scrolls.put(Integer.valueOf(22006), new EnchantScroll(true, false, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(22007), new EnchantScroll(true, false, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(22008), new EnchantScroll(true, false, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(22009), new EnchantScroll(true, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(22010), new EnchantScroll(false, false, false, false, 1, 20, 0, null));
		_scrolls.put(Integer.valueOf(22011), new EnchantScroll(false, false, false, false, 2, 20, 0, null));
		_scrolls.put(Integer.valueOf(22012), new EnchantScroll(false, false, false, false, 3, 20, 0, null));
		_scrolls.put(Integer.valueOf(22013), new EnchantScroll(false, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(22014), new EnchantScroll(true, false, false, false, false, true, 5, 25, 0, null));
		_scrolls.put(Integer.valueOf(22015), new EnchantScroll(true, false, false, true, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(22016), new EnchantScroll(false, false, false, false, false, true, 5, 25, 0, null));
		_scrolls.put(Integer.valueOf(22017), new EnchantScroll(false, false, false, true, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(22018), new EnchantScroll(true, false, false, false, true, false, 5, 25, 0, null));
		_scrolls.put(Integer.valueOf(22019), new EnchantScroll(true, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(22020), new EnchantScroll(false, false, false, false, true, false, 5, 25, 0, null));
		_scrolls.put(Integer.valueOf(22021), new EnchantScroll(false, false, false, false, 4, 20, 0, null));
		_scrolls.put(Integer.valueOf(13540), new EnchantScroll(true, false, false, false, 0, 0, 0, new int[]
		{
			13539
		}));
		_supports.put(Integer.valueOf(12362), new EnchantItem(true, 1, 15, 20, null));
		_supports.put(Integer.valueOf(12363), new EnchantItem(true, 2, 15, 18, null));
		_supports.put(Integer.valueOf(12364), new EnchantItem(true, 3, 15, 15, null));
		_supports.put(Integer.valueOf(12365), new EnchantItem(true, 4, 15, 12, null));
		_supports.put(Integer.valueOf(12366), new EnchantItem(true, 5, 15, 10, null));
		_supports.put(Integer.valueOf(12367), new EnchantItem(false, 1, 15, 35, null));
		_supports.put(Integer.valueOf(12368), new EnchantItem(false, 2, 15, 27, null));
		_supports.put(Integer.valueOf(12369), new EnchantItem(false, 3, 15, 23, null));
		_supports.put(Integer.valueOf(12370), new EnchantItem(false, 4, 15, 18, null));
		_supports.put(Integer.valueOf(12371), new EnchantItem(false, 5, 15, 15, null));
		_supports.put(Integer.valueOf(14702), new EnchantItem(true, 1, 15, 20, null));
		_supports.put(Integer.valueOf(14703), new EnchantItem(true, 2, 15, 18, null));
		_supports.put(Integer.valueOf(14704), new EnchantItem(true, 3, 15, 15, null));
		_supports.put(Integer.valueOf(14705), new EnchantItem(true, 4, 15, 12, null));
		_supports.put(Integer.valueOf(14706), new EnchantItem(true, 5, 15, 10, null));
		_supports.put(Integer.valueOf(14707), new EnchantItem(false, 1, 15, 35, null));
		_supports.put(Integer.valueOf(14708), new EnchantItem(false, 2, 15, 27, null));
		_supports.put(Integer.valueOf(14709), new EnchantItem(false, 3, 15, 23, null));
		_supports.put(Integer.valueOf(14710), new EnchantItem(false, 4, 15, 18, null));
		_supports.put(Integer.valueOf(14711), new EnchantItem(false, 5, 15, 15, null));
	}
	
	protected static final EnchantScroll getEnchantScroll(L2ItemInstance scroll)
	{
		return _scrolls.get(Integer.valueOf(scroll.getItemId()));
	}
	
	protected static final EnchantItem getSupportItem(L2ItemInstance item)
	{
		return _supports.get(Integer.valueOf(item.getItemId()));
	}
	
	protected static final boolean isEnchantable(L2ItemInstance item)
	{
		if (item.isHeroItem())
			return false;
		if (item.isShadowItem())
			return false;
		if (item.isCommonItem())
			return false;
		if (item.isEtcItem())
		{
			return false;
		}
		if (item.isWear())
		{
			return false;
		}
		if (item.getItem().getItemType() == L2WeaponType.ROD)
		{
			return false;
		}
		if (item.getItemId() >= 7816 && item.getItemId() <= 7831)
		{
			return false;
		}
		if (item.getItem().getBodyPart() == 2097152)
			return false;
		if (item.getItem().getBodyPart() == 1048576)
			return false;
		if (item.getItem().getBodyPart() == 8192)
		{
			return false;
		}
		if (item.getItemLocation() != L2ItemInstance.ItemLocation.INVENTORY && item.getItemLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
		{
			return false;
		}
		return true;
	}
}