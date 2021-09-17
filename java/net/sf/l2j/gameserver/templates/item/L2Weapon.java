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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameChance;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * This class is dedicated to the management of weapons.
 *
 * @version $Revision: 1.4.2.3.2.5 $ $Date: 2005/04/02 15:57:51 $
 */
public final class L2Weapon extends L2Item
{
private final int _pDam;
private final int _rndDam;
private final int _critical;
private final double _hitModifier;
private final int _standardShopItem;
private final int _shieldDef;
private final double _shieldDefRate;
private final int _atkSpeed;
private final int _atkReuse;
private final int _mpConsume;
private final int _mDam;
private L2Skill _enchant4Skill = null; // skill that activates when item is enchanted +4 (for duals)
private final int _changeWeaponId;
private final String[] _skill;

// Attached skills for Special Abilities
protected L2Skill _skillsOnCast;
protected Condition _skillsOnCastCondition;
protected L2Skill _skillsOnCrit;
protected Condition _skillsOnCritCondition;

/**
 * Constructor for Weapon.<BR><BR>
 * <U><I>Variables filled :</I></U><BR>
 * <LI>_pDam & _mDam & _rndDam</LI>
 * <LI>_critical</LI>
 * <LI>_hitModifier</LI>
 * <LI>_avoidModifier</LI>
 * <LI>_shieldDes & _shieldDefRate</LI>
 * <LI>_atkSpeed & _AtkReuse</LI>
 * <LI>_mpConsume</LI>
 * @param type : L2ArmorType designating the type of armor
 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
 * @see L2Item constructor
 */
public L2Weapon(L2WeaponType type, StatsSet set)
{
	super(type, set);
	_pDam = set.getInteger("p_dam");
	_rndDam = set.getInteger("rnd_dam");
	_critical = set.getInteger("critical");
	_hitModifier = set.getDouble("hit_modify");
	_standardShopItem = set.getInteger("avoid_modify");
	_shieldDef = set.getInteger("shield_def");
	_shieldDefRate = set.getDouble("shield_def_rate");
	_atkSpeed = set.getInteger("atk_speed");
	_atkReuse = set.getInteger("atk_reuse", (type == L2WeaponType.BOW) ? 1500 : (type == L2WeaponType.CROSSBOW) ? 1200 : 0);
	_mpConsume = set.getInteger("mp_consume");
	_mDam = set.getInteger("m_dam");
	
	_skill = set.getString("skill").split(";");
	
	
	int sId = set.getInteger("enchant4_skill_id");
	int sLv = set.getInteger("enchant4_skill_lvl");
	if (sId > 0 && sLv > 0)
		_enchant4Skill = SkillTable.getInstance().getInfo(sId, sLv);
	
	sId = set.getInteger("onCast_skill_id");
	sLv = set.getInteger("onCast_skill_lvl");
	int sCh = set.getInteger("onCast_skill_chance");
	if (sId > 0 && sLv > 0 && sCh > 0)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
		//skill.attach(new ConditionGameChance(sCh),true);
		attachOnCast(skill, sCh);
	}
	
	sId = set.getInteger("onCrit_skill_id");
	sLv = set.getInteger("onCrit_skill_lvl");
	sCh = set.getInteger("onCrit_skill_chance");
	if (sId > 0 && sLv > 0 && sCh > 0)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
		//skill.attach(new ConditionGameChance(sCh),true);
		attachOnCrit(skill, sCh);
	}
	_changeWeaponId = set.getInteger("change_weaponId");
}

/**
 * Returns the type of Weapon
 * @return L2WeaponType
 */
@Override
public L2WeaponType getItemType()
{
	return (L2WeaponType) super._type;
}

/**
 * Returns the ID of the Etc item after applying the mask.
 * @return int : ID of the Weapon
 */
@Override
public int getItemMask()
{
	return getItemType().mask();
}

/**
 * Returns the physical damage.
 * @return int
 */
public int getPDamage()
{
	return _pDam;
}

/**
 * Returns the random damage inflicted by the weapon
 * @return int
 */
public int getRandomDamage()
{
	return _rndDam;
}

/**
 * Returns the attack speed of the weapon
 * @return int
 */
public int getAttackSpeed()
{
	return _atkSpeed;
}

