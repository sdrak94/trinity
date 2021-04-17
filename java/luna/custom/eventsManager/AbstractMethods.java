package luna.custom.eventsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AbstractMethods
{
	public void onKill(L2PcInstance player, String EventName)
	{
	}

	public void onDie(L2PcInstance player, String EventName)
	{
		
	}
	
	public void onScoreFlag(L2PcInstance player, String EventName)
	{
		
	}
	
	public void onWin(L2PcInstance player, String EventName, int kda[])
	{
		
	}
	
	public void onLose(L2PcInstance player, String EventName, int kda[])
	{
		int kills = kda[0];
		int deaths = kda[1];
		
	}
	

	public static void updateEventStats(String cat, int value, int objid, int eventType)
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE event_statistics SET " + cat + " = ? WHERE objid = ? AND event_type = ?");
			statement.setInt(1, value);
			statement.setInt(2, objid);
			statement.executeUpdate();
			statement.close();
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
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	public static void checkForEventStats(int objid, int eventType)
	{
		Connection con = null;
		PreparedStatement statement;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT * FROM event_statistics WHERE player_objid = ? AND event_type = ?");
				statement.setInt(1, objid);
				statement.setInt(2, eventType);
				
				ResultSet rset = statement.executeQuery();
				
				if(!rset.next())
				{
					PreparedStatement statement2 = con.prepareStatement("INSERT INTO event_statistics VALUES (?,?,0,0,0,0,0,0,0,0);");
					statement2.setInt(1, objid);
					statement2.setInt(2, eventType);
					statement2.executeUpdate();
					statement2.close();

					rset.close();
					statement2.close();
					statement.close();
				}
				else
					return;
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
			catch (Exception e)
			{}
		}
	}
}
