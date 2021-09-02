/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.templates.skills;

import java.lang.reflect.Constructor;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillAgathion;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillAppearance;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillChangeWeapon;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillChargeDmg;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillCreateItem;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDecoy;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDefault;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDrain;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillLearnSkill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillMount;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignet;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillTeleport;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillTrap;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * @author nBd
 */
public enum L2SkillType
{
	// Damage
	PDAM,
	MDAM,
	CPDAM,
	MANADAM,
	CPDAMPERCENT,
	DOT,
	MDOT,
	DRAIN_SOUL,
	DRAIN(L2SkillDrain.class),
	DEATHLINK,
	FATAL,
	BLOW,
	SIGNET(L2SkillSignet.class),
	SIGNET_CASTTIME(L2SkillSignetCasttime.class),
	// Disablers
	BLEED,
	POISON,
	STUN,
	ROOT,
	CONFUSION,
	FEAR,
	SLEEP,
	CONFUSE_MOB_ONLY,
	MUTE,
	PARALYZE,
	WEAKNESS,
	DISARM,
	// hp, mp, cp
	HEAL,
	HOT,
	BALANCE_LIFE,
	HEAL_PERCENT,
	HEAL_STATIC,
	COMBATPOINTHEAL,
	CPHEAL_PERCENT,
	CPHOT,
	MANAHEAL,
	MANA_BY_LEVEL,
	MANAHEAL_PERCENT,
	MANARECHARGE,
	MPHOT,
	// sp
	GIVE_SP,
	// vitality
	GIVE_VITALITY,
	// Aggro
	AGGDAMAGE,
	AGGREDUCE,
	AGGREMOVE,
	AGGREDUCE_CHAR,
	AGGDEBUFF,
	// Fishing
	FISHING,
	PUMPING,
	REELING,
	// MISC
	UNLOCK,
	ENCHANT_ARMOR,
	ENCHANT_WEAPON,
	ENCHANT_ATTRIBUTE,
	SIEGEFLAG(L2SkillSiegeFlag.class),
	TAKECASTLE,
	TAKEFORT,
	WEAPON_SA,
	DELUXE_KEY_UNLOCK,
	SOW,
	HARVEST,
	GET_PLAYER,
	AGATHION(L2SkillAgathion.class),
	MOUNT(L2SkillMount.class),
	INSTANT_JUMP,
	DUMMY,
	// Creation
	COMMON_CRAFT,
	DWARVEN_CRAFT,
	CREATE_ITEM(L2SkillCreateItem.class),
	SUMMON_TREASURE_KEY,
	EXTRACTABLE,
	LEARN_SKILL(L2SkillLearnSkill.class),
	// Summons
	SUMMON(L2SkillSummon.class),
	FEED_PET,
	DEATHLINK_PET,
	STRSIEGEASSAULT,
	ERASE,
	BETRAY,
	DECOY(L2SkillDecoy.class),
	// Cancel
	CANCEL,
	SUPER_CANCEL,
	CANCEL_STATS,
	CANCEL_DEBUFF,
	MAGE_BANE,
	WARRIOR_BANE,
	NEGATE,
	BUFF,
	DEBUFF,
	PASSIVE,
	CONT,
	FUSION,
	RESURRECT,
	RESURRECT_PET,
	CHARGEDAM(L2SkillChargeDmg.class),
	MHOT,
	DETECT_WEAKNESS,
	LUCK,
	RECALL(L2SkillTeleport.class),
	TELEPORT(L2SkillTeleport.class),
	SUMMON_FRIEND,
	REFLECT,
	SPOIL,
	SWEEP,
	FAKE_DEATH,
	UNDEAD_DEFENSE,
	BEAST_FEED,
	CHARGESOUL,
	TRANSFORMDISPEL,
	SUMMON_TRAP(L2SkillTrap.class),
	DETECT_TRAP,
	REMOVE_TRAP,
	SHIFT_TARGET,
	// Kamael WeaponChange
	CHANGEWEAPON(L2SkillChangeWeapon.class),
	STEAL_BUFF,
	SLOW,
	INVISIBLE,
	// Skill is done within the core.
	COREDONE,
	CHANGE_APPEARANCE(L2SkillAppearance.class),
	// unimplemented
	NOTDONE,
	BALLISTA,
	SWITCH,
	SUPER_HEAL,
	DETECTION,
	PROC,
	PDAMPERC;
	
	private final Class<? extends L2Skill> _class;
	
	public L2Skill makeSkill(StatsSet set)
	{
		try
		{
			Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);
			return c.newInstance(set);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private L2SkillType()
	{
		_class = L2SkillDefault.class;
	}
	
	private L2SkillType(Class<? extends L2Skill> classType)
	{
		_class = classType;
	}
}
