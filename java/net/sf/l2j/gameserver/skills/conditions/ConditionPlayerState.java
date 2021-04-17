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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.PlayerState;
import net.sf.l2j.gameserver.skills.Env;


/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConditionPlayerState extends Condition
{

public final PlayerState _check;
public final boolean _required;

public ConditionPlayerState(PlayerState check, boolean required)
{
	_check = check;
	_required = required;
}

@Override
public boolean testImpl(Env env)
{
	L2PcInstance player;
	switch (_check)
	{
	case RESTING:
		if (env.player instanceof L2PcInstance)
			return ((L2PcInstance) env.player).isSitting() == _required;
		return !_required;
	case MOVING:
		return env.player.isMoving() == _required;
	case RUNNING:
		return env.player.isMoving() == _required && env.player.isRunning() == _required;
	case FLYING:
		return env.player.isFlying() == _required;
	case BEHIND:
		return env.target != null && env.target != env.player && env.player.isBehind(env.target) == _required;
	case COMBAT:
		return env.player.isInCombat() == _required;
	case ATTACKING:
		return env.player.isAttackingNow() == _required;
	case CASTING:
		return env.player.isCastingNow() == _required;
	case INVISIBLE:
		return env.player.isInvisible() == _required;
	case BLEEDING:
		return env.player.isBleeding() == _required;
	case FRONT:
		return env.target != null && env.target != env.player && env.player.isInFrontOf(env.target) == _required;
	case CHAOTIC:
		player = env.player.getActingPlayer();
		if (player != null)
			return player.getKarma() > 0 == _required;
			return !_required;
	case FLAGGED:
		player = env.player.getActingPlayer();
		if (player != null)
			return player.getPvpFlag() > 0 == _required;
			return !_required;
	case OLYMPIAD:
		player = env.player.getActingPlayer();
		if (player != null)
			return player.isInOlympiadMode() == _required;
		return !_required;
	}
	return !_required;
}
}

