/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jserver.geodriver.Cell;
import com.l2jserver.geodriver.GeoDriver;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.FenceTable;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.pathfinding.Node;
import net.sf.l2j.util.GeoUtils;
import net.sf.l2j.util.LinePointIterator;
import net.sf.l2j.util.Point3D;

/**
 * @author -Nemesiss-
 */
public class GeoData
{
	private static final Logger					LOG							= Logger.getLogger(GeoData.class.getName());
	private static final String					FILE_NAME_FORMAT			= "%d_%d.l2j";
	private static final int					ELEVATED_SEE_OVER_DISTANCE	= 2;
	private static final int					MAX_SEE_OVER_HEIGHT			= 48;
	private static final int					SPAWN_Z_DELTA_LIMIT			= 100;
	private static final Map<String, Boolean>	GEODATA_REGIONS				= Config.GEODATA_REGIONS;
	private final GeoDriver						_driver						= new GeoDriver();
	
	protected GeoData()
	{
		// loadGeodataRegions();
		if (Config.GEODATA == 0)
			return;
		int loadedRegions = 0;
		try
		{
			for (int regionX = L2World.TILE_X_MIN; regionX <= L2World.TILE_X_MAX; regionX++)
			{
				for (int regionY = L2World.TILE_Y_MIN; regionY <= L2World.TILE_Y_MAX; regionY++)
				{
					final Path geoFilePath = Config.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
					final Boolean loadFile = GEODATA_REGIONS.get(regionX + "_" + regionY);
					if (loadFile != null)
					{
						if (loadFile)
						{
							LOG.info("Loading " + geoFilePath.getFileName() + "...");
							_driver.loadRegion(geoFilePath, regionX, regionY);
							loadedRegions++;
						}
					}
					else if (Config.TRY_LOAD_UNSPECIFIED_REGIONS && Files.exists(geoFilePath))
					{
						try
						{
							LOG.info("Loading " + geoFilePath.getFileName() + "...");
							_driver.loadRegion(geoFilePath, regionX, regionY);
							loadedRegions++;
						}
						catch (Exception ex)
						{
							LOG.info("Failed to load " + geoFilePath.getFileName() + "!");
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			LOG.info("Failed to load geodata!");
			System.exit(1);
		}
		LOG.info("Loaded " + loadedRegions + " regions.");
	}
	
	private GeoData(final boolean disabled)
	{
		if (disabled)
		{
			LOG.info("Geodata Engine: Disabled.");
		}
	}
	
	public static GeoData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// Public Methods
	/**
	 * @param x
	 * @param y
	 * @return Geo Block Type
	 */
	public short getType(int x, int y)
	{
		return 0;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Nearles Z
	 */
	public short getHeight(int x, int y, int z)
	{
		return (short) z;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param zmin
	 * @param zmax
	 * @param spawnid
	 * @return
	 */
	public short getSpawnHeight(int x, int y, int zmin, int zmax, int spawnid)
	{
		return (short) zmin;
	}
	
	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public String geoPosition(int x, int y)
	{
		return "";
	}
	
	/**
	 * @param cha
	 * @param target
	 * @return True if cha can see target (LOS)
	 */
	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return (Math.abs(target.getZ() - cha.getZ()) < 1000);
	}
	
	public boolean canSeeTarget(L2Object cha, Point3D worldPosition)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return Math.abs(worldPosition.getZ() - cha.getZ()) < 1000;
	}
	
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return (Math.abs(z - tz) < 1000);
	}
	
	public boolean canSeeTarget(int x, int y, int z, int instanceWorld, int tx, int ty, int tz)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceWorld))
		{
			return false;
		}
		return canSeeTarget(x, y, z, tx, ty, tz);
	}
	
