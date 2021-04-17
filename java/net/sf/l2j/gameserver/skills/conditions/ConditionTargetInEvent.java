package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetInEvent extends Condition
{
private final boolean _event;

public ConditionTargetInEvent(boolean val)
{
	_event = val;
}

@Override
public boolean testImpl(Env env)
{
	return env.target != null && env.target instanceof L2Playable && env.target.getActingPlayer().isInFunEvent() == _event;
}
}
