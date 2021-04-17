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
package net.sf.l2j.gameserver.model.zone;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.util.Rnd;

/**
 * Abstract zone with spawn locations
 * @author DS
 */
public abstract class L2SpawnZone extends L2ZoneType
{
	private List<Location> _spawnLocs = null;
	private List<Location> _chaoticSpawnLocs = null;
	
	public L2SpawnZone(final int id)
	{
		super(id);
	}
	
	public final void addSpawn(final int x, final int y, final int z)
	{
		if (_spawnLocs == null)
			_spawnLocs = new ArrayList<>();
		_spawnLocs.add(new Location(x, y, z));
	}
	
	public final void addChaoticSpawn(final int x, final int y, final int z)
	{
		if (_chaoticSpawnLocs == null)
			_chaoticSpawnLocs = new ArrayList<>();
		_chaoticSpawnLocs.add(new Location(x, y, z));
	}
	
	public final List<Location> getSpawns()
	{
		return _spawnLocs;
	}
	
	public final Location getSpawnLoc()
	{
		return _spawnLocs.get(Rnd.get(_spawnLocs.size()));
	}
	
	public final Location getChaoticSpawnLoc()
	{
		if (_chaoticSpawnLocs != null)
			return _chaoticSpawnLocs.get(Rnd.get(_chaoticSpawnLocs.size()));
		return getSpawnLoc();
	}
}