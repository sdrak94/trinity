/**
 * 
 */
package cz.nxs.events.engine.stats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class GlobalStats extends EventStats
{
	private Map<PlayerEventInfo, Map<EventType, GlobalStatsModel>> _playerGlobalStats;
	private Map<PlayerEventInfo, String> _playerGlobalStatsHtml;
	
	private Map<SortType, Map<Integer, String>> _globalStatsHtml;
	
	private Map<Integer, GlobalStatsSum> _data;
	
	private ScheduledFuture<?> _globalStatsReload;
	private long _lastLoad;
	
	public boolean _enableStatistics;
	public boolean _enableGlobalStatistics;
	private String _statsSorting;
	private boolean _ignoreBannedPlayers = true;
	private boolean _ignoreGMs = false;
	private int _playersPerPage = 8;
	private int _statsRefresh = 1800;
	
	private boolean _showDetailedPlayerInfo = true;
	private boolean _showPkCount = true;
	
	private boolean globalStatsLoaded = false;
	
	public enum GlobalStatType
	{
		COUNT_PLAYED("count played"),
		WINS("wins"),
		LOSES("loses"),
		KILLS("kills"),
		DEATHS("deaths"),
		SCORE("score");
		
		String name;
		GlobalStatType(String name)
		{
			this.name= name;
		}
	}
	
	private enum SortType
	{
		NAME("characters.char_name"),
		COUNTPLAYED("nexus_stats_global.count_played DESC"),
		WINS("nexus_stats_global.wins DESC"),
		LOSES("nexus_stats_global.loses DESC"),
		SCORE("nexus_stats_global.score DESC"),
		DEATHS("nexus_stats_global.deaths DESC"),
		LEVEL("characters.level DESC"),
		KDRATIO("");
		
		SortType(String dbName)
		{
		}
	}
	
	public GlobalStats()
	{
		
	}
	
	private void loadConfigs()
	{
		_enableStatistics = EventConfig.getInstance().getGlobalConfigBoolean("enableStatistics");
		_enableGlobalStatistics = EventConfig.getInstance().getGlobalConfigBoolean("enableGlobalStatistics");
		_statsRefresh = EventConfig.getInstance().getGlobalConfigInt("globalStatisticsRefresh");
		_statsSorting = EventConfig.getInstance().getGlobalConfigValue("statsSorting");
		_ignoreBannedPlayers = EventConfig.getInstance().getGlobalConfigBoolean("statsIgnoreBanned");
		_ignoreGMs = EventConfig.getInstance().getGlobalConfigBoolean("statsIgnoreGMs");
		_playersPerPage = EventConfig.getInstance().getGlobalConfigInt("statsPlayersPerPage");
		
		_showDetailedPlayerInfo = EventConfig.getInstance().getGlobalConfigBoolean("statsDetailedPlayerInfo");
		_showPkCount = EventConfig.getInstance().getGlobalConfigBoolean("statsShowPkCount");
	}
	
	public GlobalStatsModel getPlayerGlobalStatsCopy(PlayerEventInfo player, EventType type)
	{
		GlobalStatsModel oldModel = _playerGlobalStats.get(player).get(type);
		
		FastMap<GlobalStatType, Integer> stats = new FastMap<GlobalStatType, Integer>();
		stats.putAll(oldModel.stats);

		return new GlobalStatsModel(type, stats);
	}
	
	public GlobalStatsModel getPlayerGlobalStats(PlayerEventInfo player, EventType type)
	{
		return _playerGlobalStats.get(player).get(type);
	}
	
	public void setPlayerGlobalStats(PlayerEventInfo player, EventType type, GlobalStatsModel stats)
	{
		_playerGlobalStats.get(player).put(type, stats);
	}

	@Override
	public void load()
	{
		_playerGlobalStats = new FastMap<PlayerEventInfo, Map<EventType, GlobalStatsModel>>();
		_playerGlobalStatsHtml = new FastMap<PlayerEventInfo, String>();
		
		loadConfigs();
		
		loadGlobalStats();
		
		NexusLoader.debug("Global statistics engine loaded.");
	}
	
	@Override
	public void onLogin(PlayerEventInfo player)
	{
		loadPlayer(player);
	}

	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		forgetPlayerGlobalStats(player);
	}
	
	@Override
	public void statsChanged(PlayerEventInfo player)
	{
		_playerGlobalStatsHtml.remove(player);
	}
	
	@Override
	public void onCommand(PlayerEventInfo player, String command)
	{
		if(command.startsWith("oneplayer"))
		{
			PlayerEventInfo target = null;
			String name = null;
			String sortType = null;
			String page = null;
			
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			if(st.hasMoreTokens())
				name = st.nextToken();
			
			if(st.hasMoreTokens())
				sortType = st.nextToken();
			
			if(st.hasMoreTokens())
				page = st.nextToken();
			
			boolean backToCbMenu = false;
			
			if(name == null)
				target = player;
			else
			{
				if(name.equals("cbmenu"))
				{
					target = player;
					backToCbMenu = true;
				}
				else
					target = CallBack.getInstance().getOut().getPlayer(name);
			}
			
			if(target != null)
				showPlayersGlobalStats(player, target, sortType, page, backToCbMenu);
			else
			{
				player.screenMessage("This player is either offline or doesn't exist.", "Statistics", false);
				player.sendMessage("This player is either offline or doesn't exist.");
			}
		}
		else if(command.startsWith("topplayers"))
		{
			String params = command.substring(11);
			showGlobalStats(player, params);
		}
	}
	
	private void showGlobalStats(PlayerEventInfo player, String params)
	{
		if(!globalStatsLoaded || !_enableStatistics || !_enableGlobalStatistics)
		{
			player.sendMessage("Statistics engine is turned off.");
			return;
		}
		
		StringTokenizer st = new StringTokenizer(params);
		
		int page = Integer.parseInt(st.nextToken());
		if(page == 0)
			page = 1;
		SortType sort = st.hasMoreTokens() ? SortType.valueOf(st.nextToken()) : null;
		
		boolean backToCbMenu = false;
		
		/*if(st.hasMoreTokens())
		{
			if(st.nextToken().equals("cbmenu"))
			{
				backToCbMenu = true;
			}
		}*/
		
		backToCbMenu = true;
		
		if(sort != null)
		{
			String text = null;
			try
			{
				text = _globalStatsHtml.get(sort).get(page);
			}
			catch (Exception e)
			{
				if(player != null)
				{
					player.sendMessage("Statistics engine will become functional as soon as some events are runned.");
				}
				return;
			}
			
			if(text != null)
			{
				text = updateStatuses(text, sort.toString(), page);
				
				if(backToCbMenu)
					text = text.replaceAll("%back%", "<button value=\"Back\" width=60 action=\"bypass nxs_showstats_cbmenu\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				else
					text = text.replaceAll("%back%", "");
				
				showHtmlText(player, text);
			}
		}
	}
	
	private void showPlayersGlobalStats(PlayerEventInfo player, PlayerEventInfo target, String sortType, String page, boolean backToCbMenu)
	{
		if(!_enableStatistics)
		{
			player.sendMessage("Statistics engine is turned off.");
			return;
		}
		
		statsChanged(target);
		
		if(_ignoreGMs && target.isGM() && !player.isGM())
		{
			player.sendMessage("GM's stats are uber secret.");
			return;
		}
		
		String text = null;
		if(!_playerGlobalStatsHtml.containsKey(target))
			text = generatePlayersGlobalStatsHtml(target);
		else
			text = _playerGlobalStatsHtml.get(target);
		
		text = addExtraData(text, sortType, page, backToCbMenu);
			
		if(text != null)
		{
			showHtmlText(player, text);
			player.sendStaticPacket();
		}
	}
	
	private String addExtraData(String text, String sortType, String page, boolean backToCbMenu)
	{
		if(backToCbMenu)
		{
			text = text.replaceAll("%data%", "<button value=\"Back\" width=60 action=\"bypass nxs_showstats_cbmenu\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		else
		{
			if(sortType != null && page != null)
			{
				int pageNumber = Integer.parseInt(page);
				
				text = text.replaceAll("%data%", "<button value=\"Back\" width=60 action=\"bypass nxs_showstats_global_topplayers " + pageNumber + " " + sortType + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			}
			else
			{
				text = text.replaceAll("%data%", "");
			}
		}
		
		return text;
	}
	
	private String generatePlayersGlobalStatsHtml(PlayerEventInfo player)
	{
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><body><br><center>");
		
		if(_showDetailedPlayerInfo)
		{
			GlobalStatsSum sum = _data.get(player.getPlayersId());
			
			if(sum != null)
			{
				tb.append("<font color=ac9887>" + player.getPlayersName() + " </font><font color=9f9f9f>(Lvl " + player.getLevel() + " " + player.getClassName() + ")</font><br>");
				
				tb.append("<center><table width=430 bgcolor=2E2E2E>"); // level, clan, ally, class, pvp kills, pk kills (configurable)
				
				String clan = CallBack.getInstance().getOut().getClanName(player.getClanId());
				String ally = CallBack.getInstance().getOut().getAllyName(player.getClanId());
				
				tb.append("<tr><td width=90><font color=B09D8E>Clan name:</font></td><td width=155 align=left><font color=A9A8A7>" + (clan == null ? "<font color=6f6f6f>No clan</font>" : clan) + "</font></td>");
				tb.append("<td width=80><font color=B09D8E>Ally name:</font></td><td width=120 align=left><font color=A9A8A7>" + (ally == null ? "<font color=6f6f6f>No ally</font>" : ally) + "</font></td></tr>");
				
				String pvps = String.valueOf(player.getPvpKills());
				
				String pks = String.valueOf(player.getPkKills());
				if(!_showPkCount)
					pks = "<font color=6f6f6f>-secret-</font>";
				
				tb.append("<tr><td width=90><font color=B09D8E>PvP kills:</font></td><td width=155 align=left><font color=B3AA9D>" + pvps + "</font></td>");
				tb.append("<td width=80><font color=B09D8E>PK count:</font></td><td width=120 align=left><font color=B3AA9D>" + pks + "</font></td></tr>");
				
				tb.append("<tr></tr><tr><td width=90><font color=B09D8E>Won:</font></td><td width=155 align=left><font color=A9A8A7>" + sum.get(GlobalStatType.WINS) + " </font><font color=8f8f8f>events</font></td>");
				tb.append("<td width=80><font color=B09D8E>Lost:</font></td><td width=120 align=left><font color=A9A8A7>" + sum.get(GlobalStatType.LOSES) + " <font color=8f8f8f>events</font></td></tr>");
				
				tb.append("<tr><td width=86><font color=B09D8E>Participated:</font></td><td width=120 align=left><font color=A9A8A7><font color=8f8f8f>in</font> " + sum.get(GlobalStatType.COUNT_PLAYED) + " <font color=8f8f8f>events</font></td>");
				tb.append("<td width=80><font color=B09D8E>K:D ratio:</font></td><td width=155 align=left><font color=A9A8A7>" + sum.kdRatio + "</font></font></td></tr>");
				
				tb.append("<tr><td width=90><font color=B09D8E>Kills/Deaths:</font></td><td width=155 align=left><font color=A9A8A7>" + sum.get(GlobalStatType.KILLS) + " / " + sum.get(GlobalStatType.DEATHS) + "</font></font></td>");
				tb.append("<td width=80><font color=B09D8E>Score:</font></td><td width=120 align=left><font color=A9A8A7>" + sum.get(GlobalStatType.SCORE) + "</font></td></tr>");
				
				tb.append("</table><br><br><br>");
			}
		}
		
		tb.append("<font color=ac9887>" + player.getPlayersName() + "'s event statistics</font><br1>");
		tb.append("<font color=6f6f6f>(click on event for more info)</font><br>");
		
		String success, kdRatio;
		
		tb.append("<table width=740 bgcolor=4E4E4E><tr> "
				+ "<td width=130><font color=B09D8E>Event</font></td><td width=90 align=center><font color=A9A8A7>Times played</font></td><td width=65 align=center><font color=A9A8A7>Win %</font></td><td width=65 align=center><font color=A9A8A7>K:D ratio</font></td><td width=65 align=center><font color=A9A8A7>Wins</font></td><td width=65 align=center><font color=A9A8A7>Loses</font></td><td width=65 align=center><font color=A9A8A7>Kills</font></td><td width=65 align=center><font color=A9A8A7>Deaths</font></td><td width=65 align=center><font color=A9A8A7>Score</font></td></tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=740 height=6>");
		
		boolean bg = false;
		
		GlobalStatsModel stats;
		for(EventType event : EventType.values())
		{
			if(_playerGlobalStats.get(player) == null)
			{
				tb.append("<table width=740><tr><td>Event data not available.</td></tr></table>");
				break;
			}
			
			stats = _playerGlobalStats.get(player).get(event);
			if(stats == null)
				continue;
			
			int kills = stats.get(GlobalStatType.KILLS);
			int deaths = stats.get(GlobalStatType.DEATHS);
			int timesPlayed = stats.get(GlobalStatType.COUNT_PLAYED);
			int wins = stats.get(GlobalStatType.WINS);
			
			kdRatio = String.valueOf((deaths == 0 ? (double) kills : ((double) kills / (double) deaths)));
			success = String.valueOf((int) ((double)wins / timesPlayed * 100));
			kdRatio = kdRatio.substring(0, Math.min(3, kdRatio.length()));
			success = success.substring(0, Math.min(5, success.length())) + "%";
			
			tb.append("<table width=740 bgcolor=" + (bg ? "3E3E3E" : "2E2E2E") + "><tr><td width=130><font color=B09D8E>" + event.getHtmlTitle() + "</font> </td><td width=90 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.COUNT_PLAYED) + "</font></td><td width=65 align=center><font color=B3AA9D>" + success + "</font></td><td width=65 align=center><font color=B3AA9D>" + kdRatio + "</font></td><td width=65 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.WINS) + "</font></td><td width=65 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.LOSES) + "</font></td><td width=65 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.KILLS) + "</font></td><td width=65 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.DEATHS) + "</font></td><td width=65 align=center><font color=B3AA9D>" + stats.get(GlobalStatType.SCORE) + "</font></td></tr>");
			tb.append("</table><img src=\"L2UI.SquareBlank\" width=740 height=3>");
			
			bg = !bg;
		}
		
		tb.append("<br>%data%");
		tb.append("</center></body></html>");
		
		_playerGlobalStatsHtml.put(player, tb.toString());
		return tb.toString();
	}
	
	private void loadPlayer(PlayerEventInfo player)
	{
		if(!_playerGlobalStats.containsKey(player))
		{
			synchronized(_playerGlobalStats)
			{
				_playerGlobalStats.put(player, new FastMap<EventType, GlobalStatsModel>());
			}
		}
		
		GlobalStatsModel stats;
		Connection con = null;

		EventType type;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT event, count_played, wins, loses, kills, deaths, score FROM nexus_stats_global WHERE player = " + player.getPlayersId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				type = EventType.getType(rset.getString("event"));
				
				if(type != null)
				{
					Map<GlobalStatType, Integer> map = new FastMap<GlobalStatType, Integer>();
					
					map.put(GlobalStatType.COUNT_PLAYED, rset.getInt("count_played"));
					map.put(GlobalStatType.WINS, rset.getInt("wins"));
					map.put(GlobalStatType.LOSES, rset.getInt("loses"));
					map.put(GlobalStatType.KILLS, rset.getInt("kills"));
					map.put(GlobalStatType.DEATHS, rset.getInt("deaths"));
					map.put(GlobalStatType.SCORE, rset.getInt("score"));
					
					stats = new GlobalStatsModel(type, map);

					try
					{
						synchronized(_playerGlobalStats)
						{
							_playerGlobalStats.get(player).put(type, stats);
						}
					}
					catch (Exception e)
					{
						try
						{
							NexusLoader.debug("An error occured while running GlobalStas.loadPlayer for player " + player.getPlayersName(), Level.WARNING);
						}
						catch (NullPointerException e2)
						{
							NexusLoader.debug("An error occured while running GlobalStas.loadPlayer, player is null", Level.WARNING);
						}
						
						e.printStackTrace();
					}
				}
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
			{
			}
		}
	}
	
	public void updateGlobalStats(Map<PlayerEventInfo, GlobalStatsModel> data)
	{
		Connection con = null;
		
		GlobalStatsModel stats;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement;
			
			for(Entry<PlayerEventInfo, GlobalStatsModel> e : data.entrySet())
			{
				statsChanged(e.getKey());
				
				stats = getPlayerGlobalStats(e.getKey(), e.getValue().getEvent());
				
				if(stats == null)
				{
					stats = e.getValue();
					setPlayerGlobalStats(e.getKey(), stats.getEvent(), stats);
				}
				else
				{
					stats.add(e.getValue());
				}
				
				statement = con.prepareStatement("REPLACE INTO nexus_stats_global VALUES (?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, e.getKey().getPlayersId());
				statement.setString(2, stats.getEvent().getAltTitle());
				statement.setInt(3, stats.get(GlobalStatType.COUNT_PLAYED));
				statement.setInt(4, stats.get(GlobalStatType.WINS));
				statement.setInt(5, stats.get(GlobalStatType.LOSES));
				statement.setInt(6, stats.get(GlobalStatType.KILLS));
				statement.setInt(7, stats.get(GlobalStatType.DEATHS));
				statement.setInt(8, stats.get(GlobalStatType.SCORE));
				statement.setString(9, stats.getFavoriteEvent());
				
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
			{
			}
		}
	}
	
	private void forgetPlayerGlobalStats(PlayerEventInfo player)
	{
		synchronized(_playerGlobalStats)
		{
			_playerGlobalStats.remove(player);
		}
	}
	
	private class GlobalStatsSum
	{
		protected Map<GlobalStatType, Integer> stats;
		
		protected String name;
		@SuppressWarnings("unused")
		protected int level, pvp, pk, clan, classId;
		protected EventType mostPlayedEvent;
		protected int mostPlayedCount = 0;
		
		protected String kdRatio;
		
		public GlobalStatsSum(String name, int level, int pvp, int pk, int clan, int classId)
		{
			this.name = name;
			this.level = level;
			this.pvp = pvp;
			this.pk = pk;
			this.clan = clan;
			this.classId = classId;
			this.stats = new FastMap<GlobalStatType, Integer>();
			
			for(GlobalStatType t : GlobalStatType.values())
			{
				stats.put(t, 0);
			}
		}
		
		public int get(GlobalStatType type)
		{
			return stats.get(type);
		}
		
		public void set(GlobalStatType type, int value)
		{
			stats.put(type, value);
		}
		
		public void raise(GlobalStatType type, int value)
		{
			set(type, get(type) + value);
			if(type == GlobalStatType.KILLS || type == GlobalStatType.DEATHS)
				updateKdRatio();
		}
		
		private void updateKdRatio()
		{
			int kills = get(GlobalStatType.KILLS);
			int deaths = get(GlobalStatType.DEATHS);
			kdRatio = String.valueOf((deaths == 0 ? (double) kills : ((double) kills / (double) deaths)));
			kdRatio = kdRatio.substring(0, Math.min(3, kdRatio.length()));
		}
	}
	
	protected String updateStatuses(String text, String sortType, int page)
	{
		String updated = text;
		int start = 0, end = 0;
		String name;
		PlayerEventInfo player;
		
		while(true)
		{
			start = updated.indexOf("<i>");
			if(start == -1)
				break;
			
			start += 3;
			end = updated.indexOf("</i>");
			
			name = updated.substring(start, end);
			player = CallBack.getInstance().getOut().getPlayer(name);
			
			if(player != null)
			{
				updated = updated.replaceFirst("<i>", "<font color=9EB39D><a action=\"bypass -h nxs_showstats_global_oneplayer " + name + " " + sortType + " " + page + "\">");
				updated = updated.replaceFirst("</i>", "</a></font>");
			}
			else
			{
				updated = updated.replaceFirst("<i>", "<font color=A9A8A7>");
				updated = updated.replaceFirst("</i>", "</font>");
			}
		}
		
		updated = updated.replaceAll("%reloaded%", calcLastLoadedTime());
		
		return updated;
	}
	
	protected void loadGlobalStats()
	{
		loadConfigs();
		
		if(!_enableStatistics || !_enableGlobalStatistics)
			return;
		
		_globalStatsHtml = new FastMap<SortType, Map<Integer, String>>();
		
		TextBuilder tb = new TextBuilder();
		
		Connection con = null;
		PreparedStatement statement = null;
		
		int timesPlayed, wins, loses, kills, deaths, score, level, pvpkills, pkkills, clanid, classid, charId;
		String charName = null;
		String kdRatio, success, clan;
		
		final int playersPerPage = _playersPerPage;
		
		String condition = "";
		if(_ignoreGMs && _ignoreBannedPlayers)
			condition = "WHERE characters.accesslevel = 0";
		else if(_ignoreGMs)
			condition = "WHERE characters.accesslevel <= 0";
		else if(_ignoreBannedPlayers)
			condition = "WHERE characters.accesslevel >= 0";
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			//for(SortType sortType : SortType.values())
			{
				_data = new FastMap<Integer, GlobalStats.GlobalStatsSum>();
				statement = con.prepareStatement("SELECT characters.char_name, characters.charId, characters.online, characters.level, characters.pvpkills, characters.pkkills, characters.clanid, characters.classid, nexus_stats_global.* FROM nexus_stats_global INNER JOIN characters ON characters.charId = nexus_stats_global.player " + condition + " ORDER BY characters.char_name");
				
				ResultSet rset = statement.executeQuery();
				
				while(rset.next())
				{
					charId = rset.getInt("charId");
					
					if(!_data.containsKey(charId))
					{
						charName = rset.getString("char_name");
						level = rset.getInt("level");
						pvpkills = rset.getInt("pvpkills");
						pkkills = rset.getInt("pkkills");
						clanid = rset.getInt("clanid");
						classid = rset.getInt("classid");
						
						_data.put(charId, new GlobalStatsSum(charName, level, pvpkills, pkkills, clanid, classid));
					}
					
					timesPlayed = rset.getInt("count_played");
					
					if(timesPlayed > _data.get(charId).mostPlayedCount)
					{
						_data.get(charId).mostPlayedCount = timesPlayed;
						_data.get(charId).mostPlayedEvent = EventType.getType(rset.getString("event"));
					}
					
					wins = rset.getInt("wins");
					loses = rset.getInt("loses");
					kills = rset.getInt("kills");
					deaths = rset.getInt("deaths");
					score = rset.getInt("score");
					
					_data.get(charId).raise(GlobalStatType.COUNT_PLAYED, timesPlayed);
					_data.get(charId).raise(GlobalStatType.WINS, wins);
					_data.get(charId).raise(GlobalStatType.LOSES, loses);
					_data.get(charId).raise(GlobalStatType.KILLS, kills);
					_data.get(charId).raise(GlobalStatType.DEATHS, deaths);
					_data.get(charId).raise(GlobalStatType.SCORE, score);
				}
				
				rset.close();
				statement.close();
				
				List<GlobalStatsSum> sorted;
				
				int type = 1;
				if(_statsSorting.equals("advanced"))
					type = 2;
				else if(_statsSorting.equals("full"))
					type = 3;
				
				for(SortType sortType : SortType.values())
				{
					if(type == 1)
					{
						if(sortType != SortType.NAME && sortType != SortType.LEVEL)
							continue;
					}
					else if(type == 2)
					{
						if(sortType != SortType.NAME && sortType != SortType.LEVEL && sortType != SortType.COUNTPLAYED && sortType != SortType.KDRATIO)
							continue;
					}
					
					sorted = new FastList<GlobalStatsSum>();
					sorted.addAll(_data.values());
					
					switch(sortType)
					{
						case NAME:
						{
							// already sorted by this value from database
							break;
						}
						case LEVEL:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int level1 = stats1.level;
									int level2 = stats2.level;
									
									return level1 == level2 ? 0 : level1 < level2 ? 1 : -1;
								}
							});
							break;
						}
						case WINS:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int wins1 = stats1.get(GlobalStatType.WINS);
									int wins2 = stats2.get(GlobalStatType.WINS);
									
									return wins1 == wins2 ? 0 : wins1 < wins2 ? 1 : -1;
								}
							});
							break;
						}
						case DEATHS:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int deaths1 = stats1.get(GlobalStatType.DEATHS);
									int deaths2 = stats2.get(GlobalStatType.DEATHS);
									
									return deaths1 == deaths2 ? 0 : deaths1 < deaths2 ? 1 : -1;
								}
							});
							break;
						}
						case SCORE:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int score1 = stats1.get(GlobalStatType.SCORE);
									int score2 = stats2.get(GlobalStatType.SCORE);
									
									return score1 == score2 ? 0 : score1 < score2 ? 1 : -1;
								}
							});
							break;
						}
						case COUNTPLAYED:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int count1 = stats1.get(GlobalStatType.COUNT_PLAYED);
									int count2 = stats2.get(GlobalStatType.COUNT_PLAYED);
									
									return count1 == count2 ? 0 : count1 < count2 ? 1 : -1;
								}
							});
							break;
						}
						case LOSES:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									int loses1 = stats1.get(GlobalStatType.LOSES);
									int loses2 = stats2.get(GlobalStatType.LOSES);
									
									return loses1 == loses2 ? 0 : loses1 < loses2 ? 1 : -1;
								}
							});
							break;
						}
						case KDRATIO:
						{
							Collections.sort(sorted, new Comparator<GlobalStatsSum>()
							{
								@Override
								public int compare(GlobalStatsSum stats1, GlobalStatsSum stats2)
								{
									try
									{
										double ratio1 = Double.valueOf(stats1.kdRatio);
										double ratio2 = Double.valueOf(stats2.kdRatio);
										
										return ratio1 == ratio2 ? 0 : ratio1 < ratio2 ? 1 : -1;
									}
									catch (Exception e)
									{
										return 0;
									}
								}
							});
							break;
						}
					}
					
					int size = _data.size();
					int count = 0;
					boolean bg = false;
					
					for(GlobalStatsSum stats : sorted)
					{
						count++;
						
						if (count % playersPerPage == 1)
						{
							tb.append("<html><body><br><center><font color=ac9887>Server event statistics</font><br1><font color=7f7f7f>(reloaded: %reloaded%)</font><br><br>");
							tb.append("<table width=725><tr><td width=70 align=left><button value=\"Refresh\" width=70 action=\"bypass nxs_showstats_global_topplayers " + (((count-1) / playersPerPage) + 1) + " " + sortType.toString() + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=500 align=center><font color=7f7f7f>(click on column name to sort the players)</font></td><td width=60><font color=ac9887>Name:</font></td><td width=100 align=left><edit var=\"name\" width=100 height=14></td><td width=65 align=right><button value=\"Find\" width=60 action=\"bypass nxs_showstats_global_oneplayer $name " + sortType.toString() + " " + (((count - 1) / playersPerPage) + 1) + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
							
							if(_statsSorting.equals("simple"))
							{
								tb.append("<br><center><table width=747 bgcolor=5A5A5A><tr><td width=25 ><font color=B09D8E>&nbsp;#</font></td><td width=125><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.NAME.toString() + "\">Name</a>&nbsp;(<a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.LEVEL.toString() + "\">lvl</a>)</font></td><td width=55><font color=A9A8A7>Clan</font></td><td width=55><font color=A9A8A7>Played ev.</font></td><td width=50><center><font color=A9A8A7>&nbsp;Win%</font></td><td width=50><center><font color=A9A8A7>K:D</font></td><td width=50><center><font color=A9A8A7>Wins</font></td><td width=55><center><font color=A9A8A7>Loses</font></td><td width=55><center><font color=A9A8A7>Score</font></td><td width=55><center><font color=A9A8A7>Deaths</font></td><td width=95><center><font color=A9A8A7>Favorite Event</font></td></tr></table><br>" + "<center>");
							}
							else if(_statsSorting.equals("advanced"))
							{
								tb.append("<br><center><table width=747 bgcolor=5A5A5A><tr><td width=25 ><font color=B09D8E>&nbsp;#</font></td><td width=125><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.NAME.toString() + "\">Name</a>&nbsp;(<a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.LEVEL.toString() + "\">lvl</a>)</font></td><td width=55><font color=A9A8A7>Clan</font></td><td width=55><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.COUNTPLAYED.toString() + "\">Played ev.</a></font></td><td width=50><center><font color=A9A8A7>&nbsp;Win%</font></td><td width=50><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.KDRATIO.toString() + "\">K:D</a></font></td><td width=50><center><font color=A9A8A7>Wins</font></td><td width=55><center><font color=A9A8A7>Loses</font></td><td width=55><center><font color=A9A8A7>Score</font></td><td width=55><center><font color=A9A8A7>Deaths</font></td><td width=95><center><font color=A9A8A7>Favorite Event</font></td></tr></table><br>" + "<center>");
							}
							else if(_statsSorting.equals("full"))
							{
								tb.append("<br><center><table width=747 bgcolor=5A5A5A><tr><td width=25 ><font color=B09D8E>&nbsp;#</font></td><td width=125><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.NAME.toString() + "\">Name</a>&nbsp;(<a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.LEVEL.toString() + "\">lvl</a>)</font></td><td width=55><font color=A9A8A7>Clan</font></td><td width=55><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.COUNTPLAYED.toString() + "\">Played ev.</a></font></td><td width=50><center><font color=A9A8A7>&nbsp;Win%</font></td><td width=50><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.KDRATIO.toString() + "\">K:D</a></font></td><td width=50><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.WINS.toString() + "\">Wins</a></font></td><td width=55><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.LOSES.toString() + "\">Loses</a></font></td><td width=55><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.SCORE.toString() + "\">Score</a></font></td><td width=55><center><font color=A9A8A7><a action=\"bypass -h nxs_showstats_global_topplayers " + ((count-1) / playersPerPage) + " " + SortType.DEATHS.toString() + "\">Deaths</a></font></td><td width=95><center><font color=A9A8A7>Favorite Event</font></td></tr></table><br>" + "<center>");
							}
						}
						
						tb.append("<center><table width=740 " + (bg ? "bgcolor=3E3E3E" : "bgcolor=2E2E2E") + "><tr><td width=30 align=left><font color=B09D8E>&nbsp;" + count + ".</font></td>");
						bg = !bg;
						
						clan = CallBack.getInstance().getOut().getClanName(stats.clan);
						if(clan == null)
							clan = "";
						else if(clan.length() > 15)
							clan = clan.substring(0, 12) + "..";
						
						tb.append("<td width=115><i>" + stats.name + "</i> <font color=A9A8A7>(" + stats.level + ")</font></td><td width=108 align=left><font color=B09D8E>" + clan + "</font></td>");
						
						timesPlayed = stats.get(GlobalStatType.COUNT_PLAYED);
						
						wins = stats.get(GlobalStatType.WINS);
						loses = stats.get(GlobalStatType.LOSES);
						kills = stats.get(GlobalStatType.KILLS);
						deaths = stats.get(GlobalStatType.DEATHS);
						score = stats.get(GlobalStatType.SCORE);
						kdRatio = stats.kdRatio;
						
						success = String.valueOf((int) ((double)wins / timesPlayed * 100));
						success = success.substring(0, Math.min(5, success.length()));
						
						tb.append("<td width=53 align=left><font color=B3AA9D>" + timesPlayed + "</font></td><td width=53><font color=B3AA9D>" + success + "%</font></td><td width=45><font color=B3AA9D>&nbsp;" + kdRatio + "</font></td><td width=57><center><font color=B3AA9D>" + wins + "</font></td><td width=55><center><font color=B3AA9D>" + loses + "</font></td><td width=55><center>&nbsp;&nbsp;<font color=B3AA9D>" + score + "</font></td>" + "<td width=55><center>&nbsp;&nbsp;&nbsp;<font color=B3AA9D>" + deaths + "</font></td><td width=120><center><font color=B3AA9D>" + (stats.mostPlayedEvent != null ? stats.mostPlayedEvent.getAltTitle() : "N/A") + "</font> <font color=7B7A79>(" + stats.mostPlayedCount + "x)</font></td></tr></table><img src=\"L2UI.SquareBlank\" width=740 height=3>");

						if (count % playersPerPage == 0)
						{
							tb.append("<center><br><br><table width=140><tr><td width=70 align=left>" + ((count - 1) / playersPerPage != 0 ? "<button value=\"Page " + ((count - 1) / playersPerPage) + "\" width=60 action=\"bypass nxs_showstats_global_topplayers " + ((count - 1) / playersPerPage) + " " + sortType.toString() + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "<font color=ac9887>Page " + (((count - 1) / playersPerPage)+1) + "</font>") + "</td>"
									+ "<td width=70 align=right>" + ((count - 1) / playersPerPage != size / playersPerPage ? "<button value=\"Page " + (((count - 1) / playersPerPage) + 2) + "\" width=60 action=\"bypass nxs_showstats_global_topplayers " + (((count - 1) / playersPerPage) + 2) + " " + sortType.toString() + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "<font color=ac9887>Page " + (((count - 1) / playersPerPage)+2) + "</font>") + "</td></tr></table>");
							
							tb.append("<center><br>%back%</center></body></html>");
							
							if(!_globalStatsHtml.containsKey(sortType))
								_globalStatsHtml.put(sortType, new FastMap<Integer, String>());
							
							_globalStatsHtml.get(sortType).put(count / playersPerPage, tb.toString());
							tb.clear();
						}
					}
					
					if (count % playersPerPage != 0)
					{
						tb.append("<center><br><br><table width=140><tr><td width=70 align=left>" + ((count - 1) / playersPerPage != 0 ? "<button value=\"Page " + ((count - 1) / playersPerPage) + "\" width=60 action=\"bypass nxs_showstats_global_topplayers " + ((count - 1) / playersPerPage) + " " + sortType.toString() + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "<font color=ac9887>Page " + (((count - 1) / playersPerPage)+1) + "</font>") + "</td>"
								+ "<td width=70 align=right>" + ((count - 1) / playersPerPage != size / playersPerPage ? "<button value=\"Page " + (((count - 1) / playersPerPage) + 2) + "\" width=60 action=\"bypass nxs_showstats_global_topplayers " + (((count - 1) / playersPerPage) + 2) + " " + sortType.toString() + "\" height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "<font color=ac9887>Page " + (((count - 1) / playersPerPage)+1) + "</font>") + "</td></tr></table>");
						
						tb.append("<center><br>%back%</center></body></html>");
						
						if(!_globalStatsHtml.containsKey(sortType))
							_globalStatsHtml.put(sortType, new FastMap<Integer, String>());
						
						if(!_globalStatsHtml.get(sortType).containsKey(count / playersPerPage + 1))
						{
							_globalStatsHtml.get(sortType).put((count / playersPerPage + 1), tb.toString());
							tb.clear();
						}
					}
				}
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
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		
		globalStatsLoaded = true;
		
		_lastLoad = System.currentTimeMillis();
		
		NexusLoader.debug("Global statistics reloaded.");
		
		scheduleReloadGlobalStats();
	}
	
	private String calcLastLoadedTime()
	{
		long time = System.currentTimeMillis();
		long diff = (time - _lastLoad) / 1000;
		
		if(diff > 3600)
			return (diff / 3600 + " hours ago");
		else if(diff > 60)
			return (diff / 60 + " minutes ago");
		else
			return (diff + " seconds ago");
	}
	
	private synchronized void scheduleReloadGlobalStats()
	{
		if(_globalStatsReload != null)
		{
			_globalStatsReload.cancel(false);
			_globalStatsReload = null;
		}
		
		_globalStatsReload = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				loadGlobalStats();
			}
		}, _statsRefresh*1000);
	}
}
