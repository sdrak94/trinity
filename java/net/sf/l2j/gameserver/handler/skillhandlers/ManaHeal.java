package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class ManaHeal implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.MANAHEAL,
	L2SkillType.MANARECHARGE,
	L2SkillType.MANAHEAL_PERCENT
};

public void useSkill(L2Character actChar, L2Skill skill, L2Object[] targets)
{
	for (L2Character target: (L2Character[]) targets)
	{
		double mp = skill.getPower(actChar);
		if (skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT)
		{
			//double mp = skill.getPower();
			mp = target.getMaxMp() * mp / 100.0;
		}
		else
		{
			mp = (skill.getSkillType() == L2SkillType.MANARECHARGE) ? target.calcStat(Stats.RECHARGE_MP_RATE, mp, null, skill) : mp;
		}
		//from CT2 u will receive exact MP, u can't go over it, if u have full MP and u get MP buff, u will receive 0MP restored message
		if ((target.getCurrentMp() + mp) >= target.getMaxMp())
		{
			mp = target.getMaxMp() - target.getCurrentMp();
		}
		if (mp > 0)
		{
			//int cLev = activeChar.getLevel();
			//hp += skill.getPower()/*+(Math.sqrt(cLev)*cLev)+cLev*/;
			target.setLastHealAmount((int) mp);
			target.setCurrentMp(mp + target.getCurrentMp());
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
			target.sendPacket(sump);
			
			if (actChar instanceof L2PcInstance && actChar != target)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_RESTORED_BY_C1);
				sm.addCharName(actChar);
				sm.addNumber((int) mp);
				target.sendPacket(sm);
				
				if (targets.length == 1 && target != actChar)
					actChar.sendMessage("You recharged "+target.getDisplayName()+" for "+(int)mp+" MP");
//				for (final L2Attackable att : target.getKnownList().getKnownType(L2Attackable.class))
//					if (att.getMostHated() == target || att.getTarget() == target)
//						att.addDamageHate(actChar, skill.getAggroPoints());
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_MP_RESTORED);
				sm.addNumber((int) mp);
				target.sendPacket(sm);
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
