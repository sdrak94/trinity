package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.StatsBBSManager;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class CharStat
{
// =========================================================
// Data Field
protected L2Character _activeChar;
private long _exp = 0;
private int _sp = 0;
private byte _level = 1;

// =========================================================
// Constructor
public CharStat(L2Character activeChar)
{
	_activeChar = activeChar;
}

public CharStat(StatsBBSManager statsBBSManager) {
	// TODO Auto-generated constructor stub
}

// =========================================================
// Method - Public
/**
 * Calculate the new value of the state with modifiers that will be applied
 * on the targeted L2Character.<BR>
 * <BR>
 *
 * <B><U> Concept</U> :</B><BR>
 * <BR>
 * A L2Character owns a table of Calculators called <B>_calculators</B>.
 * Each Calculator (a calculator per state) own a table of Func object. A
 * Func object is a mathematic function that permit to calculate the
 * modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
 * <BR>
 *
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
 * <BR>
 *
 * When the calc method of a calculator is launched, each mathematic
 * function is called according to its priority <B>_order</B>. Indeed, Func
 * with lowest priority order is executed firsta and Funcs with the same
 * order are executed in unspecified order. The result of the calculation is
 * stored in the value property of an Env class instance.<BR>
 * <BR>
 *
 * @param stat
 *            The stat to calculate the new value with modifiers
 * @param init
 *            The initial value of the stat before applying modifiers
 * @param target
 *            The L2Charcater whose properties will be used in the
 *            calculation (ex : CON, INT...)
 * @param skill
 *            The L2Skill whose properties will be used in the calculation
 *            (ex : Level...)
 *
 */
@SuppressWarnings("incomplete-switch")
public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
{
	if (_activeChar == null)
		return init;
	
	int id = stat.ordinal();
	
	Calculator c = _activeChar.getCalculators()[id];
	
	// If no Func object found, no modifier is applied
	if (c == null || c.size() == 0)
		return init;
	
	// Create and init an Env object to pass parameters to the Calculator
	Env env = new Env();
	env.player = _activeChar;
	env.target = target;
	env.skill = skill;
	env.value = init;
	// Launch the calculation
	c.calc(env);
	// avoid some troubles with negative stats (some stats should never be negative)
	
	if (env.value <= 600 && stat != null)
	{
		if (stat == Stats.MAX_HP || stat == Stats.MAX_MP || stat == Stats.MAX_CP)
		{
			env.value = 621;
		}
		else
		{
			if (env.value <= 0)
			{
				switch (stat)
				{
				case MAGIC_DEFENCE:
				case POWER_DEFENCE:
				case POWER_ATTACK:
				case MAGIC_ATTACK:
				case POWER_ATTACK_SPEED:
				case MAGIC_ATTACK_SPEED:
				case SHIELD_DEFENCE:
				case STAT_CON:
				case STAT_DEX:
				case STAT_INT:
				case STAT_MEN:
				case STAT_STR:
				case STAT_WIT:
					env.value = 1;
				}
			}
		}
	}
	
	return env.value;
}

// =========================================================
// Method - Private

// =========================================================
// Property - Public
/**
 * Return the Accuracy (base+modifier) of the L2Character in function of the
 * Weapon Expertise Penalty.
 * @param target
 */
public int getAccuracy(L2Character target)
{
	if (_activeChar == null)
		return 0;
	
	if (getActiveChar() instanceof L2Npc)
	{
		if (getActiveChar().isAPC() || (getActiveChar() instanceof L2RaidBossInstance && getActiveChar().getLevel() > 92))
			return (int) (calcStat(Stats.ACCURACY_COMBAT, 35, target, null))+5;
		
		return (int) (calcStat(Stats.ACCURACY_COMBAT, 0, target, null))+8;
	}
	
	return (int) (calcStat(Stats.ACCURACY_COMBAT, 0, target, null));
}

public L2Character getActiveChar()
{
	return _activeChar;
}

/**
 * Return the Attack Speed multiplier (base+modifier) of the L2Character to
 * get proper animations.
 */
public final float getAttackSpeedMultiplier()
{
	if (_activeChar == null)
		return 1;
	
	return (float) ((1.1) * getPAtkSpd(null) / _activeChar.getTemplate().basePAtkSpd);
}

public final float getFakeAttackSpeedMultiplier(int base)
{
	if (_activeChar == null)
		return 1;
	
	if (base < 1)
	{
		System.out.println("WTF LOL fakeattackspeedmulti base < 1!!!!");
		return 1;
	}
	
	return (float) ((1.1) * getPAtkSpd(null) / base);
}

/** Return the CON of the L2Character (base+modifier). */
public final int getCON()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_CON, _activeChar.getTemplate().baseCON, null, null);
}

