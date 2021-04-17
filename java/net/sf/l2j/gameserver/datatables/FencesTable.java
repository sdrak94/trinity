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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * @author Nik based on Christian's work.
 */
public class FencesTable
{
	protected static final Logger _log = Logger.getLogger(FencesTable.class.getName());
	
	private static final class SingletonHolder
	{
		protected static final FencesTable _instance = new FencesTable();
	}
	
	public static FencesTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private final Map<Integer, L2FenceInstance> _fences = new HashMap<>();
	private final Map<L2WorldRegion, List<L2FenceInstance>> _regions = new ConcurrentHashMap<>();
	private static final int MAX_Z_DIFF = 100;
	
	protected FencesTable()
	{
		load();
		_log.log(Level.INFO, StringUtil.concat("Loaded ", String.valueOf(_fences.size()), " stored fences."));
	}
	
	private void load()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM fences_trinity");
			ResultSet res = stmt.executeQuery();
			while (res.next())
			{
				L2FenceInstance fence = new L2FenceInstance(res.getInt("object_id"), res.getString("name"), res.getInt("state"), res.getInt("x"), res.getInt("y"), res.getInt("z"), res.getInt("width"), res.getInt("length"), res.getInt("height"), res.getInt("mapId"));
				fence.setStoredToDB(true); // They are loaded from DB, therefore they are stored there.
				fence.setName(res.getString("name"));
				_fences.put(fence.getObjectId(), fence);
				//fence.spawnMe();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void reload()
	{
		for (L2FenceInstance fence : _fences.values())
		{
			// Do not delete temp fences, they aren't stored in DB therefore they won't be reloaded, but deleted.
			if (!fence.isStoredToDB())
			{
				fence.decay();
			}
		}
		load();
	}
	
	public void addFence(L2FenceInstance fence)
	{
		if(fence != null)
		{
			_fences.put(fence.getObjectId(), fence);
		}
	}
	
	public L2FenceInstance getFence(int objectId)
	{
		return _fences.get(objectId);
	}
	
	public L2FenceInstance removeFence(int objectId)
	{
		return _fences.remove(objectId);
	}
	
	public Map<Integer, L2FenceInstance> getFences()
	{
		return _fences;
	}
	
	public void deleteFence(int objectId, boolean deleteFromDB)
	{
		L2FenceInstance fence = removeFence(objectId);
		if (fence != null)
		{
			fence.decay();
		}
		if (deleteFromDB)
		{
			deleteStoredFence(objectId);
		}
	}
	
	/**
	 * @param fence
	 *            - Stores the fence to the database.
	 */
	public void storeFence(L2FenceInstance fence)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO fences_trinity (object_id,name,state,x,y,z,width,length,height) VALUES(?,?,?,?,?,?,?,?,?)"))
		{
			stmt.setInt(1, fence.getObjectId());
			stmt.setString(2, fence.getName());
			stmt.setInt(3, fence.getState());
			stmt.setInt(4, fence.getX());
			stmt.setInt(5, fence.getY());
			stmt.setInt(6, fence.getZ());
			stmt.setInt(7, fence.getWidth());
			stmt.setInt(8, fence.getLength());
			stmt.setInt(9, fence.getHeight());
			stmt.executeUpdate();
		}
		catch (SQLException sqle)
		{
			_log.log(Level.WARNING, "Error while storing fence:", sqle);
		}
	}
	
	public boolean deleteStoredFence(int objectId)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement("DELETE FROM fences_trinity WHERE object_id=?");)
		{
			stmt.setInt(1, objectId);
			if (stmt.executeUpdate() > 0)
			{
				return true;
			}
		}
		catch (SQLException sqle)
		{
			_log.log(Level.WARNING, "Error while deleting stored fence:", sqle);
		}
		return false;
	}
	
	public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		final Predicate<L2FenceInstance> filter = fence ->
		{
			// Check if fence is geodata enabled.
			if (!(fence.getState() == 2))
			{
				return false;
			}
			// Check if fence is within the instance we search for.
			if (fence.getInstanceId() != instanceId)
			{
				return false;
			}
			final int xMin = fence.getXMin();
			final int xMax = fence.getXMax();
			final int yMin = fence.getYMin();
			final int yMax = fence.getYMax();
			if ((x < xMin) && (tx < xMin))
			{
				return false;
			}
			if ((x > xMax) && (tx > xMax))
			{
				return false;
			}
			if ((y < yMin) && (ty < yMin))
			{
				return false;
			}
			if ((y > yMax) && (ty > yMax))
			{
				return false;
			}
			if ((x > xMin) && (tx > xMin) && (x < xMax) && (tx < xMax))
			{
				if ((y > yMin) && (ty > yMin) && (y < yMax) && (ty < yMax))
				{
					return false;
				}
			}
			if (crossLinePart(xMin, yMin, xMax, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMin, xMax, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMax, xMin, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMin, yMax, xMin, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax))
			{
				if ((z > (fence.getZ() - MAX_Z_DIFF)) && (z < (fence.getZ() + MAX_Z_DIFF)))
				{
					return true;
				}
			}
			return false;
		};
		final L2WorldRegion region = L2World.getInstance().getRegion(x, y); // Should never be null.
		return region == null ? false : _regions.getOrDefault(region, Collections.emptyList()).stream().anyMatch(filter);
	}
	private boolean crossLinePart(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double xMin, double yMin, double xMax, double yMax)
	{
		final double[] result = intersection(x1, y1, x2, y2, x3, y3, x4, y4);
		if (result == null)
		{
			return false;
		}
		
		final double xCross = result[0];
		final double yCross = result[1];
		if ((xCross <= xMax) && (xCross >= xMin))
		{
			return true;
		}
		if ((yCross <= yMax) && (yCross >= yMin))
		{
			return true;
		}
		
		return false;
	}
	private double[] intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
	{
		final double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
		if (d == 0)
		{
			return null;
		}
		
		final double xi = (((x3 - x4) * ((x1 * y2) - (y1 * x2))) - ((x1 - x2) * ((x3 * y4) - (y3 * x4)))) / d;
		final double yi = (((y3 - y4) * ((x1 * y2) - (y1 * x2))) - ((y1 - y2) * ((x3 * y4) - (y3 * x4)))) / d;
		return new double[]
		{
			xi,
			yi
		};
	}

}