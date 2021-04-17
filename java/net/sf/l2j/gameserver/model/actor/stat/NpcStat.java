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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.skills.Stats;

public class NpcStat extends CharStat
{
// =========================================================
// Data Field

// =========================================================
// Constructor
public NpcStat(L2Npc activeChar)
{
	super(activeChar);
	
	//TODO: Since this class is now init BEFORE the Npc is fully init, we can not do this here. Overwritten getLevel() is a temp solution.
	//setLevel(getActiveChar().getTemplate().level);
}

@Override
public byte getLevel()
{
	return getActiveChar().getTemplate().level;
}

// =========================================================
// Method - Public

// =========================================================
// Method - Private

// =========================================================
// Property - Public
@Override
public L2Npc getActiveChar() { return (L2Npc)super.getActiveChar(); }

@Override
public final int getMaxHp() { return (int)calcStat(Stats.MAX_HP, getActiveChar().getTemplate().baseHpMax , null, null); }

@Override
public int getWalkSpeed(){ return (int) calcStat(Stats.WALK_SPEED, getActiveChar().getTemplate().baseWalkSpd, null, null);}

@Override
public float getMovementSpeedMultiplier()
{
	if (getActiveChar() == null)
		return 1;
	
	if (getActiveChar().isRunning())
		return getRunSpeed() * 1f / getActiveChar().getTemplate().baseRunSpd;
	else
		return getWalkSpeed() * 1f / getActiveChar().getTemplate().baseWalkSpd;
}

public float getFakeMovementSpeedMultiplier(int base)
{
	if (_activeChar == null)
		return 1;
	
	if (base == 0)
	{
		System.out.println("WTF LOL fakemovementspeedmulti base == 0!!!!");
		return 1;
	}
	
	return getRunSpeed() * 1f / base;
}
}
