package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class ShiftTarget implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.SHIFT_TARGET
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (targets == null)
		return;
	L2Character target = (L2Character) targets[0];
	
	if (activeChar.isAlikeDead() || target == null)
		return;
	
	for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius(activeChar)))
	{
		if (!(obj instanceof L2Attackable) || obj.isDead())
			continue;
		L2Attackable hater = ((L2Attackable) obj);
		if (hater.getHating(activeChar) == 0)
			continue;
		hater.addDamageHate(target, hater.getHating(activeChar));
		
	}
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
