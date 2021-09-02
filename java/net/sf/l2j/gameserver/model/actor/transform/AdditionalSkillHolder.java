package net.sf.l2j.gameserver.model.actor.transform;

import net.sf.l2j.gameserver.util.SkillHolder;

public class AdditionalSkillHolder extends SkillHolder
{
	private final int _minLevel;
	
	public AdditionalSkillHolder(int skillId, int skillLevel, int minLevel)
	{
		super(skillId, skillLevel);
		_minLevel = minLevel;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
}