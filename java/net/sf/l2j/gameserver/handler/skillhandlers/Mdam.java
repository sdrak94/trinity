package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Mdam implements ISkillHandler
{
protected static final Logger _log = Logger.getLogger(Mdam.class.getName());

private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.MDAM,
	L2SkillType.DEATHLINK
};


public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (activeChar.isAlikeDead())
		return;
	
	double dmgSoulMulti = 1;
	
	if (skill.getMaxSoulConsumeCount(activeChar) > 0 && activeChar instanceof L2PcInstance)
	{
		switch (((L2PcInstance)activeChar).getLastConsumedSoulAmount())
		{
		case 0:
			break;
		case 1:
			dmgSoulMulti = 1.10;
			break;
		case 2:
			dmgSoulMulti = 1.13;
			break;
		case 3:
			dmgSoulMulti = 1.16;
			break;
		case 4:
			dmgSoulMulti = 1.19;
			break;
		case 5:
			dmgSoulMulti = 1.22;
			break;
		case 6:
			dmgSoulMulti = 1.26;
			break;
		case 7:
			dmgSoulMulti = 1.30;
			break;
		case 8:
			dmgSoulMulti = 1.34;
			break;
		case 9:
			dmgSoulMulti = 1.38;
			break;
		default:
			dmgSoulMulti = 1.42;
			break;
		}
	}
	
	final SkillTargetType targetType = skill.getTargetType(activeChar);
	
	double mod = 0;
	
	if (skill.getAreaDmgTaper() != 1)
	{
		mod = 1 - skill.getAreaDmgTaper();
	}
	
	for (L2Character target: (L2Character[]) targets)
	{
		if (target.isDead())
			continue;
		
		if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance)target).isFakeDeath())
			target.stopFakeDeath(null);
		
		if (target == activeChar)
		{
			if (target.getFirstEffect(L2EffectType.INVINCIBLE) != null)
				target.stopEffects(L2EffectType.INVINCIBLE);
		}
		
		if (target != activeChar && Formulas.calcMagicalSkillEvasion(activeChar, target, skill))
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
		
		final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill), target);
		final byte shld = Formulas.calcShldUse(activeChar, target, skill);
		int damage = (int) Formulas.calcMagicDam(activeChar, target, skill, shld, true, true, mcrit);
		
		if (damage > 0)
		{
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
					double percentOfDistance = Math.min(Util.calculateDistance(activeChar, target, false)/skill.getSkillRadius(activeChar), 1);
					damage -= mod * percentOfDistance * damage;
					break;
				}
				case TARGET_AREA:
				case TARGET_BEHIND_AREA:
				{
					double percentOfDistance = Math.min(Util.calculateDistance(targets[0], target, false)/skill.getSkillRadius(activeChar), 1);
					damage -= mod * percentOfDistance * damage;
					break;
				}
				default:
					_log.warning("LOL WTF MDAM SKILL RADIUS TAPER IS DEFAULT SKILL TYPE");
				}
			}
			
			damage *= dmgSoulMulti;
			
			if (skill.getId() == 1439) //curse of divinity
			{
				int debuffCount = 0;
				
				for (L2Effect e : target.getAllEffects())
				{
					if (e != null && e.getSkill().isDebuff())
						debuffCount ++;
				}
				
				if (debuffCount > 0)
				{
					damage *= (1 + (debuffCount * 0.8));
				}
			}
			
			if (mcrit)
			{
				if (target.isBleeding())
				{
					final int bleedDmgExtra = (int)activeChar.calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, target, skill);
					
					if (bleedDmgExtra > 0)
					{
						damage += bleedDmgExtra;
						activeChar.sendMessage(target.getDisplayName()+" is bleeding and receives "+bleedDmgExtra+" more damage from your "+skill.getName());
					}
				}
			}

			if (activeChar instanceof L2PcInstance && target instanceof L2RaidBossInstance)
			{
				activeChar.incDamageCount((int)damage); 
				target.addRaidAttackerToList(activeChar);
			}
			activeChar.sendDamageMessage(target, damage, mcrit, false, false);
			
			if (target instanceof L2PcInstance)
				target.reduceCurrentHp(damage, activeChar, true, false, skill, skill.getDmgDirectlyToHP());
			else
				target.reduceCurrentHp(damage, activeChar, skill);
			
			double absorb = (int) activeChar.calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, target, skill);
			
			if (absorb > 0 && !target.isInvul() && damage > 0)
			{
				if (target.isUndead())
					absorb /= 2;
				
				int hpDrained = (int) (damage * (absorb / 100));
				
				if (hpDrained > 0)
				{
					final int finalHp = (int) Math.min(activeChar.getMaxHp(), hpDrained + activeChar.getCurrentHp());
					
					if (activeChar.getCurrentHp() < finalHp)
					{
						activeChar.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, false, 2500, 0, "Drained " + target.getDisplayName() + " for " + hpDrained + " HP"));
						
						if (!(target instanceof L2TesterInstance))
							activeChar.setCurrentHp(finalHp);
					}
				}
			}
			
			final byte reflect = Formulas.calcSkillReflect(activeChar, target, skill);
			
			// vengeance reflected damage
			if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
			{
				double reflectPercent = target.calcStat(Stats.VENGEANCE_SKILL_MAGIC_DAMAGE, -activeChar.calcStat(Stats.REFLECT_RES, 0, target, skill), activeChar, skill);
				
				if (reflectPercent > 100)
				{
					reflectPercent-=100;
					reflectPercent/=120;
					reflectPercent++;
					
					activeChar.reduceCurrentHp(damage*reflectPercent, target, skill);
				}
				else
					activeChar.reduceCurrentHp(damage, target, skill);
			}
			
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
			
			if (skill.hasEffects())
			{
				if (activeChar instanceof L2Playable && (reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
				{
					skill.getEffects(target, activeChar);
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
				else
				{
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
				}
			}
		}
		else // No - damage
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
		}
	}
	
	skill.getEffectsSelf(activeChar);
}

/**
 * 
 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
 */
public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}
