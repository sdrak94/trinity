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
package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel;

public class SummonStatus extends PlayableStatus
{
public SummonStatus(L2Summon activeChar)
{
	super(activeChar);
}

@Override
public void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

@Override
public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP)
{
	if (getActiveChar().isDead())
		return;
	
	if (attacker != null)
	{
		final L2PcInstance attackerPlayer = attacker.getActingPlayer();
		if (attackerPlayer != null
				&& (getActiveChar().getOwner() == null
						|| getActiveChar().getOwner().getDuelId() != attackerPlayer.getDuelId()))
			attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
	}
	super.reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
}

@Override
public L2Summon getActiveChar()
{
	return (L2Summon)super.getActiveChar();
}
}
