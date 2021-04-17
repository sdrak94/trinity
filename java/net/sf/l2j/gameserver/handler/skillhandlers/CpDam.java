package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class CpDam implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.CPDAM
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (activeChar.isAlikeDead())
		return;
	
	for (L2Character target: (L2Character[]) targets)
	{
		if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance)target).isFakeDeath())
		{
			target.stopFakeDeath(null);
		}
		else if (target.isDead())
		{
			continue;
		}
		
		byte shld = Formulas.calcShldUse(activeChar, target, skill);
		
		double power = skill.getPower(activeChar);
		
		if (power > 100)
			power = 100;
		else if (power < 5)
			power = 5;
		
		final int currentCp = (int)target.getCurrentCp();
		
		int damage = (int) (currentCp * (power/100));

		if (target.calcStat(Stats.PDAM_MAX, 0, target, skill) > 0)
		{
			damage = (int) Math.min(damage, target.calcStat(Stats.PDAM_MAX, 0, activeChar, skill));
		}
		activeChar.sendDamageMessage(target, damage, false, false, false);
		target.setCurrentCp(currentCp - damage);
		
		if (skill.hasEffects())
			skill.getEffects(activeChar, target, new Env(shld, true, true, true));
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