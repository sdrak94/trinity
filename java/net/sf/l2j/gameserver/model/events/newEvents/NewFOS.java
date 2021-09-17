package net.sf.l2j.gameserver.model.events.newEvents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import inertia.controller.InertiaController;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import luna.custom.holder.LunaGlobalVariablesHolder;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerStatsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventRewardManager;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
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

public class NewFOS
{
	protected static final Logger			_log					= Logger.getLogger(NewFOS.class.getName());
	public static boolean					_joining				= false;
	public static boolean					_teleport				= false;
	public static boolean					_started				= false;
	public static boolean					_sitForced				= false;
	public static int						_FlagNPC				= 35062;
	public static int						_npcId					= 0;
	public static int						_npcX					= 0;
	public static int						_npcY					= 0;
	public static int						_npcZ					= 0;
	public static int						_npcHeading				= 0;
	public static int						_rewardId				= 0;
	public static int						_rewardAmount			= 0;
	public static int						_minlvl					= 0;
	public static int						_maxlvl					= 0;
	public static int						_joinTime				= 0;
	public static int						_eventTime				= 0;
	public static int						_minPlayers				= 0;
	public static int						_maxPlayers				= 0;
	public static int						_flagX					= 0;
	public static int						_flagY					= 0;
	public static int						_flagZ					= 0;
	public static int						_topScore				= 0;
	public static int						eventCenterX			= 0;
	public static int						eventCenterY			= 0;
	public static int						eventCenterZ			= 0;
	public static FastList<Location>		_team1Locs				= new FastList<Location>();
	public static FastList<Location>		_team2Locs				= new FastList<Location>();
	public static FastList<Integer>			_doors					= new FastList<Integer>();
	public static String					_eventName;
	public static String					_eventDesc;
	public static String					_joiningLocationName;
	public static String					_topTeamName;
	public static FastList<Integer>			_teamPlayersCount		= new FastList<Integer>();
	/* public static FastList<Integer> _teamColors = new FastList<Integer>(2); */
	public static FastList<Integer>			_teamsX					= new FastList<Integer>(2);
	public static FastList<Integer>			_teamsY					= new FastList<Integer>(2);
	public static FastList<Integer>			_teamsZ					= new FastList<Integer>(2);
	public static FastList<Integer>			_teamPointsCount		= new FastList<Integer>(2);
	public static FastList<String>			_teams					= new FastList<String>(2);
	public static FastList<String>			_savePlayerTeams		= new FastList<String>();
	public static FastList<Integer>			_savePlayers			= new FastList<Integer>();
	public static FastList<L2PcInstance>	_players				= new FastList<L2PcInstance>();
	public static FastList<L2PcInstance>	_playersShuffle			= new FastList<L2PcInstance>();
	public static L2Spawn					_npcSpawn;
	public static L2Spawn					_flagSpawn;
	public static ScheduledFuture<?>		_FoSTimer				= null;
	private static int						_FoSTimerMinutes		= 0;
	private static FastList<L2PcInstance>	earlyBirdPlayers		= new FastList<L2PcInstance>();
	public static String					_startType				= "assault";
	public static int						_sealersPerSide			= 1;
	public static String					_sortType				= "fame";
	public static double					_defenderRespawnMulti	= 2;
	public static FastList<Integer>			_team1Sealers;
	public static FastList<Integer>			_team2Sealers;
	private static int						_top1Kills;
	private static int						_top2Kills;
	private static int						_top3Kills;
	public static boolean					_running				= false;
	public static FastList<String>			_teamColors				= new FastList<String>();
	public static FastList<String>			_teamTColors			= new FastList<String>();
	public static int						SIEGE_EVENT_INSTANCE_ID	= 1;
	public static int						_instanceId				= 1;
	public static int						_respawnDelay			= 20000;
	public static FastList<L2PcInstance>	_topPlayers				= new FastList<L2PcInstance>();
	public static boolean					_regAll					= false;
	public static boolean					_doublePvPs					= false;
	public static FastList<L2Spawn>			_buffers				= new FastList<L2Spawn>();
	public static L2Npc _registrationNpc = null;
	public static FastList<L2FenceInstance> _fences					= new FastList<L2FenceInstance>();
	public static boolean					_cleaned				= false;
	
