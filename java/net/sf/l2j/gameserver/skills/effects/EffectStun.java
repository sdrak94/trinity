package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectStun extends L2Effect
{

public EffectStun(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
 */
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.STUN;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	if (getEffected().isStunned())
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
	super.onStart();
	getEffected().startStunning();
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	getEffected().stopStunning(this);
	super.onExit();
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	// just stop this effect
	return false;
}
}
