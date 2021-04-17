package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Trap implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.DETECT_TRAP,
	L2SkillType.REMOVE_TRAP,
	L2SkillType.DETECTION
};

@SuppressWarnings("incomplete-switch")
public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (activeChar == null || activeChar.isAlikeDead() || skill == null)
		return;
	
	switch (skill.getSkillType())
	{
	case DETECT_TRAP:
	{
		for (L2Character target: (L2Character[]) targets)
		{
			if (!(target instanceof L2TrapInstance))
				continue;
			
			if (target.isAlikeDead())
				continue;
			
			if (((L2TrapInstance)target).getLevel() <= skill.getPower())
			{
				(((L2TrapInstance)target)).setDetected();
				
				for (L2PcInstance player : target.getKnownList().getKnownPlayers().values())
				{
					if (player != null && !player.getKnownList().knowsObject(target))
						player.getKnownList().addKnownObject(target);
				}
				
				if (activeChar instanceof L2PcInstance)
					activeChar.sendMessage("A Trap has been detected!");
			}
		}
		break;
	}
	case REMOVE_TRAP:
	{
		for (L2Character target: (L2Character[]) targets)
		{
			if (!(target instanceof L2TrapInstance))
				continue;
			
			if (((L2TrapInstance)target).getLevel() > skill.getPower())
				continue;
			
			if (!activeChar.getKnownList().knowsObject(target))
				continue;
			
			((L2TrapInstance)target).unSummon();
			
			if (activeChar instanceof L2PcInstance)
				((L2PcInstance) activeChar).sendPacket(new SystemMessage(SystemMessageId.A_TRAP_DEVICE_HAS_BEEN_STOPPED));
		}
	}
	case DETECTION:
	{
		final boolean duel = activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isInDuel();
		final boolean arena = activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isInsideZone(L2Character.ZONE_PVP);
		final boolean peace = activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isInsideZone(L2Character.ZONE_PEACE);
		
		for (L2PcInstance target : activeChar.getKnownList().getKnownPlayersInRadius(skill.getSkillRadius(activeChar)))
		{
			if (target == null || target.isDead() || target.isGM() || target.inObserverMode() || !target.isInvisible())
				continue;
			
			if (duel)
			{
				if (target.getDuelId() != activeChar.getActingPlayer().getDuelId())
					continue;
			}
			else if (target.isInDuel())
				continue;
			
			if (target.isAutoAttackable(activeChar) || (activeChar instanceof L2Playable && activeChar.getActingPlayer().isInOneSideClanwarWith(target)))
			{
				target.stopEffects(L2EffectType.INVISIBLE);
				target.sendMessage("You have been detected");
				
				if (!arena && !peace && activeChar instanceof L2PcInstance)
					activeChar.getActingPlayer().updatePvPStatus(target);
			}
		}
	}
	}
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}