	/**
	 * @param cha
	 * @param target
	 * @return True if cha can see target (LOS) and send usful info to PC
	 */
	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		return true;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Geo NSWE (0-15)
	 */
	public short getNSWE(int x, int y, int z)
	{
		return 15;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param tx
	 * @param ty
	 * @param tz
	 * @param instanceId
	 * @return Last Location (x,y,z) where player can walk - just before wall
	 */
	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		return new Location(tx, ty, tz);
	}
	
	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		return true;
	}
	
	/**
	 * @param gm
	 * @param comment
	 */
	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
		// Do Nothing
	}
	
	public static void unloadGeodata(byte rx, byte ry)
	{}
	
	public static boolean loadGeodataFile(byte rx, byte ry)
	{
		return false;
	}
	
	public boolean hasGeo(int x, int y)
	{
		return false;
	}
	
	public Node[] getNeighbors(Node n)
	{
		return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GeoData _instance = Config.GEODATA > 0 ? GeoEngine.getInstance() : new GeoData(true);
	}
	
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return _driver.checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _driver.getNearestZ(geoX, geoY, worldZ);
	}
	
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return _driver.getNextLowerZ(geoX, geoY, worldZ);
	}
	
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return _driver.getNextHigherZ(geoX, geoY, worldZ);
	}
	
	public int getGeoX(int worldX)
	{
		return _driver.getGeoX(worldX);
	}
	
	public int getGeoY(int worldY)
	{
		return _driver.getGeoY(worldY);
	}
	
	public int getWorldX(int geoX)
	{
		return _driver.getWorldX(geoX);
	}
	
	public int getWorldY(int geoY)
	{
		return _driver.getWorldY(geoY);
	}
	
	public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe)
	{
		boolean can = true;
		if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST)
		{
			// can = canEnterNeighbors(prevX, prevY - 1, prevGeoZ, Direction.EAST) && canEnterNeighbors(prevX + 1, prevY, prevGeoZ, Direction.NORTH);
			can = checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
		}
		if (can && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST))
		{
			// can = canEnterNeighbors(prevX, prevY - 1, prevGeoZ, Direction.WEST) && canEnterNeighbors(prevX - 1, prevY, prevGeoZ, Direction.NORTH);
			can = checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_NORTH);
		}
		if (can && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST))
		{
			// can = canEnterNeighbors(prevX, prevY + 1, prevGeoZ, Direction.EAST) && canEnterNeighbors(prevX + 1, prevY, prevGeoZ, Direction.SOUTH);
			can = checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_EAST) && checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}
		if (can && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST))
		{
			// can = canEnterNeighbors(prevX, prevY + 1, prevGeoZ, Direction.WEST) && canEnterNeighbors(prevX - 1, prevY, prevGeoZ, Direction.SOUTH);
			can = checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_WEST) && checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
		}
		return can && checkNearestNswe(geoX, geoY, worldZ, nswe);
	}
	
	public boolean hasGeoPos(int geoX, int geoY)
	{
		return _driver.hasGeoPos(geoX, geoY);
	}
	
	public boolean canMove(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int instanceId)
	{
		int geoX = getGeoX(fromX);
		int geoY = getGeoY(fromY);
		fromZ = getNearestZ(geoX, geoY, fromZ);
		int tGeoX = getGeoX(toX);
		int tGeoY = getGeoY(toY);
		toZ = getNearestZ(tGeoX, tGeoY, toZ);
		if (DoorTable.getInstance().checkIfDoorsBetween(fromX, fromY, fromZ, toX, toY, toZ, instanceId))
		{
			return false;
		}
		LinePointIterator pointIter = new LinePointIterator(geoX, geoY, tGeoX, tGeoY);
		// first point is guaranteed to be available
		pointIter.next();
		int prevX = pointIter.x();
		int prevY = pointIter.y();
		int prevZ = fromZ;
		while (pointIter.next())
		{
			int curX = pointIter.x();
			int curY = pointIter.y();
			int curZ = getNearestZ(curX, curY, prevZ);
			if (hasGeoPos(prevX, prevY))
			{
				int nswe = GeoUtils.computeNswe(prevX, prevY, curX, curY);
				if (!checkNearestNsweAntiCornerCut(prevX, prevY, prevZ, nswe))
				{
					return false;
				}
			}
			prevX = curX;
			prevY = curY;
			prevZ = curZ;
		}
		// different floors
		return !hasGeoPos(prevX, prevY) || (prevZ == toZ);
	}
	
	/**
	 * Checks if its possible to move from one location to another.
	 * 
	 * @param from
	 *            the {@code ILocational} to start checking from
	 * @param toX
	 *            the X coordinate to end checking at
	 * @param toY
	 *            the Y coordinate to end checking at
	 * @param toZ
	 *            the Z coordinate to end checking at
	 * @return {@code true} if the character at start coordinates can move to end coordinates, {@code false} otherwise
	 */

	
	public boolean canSeeTarget(L2Object cha, Location worldPosition)
	{
		return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), cha.getInstanceId(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
	}

	public Location getValidLocation(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// Door checks.
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz, instanceId))
		{
			return new Location(ox, oy, oz);
		}
		
		// Fence checks.
		if (FenceTable.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz, instanceId))
		{
			return new Location(ox, oy, oz);
		}

		return new Location(tx, ty, tz);
	}
}
