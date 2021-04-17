package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectImmobileAutoAttack extends L2Effect
{
public EffectImmobileAutoAttack(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.IMMOBILE_BUFF;
}

@Override
public boolean onStart()
{
	super.onStart();
	getEffected().setIsImmobilized(true);
	getEffected().setIsConfused(true);
	getEffected().setIsPhysicalMuted(true);
	getEffected().setIsMuted(true);
	getEffected().setPreventedFromReceivingBuffs(true);
	getEffected().startImmobileautoAttackTask(Math.max(getSkill().getAreaAngle(getEffected()), 5), getEffected().getHeading());
	return true;
}

@Override
public void onExit()
{
	getEffected().stopImmobileautoAttackTask();
	getEffected().abortAttack();
	getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	getEffected().setPreventedFromReceivingBuffs(false);
	getEffected().setIsImmobilized(false);
	getEffected().setIsPhysicalMuted(false);
	getEffected().setIsMuted(false);
	getEffected().setIsConfused(false);
	super.onExit();
}

@Override
public boolean onActionTime()
{
	return false;
}
}
