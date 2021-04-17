package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class TakeFort implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.TAKEFORT
};

/**
 * 
 * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.actor.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
 */
public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (!(activeChar instanceof L2PcInstance))
		return;
	
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (player.getClan() == null)
		return;
	
	Fort fort = FortManager.getInstance().getFort(player);
	if (fort == null || !player.checkIfOkToCastFlagDisplay(fort, skill, targets[0]))
		return;
	
	try
	{
		fort.EndOfSiege(player.getClan());
	}
	catch (Exception e)
	{
	}
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}
