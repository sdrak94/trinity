package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerInEvent extends Condition
{
private final boolean _event;

public ConditionPlayerInEvent(boolean val)
{
	_event = val;
}

@Override
public boolean testImpl(Env env)
{
	return env.player != null && env.player instanceof L2Playable && env.player.getActingPlayer().isInFunEvent() == _event;
}
}
