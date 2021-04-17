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

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectMpConsumePerLevel extends L2Effect
{
public EffectMpConsumePerLevel(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	return L2EffectType.MP_CONSUME_PER_LEVEL;
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
	return true;
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
	super.onExit();
}
@Override
public boolean onActionTime()
{
	if (getEffected().isDead())
	{
		if (getSkill().isPositive())
			return true; //special edit here to prevent toggles from disabling upon death
		else return false;
	}
	
	double base = calc();
	double consume = (getEffected().getLevel(true) - 1) / 7.5 * base * getPeriod();
	
	if (consume > getEffected().getCurrentMp())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
		getEffected().sendPacket(sm);
		return false;
	}
	
	getEffected().reduceCurrentMp(consume);
	return true;
}
}