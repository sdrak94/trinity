package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class InstantJump implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.INSTANT_JUMP
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	final L2Character target = (L2Character)targets[0];
	
	if (target == null) return;
	
	int x=0,y=0,z=0;
	
	int px = target.getX();
	int py = target.getY();
	double ph = Util.convertHeadingToDegree(target.getHeading());
	
	ph+=180;
	
	if(ph>360)
		ph-=360;
	
	ph = (Math.PI * ph) / 180;
	
	x = (int) (px + (25 * Math.cos(ph)));
	y = (int) (py + (25 * Math.sin(ph)));
	z = target.getZ();
	
	activeChar.abortAttack();
	if (activeChar instanceof L2Playable)
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	activeChar.abortCast();
	
	final int id, x1, y1, z1;
	
	id = activeChar.getObjectId();
	x1 = activeChar.getX();
	y1 = activeChar.getY();
	z1 = activeChar.getZ();
	
	if (Config.GEODATA > 0)
	{
		Location destiny = GeoData.getInstance().moveCheck(x1, y1, z1, x, y, z, activeChar.getInstanceId());
		x = destiny.getX();
		y = destiny.getY();
		z = destiny.getZ();
	}
	
	for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
	{
		if (player != null && player != activeChar)
		{
			if (player.getTarget() != null && player.getTarget() == activeChar)
			{
				if (player.isAutoAttackable(activeChar))
				{
					player.setIsSelectingTarget(4);
					player.setTarget(null);
					player.abortAttack();
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					player.abortCast();
				}
			}
		}
	}
	
	activeChar.broadcastPacket(new FlyToLocation(id, x1, y1, z1, x, y, z, FlyType.DUMMY));
	activeChar.getPosition().setXYZ(x, y, z);
	
	if (skill.hasEffects())
	{
		if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
		{
			skill.getEffects(target, activeChar);
		}
		else
		{
			byte shld = Formulas.calcShldUse(activeChar, target, skill);
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				skill.getEffects(activeChar, target, new Env(shld, false, false, false));
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
			}
		}
	}
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}