	NewFOS()
	{
		cleanFos();
	}
	
	public static void cleanFos()
	{
		if(_cleaned)
			return;
		healDoors();
		closeDoors();
		if (_FoSTimer != null)
		{
			_FoSTimer.cancel(true);
			_FoSTimer = null;
		}
		if (_players != null && !_players.isEmpty())
			for (L2PcInstance player : _players)
			{
				if (player == null)
					continue;
				int wins = 0;
				int loses = 1;
				int kills = 0;
				int deaths = 0;
				int score = 0;
				if (player.wonEvent)
				{
					wins = 1;
					loses = 0;
				}
				kills = player._countFOSKills;
				deaths = player._countFOSdies;
				score = player._countFOSCaps;
				EventStats.getInstance().parseStats(player, new PlayerStatsTemplate("Siege Event", 1, wins, loses, kills, deaths, score));
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
		
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_teamPlayersCount = new FastList<Integer>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		_doors.clear();
		_players.clear();
		_playersShuffle.clear();
		_teamPlayersCount.clear();
		_teamsX.clear();
		_teamsY.clear();
		_teamsZ.clear();
		_teams.clear();
		_buffers = new FastList<L2Spawn>();
		// EventScheduler.getInstance().startNextEvent();
		EventEngine.getInstance().terminateEvent();
		EventStats.getInstance().generateTopStats();
		Communicator.getInstance().resetStats();
		unSpawnFences();
		LunaGlobalVariablesHolder.getInstance().endEventDoublePvP();
		_cleaned = true;
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
		_teamColors = new FastList<String>();
		_teamColors.add("0xffffff");
		_teamColors.add("0xffffff");
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
		_regAll = false;
	}
	
	public static void loadData(int id)
	{
		_cleaned = false;
		Event e = EventsParser.getInstance().getEvents().get(id);
		_eventName = e.getName();
		_eventDesc = e.getDesc();
		_joiningLocationName = e.getJoinLocName();
		_minlvl = e.getMinLvl();
		_maxlvl = e.getMaxLvl();
		_npcId = e.getRegNpcId();
		_npcX = e.getRegNpcLoc().getX();
		_npcY = e.getRegNpcLoc().getY();
		_npcZ = e.getRegNpcLoc().getZ();
		_npcHeading = 0;
		_joinTime = e.getJoinTime();
		_eventTime = e.getEventTime();
		_minPlayers = e.getMinPl();
		_maxPlayers = e.getMaxPl();
		eventCenterX = e.getCenterLoc().getX();
		eventCenterY = e.getCenterLoc().getY();
		eventCenterZ = e.getCenterLoc().getZ();
		_sortType = e.getSortType();
		_startType = e.getStartType();
		_instanceId = e.getInstanceId();
		_respawnDelay = e.getRespawnDelay();
		_defenderRespawnMulti = e.getRespawnDelayMul();
		_flagX = e.getSiegeFlagLoc().getX();
		_flagY = e.getSiegeFlagLoc().getY();
		_flagZ = e.getSiegeFlagLoc().getZ();
		_team1Sealers = new FastList<Integer>();
		_team2Sealers = new FastList<Integer>();
		_team1Locs.clear();
		_team2Locs.clear();
		_regAll = e.getRegAll();
		_doublePvPs = e.getDoublePvPs();
		_sealersPerSide = e.getSealersPerSide();
		for (int team = 0; team < EventsParser.getInstance().getEvents().get(id).getEvTeams().size(); team++)
		{
			_teams.add(e.getEvTeams().get(team).getName());
			_teamPlayersCount.add(0);
			_teamPlayersCount.add(0);
			_teamColors.add("0");
			_teamColors.add("0");
			_teamTColors.add("0");
			_teamColors.set(team, EventsParser.getInstance().getEvents().get(id).getEvTeams().get(team).getNameColor());
			_teamTColors.set(team, EventsParser.getInstance().getEvents().get(id).getEvTeams().get(team).getTitleColor());
		}
		for (int teamLoc = 0; teamLoc < EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().size(); teamLoc++)
		{
			String teamName = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getId();
			int x = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnX();
			int y = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnY();
			int z = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnZ();
			Location teamLocation = new Location(x, y, z);
			EventsParser.getInstance().getEvents().get(id).getEvTeamLocs();
			if (teamName.equalsIgnoreCase(_teams.get(0)))
			{
				_team1Locs.add(teamLocation);
			}
			if (teamName.equalsIgnoreCase(_teams.get(1)))
			{
				_team2Locs.add(teamLocation);
			}
		}
		for (int door = 0; door < EventsParser.getInstance().getEvents().get(id).getEvDoors().size(); door++)
		{
			_doors.add(EventsParser.getInstance().getEvents().get(id).getEvDoors().get(door).getDoorId());
		}
		_teamPointsCount = new FastList<Integer>();
		_teamPointsCount.add(0);
		_teamPointsCount.add(0);
		EventRewardManager.getInstance().loadRewards(EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId()));
	}
	
