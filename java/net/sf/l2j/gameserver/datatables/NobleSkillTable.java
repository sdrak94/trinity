package net.sf.l2j.gameserver.datatables;

import net.sf.l2j.gameserver.model.L2Skill;

public class NobleSkillTable
{
	private static L2Skill[] _nobleSkills;
	
	private NobleSkillTable()
	{
		_nobleSkills = new L2Skill[8];
		_nobleSkills[0] = SkillTable.getInstance().getInfo(326, 1);
		_nobleSkills[1] = SkillTable.getInstance().getInfo(327, 1);
		_nobleSkills[2] = SkillTable.getInstance().getInfo(1323, 1);
		_nobleSkills[3] = SkillTable.getInstance().getInfo(1324, 1);		
		_nobleSkills[4] = SkillTable.getInstance().getInfo(1326, 1);
		_nobleSkills[5] = SkillTable.getInstance().getInfo(1327, 1);
	}
	
	public static NobleSkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public L2Skill[] getNobleSkills()
	{
		return _nobleSkills;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NobleSkillTable _instance = new NobleSkillTable();
	}
}
