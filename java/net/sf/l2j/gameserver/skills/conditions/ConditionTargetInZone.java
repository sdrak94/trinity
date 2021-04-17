package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetInZone extends Condition
{
private final String _zoneName;

public ConditionTargetInZone(String zoneName)
{
	_zoneName = zoneName;
}

@Override
public boolean testImpl(Env env)
{
	if (env.target == null || _zoneName == null || _zoneName.equalsIgnoreCase(""))
		return false;
	
	if (_zoneName.equalsIgnoreCase("gludin"))
		return env.target instanceof L2Playable && env.target.getActingPlayer().isInGludin();
	
	if (_zoneName.equalsIgnoreCase("huntervillage"))
		return env.target instanceof L2Playable && env.target.getActingPlayer().isInHuntersVillage();
	
	if (_zoneName.equalsIgnoreCase("szone"))
		return env.target instanceof L2Playable && env.target.getActingPlayer().isInSgradeZone();
	
	if (_zoneName.equalsIgnoreCase("s80zone"))
		return env.target instanceof L2Playable && env.target.getActingPlayer().isInS80zone();
	
	return false;
}
}