	private static Location getRndTeamLoc(String teamName)
	{
		Location LocChosen = null;
		if (teamName.equalsIgnoreCase(_teams.get(0)))
		{
			if (!_team1Locs.isEmpty())
			{
				LocChosen = (_team1Locs.get(Rnd.get(_team1Locs.size())));
			}
		}
		if (teamName.equalsIgnoreCase(_teams.get(1)))
		{
			if (!_team2Locs.isEmpty())
			{
				LocChosen = (_team2Locs.get(Rnd.get(_team2Locs.size())));
			}
		}
		return LocChosen;
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
	
	public static boolean startJoinOk()
	{
		if (_started || _teleport || _joining || _teams.size() > 2 || _teams.size() < 1 || _eventName.equalsIgnoreCase("") || _joiningLocationName.equals("") || _eventDesc.equals("") || _npcId == 0 || _flagX == 0 || _flagY == 0 || _flagZ == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _team1Locs.isEmpty() || _team2Locs.isEmpty())
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
	
	private static void regAll()
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : pls)
		{
			addPlayer(p, "eventShuffle");
			p.sendMessage("ok");
			// _playersShuffle.add(p);
			// _players.add(p);
			p._inEventFOS = true;
		}
	}
	private static void spawnFences()
	{
		int eventId = EventVarHolder.getInstance().getRunningEventId();
		for (int fence = 0; fence < EventsParser.getInstance().getEvents().get(eventId).getEvFences().size(); fence++)
		{
			final L2FenceInstance fenceTemplate = FencesTable.getInstance().getFence(EventsParser.getInstance().getEvents().get(eventId).getEvFences().get(fence).getFenceId());
			try
			{
				if (fenceTemplate != null)
				{
					fenceTemplate.setInstanceId(EventsParser.getInstance().getEvents().get(eventId).getInstanceId());
					fenceTemplate.setState(2); // closed bars
					for (L2PcInstance plr : L2World.getInstance().getAllPlayers().values())
						fenceTemplate.sendInfo(plr);
				}
				_fences.add(fenceTemplate);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	private static void unSpawnFences()
	{
		int eventId = EventVarHolder.getInstance().getRunningEventId();
		for (int fence = 0; fence < EventsParser.getInstance().getEvents().get(eventId).getEvFences().size(); fence++)
		{
			final L2FenceInstance fenceTemplate = FencesTable.getInstance().getFence(EventsParser.getInstance().getEvents().get(eventId).getEvFences().get(fence).getFenceId());
			try
			{
				if (fenceTemplate != null)
				{
					fenceTemplate.setState(0); // closed bars
					for (L2PcInstance plr : L2World.getInstance().getAllPlayers().values())
						fenceTemplate.sendInfo(plr);
				}
				_fences.remove(fenceTemplate);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	private static void spawnEventNpc()
	{

		if(_regAll)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("Event:" + EventEngine.getInstance().getActiveEvent().getId() + "has regAll enabled.");
			regAll();
		}
		if(_doublePvPs)
		{
			switch(_joiningLocationName)
			{
				case "Hunter's Village":
					LunaGlobalVariablesHolder.getInstance().setDoublePvPsHunter(true);
					break;
				case "Primeval Island":
					LunaGlobalVariablesHolder.getInstance().setDoublePvPsPI(true);
					break;
				case "Gludin Village":
					LunaGlobalVariablesHolder.getInstance().setDoublePvPsGludin(true);
					LunaGlobalVariablesHolder.getInstance().setAutoFlagGludin(true);
					break;
			}
		}
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		try
		{
			L2Spawn _npcSpawn;
			_npcSpawn = new L2Spawn(tmpl);
			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(1);
			_npcSpawn.setInstanceId(0);
			
			L2Npc npc = _npcSpawn.doSpawn();
			boolean update = false;
			npc.setTitle(_eventName);
			npc._isLunaEventNpc = true;
			update = true;
			if (update)
				npc.broadcastNpcInfo();
			npc.broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
			_registrationNpc = npc;
//			_npcSpawn = new L2Spawn(tmpl);
//			_npcSpawn.setLocx(_npcX);
//			_npcSpawn.setLocy(_npcY);
//			_npcSpawn.setLocz(_npcZ);
//			_npcSpawn.setAmount(1);
//			_npcSpawn.setHeading(_npcHeading);
//			_npcSpawn.setRespawnDelay(6);
//			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
//			_npcSpawn.init();
//			_npcSpawn.getLastSpawn().setIsInvul(true);
//			_npcSpawn.getLastSpawn()._isLunaEventNpc = true;
//			//_npcSpawn.getLastSpawn()._isEventMobFOS = true;
//			_npcSpawn.getLastSpawn().setTitle(_eventName);
//			_npcSpawn.getLastSpawn().decayMe();
//			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
//			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("Fortress Siege Engine[spawnEventNpc(exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void unspawnEventNpc()
	{
		if(_registrationNpc != null)
			_registrationNpc.deleteMe();
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
				if(player.isGM())
					return true;
				if (Config.HWID_EVENTS_CHECK)
				{
					String HWID = player.getHWID();
					String HWID2 = eventPlayer.getHWID();
					if (HWID == null || HWID2 == null)
						continue;
					if (HWID.equalsIgnoreCase(HWID2))
					{
						eventPlayer.sendMessage("There is already a player with the same HWID participating in the event.");
						return false;
					}
				}
			}

			if (Config.EVENTS_LIMIT_IPS)
			{
				int totalIps = 1;
				int maxIps = Config.EVENTS_LIMIT_IPS_NUM;
				for (L2PcInstance player : _playersShuffle)
				{
					if (player == null || eventPlayer == null)
						continue;
					if (eventPlayer instanceof Ghost || eventPlayer instanceof Ghost)
						continue;
					String IP = player.getClient().getIP();
					String IP2 = eventPlayer.getClient().getIP();
					if (player.isGM() || eventPlayer.isGM())
						continue;
					if (IP == null || IP2 == null)
						continue;
					if (IP.equalsIgnoreCase(IP2))
					{
						if (totalIps < maxIps)
						{
							totalIps += 1;
						}
						else
						{
							eventPlayer.sendMessage("You have reached maximum particapating players per IP (3).");
							return false;
						}
					}
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
		player.wonEvent = false;
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
					player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(true);
				}
				else
				{
					player.setIsParalyzed(false);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
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
		if (EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId()).getSiegeFlagNpcId() != 0)
		{
			return EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId()).getSiegeFlagNpcId();
		}
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
			_flagSpawn.setInstanceId(_instanceId);
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

	public static void setPara(boolean para)
	{
		for (L2PcInstance player : _players)
		{
			if(para)
			{
				if (player != null && player.isOnline() != 0)
				{
					if (player.isTransformed())
						player.stopTransformation(null);
					if (player.isInDuel())
					player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
					player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					player.stopMove(null, false);
					player.abortAttack();
					player.abortCast();
					player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(true);
					player.setIsInvul(true);
					player.getStatus().setCurrentHp(player.getMaxHp());
					player.getStatus().setCurrentCp(player.getMaxCp());
				}
			}
			if(!para)
			{
				if (player != null && player.isOnline() != 0)
				{
					player.stopMove(null, false);
					player.abortAttack();
					player.abortCast();
					if (player.isTransformed())
						player.stopTransformation(null);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(false);
					player.setIsInvul(false);
					player.getStatus().setCurrentHp(player.getMaxHp());
					player.getStatus().setCurrentCp(player.getMaxCp());
				}
			}
		}
	}
	public static boolean startEvent()
	{
		if (!startEventOk())
			return false;
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		//sit();
		setPara(false);
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
			if (player._teamNameFOS.equalsIgnoreCase(NewFOS._teams.get(0)))
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
		return true;
	}
	

	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
		{
			//EventScheduler.getInstance().startNextEvent();
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
		setPara(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
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
							player.teleToLocation(_npcX + Rnd.get(-400, 400), _npcY + Rnd.get(-400, 400), _npcZ + 5, false);
							player.stopMove(null, false);
							player.abortAttack();
							player.abortCast();
							player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
							player.setIsParalyzed(false);
							player.setIsInvul(false);
							InertiaController.getInstance().fetchChill(player).addCredit(Config.EVENT_CREDIT);
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
				removeDoors();
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
			rewardTeam(null, true);
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
			rewardTeam(_teams.get(0), false);
		}
		else if (_teamPointsCount.get(1) > _teamPointsCount.get(0))
		{
			Announcements(_eventName + ": Team " + _teams.get(1) + " wins the match, with " + _teamPointsCount.get(1) + " successful sieges!");
			rewardTeam(_teams.get(1), false);
		}
		else
		{
			Announcements(_eventName + ": Maximum Successful sieges : " + _teamPointsCount.get(0) + " Sieges! It's a tie.");
			rewardTeam(null, true);
		}
	}
	
	public static void rewardTeam(String teamName, boolean tie)
	{
		for (L2PcInstance player : _players)
		{
			boolean earlyReg = false;
			try
			{
				if (player != null && player.isOnline() == 1 && player._inEventFOS)
				{
					if (player._countFOSKills > 0 || player._countFOSCaps > 0)
					{
						if (player._teamNameFOS.equals(teamName))
						{
							if (earlyBirdPlayers.contains(player))
							{
								earlyReg = true;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().siegeEvReg++;
								}
							}
							EventRewardManager.getInstance().rewardPlayer(1, player, earlyReg);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.wonEvent = true;
							// player.addItem("Domination Event: " + _eventName, NewDomination.MEDAL_OF_VICTORY, 2, player, true);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().siegeEvWon++;
								player.getCounters().siegeEvPlayed++;
							}
						}
						else if (teamName != null)
						{
							if (earlyBirdPlayers.contains(player))
							{
								earlyReg = true;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().siegeEvReg++;
								}
							}
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().siegeEvPlayed++;
							}
							EventRewardManager.getInstance().rewardPlayer(2, player, earlyReg);
							// player.addItem("Siege Event: " + _eventName, 97000, 2, player, true);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 1/2 of the event prize for trying.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.wonEvent = false;
						}
						else if (tie)
						{
							if (earlyBirdPlayers.contains(player))
							{
								earlyReg = true;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().siegeEvReg++;
								}
							}
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().siegeEvPlayed++;
							}
							EventRewardManager.getInstance().rewardPlayer(3, player, earlyReg);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Nobody won this event, therefore the prize is split between the teams.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.wonEvent = false;
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
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
		setPara(true);
		//sit();// stop everything;
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
		String color = _teamColors.get(0); // swap points!
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
			player.setInstanceId(_instanceId);
			player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);
			// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{// teleport players back to
			// reverse positions
			public void run()
			{
				setPara(false);
				//sit();
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
		setPara(true);
		//sit();// stop everything;
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
		String color = _teamColors.get(0); // swap points!
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
			player.setInstanceId(_instanceId);
			player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);
			// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{// teleport players back to
			// reverse positions
			public void run()
			{

				setPara(false);
				//sit();
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
			if (InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).getDoor(doorId) == null)
			{
				InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).addDoor(doorId, false);
			}
		}
	}
	
	private static void removeDoors()
	{
		for (Integer doorId : _doors)
		{
			InstanceManager.getInstance().getInstance(SIEGE_EVENT_INSTANCE_ID).removeDoor(doorId);
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

					player.sendMessage("You will be ported back to event in 5 seconds");

					player.stopMove(null, false);
					player.abortAttack();
					player.abortCast();
					player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(true);
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						public void run()
						{
							player.setInstanceId(_instanceId);
							player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);

							player.stopMove(null, false);
							player.abortAttack();
							player.abortCast();
							player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
							player.setIsParalyzed(false);
						}
					}, 5000);
					// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)));
					player.startKickFromEventTask();
				}
			}
		}
	}
	public static void telePlayerToRndTeamSpot(L2PcInstance player)
	{
		if(player.getInstanceId() != 1)
		{
			player.setInstanceId(1);
		}
		player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);
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
		setPara(true);
		spawnFences();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				//sit();
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
						if (player.getParty() != null)
						{
							player.leaveParty();
						}
						player.removeCubics();
						if (Config.FortressSiege_ON_START_REMOVE_ALL_EFFECTS)
							player.stopAllEffectsExceptThoseThatLastThroughDeath();
						player.leaveParty();
						player.doRevive();
						player.setInstanceId(_instanceId);
						player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)));
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

		Announcements(true, "========================");
		Announcements(true, "Registration for Siege Event: " + _eventName + " has been opened");
		Announcements(true, "The registration NPC is available in " + _joiningLocationName);
		if(_doublePvPs)
		Announcements(true, "Double PvPs During Event's Phase");
		Announcements(true, "Check Community Board (ALT+B) at Events Tab for more info.");
		Announcements(true, "========================");
		return true;
	}
	
	private static void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		boolean run = true;
		long interv = interval;
		boolean toBeFinished = false;
		while (startWaiterTime + interv > System.currentTimeMillis())
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
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String text = "Time: " + mins + ":" + secs + "  ";
			// Communicator
			String eventPhase = "none";
			if (_joining)
				eventPhase = "Registration";
			if (_started)
				eventPhase = "Event Started";
			if (_teleport)
				eventPhase = "Prep. Players for Battle";

			if (_started || _joining || _teleport)
			{
				Communicator.getInstance().setEventName(_eventName);
				Communicator.getInstance().setEventPhase(eventPhase);
				Communicator.getInstance().setTimeRemaining(mins + ":" + secs);
				Communicator.getInstance().setRegPlayers(_joining?_playersShuffle.size():_players.size());
			}
			if (_started)
			{
				Communicator.getInstance().setTeam1Name(_teams.get(0));
				Communicator.getInstance().setTeam2Name(_teams.get(1));
				processTopPlayerTemp();
				Communicator.getInstance().setTeam1Score("" + _teamPointsCount.get(0) / 2);
				Communicator.getInstance().setTeam2Score("" + _teamPointsCount.get(1) / 2);

				text += _teams.get(0) + ": " + _teamPointsCount.get(0) / 2 + " - " + _teams.get(1) + ": " + _teamPointsCount.get(1) / 2;
				for (L2PcInstance player : _players)
				{
					if (player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
			}
			if (!_started && !_joining && !_teleport)
			{
				cleanFos();
				run = false;
			}
			if (!run)
			{
				if(seconds > 5)
				{
					interv = 5000;
					seconds = 5;
					run = true;
					toBeFinished = true;
				}
			}
			if(toBeFinished)
			{
				GmListTable.broadcastMessageToAdvancedGMs2("Event will end in "+ seconds +" seconds.");
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
	
	public static Comparator<L2PcInstance> comparePlayersScore = new Comparator<L2PcInstance>()
	{
		@Override
		public int compare(L2PcInstance p1, L2PcInstance p2)
		{
			int score1 = p1._countFOSKills;
			int score2 = p2._countFOSKills;
			if (score1 == score2)
			{
				int deaths1 = p1._countFOSKills;
				int deaths2 = p2._countFOSKills;
				return deaths1 == deaths2 ? 0 : deaths1 < deaths2 ? -1 : 1;
			}
			else
				return score1 < score2 ? 1 : -1;
		}
	};
	
	public static void processTopPlayerTemp()
	{
		List<L2PcInstance> tempPlayers = new FastList<L2PcInstance>();
		tempPlayers.addAll(_players);
		Collections.sort(tempPlayers, comparePlayersScore);
		final int countTopPlayers = 5;
		int i = 0;
		_topPlayers.clear();
		for (L2PcInstance player : tempPlayers)
		{
			_topPlayers.add(player);
			if (i >= countTopPlayers)
				break;
		}
	}
	
	public static FastList<L2PcInstance> getTopPlayers()
	{
		return _topPlayers;
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
		int eventId = EventVarHolder.getInstance().getRunningEventId();
		for (int npc = 0; npc < EventsParser.getInstance().getEvents().get(eventId).getEvSpawns().size(); npc++)
		{
			final L2NpcTemplate template = NpcTable.getInstance().getTemplate(EventsParser.getInstance().getEvents().get(eventId).getEvSpawns().get(npc).getId());
			try
			{
				final L2Spawn spawn = new L2Spawn(template);
				spawn.setLocx(EventsParser.getInstance().getEvents().get(eventId).getEvSpawns().get(npc).getLoc().getX());
				spawn.setLocy(EventsParser.getInstance().getEvents().get(eventId).getEvSpawns().get(npc).getLoc().getY());
				spawn.setLocz(EventsParser.getInstance().getEvents().get(eventId).getEvSpawns().get(npc).getLoc().getZ());
				spawn.setAmount(1);
				spawn.setHeading(0);
				spawn.setRespawnDelay(10);
				spawn.setInstanceId(EventsParser.getInstance().getEvents().get(eventId).getInstanceId());
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				spawn.init();
				spawn.startUnspawnTimer(_eventTime * 60);
				_buffers.add(spawn);
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
		long reviveDelay = _respawnDelay;
		final boolean teleToCentral = player.getSiegeState() == 2 /* && Rnd.get(100) < 50 */;
		if (player.getSiegeState() == 2) // defender
		{
			reviveDelay *= _defenderRespawnMulti;
		}
		player.sendMessage("You will be revived and teleported to team spot in " + reviveDelay + " seconds");
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
							player.teleToLocation(getRndTeamLoc(player._teamNameFOS), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameFOS)), _teamsY.get(_teams.indexOf(player._teamNameFOS)), _teamsZ.get(_teams.indexOf(player._teamNameFOS)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, reviveDelay * 1000);
		final L2PcInstance killer = killa.getActingPlayer();
		if (killer != null)
		{
			if (killer._inEventFOS && !(killer._teamNameFOS.equals(player._teamNameFOS)))
			{
				killer.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, killer.getObjectId(), killer.getX(), killer.getY(), killer.getZ()));
				player._countFOSdies++;
				killer._countFOSKills++;
				killer.broadcastUserInfo();
			}
		}
	}
	public static void abortAndFinish()
	{
		unspawnEventNpc();
		if (_started)
		{
			unspawnBuffers();
			unspawnEventNpc();
			unspawnFlag();
			processTopTeam();
			teleportFinishFast();
			_started = false;
			// finishEventAbort();
		}
		else
			abortFinish();
	}
	private static void unspawnBuffers()
	{
		for (int i = 0; i < _buffers.size(); i++)
		{
			_buffers.get(i).stopRespawn();
			_buffers.get(i).getLastSpawn().deleteMe();
		}
	}
	public static void abortFinish()
	{
		if (!_joining && !_teleport && !_started)
		{
			cleanFos();
			return;
		}
		else if (_joining && !_teleport && !_started)
		{
			_joining = false;
			Announcements(_eventName + ": Event aborted!");
			unspawnEventNpc();
			cleanFos();
			return;
		}
		if (!_joining && _teleport || _started)
		{
			_joining = false;
			Announcements(_eventName + ": Event aborted!");
			unspawnEventNpc();
			teleportFinishFast();
			cleanFos();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
	}
	public static void teleportFinishFast()
	{
		Announcements(_eventName + ": Teleport back to participation NPC!");
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.doRevive();
					player.setKarma(0);
					player.setInstanceId(0);
					player.teleToLocation(_npcX + Rnd.get(-600, 600), _npcY + Rnd.get(-600, 600), _npcZ, false);
					player.setForceNoSpawnProtection(false);
					player.stopMove(null, false);
					player.abortAttack();
					player.abortCast();
					if (player.isTransformed())
						player.stopTransformation(null);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(false);
					player.getStatus().setCurrentHp(player.getMaxHp());
					player.getStatus().setCurrentCp(player.getMaxCp());
					player.setIsInvul(false);
					player.broadcastUserInfo();
				}
				else
				{
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
						statement.setInt(1, _npcX);
						statement.setInt(2, _npcY);
						statement.setInt(3, _npcZ);
						statement.setString(4, player.getName());
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
		removeDoors();
		cleanFos();
	}
}