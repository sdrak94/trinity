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
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectCombatPointHealOverTime extends L2Effect
{
public EffectCombatPointHealOverTime(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	return L2EffectType.COMBAT_POINT_HEAL_OVER_TIME;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	if (getEffected().isDead())
		return false;
	
	double cp = getEffected().getCurrentCp();
	double maxcp = getEffected().getMaxCp();
	
	int healed = (int) calc();
	
	if (getEffected() instanceof L2PcInstance && getEffected().getActingPlayer().isInOlympiadMode())
		healed /= 3;
	
	cp += healed;
	if (cp > maxcp)
	{
		cp = maxcp;
	}
	getEffected().setCurrentCp(cp);
	StatusUpdate sump = new StatusUpdate(getEffected().getObjectId());
	sump.addAttribute(StatusUpdate.CUR_CP, (int) cp);
	getEffected().sendPacket(sump);
	return true;
}
}
