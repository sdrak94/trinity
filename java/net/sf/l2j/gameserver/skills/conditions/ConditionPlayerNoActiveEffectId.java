package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerNoActiveEffectId extends Condition
{
private final int _effectId;
private final int _effectLvl;

public ConditionPlayerNoActiveEffectId(int effectId)
{
	_effectId = effectId;
	_effectLvl = -1;
}

@Override
public boolean testImpl(Env env)
{
	for (L2Effect e : env.player.getAllEffects())
	{
		if (e != null)
		{
			if (e.getSkill().getId() == _effectId)
			{
				if (_effectLvl == -1 || _effectLvl <= e.getSkill().getLevel())
					return false;
			}
		}
	}
	return true;
}
}
