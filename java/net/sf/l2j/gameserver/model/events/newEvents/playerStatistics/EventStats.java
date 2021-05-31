package net.sf.l2j.gameserver.model.events.newEvents.playerStatistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.EventBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerStatsTemplate;
import net.sf.l2j.gameserver.util.StringUtil;

public class EventStats
{
	Map<L2PcInstance, PlayerStatsTemplate>	data		= new FastMap<L2PcInstance, PlayerStatsTemplate>();
	public void updateGlobalStats(Map<L2PcInstance, PlayerStatsTemplate> data)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			for (Entry<L2PcInstance, PlayerStatsTemplate> e : data.entrySet())
			{
				statement = con.prepareStatement("REPLACE INTO event_stats VALUES (?,?,?,?,?,?,?,?)");
				statement.setInt(1, e.getKey().getObjectId());
				statement.setString(2, e.getValue().getEvent());
				statement.setInt(3, e.getValue().getCountPlayed());
				statement.setInt(4, e.getValue().getWins());
				statement.setInt(5, e.getValue().getLoses());
				statement.setInt(6, e.getValue().getKills());
				statement.setInt(7, e.getValue().getDeaths());
				statement.setInt(8, e.getValue().getScore());
				statement.executeUpdate();
				statement.close();
			}
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
	public void generateTopStats()
	{
		EventBBSManager.getInstance().generateTopStatsGlobal();
	}
	
	public String getTopStats(String Event, String sort)
	{
		final StringBuilder playerStats = StringUtil.startAppend(1000, "");
		PreparedStatement statement;
		ResultSet rs;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT event_stats.*, characters.char_name FROM event_stats INNER JOIN characters ON event_stats.player = characters.charId WHERE event = '"+Event+"'ORDER BY "+sort+" DESC LIMIT 0, 1");
			//statement = con.prepareStatement("SELECT * FROM event_stats WHERE event = '"+Event+"' ORDER BY "+sort+" DESC LIMIT 0, 1");
			boolean lol = false;
			
			String bgcolor;
			rs = statement.executeQuery();
			while (rs.next())
			{
				String eventName = rs.getString("event");
				String countPlayed = rs.getString("char_name");
				String wins = String.valueOf(rs.getInt("wins"));
				String loses = String.valueOf(rs.getInt("loses"));
				String kills = String.valueOf(rs.getInt("kills"));
				String deaths = String.valueOf(rs.getInt("deaths"));
				String score = String.valueOf(rs.getInt("score"));

				if (lol)
				{
					lol = false;
					//color = "817679";
					bgcolor = "bgcolor=00080b";
				}
				else
				{
					lol = true;
					//color = "FFF8C6";
					bgcolor = "bgcolor=011118";
				}
				playerStats.append( 
				"<tr>" + 
				"	<td>" + 
				"		<table width=496 height=15 "+bgcolor+">" + 
				"		   <tr>" + 
				"			  <td width=110>" + 
				"				 <font color=8AC0F6>"+eventName+"</font>" + 
				"			  </td>" + 
				"			  <td width=122 align=center>" + 
				"				 <font color=57799B>"+countPlayed+"</font>" + 
				"			  </td>" + 
				"			  <td width=65 align=center><font color=57799B>"+wins+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+loses+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+kills+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+deaths+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+score+"</font></td>" + 
				"		   </tr>" + 
				"		</table>" + 
				"	</td>" + 
				"</tr>");
			}
			
			rs.close();
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
			}
		}
		return playerStats.toString();
	}
	public String getPersonalStats(L2PcInstance activeChar)
	{
		final StringBuilder playerStats = StringUtil.startAppend(1000, "");
		PreparedStatement statement;
		ResultSet rs;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM event_stats WHERE player = ?");
			boolean lol = false;
			
			String bgcolor;
			
			statement.setInt(1, activeChar.getObjectId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				String eventName = rs.getString("event");
				String countPlayed = String.valueOf(rs.getInt("count_played"));
				String wins = String.valueOf(rs.getInt("wins"));
				String loses = String.valueOf(rs.getInt("loses"));
				String kills = String.valueOf(rs.getInt("kills"));
				String deaths = String.valueOf(rs.getInt("deaths"));
				String score = String.valueOf(rs.getInt("score"));

				if (lol)
				{
					lol = false;
					//color = "817679";
					bgcolor = "bgcolor=00080b";
				}
				else
				{
					lol = true;
					//color = "FFF8C6";
					bgcolor = "bgcolor=011118";
				}
				playerStats.append( 
				"<tr>" + 
				"	<td>" + 
				"		<table width=496 height=15 "+bgcolor+">" + 
				"		   <tr>" + 
				"			  <td width=110>" + 
				"				 <font color=8AC0F6>"+eventName+"</font>" + 
				"			  </td>" + 
				"			  <td width=122 align=center>" + 
				"				 <font color=57799B>"+countPlayed+"</font>" + 
				"			  </td>" + 
				"			  <td width=65 align=center><font color=57799B>"+wins+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+loses+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+kills+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+deaths+"</font></td>" + 
				"			  <td width=65 align=center><font color=57799B>"+score+"</font></td>" + 
				"		   </tr>" + 
				"		</table>" + 
				"	</td>" + 
				"</tr>");
			}
			
			rs.close();
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
			}
		}
		return playerStats.toString();
	}

	public void parseStats(L2PcInstance player, PlayerStatsTemplate pst)
	{
 		PlayerStatsTemplate newPst = null;
		int _countPlayed = pst.getCountPlayed();
		int _wins = pst.getWins();
		int _loses = pst.getLoses();
		int _kills = pst.getKills();
		int _deaths = pst.getDeaths();
		int _score = pst.getScore();
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("select * from event_stats WHERE player=? AND event = ?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, pst.getEvent());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_countPlayed = rs.getInt(3) + 1;
				_wins = rs.getInt(4) + pst.getWins();
				_loses = rs.getInt(5) + pst.getLoses();
				_kills = rs.getInt(6) + pst.getKills();
				_deaths = rs.getInt(7) + pst.getDeaths();
				_score = rs.getInt(8) + pst.getScore();
			}
			statement.close();
			newPst = new PlayerStatsTemplate(pst.getEvent(), _countPlayed, _wins, _loses, _kills, _deaths, _score);

			//System.out.println(player.getName() + " "+ pst.getEvent() + " "+ _countPlayed + " " + _wins + " " + _loses + " "+ _kills + " "+ _deaths + " "+ _score + " ");
		}
		catch (Exception e)
		{
			System.out.println("Exception: EventCommander.parseStats(): " + e);
		}
		finally
		{
			try
			{
				con.close();
				if (newPst != null)
				{
					data.put(player, newPst);
					updateStats();
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	public void updateStats()
	{
		if (data.isEmpty())
			return;
		EventStats.getInstance().updateGlobalStats(data);
		data.clear();
	}

	public static EventStats getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventStats _instance = new EventStats();
	}
}
