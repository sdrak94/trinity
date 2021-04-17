package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import net.sf.l2j.gameserver.skills.Env;

public final class ConditionUsingSkill extends Condition
{
	public final int _skillId;

	public ConditionUsingSkill(int skillId)
	{
		_skillId = skillId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (env.player == null || !(env.player instanceof L2PcInstance))
			return false;
		
		final L2PcInstance player = (L2PcInstance)env.player;
		
		final SkillDat skill = player.getCurrentSkill();
		
		return skill != null && skill.getSkillId() == _skillId;
	}
}
