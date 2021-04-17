package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerInZone extends Condition
{
private final String _zoneName;

public ConditionPlayerInZone(String zoneName)
{
	_zoneName = zoneName;
}

@Override
public boolean testImpl(Env env)
{
	if (env.player == null || _zoneName == null || _zoneName.equalsIgnoreCase(""))
		return false;
	
	if (_zoneName.equalsIgnoreCase("gludin"))
		return env.player instanceof L2Playable && env.player.getActingPlayer().isInGludin();
	
	if (_zoneName.equalsIgnoreCase("huntervillage"))
		return env.player instanceof L2Playable && env.player.getActingPlayer().isInHuntersVillage();
	
	if (_zoneName.equalsIgnoreCase("szone"))
		return env.player instanceof L2Playable && env.player.getActingPlayer().isInSgradeZone();
	
	if (_zoneName.equalsIgnoreCase("s80zone"))
		return env.player instanceof L2Playable && env.player.getActingPlayer().isInS80zone();
	
	return false;
}
}
