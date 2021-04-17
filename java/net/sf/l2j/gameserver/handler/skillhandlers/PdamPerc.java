package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
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

public class PdamPerc implements ISkillHandler
{

private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.PDAMPERC
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
		if (target == null) continue;
		
		if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance)target).isFakeDeath())
		{
			target.stopFakeDeath(null);
		}
		else if (target.isDead())
			continue;
		
		if (target == activeChar)
		{
			if (target.getFirstEffect(L2EffectType.INVINCIBLE) != null)
				target.stopEffects(L2EffectType.INVINCIBLE);
		}
		
		final boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(activeChar, target, skill);
		
		boolean success = true;
		
		if (!skillIsEvaded)
		{
			if ((skill.getCondition() & L2Skill.COND_BEHIND) != 0)
				success = activeChar.isBehind(target);
			
			boolean crit = false;
			final int combinedCritRate = (int)(activeChar.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, skill.getBaseCritRate() * Formulas.DEXbonus[activeChar.getDEX()], target, skill));
			if (combinedCritRate > 0)
				crit = Formulas.calcCrit(activeChar, combinedCritRate * 10, target, skill);
			
			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				success = false;
			
			int damage = 0;
			byte shld = 0;
			
			if (success)
			{
				damage = (int) (target.getCurrentHp() * skill.getPower(activeChar) / 100);
				
				shld = Formulas.calcShldUse(activeChar, target, skill);
				
				if (shld == 1)
					damage /= 1.6;
				else if (shld == 2)
					damage /= 3.5;
				
				if (target instanceof L2Attackable || target instanceof L2DoorInstance)
					damage = Math.min(damage, 40000);
			}
			
			if (damage > 0)
			{
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

				if (activeChar instanceof L2PcInstance && target instanceof L2RaidBossInstance)
				{
					activeChar.incDamageCount((int)damage); 
					target.addRaidAttackerToList(activeChar);
				}
				activeChar.sendDamageMessage(target, damage, false, false, false);
				
				if (target instanceof L2PcInstance)
				{
					target.reduceCurrentHp(damage, activeChar, true, false, skill, true);
					target.sendMessage(activeChar.getDisplayName() + "just removed "+(int)skill.getPower(activeChar)+"% of your current HP!");
				}
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
				
				Formulas.calcLethalHit(activeChar, target, skill);
				
				if (skill.hasEffects())
				{
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
				}
			}
			else // No - damage
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}
		else
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
		}
		
		if (activeChar instanceof L2PcInstance)
		{
			L2Skill soulmastery = SkillTable.getInstance().getInfo(467, ((L2PcInstance) activeChar).getSkillLevel(467));
			
			if (soulmastery != null)
			{
				final int totalMaxSouls = (int) (activeChar.calcStat(Stats.SOUL_MAX, soulmastery.getNumSouls(), null, null));
				
				if (((L2PcInstance) activeChar).getSouls() < totalMaxSouls)
				{
					int count = 0;
					
					if (((L2PcInstance) activeChar).getSouls() + skill.getNumSouls() <= totalMaxSouls)
						count = skill.getNumSouls();
					else
						count = totalMaxSouls - ((L2PcInstance) activeChar).getSouls();
					
					if (count > 0)
						((L2PcInstance) activeChar).increaseSouls(count);
				}
			}
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