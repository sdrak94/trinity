package net.sf.l2j.gameserver.model.events.newEvents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import inertia.controller.InertiaController;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import luna.custom.holder.LunaGlobalVariablesHolder;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerStatsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventRewardManager;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NicknameChanged;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

public class NewTvT
{
	protected static final Logger			_log					= Logger.getLogger(NewTvT.class.getName());
	public static String					_eventName				= "";
	public static String					_dispEventName			= "Team Vs Team";
	public static String					_eventDesc				= "";
	public static String					_topTeam				= "";
	public static String					_joiningLocationName	= "";
	public static FastList<String>			_teams					= new FastList<String>();
	public static FastList<Integer>			_savePlayers			= new FastList<Integer>();
	public static FastList<String>			_savePlayerTeams		= new FastList<String>();
	public static FastList<L2PcInstance>	_players				= new FastList<L2PcInstance>();
	public static FastList<L2PcInstance>	_playersShuffle			= new FastList<L2PcInstance>();
	public static FastList<Integer>			_teamPlayersCount		= new FastList<Integer>();
	public static FastList<Integer>			_teamKillsCount			= new FastList<Integer>();
	public static FastList<String>			_teamColors				= new FastList<String>();
	public static FastList<String>			_teamTColors			= new FastList<String>();
	public static FastList<Integer>			_teamsX					= new FastList<Integer>();
	public static FastList<Integer>			_teamsY					= new FastList<Integer>();
	public static FastList<Integer>			_teamsZ					= new FastList<Integer>();
	public static boolean					_joining				= false;
	public static boolean					_teleport				= false;
	public static boolean					_started				= false;
	public static boolean					_sitForced				= false;
	public static L2Spawn					_npcSpawn;
	public static int						_npcId					= 0;
	public static int						_npcX					= 0;
	public static int						_npcY					= 0;
	public static int						_npcZ					= 0;
	public static int						_npcHeading				= 0;
	public static int						_rewardId				= 0;
	public static int						_rewardAmount			= 0;
	public static int						_topKills				= 0;
	public static int						_top1Kills				= 0;
	public static int						_top2Kills				= 0;
	public static int						_top3Kills				= 0;
	public static int						_top1Assists			= 0;
	public static int						_top2Assists			= 0;
	public static int						_top3Assists			= 0;
	public static int						_minlvl					= 0;
	public static int						_maxlvl					= 0;
	public static int						_joinTime				= 0;
	public static int						_eventTime				= 0;										// min
	public static int						_minPlayers				= 0;
	public static int						_maxPlayers				= 0;
	public static int						_playerWon				= 0;
	public static long						lastEventTime			= 0;
	public static boolean					_sGrade					= false;
	public static boolean					_running				= false;
	public static final int					MEDAL_OF_VICTORY		= 97002;
	private static FastList<L2PcInstance>	earlyBirdPlayers		= new FastList<L2PcInstance>();
	public static FastList<Integer>			_doors					= new FastList<Integer>();
	public static FastList<Location>		_team1Locs				= new FastList<Location>();
	public static FastList<Location>		_team2Locs				= new FastList<Location>();
	private static int						_respawnDelay			= 20000;
	public static FastList<L2PcInstance>	_topPlayers				= new FastList<L2PcInstance>();
	public static boolean 					_regAll					= false;
	public static boolean					_doublePvPs					= false;
	public static FastList<L2Spawn>			_buffers				= new FastList<L2Spawn>();
	private static L2Npc _registrationNpc = null;
	public static boolean					_cleaned				= false;
	public static FastList<L2FenceInstance> _fences					= new FastList<L2FenceInstance>();
	

	public static void AnnounceToPlayers(Boolean toall, String announce)
	{
		if (toall)
			Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", "Event Engine: " + announce));
		else
		{
			CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", "Event Engine: " + announce);
			if (_players != null && !_players.isEmpty())
			{
				for (L2PcInstance player : _players)
				{
					if (player != null && player.isOnline() != 0)
						player.sendPacket(cs);
				}
			}
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
	public static void kickPlayerFromTvt(L2PcInstance playerToKick)
	{
		if (playerToKick == null)
			return;
		if (_joining)
		{
			_playersShuffle.remove(playerToKick);
			_players.remove(playerToKick);
			playerToKick._inEventTvT = false;
			playerToKick._teamNameTvT = "";
			playerToKick._countTvTkills = 0;
			playerToKick._countTvTdies = 0;
		}
		else if (_started || _teleport)
		{
			removePlayer(playerToKick);
			if (playerToKick.isOnline() != 0)
			{
				playerToKick.setKarma(playerToKick._originalKarmaTvT);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the TvT.");
				playerToKick.teleToLocation(_npcX, _npcY, _npcZ, false);
			}
		}
	}
	
	public static void setNpcPos(L2PcInstance activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
		_npcHeading = activeChar.getHeading();
	}
	
	public static void setNpcPos(int x, int y, int z)
	{
		_npcX = x;
		_npcY = y;
		_npcZ = z;
	}
	
	public static boolean checkMaxLevel(int maxlvl)
	{
		return _minlvl < maxlvl;
	}
	
	public static boolean checkMinLevel(int minlvl)
	{
		return _maxlvl > minlvl;
	}
	
	/**
	 * returns true if participated players is higher or equal then minimum
	 * needed players
	 */
	public static boolean checkMinPlayers(int players)
	{
		return _minPlayers <= players;
	}
	
	/**
	 * returns true if max players is higher or equal then participated players
	 */
	public static boolean checkMaxPlayers(int players)
	{
		return _maxPlayers > players;
	}
	
	public static void removeTeam(String teamName)
	{
		if (!checkTeamOk() || _teams.isEmpty())
			return;
		if (teamPlayersCount(teamName) > 0)
			return;
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamsZ.remove(index);
		_teamsY.remove(index);
		_teamsX.remove(index);
		_teamColors.remove(index);
		_teamKillsCount.remove(index);
		_teamPlayersCount.remove(index);
		_teams.remove(index);
	}
	
	public static void setTeamPos(String teamName, L2PcInstance activeChar)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamsX.set(index, activeChar.getX());
		_teamsY.set(index, activeChar.getY());
		_teamsZ.set(index, activeChar.getZ());
	}
	
	public static void setTeamPos(String teamName, int x, int y, int z)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamsX.set(index, x);
		_teamsY.set(index, y);
		_teamsZ.set(index, z);
	}
	
