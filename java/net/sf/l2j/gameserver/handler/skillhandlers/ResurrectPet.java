package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class ResurrectPet implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.RESURRECT_PET
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (activeChar.getPet() == null)
	{
		activeChar.sendMessage("No pet.");
		return;
	}
	else
	{
		L2Character player = null;
		if (activeChar instanceof L2PcInstance)
			player = (L2Character) activeChar;
		{
			player.getPet().doRevive();
			player.getPet().teleToLocation(player.getPet().getOwner().getX(), player.getPet().getOwner().getY(), player.getPet().getOwner().getZ(), true);
		}
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
