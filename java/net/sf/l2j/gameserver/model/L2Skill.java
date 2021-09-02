package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2ArtefactInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2BufferInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public abstract class L2Skill implements IChanceSkillTrigger
{
	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());

	private static final L2Character[] _emptyTargetList = null;

	public static final int SKILL_LUCKY = 194;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	public static final int SKILL_CLAN_LUCK = 390;
	
	private static final int[] INVISIBLE_SKILLS = new int[] { 9001, 9002, 9003, 9004, 9005, 9006, 9073, 9074, 9075, 9076, 9077, 9081, 9082, 9083, 9084, 9085, 9421, 9422, 9423, 9424, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9434, 9435, 9436, 9437, 9438, 9439, 9440, 9441, 9442, 9443, 9444, 9445, 9446, 9447, 9448, 9449, 9450, 9451, 9452, 9453, 9454, 9455, 9456, 9457, 9458, 9459, 9460, 9461, 9462, 9463, 9464, 9465, 9466, 9467, 9468, 9469, 9470, 9471, 9472, 9473, 9474, 9475, 9476, 9477, 9478, 9479, 9480, 9481, 9482, 9483, 9484, 9485, 9486, 9487, 9488, 9489, 9490, 9491, 9492, 9493, 9494, 9495, 9496, 9497, 9498, 9499, 9500, 9501, 9502, 9503, 9504, 38000, 38001, 38002, 38003, 38004, 38005, 38006, 38007, 38008, 38009, 38010, 38011, 38012, 38013, 38014, 38015, 38016, 38017, 38018, 38019, 38020, 38021, 38022, 38023, 38024, 38025, 38026, 38027, 38028, 38029, 38030, 38031, 38032, 38033, 38034, 38035, 37558, 38050, 38051, 38052};

	public static final boolean geoEnabled = Config.GEODATA > 0;

	public static enum SkillOpType
	{
		OP_PASSIVE, OP_ACTIVE, OP_TOGGLE
	}

	/** Target types of skills : SELF, PARTY, CLAN, PET... */
	public static enum SkillTargetType
	{
		TARGET_NONE,
		TARGET_SELF,
		TARGET_ONE,
		TARGET_PARTY,
		TARGET_ALLY,
		TARGET_CLAN,
		TARGET_PET,
		TARGET_SUMMON,
		TARGET_AREA,
		TARGET_FRONT_AREA,
		TARGET_BEHIND_AREA,
		TARGET_AURA,
		TARGET_ALL,
		TARGET_FRONT_AURA,
		TARGET_BEHIND_AURA,
		TARGET_CORPSE,
		TARGET_UNDEAD,
		TARGET_AREA_UNDEAD,
		TARGET_MULTIFACE,
		TARGET_CORPSE_ALLY,
		TARGET_CORPSE_CLAN,
		TARGET_CORPSE_PLAYER,
		TARGET_CORPSE_PET,
		TARGET_AREA_CORPSE_MOB,
		TARGET_CORPSE_MOB,
		TARGET_UNLOCKABLE,
		TARGET_HOLY,
		TARGET_FLAGPOLE,
		TARGET_SELF_AND_PET,
		TARGET_ONE_AND_PET,
		TARGET_PARTY_MEMBER,
		TARGET_PARTY_OTHER,
		TARGET_ENEMY_SUMMON,
		TARGET_OWNER_PET,
		TARGET_GROUND,
		TARGET_COUPLE,
		TARGET_SELF_AND_COUPLE,
		TARGET_CORPSE_DRAIN,
		TARGET_CORPSE_DRAIN_AOE,
		TARGET_TRAP
	}

	//conditional values
	public final static int COND_RUNNING = 0x0001;
	public final static int COND_WALKING = 0x0002;
	public final static int COND_SIT = 0x0004;
	public final static int COND_BEHIND = 0x0008;
	public final static int COND_CRIT = 0x0010;
	public final static int COND_LOWHP = 0x0020;
	public final static int COND_ROBES = 0x0040;
	public final static int COND_CHARGES = 0x0080;
	public final static int COND_SHIELD = 0x0100;

	private static final Func[] _emptyFunctionSet = new Func[0];
	private static final L2Effect[] _emptyEffectSet = new L2Effect[0];

	// these two build the primary key
	private final int _id;
	private int _level;

	/** Identifier for a skill that client can't display */
	private int _displayId;
	private int _displayLvl;

	public int getDisplayLvl()
	{
		return this._displayLvl;
	}

	// not needed, just for easier debug
	private final String _name;
	private final SkillOpType _operateType;
	private final boolean _magic;
	private final boolean _staticReuse;
	private final boolean _staticHitTime;
	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final float _mpConsumeMulti;
	private final int _hpConsume;
	private final int _hpConsumePercent;
	private final int _cpConsume;

	private final int _targetConsume;
	private final int _targetConsumeId;

	private final int _itemConsume;
	private final int _itemConsumeId;

	private final int _castRange;
	private final int _effectRange;

	// Abnormal levels for skills and their canceling, e.g. poison vs negate
	private final int _abnormalLvl; // e.g. poison or bleed lvl 2
	// Note: see also _effectAbnormalLvl
	private final int _negateLvl; // abnormalLvl is negated with negateLvl
	private final int[] _negateId; // cancels the effect of skill ID
	private final L2SkillType[] _negateStats; // lists the effect types that are canceled
	private final int _maxNegatedEffects; // maximum number of effects to negate

	private final boolean _stayAfterDeath; // skill should stay after death

	private final int _mustNegateId;

	// kill by damage over time
	private final boolean _killByDOT;

	private final int _refId;
	// all times in milliseconds
	private final int _hitTime;
	private final int[] _hitTimings;
	private final int _coolTime;
	private final int _reuseDelay;
	private final int _buffDuration;
	// for item skills delay on equip
	private final int _equipDelay;

	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final SkillTargetType _targetType;
	private final int _feed;
	// base success chance
	private final double _power;
	private final int _magicLevel;
	private final int _levelDepend;
	private final boolean _ignoreResists;

	private final boolean _isNeutral;
	// Effecting area of the skill, in radius.
	// The radius center varies according to the _targetType:
	// "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
	private final int _skillRadius;

	private final L2SkillType _skillType;
	private final L2SkillType _effectType; // additional effect has a type
	private final int _effectAbnormalLvl; // abnormal level for the additional effect type, e.g. poison lvl 1
	private final int _effectId;
	private final int _effectLvl; // normal effect level

	private final boolean _isPotion;
	private final byte _element;
	private final int _elementPower;

	private final Stats _stat;

	private final int _condition;
	private final int _conditionValue;
	private final boolean _overhit;
	private final int _weaponsAllowed;
	private final int _armorsAllowed;

	private final int _minPledgeClass;

	private final boolean _isOffensive;
	private final int _requiredCharges;
	private final int _maxCharges;
	private final int _numCharges;
	private final int _triggeredId;
	private final int _triggeredLevel;
	private final String _chanceType;
	private final int _soulMaxConsume;
	private final int _soulConsume;
	private final int _numSouls;
	private final int _expNeeded;
	private final int _critChance;

	private final float _pvpMulti;
	private final float _pvmMulti;

	private final int _transformId;
	private final int _transformDuration;

	private final int _afterEffectId;
	private final int _afterEffectLvl;
	private boolean _isHeroSkill; // If true the skill is a Hero Skill

	private final int _baseCritRate; // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
	private final int _lethalEffect1; // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
	private final int _lethalEffect2; // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
	private final boolean _directHpDmg; // If true then dmg is being make directly
	private final boolean _isDance; // If true then casting more dances will cost more MP
	private final int _nextDanceCost;
	private final float _sSBoost; //If true skill will have SoulShot boost (power*2)
	private final int _aggroPoints;

	protected Condition _preCondition;
	protected Condition _itemPreCondition;
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected EffectTemplate[] _effectTemplatesSelf;

	protected ChanceCondition _chanceCondition = null;

	// Flying support
	private final String _flyType;
	private final String _knockbackType;
	private final int _flyRadius;
	private final float _flyCourse;

	private boolean _followTarget;

	private final boolean _isDebuff;

	private final String _attribute;
	private final int _afroId;

	private final String _desc;

	private final boolean _ignoreShield;
	private final boolean _isSuicideAttack;
	private final boolean _canBeReflected;

	private final boolean _canBeDispeled;

	private final int _maxLandChance;
	private final int _minLandChance;

	private final float _reuseMulti;
	private final float _powerMulti;

	private final int _areaAngle;
	private final float _areaDmgTaper;
	public final boolean _areaLimit;
	private final boolean _noOly;
	private final boolean _noKorean;
	private final boolean _Oly;

	private final int _areaInclude;

	private final boolean _simultaneous;

	public final int getAreaInclude()
	{
		return _areaInclude;
	}

	private final int _olyNerf;
	private final float _olyTimeMulti;
	public final int _retries;

	private final boolean _chillIgnore;
	
	public final int getRetries(L2Character caster)
	{
		return (int) caster.calcStat(Stats.SKILL_RETRY_CHANGE, _retries, null, this);
	}

	private final boolean _staticPower;

	/*private int _ownerObjId = 0;
	private final int _partyPassiveRadius;*/
	private final boolean _sendIcon;

	final private String _sound;

	
	private final boolean _excludedFromCheck;
	
	protected L2Skill(StatsSet set)
	{
		_chillIgnore = set.getBool("chillIgnore", false);
		
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");
		_refId = set.getInteger("referenceId", set.getInteger("itemConsumeId", 0));
		_afroId = set.getInteger("afroId", 0);
		_displayId = set.getInteger("displayId", _id);
		_displayLvl = set.getInteger("displayLvl", 1);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_magic = set.getBool("isMagic", false);
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		_staticPower = set.getBool("staticPower", false);
		_isPotion = set.getBool("isPotion", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", isPotion() ? 0 : 70);
		_mpConsumeMulti = set.getFloat("mpConsumeMulti", 1);
		_hpConsume = set.getInteger("hpConsume", 0);
		_hpConsumePercent = set.getInteger("hpConsumePerc", 0);
		_cpConsume = set.getInteger("cpConsume", 0);
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);
		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_afterEffectId = set.getInteger("afterEffectId", 0);
		_afterEffectLvl = set.getInteger("afterEffectLvl", 1);

		_castRange = set.getInteger("castRange", -1);
		_effectRange = set.getInteger("effectRange", -1);

		_abnormalLvl = set.getInteger("abnormalLvl", -1);
		_effectAbnormalLvl = set.getInteger("effectAbnormalLvl", -1); // support for a separate effect abnormal lvl, e.g. poison inside a different skill
		_negateLvl = set.getInteger("negateLvl", -1);

		_attribute = set.getString("attribute", "");

		_pvpMulti = set.getFloat("pvpMulti", 1);
		_pvmMulti = set.getFloat("pvmMulti", 1);

		_maxLandChance = set.getInteger("maxLand", 0);
		_minLandChance = set.getInteger("minLand", 0);

		_followTarget = set.getBool("followTarget", false);

		String str = set.getString("negateStats", "");

		_desc = set.getString("desc", null);

		_areaAngle = set.getInteger("areaAngle", 45);
		_areaLimit = set.getBool("areaLimit", false);
		_areaInclude = set.getInteger("areaInclude", 0);
		_areaDmgTaper = set.getFloat("areaDmgTaper", 1);

		if (str == "")
			_negateStats = new L2SkillType[0];
		else
		{
			String[] stats = str.split(" ");
			L2SkillType[] array = new L2SkillType[stats.length];

			for (int i = 0; i < stats.length; i++)
			{
				L2SkillType type = null;
				try
				{
					type = Enum.valueOf(L2SkillType.class, stats[i]);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("SkillId: " + _id + "Enum value of type " + L2SkillType.class.getName() + "required, but found: " + stats[i]);
				}

				array[i] = type;
			}
			_negateStats = array;
		}

		String negateId = set.getString("negateId", null);
		if (negateId != null)
		{
			String[] valuesSplit = negateId.split(",");
			_negateId = new int[valuesSplit.length];
			for (int i = 0; i < valuesSplit.length; i++)
			{
				_negateId[i] = Integer.parseInt(valuesSplit[i]);
			}
		}
		else
			_negateId = new int[0];
		_maxNegatedEffects = set.getInteger("maxNegated", 0);

		_stayAfterDeath = set.getBool("stayAfterDeath", false);

		_killByDOT = set.getBool("killByDOT", false);
		_isNeutral = set.getBool("neutral", false);
		_hitTime = set.getInteger("hitTime", 0);
		String hitTimings = set.getString("hitTimings", null);
		if (hitTimings != null)
		{
			try
			{
				String[] valuesSplit = hitTimings.split(",");
				_hitTimings = new int[valuesSplit.length];
				for (int i = 0; i < valuesSplit.length; i++)
					_hitTimings[i] = Integer.parseInt(valuesSplit[i]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid hitTimings value: " + hitTimings + ", \"percent,percent,...percent\" required");
			}
		}
		else
			_hitTimings = new int[0];
		_coolTime = set.getInteger("coolTime", 0);
		_isDebuff = set.getBool("isDebuff", false);
		_feed = set.getInteger("feed", 0);
		if (Config.ENABLE_MODIFY_SKILL_REUSE && Config.SKILL_REUSE_LIST.containsKey(_id))
		{
			if (Config.DEBUG)
				_log.info("*** Skill " + _name + " (" + _level + ") changed reuse from " + set.getInteger("reuseDelay", 0) + " to " + Config.SKILL_REUSE_LIST.get(_id) + " seconds.");
			_reuseDelay = Config.SKILL_REUSE_LIST.get(_id);
		}
		else
		{
			_reuseDelay = set.getInteger("reuseDelay", 0);
		}

		_buffDuration = set.getInteger("buffDuration", 0);

		_equipDelay = set.getInteger("equipDelay", 0);

		_skillRadius = set.getInteger("skillRadius", 20);

		_targetType = set.getEnum("target", SkillTargetType.class);
		_power = set.getFloat("power", 0);
		_magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));
		_levelDepend = set.getInteger("lvlDepend", 0);
		_ignoreResists = set.getBool("ignoreResists", false);
		_stat = set.getEnum("stat", Stats.class, null);
		_ignoreShield = set.getBool("ignoreShld", false);
		_skillType = set.getEnum("skillType", L2SkillType.class);
		_effectType = set.getEnum("effectType", L2SkillType.class, null);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getInteger("effectLevel", 0);

		_element = set.getByte("element", (byte) -1);
		_elementPower = set.getInteger("elementPower", 0);

		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_armorsAllowed = set.getInteger("armorsAllowed", 0);

		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_maxCharges = set.getInteger("maxCharges", 0);
		_numCharges = set.getInteger("numCharges", 0);
		_requiredCharges = set.getInteger("requiredCharges", 0);
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 0);
		_chanceType = set.getString("chanceType", "");
		if (_chanceType != "" && !_chanceType.isEmpty())
			_chanceCondition = ChanceCondition.parse(set);

		_numSouls = set.getInteger("num_souls", 0);
		_soulMaxConsume = set.getInteger("soulMaxConsumeCount", 0);
		_soulConsume = set.getInteger("soulConsumeCount", 0);
		_expNeeded = set.getInteger("expNeeded", 0);
		_critChance = set.getInteger("critChance", 0);

		_transformId = set.getInteger("transformId", 0);
		_transformDuration = set.getInteger("transformDuration", 0);

		_excludedFromCheck = set.getBool("excludedFromCheck", false);
		_isHeroSkill = false;

		for (int i : L2PcInstance.HERO_SKILLS)
		{
			if (i == _id)
			{
				_isHeroSkill = true;
				break;
			}
		}

		_baseCritRate = set.getInteger("baseCritRate", (isDamaging()) ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);

		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isDance = set.getBool("isDance", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_sSBoost = set.getFloat("SSBoost", 0.f);
		_aggroPoints = set.getInteger("aggroPoints", 0);

		_flyType = set.getString("flyType", null);
		_knockbackType = set.getString("knockbackType", "THROW_HORIZONTAL");
		_flyRadius = set.getInteger("flyRadius", 200);
		_flyCourse = set.getFloat("flyCourse", 0);
		_canBeReflected = set.getBool("canBeReflected", true);
		_canBeDispeled = set.getBool("canBeDispeled", true);

		_mustNegateId = set.getInteger("mustNegateId", 0);
		_reuseMulti = set.getFloat("reuseMulti", 1);
		_powerMulti = set.getFloat("powerMulti", 1);
		_noOly = set.getBool("olyDisabled", false);
		_noKorean = set.getBool("koreanDisabled", false);
		_Oly = set.getBool("olyEnabled", false);
		_olyNerf = set.getInteger("olyNerf", 0);
		_olyTimeMulti = set.getFloat("olyTimeMulti", 1);
		_retries = set.getInteger("retries", 0);
		/*_partyPassiveRadius = set.getInteger("partyPassiveRadius", 0);*/
		_sendIcon = set.getBool("sendIcon", true);
		_simultaneous = set.getBool("simultaneous", false);
		_sound = set.getString("sound", null);
	}

	public final String getKnockbackType()
	{
		return _knockbackType;
	}

	public final boolean isSimultaneous()
	{
		return _simultaneous;
	}

	public final float getAreaDmgTaper()
	{
		return _areaDmgTaper;
	}

	public final boolean isFollowTarget()
	{
		return _followTarget;
	}

	public final void setFollowTarget(boolean followTarget)
	{
		_followTarget = followTarget;
	}

	public final int getHpConsumePercent()
	{
		return (int) (_hpConsumePercent * _mpConsumeMulti);
	}

	/*public final void setOwnerObjId(final int id) //only used to identify which person this skill's from in target_party passives
	{
		_ownerObjId = id;
	}
	public final int getOwnerObjId()
	{
		return _ownerObjId;
	}
	public final int getPartyPassiveRadius()
	{
		return _partyPassiveRadius;
	}*/
   public final boolean sendIcon()
   {
     getId();
     for (int ids : INVISIBLE_SKILLS)
		{
	    	 if (getId() == ids)
	    	 {
	    	   return false;
	    	 }
		} 
     return _sendIcon;
	}