/** Return the Critical Damage rate (base+modifier) of the L2Character. */
@SuppressWarnings("incomplete-switch")
public final double getCriticalDmg(L2Character target, double init, L2Skill skill)
{
	float multi = 1;
	
	if (_activeChar.isAPC())
	{
		final L2Item wep = ItemTable.getInstance().getTemplate(FakePcsTable.getInstance().getFakePc(((L2Npc)_activeChar).getNpcId()).pdRHand);
		
		if (wep != null && wep instanceof L2Weapon)
		{
			switch (((L2Weapon)wep).getItemType())
			{
			case BLUNT:
			case BIGBLUNT:
			case POLE:
			case DUALFIST:
			case FIST:
				multi = 2;
				break;
			case SWORD:
			case DUAL:
			case BIGSWORD:
			case ANCIENT_SWORD:
				multi = (float) 1.5;
				break;
			case DAGGER:
			case DUAL_DAGGER:
			case RAPIER:
			case CROSSBOW:
			case BOW:
				multi = (float) 2.5;
				break;
			}
		}
	}
	
	return calcStat(Stats.CRITICAL_DAMAGE, init, target, skill) * multi;
}

/** Return the Critical Hit rate (base+modifier) of the L2Character. */
public int getCriticalHitRate(L2Character target, L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	int baseCritRate = _activeChar.getTemplate().baseCritRate;
	
	if (_activeChar.isAPC())
	{
		final L2Item wep = ItemTable.getInstance().getTemplate(FakePcsTable.getInstance().getFakePc(((L2Npc)_activeChar).getNpcId()).pdRHand);
		
		if (wep != null && wep instanceof L2Weapon)
		{
			baseCritRate = (int) Math.max(120, baseCritRate * ((((L2Weapon)wep).getCritical()/1.08)));
		}
	}
	
	int criticalHit = (int) (calcStat(Stats.CRITICAL_RATE, baseCritRate, target, skill)*10.0 + 0.5);
	
	criticalHit /= 10;
	
	final int critMaxAdd = (int)calcStat(Stats.CRIT_MAX_ADD, Config.MAX_PCRIT_RATE, target, null);
	
	if (criticalHit > critMaxAdd)
		criticalHit = critMaxAdd;
	
	return criticalHit;
}
public int getVampRate(L2Character target)
{
	final L2Item weapon = _activeChar.getActiveWeaponItem();

	if (_activeChar == null)
		return 1;
	int baseVampHitRate = 3;
	int VampHitRate = 0;
	
	if ((weapon == null || (weapon.getItemType() == L2WeaponType.BOW || weapon.getItemType() == L2WeaponType.CROSSBOW)));
	{
		VampHitRate = 0;	
	}
	if ((weapon != null))
		{
		if (weapon.getItemType() == L2WeaponType.DUAL || weapon.getItemType() == L2WeaponType.DUAL_DAGGER ||weapon.getItemType() == L2WeaponType.DUALFIST)
			{
				baseVampHitRate += 1;
			}
		else 
			baseVampHitRate = 3;
		}
	
	VampHitRate = (int) (calcStat(Stats.ABSORB_DAMAGE_CHANCE, baseVampHitRate, target, null));
	return VampHitRate;
}
/** Return the DEX of the L2Character (base+modifier). */
public final int getDEX()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_DEX, _activeChar.getTemplate().baseDEX, null, null);
}

