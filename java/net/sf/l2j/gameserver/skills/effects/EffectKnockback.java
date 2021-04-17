package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectKnockback extends L2Effect
{
public EffectKnockback(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.KNOCKBACK;
}

@Override
public boolean onStart()
{
	if (!(getEffected() instanceof L2Playable))
		return false;
	
	if (getEffected() instanceof L2PcInstance)
	{
		if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
		{
			L2PcInstance player = (L2PcInstance)getEffected();
			final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
			
			if (player != null)
			{
				if (getEffectTemplate()._ignoreProtection ? !player.canBeKnockedBack(effectorId) : !player.canBeKnockedBack(0))
				{
					getEffector().sendMessage(player.getDisplayName()+" is not affected by your "+getSkill().getName()+" because of 15 second knockback protection");
					return false;
				}
			}
			else return false;
		}
	}
	
	super.onStart();
	getEffector().doKnockback(getEffected(), getSkill());
	return true;
}

@Override
public boolean onActionTime()
{
	return false;
}
}