/*	public final boolean sendIcon()
	{
		return _sendIcon;
	}*/

	public final String getDescription()
	{
		return _desc;
	}

	public abstract void useSkill(L2Character caster, L2Object[] targets);

	public final boolean isPotion()
	{
		return _isPotion;
	}

	public final boolean isDisabledInKorean()
	{
		return _noKorean;
	}
	public final boolean isDisabledInOlympiad()
	{
		return _noOly;
	}
	public final boolean isEnabledInOlympiad()
	{
		return _Oly;
	}
	public final int getOlyNerf()
	{
		return _olyNerf;
	}

	public final int getArmorsAllowed()
	{
		return _armorsAllowed;
	}

	public final int getConditionValue()
	{
		return _conditionValue;
	}

	public final L2SkillType getSkillType()
	{
		return _skillType;
	}

	public final byte getElement()
	{
		return _element;
	}

	public final int getElementPower()
	{
		return _elementPower;
	}

	/**
	 * Return the target type of the skill : SELF, PARTY, CLAN, PET...<BR><BR>
	 *
	 */
	public final SkillTargetType getTargetType(L2Character caster)
	{
		if (caster != null && caster instanceof L2PcInstance)
		{
			final int typeChange = (int) caster.calcStat(Stats.SKILL_TARGET_TYPE_CHANGE, 0, null, this);

			switch (typeChange)
			{
				case 1:
					return SkillTargetType.TARGET_AREA;
				case 2:
					return SkillTargetType.TARGET_FRONT_AREA;
				case 3:
					return SkillTargetType.TARGET_BEHIND_AREA;
				case 4:
					return SkillTargetType.TARGET_AURA;
				case 5:
					return SkillTargetType.TARGET_FRONT_AURA;
				case 6:
					return SkillTargetType.TARGET_BEHIND_AURA;
				case 7:
					return SkillTargetType.TARGET_ONE;
				case 8:
					return SkillTargetType.TARGET_PARTY;
				case 9:
					return SkillTargetType.TARGET_PARTY_MEMBER;
				case 10:
					return SkillTargetType.TARGET_PARTY_OTHER;
			}
		}

		return _targetType;
	}

	public final int getCondition()
	{
		return _condition;
	}

	public final boolean isOverhit()
	{
		return _overhit;
	}

	public final boolean killByDOT()
	{
		return _killByDOT;
	}

	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}

	public final boolean allowOnTransform()
	{
		return isPassive();
	}

	/**
	 * Return the power of the skill.<BR><BR>
	 */
	public final double getPower(L2Character activeChar)
	{
		if (activeChar == null)
			return _power;

		float olympiadMulti = 1;

		int finalPower = (int) _power;

		if (activeChar.getActingPlayer() != null)
		{
			if (activeChar.getActingPlayer().isInOlympiadMode())
			{
				finalPower = finalPower - _olyNerf;

				if (finalPower < 2)
					finalPower = 2;

				if (useSoulShot())
				{
					if (getSkillType() == L2SkillType.BLOW)
					{
					}
					else
						olympiadMulti = (float) 0.66;
				}
				else if (useSpiritShot())
				{
					olympiadMulti = (float) 0.8;
				}
			}
			else if (activeChar.getActingPlayer().isInSgradeZone())
			{
				if (useSoulShot())
				{
					olympiadMulti = (float) 0.7;
				}
			}
			else if (activeChar.getActingPlayer()._inEventDM && DM._started)
			{
				if (useSoulShot())
				{
					if (getSkillType() == L2SkillType.BLOW)
						olympiadMulti = (float) 0.65;
					else
						olympiadMulti = (float) 0.72;
				}
			}

			else if (activeChar.getActingPlayer()._inEventDM && NewDM._started)
			{
				if (useSoulShot())
				{
					if (getSkillType() == L2SkillType.BLOW)
						olympiadMulti = (float) 0.65;
					else
						olympiadMulti = (float) 0.72;
				}
			}
			finalPower = (int) activeChar.getActingPlayer().calcStat(Stats.SKILL_POWER_BOOST, finalPower, null, this);
		}

		switch (_skillType)
		{
			case DEATHLINK:
			{
				return finalPower * _powerMulti * olympiadMulti * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2.2) * 0.577;
				/*
				 * DrHouse: Rolling back to old formula (look below) for DEATHLINK due to this one
				 * based on logarithm is not accurate enough. Commented here because probably is a
				 * matter of just adjusting a constant if(activeChar.getCurrentHp() /
				 * activeChar.getMaxHp() > 0.005) return
				 * _power*(-0.45*Math.log(activeChar.getCurrentHp()/activeChar.getMaxHp())+1.); else
				 * return _power*(-0.45*Math.log(0.005)+1.);
				 */
			}
			case FATAL:
			{
				return finalPower * _powerMulti * 3.9 * (1 - activeChar.getCurrentHp() / activeChar.getMaxHp()) * olympiadMulti;
			}
			default:
				return finalPower * _powerMulti * olympiadMulti;
		}
	}

	public final double getPower()
	{
		return _power * _powerMulti;
	}

	public final L2SkillType[] getNegateStats()
	{
		return _negateStats;
	}

	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}

	public final int getNegateLvl()
	{
		return _negateLvl;
	}

	public final int[] getNegateId()
	{
		return _negateId;
	}

	public final int getMustNegateId()
	{
		return _mustNegateId;
	}

	public final int getMagicLevel()
	{
		return _magicLevel;
	}

	public final int getMaxNegatedEffects()
	{
		return _maxNegatedEffects;
	}

	public final int getLevelDepend()
	{
		return _levelDepend;
	}

	/**
	 * Return true if skill should ignore all resistances
	 */
	public final boolean ignoreResists()
	{
		return _ignoreResists;
	}

	/**
	 * Return the additional effect Id.<BR><BR>
	 */
	public final int getEffectId()
	{
		return _effectId;
	}

	/**
	 * Return the additional effect level.<BR><BR>
	 */
	public final int getEffectLvl()
	{
		return _effectLvl;
	}

	public final int getEffectAbnormalLvl()
	{
		return _effectAbnormalLvl;
	}

	/**
	 * Return the additional effect skill type (ex : STUN, PARALYZE,...).<BR><BR>
	 */
	public final L2SkillType getEffectType()
	{
		return _effectType;
	}

	/**
	 * @return Returns the buffDuration.
	 */
	public final int getBuffDuration()
	{
		return _buffDuration;
	}

	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange(final L2Character caster)
	{
		if (caster != null)
		{
			if (_castRange > 0 && caster instanceof L2PcInstance)
			{
				if (getFlyType() != null) // is rush skill
					return (int) (caster.calcStat(Stats.RUSH_DIST_ADD, _castRange, null, this));
			}
			else if (caster instanceof L2Attackable)
			{
				if (_castRange >= 40)
				{
					final L2CharacterAI ai = caster.getAIWithOutInitializing();
					if (ai != null && ai.getFollowTarget() != null && ai.getFollowTarget().isMoving())
						return _castRange + 50;
				}
			}
		}

		return _castRange;
	}

	public final int getMobCastRange(final L2Character caster)
	{
		if (caster != null)
		{
			if (caster instanceof L2Attackable)
			{
				if (_castRange >= 30)
				{
					final L2CharacterAI ai = caster.getAIWithOutInitializing();
					if (ai != null && ai.getFollowTarget() != null && ai.getFollowTarget().isMoving())
						return _castRange + 50;
				}
				else if (_skillRadius > 0)
					return Math.max(1, _skillRadius - 30);
			}
		}

		return _castRange;
	}

	/**
	 * @return Returns the cpConsume;
	 */
	public final int getCpConsume()
	{
		return _cpConsume;
	}

	/**
	 * @return Returns the effectRange.
	 */
	public final int getEffectRange(final L2Character caster)
	{
		if (caster != null && caster instanceof L2PcInstance)
		{
			return (int) (caster.calcStat(Stats.MAGIC_ATTACK_RANGE, _effectRange, null, this));
		}

		return _effectRange;
	}

	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return (int) (_hpConsume * _mpConsumeMulti);
	}

	/**
	 * @return Returns the id.
	 */
	public final int getId()
	{
		return _id;
	}
	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff2()
	{
		switch (_skillType)
		{
			case BLEED:
			case POISON:
			case PROC:
			case DEBUFF:
			case STUN:
			case ROOT:
			case SWITCH:
			case CONFUSION:
			case ERASE:
			case FATAL:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SPOIL:
			case WEAKNESS:
			case MANA_BY_LEVEL:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case BETRAY:
			case SOW:
			case DISARM:
			case STEAL_BUFF:
				return true;
			default:
				return false;
		}
	}
	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff()
	{
		return _isDebuff || _skillType == L2SkillType.DEBUFF;
	}

	public final boolean isPositive()
	{
		switch (_skillType)
		{
			case BUFF:
			case HEAL:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case BALANCE_LIFE:
			case HOT:
			case MANAHEAL:
			case SUPER_HEAL:
			case NEGATE:
			case CANCEL_DEBUFF:
			case MANAHEAL_PERCENT:
			case MANA_BY_LEVEL:
			case MANARECHARGE:
			case COMBATPOINTHEAL:
			case CPHEAL_PERCENT:
			case CONT:
			case CPHOT:
			case MPHOT:
			case REFLECT:
			case SHIFT_TARGET:
			case RESURRECT:
				return true;
			default:
				return false;
		}
	}

	public int getDisplayId()
	{
		return _displayId;
	}

	public int getTriggeredId()
	{
		return _triggeredId;
	}

	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}

	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}

	/**
	 * Return the skill type (ex : BLEED, SLEEP, WATER...).<BR><BR>
	 */
	public final Stats getStat()
	{
		return _stat;
	}

	/**
	 * @return Returns the _targetConsumeId.
	 */
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}

	/**
	 * @return Returns the targetConsume.
	 */
	public final int getTargetConsume()
	{
		return _targetConsume;
	}

	/**
	 * @return Returns the itemConsume.
	 */
	public final int getItemConsume()
	{
		return _itemConsume;
	}

	/**
	 * @return Returns the itemConsumeId.
	 */
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	/**
	 * @return Returns the level.
	 */
	public final int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns the magic.
	 */
	public final boolean isMagic()
	{
		return _magic;
	}

	/**
	 * @return Returns true to set static reuse.
	 */
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}

	/**
	 * @return Returns true to set static hittime.
	 */
	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}

	public final boolean isStaticPower()
	{
		return _staticPower;
	}

	/**
	 * @return Returns the mpConsume.
	 */
	public final int getMpConsume()
	{
		return (int) (_mpConsume * _mpConsumeMulti);
	}

	/**
	 * @return Returns the mpInitialConsume.
	 */
	public final int getMpInitialConsume()
	{
		return (int) (_mpInitialConsume * _mpConsumeMulti);
	}

	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the reuseDelay.
	 */
	public final int getReuseDelay()
	{
		return (int) (_reuseDelay * _reuseMulti);
	}

	public final int getReuseDelay(L2Character activeChar)
	{
		if (activeChar != null && activeChar instanceof L2PcInstance && !isPotion())
		{
			return (int) activeChar.calcStat(Stats.SKILL_REUSE_CHANGE, _reuseDelay * _reuseMulti, null, this);
		}

		return (int) (_reuseDelay * _reuseMulti);
	}

	public final int getEquipDelay()
	{
		return _equipDelay;
	}

	public final int getHitTime()
	{
		return _hitTime;
	}

	public final int getHitCounts()
	{
		return _hitTimings.length;
	}

	public final int[] getHitTimings()
	{
		return _hitTimings;
	}

	public final int getCoolTime()
	{
		return _coolTime;
	}

	public final int getSkillRadius(final L2Character activeChar)
	{
		if (activeChar != null && activeChar instanceof L2PcInstance)
		{
			if (getFlyType() != null) // is rush skill
				return (int) (activeChar.calcStat(Stats.RUSH_DIST_ADD, _skillRadius, null, this));

			return (int) (activeChar.calcStat(Stats.SKILL_RADIUS_BOOST, _skillRadius, null, this));
		}

		return _skillRadius;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	public final boolean isChance()
	{
		return _chanceCondition != null/* && isPassive()*/;
	}

	public ChanceCondition getChanceCondition()
	{
		return _chanceCondition;
	}

	public final boolean isDance()
	{
		return _isDance;
	}

	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}

	public final float getSSBoost()
	{
		return _sSBoost;
	}

	public final int getAggroPoints()
	{
		return _aggroPoints;
	}

	public final boolean useSoulShot()
	{
		switch (getSkillType())
		{
			case PDAM:
			case PDAMPERC:
			case CHARGEDAM:
			case BLOW:
			case FATAL:
			case CPDAM:
				return true;
			default:
				return false;
		}
	}

	public final boolean useSpiritShot()
	{
		return isMagic();
	}

	public final boolean useFishShot()
	{
		return ((getSkillType() == L2SkillType.PUMPING) || (getSkillType() == L2SkillType.REELING));
	}

	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	public final boolean isPvpSkill()
	{
		switch (_skillType)
		{
			case DOT:
			case BLEED:
			case CONFUSION:
			case POISON:
			case DEBUFF:
			case AGGREDUCE_CHAR:
			case AGGDEBUFF:
			case AGGDAMAGE:
			case STUN:
			case ROOT:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MUTE:
			case WEAKNESS:
			case SWITCH:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case BETRAY:
			case DISARM:
			case MANADAM:
			case STEAL_BUFF:
				return true;
			default:
			{
				if (getEffectType() == L2SkillType.DEBUFF || getEffectType() == L2SkillType.PARALYZE || getEffectType() == L2SkillType.MUTE || getEffectType() == L2SkillType.ROOT)
					return true;
			}
				return false;
		}
	}

	public final boolean isOffensive()
	{
		return _isOffensive;
	}

	public final boolean isNeutral()
	{
		return _isNeutral;
	}

	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}

	public final int getNumCharges()
	{
		return _numCharges;
	}

	public final int getNumSouls()
	{
		return _numSouls;
	}

	public final int getMaxSoulConsumeCount(L2Character activeChar)
	{
		return (int) activeChar.calcStat(Stats.SOUL_DOUBLING, _soulMaxConsume, null, this);
	}

	public final int getSoulConsumeCount()
	{
		return _soulConsume;
	}

	public final int getExpNeeded()
	{
		return _expNeeded;
	}

	public final int getCritChance()
	{
		return _critChance;
	}

	public final int getTransformId()
	{
		return _transformId;
	}

	public final int getTransformDuration()
	{
		return _transformDuration;
	}

	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}

	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}

	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}

	public final String getFlyType()
	{
		return _flyType;
	}

	public final int getFlyRadius()
	{
		return _flyRadius;
	}

	public final float getFlyCourse()
	{
		return _flyCourse;
	}

	public final boolean isSkillTypeOffensive()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case BLOW:
			case CPDAM:
			case DOT:
			case BLEED:
			case CPDAMPERCENT:
			case POISON:
			case AGGDAMAGE:
			case PROC:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case SWITCH:
			case CONFUSION:
			case ERASE:
			case FATAL:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case CONFUSE_MOB_ONLY:
			case PDAMPERC:
			case DEATHLINK:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SPOIL:
			case WEAKNESS:
			case MANA_BY_LEVEL:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case BETRAY:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case DISARM:
			case STEAL_BUFF:
			case INSTANT_JUMP:
				return true;
			default:
				return isDebuff();
		}
	}

	public final boolean is7Signs()
	{
		if (_id > 4360 && _id < 4367)
			return true;
		return false;
	}

	public final boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}

	//	int weapons[] = {L2Weapon.WEAPON_TYPE_ETC, L2Weapon.WEAPON_TYPE_BOW,
	//	L2Weapon.WEAPON_TYPE_POLE, L2Weapon.WEAPON_TYPE_DUALFIST,
	//	L2Weapon.WEAPON_TYPE_DUAL, L2Weapon.WEAPON_TYPE_BLUNT,
	//	L2Weapon.WEAPON_TYPE_SWORD, L2Weapon.WEAPON_TYPE_DAGGER};

	public final boolean getWeaponDependancy(L2Character activeChar)
	{
		if (getWeaponDependancy(activeChar, false))
		{
			return true;
		}
		else
		{
			SystemMessage message = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			message.addSkillName(this);
			activeChar.sendPacket(message);

			return false;
		}
	}

	public final boolean getWeaponDependancy(L2Character activeChar, boolean chance)
	{
		final int weaponsAllowed = getWeaponsAllowed();
		//check to see if skill has a weapon dependency.
		if (weaponsAllowed == 0)
			return true;

		int mask = 0;

		final L2Weapon wep = activeChar.getActiveWeaponItem();

		if (wep != null)
		{
			mask |= wep.getItemType().mask();

			if (wep.getItemId() == 91028) //star smashers
			{
				mask |= 1 << 9; //(9 is duals)
				mask |= 1 << 14;//(14 is bigblunt)
			}
			else if (wep.getItemId() == Config.AURAFANG)
			{
				mask |= 1 << 2; //(2 is sword)
				mask |= 1 << 4;//(4 is dagger)
				mask |= 1 << 17;//(17 is rapier)
			}
			else if (wep.getItemId() == Config.RAYBLADE)
			{
				mask |= 1 << 2; //(2 is sword)
				mask |= 1 << 3; //(3 is blunt)
				mask |= 1 << 11;//(11 is bigsword)
				mask |= 1 << 14;//(14 is bigblunt)
				mask |= 1 << 15;//(15 is ancient)
			}
			else if (wep.getItemId() == Config.WAVEBRAND)
			{
				mask |= 1 << 9; //(9 is duals)
				mask |= 1 << 18;//(18 is dualdaggers)
			}
		}

		if (activeChar.getSecondaryWeaponItem() != null)
		{
			mask |= activeChar.getSecondaryWeaponItem().getItemType().mask();
		}

		if ((mask & weaponsAllowed) != 0)
			return true;

		return false;
	}

	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if (activeChar.isGM() && !Config.GM_SKILL_RESTRICTION)
			return true;

		if ((getCondition() & L2Skill.COND_BEHIND) != 0)
		{
			if (!activeChar.isBehind(target))
				return false;
		}

		if (getId() == 12001) //love & rage
		{
			if (activeChar instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) activeChar;

				if (player != null && player.isThisCharacterMarried())
				{
					if (player.isCursedWeaponEquipped())
						return false;

					final L2PcInstance partner = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());

					if (partner != null && partner.isOnline() == 1 && !activeChar.getActingPlayer().isInDuel() && !partner.isInDuel())
					{
						if (partner.getCurrentHp() / partner.getMaxHp() > 0.25)
						{
							player.sendMessage("Your partner's HP is too high");
							return false;
						}

						if (partner.isCursedWeaponEquipped())
							return false;

						if (!Util.checkIfInRange(900, player, partner, false))
							return false;
					}
					else
						return false;
				}
				else
					return false;
			}
			else
				return false;
		}

		if ((getCondition() & L2Skill.COND_SHIELD) != 0)
		{
		}

		Condition preCondition = _preCondition;
		if (itemOrWeapon)
			preCondition = _itemPreCondition;
		if (preCondition == null)
			return true;

		Env env = new Env();
		env.player = activeChar;
		if (target instanceof L2Character) // TODO: object or char?
			env.target = (L2Character) target;
		env.skill = this;

		if (!preCondition.test(env))
		{
			String msg = preCondition.getMessage();
			int msgId = preCondition.getMessageId();
			if (msgId != 0)
			{
				SystemMessage sm = new SystemMessage(msgId);
				if (preCondition.isAddName())
					sm.addSkillName(_id);
				activeChar.sendPacket(sm);
			}
			else if (msg != null)
			{
				activeChar.sendMessage(msg);
			}
			return false;
		}
		return true;
	}

	public final L2Character[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		// Init to null the target of the skill
		L2Character target = null;

		// Get the L2Objcet targeted by the user of the skill at this moment
		L2Object objTarget = activeChar.getTarget();
		// If the L2Object targeted is a L2Character, it becomes the L2Character target
		if (objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}

		return getTargetList(activeChar, onlyFirst, target);
	}

	/**
	 * Return all targets of the skill in a table in function a the skill type.<BR><BR>
	 *
	 * <B><U> Values of skill type</U> :</B><BR><BR>
	 * <li>ONE : The skill can only be used on the L2PcInstance targeted, or on the caster if it's a L2PcInstance and no L2PcInstance targeted</li>
	 * <li>SELF</li>
	 * <li>HOLY, UNDEAD</li>
	 * <li>PET</li>
	 * <li>AURA, AURA_CLOSE</li>
	 * <li>AREA</li>
	 * <li>MULTIFACE</li>
	 * <li>PARTY, CLAN</li>
	 * <li>CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN</li>
	 * <li>UNLOCKABLE</li>
	 * <li>ITEM</li><BR><BR>
	 *
	 * @param activeChar The L2Character who use the skill
	 *
	 */
	@SuppressWarnings("incomplete-switch")
	public final L2Character[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		List<L2Character> targetList = new FastList<L2Character>();

		// Get the target type of the skill
		// (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN, UNLOCKABLE, ITEM, UNDEAD)
		SkillTargetType targetType = getTargetType(activeChar);

		// Get the type of the skill
		// (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT, RESURRECT, PASSIVE...)
		L2SkillType skillType = getSkillType();

		switch (targetType)
		{
		// The skill can only be used on the L2Character targeted, or on the caster itself
			case TARGET_ONE:
			{
				// automaticly selects caster if no target is selected (only positive skills)
				if (isPositive() && target == null)
					target = activeChar;

				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case SUPER_HEAL:
					case HEAL_STATIC:
					case HEAL_PERCENT:
					case CPHEAL_PERCENT:
					case CPHOT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
						/*case CANCEL:*/
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}//resurrection is target_corpse

				// Check for null target or any other invalid target
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					/*activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));*/
					return null;
				}

				if (!GeoData.getInstance().canSeeTarget(activeChar, target))
					return null;

				if (isOffensive())
				{
					if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAutoAttackable(activeChar))
						return null;

					if (isPvpSkill() && activeChar instanceof L2Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}

				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[] { target };
			}
			case TARGET_ONE_AND_PET:
			{
				// automaticly selects caster if no target is selected (only positive skills)
				if (isPositive() && target == null)
					target = activeChar;

				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case SUPER_HEAL:
					case HEAL_STATIC:
					case HEAL_PERCENT:
					case CPHEAL_PERCENT:
					case CPHOT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
						/*case CANCEL:*/
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}//resurrection is target_corpse

				// Check for null target or any other invalid target
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					/*activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));*/
					return null;
				}

				if (!GeoData.getInstance().canSeeTarget(activeChar, target))
					return null;

				if (isPositive())
				{
					if (target instanceof L2Summon && ((L2Summon) target).getOwner() != null && !((L2Summon) target).getOwner().isDead())
						return new L2Character[] { target, ((L2Summon) target).getOwner() };
					else if (target.getPet() != null && !target.getPet().isDead())
						return new L2Character[] { target, target.getPet() };
				}
				else if (isOffensive())
				{
					if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAutoAttackable(activeChar))
						return null;

					if (isPvpSkill() && activeChar instanceof L2Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}

				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[] { target };
			}
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new L2Character[] { activeChar };
			}
			case TARGET_SELF_AND_PET:
			{
				final L2Summon pet = activeChar.getPet();
				if (pet != null && !pet.isDead() && pet.isInsideRadius(activeChar, 1000, true, false))
					return new L2Character[] { activeChar, pet };

				return new L2Character[] { activeChar };
			}
			case TARGET_HOLY:
			{
				if (activeChar instanceof L2PcInstance)
				{
					if (((L2PcInstance) activeChar).checkFOS())
						return new L2Character[] { (L2NpcInstance) target };
					else if (target instanceof L2ArtefactInstance)
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_FLAGPOLE:
			{
				if (target != null && activeChar instanceof L2PcInstance)
				{
					if (activeChar.getActingPlayer().checkIfOkToCastFlagDisplay(FortManager.getInstance().getFort(activeChar), this, target))
						return new L2Character[] { target };
				}
				return _emptyTargetList;
			}
			case TARGET_TRAP:
			{
				if (target != null && target instanceof L2TrapInstance)
				{
					return new L2Character[] { target };
				}
				return _emptyTargetList;
			}
			case TARGET_PET:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead())
					return new L2Character[] { target };

				return _emptyTargetList;
			}
			case TARGET_SUMMON:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead() && target instanceof L2SummonInstance)
					return new L2Character[] { target };

				return _emptyTargetList;
			}
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof L2Summon)
				{
					target = ((L2Summon) activeChar).getOwner();
					if (target != null && !target.isDead())
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar instanceof L2PcInstance)
				{
					target = activeChar.getPet();
					if (target != null && target.isDead())
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_AURA:
			{
				final int radius = getSkillRadius(activeChar);
				final L2PcInstance src = activeChar.getActingPlayer();

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				if (src != null && src._inEventDM && NewDM._started)
					size = 3;
				
				// Go through the L2Character _knownList
				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playable and exists
						{
							if (obj instanceof L2Playable)
							{
								final L2PcInstance player = obj.getActingPlayer();

								if (src == player)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, (L2Character) activeChar.getTarget(), obj))
								continue;
						}

						targetList.add(obj);

						if (onlyFirst)
							break;
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_ALL:
			{
				final int radius = getSkillRadius(activeChar);
				final L2PcInstance src = activeChar.getActingPlayer();

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				// Go through the L2Character _knownList
				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj.isDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof L2Playable)
							{
								final L2PcInstance player = obj.getActingPlayer();

								if (!player.isAutoAttackableTargetAll(src))
									continue;
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, (L2Character) activeChar.getTarget(), obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				targetList.add(activeChar);

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_FRONT_AURA:
			{
				final int radius = getSkillRadius(activeChar);
				final L2PcInstance src = activeChar.getActingPlayer();

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				// Go through the L2Character _knownList
				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!activeChar.isFacing(obj, getAreaAngle(activeChar)))
							continue;

						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof L2Playable)
							{
								L2PcInstance player = obj.getActingPlayer();

								if (src == player)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, (L2Character) activeChar.getTarget(), obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_BEHIND_AURA:
			{
				final int radius = getSkillRadius(activeChar);
				final L2PcInstance src = activeChar.getActingPlayer();

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				// Go through the L2Character _knownList
				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable || obj instanceof L2DoorInstance))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!obj.isBehind(activeChar, getAreaAngle(activeChar)))
							continue;

						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof L2Playable)
							{
								L2PcInstance player = obj.getActingPlayer();

								if (src == player)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, (L2Character) activeChar.getTarget(), obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA:
			{
				if (target == null || target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if (isOffensive())
				{
					if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAutoAttackable(activeChar))
						return null;

					if (isPvpSkill() && activeChar instanceof L2Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}

				L2Character cha = target;

				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new L2Character[] { cha };

				final L2PcInstance src = activeChar.getActingPlayer();

				final int radius = getSkillRadius(activeChar);

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 2;

				for (L2Character obj : cha.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || (obj == activeChar && getAreaInclude() < 2) || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!GeoData.getInstance().canSeeTarget(cha, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playable and exists
						{
							if (obj instanceof L2Playable)
							{
								L2PcInstance player = obj.getActingPlayer();

								if (src == player && getAreaInclude() < 2)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, getAreaInclude() == 1 ? true : false))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof L2Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				if (targetList.size() == 0)
					return null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_FRONT_AREA:
			{
				if (target == null || target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if (isOffensive())
				{
					if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAutoAttackable(activeChar))
						return null;

					if (isPvpSkill() && activeChar instanceof L2Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}

				L2Character cha = target;

				// Make sure that char is facing selected target
				if (target != activeChar)
					activeChar.setHeading(Util.calculateHeadingFrom(activeChar, target));

				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new L2Character[] { cha };

				final L2PcInstance src = activeChar.getActingPlayer();

				int radius = getSkillRadius(activeChar);

				if (_areaLimit) //this is a target_front_area skill and the skill stops where the target is
				{ //fix for having charge skills hitting farther than selected target
					radius = (int) Math.min(radius, Util.calculateDistance(activeChar, cha, false));
				}

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || obj == activeChar || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!activeChar.isFacing(obj, getAreaAngle(activeChar)))
							continue;

						if (!GeoData.getInstance().canSeeTarget(cha, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof L2Playable)
							{
								L2PcInstance player = obj.getActingPlayer();

								if (src == player)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof L2Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				if (targetList.size() == 0)
					return null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_BEHIND_AREA:
			{
				if (target == activeChar || target.isDead()) // target is null or self or dead/faking
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if (isOffensive())
				{
					if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAutoAttackable(activeChar))
						return null;

					if (isPvpSkill() && activeChar instanceof L2Playable)
					{
						if (!activeChar.getActingPlayer().checkPvpSkill(target, this, false))
							return null;
					}
				}

				L2Character cha;

				cha = target;

				// Make sure that char is facing selected target
				if (target != activeChar)
					activeChar.setHeading(Util.calculateHeadingFrom(activeChar, target));

				if (!onlyFirst)
					targetList.add(cha); // Add target to target list
				else
					return new L2Character[] { cha };

				final L2PcInstance src = activeChar.getActingPlayer();

				final int radius = getSkillRadius(activeChar);

				int newHeading = getNewHeadingToTarget(activeChar, cha);

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				for (L2Character obj : cha.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == cha || obj == activeChar || obj.isAlikeDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (Util.checkIfInRange(getCastRange(activeChar), activeChar, obj, true))
							continue;

						if (isBehindFromCaster(newHeading, cha, obj))
							continue;

						if (!GeoData.getInstance().canSeeTarget(cha, obj))
							continue;

						if (obj.calcStat(Stats.EVADE_AOE_SPELL, 0, null, null) > Rnd.get(100))
							continue;

						if (src != null) // caster is l2playableinstance and exists
						{
							if (obj instanceof L2Playable)
							{
								L2PcInstance player = obj.getActingPlayer();

								if (src == player)
									continue;

								if (getAreaInclude() == 0)
								{
									if (!src.checkAOEPvPSkill(player, this))
										continue;
								}
								else
								{
									if (!player.isAutoAttackableTargetAll(src, true))
										continue;
								}
							}
							else if (src.isInDuel() || cha instanceof L2Playable || !src.canAttackDueToSoloMob(obj))
								continue;
						}
						else
						// Skill user is not L2PlayableInstance
						{
							if (!(obj instanceof L2Playable))
								continue;

							if (!canBeIncludedAsAOETarget(activeChar, cha, obj))
								continue;
						}

						targetList.add(obj);
					}
				}

				if (targetList.size() == 0)
					return null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_MULTIFACE:
			{
				if ((!(target instanceof L2Attackable) && !(target instanceof L2PcInstance)))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				if (!onlyFirst)
					targetList.add(target);
				else
					return new L2Character[] { target };

				int radius = getSkillRadius(activeChar);

				Collection<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(radius);
				//synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Character obj : objs)
					{
						if (obj instanceof L2Attackable && obj != target)
							targetList.add(obj);

						if (targetList.isEmpty())
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
							return _emptyTargetList;
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
				//TODO multiface targets all around right now.  need it to just get targets
				//the character is facing.
			}
			case TARGET_PARTY:
			{
				if (onlyFirst)
					return new L2Character[] { activeChar };

				targetList.add(activeChar);

				if (activeChar instanceof L2Npc && ((L2Npc) activeChar).getFactionId() != null)
				{
					final String faction = ((L2Npc) activeChar).getFactionId();

					for (L2Character chara : activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius(activeChar)))
					{
						if (chara == null || !(chara instanceof L2Npc) || chara == activeChar)
							continue;

						L2Npc npc = (L2Npc) chara;

						if (npc.getFactionId() != null && npc.getFactionId().equalsIgnoreCase(faction))
							targetList.add(chara);
					}
				}
				else
				{
					L2PcInstance player = null;

					if (activeChar instanceof L2Summon)
					{
						player = ((L2Summon) activeChar).getOwner();
						targetList.add(player);
					}
					else if (activeChar instanceof L2PcInstance)
					{
						player = (L2PcInstance) activeChar;
						if (activeChar.getPet() != null)
							targetList.add(activeChar.getPet());
					}

					if (player != null && (player.isInDuel() || player.isInHuntersVillage() || player.isInOrcVillage()))
						return targetList.toArray(new L2Character[targetList.size()]);

					if (activeChar.getParty() != null)
					{
						final boolean isInPeace = activeChar.getActingPlayer().isInsideZone(L2Character.ZONE_PEACE);

						// Get all visible objects in a spherical area near the L2Character
						// Get a list of Party Members
						List<L2PcInstance> partyList = activeChar.getParty().getPartyMembers();

						for (L2PcInstance partyMember : partyList)
						{
							if (player == null || partyMember == null || partyMember == player || partyMember.isDead())
								continue;

							if (partyMember.isInDuel())
								continue;

							if (isInPeace && !partyMember.isInsideZone(L2Character.ZONE_PEACE))
								continue;

							if (partyMember.isInFunEvent())
							{
								if (activeChar.isInFunEvent())
								{
									if (partyMember.CanAttackDueToInEvent(activeChar))
									{
										continue;
									}
								}
								else
									continue;
							}
							else if (activeChar.isInFunEvent())
							{
								continue;
							}

							if (!partyMember.isDead() && Util.checkIfInRange(getSkillRadius(activeChar), activeChar, partyMember, true))
							{
								targetList.add(partyMember);

								if (partyMember.getPet() != null && !partyMember.getPet().isDead())
									targetList.add(partyMember.getPet());
							}
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if (!(activeChar instanceof L2Playable))
				{
					if (target != null && !target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new L2Character[] { target };
					}

					return _emptyTargetList;
				}

				if ((target != null && target == activeChar)
						|| (target != null && activeChar.getParty() != null && target.getParty() != null && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
						|| (target != null && activeChar instanceof L2PcInstance && target instanceof L2Summon && activeChar.getPet() == target)
						|| (target != null && activeChar instanceof L2Summon && target instanceof L2PcInstance && activeChar == target.getPet()))
				{
					if (!target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new L2Character[] { target };
					}

					activeChar.sendMessage("Must target yourself or another player in your party.");
					return _emptyTargetList;
				}
				else
				{
					if (target == null)
					{
						// automaticly selects caster if no target is selected (only positive skills)
						if (isPositive() && skillType != L2SkillType.RESURRECT)
							return new L2Character[] { activeChar };
					}
				}

				activeChar.sendMessage("Must target yourself or another player in your party.");
				return null;
			}
			case TARGET_PARTY_OTHER:
			{
				if (!(activeChar instanceof L2Playable))
				{
					if (target != null && !target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new L2Character[] { target };
					}

					return _emptyTargetList;
				}

				if (target != null && target != activeChar && activeChar.getParty() != null && target.getParty() != null
						&& activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
				{
					if (!target.isDead())
					{
						if (target instanceof L2PcInstance)
						{
							L2PcInstance player = (L2PcInstance) target;
							switch (getId())
							{
							// FORCE BUFFS may cancel here but there should be a proper condition
								case 426:
									if (!player.isMageClass())
										return new L2Character[] { target };
									else
										return _emptyTargetList;
								case 427:
									if (player.isMageClass())
										return new L2Character[] { target };
									else
										return _emptyTargetList;
							}
						}
						return new L2Character[] { target };
					}
					else
						return _emptyTargetList;
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
			}
			case TARGET_COUPLE:
			{
				final L2PcInstance player = (L2PcInstance) activeChar;

				if (player != null && player.isThisCharacterMarried())
				{
					if (player.isCursedWeaponEquipped())
						return null;

					final L2PcInstance partner = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());

					if (partner != null && partner.isOnline() == 1 && (!partner.isDead() || (getSkillType() == L2SkillType.RESURRECT && partner.isDead())) && !activeChar.getActingPlayer().isInDuel()
							&& !partner.isInDuel())
					{
						if (partner.isCursedWeaponEquipped())
							return null;

						if (Util.checkIfInRange(1600, player, partner, false))
							return new L2Character[] { partner };
					}
				}
				return null;
			}
			case TARGET_SELF_AND_COUPLE:
			{
				final L2PcInstance player = (L2PcInstance) activeChar;

				if (player != null && player.isThisCharacterMarried())
				{
					if (player.isCursedWeaponEquipped())
						return null;

					final L2PcInstance partner = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());

					if (partner != null && partner.isOnline() == 1 && !partner.isDead() && !activeChar.getActingPlayer().isInDuel() && !partner.isInDuel())
					{
						if (partner.isCursedWeaponEquipped())
							return null;

						if (Util.checkIfInRange(getSkillRadius(activeChar), player, partner, false))
							return new L2Character[] { player, partner };
					}
				}

				return null;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				if (activeChar instanceof L2Npc && ((L2Npc) activeChar).getFactionId() != null)
				{
					targetList.add(activeChar);

					final String faction = ((L2Npc) activeChar).getFactionId();

					for (L2Character chara : activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius(activeChar)))
					{
						if (chara == null || !(chara instanceof L2Npc) || chara == activeChar)
							continue;

						L2Npc npc = (L2Npc) chara;

						if (npc.getFactionId() != null && npc.getFactionId().equalsIgnoreCase(faction))
							targetList.add(chara);
					}
				}
				else

				if (activeChar instanceof L2PcInstance)
				{
					int radius = getSkillRadius(activeChar);
					L2PcInstance player = (L2PcInstance) activeChar;
					L2Clan clan = player.getClan();

					if (player.isInOlympiadMode())
					{
						if (player.getPet() == null)
							return new L2Character[] { player };

						return new L2Character[] { player, player.getPet() };
					}

					if (targetType != SkillTargetType.TARGET_CORPSE_ALLY)
					{
						if (!onlyFirst)
							targetList.add(player);
						else
							return new L2Character[] { player };
					}

					if (activeChar.getPet() != null)
					{
						if ((targetType != SkillTargetType.TARGET_CORPSE_ALLY) && !(activeChar.getPet().isDead()))
							targetList.add(activeChar.getPet());
					}

					if (player != null && (player.isInDuel() || player.isInHuntersVillage()))
						return targetList.toArray(new L2Character[targetList.size()]);

					if (clan != null)
					{
						final boolean isInPeace = player.isInsideZone(L2Character.ZONE_PEACE);
						// Get all visible objects in a spherical area near the L2Character
						// Get Clan Members
						Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
						{
							for (L2Object obj : objs)
							{
								if (targetList.size() < 45)
								{
									if (obj == player || !(obj instanceof L2Playable) || obj.getActingPlayer() == null)
										continue;

									final L2PcInstance newTarget = obj.getActingPlayer();

									if ((newTarget.getAllyId() == 0 || newTarget.getAllyId() != player.getAllyId()) && (newTarget.getClan() == null || newTarget.getClanId() != player.getClanId()))
										continue;

									if (newTarget.isInDuel())
										continue;

									if (newTarget.isInFunEvent())
									{
										if (activeChar.isInFunEvent())
										{
											if (newTarget.CanAttackDueToInEvent(activeChar))
											{
												continue;
											}
										}
										else
											continue;
									}
									else if (activeChar.isInFunEvent())
									{
										continue;
									}

									if (isInPeace && !newTarget.isInsideZone(L2Character.ZONE_PEACE))
										continue;

									L2Summon pet = newTarget.getPet();
									if (pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst
											&& ((targetType == SkillTargetType.TARGET_CORPSE_ALLY && pet.isDead()) || (targetType == SkillTargetType.TARGET_ALLY && !pet.isDead()))
									/*											&& player.checkPvpSkill(newTarget, this)*/
									)
										targetList.add(pet);

									if (targetType == SkillTargetType.TARGET_CORPSE_ALLY)
									{
										if (!newTarget.isDead())
											continue;
										// Siege battlefield resurrect has been made possible for participants
										if (getSkillType() == L2SkillType.RESURRECT)
										{
											if (newTarget.isInsideZone(L2Character.ZONE_SIEGE) && !newTarget.isInSiege())
												continue;
										}
									}
									else
									{
										if (newTarget.isDead())
											continue;
									}

									if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
										continue;

									/*									// Don't add this target if this is a Pc->Pc pvp
										// casting and pvp condition not met
										if (!player.checkPvpSkill(newTarget, this))
											continue;*/
									if (obj instanceof L2PcInstance && obj!=null && ((L2PcInstance) obj).eventSitForced && ((L2PcInstance) obj).isInKoreanEvent())
										continue;
									if (!onlyFirst)
										targetList.add(newTarget);
									else
										return new L2Character[] { newTarget };
								}
							}
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_CLAN:
			case TARGET_CLAN:
			{
				if (activeChar instanceof L2Npc && ((L2Npc) activeChar).getFactionId() != null)
				{
					targetList.add(activeChar);

					final String faction = ((L2Npc) activeChar).getFactionId();

					for (L2Character chara : activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius(activeChar)))
					{
						if (chara == null || !(chara instanceof L2Npc) || chara == activeChar)
							continue;

						L2Npc npc = (L2Npc) chara;

						if (npc.getFactionId() != null && npc.getFactionId().equalsIgnoreCase(faction))
							targetList.add(chara);
					}
				}
				else if (activeChar instanceof L2Playable)
				{
					int radius = getSkillRadius(activeChar);
					L2PcInstance player = null;
					if (activeChar instanceof L2Summon)
						player = ((L2Summon) activeChar).getOwner();
					else
						player = (L2PcInstance) activeChar;
					if (player == null)
						return _emptyTargetList;
					L2Clan clan = player.getClan();

					if (player.isInOlympiadMode())
					{
						if (player.getPet() == null)
							return new L2Character[] { player };

						return new L2Character[] { player, player.getPet() };
					}

					if (targetType != SkillTargetType.TARGET_CORPSE_CLAN)
					{
						if (!onlyFirst)
							targetList.add(player);
						else
							return new L2Character[] { player };
					}

					if (activeChar.getPet() != null)
					{
						if ((targetType != SkillTargetType.TARGET_CORPSE_CLAN) && !(activeChar.getPet().isDead()))
							targetList.add(activeChar.getPet());
					}

					if (player != null && (player.isInDuel() || player.isInHuntersVillage()))
						return targetList.toArray(new L2Character[targetList.size()]);

					if (clan != null)
					{
						final boolean isInPeace = player.isInsideZone(L2Character.ZONE_PEACE);

						// Get all visible objects in a spheric area near the L2Character
						// Get Clan Members
						for (L2ClanMember member : clan.getMembers())
						{
							L2PcInstance newTarget = member.getPlayerInstance();

							if (newTarget == null || newTarget == player || newTarget.isInDuel())
								continue;

							if (newTarget.isInFunEvent())
							{
								if (activeChar.isInFunEvent())
								{
									if (newTarget.CanAttackDueToInEvent(activeChar))
									{
										continue;
									}
								}
								else
									continue;
							}
							else if (activeChar.isInFunEvent())
							{
								continue;
							}

							if (isInPeace && !newTarget.isInsideZone(L2Character.ZONE_PEACE))
								continue;

							if (newTarget.getPet() != null)
								if (Util.checkIfInRange(radius, activeChar, newTarget.getPet(), true))
									if ((targetType != SkillTargetType.TARGET_CORPSE_CLAN) && !(newTarget.getPet().isDead())
									/*			&& player.checkPvpSkill(newTarget, this)*/
									&& !onlyFirst)
										targetList.add(newTarget.getPet());

							if (targetType == SkillTargetType.TARGET_CORPSE_CLAN)
							{
								if (getSkillType() == L2SkillType.RESURRECT)
								{
									// check target is not in a active siege zone
									if (newTarget.isInsideZone(L2Character.ZONE_SIEGE) && newTarget.getSiegeState() == 0)
										continue;
								}
							}
							else
							{
								if (newTarget.isDead())
									continue;
							}

							if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
								continue;

							/*                            // Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							    if (!player.checkPvpSkill(newTarget, this)) continue;*/

							if (!onlyFirst)
								targetList.add(newTarget);
							else
								return new L2Character[] { newTarget };

						}
					}
				}
				/*		else if (activeChar instanceof L2Npc)
				{
					// for buff purposes, returns one unbuffed friendly mob nearby or mob itself?
					L2Npc npc = (L2Npc) activeChar;
					Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					//synchronized (activeChar.getKnownList().getKnownObjects())
					{
						for (L2Object newTarget : objs)
						{
							if (newTarget instanceof L2Npc
									&& ((L2Npc) newTarget).getFactionId() == npc.getFactionId())
							{
								if (!Util.checkIfInRange(getCastRange(activeChar), activeChar, newTarget, true))
									continue;
								if (((L2Npc) newTarget).getFirstEffect(this) != null)
								{
									targetList.add((L2Npc) newTarget);
									break;
								}
							}
						}
					}
					if (targetList.isEmpty())
					{
						targetList.add(activeChar);
					}
				}*/

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_PLAYER:
			{
				if (target != null && target.isDead())
				{
					L2PcInstance player = null;
					if (activeChar instanceof L2PcInstance)
						player = (L2PcInstance) activeChar;

					L2PcInstance targetPlayer = null;
					if (target instanceof L2PcInstance)
						targetPlayer = (L2PcInstance) target;

					L2PetInstance targetPet = null;
					if (target instanceof L2PetInstance)
						targetPet = (L2PetInstance) target;

					if (player != null && (targetPlayer != null || targetPet != null))
					{
						boolean condGood = true;

						if (getSkillType() == L2SkillType.RESURRECT)
						{
							/*//check target is not in a active siege zone
									Castle castle = null;
									
									if (targetPlayer != null)
										castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
									else
										castle = CastleManager.getInstance().getCastle(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
									
									if (castle != null && castle.getSiege().getIsInProgress())
									{
										condGood = false;
										activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
									}*/
							
							if (targetPlayer != null)
							{
								if (targetPlayer.isInsideZone(L2Character.ZONE_SIEGE) && !targetPlayer.isInSiege())
								{
									condGood = false;
									activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
								}

								if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
								{
									condGood = false;
									activeChar.sendMessage("You may not resurrect participants in a festival.");
								}
								if (targetPlayer.isReviveRequested())
								{
									if (targetPlayer.isRevivingPet())
										player.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
									else
										player.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									if (targetPet.getOwner().isReviveRequested())
									{
										if (targetPet.getOwner().isRevivingPet())
											player.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
										else
											player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
										condGood = false;
									}
								}
							}
						}
						if (targetPlayer.getPet() != null)
						{
						targetPlayer.getPet().doRevive();
						targetPlayer.getPet().teleToLocation(targetPlayer.getPet().getOwner().getX(), targetPlayer.getPet().getOwner().getY(), targetPlayer.getPet().getOwner().getZ(), true);
						}
						if (condGood)
						{
							if (!onlyFirst)
							{
								targetList.add(target);
								return targetList.toArray(new L2Character[targetList.size()]);
							}
							else
								return new L2Character[] { target };

						}
					}
				}

				return _emptyTargetList;
			}
			case TARGET_CORPSE_DRAIN:
			{
				if (target != null && target.isDead())
				{
					L2PcInstance player = null;

					if (activeChar instanceof L2PcInstance)
						player = (L2PcInstance) activeChar;

					if (player != null)
					{
						if (target.isAutoAttackable(player))
							return new L2Character[] { target };
					}
				}

				return null;
			}
			case TARGET_CORPSE_DRAIN_AOE:
			{
				final int radius = getSkillRadius(activeChar);
				final L2PcInstance src = activeChar.getActingPlayer();

				int size = activeChar.isGM() ? 300 : 20;

				if (src != null && src._inEventDM && DM._started)
					size = 3;

				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if (targetList.size() < size)
					{
						if (obj == null || obj == activeChar || !obj.isDead())
							continue;

						if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
							continue;

						if (obj instanceof L2GuardInstance)
							continue;

						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						if (obj.isAutoAttackable(activeChar))
							targetList.add(obj);
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_MOB:
			{
				if (!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				// Corpse mob only available for half time
				switch (getSkillType())
				{
					case DRAIN:
					case SUMMON:
					{
						if (DecayTaskManager.getInstance().getTasks().containsKey(target)
								&& (System.currentTimeMillis() - DecayTaskManager.getInstance().getTasks().get(target)) > DecayTaskManager.ATTACKABLE_DECAY_TIME / 2)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED));
							return _emptyTargetList;
						}
					}
				}

				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Character[targetList.size()]);
				}
				else
					return new L2Character[] { target };

			}
			case TARGET_AREA_CORPSE_MOB:
			{
				if ((!(target instanceof L2Attackable)) || !target.isDead())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				if (!onlyFirst)
					targetList.add(target);
				else
					return new L2Character[] { target };

				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
				L2PcInstance src = null;
				if (activeChar instanceof L2PcInstance)
					src = (L2PcInstance) activeChar;
				L2PcInstance trg = null;

				final int radius = getSkillRadius(activeChar);
				Collection<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(radius);
				//synchronized (activeChar.getKnownList().getKnownObjects())
				{
					for (L2Character obj : objs)
					{
						if (!(obj instanceof L2Attackable || obj instanceof L2Playable) || obj.isDead() || obj == activeChar)
							continue;

						/*if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
									continue;*/

						if (obj instanceof L2PcInstance && src != null)
						{
							trg = (L2PcInstance) obj;

							if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								continue;

							if (trg.isInsideZone(L2Character.ZONE_PEACE))
								continue;

							if (!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
									continue;

								if (src.getClan() != null && trg.getClan() != null)
								{
									if (src.getClan().getClanId() == trg.getClan().getClanId())
										continue;
								}
							}
						}
						if (obj instanceof L2Summon && src != null)
						{
							trg = ((L2Summon) obj).getOwner();

							if ((src.getParty() != null && trg.getParty() != null) && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								continue;

							if (!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if (src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
									continue;

								if (src.getClan() != null && trg.getClan() != null)
								{
									if (src.getClan().getClanId() == trg.getClan().getClanId())
										continue;
								}

								/*								if (!src.checkPvpSkill(trg, this))
											continue;*/
							}

							if (((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
								continue;
						}
						if (!GeoData.getInstance().canSeeTarget(activeChar, obj))
							continue;

						targetList.add(obj);
					}
				}

				if (targetList.isEmpty())
					return _emptyTargetList;
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				if (!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
				{
					//activeChar.sendPacket(new SystemMessage(SystemMessage.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Character[targetList.size()]);
				}
				else
					return new L2Character[] { target };

			}
			case TARGET_UNDEAD:
			{
				if (target instanceof L2Npc || target instanceof L2SummonInstance)
				{
					if (!target.isUndead() || target.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						return _emptyTargetList;
					}

					if (!onlyFirst)
						targetList.add(target);
					else
						return new L2Character[] { target };

					return targetList.toArray(new L2Character[targetList.size()]);
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
			}
			case TARGET_AREA_UNDEAD:
			{
				L2Character cha;
				int radius = getSkillRadius(activeChar);
				if (getCastRange(activeChar) >= 0 && (target instanceof L2Npc || target instanceof L2SummonInstance) && target.isUndead() && !target.isAlikeDead())
				{
					cha = target;

					if (!onlyFirst)
						targetList.add(cha); // Add target to target list
					else
						return new L2Character[] { cha };

				}
				else
					cha = activeChar;

				Collection<L2Character> objs = cha.getKnownList().getKnownCharactersInRadius(radius);
				//synchronized (cha.getKnownList().getKnownObjects())
				{
					for (L2Character obj : objs)
					{
						if (obj instanceof L2Npc)
							target = obj;
						else if (obj instanceof L2SummonInstance)
							target = obj;
						else
							continue;

						if (!target.isAlikeDead()) // If target is not
						// dead/fake death and not
						// self
						{
							if (!target.isUndead())
								continue;

							if (!GeoData.getInstance().canSeeTarget(activeChar, target))
								continue;

							if (!onlyFirst)
								targetList.add(obj);
							else
								return new L2Character[] { obj };
						}
					}
				}

				if (targetList.isEmpty())
					return _emptyTargetList;
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_ENEMY_SUMMON:
			{
				if (target instanceof L2Summon)
				{
					L2Summon targetSummon = (L2Summon) target;
					if (activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead()
							&& (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0)
							|| (targetSummon.getOwner().isInsideZone(L2Character.ZONE_PVP) && ((L2PcInstance) activeChar).isInsideZone(L2Character.ZONE_PVP)))
						return new L2Character[] { targetSummon };
				}
				return _emptyTargetList;
			}

			default:
			{
				activeChar.sendMessage("Target type of skill is not currently handled");
				return _emptyTargetList;
			}
		}//end switch
		
	}

	private static boolean canBeIncludedAsAOETarget(L2Character activeChar, L2Character mainTarget, L2Character obj)
	{
		if (activeChar == null || obj == null)
			return false;

		if (!(obj instanceof L2Playable) && !(obj instanceof L2Decoy))
			return false;

		if (mainTarget == null)
			return true;

		if (!(mainTarget instanceof L2Playable) && !(mainTarget instanceof L2Decoy))
			return false;

		if (mainTarget.getActingPlayer() == obj.getActingPlayer())
			return true;

		if ((activeChar instanceof L2RaidBossInstance && activeChar.getLevel() > 91) || mainTarget.getInstanceId() > 0)
			return true;

		final L2PcInstance targetPlayer = mainTarget.getActingPlayer();

		if (targetPlayer.getInSameClanAllyAs(obj.getActingPlayer()) > 0)
			return true;

		if (targetPlayer.getParty() != null && targetPlayer.getParty().getPartyMembers().contains(obj.getActingPlayer()))
			return true;

		return false;
	}

	public final L2Character[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}

	public final L2Character getFirstOfTargetList(L2Character activeChar)
	{
		L2Character[] targets;

		targets = getTargetList(activeChar, true);

		if (targets == null || targets.length == 0)
			return null;
		else
			return targets[0];
	}

	/*
	 * Check if should be target added to the target list
	 * false if target is dead, target same as caster,
	 * target inside peace zone, target in the same party with caster,
	 * caster can see target
	 * Additional checks if not in PvP zones (arena, siege):
	 * target in not the same clan and alliance with caster,
	 * and usual skill PvP check.
	 * If TvT event is active - performing additional checks.
	 * 
	 * Caution: distance is not checked.
	     
	    public static final boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena)
	    {
	    	if (target == null || target.isDead() || target == caster)
	    		return false;
	    	
	    	boolean geoCanSeeTarget = geoEnabled ? GeoData.getInstance().canSeeTarget(caster, target) : true;

	    	final L2PcInstance player = caster.getActingPlayer();
	    	if (player != null)
	    	{
	    		final L2PcInstance targetPlayer = target.getActingPlayer();
	    		if (targetPlayer != null)
	    		{
	    			if (targetPlayer == caster || targetPlayer == player)
	    				return false;

	    			if (targetPlayer.inObserverMode())
	    				return false;
	    			
	    			if(NexusEvents.isInEvent(player))
					{
						if(!NexusEvents.isInEvent(targetPlayer))
							return false;
						
						if(!NexusEvents.isSkillNeutral(player, skill))
						{
							if(NexusEvents.isSkillOffensive(player, skill) && !NexusEvents.canAttack(player, targetPlayer))
								return false;
							
							if(!NexusEvents.isSkillOffensive(player, skill) && !NexusEvents.canSupport(player, targetPlayer))
								return false;
						}
						
						return geoCanSeeTarget;
					}

	    			if (target.isInsideZone(L2Character.ZONE_PEACE))
	    				return false;

	    			if ((player.getParty() != null && targetPlayer.getParty() != null)
	    			        && player.getParty().getPartyLeaderOID() == targetPlayer.getParty().getPartyLeaderOID())
	    				return false;

	    			if (!TvTEvent.checkForTvTSkill(player, targetPlayer, skill))
	    				return false;

	    			if (!sourceInArena && !(targetPlayer.isInsideZone(L2Character.ZONE_PVP) && !targetPlayer.isInsideZone(L2Character.ZONE_SIEGE)))
	    			{
	    				if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId())
	    					return false;

	    				if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId())
	    					return false;

	    				if (!player.checkPvpSkill(targetPlayer, skill))
	    					return false;
	    			}
	    		}
	    	}
	    	else
	    	{
	    		// source is not playable
	    		if (!(target instanceof L2Playable))
	    				return false;
	    	}

	    	if (!geoCanSeeTarget)
				return false;

			return true;
	    }*/

	public static final boolean addSummon(L2Character caster, L2PcInstance owner, int radius, boolean isDead)
	{
		final L2Summon summon = owner.getPet();

		if (summon == null)
			return false;

		return addCharacter(caster, summon, radius, isDead);
	}

	public static final boolean addCharacter(L2Character caster, L2Character target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
			return false;

		if (radius > 0 && !Util.checkIfInRange(radius, caster, target, true))
			return false;

		return true;
	}

	public final Func[] getStatFuncs(L2Effect effect, L2Character player)
	{
		if (!(player instanceof L2Playable) && !(player instanceof L2Attackable))
			return _emptyFunctionSet;

		if (_funcTemplates == null)
			return _emptyFunctionSet;

		List<Func> funcs = new FastList<Func>();

		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.skill = this;
			Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;

		return funcs.toArray(new Func[funcs.size()]);
	}

	public boolean hasEffects()
	{
		return (_effectTemplates != null && _effectTemplates.length > 0);
	}

	public EffectTemplate[] getEffectTemplates()
	{
		return _effectTemplates;
	}

	public final L2Effect[] getEffects(L2Character effector, L2Character effected, Env env)
	{
		return getEffects(effector, effected, env, false);
	}

	/**
	 * Env is used to pass parameters for secondary effects (shield and ss/bss/bsss)
	 * 
	 * @return an array with the effects that have been added to effector
	 */
	public final L2Effect[] getEffects(L2Character effector, L2Character effected, Env env, boolean ignoreInvul)
	{
		if (isPassive())
			return _emptyEffectSet;

		if (_effectTemplates == null)
			return _emptyEffectSet;

		// doors and siege flags cannot receive any effects
		if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance)
			return _emptyEffectSet;

		if (!ignoreInvul && getId() != 10011)
		{
			if (effector != effected && !(effector instanceof L2BufferInstance))
			{
				if (effected.isInvul() || effected.isPreventedFromReceivingBuffs())
					return _emptyEffectSet;
			}
		}

		if (isOffensive() && effected.isPreventedFromReceivingDebuffs())
			return _emptyEffectSet;

		List<L2Effect> effects = new FastList<L2Effect>();

		if (env == null)
			env = new Env();

		env.skillMastery = Formulas.calcSkillMastery(effector, this);
		env.player = effector;
		env.target = effected;
		env.skill = this;

		boolean sent = false;

		final boolean moreThanOneEffect = _effectTemplates.length > 1;

		List<String> _success = new ArrayList<>();
		
		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;

			double rate = 0;
			
			if (isOffensive())
			{
				if (env.player instanceof L2Playable && env.target instanceof L2Playable && env.player != env.target)
				{
					if (!env.player.getActingPlayer().isGM())
					{
						if (!env.target.getActingPlayer().isDebuffable(env.player.getActingPlayer()))
						{
							success = false;
						}
					}
				}

				if (success)
				{
					if (et.effectPower > -1)
					{
						rate = Formulas.calcEffectSuccessChance(effector, effected, et, this, env.shld);
						if (rate <= 0)
						{
							success = false;
						}
						else
						{
							success = Rnd.get(100) < rate;
						}
					}
				}
			}
			
			if (success)
			{
				L2Effect e = et.getEffect(env);

				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}


				
				if (!sent && e.getSendMessage() && !e.isSelfEffect())
				{
					if (isPositive())
						sent = true;

					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);

					if (moreThanOneEffect && !isPositive())
					{
							sm.addString(getName() + "'s " + et.funcName);
							if (effector instanceof L2PcInstance && !(et.getEffectType().equals(L2SkillType.BUFF)))
							{
								_success.add("Success: " + et.funcName + " " + "("+rate+"%)");
							}
					}
					else
					{
						sm.addSkillName(this);
					}
					env.target.sendPacket(sm);
				}
			}
			else if (effector instanceof L2PcInstance)
			{
				if (et.msg)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					
					sm.addCharName(effected);
					if (moreThanOneEffect)
					{
						sm.addString(getName() + "'s " + et.funcName);
						if (effector instanceof L2PcInstance && !(et.getEffectType().equals(L2SkillType.BUFF)))
						{
							_success.add("Failed: " + et.funcName + " " + "("+rate+"%)");
						}
					}
					else
					{
						sm.addSkillName(this);
					}

					((L2PcInstance) effector).sendPacket(sm);
				}
			}
		}
		
		if (!_success.isEmpty())
		{
			SystemMessage sm;

			//effector.sendMessage("--> "+this.getName());
			//effected.sendMessage("<-- "+this.getName());
			
			for(String str : _success)
			{
				if (str.contains("Success:"))
				{
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_SUCCESS);
					sm.addString(">  " + str);
					effector.sendPacket(sm);;
					sm = null;
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_SUCCESS);
					sm.addString("<  " + str);
					effected.sendPacket(sm);;
					sm = null;
				}
				else if (str.contains("Failed:"))
				{
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
					sm.addString(">  " + str);
					effector.sendPacket(sm);;
					sm = null;
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
					sm.addString("<  " + str);
					effected.sendPacket(sm);;
					sm = null;
				}
			}
		}

		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final L2Effect[] getEffectsRestore(L2Character effector, L2Character effected, Env env)
	{
		if (isPassive())
			return _emptyEffectSet;

		if (_effectTemplates == null)
			return _emptyEffectSet;

		List<L2Effect> effects = new FastList<L2Effect>();

		if (env == null)
			env = new Env();

		env.skillMastery = false;
		env.player = effector;
		env.target = effected;
		env.skill = this;

		for (EffectTemplate et : _effectTemplates)
		{
			L2Effect e = et.getEffect(env, true);

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
	 * Warning: this method doesn't consider modifier (shield, _randomWalkRange, sps, _soloMob) for secondary effects
	 * 
	 */
	public final L2Effect[] getEffects(L2Character effector, L2Character effected)
	{
		return getEffects(effector, effected, null);
	}

	public final int getEffectsLandChance(L2Character effector, L2Character effected)
	{
		if (isPassive())
			return 0;

		if (_effectTemplates == null)
			return 0;

		// doors and siege flags cannot receive any effects
		if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance)
			return 0;

		if (getId() != 10011)
		{
			if (effector != effected && !(effector instanceof L2BufferInstance))
			{
				if (effected.isInvul() || effected.isPreventedFromReceivingBuffs())
					return 0;
			}
		}

		if (isOffensive() && effected.isPreventedFromReceivingDebuffs())
			return 0;

		Env env = new Env();

		env.player = effector;
		env.target = effected;
		env.skill = this;

		int successChance = 100;

		for (EffectTemplate et : _effectTemplates)
		{
			if (et.effectPower > -1 && !isPositive())
			{
				successChance = Formulas.calcEffectSuccessChance(effector, effected, et, this, (byte) 0);
				break;
			}
		}

		return successChance;
	}

	/**
	 * This method has suffered some changes in CT2.2 ->CT2.3<br>
	 * Effect engine is now supporting secondary effects with independent
	 * success/fail calculus from effect skill. Env parameter has been added to
	 * pass parameters like soulshot, spiritshots, blessed spiritshots or shield deffence.
	 * Some other optimizations have been done
	 * <br><br>
	 * This new feature works following next rules:
	 * <li> To enable feature, effectPower must be over -1 (check DocumentSkill#attachEffect for further information)</li>
	 * <li> If main skill fails, secondary effect always fail</li>
	 */
	public final L2Effect[] getEffects(L2CubicInstance effector, L2Character effected, Env env)
	{
		if (isPassive())
			return _emptyEffectSet;

		if (_effectTemplates == null)
			return _emptyEffectSet;

		if (effected.isInvul())
			return _emptyEffectSet;

		List<L2Effect> effects = new FastList<L2Effect>();

		if (env == null)
			env = new Env();

		env.player = effector.getOwner();
		env.cubic = effector;
		env.target = effected;
		env.skill = this;

		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;

			if (isOffensive())
			{
				if (env.player instanceof L2Playable && env.target instanceof L2Playable && env.player != env.target)
				{
					if (!env.player.getActingPlayer().isGM())
					{
						if (!env.target.getActingPlayer().isDebuffable(env.player.getActingPlayer()))
							success = false;
					}
				}

				if (success)
				{
					if (et.effectPower > -1)
						success = Formulas.calcEffectSuccess(env.player, env.target, et, this, env.shld);
				}
			}

			if (success)
			{
				L2Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}

		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final L2Effect[] getEffectsSelf(L2Character effector)
	{
		if (isPassive())
			return _emptyEffectSet;

		if (_effectTemplatesSelf == null)
			return _emptyEffectSet;

		List<L2Effect> effects = new FastList<L2Effect>();

		for (EffectTemplate et : _effectTemplatesSelf)
		{
			Env env = new Env();
			env.player = effector;
			env.target = effector;
			env.skill = this;
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
	
	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null && _effectTemplatesSelf.length > 0);
	}
	
	public EffectTemplate[] getSelfEffectTemplates()
	{
		return _effectTemplatesSelf;
	}

	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[] { f };
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[] { effect };
		}
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}

	}

	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[] { effect };
		}
		else
		{
			int len = _effectTemplatesSelf.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
		}
	}

	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
			_itemPreCondition = c;
		else
			_preCondition = c;
	}

	@Override
	public String toString()
	{
		return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
	}

	/**
	 * @return pet food
	 */
	public int getFeed()
	{
		return _feed;
	}

	/**
	 * used for tracking item id in case that item consume cannot be used
	 * @return reference item id
	 */
	public int getReferenceItemId()
	{
		return _refId;
	}

	/**
	 * @return
	 */
	public int getAfroColor()
	{
		return _afroId;
	}

	public final int getRequiredCharges()
	{
		return _requiredCharges;
	}

	public final int getMaxCharges()
	{
		return _maxCharges;
	}

	public int getAfterEffectId()
	{
		return _afterEffectId;
	}

	public int getAfterEffectLvl()
	{
		return _afterEffectLvl;
	}

	@Override
	public boolean triggersChanceSkill()
	{
		return _triggeredId > 0 && isChance();
	}

	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}

	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}

	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}

	public String getAttributeName()
	{
		return _attribute;
	}

	public boolean ignoreShield()
	{
		return _ignoreShield;
	}

	public boolean canBeReflected()
	{
		return _canBeReflected;
	}

	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}

	public float getPvpMulti()
	{
		return _pvpMulti;
	}

	public float getPvmMulti()
	{
		return _pvmMulti;
	}

	public final int getMaxLandChance()
	{
		return _maxLandChance;
	}

	public final int getMinLandChance()
	{
		return _minLandChance;
	}

	public final float getReuseMulti()
	{
		return _reuseMulti;
	}

	public final float getPowerMulti()
	{
		return _powerMulti;
	}

	@SuppressWarnings("incomplete-switch")
	public final boolean isHeal()
	{
		switch (_skillType)
		{
			case HEAL:
			case SUPER_HEAL:
			case HEAL_STATIC:
			case HEAL_PERCENT:
			case BALANCE_LIFE:
			case MANARECHARGE:
			case RESURRECT:
				return true;
		}

		return false;
	}

	public final boolean isDamaging()
	{
		switch (_skillType)
		{
			case MDAM:
			case PDAM:
			case BLOW:
			case PDAMPERC:
			case CPDAM:
			case DRAIN:
			case CHARGEDAM:
			case FATAL:
			case DEATHLINK:
			case MANADAM:
				return true;
			default:
				return getEffectType() == L2SkillType.BLEED;
		}
	}

	public final boolean isTeleTypeSkill()
	{
		return getId() == 484 || getId() == 628 || getId() == 1448 || getSkillType() == L2SkillType.INSTANT_JUMP || getId() == 15004 || getId() == 15005;
	}

	public final boolean isTeleTypeSkill2()
	{
		return getFlyType() != null || getId() == 628 || getId() == 1448 || getSkillType() == L2SkillType.INSTANT_JUMP || getId() == 15005;
	}

	public final boolean isTeleTypeSkillMob()
	{
		return getFlyType() != null || getId() == 628 || getSkillType() == L2SkillType.INSTANT_JUMP;
	}

	public final boolean stopsAutoAttack()
	{
		return isTeleTypeSkill2() || getId() == 11 || getId() == 106 || getId() == 296 || getId() == 525 || getId() == 922; //trick and hide skills
	}

	protected int getNewHeadingToTarget(L2Character caster, L2Character target)
	{
		if (caster == null || target == null)
			return 0;

		double befHeading = Util.convertHeadingToDegree(caster.getHeading());
		if (befHeading > 360)
			befHeading -= 360;

		int dx = caster.getX() - target.getX();
		int dy = caster.getY() - target.getY();

		double dist = Math.sqrt(dx * dx + dy * dy);

		if (dist == 0)
			dist = 0.01;

		double sin = dy / dist;
		double cos = dx / dist;
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);

		return heading;
	}

	public boolean isBehindFromCaster(int heading, L2Character caster, L2Character target)
	{
		if (caster == null || target == null)
			return true;

		double befHeading = Util.convertHeadingToDegree(heading);
		if (befHeading > 360)
			befHeading -= 360;
		else if (befHeading < 0)
			befHeading += 360;

		int dx = caster.getX() - target.getX();
		int dy = caster.getY() - target.getY();

		double dist = Math.sqrt(dx * dx + dy * dy);

		if (dist == 0)
			dist = 0.01;

		double sin = dy / dist;
		double cos = dx / dist;
		int newheading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);

		double aftHeading = Util.convertHeadingToDegree(newheading);
		if (aftHeading > 360)
			aftHeading -= 360;
		else if (aftHeading < 0)
			aftHeading += 360;

		double diffHeading = Math.abs(aftHeading - befHeading);
		if (diffHeading > 360)
			diffHeading -= 360;
		else if (diffHeading < 0)
			diffHeading += 360;

		return (diffHeading >= 40) && (diffHeading <= 320);
	}

	final public int getAreaAngle(L2Character caster)
	{
		return (int) caster.calcStat(Stats.SKILL_AREA_ANGLE_MOD, _areaAngle, null, this);
	}

	public boolean isTrap()
	{
		return getSkillType() == L2SkillType.SUMMON_TRAP;
	}

	public boolean isPvpTransformSkill()
	{
		for (Integer id : L2PcInstance.PVP_TRANSFORM_SKILLS)
		{
			if (_id == id || _id == 8247 || _id == 8261 || _id == 8262) //horse, beatle, and lion transformation/mounts
				return true;
		}

		return false;
	}

	@SuppressWarnings("incomplete-switch")
	public boolean startsAutoAttack()
	{
		if (getId() == 10004) //air rave
			return false;

		if (getId() != 1448) //blink
		{
			switch (getSkillType())
			{
				case PDAM:
				case BLOW:
				case CHARGEDAM:
				case CPDAM:
				case FATAL:
				case BLEED:
				case PDAMPERC:
				{
					if (isMagic())
						return false;
				}
				case SOW:
				case SPOIL:
				case DRAIN_SOUL:
					return true;
			}
		}

		return false;
	}

	public void setLevel(int lvl)
	{
		_level = lvl;
	}

	public float getOlyTimeMulti()
	{
		return _olyTimeMulti;
	}

	public String getSound()
	{
		return _sound;
	}

	public boolean isBlow()
	{
		return _skillType == L2SkillType.BLOW;
	}

	public float getMpConsumeMulti()
	{
		return _mpConsumeMulti;
	}

	public int getLongestEffectDuration()
	{
		int duration = 0;

		if (getEffectTemplates() != null)
		{
			for (EffectTemplate et : getEffectTemplates())
			{
				if (et != null && et.period > duration)
					duration = et.period;
			}
		}

		return duration;
	}
	public boolean isExcludedFromCheck()
	{
		return _excludedFromCheck;
	}
	public FuncTemplate[] getFuncTemplates()
	{
		return _funcTemplates;
	}

	public final boolean isNoble()
	{
		return SkillTable.getInstance().isNobleSkill(this);
	}
	
	public boolean noblessSkill()
	{
		return getId() == 325 || getId() == 326 || getId() == 327 || getId() == 1323 || getId() == 1324 || getId() == 1325 || getId() == 1326 || getId() == 1327;
	}
	public boolean isChillAllow()
	{
		return !_chillIgnore;
	}
	
	public final String getIcon()
	{
		return IconsTable.getInstance().getSkillIcon(_id);
	}

}