/** Return the Attack Evasion rate (base+modifier) of the L2Character. */
public int getEvasionRate(L2Character target)
{
	if (_activeChar == null)
		return 1;
	
	int val = 1;
	
	if (getActiveChar() instanceof L2Npc)
	{
		if (getActiveChar().isAPC())
			val = (int) (calcStat(Stats.EVASION_RATE, 25, target, null));
		
		val = (int) (calcStat(Stats.EVASION_RATE, 1, target, null))+5;
	}
	else
		val = (int) (calcStat(Stats.EVASION_RATE, 1, target, null));
	
	if (_activeChar.isSleeping() || _activeChar.isStunned() || _activeChar.isParalyzed())
		val /= 1.2;
	else if (_activeChar.isRooted() || _activeChar.isImmobilized())
		val /= 1.14;
	
	if (val > Config.MAX_EVASION && !_activeChar.isGM())
		val = Config.MAX_EVASION;
	
	return val;
}

public long getExp()
{
	return _exp;
}

public void setExp(long value)
{
	_exp = value;
}

/** Return the INT of the L2Character (base+modifier). */
public int getINT()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_INT, _activeChar.getTemplate().baseINT, null, null);
}

public byte getLevel()
{
	return _level;
}

public void setLevel(byte value)
{
	_level = value;
}

/** Return the Magical Attack range (base+modifier) of the L2Character. */
public final int getMagicalAttackRange(L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	if (skill != null)
	{
		final int range = skill.getCastRange(_activeChar);
		
		if (range > 0)
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, range, null, skill);
		
		return range;
	}
	
	return _activeChar.getTemplate().baseAtkRange;
}

/** Return the Magical Attack range (base+modifier) of the L2Character. */
public final int getMagicalRangeBoost()
{
	if (_activeChar == null)
		return 0;
	
	return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, 0, null, null);
}

public int getMaxCp()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.MAX_CP, _activeChar.getTemplate().baseCpMax, null, null);
}

public int getMaxHp()
{
	if (_activeChar == null)
		return 1;
	
	double modifier = 1;
	
	if (_activeChar instanceof L2Summon)
	{
//		if (_activeChar.getActingPlayer().isInSgradeZone())
//		{
//			if (_activeChar.getActingPlayer().isInHuntersVillage())
//				modifier = 0.7;
//			else
//				modifier = 1;
//		}
		if (_activeChar.getActingPlayer().isInOlympiadMode())
		{
			if (_activeChar.getActingPlayer().isDwarfClass())
				modifier = 0.32;
			else if (_activeChar.getActingPlayer().isTankClass())
				modifier = 0.32;
		}
	}
	
	return (int)(calcStat(Stats.MAX_HP, _activeChar.getTemplate().baseHpMax, null, null)*modifier);
}

public int getMaxMp()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.MAX_MP, _activeChar.getTemplate().baseMpMax, null, null);
}

/**
 * Return the MAtk (base+modifier) of the L2Character for a skill used in
 * function of abnormal effects in progress.<BR>
 * <BR>
 *
 * <B><U> Example of use </U> :</B><BR>
 * <BR>
 * <li> Calculate Magic damage </li>
 * <BR>
 * <BR>
 *
 * @param target
 *            The L2Character targeted by the skill
 * @param skill
 *            The L2Skill used against the target
 */
@SuppressWarnings("incomplete-switch")
public int getMAtk(L2Character target, L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	if (_activeChar instanceof L2PcInstance && _activeChar.getActingPlayer().getActiveWeaponInstance() == null)
		return 100;
	
	double bonusAtk = 1;
	if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		bonusAtk = Config.L2JMOD_CHAMPION_ATK;
	else if (_activeChar instanceof L2Summon)
	{
//		if (_activeChar.getActingPlayer().isInSgradeZone())
//		{
//			if (_activeChar.getActingPlayer().isInHuntersVillage())
//				bonusAtk = 0.55;
//			else
//				bonusAtk = 0.9;
//		}
		if (_activeChar.getActingPlayer().isInOlympiadMode())
		{
			bonusAtk = 0.15;
		}
	}
	double attack = _activeChar.getTemplate().baseMAtk * bonusAtk;
	// Get the skill type to calculate its effect in function of base stats
	// of the L2Character target
	Stats stat = skill == null ? null : skill.getStat();
	
	if (stat != null)
	{
		switch (stat)
		{
		case AGGRESSION:
			attack += _activeChar.getTemplate().baseAggression;
			break;
		case BLEED:
			attack += _activeChar.getTemplate().baseBleed;
			break;
		case POISON:
			attack += _activeChar.getTemplate().basePoison;
			break;
		case STUN:
			attack += _activeChar.getTemplate().baseStun;
			break;
		case ROOT:
			attack += _activeChar.getTemplate().baseRoot;
			break;
		case MOVEMENT:
			attack += _activeChar.getTemplate().baseMovement;
			break;
		case CONFUSION:
			attack += _activeChar.getTemplate().baseConfusion;
			break;
		case SLEEP:
			attack += _activeChar.getTemplate().baseSleep;
			break;
		}
	}
	
	// Add the power of the skill to the attack effect
	if (skill != null)
		attack += skill.getPower();
	
	// Calculate modifiers Magic Attack
	return (int) calcStat(Stats.MAGIC_ATTACK, attack, target, skill);
}

