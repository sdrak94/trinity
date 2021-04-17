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
package net.sf.l2j.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;

public class DoorTable
{
	private static final Logger _log = Logger.getLogger(DoorTable.class.getName());
	
	private Map<Integer, L2DoorInstance> _staticItems = new FastMap<Integer, L2DoorInstance>();
	private boolean _initialized;
	
	public static DoorTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private DoorTable()
	{
		_initialized = true;
		parseData();
		onStart();
	}
	
	public void reloadAll()
	{
		respawn();
	}
	
	public void respawn()
	{
		_staticItems.clear();
		parseData();
	}
	
	public void parseData()
	{
		LineNumberReader lnr = null;
		try
		{
			File doorData = new File(Config.DATAPACK_ROOT, "data/door.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));

			String line = null;
			_log.info("Searching clan halls doors:");

			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;

				L2DoorInstance door = parseList(line, false);
				_staticItems.put(door.getDoorId(), door);
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				ClanHall clanhall = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 500);
				if (clanhall != null)
				{
					clanhall.getDoors().add(door);
					door.setClanHall(clanhall);
					if (Config.DEBUG)
						_log.info("door " + door.getDoorName() + " attached to ch " + clanhall.getName());
				}
			}

			_log.config("DoorTable: Loaded " + _staticItems.size() + " Door Templates.");
		}
		catch (FileNotFoundException e)
		{
			_initialized = false;
			_log.warning("door.csv is missing in data folder");
		}
		catch (IOException e)
		{
			_initialized = false;
			_log.warning("error while creating door table " + e);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch (Exception e1)
			{ /* ignore problems */
			}
		}
	}
	
	/**
	 * Parses door list.
	 *
	 * @param line string
	 * @param commanderDoor whether the door is commander door (fortress)
	 *
	 * @return created door instance
	*/
	public static L2DoorInstance parseList(final String line, final boolean commanderDoor)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		L2DoorInstance door = null;
		try
		{
			String name = st.nextToken();
			int id = Integer.parseInt(st.nextToken());
			int x = Integer.parseInt(st.nextToken());
			int y = Integer.parseInt(st.nextToken());
			int z = Integer.parseInt(st.nextToken());
			int rangeXMin = Integer.parseInt(st.nextToken());
			int rangeYMin = Integer.parseInt(st.nextToken());
			int rangeZMin = Integer.parseInt(st.nextToken());
			int rangeXMax = Integer.parseInt(st.nextToken());
			int rangeYMax = Integer.parseInt(st.nextToken());
			int rangeZMax = Integer.parseInt(st.nextToken());
			int hp = Integer.parseInt(st.nextToken());
			int pdef = Integer.parseInt(st.nextToken());
			int mdef = Integer.parseInt(st.nextToken());
			boolean unlockable = false;
			if (st.hasMoreTokens())
				unlockable = Boolean.parseBoolean(st.nextToken());
			boolean startOpen = false;
			if (st.hasMoreTokens())
				startOpen = Boolean.parseBoolean(st.nextToken());
			if (rangeXMin > rangeXMax)
				_log.severe("Error in door data, XMin > XMax, ID:" + id);
			if (rangeYMin > rangeYMax)
				_log.severe("Error in door data, YMin > YMax, ID:" + id);
			if (rangeZMin > rangeZMax)
				_log.severe("Error in door data, ZMin > ZMax, ID:" + id);
			int collisionRadius; // (max) radius for movement checks
			if ((rangeXMax - rangeXMin) > (rangeYMax - rangeYMin))
				collisionRadius = rangeYMax - rangeYMin;
			else
				collisionRadius = rangeXMax - rangeXMin;

			StatsSet npcDat = new StatsSet();
			npcDat.set("npcId", id);
			npcDat.set("level", 0);
			npcDat.set("jClass", "door");

			npcDat.set("baseSTR", 0);
			npcDat.set("baseCON", 0);
			npcDat.set("baseDEX", 0);
			npcDat.set("baseINT", 0);
			npcDat.set("baseWIT", 0);
			npcDat.set("baseMEN", 0);

			npcDat.set("baseShldDef", 0);
			npcDat.set("baseShldRate", 0);
			npcDat.set("baseAccCombat", 38);
			npcDat.set("baseEvasRate", 38);
			npcDat.set("baseCritRate", 38);

			//npcDat.set("name", "");
			npcDat.set("collision_radius", collisionRadius);
			npcDat.set("collision_height", rangeZMax - rangeZMin);
			npcDat.set("fcollision_radius", collisionRadius);
			npcDat.set("fcollision_height", rangeZMax - rangeZMin & 0xfff0);
			npcDat.set("sex", "male");
			npcDat.set("type", "");
			npcDat.set("baseAtkRange", 0);
			npcDat.set("baseMpMax", 0);
			npcDat.set("baseCpMax", 0);
			npcDat.set("rewardExp", 0);
			npcDat.set("rewardSp", 0);
			npcDat.set("basePAtk", 0);
			npcDat.set("baseMAtk", 0);
			npcDat.set("basePAtkSpd", 0);
			npcDat.set("aggroRange", 0);
			npcDat.set("baseMAtkSpd", 0);
			npcDat.set("rhand", 0);
			npcDat.set("lhand", 0);
			npcDat.set("armor", 0);
			npcDat.set("baseWalkSpd", 0);
			npcDat.set("baseRunSpd", 0);
			npcDat.set("name", name);
			npcDat.set("baseHpMax", hp);
			npcDat.set("baseHpReg", 3.e-3f);
			npcDat.set("baseMpReg", 3.e-3f);
			npcDat.set("basePDef", pdef);
			npcDat.set("baseMDef", mdef);

			L2CharTemplate template = new L2CharTemplate(npcDat);
			door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, id, name, unlockable);
			door.setRange(rangeXMin, rangeYMin, rangeZMin, rangeXMax, rangeYMax, rangeZMax);
			door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp());
			door.setXYZInvisible(x, y, z);
			door.setMapRegion(MapRegionTable.getInstance().getMapRegion(x, y));

			if (commanderDoor)
				door.setIsCommanderDoor(startOpen);
			else
				door.setOpen(startOpen);
		}
		catch (Exception e)
		{
			_log.severe("Error in door data at line: " + line);
		}
		return door;
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public L2DoorInstance getDoor(Integer id)
	{
		return _staticItems.get(id);
	}
	
	public void putDoor(L2DoorInstance door)
	{
		_staticItems.put(door.getDoorId(), door);
	}
	
	public L2DoorInstance[] getDoors()
	{
		L2DoorInstance[] _allTemplates = _staticItems.values().toArray(new L2DoorInstance[_staticItems.size()]);
		return _allTemplates;
	}
	
	/**
	 * Performs a check and sets up a scheduled task for
	 * those doors that require auto opening/closing.
	 */
	public void checkAutoOpen()
	{
		for (L2DoorInstance doorInst : getDoors())
			// Garden of Eva (every 7 minutes)
			if (doorInst.getDoorName().startsWith("goe"))
				doorInst.setAutoActionDelay(420000);

			// Tower of Insolence (every 5 minutes)
			else if (doorInst.getDoorName().startsWith("aden_tower"))
				doorInst.setAutoActionDelay(300000);

		/* TODO: check which are automatic  
		// devils (every 5 minutes)
		else if (doorInst.getDoorName().startsWith("pirate_isle"))
		   doorInst.setAutoActionDelay(300000);

		// Cruma Tower (every 20 minutes) 
		else if (doorInst.getDoorName().startsWith("cruma")) 
			doorInst.setAutoActionDelay(1200000);

		// Coral Garden Gate (every 15 minutes) 
		else if (doorInst.getDoorName().startsWith("Coral_garden")) 
			doorInst.setAutoActionDelay(900000);

		// Normil's cave (every 5 minutes) 
		else if (doorInst.getDoorName().startsWith("Normils_cave")) 
			doorInst.setAutoActionDelay(300000);

		// Normil's Garden (every 15 minutes) 
		else if (doorInst.getDoorName().startsWith("Normils_garden")) 
			doorInst.setAutoActionDelay(900000);
		*/
	}
	
	public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end, int instanceId)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instanceId);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		int region;
		try
		{
			region = MapRegionTable.getInstance().getMapRegion(x, y);
		}
		catch (Exception e)
		{
			return false;
		}

		Collection<L2DoorInstance> allDoors;
		if (instanceId > 0 && InstanceManager.getInstance().getInstance(instanceId) != null)
			allDoors = InstanceManager.getInstance().getInstance(instanceId).getDoors();
		else
			allDoors = _staticItems.values();

		// there are quite many doors, maybe they should be splitted
		for (L2DoorInstance doorInst : allDoors)
		{
			if (doorInst.getMapRegion() != region)
				continue;
			if (doorInst.getXMax() == 0)
				continue;

			// line segment goes through box
			// first basic checks to stop most calculations short
			// phase 1, x
			if ((x <= doorInst.getXMax() && tx >= doorInst.getXMin()) || (tx <= doorInst.getXMax() && x >= doorInst.getXMin()))
			{
				//phase 2, y
				if ((y <= doorInst.getYMax() && ty >= doorInst.getYMin()) || (ty <= doorInst.getYMax() && y >= doorInst.getYMin()))
				{
					// phase 3, basically only z remains but now we calculate it with another formula (by rage)
					// in some cases the direct line check (only) in the beginning isn't sufficient, 
					// when char z changes a lot along the path
					if (doorInst.getCurrentHp() > 0 && !doorInst.getOpen())
					{
						int px1 = doorInst.getXMin();
						int py1 = doorInst.getYMin();
						int pz1 = doorInst.getZMin();
						int px2 = doorInst.getXMax();
						int py2 = doorInst.getYMax();
						int pz2 = doorInst.getZMax();

						int l = tx - x;
						int m = ty - y;
						int n = tz - z;

						int dk;

						if ((dk = (doorInst.getA() * l + doorInst.getB() * m + doorInst.getC() * n)) == 0)
							continue; // Parallel

						float p = (float) (doorInst.getA() * x + doorInst.getB() * y + doorInst.getC() * z + doorInst.getD()) / (float) dk;

						int fx = (int) (x - l * p);
						int fy = (int) (y - m * p);
						int fz = (int) (z - n * p);

						if ((Math.min(x, tx) <= fx && fx <= Math.max(x, tx)) && (Math.min(y, ty) <= fy && fy <= Math.max(y, ty))
								&& (Math.min(z, tz) <= fz && fz <= Math.max(z, tz)))
						{
							if (((fx >= px1 && fx <= px2) || (fx >= px2 && fx <= px1))
									&& ((fy >= py1 && fy <= py2) || (fy >= py2 && fy <= py1))
									&& ((fz >= pz1 && fz <= pz2) || (fz >= pz2 && fz <= pz1)))
								return true; // Door between
						}
					}
				}
			}
		}
		return false;
	}
		
	private void onStart()
	{
		try
		{
			getDoor(24190001).openMe();
			getDoor(24190002).openMe();
			getDoor(24190003).openMe();
			getDoor(24190004).openMe();
			getDoor(23180001).openMe();
			getDoor(23180002).openMe();
			getDoor(23180003).openMe();
			getDoor(23180004).openMe();
			getDoor(23180005).openMe();
			getDoor(23180006).openMe();
			
			checkAutoOpen();
		}
		catch (NullPointerException e)
		{
			_log.log(Level.WARNING, "There are errors in your Door.csv file. Update door.csv", e);
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final DoorTable _instance = new DoorTable();
	}
}
