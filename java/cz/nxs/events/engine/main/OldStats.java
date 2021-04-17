/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General private License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General private License for more
 * details.
 *
 * You should have received a copy of the GNU General private License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.nxs.events.engine.main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.ShowBoardData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author Rizel
 * to be replaced
 */

public class OldStats
{

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		private static final OldStats _instance = new OldStats();
	}

	private class StatModell
	{
		private int num;
		private int wins;
		private int losses;
		private int kills;
		private int deaths;
		private int scores;

		private StatModell(int num, int wins, int losses, int kills, int deaths, int scores)
		{
			this.num = num;
			this.wins = wins;
			this.losses = losses;
			this.kills = kills;
			this.deaths = deaths;
			this.scores = scores;
		}

	}

	public static OldStats getInstance()
	{
		return SingletonHolder._instance;
	}

	private FastMap<Integer, FastMap<Integer, StatModell>> stats;

	public FastMap<Integer, int[]> tempTable;

	private FastMap<Integer, ShowBoardData> htmls;

	private FastMap<Integer, int[]> statSums;
	
	private boolean enabled = false;

	protected OldStats()
	{
		stats = new FastMap<Integer, FastMap<Integer, StatModell>>();
		tempTable = new FastMap<Integer, int[]>();
		htmls = new FastMap<Integer, ShowBoardData>();
		statSums = new FastMap<Integer, int[]>();
		loadSQL();
	}

	protected void applyChanges()
	{
		if(!enabled)
			return;

		int eventId = EventManager.getInstance().getCurrentMainEvent().getEventType().getMainEventId();
		for (PlayerEventInfo player : EventManager.getInstance().getCurrentMainEvent().getPlayers(0))
		{
			int playerId = player.getPlayersId();

			if (!stats.containsKey(playerId))
				stats.put(playerId, new FastMap<Integer, StatModell>());

			if (!stats.get(playerId).containsKey(eventId))
				stats.get(playerId).put(eventId, new StatModell(0, 0, 0, 0, 0, 0));

			if (tempTable.get(playerId)[0] == 1)
				stats.get(playerId).get(eventId).wins = stats.get(playerId).get(eventId).wins + 1;
			else
				stats.get(playerId).get(eventId).losses = stats.get(playerId).get(eventId).losses + 1;

			stats.get(playerId).get(eventId).num = stats.get(playerId).get(eventId).num + 1;
			stats.get(playerId).get(eventId).kills = stats.get(playerId).get(eventId).kills + tempTable.get(playerId)[1];
			stats.get(playerId).get(eventId).deaths = stats.get(playerId).get(eventId).deaths + tempTable.get(playerId)[2];
			stats.get(playerId).get(eventId).scores = stats.get(playerId).get(eventId).scores + tempTable.get(playerId)[3];
		}
		
		NexusLoader.debug("applyChanges finished");
	}
	
	public void applyMiniEventStatsChanges(int eventId, FastMap<Integer, int[]> statsTable)
	{
		if(!enabled)
			return;

		for (Entry<Integer, int[]> e : statsTable.entrySet())
		{
			int playerId = e.getKey();

			if (!stats.containsKey(playerId))
				stats.put(playerId, new FastMap<Integer, StatModell>());

			if (!stats.get(playerId).containsKey(eventId))
				stats.get(playerId).put(eventId, new StatModell(0, 0, 0, 0, 0, 0));

			if(statsTable.get(playerId)[0] != -1)
			{
				if (statsTable.get(playerId)[0] == 1)
					stats.get(playerId).get(eventId).wins = stats.get(playerId).get(eventId).wins + 1;
				else
					stats.get(playerId).get(eventId).losses = stats.get(playerId).get(eventId).losses + 1;
			}

			stats.get(playerId).get(eventId).num = stats.get(playerId).get(eventId).num + 1;
			stats.get(playerId).get(eventId).kills = stats.get(playerId).get(eventId).kills + statsTable.get(playerId)[1];
			stats.get(playerId).get(eventId).deaths = stats.get(playerId).get(eventId).deaths + statsTable.get(playerId)[2];
			stats.get(playerId).get(eventId).scores = stats.get(playerId).get(eventId).scores + statsTable.get(playerId)[3];
		}
		
		NexusLoader.debug("applyChanges finished for mini events");
	}

	private void createHtmls()
	{
		htmls.clear();
		TextBuilder sb = new TextBuilder();
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			statement = con.prepareStatement("SELECT characters.char_name, nexus_stats_full.* FROM nexus_stats_full INNER JOIN characters ON characters.charId = nexus_stats_full.player ORDER BY nexus_stats_full.wins DESC");
			ResultSet rset = statement.executeQuery();
			rset.last();
			int size = rset.getRow();
			rset.beforeFirst();
			int count = 0;
			while (rset.next())
			{
				count++;
				if (count % 10 == 1)
					sb.append("<html><body><br><br><center><table width=150><tr><td width=50><center>" + ((count - 1) / 10 != 0 ? "<a action=\"bypass -h eventstats " + ((count - 1) / 10) + "\">Prev</a>" : "Prev") + "</td><td width=50><center>" + (((count - 1) / 10) + 1) + "</td><td width=50><center>" + ((count - 1) / 10 != size / 10 ? "<a action=\"bypass -h eventstats " + (((count - 1) / 10) + 2) + "\">Next</a>" : "Next") + "</td></tr></table><br><br><center><table width=700 bgcolor=5A5A5A><tr><td width=30><center>Rank</td><td width=100><center>Name</td><td width=65><center>Events</td><td width=65><center>Win%</td><td width=65><center>K:D</td><td width=65><center>Wins</td><td width=65><center>Losses</td><td width=65><center>Kills</td><td width=65><center>Deaths</td><td width=100><center>Favourite Event</td></tr></table><br>" + "<center><table width=720>");

				sb.append("<tr><td width=30><center>" + count + ".</td><td width=100><a action=\"bypass -h eventstats_show " + rset.getInt("player") + "\">" + rset.getString("char_name") + "</a></td><td width=65><center>" + rset.getInt("num") + "</td><td width=65><center>" + rset.getDouble("winpercent") + "%</td><td width=65><center>" + rset.getDouble("kdratio") + "</td><td width=65><center>" + rset.getInt("wins") + "</td><td width=65><center>" + rset.getInt("losses") + "</td><td width=65><center>" + rset.getInt("kills") + "</td>" + "<td width=65><center>" + rset.getInt("deaths") + "</td><td width=120><center>" + EventType.getEventByMainId(rset.getInt("favevent")).getHtmlTitle() + "</td></tr>");

				if (count % 10 == 0)
				{
					sb.append("</table></body></html>");
					htmls.put(count / 10, new ShowBoardData(sb.toString(), "101"));
					sb.clear();
				}
			}
			if (count % 10 != 0 && !htmls.containsKey(count / 10 + 1))
			{
				sb.append("</table></body></html>");
				htmls.put((count / 10 + 1), new ShowBoardData(sb.toString(), "101"));
				sb.clear();
			}
			rset.close();
			statement.close();

		}
		catch (Exception e)
		{
			System.out.println("create SQL exception.");
		}
		finally
		{
			try
			{
				con.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		NexusLoader.debug("createHtmls finished");
	}

	private void loadSQL()
	{
		if(!enabled)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			statement = con.prepareStatement("SELECT * FROM nexus_stats");
			ResultSet rset = statement.executeQuery();
			int count = 0;
			while (rset.next())
			{
				count++;
				if (!stats.containsKey(rset.getInt("player")))
					stats.put(rset.getInt("player"), new FastMap<Integer, StatModell>());

				stats.get(rset.getInt("player")).put(rset.getInt("event"), new StatModell(rset.getInt("num"), rset.getInt("wins"), rset.getInt("losses"), rset.getInt("kills"), rset.getInt("deaths"), rset.getInt("scores")));
			}
			rset.close();
			statement.close();

			NexusLoader.debug("Stats loaded: " + count + " records.");
		}
		catch (Exception e)
		{
			System.out.println("EventStats SQL catch");
		}
		finally
		{
			try
			{
				con.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		createHtmls();
	}

	public void showHtml(int id, PlayerEventInfo player)
	{
		if(!enabled)
		{
			player.sendMessage("The stat tracking is disabled.");
			return;
		}

		if(!htmls.containsKey(id))
			return;
		
		ShowBoardData sb;
		
		sb = htmls.get(id);
		sb.sendToPlayer(player);
		
		sb = new ShowBoardData(null, "102");
		sb.sendToPlayer(player);
		
		sb = new ShowBoardData(null, "103");
		sb.sendToPlayer(player);
	}

	public void showPlayerStats(int playerId, PlayerEventInfo player)
	{
		TextBuilder tb = new TextBuilder();
		String kdRatio;
		String winPercent;
		tb.append("<html><body><br><br><center><table width=640 bgcolor=5A5A5A><tr><td width=120><center>Event</td><td width=65><center>Count</td><td width=65><center>Win%</td><td width=65><center>K:D</td><td width=65><center>Wins</td><td width=65><center>Losses</td><td width=65><center>Kills</td><td width=65><center>Deaths</td><td width=65><center>Scores</td></tr></table><br>" + "<center><table width=640>");

		if (stats.containsKey(playerId))
			for (Map.Entry<Integer, StatModell> event : stats.get(playerId).entrySet())
			{
				StatModell stats = event.getValue();
				
				if(EventType.getEventByMainId(event.getKey()) != null)
				{
					kdRatio = String.valueOf((stats.deaths == 0 ? (double) stats.kills : ((double) stats.kills / (double) stats.deaths)));
					winPercent = String.valueOf((double)stats.wins / stats.num * 100);
					
					kdRatio = kdRatio.substring(0, Math.min(3, kdRatio.length()));
					winPercent = winPercent.substring(0, Math.min(5, winPercent.length()));
					
					tb.append("<tr><td width=120>" + EventType.getEventByMainId(event.getKey()).getHtmlTitle() + "</td><td width=65><center>" + stats.num + "</td><td width=65><center>" + winPercent + "%</td><td width=65><center>" + kdRatio + "</td><td width=65><center>" + stats.wins + "</td><td width=65><center>" + stats.losses + "</td><td width=65><center>" + stats.kills + "</td><td width=65><center>" + stats.deaths + "</td><td width=65><center>" + stats.scores + "</td></tr>");
				}
			}

		tb.append("</table></body></html>");
		
		ShowBoardData sb;
		
		sb = new ShowBoardData(tb.toString(), "101");
		sb.sendToPlayer(player);
		
		sb = new ShowBoardData(null, "102");
		sb.sendToPlayer(player);
		
		sb = new ShowBoardData(null, "103");
		sb.sendToPlayer(player);
	}

	// num | wins | losses | kills | deaths | fav_nexus_id
	private void sumPlayerStats()
	{
		if(!enabled)
			return;

		statSums.clear();

		for (int playerId : stats.keySet())
		{

			int num = 0;
			int wins = 0;
			int losses = 0;
			int kills = 0;
			int deaths = 0;
			int faveventid = 0;
			int faveventamm = 0;

			for (Map.Entry<Integer, StatModell> statmodell : stats.get(playerId).entrySet())
			{
				num += statmodell.getValue().num;
				wins += statmodell.getValue().wins;
				losses += statmodell.getValue().losses;
				kills += statmodell.getValue().kills;
				deaths += statmodell.getValue().deaths;

				if (statmodell.getValue().num > faveventamm)
				{
					faveventamm = statmodell.getValue().num;
					faveventid = statmodell.getKey();
				}
			}

			statSums.put(playerId, new int[] { num, wins, losses, kills, deaths, faveventid });

		}
		NexusLoader.debug("sumPlayerStats finished");
	}

	@SuppressWarnings("resource")
	public void updateSQL(Set<PlayerEventInfo> players, int eventId)
	{
		if(!enabled)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			sumPlayerStats();
			con = CallBack.getInstance().getOut().getConnection();

			for (PlayerEventInfo player : players)
			{
				int id = player.getPlayersId();

				if (statSums.get(id)[0] != 1)
				{
					statement = con.prepareStatement("UPDATE nexus_stats_full SET num=?, winpercent=?, kdratio=?, wins=?, losses=?, kills=?, deaths=?, favevent=? WHERE player=?");
					statement.setInt(1, statSums.get(id)[0]);
					statement.setDouble(2, (statSums.get(id)[0] == 0 ? 1 : (double)(statSums.get(id)[1] / statSums.get(id)[0])) * 100);
					statement.setDouble(3, (statSums.get(id)[4] == 0 ? (double) statSums.get(id)[3] : (double) (statSums.get(id)[3] / statSums.get(id)[4])));
					statement.setInt(4, statSums.get(id)[1]);
					statement.setInt(5, statSums.get(id)[2]);
					statement.setInt(6, statSums.get(id)[3]);
					statement.setInt(7, statSums.get(id)[4]);
					statement.setInt(8, statSums.get(id)[5]);
					statement.setInt(9, id);

					statement.executeUpdate();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO nexus_stats_full(player,num,winpercent,kdratio,wins,losses,kills,deaths,favevent) VALUES (?,?,?,?,?,?,?,?,?)");
					statement.setInt(1, id);
					statement.setInt(2, statSums.get(id)[0]);
					statement.setDouble(3, (statSums.get(id)[0] == 0 ? 1 : (double)(statSums.get(id)[1] / statSums.get(id)[0])) * 100);
					statement.setDouble(4, (statSums.get(id)[4] == 0 ? (double) statSums.get(id)[3] : (double) (statSums.get(id)[3] / statSums.get(id)[4])));
					statement.setInt(5, statSums.get(id)[1]);
					statement.setInt(6, statSums.get(id)[2]);
					statement.setInt(7, statSums.get(id)[3]);
					statement.setInt(8, statSums.get(id)[4]);
					statement.setInt(9, statSums.get(id)[5]);
					statement.executeUpdate();
					statement.close();
				}
				
				if (stats.get(id).get(eventId).num != 1)
				{
					statement = con.prepareStatement("UPDATE nexus_stats SET num=?, wins=?, losses=?, kills=?, deaths=?, scores=? WHERE player=? AND event=?");
					statement.setInt(1, stats.get(id).get(eventId).num);
					statement.setInt(2, stats.get(id).get(eventId).wins);
					statement.setInt(3, stats.get(id).get(eventId).losses);
					statement.setInt(4, stats.get(id).get(eventId).kills);
					statement.setInt(5, stats.get(id).get(eventId).deaths);
					statement.setInt(6, stats.get(id).get(eventId).scores);
					statement.setInt(7, id);
					statement.setInt(8, eventId);
					statement.executeUpdate();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO nexus_stats(player,event,num,wins,losses,kills,deaths,scores) VALUES (?,?,?,?,?,?,?,?)");
					statement.setInt(1, id);
					statement.setInt(2, eventId);
					statement.setInt(3, stats.get(id).get(eventId).num);
					statement.setInt(4, stats.get(id).get(eventId).wins);
					statement.setInt(5, stats.get(id).get(eventId).losses);
					statement.setInt(6, stats.get(id).get(eventId).kills);
					statement.setInt(7, stats.get(id).get(eventId).deaths);
					statement.setInt(8, stats.get(id).get(eventId).scores);
					statement.executeUpdate();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			//System.out.println("EventStats SQL catch");
		}
		finally
		{
			try
			{
				con.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		NexusLoader.debug("updateSQL finished");
		createHtmls();
	}
}