/**
 * Return the MAtk Speed (base+modifier) of the L2Character in function of
 * the Armour Expertise Penalty.
 */
public int getMAtkSpd(L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	float bonusSpdAtk = 1;
	if  (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		bonusSpdAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
	
	return (int) calcStat(Stats.MAGIC_ATTACK_SPEED, _activeChar.getTemplate().baseMAtkSpd * bonusSpdAtk, null, skill);
	
	/*		if (skill == null)
			return (int) Math.min(val, getActiveChar().calcStat(Stats.CAST_SPEED_MAX_ADD, Config.MAX_MATK_SPEED, null, null));
		
		return (int) Math.min(val, getActiveChar().calcStat(Stats.CAST_SPEED_MAX_ADD, Config.MAX_MATK_SPEED, null, skill));*/
}

/** Return the Magic Critical Hit rate (base+modifier) of the L2Character. */
public final int getMCriticalHit(L2Character target, L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	int baseCritRate = _activeChar.getTemplate().baseMCritRate;
	
	if (_activeChar.isAPC())
	{
		final L2Item wep = ItemTable.getInstance().getTemplate(FakePcsTable.getInstance().getFakePc(((L2Npc)_activeChar).getNpcId()).pdRHand);
		
		if (wep != null && wep instanceof L2Weapon)
		{
			baseCritRate = Math.max(60, baseCritRate * 2);
		}
	}
	
	double mrate = calcStat(Stats.MCRITICAL_RATE, baseCritRate, target, skill);
	
	if (skill != null && skill.isHeal())
		mrate /= 1.25;
	
	final int totalCap = (int) (calcStat(Stats.CRIT_MAGIC_MAX_ADD, Config.MAX_MCRIT_RATE, null, skill));
	
	if(mrate > totalCap)
		mrate = totalCap;
	
	if (_activeChar instanceof L2TrapInstance)
		mrate /= 1.4;
	
	return (int) mrate;
}

/**
 * Return the MDef (base+modifier) of the L2Character against a skill in
 * function of abnormal effects in progress.<BR>
 * <BR>
 *
 * <B><U> Example of use </U> :</B><BR>
 * <BR>
 * <li> Calculate Magic damage </li>
 * <BR>
 *
 * @param target
 *            The L2Character targeted by the skill
 * @param skill
 *            The L2Skill used against the target
 */
public int getMDef(L2Character target, L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	
	// Get the base MAtk of the L2Character
	double defence = _activeChar.getTemplate().baseMDef;
	
	// Calculate modifier for Raid Bosses
	if (_activeChar.isRaid())
		defence *= Config.RAID_MDEFENCE_MULTIPLIER;
	else if (_activeChar instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)_activeChar;
		
		if (player.isDaggerClass())
			return (int)Math.max(1, calcStat(Stats.MAGIC_DEFENCE, defence, target, skill)/1.03);
	}
	else if (_activeChar instanceof L2Summon)
	{
//		if (_activeChar.getActingPlayer().isInSgradeZone())
//		{
//			if (_activeChar.getActingPlayer().isInHuntersVillage())
//				defence *= 0.6;
//			else
//				defence *= 0.9;
//		}
		if (_activeChar.getActingPlayer().isInOlympiadMode())
		{
			defence *= 0.2;
		}
	}
	
	// Calculate modifiers Magic Attack
	return (int)Math.max(1, calcStat(Stats.MAGIC_DEFENCE, defence, target, skill));
}

/** Return the MEN of the L2Character (base+modifier). */
public final int getMEN()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_MEN, _activeChar.getTemplate().baseMEN, null, null);
}