/**
 * Return the Attack Reuse Delay of the L2Weapon.<BR><BR>
 * @return int
 */
public int getAttackReuseDelay()
{
	return _atkReuse;
}

@Override
public final int getStandardShopItem()
{
	return _standardShopItem;
}

/**
 * Returns the rate of critical hit
 * @return int
 */
public int getCritical()
{
	return _critical;
}

/**
 * Returns the hit modifier of the weapon
 * @return double
 */
public double getHitModifier()
{
	return _hitModifier;
}

/**
 * Returns the magical damage inflicted by the weapon
 * @return int
 */
public int getMDamage()
{
	return _mDam;
}

/**
 * Returns the MP consumption with the weapon
 * @return int
 */
public int getMpConsume()
{
	return _mpConsume;
}

/**
 * Returns the shield defense of the weapon
 * @return int
 */
public int getShieldDef()
{
	return _shieldDef;
}

/**
 * Returns the rate of shield defense of the weapon
 * @return double
 */
public double getShieldDefRate()
{
	return _shieldDefRate;
}

/**
 * Returns passive skill linked to that weapon
 * @return
 */
public String[] getSkills()
{
	return _skill;
}

/**
 * Returns skill that player get when has equiped weapon +4  or more  (for duals SA)
 * @return
 */
public L2Skill getEnchant4Skill()
{
	return _enchant4Skill;
}

/**
 * Returns the Id in wich weapon this weapon can be changed
 * @return
 */
public int getChangeWeaponId()
{
	return _changeWeaponId;
}


/**
 * Returns array of Func objects containing the list of functions used by the weapon
 * @param instance : L2ItemInstance pointing out the weapon
 * @param player : L2Character pointing out the player
 * @return Func[] : array of functions
 */
@Override
public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
{
	List<Func> funcs = new FastList<Func>();
	if (_funcTemplates != null)
	{
		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.item = instance;
			Func f = t.getFunc(env, instance);
			if (f != null)
				funcs.add(f);
		}
	}
	return funcs.toArray(new Func[funcs.size()]);
}

/**
 * Returns effects of skills associated with the item to be triggered onHit.
 * @param caster : L2Character pointing out the caster
 * @param target : L2Character pointing out the target
 * @param crit : boolean tells whether the hit was critical
 * @return L2Effect[] : array of effects generated by the skill
 */
