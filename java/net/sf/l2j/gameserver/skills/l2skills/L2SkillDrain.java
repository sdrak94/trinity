package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;

public class L2SkillDrain extends L2Skill
{
private final float _absorbPart;
private final int _absorbAbs;

public L2SkillDrain(StatsSet set)
{
	super(set);
	
	_absorbPart = set.getFloat("absorbPart", 0.f);
	_absorbAbs  = set.getInteger("absorbAbs", 0);
}

@Override
public void useSkill(L2Character activeChar, L2Object[] targets)
{
	if (activeChar.isAlikeDead())
		return;
	
	final boolean corpseDrain = getTargetType(activeChar) == SkillTargetType.TARGET_CORPSE_DRAIN ||
	getTargetType(activeChar) == SkillTargetType.TARGET_CORPSE_DRAIN_AOE;
	
	final boolean corseMob = getTargetType(activeChar) == SkillTargetType.TARGET_CORPSE_MOB;
	
	float absorbPart = _absorbPart; //absolutely needed
	
	if (absorbPart > 0)
		absorbPart = (float) activeChar.calcStat(Stats.DRAIN_PERC_BOOST, absorbPart, null, this);
	
	final SkillTargetType targetType = getTargetType(activeChar);
	
	double mod = 0;
	
	if (getAreaDmgTaper() != 1)
	{
		mod = 1 - getAreaDmgTaper();
	}
	
	for (L2Character target : (L2Character[]) targets)
	{
		if (target == null)
			continue;
		
		if (target.isDead() && !corseMob && !corpseDrain)
			continue;
		
		if (target.getActingPlayer() != null && target.getActingPlayer() == activeChar.getActingPlayer())
			continue;
		
		if (activeChar != target && target.isInvul())
			continue; // No effect on invulnerable chars unless they cast it themselves.
		
		if (!target.isDead() && target != activeChar && Formulas.calcMagicalSkillEvasion(activeChar, target, this))
		{
			if (activeChar instanceof L2PcInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_DODGES_ATTACK);
				sm.addCharName(target);
				((L2PcInstance) activeChar).sendPacket(sm);
			}
			if (target instanceof L2PcInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_C1_ATTACK);
				sm.addCharName(activeChar);
				((L2PcInstance) target).sendPacket(sm);
			}
			
			continue;
		}
		
		boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this), target);
		byte shld = Formulas.calcShldUse(activeChar, target, this);
		int damage = (int) Formulas.calcMagicDam(activeChar, target, this, shld, true, true, mcrit);
		
		if (activeChar == target && targetType == SkillTargetType.TARGET_ALL)
			damage *= 1.8;
		
		if (mod != 0)
		{
			switch (targetType)
			{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_FRONT_AREA:
			case TARGET_ALL:
			{
				double percentOfDistance = Math.min(Util.calculateDistance(activeChar, target, false)/getSkillRadius(activeChar), 1);
				damage -= mod * percentOfDistance * damage;
				break;
			}
			case TARGET_AREA:
			case TARGET_BEHIND_AREA:
			{
				double percentOfDistance = Math.min(Util.calculateDistance(targets[0], target, false)/getSkillRadius(activeChar), 1);
				damage -= mod * percentOfDistance * damage;
				break;
			}
			default:
				_log.warning("LOL WTF MDAM SKILL RADIUS TAPER IS DEFAULT SKILL TYPE");
			}
		}
		
		if (!target.isDead() && target.isBleeding())
		{
			final int bleedDmgExtra = (int) activeChar.calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, target, this);
			
			if (bleedDmgExtra > 0)
			{
				damage += bleedDmgExtra;
				activeChar.sendMessage(target.getDisplayName() + " is bleeding and receives " + bleedDmgExtra + " more damage from your " + getName());
			}
		}
		
		int _drain = 0;
		int _cp = (int) target.getCurrentCp();
		int _hp = (int) target.getCurrentHp();
		
		if (_cp > 0)
		{
			if (damage < _cp)
				_drain = 0;
			else
				_drain = damage - _cp;
		}
		else if (damage > _hp)
			_drain = _hp;
		else
			_drain = damage;
		
		double hpAdd = _absorbAbs + absorbPart * _drain;
		
		if (target instanceof L2Attackable && target.isUndead())
			hpAdd /= 2;
		
		double hp = ((activeChar.getCurrentHp() + hpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : (activeChar.getCurrentHp() + hpAdd));
		
		// Check to see if we should damage the target
		if (damage > 0 && (!target.isDead() || !corseMob || !corpseDrain))
		{
			/*// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}*/

			if (activeChar instanceof L2PcInstance && target instanceof L2RaidBossInstance)
			{
				activeChar.incDamageCount((int)damage); 
				target.addRaidAttackerToList(activeChar);
			}
			activeChar.sendDamageMessage(target, damage, mcrit, false, false);
			
			if (target instanceof L2PcInstance)
				target.reduceCurrentHp(damage, activeChar, true, false, this, getDmgDirectlyToHP());
			else
				target.reduceCurrentHp(damage, activeChar, this);
			
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, this);
		}
		
		int hpDrained = (int) (hp - activeChar.getCurrentHp());
		
		if (corpseDrain)
			activeChar.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, false, 2500, 0, "Drained " + target.getDisplayName() + "'s corpse for " + hpDrained + " HP"));
		else
			activeChar.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, false, 2500, 0, "Drained " + target.getDisplayName() + " for " + hpDrained + " HP"));
		
		if (!(target instanceof L2TesterInstance))
		{
			activeChar.setCurrentHp(hp);
		}
		
		if (hasEffects() && !corseMob && !corpseDrain)
		{
			// ignoring vengance-like reflections
			if (activeChar instanceof L2Playable && (Formulas.calcSkillReflect(activeChar, target, this) & Formulas.SKILL_REFLECT_SUCCEED) > 0)
			{
				getEffects(target, activeChar);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				sm.addSkillName(getId());
				activeChar.sendPacket(sm);
			}
			else
			{
				getEffects(activeChar, target);
			}
		}
		
		// Check to see if we should do the decay right after the cast
		if (target.isDead() && corseMob && target instanceof L2Npc)
			((L2Npc) target).endDecayTask();
	}
	
	// cast self effect if any
	getEffectsSelf(activeChar);
}

public void useCubicSkill(L2CubicInstance activeCubic, L2Object[] targets)
{
	if (Config.DEBUG)
		_log.info("L2SkillDrain: useCubicSkill()");
	
	for (L2Character target : (L2Character[]) targets)
	{
		if (target.isAlikeDead() && getTargetType(null) != SkillTargetType.TARGET_CORPSE_MOB)
			continue;
		
		boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, this), target);
		byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, this);
		
		int damage = (int) Formulas.calcMagicDam(activeCubic, target, this, mcrit, shld);
		if (Config.DEBUG)
			_log.info("L2SkillDrain: useCubicSkill() -> damage = " + damage);
		
		L2PcInstance owner = activeCubic.getOwner();
		
		if (!(target instanceof L2TesterInstance))
		{
			double hpAdd = _absorbAbs + _absorbPart * damage;
			double hp = ((owner.getCurrentHp() + hpAdd) > owner.getMaxHp() ? owner.getMaxHp() : (owner.getCurrentHp() + hpAdd));
			owner.setCurrentHp(hp);
		}
		
		// Check to see if we should damage the target
		if (damage > 0 && (!target.isDead() || getTargetType(null) != SkillTargetType.TARGET_CORPSE_MOB))
		{
			target.reduceCurrentHp(damage, activeCubic.getOwner(), this);
			
			/*// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}*/
			owner.sendDamageMessage(target, damage, mcrit, false, false);
		}
	}
}
}