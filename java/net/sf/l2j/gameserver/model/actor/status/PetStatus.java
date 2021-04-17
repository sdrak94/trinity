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

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class PetStatus extends SummonStatus
{
private int _currentFed               = 0; //Current Fed of the L2PetInstance

public PetStatus(L2PetInstance activeChar)
{
	super(activeChar);
}

@Override
public final void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

@Override
public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHpConsumption, boolean bypassCP)
{
	if (getActiveChar().isDead())
		return;
	
	super.reduceHp(value, attacker, awake, isDOT, isHpConsumption, false);
	
	if (attacker != null && !attacker.isInvisible())
	{
		if (!isDOT && getActiveChar().getOwner() != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_C1);
			sm.addCharName(attacker);
			sm.addNumber((int)value);
			getActiveChar().getOwner().sendPacket(sm);
		}
		getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
	}
}

public int getCurrentFed()
{
	return _currentFed;
}

public void setCurrentFed(int value)
{
	_currentFed = value;
}

@Override
public L2PetInstance getActiveChar()
{
	return (L2PetInstance)super.getActiveChar();
}
}