public boolean getSkillEffects(L2Character caster, L2Character target, boolean crit, L2ItemInstance item)
{
	final boolean icarus = item.getName().contains("Icarus");
	if (_skillsOnCrit == null || !crit || (icarus && item.getEnchantLevel() < 14))
		return false;
	Env env = new Env();
	env.player = caster;
	env.target = target;
	env.skill = _skillsOnCrit;
	if (icarus && env.skill != null)
	{
		if (item.getEnchantLevel() >= 20)
			env.skill.setLevel(3);
		else if (item.getEnchantLevel() >= 17)
			env.skill.setLevel(2);
	}
	env.item = item;
	if (!_skillsOnCritCondition.test(env))
		return false; // Skill condition not met
	
	byte shld = Formulas.calcShldUse(caster, target, _skillsOnCrit);
	if (!Formulas.calcSkillSuccess(caster, target, _skillsOnCrit, shld))
		return false; // These skills should not work on RaidBoss
	
	try
	{
		// Get the skill handler corresponding to the skill type
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(_skillsOnCrit.getSkillType());
		
		L2Character[] targets = new L2Character[1];
		targets[0] = target;
		
		// Launch the magic skill and calculate its effects
		if (handler != null)
			handler.useSkill(caster, _skillsOnCrit, targets);
		else
			_skillsOnCrit.useSkill(caster, targets);
		
		if (_skillsOnCrit.getSound() != null)
			caster.broadcastPacket(new PlaySound(0, _skillsOnCrit.getSound(), 1, caster.getObjectId(), caster.getX(), caster.getY(), caster.getZ()));
		
		// notify quests of a skill use
		if (caster instanceof L2PcInstance)
		{
			// Mobs in range 1000 see spell
			Collection<L2Object> objs = caster.getKnownList().getKnownObjects().values();
			//synchronized (caster.getKnownList().getKnownObjects())
			{
				for (L2Object spMob : objs)
				{
					if (spMob instanceof L2Npc)
					{
						L2Npc npcMob = (L2Npc) spMob;
						
						if (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null)
							for (Quest quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
								quest.notifySkillSee(npcMob, (L2PcInstance) caster, _skillsOnCrit, targets, false);
					}
				}
			}
		}
	}
	catch (IOException e)
	{
	}
	
	return true;
}

/**
 * Returns effects of skills associated with the item to be triggered onCast.
 * @param caster : L2Character pointing out the caster
 * @param target : L2Character pointing out the target
 * @param trigger : L2Skill pointing out the skill triggering this action
 * @return L2Effect[] : array of effects generated by the skill
 */
public boolean getSkillEffects(L2Character caster, L2Character target, L2Skill trigger, L2ItemInstance item)
{
	final boolean icarus = item.getName().contains("Icarus");
	
	if (_skillsOnCast == null || (icarus && item.getEnchantLevel() < 12))
		return false;
	if (trigger.isOffensive() != _skillsOnCast.isOffensive())
		return false; // Trigger only same type of skill
	
	if (trigger.isToggle() && _skillsOnCast.getSkillType() == L2SkillType.BUFF)
		return false; // No buffing with toggle skills
	
	Env env = new Env();
	env.player = caster;
	env.target = target;
	env.skill = _skillsOnCast;
	if (icarus && env.skill != null)
	{
		if (item.getEnchantLevel() >= 16)
			env.skill.setLevel(3);
		else if (item.getEnchantLevel() >= 14)
			env.skill.setLevel(2);
	}
	env.item = item;
	if (!_skillsOnCastCondition.test(env))
		return false;
	
	byte shld = Formulas.calcShldUse(caster, target, _skillsOnCast);
	if (_skillsOnCast.isOffensive() && !Formulas.calcSkillSuccess(caster, target, _skillsOnCast, shld))
		return false;
	
	try
	{
		// Get the skill handler corresponding to the skill type
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(_skillsOnCast.getSkillType());
		
		L2Character[] targets = new L2Character[1];
		targets[0] = target;
		
		// Launch the magic skill and calculate its effects
		if (handler != null)
			handler.useSkill(caster, _skillsOnCast, targets);
		else
			_skillsOnCast.useSkill(caster, targets);
		
		if (_skillsOnCast.getSound() != null)
			caster.broadcastPacket(new PlaySound(0, _skillsOnCast.getSound(), 1, caster.getObjectId(), caster.getX(), caster.getY(), caster.getZ()));
		
		// notify quests of a skill use
		if (caster instanceof L2PcInstance)
		{
			// Mobs in range 1000 see spell
			Collection<L2Object> objs = caster.getKnownList().getKnownObjects().values();
			//synchronized (caster.getKnownList().getKnownObjects())
			{
				for (L2Object spMob : objs)
				{
					if (spMob instanceof L2Npc)
					{
						L2Npc npcMob = (L2Npc) spMob;
						
						if (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null)
							for (Quest quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
								quest.notifySkillSee(npcMob, (L2PcInstance) caster, _skillsOnCast, targets, false);
					}
				}
			}
		}
	}
	catch (IOException e)
	{
	}
	
	return true;
}

/**
 * Add the L2Skill skill to the list of skills generated by the item triggered by critical hit
 * @param skill : L2Skill
 */
public void attachOnCrit(L2Skill skill, int chance)
{
	if (_skillsOnCrit == null)
	{
		_skillsOnCrit = skill;
		_skillsOnCritCondition = new ConditionGameChance(chance);
	}
}

/**
 * Add the L2Skill skill to the list of skills generated by the item triggered by casting spell
 * @param skill : L2Skill
 */
public void attachOnCast(L2Skill skill, int chance)
{
	_skillsOnCast = skill;
	_skillsOnCastCondition = new ConditionGameChance(chance);
}

public L2Skill getOnCastSkill()
{
	return _skillsOnCast;
}

public L2Skill getOnCritSkill()
{
	return _skillsOnCrit;
}

public Condition getOnCritSkillCondition()
{
	return _skillsOnCritCondition;
}

public Condition getOnCastSkillCondition()
{
	return _skillsOnCastCondition;
}
}