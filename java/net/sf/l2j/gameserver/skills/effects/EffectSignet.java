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

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2EffectPointInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignet;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * @authors Forsaiken, Sami
 */

public class EffectSignet extends L2Effect
{
private L2Skill _skill;
private L2EffectPointInstance _actor;

public EffectSignet(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
 */
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.SIGNET_EFFECT;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	if (getSkill() instanceof L2SkillSignet)
		_skill = SkillTable.getInstance().getInfo(((L2SkillSignet) getSkill()).effectId, getLevel());
	else if (getSkill() instanceof L2SkillSignetCasttime)
		_skill = SkillTable.getInstance().getInfo(((L2SkillSignetCasttime) getSkill()).effectId, getLevel());
	_actor = (L2EffectPointInstance) getEffected();
	super.onStart();
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	if (_skill == null)
		return true;
	int mpConsume = _skill.getMpConsume();
	
	if (mpConsume > getEffector().getCurrentMp())
	{
		getEffector().sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
		return false;
	}
	else
		getEffector().reduceCurrentMp(mpConsume);
	
	for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
	{
		if (cha == null)
			continue;
		
		if (_skill.isPositive())
		{
			if (cha.isAutoAttackable(getEffector()))
				continue;
		}
		else
		{
			if (!cha.isAutoAttackable(getEffector()))
				continue;
		}
		
		_skill.getEffects(_actor, cha);
		// there doesn't seem to be a visible effect with MagicSkillLaunched packet...
		_actor.broadcastPacket(new MagicSkillUse(_actor, cha, _skill.getDisplayId(), _skill.getDisplayLvl(), 0, 0));
	}
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	super.onExit();
	if (_actor != null)
	{
		_actor.deleteMe();
	}
}
}