public float getMovementSpeedMultiplier()
{
	if (_activeChar == null)
		return 1;

	return getRunSpeed() * 1f / _activeChar.getTemplate().baseRunSpd;
}

/**
 * Return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the
 * L2Character in function of the movement type.
 */
public final float getMoveSpeed()
{
	if (_activeChar == null)
		return 1;
	
	if (_activeChar.isRunning())
		return getRunSpeed();
	
	return getWalkSpeed();
}

/** Return the MReuse rate (base+modifier) of the L2Character. */
public final double getMReuseRate(L2Skill skill, L2Character target)
{
	if (_activeChar == null || (skill != null && skill.isPotion()))
		return 1;
	
	double cap = _activeChar.isGM() ? 0 : 0.4;
	return Math.max(calcStat(Stats.MAGIC_REUSE_RATE, _activeChar.getTemplate().baseMReuseRate, target, skill), cap);
}

/** Return the PReuse rate (base+modifier) of the L2Character. */
public final double getPReuseRate(L2Skill skill, L2Character target)
{
	if (_activeChar == null || (skill != null && skill.isPotion()))
		return 1;
	
	double mod = 1;
	
	if (_activeChar instanceof L2PcInstance)
	{
		L2PcInstance pc = (L2PcInstance) _activeChar;
		
		if (pc != null)
		{
			if (pc.getHennaStatDEX() < 0)
			{
				mod += Math.pow(pc.getHennaStatDEX(), 2)/50;
			}
		}
	}
	double cap = _activeChar.isGM() ? 0 : 0.4;
	return Math.max(calcStat(Stats.P_REUSE, _activeChar.getTemplate().baseMReuseRate, target, skill) * mod, cap);
}

public final double getMReuseRateGem(boolean magic)
{
	if (_activeChar == null)
		return 1;
	
	if (magic)
		return calcStat(Stats.MAGIC_REUSE_RATE, 1, null, null);
	
	return calcStat(Stats.P_REUSE, 1, null, null);
}

