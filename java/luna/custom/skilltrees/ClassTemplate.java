package luna.custom.skilltrees;

import java.util.ArrayList;

import net.sf.l2j.gameserver.model.L2Skill;

public class ClassTemplate
{
	private final int					_id;
	private final String				_name;
	private final ArrayList<L2Skill>	_skills;
	
	public ClassTemplate(int id, String name, ArrayList<L2Skill> skills)
	{
		_id = id;
		_name = name;
		_skills = skills;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public ArrayList<L2Skill> getSkills()
	{
		return _skills;
	}
}
