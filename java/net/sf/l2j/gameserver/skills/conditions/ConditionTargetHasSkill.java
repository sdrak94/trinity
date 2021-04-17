package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetHasSkill extends Condition
{
	private final int _skillId;

	public ConditionTargetHasSkill(int skillId)
	{
		_skillId = skillId;
	}

	@Override
	public boolean testImpl(Env env)
	{		
		return env.target != null && env.target instanceof L2PcInstance && (env.target.getSkillLevel(_skillId) >= 1);
	}
}
