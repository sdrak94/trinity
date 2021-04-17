package net.sf.l2j.gameserver.model.events.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cz.nxs.l2j.CallBack;

public class FenceManager
{
	public int _nextId = 0;
	
	public void getFenceNextId()
	{
		Connection con = null;
		int id = 0;
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) from fence_spawns");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				id = rset.getInt(1);
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
		_nextId = id++;
	}
	
	public int getNextFenceId()
	{
		getFenceNextId();
		return _nextId;
	}
	
	public static FenceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final FenceManager INSTANCE = new FenceManager();
	}
}
