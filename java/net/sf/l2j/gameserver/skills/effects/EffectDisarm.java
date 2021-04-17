package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectDisarm extends L2Effect
{
public EffectDisarm(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	return L2EffectType.DISARM;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	if (!(getEffected() instanceof L2PcInstance))
		return false;
	
	L2PcInstance player = (L2PcInstance) getEffected();
	final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
	
	if (player != null)
	{
		if (player.isCombatFlagEquipped())
		{
			if (getEffector().getTarget() != null && player == getEffector().getTarget())
				getEffector().sendMessage("You cannot disarm combat flags");
			
			return false;
		}
		
		if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
		{
			if (getEffectTemplate()._ignoreProtection ? !player.canBeDisarmed(effectorId) : !player.canBeDisarmed(0))
			{
				getEffector().sendMessage(player.getDisplayName() + " is not affected by your " + getSkill().getName() + " because of 14 second disarm protection");
				return false;
			}
		}
	}
	else
		return false;
	
	super.onStart();
	player.setDisarmProtectionTime(effectorId);
	
	player.disarmWeapons();
	player.setIsDisarmed(true);
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	getEffected().setIsDisarmed(false);
	super.onExit();
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	return false;
}
}
