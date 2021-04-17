package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectBluff2 extends L2Effect
{
public EffectBluff2(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	if (getEffected() instanceof L2SiegeSummonInstance)
		return false;
	if (getEffected() instanceof L2TesterInstance)
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
	int posX = getEffected().getX();
	int posY = getEffected().getY();
	int posZ = getEffected().getZ();
	int signx = -1;
	int signy = -1;
	if (getEffected().getX() > getEffector().getX())
		signx = 1;
	if (getEffected().getY() > getEffector().getY())
		signy = 1;
	
	if (getEffected() instanceof L2PcInstance)
		getEffected().getActingPlayer().setIsSelectingTarget(10);
	super.onStart();
	if (getEffected() instanceof L2Playable)
		getEffected().setTarget(null);
	getEffected().abortAttack();
	getEffected().abortCast();
	
	getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX + (signx * 50), posY + (signy * 50), posZ, 0));
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