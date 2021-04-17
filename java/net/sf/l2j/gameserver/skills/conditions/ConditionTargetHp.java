package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetHp extends Condition
{
	private final int _hp;

	public ConditionTargetHp(int hp)
	{
		_hp = hp;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target != null && (env.target.getCurrentHp()*100/env.target.getMaxHp() <= _hp);
	}
}
