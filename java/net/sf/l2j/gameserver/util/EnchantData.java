package net.sf.l2j.gameserver.util;

import java.util.List;

import javolution.util.FastList;

public class EnchantData
{
	public int level;
	List<SkillHolder> _skills;
	
	public EnchantData(int level)
	{
		this.level = level;
		_skills = new FastList<SkillHolder>();
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public void addSkill(int id, int level)
	{
		_skills.add(new SkillHolder(id, level));
	}
}