	public static boolean checkTeamOk()
	{
		return !(_started || _teleport || _joining);
	}
	
	public static void startJoin()
	{
		if (!startJoinOk())
			return;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, "Registration for Team VS Team event: " + _eventName + " has been opened");
		AnnounceToPlayers(true, "The registration NPC is available in " + _joiningLocationName);
		AnnounceToPlayers(true, "Check Community Board (ALT+B) at Events Tab for more info.");
		
		if (_sGrade)
			AnnounceToPlayers(true, "S grade only! (S grade gear auto-enchant to +25 during event)");
	}
	
	public static boolean startAutoJoin()
	{
		if (!startJoinOk())
			return false;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, "========================");
		AnnounceToPlayers(true, "Registration for Team VS Team event: " + _eventName + " has been opened");
		AnnounceToPlayers(true, "The registration NPC is available in " + _joiningLocationName);
		if(_doublePvPs)
			AnnounceToPlayers(true, "Double PvPs During Event's Phase");
		AnnounceToPlayers(true, "Check Community Board (ALT+B) at Events Tab for more info.");
		AnnounceToPlayers(true, "========================");
		if (_sGrade)
			AnnounceToPlayers(true, "S grade only! (S grade gear auto-enchant to +25 during event)");
		return true;
	}
	
	public static boolean startJoinOk()
	{
		return !(_started || _teleport || _joining || _teams.size() < 2 || _eventName.isEmpty() || _joiningLocationName.isEmpty() || _eventDesc.isEmpty() || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _team1Locs.isEmpty() || _team2Locs.isEmpty());
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
			p._inEventTvT = true;
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
			update = true;
			npc._isLunaEventNpc = true;
			if (update)
				npc.broadcastNpcInfo();
			npc.broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
			_registrationNpc  = npc;
		}
		catch (Exception e)
		{
			_log.severe("TvT Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	public static void teleportStart()
	{
		if (!_joining || _started || _teleport)
			return;
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			removeOfflinePlayers();
			shuffleTeams();
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			AnnounceToPlayers(true, "Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _playersShuffle.size());
			return;
		}
		_joining = false;
		AnnounceToPlayers(false, _eventName + "(TvT): Teleport to team spot in 10 seconds!");
		setUserData();
		setPara(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				for (L2PcInstance player : _players)
				{
					if (player != null)
					{
						// Remove Summon's buffs
						if (player.getPet() != null)
						{
							L2Summon summon = player.getPet();
							summon.stopAllEffects();
							if (summon instanceof L2PetInstance)
								summon.unSummon(player);
						}
						if (player.getParty() != null)
						{
							player.leaveParty();
						}player.removeCubics();
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(getRndTeamLoc(player._teamNameTvT), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
					}
				}
			}
		}, 10000);
		_teleport = true;
	}

	public static void telePlayerToRndTeamSpot(L2PcInstance player)
	{
		if(player.getInstanceId() != 1)
		{
			player.setInstanceId(1);
		}
		player.teleToLocation(getRndTeamLoc(player._teamNameTvT), false);
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
	
	public static boolean teleportAutoStart()
	{
		if (!_joining || _started || _teleport)
			return false;
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			removeOfflinePlayers();
			shuffleTeams();
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			AnnounceToPlayers(true, "Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _playersShuffle.size());
			return false;
		}
		_joining = false;
		AnnounceToPlayers(false, _eventName + "(TvT): Teleport to team spot in 20 seconds!");
		setUserData();
		spawnFences();
		spawnBuffers();
		setPara(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				for (L2PcInstance player : _players)
				{
					if (player != null)
					{
						// Remove Summon's buffs
						if (player.getPet() != null)
						{
							L2Summon summon = player.getPet();
							summon.stopAllEffects();
							if (summon instanceof L2PetInstance)
								summon.unSummon(player);
						}
						if (player.getParty() != null)
						{
							player.leaveParty();
						}
						player.removeCubics();
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(getRndTeamLoc(player._teamNameTvT), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
					}
				}
			}
		}, 20000);
		_teleport = true;
		return true;
	}
	
	public static void setJoinTime(int time)
	{
		_joinTime = time;
	}
	
	public static void setEventTime(int time)
	{
		_eventTime = time;
	}
	
	public static boolean startAutoEvent()
	{
		if (!startEventOk())
			return false;
		lastEventTime = System.currentTimeMillis();
		_running = true;
		_teleport = false;
		setPara(false);
		AnnounceToPlayers(false, _eventName + "(TvT): Started. Go to kill your enemies!");
		_started = true;
		for (L2PcInstance player : _players)
		{
			player.startKickFromEventTask();
		}
		return true;
	}
	
	public static void autoEvent()
	{
		if (startAutoJoin())
		{
			if (_joinTime > 0)
				waiter(_joinTime * 60 * 1000); // minutes for join event
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
			if (teleportAutoStart())
			{
				waiter(30 * 1000); // 30 sec wait time untill start fight after
									// teleported
				if (startAutoEvent())
				{
					waiter(_eventTime * 60 * 1000); // minutes for event time
					finishEvent();
				}
			}
			else if (!teleportAutoStart())
			{
				abortEvent();
			}
		}
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
			seconds--; // here because we don't want to see two time announce at
						// the same time
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							AnnounceToPlayers(true, "(TvT): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "TvT Event: " + seconds / 60 / 60 + " hour(s) till registration ends!");
						}
						else if (_started)
							AnnounceToPlayers(false, "TvT Event: " + seconds / 60 / 60 + " hour(s) till event ends!");
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
							AnnounceToPlayers(true, "(TvT): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "TvT Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
							AnnounceToPlayers(false, "TvT Event: " + seconds / 60 + " minute(s) till event ends!");
						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
							AnnounceToPlayers(true, "TvT Event: " + seconds + " second(s) till registration ends!");
						else if (_teleport)
							AnnounceToPlayers(false, "TvT Event: " + seconds + " seconds(s) till fight starts!");
						else if (_started)
							AnnounceToPlayers(false, "TvT Event: " + seconds + " second(s) till event ends!");
						break;
				}
			}
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String text = "" + mins + ":" + secs + "  ";
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
				Communicator.getInstance().setTeam1Name(_teams.get(0));
				Communicator.getInstance().setTeam2Name(_teams.get(1));
			}
			if (_started)
			{
				processTopPlayerTemp();
				Communicator.getInstance().setTeam1Score("" + teamKillsCount(_teams.get(0)));
				Communicator.getInstance().setTeam2Score("" + teamKillsCount(_teams.get(1)));
			}
			if (_started)
			{
				String team1 = _teams.get(0);
				String team2 = _teams.get(1);
				text += team1 + ": " + teamKillsCount(team1) + " - " + team2 + ": " + teamKillsCount(team2);
				for (L2PcInstance player : _players)
				{
					if (player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
			}
			if (!_started && !_joining && !_teleport)
			{
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
			// only the try catch with Thread.sleep(1000) give bad countdown on
			// high wait times
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
	
	private static boolean startEventOk()
	{
		if (_joining || !_teleport || _started)
			return false;
		if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
				return false;
		}
		/*
		 * else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE")) {
		 * FastList<L2PcInstance> playersShuffleTemp = new
		 * FastList<L2PcInstance>(); int loopCount = 0;
		 * loopCount = _playersShuffle.size();
		 * for (int i = 0; i < loopCount; i++) { if (_playersShuffle != null)
		 * playersShuffleTemp.add(_playersShuffle.get(i)); }
		 * _playersShuffle = playersShuffleTemp; playersShuffleTemp.clear();
		 * // if (_playersShuffle.size() < (_teams.size()*2)){ // return false;
		 * // } }
		 */
		return true;
	}
	
	public static void shuffleTeams()
	{
		int teamCount = 0, playersCount = 0;
		for (;;)
		{
			if (_playersShuffle.isEmpty())
				break;
			int playerToAddIndex = Rnd.nextInt(_playersShuffle.size());
			L2PcInstance player = null;
			player = _playersShuffle.get(playerToAddIndex);
			player._originalKarmaTvT = player.getKarma();
			_players.add(player);
			_players.get(playersCount)._teamNameTvT = _teams.get(teamCount);
			_savePlayers.add(_players.get(playersCount).getObjectId());
			_savePlayerTeams.add(_teams.get(teamCount));
			playersCount++;
			if (teamCount == _teams.size() - 1)
				teamCount = 0;
			else
				teamCount++;
			_playersShuffle.remove(playerToAddIndex);
		}
		int team1healers = 0;
		int team2healers = 0;
		FastList<Integer> team1healersIndex = new FastList<Integer>();
		FastList<Integer> team2healersIndex = new FastList<Integer>();
		for (L2PcInstance player : _players)
		{
			if (player == null || player.isOnline() == 0)
				continue;
			if (player.getActiveClass() == 97)
			{
				System.out.println("Healer Detected: " + player.getName());
				if (player._teamNameTvT == _teams.get(0))
				{
					System.out.println("Healer belongs to team 0");
					team1healers++;
					team1healersIndex.add(_players.indexOf(player));
				}
				else if (player._teamNameTvT == _teams.get(1))
				{
					System.out.println("Healer belongs to team 1");
					team2healers++;
					team2healersIndex.add(_players.indexOf(player));
				}
			}
			else
			{
				continue;
			}
		}
		if (team1healers + team2healers > 1 && team1healers != team2healers)
		{
			System.out.println("Healers are uneven");
			System.out.println("Healers on team 1:" + team1healers);
			System.out.println("Healers on team 2:" + team2healers);
			if (team1healers > team2healers)
			{
				int var = 0;
				while (team1healers != team2healers && team1healers - team2healers > 1)
				{
					if (var > team1healersIndex.size() - 1)
						break;
					System.out.println("Moving healer from team 0 to team 1");
					int index = team1healersIndex.get(var);
					moveHealer(0, 1, index);
					team1healers--;
					team2healers++;
					var++;
				}
			}
			else
			{
				int var = 0;
				while (team2healers != team1healers && team2healers - team1healers > 1)
				{
					if (var > team2healersIndex.size() - 1)
						break;
					System.out.println("Moving healer from team 1 to team 0");
					int index = team2healersIndex.get(var);
					moveHealer(1, 0, index);
					team2healers--;
					team1healers++;
					var++;
				}
			}
		}
		/*
		 * int teamCount = 0, playersCount = 0;
		 * int healers = 0; FastList<Integer> healersIndex = new
		 * FastList<Integer>(); for (L2PcInstance player : _playersShuffle) {
		 * if(player == null) continue;
		 * if(player.getActiveClass() == 97){ healers++;
		 * healersIndex.add(_playersShuffle.indexOf(player)); } }
		 * if(healers > 1) {
		 * for (int i = 0; i < healersIndex.size();i++) {
		 * L2PcInstance player = null; player =
		 * _playersShuffle.get(healersIndex.get(i)); player._originalKarmaTvT =
		 * player.getKarma();
		 * _players.add(player); _players.get(playersCount)._teamNameTvT =
		 * _teams.get(teamCount);
		 * _savePlayers.add(_players.get(playersCount).getObjectId());
		 * _savePlayerTeams.add(_teams.get(teamCount)); playersCount++;
		 * if (teamCount == _teams.size() - 1) teamCount = 0; else teamCount++;
		 * }
		 * }
		 * FastMap<Float,L2PcInstance> playerStats = new
		 * FastMap<Float,L2PcInstance>();
		 * for(L2PcInstance player : _playersShuffle){
		 * if(player.isOnline() == 0) continue;
		 * float gearLvl = player.getGearLevel(); int charID =
		 * player.getCharId();
		 * String finalkey = String.valueOf(gearLvl) + String.valueOf(charID);
		 * Float finalFloatKey = Float.valueOf(finalkey);
		 * playerStats.put(finalFloatKey, player);
		 * }
		 * FastList<L2PcInstance> _sortedPlayers = sortPlayers(playerStats);
		 * _playersShuffle = _sortedPlayers;
		 * int indexNo = 0; for (;;) { if (indexNo >= _playersShuffle.size())
		 * break;
		 * L2PcInstance player = null; player = _playersShuffle.get(indexNo);
		 * indexNo++;
		 * if(player.isOnline() == 0) { continue; }
		 * if(player.getActiveClass() != 97 || healers <= 1){
		 * player._originalKarmaTvT = player.getKarma();
		 * _players.add(player); _players.get(playersCount)._teamNameTvT =
		 * _teams.get(teamCount);
		 * _savePlayers.add(_players.get(playersCount).getObjectId());
		 * _savePlayerTeams.add(_teams.get(teamCount)); playersCount++;
		 * if (teamCount == _teams.size() - 1) teamCount = 0; else teamCount++;
		 * }
		 * }
		 * _playersShuffle.clear();
		 */
	}
	
	private static void moveHealer(int teamFrom, int TeamTo, int index)
	{
		_players.get(index)._teamNameTvT = _teams.get(TeamTo);
		L2PcInstance playa = _players.get(index);
		if (playa != null && playa.isOnline() == 1)
		{
			_savePlayerTeams.set(_savePlayers.indexOf(playa.getObjectId()), _teams.get(TeamTo));
		}
		if (playa != null)
			System.out.println("Moved " + playa.getName() + "from team " + teamFrom + " to team " + TeamTo);
		else
			System.out.println("Moved healer from team " + teamFrom + " to team " + TeamTo);
	}
	
	private static FastList<L2PcInstance> sortPlayers(FastMap<Float, L2PcInstance> map)
	{
		FastList<L2PcInstance> list = new FastList<L2PcInstance>();
		ArrayList<Float> sortedKeys = new ArrayList<Float>(map.keySet());
		Collections.sort(sortedKeys, Collections.reverseOrder());
		for (float gearLvL : sortedKeys)
		{
			list.add(map.get(gearLvL));
		}
		return list;
	}
	
	public static void setUserData()
	{
		for (L2PcInstance player : _players)
		{
			if (player.isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
			player.setKarma(0);
			player.broadcastUserInfo();
		}
	}
	

	
	public static void finishEvent()
	{
		try
		{
			if (!finishEventOk())
				return;
			_started = false;
			unspawnEventNpc();
			processTopTeam();
			L2PcInstance[] topkiller = new L2PcInstance[3];
			synchronized (topkiller)
			{
				FastList<L2PcInstance> plrs = new FastList<L2PcInstance>();
				// collect
				for (L2PcInstance player : _players)
				{
					if (player == null)
					{
						continue;
					}
					if (player.isSupportClass())
					{
						continue;
					}
					player.abortAttack();
					player.abortCast();
					if (player._countTvTkills <= 0)
					{
						continue;
					}
					plrs.add(player);
				}
				for (L2PcInstance player : plrs)
				{
					if (player._countTvTkills > _top1Kills || (player._countTvTkills >= _top1Kills && player._countTvTdies < topkiller[0]._countTvTdies))
					{
						topkiller[0] = player;
						_top1Kills = player._countTvTkills;
					}
				}
				for (L2PcInstance player : plrs)
				{
					if (player != topkiller[0] && !player._teamNameTvT.equals(topkiller[0]._teamNameTvT) && (player._countTvTkills > _top2Kills || (player._countTvTkills >= _top2Kills && player._countTvTdies < topkiller[1]._countTvTdies)))
					{
						topkiller[1] = player;
						_top2Kills = player._countTvTkills;
					}
				}
				for (L2PcInstance player : plrs)
				{
					if (player != null && !player.isSupportClass() && player._countTvTkills > 0 && player != topkiller[0] && player != topkiller[1] && !player._teamNameTvT.equals(topkiller[0]._teamNameTvT) && !player._teamNameTvT.equals(topkiller[1]._teamNameTvT) && (player._countTvTkills > _top3Kills || (player._countTvTkills >= _top3Kills && player._countTvTdies < topkiller[2]._countTvTdies)))
					{
						topkiller[2] = player;
						_top3Kills = player._countTvTkills;
					}
				}
			}
			//rewardTeam(_topTeam, false);
			playKneelAnimation(_topTeam);
			AnnounceToPlayers(true, _eventName + " Team Statistics:");
			for (String team : _teams)
			{
				int _kills = teamKillsCount(team);
				AnnounceToPlayers(true, "Team: " + team + " - Kills: " + _kills);
			}
			if (topkiller[0] != null)
			{
				AnnounceToPlayers(true, "Top killer of each team, they are rewarded 20% more:");
				AnnounceToPlayers(true, topkiller[0].getName() + " - " + topkiller[0]._teamNameTvT + " team - " + _top1Kills + " kills - " + topkiller[0]._countTvTdies + " deaths");
			}
			if (topkiller[1] != null)
				AnnounceToPlayers(true, topkiller[1].getName() + " - " + topkiller[1]._teamNameTvT + " team - " + _top2Kills + " kills - " + topkiller[1]._countTvTdies + " deaths");
			if (topkiller[2] != null)
				AnnounceToPlayers(true, topkiller[2].getName() + " - " + topkiller[2]._teamNameTvT + " team - " + _top3Kills + " kills - " + topkiller[2]._countTvTdies + " deaths");
			teleportFinish();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
	
	// show loosers and winners animations
	public static void playKneelAnimation(String teamName)
	{
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				if (!player._teamNameTvT.equals(teamName))
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
				}
				else if (player._teamNameTvT.equals(teamName))
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				}
			}
		}
	}
	
	private static boolean finishEventOk()
	{
		return _started;
	}
	
	public static void processTopTeam()
	{
		if (teamKillsCount(_teams.get(0)) < 1 && teamKillsCount(_teams.get(1)) < 1)
		{
			// 2 loosing teams
			_teamKillsCount.set(0, 0);
			_teamKillsCount.set(1, 0);
			AnnounceToPlayers(true, _eventName + "(TvT): Match ended as Draw!");
			rewardTeam(null, true);
			return;
		}
		if (_teamKillsCount.get(0) > _teamKillsCount.get(1))
		{
			AnnounceToPlayers(true, _eventName + ": Team " + _teams.get(0) + " wins the match, with " + _teamKillsCount.get(0) + " Kills!");
			_topTeam = _teams.get(0);
			_topKills = _teamKillsCount.get(0);
			rewardTeam(_topTeam, false);
		}
		else if (_teamKillsCount.get(1) > _teamKillsCount.get(0))
		{
			AnnounceToPlayers(true, _eventName + ": Team " + _teams.get(1) + " wins the match, with " + _teamKillsCount.get(1) + " Kills!");
			_topTeam = _teams.get(1);
			_topKills = _teamKillsCount.get(1);
			rewardTeam(_topTeam, false);
		}
		else
		{
			AnnounceToPlayers(true, _eventName + "(TvT): Match ended as Draw!");
			AnnounceToPlayers(true, _eventName + ": " + _teams.get(0) + " Kills: " + _teamKillsCount.get(0) + " - " + _teams.get(1) + " Kills:" + _teamKillsCount.get(0));
			_topTeam = null;
			_topKills = 0;
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
				if (player != null && player.isOnline() == 1 && player._inEventTvT)
				{
					if (player._countTvTkills > 0 || Config.TVT_PRICE_NO_KILLS)
					{
						if (player._teamNameTvT.equals(teamName))
						{
							if (earlyBirdPlayers.contains(player))
							{
								earlyReg = true;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().tvtReg++;
								}
							}
							EventRewardManager.getInstance().rewardPlayer(1, player, earlyReg);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().tvtWon++;
								player.getCounters().tvtPlayed++;
							}
							player.wonEvent = true;
						}
						else if (!tie && teamName != null)
						{
							if (earlyBirdPlayers.contains(player))
							{
								earlyReg = true;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().tvtReg++;
								}
							}
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().tvtPlayed++;
							}
							EventRewardManager.getInstance().rewardPlayer(2, player, earlyReg);
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
									player.getCounters().tvtReg++;
								}
							}
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().tvtPlayed++;
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
	
	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
		{
			// EventScheduler.getInstance().startNextEvent();
			return;
		}
		if (_joining && !_teleport && !_started)
		{
			unspawnEventNpc();
			_joining = false;
			AnnounceToPlayers(true, _eventName + "(TvT): Match aborted!");
			cleanTvT();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		_sGrade = false;
		unspawnEventNpc();
		AnnounceToPlayers(true, _eventName + "(TvT): Match aborted!");
		teleportFinish();
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
	public static void setPara(boolean para)
	{
		for (L2PcInstance player : _players)
		{
			if (para)
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
			if (!para)
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
	public static void dumpData()
	{
		_log.info("");
		_log.info("");
		if (!_joining && !_teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> TvT Engine infos dump (INACTIVE) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> TvT Engine infos dump (JOINING) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> TvT Engine infos dump (TELEPORT) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> TvT Engine infos dump (STARTED) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		_log.info("Name: " + _eventName);
		_log.info("Desc: " + _eventDesc);
		_log.info("Join location: " + _joiningLocationName);
		_log.info("Min lvl: " + _minlvl);
		_log.info("Max lvl: " + _maxlvl);
		_log.info("");
		_log.info("##########################");
		_log.info("# _teams(FastList<String>) #");
		_log.info("##########################");
		for (String team : _teams)
			_log.info(team + " Kills Done :" + _teamKillsCount.get(_teams.indexOf(team)));
		if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
		{
			_log.info("");
			_log.info("#########################################");
			_log.info("# _playersShuffle(FastList<L2PcInstance>) #");
			_log.info("#########################################");
			for (L2PcInstance player : _playersShuffle)
			{
				if (player != null)
					_log.info("Name: " + player.getName());
			}
		}
		_log.info("");
		_log.info("##################################");
		_log.info("# _players(FastList<L2PcInstance>) #");
		_log.info("##################################");
		for (L2PcInstance player : _players)
		{
			if (player != null)
				_log.info("Name: " + player.getName() + "   Team: " + player._teamNameTvT + "  Kills Done:" + player._countTvTkills);
		}
		_log.info("");
		_log.info("#####################################################################");
		_log.info("# _savePlayers(FastList<String>) and _savePlayerTeams(FastList<String>) #");
		_log.info("#####################################################################");
		/*
		 * for (String player : _savePlayers) _log.info("Name: " + player +
		 * "	Team: " + _savePlayerTeams.get(_savePlayers.indexOf(player)));
		 */
		_log.info("");
		_log.info("");
	}
	
	public static void initData()
	{
		_eventName = "";
		_eventDesc = "";
		_topTeam = "";
		_joiningLocationName = "";
		_teams = new FastList<String>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_teamPlayersCount = new FastList<Integer>();
		_teamKillsCount = new FastList<Integer>();
		_teamColors = new FastList<String>();
		_teamTColors = new FastList<String>();
		_teamsX = new FastList<Integer>();
		_teamsY = new FastList<Integer>();
		_teamsZ = new FastList<Integer>();
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_sGrade = false;
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_npcHeading = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topKills = 0;
		_top1Kills = 0;
		_top2Kills = 0;
		_top3Kills = 0;
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
		initData();
		_cleaned = false;
		Event e = EventsParser.getInstance().getEvents().get(id);
		_eventName = e.getName();
		_eventDesc = e.getDesc();
		_joiningLocationName = e.getJoinLocName();
		_respawnDelay = e.getRespawnDelay();
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
		_team1Locs.clear();
		_team2Locs.clear();
		_regAll = e.getRegAll();
		_doublePvPs = e.getDoublePvPs();
		for (int team = 0; team < EventsParser.getInstance().getEvents().get(id).getEvTeams().size(); team++)
		{
			_teams.add(e.getEvTeams().get(team).getName());
			_teamPlayersCount.add(0);
			_teamColors.add("0");
			_teamTColors.add("0");
			_teamColors.set(team, e.getEvTeams().get(team).getNameColor());
			_teamTColors.set(team, e.getEvTeams().get(team).getTitleColor());
			_teamKillsCount.add(0);
		}
		for (int teamLoc = 0; teamLoc < EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().size(); teamLoc++)
		{
			String teamName = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getId();
			int x = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnX();
			int y = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnY();
			int z = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnZ();
			Location teamLocation = new Location(x, y, z); 
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
		EventRewardManager.getInstance().loadRewards(EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId()));
	}
	
	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
		String html = HtmCache.getInstance().getHtm(eventPlayer.getHtmlPrefix(), "data/html/custom/Event/registering.htm");
		html = html.replace("%eventName%", _dispEventName);
		html = html.replace("%minLevel%", String.valueOf(_minlvl));
		html = html.replace("%maxLevel%", String.valueOf(_maxlvl));
		html = html.replace("%minPvP%", "%minPvP%");
		html = html.replace("%minFame%", "%minFame%");
		html = html.replace("%time%", Communicator.getInstance().getTimeRemaining());
		html = html.replace("%earlyBirds%", earlyBirdPlayers.size() + "/16");
		html = html.replace("%regPlayers%", String.valueOf(_players.size()));
		html = html.replace("%maxPlayers%", String.valueOf(_maxPlayers));
		html = html.replace("%objectId%", String.valueOf(objectId));
		NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setHtml(html);
		eventPlayer.sendPacket(msg);
	}
	
	public static void addPlayer(L2PcInstance player, String teamName)
	{
		if (!addPlayerOk(teamName, player))
			return;
		if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
		{
			player._teamNameTvT = teamName;
			_players.add(player);
			setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
		}
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
			_playersShuffle.add(player);
		player._inEventTvT = true;
		player._countTvTkills = 0;
		player._countTvTdies = 0;
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(player);
			player.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
	}
	
	public static void removeOfflinePlayers()
	{
		try
		{
			if (_playersShuffle == null || _playersShuffle.isEmpty())
				return;
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null)
					_playersShuffle.remove(player);
				else if (player.isOnline() == 0 || player.isInJail())
					removePlayer(player);
				if (_playersShuffle.size() == 0 || _playersShuffle.isEmpty())
					break;
			}
		}
		catch (Exception e)
		{}
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
					eventPlayer._inEventTvT = true;
					eventPlayer._countTvTkills = 0;
					eventPlayer._countTvTdies = 0;
					return true;
				}
			}
		}
		catch (Exception e)
		{}
		eventPlayer._inEventTvT = false;
		return false;
	}
	
	public static boolean addPlayerOk(String teamName, L2PcInstance eventPlayer)
	{
		try
		{
			if (Olympiad.getInstance().isRegistered(eventPlayer))
			{
				eventPlayer.sendMessage("You're registered in the Olympiad");
				return false;
			}
			if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventTvT)
			{
				eventPlayer.sendMessage("You are already participating in the event!");
				return false;
			}
			if (eventPlayer._inEventFOS || eventPlayer._inEventCTF || eventPlayer._inEventDM || eventPlayer._inEventVIP)
			{
				eventPlayer.sendMessage("You are already participating in another event!");
				return false;
			}
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null || eventPlayer == null)
					continue;
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
				if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer.sendMessage("You are already participating in the event!");
					return false;
				}
				else if (player.getName() == eventPlayer.getName())
				{
					eventPlayer.sendMessage("You are already participating in the event!");
					return false;
				}
			}
			if (_players.contains(eventPlayer))
			{
				eventPlayer.sendMessage("You are already participating in the event!");
				return false;
			}
			if (CTF._savePlayers.contains(eventPlayer.getObjectId()))
			{
				eventPlayer.sendMessage("You are already participating in another event!");
				return false;
			}
			if (NewCTF._savePlayers.contains(eventPlayer.getObjectId()))
			{
				eventPlayer.sendMessage("You are already participating in another event!");
				return false;
			}
		}
		catch (Exception e)
		{
			_log.warning("TvT Engine exception: " + e.getMessage());
		}
		if (Config.TVT_EVEN_TEAMS.equals("NO"))
			return true;
		else if (Config.TVT_EVEN_TEAMS.equals("BALANCE"))
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
		else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
			return true;
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	public static synchronized void addDisconnectedPlayer(L2PcInstance player)
	{
		if ((Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			player._teamNameTvT = _savePlayerTeams.get(_savePlayers.indexOf(player.getObjectId()));
			for (L2PcInstance p : _players)
			{
				if (p == null)
				{
					continue;
				}
				// check by name incase player got new objectId
				else if (p.getObjectId() == player.getObjectId())
				{
					player._originalKarmaTvT = player.getKarma();
					player._inEventTvT = true;
					player._countTvTkills = p._countTvTkills;
					player._countTvTdies = p._countTvTdies;
					_players.remove(p); // removing old object id from FastList
					_players.add(player); // adding new objectId to FastList
					player.setKarma(0);
					player.setIsPendingRevive(true);
					player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
					player.teleToLocation(getRndTeamLoc(player._teamNameTvT), false);
					// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
					player.startKickFromEventTask();
					break;
				}
			}
		}
	}
	
	public static void removePlayer(L2PcInstance player)
	{
		if (!_joining)
		{
			player.setKarma(player._originalKarmaTvT);
			player.broadcastUserInfo();
			final int index = _savePlayers.indexOf(player.getObjectId());
			if (index >= 0)
				_savePlayerTeams.remove(index);
			_savePlayers.remove((Integer) player.getObjectId());
		}
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
		kills = player._countTvTkills;
		deaths = player._countTvTdies;
		score = 0;
		EventStats.getInstance().parseStats(player, new PlayerStatsTemplate("Team Vs Team", 1, wins, loses, kills, deaths, score));
		player._teamNameTvT = "";
		player._countTvTkills = 0;
		player._countTvTdies = 0;
		player._inEventTvT = false;
		player.setInstanceId(0);
		if ((Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player))
		{
			setTeamPlayersCount(player._teamNameTvT, teamPlayersCount(player._teamNameTvT) - 1);
		}
		player.wonEvent = false;
		_players.remove(player);
		_playersShuffle.remove(player);
	}
	
	public static void cleanTvT()
	{
		if(_cleaned)
			return;
		_log.info("TvT : Cleaning players.");
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				player.abortAttack();
				player.abortCast();
				removePlayer(player);
				if (_savePlayers.contains(player.getObjectId()))
					_savePlayers.remove((Integer) player.getObjectId());
				player._inEventTvT = false;
			}
		}
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
		{
			for (L2PcInstance player : _playersShuffle)
			{
				if (player != null)
					player._inEventTvT = false;
			}
		}
		_log.info("TvT : Cleaning teams.");
		for (String team : _teams)
		{
			int index = _teams.indexOf(team);
			_teamPlayersCount.set(index, 0);
			_teamKillsCount.set(index, 0);
		}
		_topKills = 0;
		_top1Kills = 0;
		_top2Kills = 0;
		_top3Kills = 0;
		_sGrade = false;
		_topTeam = "";
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_buffers = new FastList<L2Spawn>();
		_log.info("Cleaning TvT done.");
		EventEngine.getInstance().terminateEvent();
		EventStats.getInstance().generateTopStats();
		Communicator.getInstance().resetStats();
		unSpawnFences();
		LunaGlobalVariablesHolder.getInstance().endEventDoublePvP();
		_cleaned = true;
	}
	
	public static void unspawnEventNpc()
	{
		if(_registrationNpc != null)
			_registrationNpc.deleteMe();
	}
	
	public static void teleportFinish()
	{
		AnnounceToPlayers(false, _eventName + "(TvT): Teleport back to participation NPC in 20 seconds!");
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
							player.setForceNoSpawnProtection(false);
							player.stopMove(null, false);
							player.abortAttack();
							player.abortCast();
							if (player.isTransformed())
								player.stopTransformation(null);
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
				_log.info("TvT: Teleport finish done.");
				cleanTvT();
			}
		}, 20000);
	}
	
	public static int teamKillsCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		return _teamKillsCount.get(index);
	}
	
	public static void setTeamKillsCount(String teamName, int teamKillsCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamKillsCount.set(index, teamKillsCount);
	}
	
	public static int teamPlayersCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		return _teamPlayersCount.get(index);
	}
	
	public static void setTeamPlayersCount(String teamName, int teamPlayersCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamPlayersCount.set(index, teamPlayersCount);
	}
	
	/**
	 * Finds all players from the same IP and places them in the same teams ;]
	 * 
	 * @param player
	 *            L2PcInstance of the player that has already been removed
	 *            from the queue
	 */
	private final static void checkForSameIP()
	{
		try
		{
			for (L2PcInstance player : _playersShuffle)
			{
				if (player == null || player.isOnline() == 0 || player.isInJail())
				{
					try
					{
						kickPlayerFromTvt(player);
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
					kickPlayerFromTvt(player);
					continue;
				}
				synchronized (_playersShuffle)
				{
					for (L2PcInstance same : _playersShuffle)
					{
						if (same == null || same.isOnline() == 0 || same.isInJail())
						{
							kickPlayerFromTvt(same);
							continue;
						}
						if (same == player)
							continue; // lolz checks if it's actually the same
										// char
						final String sameIP = same.getIP();
						if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
						{
							kickPlayerFromTvt(same);
							continue;
						}
						if (sameIP.equalsIgnoreCase(playerIP))
						{
							final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
							player.sendMessage(msg);
							same.sendMessage(msg);
							kickPlayerFromTvt(same);
						}
					}
				}
			}
		}
		catch (Throwable t)
		{}
	}
	
	public static Comparator<L2PcInstance> comparePlayersScore = new Comparator<L2PcInstance>()
	{
		@Override
		public int compare(L2PcInstance p1, L2PcInstance p2)
		{
			int score1 = p1._countTvTkills;
			int score2 = p2._countTvTkills;
			if (score1 == score2)
			{
				int deaths1 = p1._countTvTdies;
				int deaths2 = p2._countTvTdies;
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
	
	final public static void onDeath(final L2PcInstance player, final L2Character killa)
	{
		if (player == null || player.isOnline() == 0)
			return;
		player.sendMessage("You will be revived and teleported to team spot in " + _respawnDelay + " seconds");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventTvT && _started)
				{
					if (player.isDead())
					{
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(getRndTeamLoc(player._teamNameTvT), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, _respawnDelay * 1000);
		final L2PcInstance killer = killa.getActingPlayer();
		if (killer != null)
		{
			if (killer._inEventTvT && !(killer._teamNameTvT.equals(player._teamNameTvT)))
			{
				killer.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, killer.getObjectId(), killer.getX(), killer.getY(), killer.getZ()));
				player._countTvTdies++;
				killer._countTvTkills++;
				killer.broadcastPacket(new NicknameChanged(killer));
				setTeamKillsCount(killer._teamNameTvT, teamKillsCount(killer._teamNameTvT) + 1);
			}
		}
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
	public static void abortAndFinish()
	{
		unspawnEventNpc();
		if (_started)
		{
			unspawnBuffers();
			unspawnEventNpc();
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
			cleanTvT();
			return;
		}
		else if (_joining && !_teleport && !_started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + ": Event aborted!");
			unspawnEventNpc();
			cleanTvT();
			return;
		}
		if (!_joining && _teleport || _started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + ": Event aborted!");
			unspawnEventNpc();
			teleportFinishFast();
			cleanTvT();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
	}
	public static void teleportFinishFast()
	{
		AnnounceToPlayers(false, _eventName + ": Teleport back to participation NPC!");
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
		cleanTvT();
	}
}