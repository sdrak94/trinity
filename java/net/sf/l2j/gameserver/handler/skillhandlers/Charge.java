package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Charge implements ISkillHandler
{
static Logger _log = Logger.getLogger(Charge.class.getName());

private static final L2SkillType[] SKILL_IDS =
{/*L2SkillType.CHARGE*/};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	for (L2Object target: targets)
	{
		if (!(target instanceof L2PcInstance))
			continue;
		skill.getEffects(activeChar, (L2PcInstance) target);
	}
	
	skill.getEffectsSelf(activeChar);
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}
