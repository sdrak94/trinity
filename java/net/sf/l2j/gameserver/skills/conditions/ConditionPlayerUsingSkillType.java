package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

public final class ConditionPlayerUsingSkillType extends Condition
{
private final boolean _required;

public ConditionPlayerUsingSkillType(boolean required)
{
	_required = required;
}

@Override
public boolean testImpl(Env env)
{
	if (env.skill == null || env.player == null)
		return false;
	
	return env.skill.isMagic() == _required;
}
}