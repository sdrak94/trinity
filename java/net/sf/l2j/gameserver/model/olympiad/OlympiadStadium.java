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
package net.sf.l2j.gameserver.model.olympiad;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

/**
 * @author GodKratos
 */

class OlympiadStadium
{
private boolean _freeToUse = true;
private static DoorTable _doorTable;
private int[] _coords = new int[3];
private int[] _doors = new int[2];
private FastList<L2PcInstance> _spectators;

public boolean isFreeToUse()
{
	return _freeToUse;
}

public void setStadiaBusy()
{
	_freeToUse = false;
}

public void setStadiaFree()
{
	_freeToUse = true;
}

public int[] getCoordinates()
{
	return _coords;
}

public int[] getDoorID()
{
	return _doors;
}

public OlympiadStadium(int x, int y, int z, int d1, int d2)
{
	_coords[0] = x;
	_coords[1] = y;
	_coords[2] = z;
	_doors[0] = d1;
	_doors[1] = d2;
	_spectators = new FastList<L2PcInstance>();
}

public void openDoors()
{
	_doorTable = DoorTable.getInstance();
	try
	{
		_doorTable.getDoor(getDoorID()[0]).openMe();
		_doorTable.getDoor(getDoorID()[1]).openMe();
	}
	catch (Exception e)
	{
	}
}

public void closeDoors()
{
	_doorTable = DoorTable.getInstance();
	try
	{
		_doorTable.getDoor(getDoorID()[0]).closeMe();
		_doorTable.getDoor(getDoorID()[1]).closeMe();
	}
	catch (Exception e)
	{
	}
}

protected void addSpectator(int id, L2PcInstance spec, boolean storeCoords)
{
	if (spec.isInCombat() || spec.getPvpFlag() != 0 || spec.getKarma() > 0)
	{
		spec.sendMessage("Observation is not possible until you get out of combat");
		spec.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	spec.enterOlympiadObserverMode(getCoordinates()[0] + 1200, getCoordinates()[1], getCoordinates()[2], id, storeCoords);
	_spectators.add(spec);
}

protected FastList<L2PcInstance> getSpectators()
{
	return _spectators;
}

protected void removeSpectator(L2PcInstance spec)
{
	if (_spectators != null && _spectators.contains(spec))
		_spectators.remove(spec);
}
}
