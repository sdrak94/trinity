package net.sf.l2j.gameserver.templates.item;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 * 
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public abstract class L2Item
{
	public static final int				DONATION_TOKEN						= 200000;
	public static final int				TYPE1_WEAPON_RING_EARRING_NECKLACE	= 0;
	public static final int				TYPE1_SHIELD_ARMOR					= 1;
	public static final int				TYPE1_ITEM_QUESTITEM_ADENA			= 4;
	public static final int				TYPE2_WEAPON						= 0;
	public static final int				TYPE2_SHIELD_ARMOR					= 1;
	public static final int				TYPE2_ACCESSORY						= 2;
	public static final int				TYPE2_QUEST							= 3;
	public static final int				TYPE2_MONEY							= 4;
	public static final int				TYPE2_OTHER							= 5;
	public static final int				TYPE2_PET_WOLF						= 6;
	public static final int				TYPE2_PET_HATCHLING					= 7;
	public static final int				TYPE2_PET_STRIDER					= 8;
	public static final int				TYPE2_PET_BABY						= 9;
	public static final int				TYPE2_PET_EVOLVEDWOLF				= 10;
	public static final int				SLOT_NONE							= 0x0000;
	public static final int				SLOT_UNDERWEAR						= 0x0001;
	public static final int				SLOT_R_EAR							= 0x0002;
	public static final int				SLOT_L_EAR							= 0x0004;
	public static final int				SLOT_LR_EAR							= 0x00006;
	public static final int				SLOT_NECK							= 0x0008;
	public static final int				SLOT_R_FINGER						= 0x0010;
	public static final int				SLOT_L_FINGER						= 0x0020;
	public static final int				SLOT_LR_FINGER						= 0x0030;
	public static final int				SLOT_HEAD							= 0x0040;
	public static final int				SLOT_R_HAND							= 0x0080;
	public static final int				SLOT_L_HAND							= 0x0100;
	public static final int				SLOT_GLOVES							= 0x0200;
	public static final int				SLOT_CHEST							= 0x0400;
	public static final int				SLOT_LEGS							= 0x0800;
	public static final int				SLOT_FEET							= 0x1000;
	public static final int				SLOT_BACK							= 0x2000;
	public static final int				SLOT_LR_HAND						= 0x4000;
	public static final int				SLOT_FULL_ARMOR						= 0x8000;
	public static final int				SLOT_HAIR							= 0x010000;
	public static final int				SLOT_ALLDRESS						= 0x020000;
	public static final int				SLOT_HAIR2							= 0x040000;
	public static final int				SLOT_HAIRALL						= 0x080000;
	public static final int				SLOT_R_BRACELET						= 0x100000;
	public static final int				SLOT_L_BRACELET						= 0x200000;
	public static final int				SLOT_DECO							= 0x400000;
	public static final int				SLOT_BELT							= 0x10000000;
	public static final int				SLOT_WOLF							= -100;
	public static final int				SLOT_HATCHLING						= -101;
	public static final int				SLOT_STRIDER						= -102;
	public static final int				SLOT_BABYPET						= -103;
	public static final int				SLOT_GREATWOLF						= -104;
	public static final int				MATERIAL_STEEL						= 0x00;						// ??
	public static final int				MATERIAL_FINE_STEEL					= 0x01;						// ??
	public static final int				MATERIAL_BLOOD_STEEL				= 0x02;						// ??
	public static final int				MATERIAL_BRONZE						= 0x03;						// ??
	public static final int				MATERIAL_SILVER						= 0x04;						// ??
	public static final int				MATERIAL_GOLD						= 0x05;						// ??
	public static final int				MATERIAL_MITHRIL					= 0x06;						// ??
	public static final int				MATERIAL_ORIHARUKON					= 0x07;						// ??
	public static final int				MATERIAL_PAPER						= 0x08;						// ??
	public static final int				MATERIAL_WOOD						= 0x09;						// ??
	public static final int				MATERIAL_CLOTH						= 0x0a;						// ??
	public static final int				MATERIAL_LEATHER					= 0x0b;						// ??
	public static final int				MATERIAL_BONE						= 0x0c;						// ??
	public static final int				MATERIAL_HORN						= 0x0d;						// ??
	public static final int				MATERIAL_DAMASCUS					= 0x0e;						// ??
	public static final int				MATERIAL_ADAMANTAITE				= 0x0f;						// ??
	public static final int				MATERIAL_CHRYSOLITE					= 0x10;						// ??
	public static final int				MATERIAL_CRYSTAL					= 0x11;						// ??
	public static final int				MATERIAL_LIQUID						= 0x12;						// ??
	public static final int				MATERIAL_SCALE_OF_DRAGON			= 0x13;						// ??
	public static final int				MATERIAL_DYESTUFF					= 0x14;						// ??
	public static final int				MATERIAL_COBWEB						= 0x15;						// ??
	public static final int				MATERIAL_SEED						= 0x15;						// ??
	public static final int				CRYSTAL_NONE						= 0x00;						// ??
	public static final int				CRYSTAL_D							= 0x01;						// ??
	public static final int				CRYSTAL_C							= 0x02;						// ??
	public static final int				CRYSTAL_B							= 0x03;						// ??
	public static final int				CRYSTAL_A							= 0x04;						// ??
	public static final int				CRYSTAL_S							= 0x05;						// ??
	public static final int				CRYSTAL_S80							= 0x06;						// ??
	public static final int				CRYSTAL_S84							= 0x07;						// ??
	////// custom ones
	public static final int				CRYSTAL_S87							= 0x07;						// ??
	public static final int				CRYSTAL_S90							= 0x07;						// ??
	public static final int				CRYSTAL_S92							= 0x07;						// ??
	public static final int				CRYSTAL_S94							= 0x07;						// ??
	public static final int				CRYSTAL_S95							= 0x07;						// ??
	private static final int[]			crystalItemId						=
	{
		0, 1458, 1459, 1460, 1461, 1462, 1462, 1462
	};
	private static final int[]			crystalEnchantBonusArmor			=
	{
		0, 11, 6, 11, 19, 25, 25, 25
	};
	private static final int[]			crystalEnchantBonusWeapon			=
	{
		0, 90, 45, 67, 144, 250, 250, 250
	};
	public static final byte			COMMON								= 0;						// buyable from shop with adena, essentially unlimited (draconic bow), and 'default' items
	public static final byte			UNCOMMON							= 1;						// buyable from multisell, not hard to get (pride weapons, pride armors)
	public static final byte			RARE								= 2;						// buyable from multisell, hard to get (Icarus, vesper weapons)
	public static final byte			UNIQUE								= 3;						// unique level 1, dropped from mobs (named weapons)
	public static final byte			EPIC								= 4;						// unique level 2, dropped from mobs
	private final String				_icon;
	private final int					_itemId;
	private final int					_displayId;
	private final int					_nonKamaeldisplayId;
	private final String				_name;
	private final int					_type1;															// needed for item list (inventory)
	private final int					_type2;															// different lists for armor, weapon, etc
	private final int					_weight;
	private final boolean				_crystallizable;
	private final boolean				_stackable;
	private final int					_materialType;
	private final int					_crystalType;													// default to none-grade
	private final int					_duration;
	private final int					_time;
	private final int					_bodyPart;
	private final int					_referencePrice;
	private final int					_crystalCount;
	private final boolean				_sellable;
	private final boolean				_dropable;
	private final boolean				_destroyable;
	private final boolean				_tradeable;
	private final boolean				_depositable;
	private final boolean				_common;
	private final boolean				_heroItem;
	private final boolean				_castleItem;
	private final boolean				_pvpItem;
	private final boolean				_bindOnEquip;
	private final int					_permChance;
	private final float					_size;
	private final float					_uniqueness;
	@SuppressWarnings("rawtypes")
	protected final Enum				_type;
	protected FuncTemplate[]			_funcTemplates;
	protected EffectTemplate[]			_effectTemplates;
	protected L2Skill[]					_skills;
	protected List<Condition>			_preConditions						= new FastList<Condition>();
	private static final Func[]			_emptyFunctionSet					= new Func[0];
	protected static final L2Effect[]	_emptyEffectSet						= new L2Effect[0];
	
	/**
	 * Constructor of the L2Item that fill class variables.<BR>
	 * <BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>type</LI>
	 * <LI>_itemId</LI>
	 * <LI>_name</LI>
	 * <LI>_type1 & _type2</LI>
	 * <LI>_weight</LI>
	 * <LI>_crystallizable</LI>
	 * <LI>_stackable</LI>
	 * <LI>_materialType & _crystalType & _crystlaCount</LI>
	 * <LI>_duration</LI>
	 * <LI>_bodypart</LI>
	 * <LI>_referencePrice</LI>
	 * <LI>_sellable</LI>
	 * 
	 * @param type
	 *            : Enum designating the type of the item
	 * @param set
	 *            : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(Enum<?> type, StatsSet set)
	{
		_type = type;
		_itemId = set.getInteger("item_id");
		_displayId = set.getInteger("displayId", _itemId);
		_nonKamaeldisplayId = set.getInteger("nonKamaelDisplayId", _itemId);
		_name = set.getString("name");
		_icon = set.getString("icon", null);
		_type1 = set.getInteger("type1"); // needed for item list (inventory)
		_type2 = set.getInteger("type2"); // different lists for armor, weapon, etc
		_weight = set.getInteger("weight");
		_crystallizable = set.getBool("crystallizable");
		_stackable = set.getBool("stackable", false);
		_materialType = set.getInteger("material");
		_crystalType = set.getInteger("crystal_type", CRYSTAL_NONE); // default to none-grade
		_duration = set.getInteger("duration");
		_time = set.getInteger("time");
		_bodyPart = set.getInteger("bodypart");
		_referencePrice = set.getInteger("price");
		_crystalCount = set.getInteger("crystal_count", 0);
		_sellable = set.getBool("sellable", true);
		_destroyable = set.getBool("destroyable", true);
		_tradeable = set.getBool("tradeable", true);
		_depositable = set.getBool("depositable", true);
		_common = (_itemId >= 12006 && _itemId <= 12361) || (_itemId >= 11605 && _itemId <= 12308);
		_heroItem = (_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842;
		_castleItem = _itemId == 14602 || (_itemId >= 6834 && _itemId <= 6841) || (_itemId >= 8182 && _itemId <= 8183) || _itemId == 7015 || (_itemId >= 75000 && _itemId <= 75014);
		_pvpItem = (_itemId >= 10667 && _itemId <= 10792) || (_itemId >= 10793 && _itemId <= 10835) || (_itemId >= 12852 && _itemId <= 12977) || (_itemId >= 14363 && _itemId <= 14519) || (_itemId >= 14520 && _itemId <= 14525) || _itemId == 14528 || _itemId == 14529 || _itemId == 14558;
		if (_castleItem || _heroItem)
			_dropable = false;
		else
			_dropable = set.getBool("dropable", true);
		_bindOnEquip = set.getBool("bind_equip", false);
		_size = set.getFloat("size", 0);
		_permChance = set.getInteger("perm_chance", -1);
		_uniqueness = set.getFloat("uniqueness", 0);
	}
	
	/**
	 * Returns the itemType.
	 * 
	 * @return Enum
	 */
	@SuppressWarnings("rawtypes")
	public Enum getItemType()
	{
		return _type;
	}
	
	/**
	 * Returns the duration of the item
	 * 
	 * @return int
	 */
	public final int getDuration()
	{
		return _duration;
	}
	
	public final float getSize()
	{
		return _size;
	}
	
	/**
	 * Returns the time of the item in MINUTES
	 * 
	 * @return int
	 */
	public final int getTime()
	{
		return _time;
	}
	
	public boolean isJewelry()
	{
		return getType2() == L2Item.TYPE2_ACCESSORY;
	}
	
	/**
	 * Returns the ID of the iden
	 * 
	 * @return int
	 */
	public final int getItemId()
	{
		return _itemId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Return the type of material of the item
	 * 
	 * @return int
	 */
	public final int getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Returns the type 2 of the item
	 * 
	 * @return int
	 */
	public final int getType2()
	{
		return _type2;
	}
	
	/**
	 * Returns the weight of the item
	 * 
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Returns if the item is crystallizable
	 * 
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return _crystallizable;
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * 
	 * @return int
	 */
	public final int getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * 
	 * @return int
	 */
	public final int getCrystalItemId()
	{
		return crystalItemId[_crystalType];
	}
	
	/**
	 * Returns the grade of the item.<BR>
	 * <BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this fucntion returns the type of crystal of the item.
	 * 
	 * @return int
	 */
	public final int getItemGrade()
	{
		return getCrystalType();
	}
	
	/**
	 * Returns the grade of the item.<BR>
	 * <BR>
	 * For grades S80 and S84 return S
	 * 
	 * @return int
	 */
	public final int getItemGradeSPlus()
	{
		switch (getItemGrade())
		{
			case CRYSTAL_S80:
			case CRYSTAL_S84:
				return CRYSTAL_S;
			default:
				return getItemGrade();
		}
	}
	
	/**
	 * Returns the quantity of crystals for crystallization
	 * 
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * Returns the quantity of crystals for crystallization on specific enchant level
	 * 
	 * @return int
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
				default:
					return _crystalCount;
			}
		else if (enchantLevel > 0)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
				default:
					return _crystalCount;
			}
		else
			return _crystalCount;
	}
	
	/**
	 * Returns the name of the item
	 * 
	 * @return String
	 */
	public final String getName()
	{
		return _name;
	}
	
	public final String getIcon()
	{
		if (_icon == null)
			return IconsTable.getInstance().getItemIcon(getItemId());
		return _icon;
	}
	
	/**
	 * Return the part of the body used with the item.
	 * 
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * Returns the type 1 of the item
	 * 
	 * @return int
	 */
	public final int getType1()
	{
		return _type1;
	}
	
	/**
	 * Returns if the item is stackable
	 * 
	 * @return boolean
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * Returns if the item is consumable
	 * 
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return false;
	}
	
	public boolean isEquipable()
	{
		return getBodyPart() != 0 && !(getItemType() instanceof L2EtcItemType);
	}
	
	/**
	 * Returns the price of reference of the item
	 * 
	 * @return int
	 */
	public final int getReferencePrice()
	{
		return (isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice);
	}
	
	/**
	 * Returns if the item can be sold
	 * 
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * Returns if the item can dropped
	 * 
	 * @return boolean
	 */
	public final boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * Returns if the item can destroy
	 * 
	 * @return boolean
	 */
	public final boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * Returns if the item can add to trade
	 * 
	 * @return boolean
	 */
	public final boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * Returns if the item can be put into warehouse
	 * 
	 * @return boolean
	 */
	public final boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * Returns if item is common
	 * 
	 * @return boolean
	 */
	public final boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Returns if item is hero-only
	 * 
	 * @return
	 */
	public final boolean isHeroItem()
	{
		return _heroItem;
	}
	
	public final boolean isCastleItem()
	{
		return _castleItem;
	}
	
	/**
	 * Returns if item is pvp
	 * 
	 * @return
	 */
	public final boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	/**
	 * Returns if item is for hatchling
	 * 
	 * @return boolean
	 */
	public boolean isForHatchling()
	{
		return (_type2 == TYPE2_PET_HATCHLING);
	}
	
	/**
	 * Returns if item is for strider
	 * 
	 * @return boolean
	 */
	public boolean isForStrider()
	{
		return (_type2 == TYPE2_PET_STRIDER);
	}
	
	/**
	 * Returns if item is for wolf
	 * 
	 * @return boolean
	 */
	public boolean isForWolf()
	{
		return (_type2 == TYPE2_PET_WOLF);
	}
	
	/**
	 * Returns if item is for Great wolf
	 * 
	 * @return boolean
	 */
	public boolean isForEvolvedWolf()
	{
		return (_type2 == TYPE2_PET_EVOLVEDWOLF);
	}
	
	/**
	 * Returns if item is for wolf
	 * 
	 * @return boolean
	 */
	public boolean isForBabyPet()
	{
		return (_type2 == TYPE2_PET_BABY);
	}
	
	/**
	 * Returns array of Func objects containing the list of functions used by the item
	 * 
	 * @param instance
	 *            : L2ItemInstance pointing out the item
	 * @param player
	 *            : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;
		List<Func> funcs = new FastList<Func>();
		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.target = player;
			env.item = instance;
			Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	/**
	 * Returns the effects associated with the item.
	 * 
	 * @param instance
	 *            : L2ItemInstance pointing out the item
	 * @param player
	 *            : L2Character pointing out the player
	 * @return L2Effect[] : array of effects generated by the item
	 */
	public L2Effect[] getEffects(L2ItemInstance instance, L2Character player)
	{
		if (_effectTemplates == null)
			return _emptyEffectSet;
		List<L2Effect> effects = new FastList<L2Effect>();
		for (EffectTemplate et : _effectTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.target = player;
			env.item = instance;
			L2Effect e = et.getEffect(env);
			if (e != null)
			{
				e.scheduleEffect();
				effects.add(e);
			}
		}
		if (effects.isEmpty())
			return _emptyEffectSet;
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	/**
	 * Returns effects of skills associated with the item.
	 * 
	 * @param caster
	 *            : L2Character pointing out the caster
	 * @param target
	 *            : L2Character pointing out the target
	 * @return L2Effect[] : array of effects generated by the skill
	 *         public L2Effect[] getSkillEffects(L2Character caster, L2Character target)
	 *         {
	 *         if (_skills == null)
	 *         return _emptyEffectSet;
	 *         List<L2Effect> effects = new FastList<L2Effect>();
	 *         for (L2Skill skill : _skills)
	 *         {
	 *         if (!skill.checkCondition(caster, target, true))
	 *         continue; // Skill condition not met
	 *         if (target.getFirstEffect(skill.getId()) != null)
	 *         target.removeEffect(target.getFirstEffect(skill.getId()));
	 *         for (L2Effect e : skill.getEffects(caster, target))
	 *         effects.add(e);
	 *         }
	 *         if (effects.isEmpty())
	 *         return _emptyEffectSet;
	 *         return effects.toArray(new L2Effect[effects.size()]);
	 *         }
	 */
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * 
	 * @param f
	 *            : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		// If _functTemplates is empty, create it and add the FuncTemplate f in it
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			// Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
			// number of components to be copied)
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	/**
	 * Add the EffectTemplate effect to the list of effects generated by the item
	 * 
	 * @param effect
	 *            : EffectTemplate
	 */
	public void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			// Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
			// number of components to be copied)
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}
	
	/**
	 * Add the L2Skill skill to the list of skills generated by the item
	 * 
	 * @param skill
	 *            : L2Skill
	 */
	public void attach(L2Skill skill)
	{
		if (_skills == null)
		{
			_skills = new L2Skill[]
			{
				skill
			};
		}
		else
		{
			int len = _skills.length;
			L2Skill[] tmp = new L2Skill[len + 1];
			// Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
			// number of components to be copied)
			System.arraycopy(_skills, 0, tmp, 0, len);
			tmp[len] = skill;
			_skills = tmp;
		}
	}
	
	public final void attach(Condition c)
	{
		if (!_preConditions.contains(c))
			_preConditions.add(c);
	}
	
	public final L2Skill[] getItemSkills()
	{
		return _skills;
	}
	
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage)
	{
		if (activeChar.isGM() && !Config.GM_ITEM_RESTRICTION)
			return true;
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
				return true;
			Env env = new Env();
			env.player = activeChar;
			if (target instanceof L2Character) // TODO: object or char?
				env.target = (L2Character) target;
			if (!preCondition.test(env))
			{
				if (activeChar instanceof L2SummonInstance)
				{
					((L2SummonInstance) activeChar).getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
					return false;
				}
				if (sendMessage)
				{
					String msg = preCondition.getMessage();
					int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
					else if (msgId != 0)
					{
						SystemMessage sm = new SystemMessage(msgId);
						if (preCondition.isAddName())
							sm.addItemName(_itemId);
						activeChar.sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the name of the item
	 * 
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}
	
	public boolean isUntradeableAfterEquip()
	{
		return _bindOnEquip;
	}
	/*
	 * public byte getEnchantLevelAtWhichItemBreaks()
	 * {
	 * switch (_crystalType)
	 * {
	 * case CRYSTAL_S84:
	 * {
	 * return 10;
	 * }
	 * case CRYSTAL_S80:
	 * {
	 * if (getName().contains("Icarus"))
	 * return 15;
	 * else
	 * return 20;
	 * }
	 * default:
	 * {
	 * return 25;
	 * }
	 * }
	 * }
	 */
	
	public int getPermChance()
	{
		return _permChance;
	}
	
	public final int getDisplayId()
	{
		return _displayId;
	}
	
	public final int getNonKamaelDisplayId()
	{
		return _nonKamaeldisplayId;
	}
	
	public float getUniqueness()
	{
		return _uniqueness;
	}
	
	public int getClutchEnchantLevel()
	{
		if (getUniqueness() == 0) // normal s
		{
			return Config.ENCHANT_CLUTCH_TIER_0;
		}
		else if (getUniqueness() == 1)
		{
			return Config.ENCHANT_CLUTCH_TIER_1;
		}
		else if (getUniqueness() == 1.5) // masterworks
		{
			return Config.ENCHANT_CLUTCH_TIER_1_5;
		}
		else if (getUniqueness() == 2) // dynasty
		{
			return Config.ENCHANT_CLUTCH_TIER_2;
		}
		else if (getUniqueness() == 2.5) // dynasty tier 2 and icarus weapons
		{
			return Config.ENCHANT_CLUTCH_TIER_2_5;
		}
		else if (isRaidbossItem()) // raidboss
		{
			return Config.ENCHANT_CLUTCH_TIER_3_5;
		}
		else if (getUniqueness() == 3) // vesper and rare pvp
		{
			if (getName().contains("Dynasty"))
				return Config.ENCHANT_CLUTCH_TIER_3_DYNASTY;
			if (getItemId() != 14163 && getItemId() != 14164 && getItemId() != 14165)
				return Config.ENCHANT_CLUTCH_TIER_3_VESPER;
			if (getItemId() == 14163 && getItemId() == 14164 && getItemId() == 14165)
				return Config.ENCHANT_CLUTCH_TIER_3_VESPER_JEWS;
			return Config.ENCHANT_CLUTCH_TIER_3_DEFAULT;
		}
		else if (getUniqueness() == 3.5) // raidboss, relic jewels
		{
			return Config.ENCHANT_CLUTCH_TIER_3_5;
		}
		else if (getUniqueness() == 4) // titanium and epic pvp
		{
			if (isStandardShopItem())
				return Config.ENCHANT_CLUTCH_TIER_4;
			else
				return Config.ENCHANT_CLUTCH_TIER_4;
		}
		else if (isWarForged()) // titanium and epic pvp
		{
			return 8;
		}
		else if (getUniqueness() == 4.5) // dread stuffs
		{
			return Config.ENCHANT_CLUTCH_TIER_4_5;
		}
		else if (getUniqueness() == 5) // currently only Rykros
		{
			return Config.ENCHANT_CLUTCH_TIER_5;
		}
		return 65535;
	}
	
	public int getSuperEnchantLevel()
	{
		return 21;
	}
	
	public boolean isEnchantable()
	{
		return (this instanceof L2Weapon || this instanceof L2Armor);
	}
	
	public boolean isStandardShopItem()
	{
		if (getStandardShopItem() == 0)
			return true;
		return false;
	}
	
	public boolean isRaidbossItem()
	{
		if (getStandardShopItem() > 50)
			return true;
		return false;
	}
	
	public int getStandardShopItem()
	{
		return 0;
	}
	
	public boolean isHairAccessory()
	{
		switch (getBodyPart())
		{
			case SLOT_HAIR:
			case SLOT_HAIR2:
			case SLOT_HAIRALL:
			case SLOT_HAIR | SLOT_HAIR2:
				return true;
		}
		return false;
	}
	
	public boolean isDread()
	{
		return getUniqueness() == 4.5f && getName().contains("Dread");
	}
	
	public boolean isTit()
	{
		return getUniqueness() == 4f && isStandardShopItem() && getName().contains("Titanium");
	}
	
	public boolean isUnique()
	{
		return getUniqueness() == 3.5f && (getItemId() >= 91000 && getItemId() >= 91029 || getItemId() == 8206);
	}
	
	public boolean isMorheim()
	{
		return getUniqueness() == 4.5f && getName().contains("Morheim");
	}
	
	public boolean isWarForged()
	{
		return getUniqueness() == 4 && getName().contains("Forged");
	}
	
	public boolean isRelicJew()
	{
		return getUniqueness() >= 3.5 && isJewelry();
	}
	
	public boolean isTalisman()
	{
		return true;
	}
	
	public boolean isPvpWeapon()
	{
		return (_itemId >= 81000 && _itemId <= 81020) || (_itemId >= 80000 && _itemId <= 80017);
	}
	
	public boolean isCorrupted()
	{
		return getUniqueness() >= 4.5 && getName().contains("Corrupted");
	}
	
	public FuncTemplate[] getFuncTemplates()
	{
		return _funcTemplates;
	}
}