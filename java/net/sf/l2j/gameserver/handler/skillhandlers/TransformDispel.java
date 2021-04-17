package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class TransformDispel implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.TRANSFORMDISPEL
	};
	
	/**
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.actor.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
	 */
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		if (!(activeChar instanceof L2PcInstance))
			return;
		L2PcInstance pc = (L2PcInstance) activeChar;
		if (pc.isAlikeDead() || pc.isCursedWeaponEquipped())
			return;
		
		if (pc.isZombie())
		{
			activeChar.sendMessage("You can't untransform at this moment.");
			return;
		}
		if (pc.isTransformed() || pc.isInStance())
		{
			/*
			 * if (pc.isFlyingMounted() && !pc.isInsideZone(L2Character.ZONE_LANDING))
			 * pc.sendPacket(new SystemMessage(SystemMessageId.BOARD_OR_CANCEL_NOT_POSSIBLE_HERE));
			 * else
			 */
			pc.stopTransformation(null);
		}
	}
	
	/**
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}