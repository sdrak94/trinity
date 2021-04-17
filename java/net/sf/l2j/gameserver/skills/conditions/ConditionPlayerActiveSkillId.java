package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.Env;

/**
 * 
 * @author  DrHouse
 */
public class ConditionPlayerActiveSkillId extends Condition
{
	public final int _skillId;
	public final int _skillLevel;
    
    public ConditionPlayerActiveSkillId(int skillId)
    {
        _skillId = skillId;
        _skillLevel = -1;
    }
    
    public ConditionPlayerActiveSkillId(int skillId, int skillLevel)
    {
        _skillId = skillId;
        _skillLevel = skillLevel;
    }
    
    @Override
    public boolean testImpl(Env env)
    {
        for (L2Skill sk : env.player.getAllSkills())
        {
            if (sk != null)
            {
                if (sk.getId() == _skillId)
                {
                	if (_skillLevel == -1 || _skillLevel <= sk.getLevel())
                		return true;
                }
            }
        }
        return false;
    }
}