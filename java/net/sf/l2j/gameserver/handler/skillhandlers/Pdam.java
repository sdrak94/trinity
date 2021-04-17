package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
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

public class Pdam implements ISkillHandler
{

private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.PDAM, L2SkillType.FATAL
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
		
		if (!skillIsEvaded)
		{
			final boolean dual = activeChar.isUsingDualWeapon();
			final byte shld = Formulas.calcShldUse(activeChar, target, skill);
			boolean crit = false;
			
			final int combinedCritRate = (int)(activeChar.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, skill.getBaseCritRate() * Formulas.DEXbonus[activeChar.getDEX()], target, skill));
			
			if (combinedCritRate > 0)
				crit = Formulas.calcCrit(activeChar, combinedCritRate * 10, target, skill);
			
			int damage = 0;
			
			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, dual, true);
			
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
				
				if (crit)
				{
					damage *= activeChar.calcStat(Stats.PHYS_SKILL_CRITICAL_DAMAGE, 1.6, target, skill);
					
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
				
				damage *= dmgSoulMulti;

				if (activeChar instanceof L2PcInstance && target instanceof L2RaidBossInstance)
				{
					activeChar.incDamageCount((int)damage); 
					target.addRaidAttackerToList(activeChar);
				}

				if (target.calcStat(Stats.PDAM_MAX, 0, target, skill) > 0)
				{
					damage = (int) Math.min(damage, target.calcStat(Stats.PDAM_MAX, 0, activeChar, skill));
				}
				activeChar.sendDamageMessage(target, damage, false, crit, false);
				
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
					activeChar.reduceCurrentHp(damage, target, skill);
				
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