package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerHasSkill extends Condition
{
	private final int _skillId;

	public ConditionPlayerHasSkill(int skillId)
	{
		_skillId = skillId;
	}

	@Override
	public boolean testImpl(Env env)
	{		
		return env.player != null && env.player instanceof L2PcInstance && (env.player.getSkillLevel(_skillId) >= 1);
	}
}
