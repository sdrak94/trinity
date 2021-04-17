package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.Rnd;

public class Unlock implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.UNLOCK
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	L2Object[] targetList = skill.getTargetList(activeChar);
	
	if (targetList == null)
		return;
	
	for (L2Object target: targets)
	{
		boolean success = Formulas.calculateUnlockChance(skill);
		if (target instanceof L2DoorInstance)
		{
			L2DoorInstance door = (L2DoorInstance) target;
			if (!door.isUnlockable() || door.getFort() != null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.UNABLE_TO_UNLOCK_DOOR));
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (success && (!door.getOpen()))
			{
				door.openMe();
				door.onOpen();
				activeChar.sendMessage("Unlock the door!");
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
			}
		}
		else if (target instanceof L2ChestInstance)
		{
			L2ChestInstance chest = (L2ChestInstance) target;
			if (chest.getCurrentHp() <= 0 || chest.isInteracted())
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else
			{
				int chestChance = 0;
				int chestGroup = 0;
				int chestTrapLimit = 0;
				
				if (chest.getLevel() > 60)
					chestGroup = 4;
				else if (chest.getLevel() > 40)
					chestGroup = 3;
				else if (chest.getLevel() > 30)
					chestGroup = 2;
				else
					chestGroup = 1;
				
				switch (chestGroup)
				{
				case 1:
				{
					if (skill.getLevel() > 10)
						chestChance = 100;
					else if (skill.getLevel() >= 3)
						chestChance = 50;
					else if (skill.getLevel() == 2)
						chestChance = 45;
					else if (skill.getLevel() == 1)
						chestChance = 40;
					
					chestTrapLimit = 10;
				}
				break;
				case 2:
				{
					if (skill.getLevel() > 12)
						chestChance = 100;
					else if (skill.getLevel() >= 7)
						chestChance = 50;
					else if (skill.getLevel() == 6)
						chestChance = 45;
					else if (skill.getLevel() == 5)
						chestChance = 40;
					else if (skill.getLevel() == 4)
						chestChance = 35;
					else if (skill.getLevel() == 3)
						chestChance = 30;
					
					chestTrapLimit = 30;
				}
				break;
				case 3:
				{
					if (skill.getLevel() >= 14)
						chestChance = 50;
					else if (skill.getLevel() == 13)
						chestChance = 45;
					else if (skill.getLevel() == 12)
						chestChance = 40;
					else if (skill.getLevel() == 11)
						chestChance = 35;
					else if (skill.getLevel() == 10)
						chestChance = 30;
					else if (skill.getLevel() == 9)
						chestChance = 25;
					else if (skill.getLevel() == 8)
						chestChance = 20;
					else if (skill.getLevel() == 7)
						chestChance = 15;
					else if (skill.getLevel() == 6)
						chestChance = 10;
					
					chestTrapLimit = 50;
				}
				break;
				case 4:
				{
					if (skill.getLevel() >= 14)
						chestChance = 50;
					else if (skill.getLevel() == 13)
						chestChance = 45;
					else if (skill.getLevel() == 12)
						chestChance = 40;
					else if (skill.getLevel() == 11)
						chestChance = 35;
					
					chestTrapLimit = 80;
				}
				break;
				}
				if (Rnd.get(100) <= chestChance)
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					chest.setSpecialDrop();
					chest.setMustRewardExpSp(false);
					chest.setInteracted();
					chest.reduceCurrentHp(99999999, activeChar, skill);
				}
				else
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
					if (Rnd.get(100) < chestTrapLimit)
						chest.chestTrap(activeChar);
					chest.setInteracted();
					chest.addDamageHate(activeChar, 1);
					chest.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
				}
			}
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
