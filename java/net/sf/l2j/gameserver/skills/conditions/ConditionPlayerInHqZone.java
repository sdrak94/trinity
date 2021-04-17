package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerInHqZone extends Condition
{
private final boolean _HqZone;

public ConditionPlayerInHqZone(boolean val)
{
	_HqZone = val;
}

@Override
public boolean testImpl(Env env)
{
	return env.player != null && env.player instanceof L2Playable && env.player.getActingPlayer().isInHqZone() == _HqZone;
}

}
