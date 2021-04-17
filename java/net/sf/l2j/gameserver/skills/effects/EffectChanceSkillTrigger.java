/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.ChanceCondition;
import net.sf.l2j.gameserver.model.IChanceSkillTrigger;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class EffectChanceSkillTrigger extends L2Effect implements IChanceSkillTrigger
{
private final int _triggeredId;
private final int _triggeredLevel;
private final ChanceCondition _chanceCondition;

public EffectChanceSkillTrigger(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
	
	_triggeredId = template.triggeredId;
	_triggeredLevel = template.triggeredLevel;
	_chanceCondition = template.chanceCondition;
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.CHANCE_SKILL_TRIGGER;
}

@Override
public boolean onStart()
{
	switch (getSkill().getId())
	{
	case 16500:
	case 16501:
	case 16502:
	{
		if (getEffected() instanceof L2PcInstance)
			getEffected().getActingPlayer()._hasTehForce = true;
	}
	}
	
	super.onStart();
	getEffected().addChanceEffect(this);
	return true;
}

@Override
public boolean onActionTime()
{
	if (getSkill().getSkillType() == L2SkillType.CONT)
		return true;
	
	return false;
}

@Override
public void onExit()
{
	switch (getSkill().getId())
	{
	case 16500:
	case 16501:
	case 16502:
	{
		if (getEffected() instanceof L2PcInstance)
			getEffected().getActingPlayer()._hasTehForce = false;
	}
	}
	getEffected().removeChanceEffect(this);
	super.onExit();
}

@Override
public int getTriggeredChanceId()
{
	return _triggeredId;
}

@Override
public int getTriggeredChanceLevel()
{
	return _triggeredLevel;
}

@Override
public boolean triggersChanceSkill()
{
	return _triggeredId > 1;
}

@Override
public ChanceCondition getTriggeredChanceCondition()
{
	return _chanceCondition;
}

}