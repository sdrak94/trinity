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
}