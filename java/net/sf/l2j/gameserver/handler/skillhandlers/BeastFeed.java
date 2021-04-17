package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class BeastFeed implements ISkillHandler
{
	private static Logger _log = Logger.getLogger(BeastFeed.class.getName());
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.BEAST_FEED
	};
	
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;
		
		L2Object[] targetList = skill.getTargetList(activeChar);
		
		if (targetList == null)
		{
			return;
		}
		
		_log.fine("Beast Feed casting succeded.");
		
		// This is just a dummy skill handler for the golden food and crystal food skills,
		// since the AI responce onSkillUse handles the rest.
		
	}
	
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
