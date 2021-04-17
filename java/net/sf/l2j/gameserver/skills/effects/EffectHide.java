package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class EffectHide extends L2Effect
{
public EffectHide(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

public EffectHide(Env env, L2Effect effect)
{
	super(env, effect);
}

@Override
public boolean onStart()
{
	super.onStart();
	getEffected().setInvisible(true);
	getEffected().startAbnormalEffect(AbnormalEffect.STEALTH);
	
	L2PcInstance activeChar = null;
	
	if (getEffected() instanceof L2PcInstance)
	{
		activeChar = ((L2PcInstance)getEffected());
		
		if (activeChar.getAI().getNextIntention() != null
				&& activeChar.getAI().getNextIntention().getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	L2GameServerPacket del = new DeleteObject(getEffected());
	
	for (L2Character nigga : getEffected().getKnownList().getKnownCharacters())
	{
		if (nigga != null && nigga != getEffected() && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(getEffected()))
		{
			if (nigga instanceof L2PcInstance)
			{
				if (!nigga.canSeeInvisiblePeople() || (activeChar != null && activeChar.getAccessLevel().getLevel() >= ((L2PcInstance)nigga).getAccessLevel().getLevel()))
					nigga.sendPacket(del);
				else
					getEffected().sendInfo((L2PcInstance) nigga);
			}
			
			if (nigga.getTarget() != null && nigga.getTarget() == getEffected())
			{
				if (nigga instanceof L2PcInstance)
				{
					((L2PcInstance)nigga).setIsSelectingTarget(3);
				}
				
				nigga.setTarget(null);
				nigga.abortCast();
				nigga.abortAttack();
			}
			
			if (nigga.hasAI())
			{
				if (nigga.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK && nigga.getAI().getAttackTarget() != null && nigga.getAI().getAttackTarget() == getEffected())
					nigga.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				else if (nigga.getAI().getIntention() == CtrlIntention.AI_INTENTION_CAST && nigga.getAI().getCastTarget() != null && nigga.getAI().getCastTarget() == getEffected())
					nigga.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				else if (nigga.getAI().getIntention() == CtrlIntention.AI_INTENTION_FOLLOW && nigga.getAI().getFollowTarget() != null && nigga.getAI().getFollowTarget() == getEffected())
					nigga.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
		}
	}
	
	return true;
}

@Override
public void onExit()
{
	if (!getEffected().isGM())
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.STEALTH);
		getEffected().setInvisible(false);
	}
	
	super.onExit();
	
	if (getEffected().getPet() != null)
		getEffected().getPet().updateAbnormalEffect();
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.INVISIBLE;
}

@Override
public boolean onActionTime()
{
	// Only cont skills shouldn't end
	if(getSkill().getSkillType() != L2SkillType.CONT)
		return false;
	
	if(getEffected().isDead())
		return false;
	
	double manaDam = calc();
	
	if(manaDam > getEffected().getStatus().getCurrentMp())
	{
		SystemMessage sm = new SystemMessage(140);
		getEffected().sendPacket(sm);
		return false;
	}
	
	getEffected().reduceCurrentMp(manaDam);
	return true;
}
}