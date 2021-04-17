package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Resurrect implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.RESURRECT
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (skill.getId() == 1125) {
	L2PcInstance player = null;
	if (activeChar instanceof L2PcInstance) {
		player = (L2PcInstance)activeChar;
	}
	if (!player.isSummoner()) {
		player.sendMessage("You are not a summoner.");
		
		return;
	} 
	if (player.getPet() == null) {
		player.sendMessage("You don't have a pet.");
		
		return;
	} 
	L2Summon summon = player.getPet();
	
	if (!summon.isDead()) {
		player.sendMessage("Your summon is alive.");
		
		return;
	} 
	summon.setTitle(activeChar.getDisplayName());
	
	summon.setCurrentHp(summon.getMaxHp());
	summon.setCurrentMp(summon.getMaxMp());
	summon.setHeading(activeChar.getHeading());
	summon.setRunning();
	summon.setInstanceId(activeChar.getInstanceId());
	
	L2World.getInstance().storeObject((L2Object)summon);
	summon.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	
	summon.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
	summon.setFollowStatus(true);
	
	return;
	} 
	if (activeChar.calcStat(Stats.RES_DISABLE, 0, null, skill) > 0)
	{
		if (activeChar.calcStat(Stats.RES_UNDISABLE, 0, null, skill) <= 0)
		{
			activeChar.sendMessage("Your ress fizzles because your soul is departed");
			return;
		}
	}
	
	L2PcInstance player = null;
	if (activeChar instanceof L2PcInstance)
		player = (L2PcInstance) activeChar;
	
	L2PcInstance targetPlayer;
	List<L2Character> targetToRes = new FastList<L2Character>();
	
	for (L2Character target: (L2Character[]) targets)
	{
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
			
			// Check for same party or for same clan, if target is for clan.
			if (skill.getTargetType(activeChar) == SkillTargetType.TARGET_CORPSE_CLAN)
			{
				if (player.getClanId() != targetPlayer.getClanId())
					continue;
			}
		}
		if (target.isVisible())
		{
			if (target.calcStat(Stats.RES_DISABLE, 0, null, skill) > 0)
			{
				if (activeChar.calcStat(Stats.RES_UNDISABLE, 0, null, skill) <= 0)
				{
					activeChar.sendMessage("Your ress fizzles because "+target.getDisplayName()+"'s soul is departed");
				}
				else
					targetToRes.add(target);
			}
			else
				targetToRes.add(target);
		}
	}
	
	if (targetToRes.isEmpty())
		return;
	
	for (L2Character cha : targetToRes)
		if (activeChar instanceof L2PcInstance)
		{
			if (cha instanceof L2PcInstance)
				((L2PcInstance) cha).reviveRequest((L2PcInstance) activeChar, skill, false);
			else if (cha instanceof L2PetInstance)
			{
				if (((L2PetInstance) cha).getOwner() == activeChar)
					cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
				else
					((L2PetInstance) cha).getOwner().reviveRequest((L2PcInstance) activeChar, skill, true);
			}
			else
				cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
		}
		else
		{
			DecayTaskManager.getInstance().cancelDecayTask(cha);
			cha.doRevive(Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
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