/** Return the PAtk (base+modifier) of the L2Character. */
public int getPAtk(L2Character target)
{
	if (_activeChar == null)
		return 1;
	double bonusAtk = 1;
	if  (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		bonusAtk = Config.L2JMOD_CHAMPION_ATK;
	else if (_activeChar instanceof L2Summon)
	{
//		if (_activeChar.getActingPlayer().isInSgradeZone())
//		{
//			if (_activeChar.getActingPlayer().isInHuntersVillage())
//				bonusAtk = 0.55;
//			else
//				bonusAtk = 0.9;
//		}
		if (_activeChar.getActingPlayer().isInOlympiadMode())
		{
			bonusAtk = 0.14;
		}
	}
	
	return (int) calcStat(Stats.POWER_ATTACK, _activeChar.getTemplate().basePAtk * bonusAtk, target, null);
}

/** Return the PAtk Modifier against animals. */
public final double getPAtkAnimals(L2Character target)
{
	return calcStat(Stats.PATK_ANIMALS, 1, target, null);
}

/** Return the PAtk Modifier against dragons. */
public final double getPAtkDragons(L2Character target)
{
	return calcStat(Stats.PATK_DRAGONS, 1, target, null);
}

/** Return the PAtk Modifier against insects. */
public final double getPAtkInsects(L2Character target)
{
	return calcStat(Stats.PATK_INSECTS, 1, target, null);
}

/** Return the PAtk Modifier against monsters. */
public final double getPAtkMonsters(L2Character target)
{
	return calcStat(Stats.PATK_MONSTERS, 1, target, null);
}

/** Return the PAtk Modifier against plants. */
public final double getPAtkPlants(L2Character target)
{
	return calcStat(Stats.PATK_PLANTS, 1, target, null);
}

/** Return the PAtk Modifier against undeads. */
public final double getPAtkUndead(L2Character target)
{
	return calcStat(Stats.PATK_UNDEAD, 1, target, null);
}

/** Return the PAtk Modifier against monsters. */
public final double getPAtkDemons(L2Character target)
{
	return calcStat(Stats.PATK_DEMONS, 1, target, null);
}

/** Return the PAtk Modifier against giants. */
public final double getPAtkGiants(L2Character target)
{
	return calcStat(Stats.PATK_GIANTS, 1, target, null);
}

/** Return the PAtk Modifier against magic creatures */
public final double getPAtkMCreatures(L2Character target)
{
	return calcStat(Stats.PATK_MCREATURES, 1, target, null);
}
/**
 * Return the PAtk Speed (base+modifier) of the L2Character in function of
 * the Armour Expertise Penalty.
 */
public int getPAtkSpd(L2Skill skill)
{
	if (_activeChar == null)
		return 1;
	float bonusAtk = 1;
	if  (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		bonusAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
	int val = (int) (calcStat(Stats.POWER_ATTACK_SPEED, _activeChar.getTemplate().basePAtkSpd * bonusAtk, null, skill));
	return val;
}

/** Return the PDef Modifier against animals. */
public final double getPDefAnimals(L2Character target)
{
	return calcStat(Stats.PDEF_ANIMALS, 1, target, null);
}

/** Return the PDef Modifier against dragons. */
public final double getPDefDragons(L2Character target)
{
	return calcStat(Stats.PDEF_DRAGONS, 1, target, null);
}

/** Return the PDef Modifier against insects. */
public final double getPDefInsects(L2Character target)
{
	return calcStat(Stats.PDEF_INSECTS, 1, target, null);
}

/** Return the PDef Modifier against monsters. */
public final double getPDefMonsters(L2Character target)
{
	return calcStat(Stats.PDEF_MONSTERS, 1, target, null);
}

/** Return the PDef Modifier against plants. */
public final double getPDefPlants(L2Character target)
{
	return calcStat(Stats.PDEF_PLANTS, 1, target, null);
}

/** Return the PDef Modifier against giants. */
public final double getPDefGiants(L2Character target)
{
	return calcStat(Stats.PDEF_GIANTS, 1, target, null);
}

/** Return the PDef (base+modifier) of the L2Character. */
public int getPDef(L2Character target)
{
	if (_activeChar == null)
		return 1;
	
	double bonusAtk = 1;
	
	if (_activeChar instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)_activeChar;
		
		if (player.isDaggerClass())
			return (int)(calcStat(Stats.POWER_DEFENCE, _activeChar.getTemplate().basePDef, target, null)/1.05);
	}
	else if (_activeChar instanceof L2Summon)
	{
//		if (_activeChar.getActingPlayer().isInSgradeZone())
//		{
//			if (_activeChar.getActingPlayer().isInHuntersVillage())
//				bonusAtk = 0.6;
//			else
//				bonusAtk = 0.9;
//		}
		if (_activeChar.getActingPlayer().isInOlympiadMode())
		{
			bonusAtk = 0.2;
		}
	}
	
	return (int) (calcStat(Stats.POWER_DEFENCE, (_activeChar.isRaid()) ? _activeChar.getTemplate().basePDef * Config.RAID_PDEFENCE_MULTIPLIER : _activeChar.getTemplate().basePDef, target, null) * bonusAtk);
}

/** Return the Physical Attack range (base+modifier) of the L2Character. */
public final int getPhysicalAttackRange()
{
	if (_activeChar == null)
		return 1;
	
	if (_activeChar.isTransformed() && !_activeChar.isInStance())
		return _activeChar.getTemplate().baseAtkRange;
	// Polearm handled here for now. Basically L2PcInstance could have a function
	// similar to FuncBowAtkRange and NPC are defined in DP.
	final L2Weapon weaponItem = _activeChar.getActiveWeaponItem();
	if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.POLE)
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, 66, null, null);
	
	return (int) calcStat(Stats.POWER_ATTACK_RANGE, _activeChar.getTemplate().baseAtkRange, null, null);
}

/** Return the weapon reuse modifier */
public final double getWeaponReuseModifier(L2Character target)
{
	return calcStat(Stats.ATK_REUSE, 1, target, null);
}

/**
 * Return the RunSpeed (base+modifier) of the L2Character in function of the
 * Armour Expertise Penalty.
 */
public int getRunSpeed()
{
	if (_activeChar == null)
		return 1;
	
	// err we should be adding TO the persons run speed
	// not making it a constant
	double baseRunSpd = _activeChar.getTemplate().baseRunSpd;
	
	if (baseRunSpd == 0)
		return 0;
	
	return (int) calcStat(Stats.RUN_SPEED, baseRunSpd, null, null);
}

