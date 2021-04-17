package cz.nxs.interf.delegate;

import cz.nxs.l2j.delegate.ISkillData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;

/**
 * @author hNoke
 *
 */
public class SkillData implements ISkillData
{
	private L2Skill _skill;
	
	public SkillData(L2Skill cha)
	{
		_skill = cha;
	}
	
	public SkillData(int skillId, int level)
	{
		_skill = SkillTable.getInstance().getInfo(skillId, level);
	}
	
	public L2Skill getOwner()
	{
		return _skill;
	}
	
	@Override
	public String getName()
	{
		return _skill.getName();
	}
	
	@Override
	public int getLevel()
	{
		return _skill.getLevel();
	}
	
	@Override
	public boolean exists()
	{
		return _skill != null;
	}
	
	@Override
	public String getSkillType()
	{
		return _skill.getSkillType().toString();
	}
	
	@Override
	public boolean isHealSkill()
	{
		if(getSkillType().equals("BALANCE_LIFE")
				|| getSkillType().equals("CPHEAL_PERCENT")
				|| getSkillType().equals("COMBATPOINTHEAL")
				|| getSkillType().equals("CPHOT")
				|| getSkillType().equals("HEAL")
				|| getSkillType().equals("HEAL_PERCENT")
				|| getSkillType().equals("HEAL_STATIC")
				|| getSkillType().equals("HOT")
				|| getSkillType().equals("MANAHEAL")
				|| getSkillType().equals("MANAHEAL_PERCENT")
				|| getSkillType().equals("MANARECHARGE")
				|| getSkillType().equals("MPHOT")
				|| getSkillType().equals("MANA_BY_LEVEL")
				)
		{
			return true;
		}
		else 
			return false;
	}
	
	@Override
	public boolean isResSkill()
	{
		if(getSkillType().equals("RESURRECT"))
			return true;
		return false;
	}
	
	@Override
	public int getHitTime()
	{
		return _skill.getHitTime();
	}
	
	@Override
	public int getReuseDelay()
	{
		return _skill.getReuseDelay();
	}
	
	@Override
	public int getId()
	{
		return _skill.getId();
	}
}
