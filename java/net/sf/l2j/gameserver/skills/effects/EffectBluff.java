package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.network.serverpackets.StartRotation;
import net.sf.l2j.gameserver.network.serverpackets.StopRotation;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectBluff extends L2Effect
{

public EffectBluff(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.BLUFF;
}

@Override
public boolean onStart()
{
	/*	if (!(getEffected() instanceof L2Playable))
		return false;*/
	
	if (getEffected() instanceof L2SiegeSummonInstance)
		return false;
	
	if (getEffected() instanceof L2PcInstance)
	{
		if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
		{
			L2PcInstance player = (L2PcInstance)getEffected();
			final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
			
			if (player != null)
			{
				if (getEffectTemplate()._ignoreProtection ? !player.canBeStunned(effectorId) : !player.canBeStunned(0))
				{
					getEffector().sendMessage(player.getDisplayName()+" is not affected by your "+getSkill().getName()+" because of 19 second stun/bluff protection");
					return false;
				}
			}
			else return false;
			
			player.setStunProtectionTime(effectorId);
		}
	}
	
	getEffected().setIsBluffed(true);
	
	if (getEffected() instanceof L2PcInstance)
		getEffected().getActingPlayer().setIsSelectingTarget(10);
	
	if (getEffected() instanceof L2Playable)
		getEffected().setTarget(null);
	getEffected().abortAttack();
	getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	getEffected().abortCast();
	super.onStart();
	getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
	getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
	getEffected().setHeading(getEffector().getHeading());
	return true;
}

@Override
public void onExit()
{
	getEffected().setIsBluffed(false);
	super.onExit();
}

@Override
public boolean onActionTime()
{
	return false;
}
}