package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerState;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetState extends Condition
{
private final PlayerState _check;
private final boolean _required;

public ConditionTargetState(PlayerState check, boolean required)
{
	_check = check;
	_required = required;
}

@Override
public boolean testImpl(Env env)
{
	if (env.target == null)
		return false;
	
	L2PcInstance target;
	switch (_check)
	{
	case RESTING:
		if (env.target instanceof L2PcInstance)
			return ((L2PcInstance) env.target).isSitting() == _required;
		return !_required;
	case MOVING:
		return env.target.isMoving() == _required;
	case RUNNING:
		return env.target.isMoving() == _required && env.target.isRunning() == _required;
	case FLYING:
		return env.target.isFlying() == _required;
	case BEHIND:
		return env.player != null && (env.player == env.target || env.target.isBehind(env.player) == _required);
	case COMBAT:
		return env.target.isInCombat() == _required;
	case CASTING:
		return env.target.isCastingNow() == _required;
	case ATTACKING:
		return env.target.isAttackingNow() == _required;
	case INVISIBLE:
		return env.target.isInvisible() == _required;
	case BLEEDING:
		return env.target.isBleeding() == _required;
	case FRONT:
		return env.player != null && (env.player == env.target || env.target.isInFrontOf(env.player) == _required);
	case CHAOTIC:
		target = env.target.getActingPlayer();
		if (target != null)
			return target.getKarma() > 0 == _required;
			return !_required;
	case FLAGGED:
		target = env.target.getActingPlayer();
		if (target != null)
			return target.getPvpFlag() > 0 == _required;
			return !_required;
	case OLYMPIAD:
		target = env.target.getActingPlayer();
		if (target != null)
			return target.isInOlympiadMode() == _required;
		return !_required;
	}
	return !_required;
}
}

