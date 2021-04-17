package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class CombatPointHeal implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.COMBATPOINTHEAL,
	L2SkillType.CPHEAL_PERCENT
};

public void useSkill(L2Character actChar, L2Skill skill, L2Object[] targets)
{
	//check for other effects
	try
	{
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);
		
		if (handler != null)
			handler.useSkill(actChar, skill, targets);
	}
	catch (Exception e)
	{
	}
	
	for (L2Character target: (L2Character[]) targets)
	{
		if(actChar instanceof L2PcInstance && (actChar.getActingPlayer().isInHuntersVillage() || actChar.getActingPlayer().isInOrcVillage()) && skill.getId() == 1305)
		{
			break;
		}
		double cp = skill.getPower(actChar);
		
		if (skill.getSkillType() == L2SkillType.CPHEAL_PERCENT)
			cp = target.getMaxCp() * cp / 100;
		//int cLev = activeChar.getLevel();
		//hp += skill.getPower()/*+(Math.sqrt(cLev)*cLev)+cLev*/;
		
		//from CT2 u will receive exact CP, u can't go over it, if u have full CP and u get CP buff, u will receive 0CP restored message
		else if ((target.getCurrentCp() + cp) >= target.getMaxCp())
		{
			cp = target.getMaxCp() - target.getCurrentCp();
		}
		
		if (cp > 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
			sm.addNumber((int) cp);
			target.setCurrentCp(cp + target.getCurrentCp());
			target.sendPacket(sm);
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_CP, (int) target.getCurrentCp());
			target.sendPacket(sump);
			
			if (targets.length == 1 && target != actChar)
				actChar.sendMessage("You healed "+target.getDisplayName()+" for "+(int)cp+" CP");
		}
	}
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}