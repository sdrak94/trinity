package net.sf.l2j.gameserver.datatables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import inertia.model.Inertia.ChillAction;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.SkillsEngine;

public class SkillTable
{
private TIntObjectHashMap<L2Skill> _skills;
private TIntIntHashMap _skillMaxLevel;

private final ArrayList<L2Skill> _pvpSkills;
private static final L2Skill[] _heroSkills = new L2Skill[14];
private static final int[] _heroSkillsId =
{
	395,
	396,
	1374,
	1375,
	1376,
};
private static final L2Skill[] _nobleSkills = new L2Skill[8];
private static final int[] _nobleSkillsId =
{
	325,
	326,
	327,
	1323,
	1324,
	1325,
	1326,
	1327
};

private static final HashSet<L2Skill> _nobleSkillz = new HashSet<>(_nobleSkills.length);

public static SkillTable getInstance()
{
	return SingletonHolder._instance;
}

private SkillTable()
{
	_skills = new TIntObjectHashMap<L2Skill>();
	_skillMaxLevel = new TIntIntHashMap();
	_pvpSkills = new ArrayList<>();
	reload();
}

public void reload()
{
	final TIntObjectHashMap<L2Skill> skills = new TIntObjectHashMap<L2Skill>();
	SkillsEngine.getInstance().loadAllSkills(skills);
	
	final TIntIntHashMap lvls = new TIntIntHashMap();
	
	for (final L2Skill skill : skills.values(new L2Skill[skills.size()]))
	{
		final int skillId = skill.getId();
		final int skillLvl = skill.getLevel();
		
		if (skillLvl >= 100)
			continue;
		
		final int maxLvl = lvls.get(skillId);
		
		if (skillLvl > maxLvl)
			lvls.put(skillId, skillLvl);
	}
	// Loading FrequentSkill enumeration values
	for (final FrequentSkill sk : FrequentSkill.values())
		sk._skill = getInfo(sk._id, sk._level);
	for (int i = 0; i < _heroSkillsId.length; i++)
		_heroSkills[i] = getInfo(_heroSkillsId[i], 1);
	for (int i = 0; i < _nobleSkills.length; i++)
		_nobleSkills[i] = getInfo(_nobleSkillsId[i], 1);
	
	_nobleSkillz.addAll(Arrays.asList(_nobleSkills));
	_skills = skills;
	_skillMaxLevel = lvls;
}

/**
 * Provides the skill hash
 * 
 * @param skill
 *            The L2Skill to be hashed
 * @return getSkillHashCode(skill.getId(), skill.getLevel())
 */
public static int getSkillHashCode(L2Skill skill)
{
	return getSkillHashCode(skill.getId(), skill.getLevel());
}

/**
 * Centralized method for easier change of the hashing sys
 * 
 * @param skillId
 *            The Skill Id
 * @param skillLevel
 *            The Skill Level
 * @return The Skill hash number
 */
public static int getSkillHashCode(int skillId, int skillLevel)
{
	return (skillId * 1021) + skillLevel;
}

public final L2Skill getInfo(final int skillId, final int level)
{
	final L2Skill result = _skills.get(getSkillHashCode(skillId, level));
	if (result != null)
		return result;
	
	// skill/level not found, fix for transformation scripts
	final int maxLvl = _skillMaxLevel.get(skillId);
	// requested level too high
	if (maxLvl > 0 && level > maxLvl)
		return _skills.get(getSkillHashCode(skillId, maxLvl));
	
	return null;
}
public final L2Skill getTransformSkillInfo(final L2PcInstance player, final int skillId, final int level)
{
	final L2Skill result = _skills.get(getSkillHashCode(skillId, level));
	if (result != null)
	{
		for (L2Skill a : player.getAllSkills())
		{
			if(a.getLevel() > 100)
			{
				result.setLevel(a.getLevel());
			}
		}
		return result;
	}
	
	// skill/level not found, fix for transformation scripts
	final int maxLvl = _skillMaxLevel.get(skillId);
	// requested level too high
	if (maxLvl > 0 && level > maxLvl)
		return _skills.get(getSkillHashCode(skillId, maxLvl));
	
	return null;
}
public final int getMaxLevel(final int skillId)
{
	return _skillMaxLevel.get(skillId);
}



public TIntObjectHashMap<L2Skill> getAllSkills()
{
	return _skills;
}



/**
 * Returns an array with siege skills. If addNoble == true, will add also Advanced headquarters.
 */
public L2Skill[] getSiegeSkills(boolean addNoble)
{
	L2Skill[] temp = null;
	
	if (addNoble)
	{
		temp = new L2Skill[3];
		temp[2] = _skills.get(SkillTable.getSkillHashCode(326, 1));
	}
	else
		temp = new L2Skill[2];
	
	temp[0] = _skills.get(SkillTable.getSkillHashCode(246, 1));
	temp[1] = _skills.get(SkillTable.getSkillHashCode(247, 1));
	
	return temp;
}
public static enum FrequentSkill
{
	LUCKY(194, 1),
	SEAL_OF_RULER(246, 1),
	BUILD_HEADQUARTERS(247, 1),
	STRIDER_SIEGE_ASSAULT(325, 1),
	DWARVEN_CRAFT(1321, 1),
	COMMON_CRAFT(1322, 1),
	LARGE_FIREWORK(2025, 1),
	SPECIAL_TREE_RECOVERY_BONUS(2139, 1),
	RAID_CURSE(4215, 1),
	WYVERN_BREATH(4289, 1),
	ARENA_CP_RECOVERY(4380, 1),
	RAID_CURSE2(4515, 1),
	VARKA_KETRA_PETRIFICATION(4578, 1),
	FAKE_PETRIFICATION(4616, 1),
	THE_VICTOR_OF_WAR(5074, 1),
	THE_VANQUISHED_OF_WAR(5075, 1),
	BLESSING_OF_PROTECTION(5182, 1),
	FIREWORK(5965, 1);
	protected final int _id;
	protected final int _level;
	protected L2Skill _skill = null;
	
	private FrequentSkill(final int id, final int level)
	{
		_id = id;
		_level = level;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
}
@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final SkillTable _instance = new SkillTable();
}
public boolean isNobleSkill(final L2Skill skill)
{
	return _nobleSkillz.contains(skill);
}

public L2Skill getInfoLevelMax(final int skillId)
{
	return _skills.get(getSkillHashCode(skillId, getMaxLevel(skillId)));
}

public static L2Skill getSkill(final ChillAction chillAction)
{
	return getInstance().getInfoLevelMax(chillAction.getActionId());
}
}
