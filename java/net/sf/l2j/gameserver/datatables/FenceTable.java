package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;

public class FenceTable
{
	public static final FenceTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected static final Logger		_log	= Logger.getLogger(FenceTable.class.getName());
	private Map<Integer, List<Fence>>	_fences;
	
	public FenceTable()
	{
		loadFences();
	}
	
	private class Fence
	{
		private int	type;
		private int	width;
		private int	length;
		private int	x;
		private int	y;
		private int	z;
		
		private Fence(int type, int width, int length, int x, int y, int z)
		{
			this.type = type;
			this.width = width;
			this.length = length;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
	
	private void loadFences()
	{
		_fences = new FastMap<Integer, List<Fence>>();
		Connection con = null;
		Fence fence = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT type, width, length, x, y, z, mapId FROM fences");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				if (_fences.get(rset.getInt("mapId")) == null)
					_fences.put(rset.getInt("mapId"), new FastList<Fence>());
				fence = new Fence(rset.getInt("type"), rset.getInt("width"), rset.getInt("length"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				_fences.get(rset.getInt("mapId")).add(fence);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public List<L2FenceInstance> getFences(int mapId)
	{
		List<L2FenceInstance> fences = new FastList<L2FenceInstance>();
		for (Fence f : _fences.get(mapId))
		{
			fences.add(new L2FenceInstance(IdFactory.getInstance().getNextId(), "Nexus", f.type, f.width, f.length, f.x, f.y, f.z, mapId, mapId));
		}
		return fences;
	}
	
	public void spawnFences(List<L2FenceInstance> list, int instance)
	{
		for (L2FenceInstance fence : list)
		{
			if (fence != null)
			{
				if (instance > 0)
					fence.setInstanceId(instance);
				fence.spawnMe(fence.getX(), fence.getY(), fence.getZ());
			}
		}
	}
	
	public void unspawnFences(List<L2FenceInstance> list)
	{
		for (L2FenceInstance fence : list)
		{
			if (fence != null)
			{
				L2WorldRegion region = fence.getWorldRegion();
				fence.decayMe();
				if (region != null)
					region.removeVisibleObject(fence);
				fence.getKnownList().removeAllKnownObjects();
				L2World.getInstance().removeObject(fence);
			}
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final FenceTable _instance = new FenceTable();
	}

	public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		final L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		final List<L2FenceInstance> fences = region != null ? region.getFences() : null;
		if ((fences == null) || fences.isEmpty())
		{
			return false;
		}
		
		for (L2FenceInstance fence : fences)
		{
			// Check if fence is geodata enabled.
//			if (!fence.getState().isGeodataEnabled())
//			{
//				continue;
//			}
			
			// Check if fence is within the instance we search for.
			if (fence.getInstanceId() != instanceId)
			{
				continue;
			}
			
			final int xMin = fence.getXMin();
			final int xMax = fence.getXMax();
			final int yMin = fence.getYMin();
			final int yMax = fence.getYMax();
			if ((x < xMin) && (tx < xMin))
			{
				continue;
			}
			if ((x > xMax) && (tx > xMax))
			{
				continue;
			}
			if ((y < yMin) && (ty < yMin))
			{
				continue;
			}
			if ((y > yMax) && (ty > yMax))
			{
				continue;
			}
			if ((x > xMin) && (tx > xMin) && (x < xMax) && (tx < xMax) && (y > yMin) && (ty > yMin) && (y < yMax) && (ty < yMax))
			{
				continue;
			}
			if ((crossLinePart(xMin, yMin, xMax, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMin, xMax, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMax, xMin, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMin, yMax, xMin, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax)) && (z > (fence.getZ() - 100)) && (z < (fence.getZ() + 100)))
			{
				return true;
			}
		}
		return false;
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