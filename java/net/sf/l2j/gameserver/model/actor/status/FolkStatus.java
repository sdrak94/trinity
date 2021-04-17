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
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;

public class FolkStatus extends NpcStatus
{
public FolkStatus(L2Npc activeChar)
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
}

@Override
public final void reduceMp(double value)
{
}

@Override
public L2NpcInstance getActiveChar()
{
	return (L2NpcInstance)super.getActiveChar();
}
}