/** Return the ShieldDef rate (base+modifier) of the L2Character. */
public final int getShldDef()
{
	return (int) calcStat(Stats.SHIELD_DEFENCE, 0, null, null);
}

public int getSp()
{
	return _sp;
}

public void setSp(int value)
{
	_sp = value;
}

/** Return the STR of the L2Character (base+modifier). */
public final int getSTR()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_STR, _activeChar.getTemplate().baseSTR, null, null);
}

/** Return the WalkSpeed (base+modifier) of the L2Character. */
public int getWalkSpeed()
{
	if (_activeChar == null)
		return 1;
	
	double baseWalkSpd = _activeChar.getTemplate().baseWalkSpd;
	
	if (baseWalkSpd == 0)
		return 0;
	
	return (int) calcStat(Stats.WALK_SPEED, baseWalkSpd, null, null);
}

/** Return the WIT of the L2Character (base+modifier). */
public final int getWIT()
{
	if (_activeChar == null)
		return 1;
	
	return (int) calcStat(Stats.STAT_WIT, _activeChar.getTemplate().baseWIT, null, null);
}

/** Return the mpConsume. */
public final int getMpConsume(L2Skill skill)
{
	if (skill == null)
		return 1;
	
	int mpconsume = skill.getMpConsume();
	
	return (int) calcStat(Stats.MP_CONSUME, mpconsume, null, skill);
}

/** Return the mpInitialConsume. */
public final int getMpInitialConsume(L2Skill skill)
{
	if (skill == null)
		return 1;
	
	return (int) calcStat(Stats.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
}

public byte getAttackElement()
{
	L2ItemInstance weaponInstance = _activeChar.getActiveWeaponInstance();
	// 1st order - weapon element
	if (weaponInstance != null && weaponInstance.getAttackElementType() >= 0 )
		return weaponInstance.getAttackElementType();
	
	// temp fix starts
	int tempVal = 0, stats[] = { 0, 0, 0, 0, 0, 0 };
	
	byte returnVal = -2;
	stats[0] = (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().baseFire, null, null);
	stats[1] = (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().baseWater, null, null);
	stats[2] = (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().baseWind, null, null);
	stats[3] = (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().baseEarth, null, null);
	stats[4] = (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().baseHoly, null, null);
	stats[5] = (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().baseDark, null, null);
	
	for (byte x = 0; x < 6; x++)
	{
		if (stats[x] > tempVal)
		{
			returnVal = x;
			tempVal = stats[x];
		}
	}
	
	return returnVal;
	// temp fix ends
	
	/*
	 * uncomment me once deadlocks in getAllEffects() fixed
			return _activeChar.getElementIdFromEffects();
	 */
}

public int getAttackElementValue(byte attackAttribute)
{
	switch (attackAttribute)
	{
	case Elementals.FIRE:
		return (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().baseFire, null, null);
	case Elementals.WATER:
		return (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().baseWater, null, null);
	case Elementals.WIND:
		return (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().baseWind, null, null);
	case Elementals.EARTH:
		return (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().baseEarth, null, null);
	case Elementals.HOLY:
		return (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().baseHoly, null, null);
	case Elementals.DARK:
		return (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().baseDark, null, null);
	default:
		return 0;
	}
}

public int getDefenseElementValue(byte defenseAttribute)
{
	switch (defenseAttribute)
	{
	case Elementals.FIRE:
		return (int) calcStat(Stats.FIRE_RES, _activeChar.getTemplate().baseFireRes, null, null);
	case Elementals.WATER:
		return (int) calcStat(Stats.WATER_RES, _activeChar.getTemplate().baseWaterRes, null, null);
	case Elementals.WIND:
		return (int) calcStat(Stats.WIND_RES, _activeChar.getTemplate().baseWindRes, null, null);
	case Elementals.EARTH:
		return (int) calcStat(Stats.EARTH_RES, _activeChar.getTemplate().baseEarthRes, null, null);
	case Elementals.HOLY:
		return (int) calcStat(Stats.HOLY_RES, _activeChar.getTemplate().baseHolyRes, null, null);
	case Elementals.DARK:
		return (int) calcStat(Stats.DARK_RES, _activeChar.getTemplate().baseDarkRes, null, null);
	default:
		return 0;
	}
}
}
