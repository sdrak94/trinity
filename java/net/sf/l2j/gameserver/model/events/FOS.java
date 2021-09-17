package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFOSEngine;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class FOS
{
	protected static final Logger			_log					= Logger.getLogger(FOS.class.getName());
	public static boolean					_joining				= false, _teleport = false, _started = false,
	_sitForced = false;
	public static int						_FlagNPC				= 35062, _npcId = 0, _npcX = 0, _npcY = 0, _npcZ = 0,
	_npcHeading = 0, _rewardId = 0, _rewardAmount = 0, _minlvl = 0, _maxlvl = 0, _joinTime = 0, _eventTime = 0,
	_minPlayers = 0, _maxPlayers = 0, _flagX = 0, _flagY = 0, _flagZ = 0, _topScore = 0, eventCenterX = 0,
	eventCenterY = 0, eventCenterZ = 0;
	public static FastList<Integer>			_doors					= new FastList<Integer>();
	public static String					_eventName, _eventDesc,
	_joiningLocationName, _topTeamName;
	public static FastList<Integer>			_teamPlayersCount		= new FastList<Integer>(),
	_teamColors = new FastList<Integer>(2), _teamsX = new FastList<Integer>(2), _teamsY = new FastList<Integer>(2),
	_teamsZ = new FastList<Integer>(2), _teamPointsCount = new FastList<Integer>(2);
	public static FastList<String>			_teams					= new FastList<String>(2),
	_savePlayerTeams = new FastList<String>();
	public static FastList<Integer>			_savePlayers			= new FastList<Integer>();
	public static FastList<L2PcInstance>	_players				= new FastList<L2PcInstance>(),
	_playersShuffle = new FastList<L2PcInstance>();
	public static L2Spawn					_npcSpawn, _flagSpawn;
	public static ScheduledFuture<?>		_FoSTimer				= null;
	private static int						_FoSTimerMinutes		= 0;
	private static FastList<L2PcInstance>	earlyBirdPlayers		= new FastList<L2PcInstance>();
	public static String					_startType				= "assault";
	public static int						_sealersPerSide			= 3;
	public static String					_sortType				= "fame";
	public static double					_defenderRespawnMulti	= 2;
	public static FastList<Integer>			_team1Sealers;
	public static FastList<Integer>			_team2Sealers;
	private static int						_top1Kills;
	private static int						_top2Kills;
	private static int						_top3Kills;
	public static boolean					_running				= false;
	public static int						SIEGE_EVENT_INSTANCE_ID	= 1;
	
	FOS()
	{
		cleanFos();
	}
	
	public static void cleanFos()
	{
		healDoors();
		closeDoors();
		if (_FoSTimer != null)
		{
			_FoSTimer.cancel(true);
			_FoSTimer = null;
		}
		_FoSTimerMinutes = 0;
		eventCenterX = 0;
		eventCenterY = 0;
		eventCenterZ = 0;
		_topTeamName = new String();
		_eventName = new String();
		_eventDesc = new String();
		_joiningLocationName = new String();
		_teams = new FastList<String>();
		_teams.add("");
		_teams.add("");
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		_team1Sealers = new FastList<Integer>();
		_team2Sealers = new FastList<Integer>();
		if (_players != null && !_players.isEmpty())
			for (L2PcInstance player : _players)
			{
				if (player == null)
					continue;
				player._countFOSKills = 0;
				player._countFOSCaps = 0;
				player._countFOSdies = 0;
				player._inEventFOS = false;
				removeSealSkill(player);
				if (player.getKarma() == player._originalKarmaFOS)
				{}
				else
					player.setKarma(player._originalKarmaFOS);
				player.setSiegeState((byte) 0);
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				for (L2PcInstance p : _players)
					p.sendPacket(new RelationChanged(player, player.getRelation(p), player.isAutoAttackable(p)));
				player.broadcastUserInfo();
			}
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null)
					continue;
				player._countFOSKills = 0;
				player._countFOSCaps = 0;
				player._countFOSdies = 0;
				player._inEventFOS = false;
			}
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_teamPlayersCount = new FastList<Integer>();
		_teamPlayersCount.add(0);
		_teamPlayersCount.add(0);
		_teamPointsCount = new FastList<Integer>();
		_teamPointsCount.add(0);
		_teamPointsCount.add(0);
		_teamColors = new FastList<Integer>();
		_teamColors.add(0xffffff);
		_teamColors.add(0xffffff);
		_teamsX = new FastList<Integer>();
		_teamsX.add(0);
		_teamsX.add(0);
		_teamsY = new FastList<Integer>();
		_teamsY.add(0);
		_teamsY.add(0);
		_teamsZ = new FastList<Integer>();
		_teamsZ.add(0);
		_teamsZ.add(0);
		_flagSpawn = null;
		_flagX = 0;
		_flagY = 0;
		_flagZ = 0;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topScore = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
		EventScheduler.getInstance().startNextEvent();
	}
	
	public static void cleanFosRetail()
	{
		healDoors();
		closeDoors();
		if (_FoSTimer != null)
		{
			_FoSTimer.cancel(true);
			_FoSTimer = null;
		}
		_FoSTimerMinutes = 0;
		eventCenterX = 0;
		eventCenterY = 0;
		eventCenterZ = 0;
		_topTeamName = new String();
		_eventName = new String();
		_eventDesc = new String();
		_joiningLocationName = new String();
		_teams = new FastList<String>();
		_teams.add("");
		_teams.add("");
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		_team1Sealers = new FastList<Integer>();
		_team2Sealers = new FastList<Integer>();
		if (_players != null && !_players.isEmpty())
			for (L2PcInstance player : _players)
			{
				if (player == null)
					continue;
				player._countFOSKills = 0;
				player._countFOSCaps = 0;
				player._countFOSdies = 0;
				player._inEventFOS = false;
				removeSealSkill(player);
				if (player.getKarma() == player._originalKarmaFOS)
				{}
				else
					player.setKarma(player._originalKarmaFOS);
				player.setSiegeState((byte) 0);
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				for (L2PcInstance p : _players)
					p.sendPacket(new RelationChanged(player, player.getRelation(p), player.isAutoAttackable(p)));
				player.broadcastUserInfo();
			}
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null)
					continue;
				player._countFOSKills = 0;
				player._countFOSCaps = 0;
				player._countFOSdies = 0;
				player._inEventFOS = false;
			}
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_teamPlayersCount = new FastList<Integer>();
		_teamPlayersCount.add(0);
		_teamPlayersCount.add(0);
		_teamPointsCount = new FastList<Integer>();
		_teamPointsCount.add(0);
		_teamPointsCount.add(0);
		_teamColors = new FastList<Integer>();
		_teamColors.add(0xffffff);
		_teamColors.add(0xffffff);
		_teamsX = new FastList<Integer>();
		_teamsX.add(0);
		_teamsX.add(0);
		_teamsY = new FastList<Integer>();
		_teamsY.add(0);
		_teamsY.add(0);
		_teamsZ = new FastList<Integer>();
		_teamsZ.add(0);
		_teamsZ.add(0);
		_flagSpawn = null;
		_flagX = 0;
		_flagY = 0;
		_flagZ = 0;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topScore = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_joinTime = 0;
		_eventTime = 0;
		_minPlayers = 0;
		_maxPlayers = 0;
	}
	
	public static void autoLoadData()
	{
		AdminFOSEngine a = new AdminFOSEngine();
		a.showSiegeLoadPage(null, true);
	}
	
	public static void loadData(int id)
	{
		cleanFosRetail();
		Connection con = null;
		PreparedStatement statement;
		ResultSet rs;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from fortress_siege_event WHERE id=?");
			statement.setInt(1, id);
			rs = statement.executeQuery();
			while (rs.next())
			{
				_eventName = rs.getString("eventName");
				_eventDesc = rs.getString("eventDesc");
				_joiningLocationName = rs.getString("joiningLocation");
				_minlvl = rs.getInt("minlvl");
				_maxlvl = rs.getInt("maxlvl");
				_npcId = rs.getInt("npcId");
				_npcX = rs.getInt("npcX");
				_npcY = rs.getInt("npcY");
				_npcZ = rs.getInt("npcZ");
				_npcHeading = rs.getInt("npcHeading");
				_rewardId = rs.getInt("rewardId");
				_rewardAmount = rs.getInt("rewardAmount");
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
				eventCenterX = rs.getInt("centerX");
				eventCenterY = rs.getInt("centerY");
				eventCenterZ = rs.getInt("centerZ");
				_teams.set(0, rs.getString("team1Name"));
				_teamsX.set(0, rs.getInt("team1X"));
				_teamsY.set(0, rs.getInt("team1Y"));
				_teamsZ.set(0, rs.getInt("team1Z"));
				_teamColors.set(0, rs.getInt("team1Color"));
				_teams.set(1, rs.getString("team2Name"));
				_teamsX.set(1, rs.getInt("team2X"));
				_teamsY.set(1, rs.getInt("team2Y"));
				_teamsZ.set(1, rs.getInt("team2Z"));
				_teamColors.set(1, rs.getInt("team2Color"));
				_flagX = rs.getInt("flagX");
				_flagY = rs.getInt("flagY");
				_flagZ = rs.getInt("flagZ");
				_doors.clear();
				String doorlist = rs.getString("doors");
				if (doorlist != null)
				{
					final StringTokenizer st = new StringTokenizer(doorlist, ";");
					while (st.hasMoreTokens())
					{
						int doorId = 0;
						try
						{
							doorId = Integer.parseInt(st.nextToken());
						}
						catch (Exception e)
						{
							e.printStackTrace();
							doorId = 0;
						}
						if (doorId != 0)
							_doors.addLast(doorId);
					}
				}
				_startType = rs.getString("starttype");
				_sealersPerSide = rs.getInt("sealers");
				_sortType = rs.getString("sorttype");
				_defenderRespawnMulti = rs.getDouble("respawnmulti");
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Exception: FOS.loadData(): " + e.toString());
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
	
	public static void saveData()
	{
		PreparedStatement statement;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO fortress_siege_event (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, joinTime, eventTime, minPlayers, maxPlayers, centerX, centerY, centerZ, team1Name, team1X, team1Y, team1Z, team1Color, team2Name, team2X, team2Y, team2Z, team2Color, flagX, flagY, flagZ, doors, starttype, sealers, sorttype, respawnmulti)" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, _eventName);
			statement.setString(2, _eventDesc);
			statement.setString(3, _joiningLocationName);
			statement.setInt(4, _minlvl);
			statement.setInt(5, _maxlvl);
			statement.setInt(6, _npcId);
			statement.setInt(7, _npcX);
			statement.setInt(8, _npcY);
			statement.setInt(9, _npcZ);
			statement.setInt(10, _npcHeading);
			statement.setInt(11, _rewardId);
			statement.setInt(12, _rewardAmount);
			statement.setInt(13, _joinTime);
			statement.setInt(14, _eventTime);
			statement.setInt(15, _minPlayers);
			statement.setInt(16, _maxPlayers);
			statement.setInt(17, eventCenterX);
			statement.setInt(18, eventCenterY);
			statement.setInt(19, eventCenterZ);
			statement.setString(20, _teams.get(0));
			statement.setInt(21, _teamsX.get(0));
			statement.setInt(22, _teamsY.get(0));
			statement.setInt(23, _teamsZ.get(0));
			statement.setInt(24, _teamColors.get(0));
			statement.setString(25, _teams.get(1));
			statement.setInt(26, _teamsX.get(1));
			statement.setInt(27, _teamsY.get(1));
			statement.setInt(28, _teamsZ.get(1));
			statement.setInt(29, _teamColors.get(1));
			statement.setInt(30, _flagX);
			statement.setInt(31, _flagY);
			statement.setInt(32, _flagZ);
			String doors = "";
			for (Integer door : _doors)
			{
				doors = doors.concat(door + ";");
			}
			statement.setString(33, doors);
			statement.setString(34, _startType);
			statement.setInt(35, _sealersPerSide);
			statement.setString(36, _sortType);
			statement.setDouble(37, _defenderRespawnMulti);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.config("Exception: FOS.saveData(): " + e.getMessage());
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
	
	public static boolean checkMinLevel(int minlvl)
	{
		return (_maxlvl >= minlvl);
	}
	
	public static boolean checkMaxLevel(int maxlvl)
	{
		return (_minlvl <= maxlvl);
	}
	
	public static void setTeamPos(String teamName, L2PcInstance activeChar)
	{
		int index = _teams.indexOf(teamName);
		if (index > -1)
		{
			_teamsX.set(index, activeChar.getX());
			_teamsY.set(index, activeChar.getY());
			_teamsZ.set(index, activeChar.getZ());
		}
		else
			activeChar.sendMessage("No such team name.");
	}
	
	public static void setTeamColor(String teamName, int color)
	{
		int index = _teams.indexOf(teamName);
		if (index > -1)
			_teamColors.set(index, color);
	}
	
	public static void dumpData()
	{
		_log.info("");
		if (!_joining && !_teleport && !_started)
		{
			_log.info("<<--------------------------------------------->>");
			_log.info(">> Fortress Siege Engine infos dump (INACTIVE) <<");
			_log.info("<<-----^-------^^--------^-------^^------^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			_log.info("<<-------------------------------------------->>");
			_log.info(">> Fortress Siege Engine infos dump (JOINING) <<");
			_log.info("<<----^-------^^--------^-------^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			_log.info("<<--------------------------------------------->>");
			_log.info(">> Fortress Siege Engine infos dump (TELEPORT) <<");
			_log.info("<<-----^-------^^--------^-------^^------^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			_log.info("<<-------------------------------------------->>");
			_log.info(">> Fortress Siege Engine infos dump (STARTED) <<");
			_log.info("<<----^-------^^--------^-------^^------^----->>");
		}
		_log.info("Fortress Name: " + _eventName);
		_log.info("Description  : " + _eventDesc);
		_log.info("Join location: " + _joiningLocationName);
		_log.info("Minimum level: " + _minlvl);
		_log.info("Maximum level: " + _maxlvl);
		_log.info("");
		_log.info("##########################");
		_log.info("# _teams(FastList<String>) #");
		_log.info("##########################");
		for (String team : _teams)
			_log.info(team + " Siege Flags Taken :" + _teamPointsCount.get(_teams.indexOf(team)));
		if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
		{
			_log.info("");
			_log.info("#########################################");
			_log.info("# _playersShuffle(FastList<L2PcInstance>) #");
			_log.info("#########################################");
			for (L2PcInstance player : _playersShuffle)
				if (player != null)
					_log.info("Name: " + player.getName());
		}
		_log.info("");
		_log.info("##################################");
		_log.info("# _players(FastList<L2PcInstance>) #");
		_log.info("##################################");
		for (L2PcInstance player : _players)
			if (player != null)
				_log.info("Name: " + player.getName() + "   Team: " + player._teamNameFOS);
		_log.info("");
		_log.info("#####################################################################");
		_log.info("# _savePlayers(FastList<String>) and _savePlayerTeams(FastList<String>) #");
		_log.info("#####################################################################");
		/*
		 * for (String player : _savePlayers)
		 * _log.info("Name: " + player + "    Team: " + _savePlayerTeams.get(_savePlayers.indexOf(player)));
		 */
		_log.info("");
		_log.info("");
		System.out.println("**********==Fortress Siege==************");
		System.out.println("FOS._teamPointsCount:" + _teamPointsCount.toString());
		System.out.println("FOS._flagSpawn:  " + _flagSpawn.toString());
		System.out.println("FOS._flagsX:     " + _flagX);
		System.out.println("FOS._flagsY:     " + _flagY);
		System.out.println("FOS._flagsZ:     " + _flagZ);
		System.out.println("************EOF**************\n");
		System.out.println("");
	}
	
	public static boolean startJoinOk()
	{
		if (_started || _teleport || _joining || _teams.size() > 2 || _teams.size() < 1 || _eventName.equalsIgnoreCase("") || _joiningLocationName.equals("") || _eventDesc.equals("") || _npcId == 0 || _flagX == 0 || _flagY == 0 || _flagZ == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0 || _teamsX.contains(0) || _teamsY.contains(0) || _teamsZ.contains(0))
			return false;
		return true;
	}
	
	public static void Announcements(boolean toall, String announce)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "Siege Event: " + announce);
		if (toall)
		{
			for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				if (player != null)
					if (player.isOnline() != 0)
						player.sendPacket(cs);
			}
		}
		else
		{
			if (_players != null && !_players.isEmpty())
				for (L2PcInstance player : _players)
				{
					if (player != null)
						if (player.isOnline() != 0)
							player.sendPacket(cs);
				}
		}
	}
	
	public static void Announcements(String announce)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "Siege Event: " + announce);
		if (!_started && !_teleport)
			for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				if (player != null)
					if (player.isOnline() != 0)
						player.sendPacket(cs);
			}
		else
		{
			if (_players != null && !_players.isEmpty())
				for (L2PcInstance player : _players)
				{
					if (player != null)
						if (player.isOnline() != 0)
							player.sendPacket(cs);
				}
		}
	}
	
	private static void spawnEventNpc()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(6);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().setIsInvul(true);
			_npcSpawn.getLastSpawn()._isEventMobFOS = true;
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("Fortress Siege Engine[spawnEventNpc(exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;
		_npcSpawn.stopRespawn();
		_npcSpawn.getLastSpawn().deleteMe();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
		_npcSpawn = null;
	}
	
	public static boolean checkMaxPlayers(int players)
	{
		return (_maxPlayers >= players);
	}
	
	public static boolean checkMinPlayers(int players)
	{
		return (_minPlayers <= players);
	}
	
	public static void showArtifactHtml(L2PcInstance eventPlayer, String objectId)
	{
		if (eventPlayer == null)
			return;
		try
		{
			eventPlayer.eventTicker = 0;
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><title>Sacred Artifact</title><head><body><center>");
			replyMSG.append("<br>");
			replyMSG.append("<font color=\"00FF00\">" + _eventName + " Artifact</font><br1>");
			if (eventPlayer._teamNameFOS != null && eventPlayer._teamNameFOS.equals(_teams.get(1)))
				replyMSG.append("<font color=\"LEVEL\">This is your Sacred Artifact. Defend it!</font><br1>");
			else
			{
				if (_team1Sealers.contains(eventPlayer.getObjectId()) || _team2Sealers.contains(eventPlayer.getObjectId()))
					replyMSG.append("<font color=\"LEVEL\">Use the Seal Of Ruler Skill to capture this artifact!</font><br1>");
				else
					replyMSG.append("<font color=\"LEVEL\">Help your Siege Leaders to capture this artifact!</font><br1>");
			}
			if (!_started)
				replyMSG.append("The Siege is not in progress yet.<br>Wait for a Admin/GM to start the event.<br>");
			replyMSG.append("</center></body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			System.out.println("FOS Engine[showArtifactHtml(" + eventPlayer.getName() + ", " + objectId + ")]: exception: " + e.getStackTrace());
		}
	}
	
	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><title>Siege Event Engine by Khalifa</title><body>");
			replyMSG.append("Current event...<br1>");
			replyMSG.append("    ... name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font><br1>");
			replyMSG.append("    ... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br>");
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm starts the participation.</center>");
			else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!_started)
				{
					replyMSG.append("Currently participated : <font color=\"00FF00\">" + _playersShuffle.size() + ".</font><br>");
					replyMSG.append("Admin set max players : <font color=\"00FF00\">" + _maxPlayers + "</font><br><br>");
					replyMSG.append("<font color=\"FFFF00\">You can't participate in this event.</font><br>");
				}
			}
			else if (eventPlayer.isCursedWeaponEquipped() && !Config.FortressSiege_JOIN_CURSED)
				replyMSG.append("<font color=\"FFFF00\">You can't participate in this event with a cursed weapon.</font><br>");
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() < _maxlvl)
			{
				if (_players.contains(eventPlayer) || _playersShuffle.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
				{
					if (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
						replyMSG.append("You participated already in team <font color=\"LEVEL\">" + eventPlayer._teamNameFOS + "</font><br><br>");
					else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
						replyMSG.append("You participated already!<br><br>");
					replyMSG.append("<table border=\"0\"><tr>");
					replyMSG.append("<td width=\"200\">Wait until the event starts or</td>");
					replyMSG.append("<td width=\"60\"><center><button value=\"Remove\" action=\"bypass -h npc_" + objectId + "_fos_player_leave\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></td>");
					replyMSG.append("<td width=\"100\">your participation!</td>");
					replyMSG.append("</tr></table>");
				}
				else
				{
					replyMSG.append("You want to participate in the event?<br>");
					replyMSG.append("<td width=\"200\">Minimum level : <font color=\"00FF00\">" + _minlvl + "</font></td><br>");
					replyMSG.append("<td width=\"200\">Maximum level : <font color=\"00FF00\">" + _maxlvl + "</font></td><br><br>");
					if (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
					{
						replyMSG.append("<center><table border=\"0\">");
						for (String team : _teams)
						{
							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined)</td>");
							replyMSG.append("<td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_fos_player_join " + team + "\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
						}
						replyMSG.append("</table></center>");
					}
					else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
					{
						replyMSG.append("<center><table border=\"0\">");
						for (String team : _teams)
							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font></td>");
						replyMSG.append("</table></center><br>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_fos_player_join eventShuffle\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
						replyMSG.append("Teams will be generated randomly!");
					}
					else if (Config.FortressSiege_EVEN_TEAMS.equals("RACIAL"))
					{
						replyMSG.append("<center><table border=\"0\">");
						for (String team : _teams)
							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font></td>");
						replyMSG.append("</table></center><br>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_fos_player_join eventRacial\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
						replyMSG.append("Humans + Elves + Dwarves VS Dark Elves + Orcs + Kamaels!");
					}
				}
				replyMSG.append("<center><table border=\"0\">");
				String reward = "";
				reward = reward.concat(String.valueOf(_rewardAmount));
				if (ItemTable.getInstance().getTemplate(_rewardId) != null)
					reward = reward + " " + ItemTable.getInstance().getTemplate(FOS._rewardId).getName();
				else
					reward = reward + " (unknown)";
				replyMSG.append("<tr><td width=\"200\">Reward: <font color=\"00FF00\">" + reward + "</font></td></tr>");
				replyMSG.append("<tr><td width=\"200\">Event Duration: <font color=\"00FF00\">" + _eventTime + "</font></td></tr>");
				replyMSG.append("<tr><td width=\"200\">Leaders/side: <font color=\"00FF00\">" + _sealersPerSide + "</font></td></tr>");
				replyMSG.append("<tr><td width=\"200\">Leader choose factor: <font color=\"00FF00\">" + getSortType() + "</font></td></tr>");
				replyMSG.append("<tr><td width=\"200\">Event mode: <font color=\"00FF00\">" + _startType + "</font></td></tr>");
				String respawn = "Attackers: " + (int) (Config.FortressSiege_REVIVE_DELAY / 1000) + " sec" + " - Defenders: " + (int) (FOS._defenderRespawnMulti * Config.FortressSiege_REVIVE_DELAY / 1000) + " sec";
				replyMSG.append("<tr><td width=\"200\">Respawn time: <font color=\"00FF00\">" + respawn + "</font></td></tr>");
				replyMSG.append("<tr><td width=\"200\">Use command channel chat (~) to chat to teammates during event</td></tr>");
				replyMSG.append("</table></center>");
			}
			else if (_started && !_joining)
				replyMSG.append("<center>This siege event has already begun.</center>");
			else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
			{
				replyMSG.append("You are level : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
				replyMSG.append("Admin set Minimum level : <font color=\"00FF00\">" + _minlvl + "</font><br>");
				replyMSG.append("Admin set Maximum level : <font color=\"00FF00\">" + _maxlvl + "</font><br><br>");
				replyMSG.append("<font color=\"FFFF00\">You can not participate in this event. How did this happen? It's almost impossible to be lower than the min level!</font><br>");
			}
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.severe("FOS Engine[showEventHtml(" + eventPlayer.getName() + ", " + objectId + ")]: exception" + e.getMessage());
		}
	}
	
	public static boolean checkShufflePlayers(L2PcInstance eventPlayer)
	{
		try
		{
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null || player.isOnline() == 0)
				{
					_playersShuffle.remove(player);
					continue;
				}
				else if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer._inEventFOS = true;
					eventPlayer._countFOSKills = 0;
					eventPlayer._countFOSCaps = 0;
					eventPlayer._countFOSdies = 0;
					return true;
				}
				// Just incase a player got a new objectid after DC or reconnect
				else if (player.getName().equals(eventPlayer.getName()))
				{
					_playersShuffle.remove(player);
					_playersShuffle.add(eventPlayer);
					eventPlayer._inEventFOS = true;
					eventPlayer._countFOSKills = 0;
					eventPlayer._countFOSCaps = 0;
					eventPlayer._countFOSdies = 0;
					return true;
				}
			}
		}
		catch (Throwable t)
		{
			System.out.println("Error: FOS.checkShufflePlayers: " + t.toString());
		}
		eventPlayer._inEventFOS = false;
		return false;
	}
	
	public static int teamPointsCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		return (index == -1) ? -1 : _teamPointsCount.get(index);
	}
	
	public static void setTeamPointsCount(String teamName, int teamPointCount)
	{
		int index = _teams.indexOf(teamName);
		if (index > -1)
			_teamPointsCount.set(index, teamPointCount);
	}
	
	public static int teamPlayersCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		return (index == -1) ? -1 : _teamPlayersCount.get(index);
	}
	
	public static void setTeamPlayersCount(String teamName, int teamPlayersCount)
	{
		int index = _teams.indexOf(teamName);
		if (index > -1)
			_teamPlayersCount.set(index, teamPlayersCount);
	}
	
	public static void addPlayer(L2PcInstance player, String teamName)
	{
		if (!_joining || !addPlayerOk(teamName, player))
			return;
		if (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
		{
			player._teamNameFOS = teamName;
			_players.add(player);
			setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
		}
		else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
			_playersShuffle.add(player);
		player._inEventFOS = true;
		player._countFOSKills = 0;
		player._countFOSCaps = 0;
		player._countFOSdies = 0;
		if (earlyBirdPlayers.size() < 14)
		{
			earlyBirdPlayers.add(player);
			player.sendMessage("Since you're one of the first 15 people to join this event, you'll be awarded 25% more reward than the others!");
		}
		if (player.getPet() != null)
			player.getPet().unSummon(player);
	}
	
	public static boolean addPlayerOk(String teamName, L2PcInstance eventPlayer)
	{
		try
		{
			if (teamName == null || eventPlayer == null)
				return false;
			if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventFOS)
			{
				eventPlayer.sendMessage("You already participated in this event!");
				return false;
			}
			if (Olympiad.getInstance().isRegistered(eventPlayer))
			{
				eventPlayer.sendMessage("You're registered in the Olympiad");
				return false;
			}
			if (eventPlayer._inEventTvT || eventPlayer._inEventCTF || eventPlayer._inEventDM || eventPlayer._inEventVIP)
			{
				eventPlayer.sendMessage("You already participated in another event!");
				return false;
			}
			for (L2PcInstance player : (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE")) ? _players : _playersShuffle)
			{
				if (player == null || eventPlayer == null)
					continue;
				if (eventPlayer instanceof Ghost || eventPlayer instanceof Ghost)
					continue;
				String HWID = player.getClient().getFullHwid();
				String HWID2 = eventPlayer.getClient().getFullHwid();
				if (HWID == null || HWID2 == null)
					continue;
				if (HWID.equalsIgnoreCase(HWID2))
				{
					eventPlayer.sendMessage("There is already a player with the same HWID participating in the event.");
					return false;
				}
			}
			for (L2PcInstance player : _players)
			{
				if (player.getObjectId() == eventPlayer.getObjectId() || player.getName().equals(eventPlayer.getName()))
				{
					eventPlayer.sendMessage("You already participated in this event!");
					return false;
				}
			}
			if (_players.contains(eventPlayer))
			{
				eventPlayer.sendMessage("You already participated in this event!");
				return false;
			}
			if (TvT._savePlayers.contains(eventPlayer.getObjectId()) || CTF._savePlayers.contains(eventPlayer.getObjectId()))
			{
				eventPlayer.sendMessage("You already participated in another event!");
				return false;
			}
		}
		catch (Exception e)
		{
			_log.warning("Siege Event Engine exception: " + e.getMessage());
		}
		if (Config.FortressSiege_EVEN_TEAMS.equals("NO"))
			return true;
		else if (Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
		{
			boolean allTeamsEqual = true;
			int countBefore = -1;
			for (int playersCount : _teamPlayersCount)
			{
				if (countBefore == -1)
					countBefore = playersCount;
				if (countBefore != playersCount)
				{
					allTeamsEqual = false;
					break;
				}
				countBefore = playersCount;
			}
			if (allTeamsEqual)
				return true;
			countBefore = Integer.MAX_VALUE;
			for (int teamPlayerCount : _teamPlayersCount)
			{
				if (teamPlayerCount < countBefore)
					countBefore = teamPlayerCount;
			}
			FastList<String> joinableTeams = new FastList<String>();
			for (String team : _teams)
			{
				if (teamPlayersCount(team) == countBefore)
					joinableTeams.add(team);
			}
			if (joinableTeams.contains(teamName))
				return true;
		}
		else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
			return true;
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	public static void removePlayer(L2PcInstance player)
	{
		if (!_joining)
		{
			player.setKarma(player._originalKarmaFOS);
			player.broadcastUserInfo();
			final int index = _savePlayers.indexOf(player.getObjectId());
			if (index >= 0)
				_savePlayerTeams.remove(index);
			_savePlayers.remove((Integer) player.getObjectId());
		}
		player._teamNameFOS = new String();
		player._countFOSKills = 0;
		player._countFOSCaps = 0;
		player._countFOSdies = 0;
		player._inEventFOS = false;
		player.setInstanceId(0);
		_players.remove(player);
		_playersShuffle.remove(player);
	}
	
	public static void removeOfflinePlayers()
	{
		try
		{
			if (_playersShuffle == null || _playersShuffle.isEmpty())
				return;
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null || player.isOnline() == 0 || player.isInJail())
					removePlayer(player);
				if (_playersShuffle.size() == 0 || _playersShuffle.isEmpty())
					break;
			}
		}
		catch (Throwable t)
		{}
	}
	
	public static void setUserData()
	{
		for (L2PcInstance player : _players)
		{
			if (player == null)
				continue;
			if (player.isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
			player.setKarma(0);
			player.broadcastUserInfo();
		}
	}
	
	public static void setTitleSiegeFlags(L2PcInstance player)
	{
		if (player == null)
			return;
		if (player._teamNameFOS.equals(_teams.get(0)))
		{ // attacking team = attackers siege flag sign
			player.setSiegeState((byte) 1);
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));
			for (L2PcInstance p : _players)
			{
				if (p.getObjectId() != player.getObjectId())
					p.sendPacket(new RelationChanged(player, player.getRelation(p), player.isAutoAttackable(p)));
			}
		}
		else if (player._teamNameFOS.equals(_teams.get(1)))
		{ // defending team = defender siege flag sign
			player.setSiegeState((byte) 2);
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));
			for (L2PcInstance p : _players)
			{
				if (p.getObjectId() != player.getObjectId())
					p.sendPacket(new RelationChanged(player, player.getRelation(p), player.isAutoAttackable(p)));
			}
		}
		else
		{
			_log.config("LOL wtf fort siege title siege flags is else");
		}
	}
	
	public static boolean checkIfOkToCastSealOfRule(L2PcInstance player)
	{
		if (!_started)
			return false;
		if (player.getLevel() < _minlvl)
			return false;
		if (player.getTarget() instanceof L2NpcInstance && ((L2NpcInstance) player.getTarget())._isFOS_Artifact && player._inEventFOS && player._teamNameFOS.equals(_teams.get(0)))
		{
			if (_team1Sealers.contains(player.getObjectId()) || _team2Sealers.contains(player.getObjectId()))
				return true;
		}
		return false;
	}
	
	public static void shuffleTeams()
	{
		int teamCount = 0;
		_teamPlayersCount.set(0, 0);
		_teamPlayersCount.set(1, 0);
		while (true)
		{
			if (_playersShuffle.isEmpty() || _playersShuffle == null)
				break;
			int randomIndex = Rnd.nextInt(_playersShuffle.size());
			L2PcInstance player = _playersShuffle.get(randomIndex);
			if (player.isOnline() == 0)
			{
				_playersShuffle.remove(randomIndex);
				continue;
			}
			player._originalKarmaFOS = player.getKarma();
			player._teamNameFOS = _teams.get(teamCount);
			_players.add(player);
			_playersShuffle.remove(randomIndex);
			_savePlayers.add(player.getObjectId());
			_savePlayerTeams.add(_teams.get(teamCount));
			_teamPlayersCount.set(teamCount, _teamPlayersCount.get(teamCount) + 1);
			if (teamCount == (_teams.size() - 1))
				teamCount = 0;
			else
				teamCount++;
		}
		// Since we add same IPs to same teams this may cause the teams to be uneven in numbers.
		// so we shift amount of players until the teams are even.
		while (_teamPlayersCount.get(0) > _teamPlayersCount.get(1) + 1)
			movePlayerFromTeamToTeam(0, 1);
		while (_teamPlayersCount.get(1) > _teamPlayersCount.get(0) + 1)
		{
			movePlayerFromTeamToTeam(1, 0);
		}
		/*
		 * int healers1 = 0;
		 * int healers2 = 0;
		 * FastList<Integer> healers1Index = new FastList<Integer>();
		 * FastList<Integer> healers2Index = new FastList<Integer>();
		 * for (L2PcInstance player : _players)
		 * {
		 * if(player == null)
		 * continue;
		 * if(player.getActiveClass() == 97){
		 * if(player._teamNameFOS.equalsIgnoreCase( _teams.get(0))){
		 * healers1++;
		 * healers1Index.add(_players.indexOf(player));
		 * }
		 * if(player._teamNameFOS.equalsIgnoreCase( _teams.get(1))){
		 * healers2++;
		 * healers2Index.add(_players.indexOf(player));
		 * }
		 * }
		 * }
		 * if(healers1 + healers2 > 1){
		 * if((healers1 + healers2) % 2 == 0){
		 * if(healers1 > healers2)
		 * {
		 * int healersPool = healers1Index.size() - 1;
		 * while (healers1 != healers2){
		 * if(healersPool < 0)
		 * break;
		 * moveHealer(0,1,healers1Index.get(healersPool));
		 * healersPool--;
		 * healers1--;
		 * healers2++;
		 * }
		 * }
		 * else if (healers2 > healers1)
		 * {
		 * int healersPool = healers2Index.size() - 1;
		 * while (healers1 != healers2){
		 * if(healersPool < 0)
		 * break;
		 * moveHealer(1,0,healers2Index.get(healersPool));
		 * healersPool--;
		 * healers1++;
		 * healers2--;
		 * }
		 * }
		 * }
		 * else
		 * {
		 * if (healers1 > healers2)
		 * healers1--;
		 * else if (healers2 > healers1)
		 * healers2--;
		 * if(healers1 != healers2){
		 * if(healers1 > healers2)
		 * {
		 * int healersPool = healers1Index.size() - 1;
		 * while (healers1 != healers2){
		 * if(healersPool < 0)
		 * break;
		 * moveHealer(0,1,healers1Index.get(healersPool));
		 * healersPool--;
		 * healers1--;
		 * healers2++;
		 * }
		 * }
		 * else if (healers2 > healers1)
		 * {
		 * int healersPool = healers2Index.size() - 1;
		 * while (healers1 != healers2){
		 * if(healersPool < 0)
		 * break;
		 * moveHealer(1,0,healers2Index.get(healersPool));
		 * healersPool--;
		 * healers1++;
		 * healers2--;
		 * }
		 * }
		 * }
		 * }
		 * }
		 */
	}
	
	private static void moveHealer(int fromTeam, int toTeam, int healersIndex)
	{
		int index = healersIndex;
		L2PcInstance player = _players.get(index);
		if (player.isOnline() != 0)
		{
			player._teamNameFOS = _teams.get(toTeam);
			_savePlayerTeams.set(index, _teams.get(toTeam));
			_teamPlayersCount.set(fromTeam, _teamPlayersCount.get(fromTeam) - 1);
			_teamPlayersCount.set(toTeam, _teamPlayersCount.get(toTeam) + 1);
		}
	}
	
	private static void movePlayerFromTeamToTeam(int fromTeam, int toTeam)
	{
		int index = 0;
		for (L2PcInstance p : _players)
			if (p._teamNameFOS.equals(_teams.get(fromTeam)))
			{
				index = _players.indexOf(p);
				break;
			}
		L2PcInstance player = _players.get(index);
		player._teamNameFOS = _teams.get(toTeam);
		_savePlayerTeams.set(index, _teams.get(toTeam));
		_teamPlayersCount.set(fromTeam, _teamPlayersCount.get(fromTeam) - 1);
		_teamPlayersCount.set(toTeam, _teamPlayersCount.get(toTeam) + 1);
	}
	
	private static void checkForSameIP()
	{
		try
		{
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null || player.isOnline() == 0 || player.isInJail())
				{
					try
					{
						kickPlayerFromFos(player);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					continue;
				}
				final String playerIP = player.getIP();
				if (playerIP == null || playerIP.equalsIgnoreCase("") || playerIP.length() < 7)
				{
					kickPlayerFromFos(player);
					continue;
				}
				synchronized (_playersShuffle)
				{
					for (L2PcInstance same : _playersShuffle)
					{
						if (same == null || same.isOnline() == 0 || same.isInJail())
						{
							kickPlayerFromFos(same);
							continue;
						}
						if (same == player)
							continue; // lolz checks if it's actually the same char
						final String sameIP = same.getIP();
						if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
						{
							kickPlayerFromFos(same);
							continue;
						}
						if (sameIP.equalsIgnoreCase(playerIP))
						{
							final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
							player.sendMessage(msg);
							same.sendMessage(msg);
							kickPlayerFromFos(same);
						}
					}
				}
			}
		}
		catch (Throwable t)
		{}
	}
	
	public static void sit()
	{
		_sitForced = !_sitForced;
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				if (player.isInDuel())
					player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
				player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				if (_sitForced)
				{
					player.stopMove(null, false);
					player.abortAttack();
					player.abortCast();
					if (!player.isSitting())
						player.sitDown();
					player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(true);
				}
				else
				{
					player.setIsParalyzed(false);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					if (player.isSitting())
						player.standUp();
				}
			}
		}
	}
	
	public static int getRandomFlagId()
	{
		int[] flagId =
		{
			31508, 31509, 31541, 35514, 35515, 35322, 35323, 35469, 31512
		};
		if (_eventName.contains("Ketra"))
			return 31558;
		else if (_eventName.contains("Varka"))
			return 31560;
		else if (_eventName.contains("Saint"))
			return 31510;
		return flagId[Rnd.get(flagId.length)];
	}
	
	public static void spawnFlag()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(getRandomFlagId());
		try
		{
			tmpl.type = "L2Npc";
			_flagSpawn = new L2Spawn(tmpl);
			_flagSpawn.setLocx(_flagX);
			_flagSpawn.setLocy(_flagY);
			_flagSpawn.setLocz(_flagZ + 20);
			_flagSpawn.setAmount(1);
			_flagSpawn.setHeading(0);
			_flagSpawn.setRespawnDelay(6);
			_flagSpawn.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
			SpawnTable.getInstance().addNewSpawn(_flagSpawn, false);
			_flagSpawn.init();
			_flagSpawn.getLastSpawn()._isFOS_Artifact = true;
			_flagSpawn.getLastSpawn().setIsInvul(true);
			_flagSpawn.getLastSpawn().setTitle(_eventName);
			_flagSpawn.getLastSpawn().decayMe();
			_flagSpawn.getLastSpawn().spawnMe(_flagSpawn.getLastSpawn().getX(), _flagSpawn.getLastSpawn().getY(), _flagSpawn.getLastSpawn().getZ() + 20);
		}
		catch (Exception e)
		{
			System.out.println("Fortress Siege Engine[spawnAllFlags()]: exception: " + e.getStackTrace());
		}
	}
	
	public static void unspawnFlag()
	{
		try
		{
			if (_flagSpawn == null || _teams == null)
				return;
			_flagSpawn.getLastSpawn().deleteMe();
			_flagSpawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(_flagSpawn, true);
		}
		catch (Throwable t)
		{}
	}
	
	public static String getSortType()
	{
		String val = _sortType;
		if (_sortType == null || _sortType.equalsIgnoreCase(""))
			val = "fame";
		else if (_sortType.startsWith("inventory "))
		{
			int itemId = 0;
			try
			{
				itemId = Integer.parseInt(_sortType.substring(10));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
				itemId = 0;
			}
			if (itemId > 0)
			{
				if (ItemTable.getInstance().getTemplate(itemId) != null)
				{
					val = ItemTable.getInstance().getTemplate(itemId).getName() + "s in inventory";
				}
				else
					val = "fame";
			}
			else
				val = "fame";
		}
		return val;
	}
	
	public static boolean startEvent()
	{
		if (!startEventOk())
			return false;
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		sit();
		_started = true;
		Announcements("Each side will have a max of " + _sealersPerSide + " siege leaders to seal the artifact!");
		Announcements("The siege leaders are chosen by having the most " + getSortType() + "!");
		giveSealSkill();
		_FoSTimerMinutes = _eventTime / 2;
		_FoSTimer = ThreadPoolManager.getInstance().scheduleGeneral(new FoSTimer(), _FoSTimerMinutes * 60 * 1000);
		Announcements(_eventName + ": Let the battles begin!");
		return true;
	}
	
	private static void giveSealSkill()
	{
		int count1 = 0, count2 = 0;
		FastList<L2PcInstance> sortedPlayers = new FastList<L2PcInstance>();
		sortedPlayers.addAll(_players);
		Collections.sort(sortedPlayers, new SiegeStatComparator());
		for (L2PcInstance player : sortedPlayers)
		{
			if (player == null || player.isOnline() == 0 || !player._inEventFOS)
				continue;
			if (player._teamNameFOS.equalsIgnoreCase(FOS._teams.get(0)))
			{
				if (count1 < _sealersPerSide)
				{
					count1++;
					_team1Sealers.add(player.getObjectId());
					SiegeManager.getInstance().addSiegeSkills(player);
					if (!player.isHero())
					{
						player.setHero(true);
						player.setFakeHero(true);
						player.setFOSHero(true);
					}
					Announcements(true, player.getName() + " is chosen as a siege leader for the " + player._teamNameFOS + " team!");
					player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
				}
			}
			else
			{
				if (count2 < _sealersPerSide)
				{
					count2++;
					_team2Sealers.add(player.getObjectId());
					SiegeManager.getInstance().addSiegeSkills(player);
					if (!player.isHero())
					{
						player.setHero(true);
						player.setFakeHero(true);
						player.setFOSHero(true);
					}
					Announcements(true, player.getName() + " is chosen as a siege leader for the " + player._teamNameFOS + " team!");
					player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
				}
			}
			player.startKickFromEventTask();
			setTitleSiegeFlags(player);
		}
	}
	
	private static void removeSealSkill(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			SiegeManager.getInstance().removeSiegeSkills(player);
			player.sendSkillList();
		}
	}
	
	public static class SiegeStatComparator implements Comparator<L2PcInstance>
	{
		Stats stats = null;
		
		public SiegeStatComparator()
		{
			stats = Stats.valueOfString(_sortType);
		}
		
		public int compare(L2PcInstance p1, L2PcInstance p2)
		{
			if (stats != null)
			{
				return Double.valueOf(p2.calcStat(stats, 1, null, null)).compareTo(Double.valueOf(p1.calcStat(stats, 1, null, null)));
			}
			else
			{
				if (_sortType.equalsIgnoreCase("experience"))
					return Long.valueOf(p2.getExp()).compareTo(Long.valueOf(p1.getExp()));
				else if (_sortType.equalsIgnoreCase("pvp"))
					return Integer.valueOf(p2.getPvpKills()).compareTo(Integer.valueOf(p1.getPvpKills()));
				else if (_sortType.equalsIgnoreCase("pk"))
					return Integer.valueOf(p2.getPkKills()).compareTo(Integer.valueOf(p1.getPkKills()));
				else if (_sortType.equalsIgnoreCase("onlinetime"))
					return Long.valueOf(p2.getOnlineTime()).compareTo(Long.valueOf(p1.getOnlineTime()));
				else if (_sortType.startsWith("inventory "))
				{
					int itemId = 0;
					try
					{
						itemId = Integer.parseInt(_sortType.substring(10));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
						itemId = 0;
					}
					if (itemId > 0)
					{
						if (ItemTable.getInstance().getTemplate(itemId) != null)
						{
							return Long.valueOf(p2.getInventory().getInventoryItemCount(itemId, -1)).compareTo(Long.valueOf(p1.getInventory().getInventoryItemCount(itemId, -1)));
						}
					}
					return Integer.valueOf(p2.getFame()).compareTo(Integer.valueOf(p1.getFame()));
				}
				else
					return Integer.valueOf(p2.getFame()).compareTo(Integer.valueOf(p1.getFame()));
			}
		}
	}
	
	private static boolean startEventOk()
	{
		if (_joining || !_teleport || _started)
			return false;
		if (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
				return false;
		}
		/*
		 * else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
		 * {
		 * FastList<L2PcInstance> playersShuffleTemp = new FastList<L2PcInstance>();
		 * int loopCount = 0;
		 * loopCount = _playersShuffle.size();
		 * for (int i = 0; i < loopCount; i++)
		 * {
		 * if (_playersShuffle != null)
		 * playersShuffleTemp.add(_playersShuffle.get(i));
		 * }
		 * _playersShuffle = playersShuffleTemp;
		 * playersShuffleTemp.clear();
		 * }
		 */
		return true;
	}
	
	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
		{
			EventScheduler.getInstance().startNextEvent();
			return;
		}
		if (_joining && !_teleport && !_started)
		{
			unspawnEventNpc();
			cleanFos();
			_joining = false;
			Announcements(_eventName + ": Event aborted!");
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		unspawnEventNpc();
		unspawnFlag();
		Announcements(_eventName + ": Siege aborted!");
		teleportFinish();
	}
	
	public static void teleportFinish()
	{
		Announcements(_eventName + ": Teleport back to participation NPC in 15 seconds!");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@SuppressWarnings("unlikely-arg-type")
			public void run()
			{
				for (L2PcInstance player : _players)
				{
					if (player != null)
					{
						if (player.isOnline() != 0)
						{
							player.doRevive();
							player.setInstanceId(0);
							player.teleToLocation(_npcX + Rnd.get(-800, 800), _npcY + Rnd.get(-800, 800), _npcZ + 5, false);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().siegeEvPlayed++;
							}
							// if (_team1Sealers.contains(player) || _team2Sealers.contains(player))
							// {
							// if (player.isFakeHero())
							// {
							// //player.startTempHeroTask(80);
							// }
							// }
						}
						else
						{
							Connection con = null;
							try
							{
								con = L2DatabaseFactory.getInstance().getConnection();
								PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE charId=?");
								statement.setInt(1, _npcX);
								statement.setInt(2, _npcY);
								statement.setInt(3, _npcZ);
								statement.setInt(4, player.getObjectId());
								statement.execute();
								statement.close();
							}
							catch (SQLException se)
							{
								_log.severe(se.getMessage());
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
					}
				}
				_log.info("FOS: Teleport finish done.");
				cleanFos();
			}
		}, 15000);
	}
	
	public static void finishEvent()
	{
		try
		{
			if (!finishEventOk())
				return;
			_started = false;
			unspawnEventNpc();
			unspawnFlag();
			processTopTeam(); // and also divides team points to get the number of successful sieges
			if (_topScore != 0)
				playKneelAnimation(_topTeamName);
			if (Config.FortressSiege_ANNOUNCE_TEAM_STATS)
			{
				Announcements(_eventName + " Team Statistics:");
				Announcements("Team: " + _teams.get(0) + " - Successful Sieges: " + _teamPointsCount.get(0));
				Announcements("Team: " + _teams.get(1) + " - Successful Sieges: " + _teamPointsCount.get(1));
			}
			L2PcInstance[] topkiller = new L2PcInstance[3];
			_top1Kills = 0;
			_top2Kills = 0;
			_top3Kills = 0;
			synchronized (topkiller)
			{
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countFOSKills > 0 && (player._countFOSKills > _top1Kills || (player._countFOSKills >= _top1Kills && (topkiller[0] == null || topkiller[0].isOnline() == 0 || player._countFOSdies < topkiller[0]._countFOSdies))))
					{
						if (player != null)
						{
							topkiller[0] = player;
							_top1Kills = player._countFOSKills;
						}
					}
				}
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countFOSKills > 0 && player != topkiller[0] && (player._countFOSKills > _top2Kills || (player._countFOSKills >= _top2Kills && (topkiller[1] == null || topkiller[1].isOnline() == 0 || player._countFOSdies < topkiller[1]._countFOSdies))))
					{
						if (player != null)
						{
							topkiller[1] = player;
							_top2Kills = player._countFOSKills;
						}
					}
				}
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countFOSKills > 0 && player != topkiller[0] && player != topkiller[1] && (player._countFOSKills > _top3Kills || (player._countFOSKills >= _top3Kills && (topkiller[2] == null || topkiller[2].isOnline() == 0 || player._countFOSdies < topkiller[2]._countFOSdies))))
					{
						if (player != null)
						{
							topkiller[2] = player;
							_top3Kills = player._countFOSKills;
						}
					}
				}
			}
			if (topkiller[0] != null)
			{
				Announcements(true, "Top killers of this event:");
				Announcements(true, topkiller[0].getName() + " - " + topkiller[0]._teamNameFOS + " team - " + _top1Kills + " kills!");
			}
			if (topkiller[1] != null)
				Announcements(true, topkiller[1].getName() + " - " + topkiller[1]._teamNameFOS + " team - " + _top2Kills + " kills!");
			if (topkiller[2] != null)
				Announcements(true, topkiller[2].getName() + " - " + topkiller[2]._teamNameFOS + " team - " + _top3Kills + " kills!");
			teleportFinish();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
	
	private static boolean finishEventOk()
	{
		return _started;
	}
	
	public static void processTopTeam()
	{
		if (_teamPointsCount.get(0) < 2 && _teamPointsCount.get(1) < 2)
		{ // 2 loosing teams
			_teamPointsCount.set(0, 0);
			_teamPointsCount.set(1, 0);
			Announcements(_eventName + ": No successful engravings were made. It's a tie.");
			rewardTeam(null);
			return;
		}
		else
		{
			_teamPointsCount.set(0, (_teamPointsCount.get(0) / 2)); // Needed to decrease the attacker point
			_teamPointsCount.set(1, (_teamPointsCount.get(1) / 2)); // Remember that they get 2 points for each siege!
		}
		if (_teamPointsCount.get(0) > _teamPointsCount.get(1))
		{
			Announcements(_eventName + ": Team " + _teams.get(0) + " wins the match, with " + _teamPointsCount.get(0) + " successful sieges!");
			rewardTeam(_teams.get(0));
		}
		else if (_teamPointsCount.get(1) > _teamPointsCount.get(0))
		{
			Announcements(_eventName + ": Team " + _teams.get(1) + " wins the match, with " + _teamPointsCount.get(1) + " successful sieges!");
			rewardTeam(_teams.get(1));
		}
		else
		{
			Announcements(_eventName + ": Maximum Successful sieges : " + _teamPointsCount.get(0) + " Sieges! It's a tie.");
			rewardTeam(null);
		}
	}
	
	public static void rewardTeam(String teamName)
	{
		if (!ItemTable.getInstance().createDummyItem(_rewardId).isStackable() && teamName == null)
			return;
		if (teamName == null)
			Announcements(_eventName + ": The prize will be divided between both teams.");
		int rewardAmount = _rewardAmount;
		for (L2PcInstance player : _players)
		{
			if (player != null && player.isOnline() == 1 && player._inEventFOS)
			{
				if (Config.FortressSiege_PRICE_NO_KILLS || player._countFOSKills > 1 || player._countFOSCaps > 0)
				{
					if (teamName != null)
					{
						rewardAmount = _rewardAmount;
						if (player._teamNameFOS.equals(teamName))
						{
							if (earlyBirdPlayers.contains(player))
							{
								rewardAmount = (int) (_rewardAmount * 1.25);
								if (rewardAmount == _rewardAmount)
									rewardAmount++;
								player.sendMessage("You received 25% more reward for being early to the event.");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().siegeEvReg++;
								}
							}
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team did a good job. Look in your inventory for the reward.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.addItem("FOS Event: " + _eventName, TvT.MEDAL_OF_VICTORY, 2, player, true);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().siegeEvWon++;
							}
						}
						else // loser team
						{
							if (_rewardAmount == 1) // if the prize is just 1 item, then don't give it to loser team
								continue;
							rewardAmount = _rewardAmount / 2;
							if (rewardAmount < 1)
								rewardAmount = 1;
							if (earlyBirdPlayers.contains(player))
							{
								int newRewardAmount = (int) (rewardAmount * 1.25);
								;
								if (rewardAmount == newRewardAmount)
									rewardAmount++;
								else
									rewardAmount = newRewardAmount;
								player.sendMessage("You received 25% more loser's reward for being early to the event.");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().siegeEvReg++;
								}
							}
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team did not manage to capture any altars during the event. You're rewarded 1/2 the prize for trying.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.addItem("Siege Event: " + _eventName, 97000, 2, player, true);
						}
					}
					else // it's a tie
					{
						if (_rewardAmount > 1)
							rewardAmount = (_rewardAmount + 1) / 2;
						if (earlyBirdPlayers.contains(player))
						{
							int newRewardAmount = (int) (rewardAmount * 1.25);
							;
							if (rewardAmount == newRewardAmount)
								rewardAmount++;
							else
								rewardAmount = newRewardAmount;
							player.sendMessage("You received 25% more reward for being early to the event.");
						}
						NpcHtmlMessage nhm = new NpcHtmlMessage(5);
						TextBuilder replyMSG = new TextBuilder("");
						replyMSG.append("<html><body>Nobody won this event, therefore the prize is split between the teams.</body></html>");
						nhm.setHtml(replyMSG.toString());
						player.sendPacket(nhm);
						player.addItem("Siege Event: " + _eventName, 97000, 2, player, true);
					}
					player.addItem("Siege Event: " + _eventName, _rewardId, rewardAmount, player, true);
				}
			}
		}
	}
	
	public static void playKneelAnimation(String teamName)
	{
		for (L2PcInstance player : _players)
			if (player != null && player.isOnline() != 0)
			{
				if (!player._teamNameFOS.equals(teamName))
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
					/* player.broadcastPacket(new SocialAction(player.getObjectId(), 13)); */
				}
				else
				{
					/* player.broadcastPacket(new SocialAction(player.getObjectId(), 16)); */
					player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				}
			}
	}
	
	public static void doSwap()
	{
		sit();// stop everything;
		healDoors(); // restore all doors
		closeDoors(); // close all inner doors
		Announcements(_eventName + ": Teleport to team spots. In 20 seconds the Siege continues!");// announce
		// to players
		_teamPointsCount.set(0, _teamPointsCount.get(0) + 2);// give points to the attacking side
		String team = _teams.get(0);// swap teams
		_teams.set(0, _teams.get(1));
		_teams.set(1, team);
		int points = _teamPointsCount.get(0);// swap points!
		_teamPointsCount.set(0, _teamPointsCount.get(1));
		_teamPointsCount.set(1, points);
		int color = _teamColors.get(0);// swap points!
		_teamColors.set(0, _teamColors.get(1));
		_teamColors.set(1, color);
		if (_FoSTimer != null)
		{
			_FoSTimer.cancel(true);
			_FoSTimer = null;
		}
		for (L2PcInstance player : _players)
			setTitleSiegeFlags(player);
		for (L2PcInstance player : _players)
		{
			player.doRevive();
			player.setInstanceId(SIEGE_EVENT_INSTANCE_ID);
			player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{// teleport players back to
			// reverse positions
			public void run()
			{
				sit();
				if (Rnd.get(30) < 11)
					Announcements(_eventName + ": Let the sieges continue!");// announce to
				// players
				else if (Rnd.get(30) < 11)
					Announcements(_eventName + ": ...and the battles begin again!");// announce
				// to
				// players
				else
					Announcements(_eventName + ": May the best team win!");// announce to
				// players
			}
		}, 20000);
	}
	
	public static void doSwapNoPoints()
	{
		sit();// stop everything;
		healDoors(); // restore all doors
		closeDoors(); // close all inner doors
		Announcements(_eventName + ": Teleport to team spots. In 20 seconds the Siege continues!");
		// announce
		// to players
		/*
		 * _teamPointsCount.set(1, _teamPointsCount.get(1)+2);//give points to the defending side
		 */ String team = _teams.get(0);// swap teams
		_teams.set(0, _teams.get(1));
		_teams.set(1, team);
		int points = _teamPointsCount.get(0);// swap points!
		_teamPointsCount.set(0, _teamPointsCount.get(1));
		_teamPointsCount.set(1, points);
		int color = _teamColors.get(0);// swap points!
		_teamColors.set(0, _teamColors.get(1));
		_teamColors.set(1, color);
		if (_FoSTimer != null)
		{
			_FoSTimer.cancel(true);
			_FoSTimer = null;
		}
		for (L2PcInstance player : _players)
			// swap title flags
			setTitleSiegeFlags(player);
		for (L2PcInstance player : _players)
		{
			player.doRevive();
			player.setInstanceId(SIEGE_EVENT_INSTANCE_ID);
			player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{// teleport players back to
			// reverse positions
			public void run()
			{
				sit();
				if (Rnd.get(30) < 11)
					Announcements(_eventName + ": Let the sieges continue!");// announce to
				// players
				else if (Rnd.get(30) < 11)
					Announcements(_eventName + ": ...and the battles begin again!");// announce
				// to
				// players
				else
					Announcements(_eventName + ": May the best team win!");// announce to
				// players
			}
		}, 20000);
	}
	
	private static void closeDoors()
	{
		try
		{
			for (Integer doorId : _doors)
			{
				final L2DoorInstance door = InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).getDoor(doorId);
				if (door != null && !door.isAlikeDead() && door.getOpen())
				{
					door.closeMe();
				}
			}
		}
		catch (Throwable t)
		{}
	}
	
	private static void healDoors()
	{
		try
		{
			for (Integer doorId : _doors)
			{
				final L2DoorInstance door = InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).getDoor(doorId);
				if (door != null)
				{
					door.doRevive();
					door.spawnMe();
					door.healHP();
				}
			}
		}
		catch (Throwable t)
		{}
	}
	
	private static void spawnDoors()
	{
		for (Integer doorId : _doors)
		{
			InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).addDoor(doorId, false);
		}
	}
	
	public static boolean isDoorAttackable(int id, L2Character attacker)
	{
		if (!_started)
			return false;
		final L2PcInstance player = attacker.getActingPlayer();
		if (player == null || !player._inEventFOS || player._teamNameFOS == null)
			return false;
		if (player._teamNameFOS.equals(_teams.get(0)))
			return true;
		return false;
	}
	
	public static synchronized void addDisconnectedPlayer(L2PcInstance player)
	{
		if ((Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			if (Config.FortressSiege_ON_START_REMOVE_ALL_EFFECTS)
				for (L2Effect e : player.getAllEffects())
					if (e != null)
						e.exit();
			for (L2PcInstance p : _players)
			{
				if (p == null)
				{
					continue;
				}
				else if (p.getObjectId() == player.getObjectId())
				{
					player._teamNameFOS = _savePlayerTeams.get(_savePlayers.indexOf(p.getObjectId()));
					player._originalKarmaFOS = player.getKarma();
					player._inEventFOS = true;
					player._countFOSKills = p._countFOSKills;
					player._countFOSCaps = p._countFOSCaps;
					player._countFOSdies = p._countFOSdies;
					_players.remove(p); // removing old object id from FastList
					_players.add(player); // adding new objectId to FastList
					player.setKarma(0);
					if (_team1Sealers.contains(p.getObjectId()) || _team2Sealers.contains(p.getObjectId()))
					{
						SiegeManager.getInstance().addSiegeSkills(player);
						if (!player.isHero())
						{
							player.setHero(true);
							player.setFakeHero(true);
							player.setFOSHero(true);
						}
					}
					setTitleSiegeFlags(player);
					player.setIsPendingRevive(true);
					player.setInstanceId(SIEGE_EVENT_INSTANCE_ID);
					player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)));
					player.startKickFromEventTask();
				}
			}
		}
	}
	
	public static void autoEvent()
	{
		if (startJoin())
		{
			if (_joinTime > 0)
				waiter(_joinTime * 60 * 1000); // minutes for join event
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
			if (teleportStart())
			{
				waiter(30 * 1000); // 30 sec wait time untill start fight after teleported
				if (startEvent())
				{
					waiter(_eventTime * 60 * 1000); // minutes for event time
					finishEvent();
				}
			}
			else if (!teleportStart())
				abortEvent();
		}
	}
	
	public static boolean teleportStart()
	{
		if (!_joining || _started || _teleport)
			return false;
		if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE"))
		{
			removeOfflinePlayers();
			if (checkMinPlayers(_playersShuffle.size()))
			{
				shuffleTeams();
			}
			else
			{
				Announcements("Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _playersShuffle.size());
				return false;
			}
		}
		_joining = false;
		Announcements(_eventName + ": Teleport to team spot in 20 seconds!");
		setUserData();
		spawnDoors();
		spawnFlag();
		spawnBuffers();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				sit();
				for (L2PcInstance player : _players)
				{
					if (player != null)
					{
						if (Config.FortressSiege_ON_START_UNSUMMON_PET)
						{
							// Remove Summon's buffs
							if (player.getPet() != null)
							{
								L2Summon summon = player.getPet();
								summon.stopAllEffects();
								if (summon instanceof L2PetInstance)
									summon.unSummon(player);
							}
						}
						if (Config.FortressSiege_ON_START_REMOVE_ALL_EFFECTS)
							player.stopAllEffectsExceptThoseThatLastThroughDeath();
						player.leaveParty();
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)));
					}
				}
			}
		}, 20000);
		_teleport = true;
		return true;
	}
	
	public static boolean startJoin()
	{
		if (!startJoinOk())
			return false;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		Announcements(_eventName + ": Joinable in " + _joiningLocationName + "!");
		Announcements("Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		return true;
	}
	
	private static void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		while (startWaiterTime + interval > System.currentTimeMillis())
		{
			seconds--; // here because we don't want to see two time announce at the same time
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcements(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements("Siege Event: " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
							Announcements("Siege Event: " + seconds / 60 / 60 + " hour(s) till event finish!");
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 120: // 2 minutes left
					case 60: // 1 minute left
						if (_joining)
						{
							removeOfflinePlayers();
							Announcements(_eventName + ": Joinable in " + _joiningLocationName + "!");
							Announcements("Siege Event: " + seconds / 60 + " minute(s) till registration close!");
						}
						else if (_started)
							Announcements("Siege Event: " + seconds / 60 + " minute(s) till event finish!");
						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 5: // 5 seconds left
					case 4: // 4 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
							Announcements("Siege Event: " + seconds + " second(s) till registration close!");
						else if (_teleport)
							Announcements("Siege Event: " + seconds + " seconds(s) till start fight!");
						else if (_started)
							Announcements("Siege Event: " + seconds + " second(s) till event finish!");
						break;
				}
			}
			long startOneSecondWaiterStartTime = System.currentTimeMillis();
			// only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{}
			}
		}
	}
	
	public static void kickPlayerFromFos(L2PcInstance playerToKick)
	{
		if (playerToKick == null)
			return;
		if (_joining)
		{
			_playersShuffle.remove(playerToKick);
			_players.remove(playerToKick);
			playerToKick._inEventFOS = false;
			playerToKick._teamNameFOS = "";
			playerToKick._countFOSKills = 0;
			playerToKick._countFOSCaps = 0;
			playerToKick._countFOSdies = 0;
		}
		else if (_started || _teleport)
		{
			removePlayer(playerToKick);
			if (playerToKick.isOnline() != 0)
			{
				playerToKick.setKarma(playerToKick._originalKarmaFOS);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the siege event.");
				playerToKick.teleToLocation(_npcX, _npcY, _npcZ, false);
			}
		}
	}
	
	static class FoSTimer implements Runnable
	{
		public FoSTimer()
		{
			Announcements("Green team has " + _FoSTimerMinutes + " minutes to take the siege or it'll be Orange's turn!");
		}
		
		public void run()
		{
			Announcements("Green team failed to seal the artifact within " + _FoSTimerMinutes + " minutes, now it's Orange's turn.");
			doSwapNoPoints();
		}
	}
	
	public static String getDoorListString()
	{
		String doors = "";
		for (Integer door : _doors)
		{
			doors = doors.concat(door + " ");
		}
		return doors.trim();
	}
	
	private static void spawnBuffers()
	{
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(CTF.NPC_BUFFER_ID);
		int index = 0;
		for (Integer x : _teamsX)
		{
			try
			{
				final L2Spawn spawn = new L2Spawn(template);
				spawn.setLocx(x + 10);
				spawn.setLocy(_teamsY.get(index) + 10);
				spawn.setLocz(_teamsZ.get(index) + 15);
				spawn.setAmount(1);
				spawn.setHeading(0);
				spawn.setRespawnDelay(10);
				spawn.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				spawn.init();
				spawn.startUnspawnTimer(_eventTime * 60);
				index++;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	final public static void onDeath(final L2PcInstance player, final L2Character killa)
	{
		if (player == null || player.isOnline() == 0)
			return;
		long reviveDelay = Config.FortressSiege_REVIVE_DELAY;
		final boolean teleToCentral = player.getSiegeState() == 2 /* && Rnd.get(100) < 50 */;
		if (player.getSiegeState() == 2) // defender
		{
			reviveDelay *= _defenderRespawnMulti;
		}
		player.sendMessage("You will be revived and teleported to team spot in " + reviveDelay / 1000 + " seconds");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventFOS && _started)
				{
					if (player.isDead())
					{
						if (teleToCentral && eventCenterX != 0)
							player.teleToLocation(eventCenterX, eventCenterY, eventCenterZ, false);
						else
							player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, reviveDelay);
		final L2PcInstance killer = killa.getActingPlayer();
		if (killer != null)
		{
			if (killer._inEventFOS && !(killer._teamNameFOS.equals(player._teamNameFOS)))
			{
				killer.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, killer.getObjectId(), killer.getX(), killer.getY(), killer.getZ()));
				player._countFOSdies++;
				killer._countFOSKills++;
			}
		}
	}
}