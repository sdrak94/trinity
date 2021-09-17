package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
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
import net.sf.l2j.util.Rnd;

public class TvT
{
	protected static final Logger			_log					= Logger.getLogger(TvT.class.getName());
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
	public static FastList<Integer>			_teamColors				= new FastList<Integer>();
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
	public static int						_eventTime				= 0;									// min
	public static int						_minPlayers				= 0;
	public static int						_maxPlayers				= 0;
	public static int						_playerWon				= 0;
	public static long						lastEventTime			= 0;
	public static boolean					_sGrade					= false;
	public static boolean					_running				= false;
	public static final int					MEDAL_OF_VICTORY		= 97002;
	private static FastList<L2PcInstance>	earlyBirdPlayers		= new FastList<L2PcInstance>();
	
	public static void AnnounceToPlayers(Boolean toall, String announce)
	{
		if (toall)
			Announcements.getInstance().announceToAll(announce);
		else
		{
			CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", announce);
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
	
	public static void addTeam(String teamName)
	{
		if (!checkTeamOk())
			return;
		if (teamName.equals(" "))
			return;
		_teams.add(teamName);
		_teamPlayersCount.add(0);
		_teamKillsCount.add(0);
		_teamColors.add(0);
		_teamsX.add(0);
		_teamsY.add(0);
		_teamsZ.add(0);
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
	
	public static void setTeamColor(String teamName, int color)
	{
		if (!checkTeamOk())
			return;
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamColors.set(index, color);
	}
	
	public static boolean checkTeamOk()
	{
		return !(_started || _teleport || _joining);
	}
	
	public static void startJoin(L2PcInstance activeChar)
	{
		if (!startJoinOk())
		{
			activeChar.sendMessage("Event not set properly.");
			return;
		}
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc(activeChar);
		AnnounceToPlayers(true, _eventName + " (TvT)!");
		AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
		if (_sGrade)
			AnnounceToPlayers(true, "S grade only! (S grade gear auto-enchant to +25 during event)");
	}
	
	public static void startJoin()
	{
		if (!startJoinOk())
			return;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, _eventName + " (TvT)!");
		if (Config.TVT_ANNOUNCE_REWARD)
			AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
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
		AnnounceToPlayers(true, _eventName + " (TvT)!");
		AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
		if (_sGrade)
			AnnounceToPlayers(true, "S grade only! (S grade gear auto-enchant to +25 during event)");
		return true;
	}
	
	public static boolean startJoinOk()
	{
		return !(_started || _teleport || _joining || _teams.size() < 2 || _eventName.isEmpty() || _joiningLocationName.isEmpty() || _eventDesc.isEmpty() || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0 || _teamsX.contains(0) || _teamsY.contains(0) || _teamsZ.contains(0));
	}
	
	private static void spawnEventNpc(L2PcInstance activeChar)
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
			_npcSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn()._isEventMobTvT = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{}
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
			_npcSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn()._isEventMobTvT = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
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
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				TvT.sit();
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
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
					}
				}
			}
		}, 10000);
		_teleport = true;
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
				spawn.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
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
		spawnBuffers();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				TvT.sit();
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
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
					}
				}
			}
		}, 20000);
		_teleport = true;
		return true;
	}
	
	public static void startEvent(L2PcInstance activeChar)
	{
		if (!startEventOk())
			return;
		lastEventTime = System.currentTimeMillis();
		_teleport = false;
		sit();
		AnnounceToPlayers(false, _eventName + "(TvT): Started. Go to kill your enemies!");
		_started = true;
		for (L2PcInstance player : _players)
		{
			player.startKickFromEventTask();
		}
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
		sit();
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
		while (startWaiterTime + interval > System.currentTimeMillis())
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
			String score = " Green: " + teamKillsCount("Green") + " - Orange: " + teamKillsCount("Orange");
			String time = "" + mins + ":" + secs + "  ";
			Communicator.getInstance().setTimeRemaining(time);
			Communicator.getInstance().setTeam1Score(""+teamKillsCount("Green"));
			Communicator.getInstance().setTeam2Score(""+teamKillsCount("Orange"));
			text += " Green: " + teamKillsCount("Green") + " - Orange: " + teamKillsCount("Orange");
			if (_started)
			{
				for (L2PcInstance player : _players)
				{
					if (player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
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
			// L2PcInstance[] topassisters = new L2PcInstance[3];
			// synchronized (topassisters)
			// {
			// FastList<L2PcInstance> plrs = new FastList<L2PcInstance>();
			// // collect
			// for (L2PcInstance player : _players)
			// {
			// if (player == null)
			// {
			// continue;
			// }
			// if (!player.isSupportClass())
			// {
			// continue;
			// }
			// if (player._teamNameTvT == null)
			// {
			// _log.warning(player.getName() + " doesn't have team name");
			// continue;
			// }
			// player.abortAttack();
			// player.abortCast();
			// if (player._countTvTkills <= 0)
			// {
			// continue;
			// }
			// plrs.add(player);
			// }
			// //
			// for (L2PcInstance player : plrs)
			// {
			// if (player._countTvTkills > _top1Assists)
			// {
			// topassisters[0] = player;
			// _top1Assists = player._countTvTkills;
			// }
			// else if (player._countTvTkills >= _top1Assists )
			// {
			// if (player._countTvTdies < topassisters[0]._countTvTdies)
			// {
			// topassisters[0] = player;
			// _top1Assists = player._countTvTkills;
			// }
			// }
			// }
			// for (L2PcInstance player : plrs)
			// {
			// if (player == topassisters[0])
			// {
			// continue;
			// }
			// if (player._teamNameTvT.equals(topassisters[0]._teamNameTvT))
			// {
			// continue;
			// }
			// if (player._countTvTkills > _top2Assists)
			// {
			// topassisters[1] = player;
			// _top2Assists = player._countTvTkills;
			// }
			// else if (player._countTvTkills >= _top2Assists)
			// {
			// if (player._countTvTdies < topassisters[1]._countTvTdies)
			// {
			// topassisters[1] = player;
			// _top2Assists = player._countTvTkills;
			// }
			// }
			// }
			// for (L2PcInstance player : plrs)
			// {
			// if (player == topassisters[0])
			// {
			// continue;
			// }
			// if (player == topassisters[1])
			// {
			// continue;
			// }
			// if (player._teamNameTvT.equals(topassisters[0]._teamNameTvT))
			// {
			// continue;
			// }
			// if (player._teamNameTvT.equals(topassisters[1]._teamNameTvT))
			// {
			// continue;
			// }
			// if (player._countTvTkills > _top3Assists)
			// {
			// topassisters[2] = player;
			// _top3Assists = player._countTvTkills;
			// }
			// else if (player._countTvTkills >= _top3Assists)
			// {
			// if (player._countTvTdies < topassisters[2]._countTvTdies)
			// {
			// topassisters[2] = player;
			// _top3Assists = player._countTvTkills;
			// }
			// }
			// }
			// }
			if (_topKills == 0)
				AnnounceToPlayers(true, _eventName + "(TvT): No team wins the match(nobody killed).");
			else
			{
				AnnounceToPlayers(true, _eventName + "(TvT): " + _topTeam + "'s win the match! " + _topKills + " kills.");
				rewardTeam(_topTeam, topkiller);
				playKneelAnimation(_topTeam);
			}
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
			// if (topassisters[0] != null)
			// {
			// AnnounceToPlayers(true, "Top assister of each team, they are rewarded 20% more:");
			// AnnounceToPlayers(true, topassisters[0].getName() + " - " + topassisters[0]._teamNameTvT + " team - "+_top1Kills+" assists - "+topassisters[0]._countTvTdies+" deaths");
			// }
			//
			// if (topassisters[1] != null)
			// AnnounceToPlayers(true, topassisters[1].getName() + " - " + topassisters[1]._teamNameTvT + " team - "+_top2Kills+" assists - "+topassisters[1]._countTvTdies+" deaths");
			//
			// if (topassisters[2] != null)
			// AnnounceToPlayers(true, topassisters[2].getName() + " - " + topassisters[2]._teamNameTvT + " team - "+_top3Kills+" assists - "+topassisters[2]._countTvTdies+" deaths");
			//
			//
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
		for (String team : _teams)
		{
			if (teamKillsCount(team) > _topKills)
			{
				_topTeam = team;
				_topKills = teamKillsCount(team);
			}
		}
	}
	
	public static void rewardTeam(String teamName, L2PcInstance[] topkillers/* , L2PcInstance[] topassisters */)
	{
		int rewardAmount = _rewardAmount;
		final boolean moreThanTwoTeams = _teams.size() > 2;
		for (L2PcInstance player : _players)
		{
			try
			{
				if (player != null && player.isOnline() == 1 && player._inEventTvT)
				{
					if (player._countTvTkills > 0 || player._countTvTdies > 1 || Config.TVT_PRICE_NO_KILLS)
					{
						if (player._teamNameTvT.equals(teamName))
						{
							if (earlyBirdPlayers.contains(player))
							{
								rewardAmount = (int) (_rewardAmount * 1.25);
								if (rewardAmount == _rewardAmount)
									rewardAmount++;
								player.sendMessage("You received 25% more reward for being early to the event");
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().tvtReg++;
								}
							}
							else
								rewardAmount = _rewardAmount;
							if (player == topkillers[0] || player == topkillers[1] || player == topkillers[2]
							// || player == topassisters[0] || player == topassisters[1]
							// || player == topassisters[2]
							)
							{
								final int newRewardAmount = (int) (rewardAmount * 1.2);
								if (rewardAmount != newRewardAmount)
								{
									rewardAmount = newRewardAmount;
									if (!player.isSupportClass())
									{
										player.sendMessage("You received 20% more reward for having top kills in the event");
									}
									else
									{
										player.sendMessage("You received 20% more reward for supporting your team in the event");
									}
								}
							}
							player.addItem("TvT Event: " + _eventName, _rewardId, rewardAmount, player, true);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
							player.addItem("TVT Event: " + _eventName, TvT.MEDAL_OF_VICTORY, 2, player, true);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().tvtWon++;
							}
						}
						else
						{
							if (_rewardAmount == 1) // if the prize is just 1
													// item, then don't give it
													// to loser team
								continue;
							if (moreThanTwoTeams)
								rewardAmount = (2 * _rewardAmount) / 2;
							else
								rewardAmount = _rewardAmount / 2;
							if (rewardAmount < 1)
								rewardAmount = 1;
							if (earlyBirdPlayers.contains(player))
							{
								final int newRewardAmount = (int) (rewardAmount * 1.25);
								;
								if (rewardAmount == newRewardAmount)
									rewardAmount++;
								else
									rewardAmount = newRewardAmount;
								player.sendMessage("You received 25% more loser's reward for being early to the event");
							}
							if (player == topkillers[0] || player == topkillers[1] || player == topkillers[2]
							// || player == topassisters[0] || player == topassisters[1]
							// || player == topassisters[2]
							)
							{
								final int newRewardAmount = (int) (rewardAmount * 1.2);
								if (rewardAmount != newRewardAmount)
								{
									rewardAmount = newRewardAmount;
									player.sendMessage("You received 20% more loser's reward for having top kills in the event");
								}
							}
							player.addItem("TvT Event: " + _eventName, _rewardId, rewardAmount, player, true);
							player.addItem("TVT Event: " + _eventName, 97000, 2, player, true);
							NpcHtmlMessage nhm = new NpcHtmlMessage(5);
							TextBuilder replyMSG = new TextBuilder("");
							if (moreThanTwoTeams)
								replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 2/3 of the event prize for trying.</body></html>");
							else
								replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 1/2 of the event prize for trying.</body></html>");
							nhm.setHtml(replyMSG.toString());
							player.sendPacket(nhm);
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
			EventScheduler.getInstance().startNextEvent();
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
	
	public static void loadData(int id)
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
		_teamColors = new FastList<Integer>();
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
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from tvt WHERE id=?");
			statement.setInt(1, id);
			rs = statement.executeQuery();
			int teams = 0;
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
				teams = rs.getInt("teamsCount");
				_joinTime = rs.getInt("joinTime");
				_eventTime = rs.getInt("eventTime");
				_minPlayers = rs.getInt("minPlayers");
				_maxPlayers = rs.getInt("maxPlayers");
			}
			statement.close();
			int index = -1;
			if (teams > 0)
				index = 0;
			while (index < teams && index > -1)
			{
				statement = con.prepareStatement("Select * from tvt_teams where id=? AND teamId = ?");
				statement.setInt(1, id);
				statement.setInt(2, index);
				rs = statement.executeQuery();
				while (rs.next())
				{
					_teams.add(rs.getString("teamName"));
					_teamPlayersCount.add(0);
					_teamKillsCount.add(0);
					_teamColors.add(0);
					_teamsX.add(0);
					_teamsY.add(0);
					_teamsZ.add(0);
					_teamsX.set(index, rs.getInt("teamX"));
					_teamsY.set(index, rs.getInt("teamY"));
					_teamsZ.set(index, rs.getInt("teamZ"));
					_teamColors.set(index, rs.getInt("teamColor"));
				}
				index++;
				statement.close();
			}
			/*
			 * if (Rnd.get(100) < 25) _sGrade = true;
			 */
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void saveData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("Delete from tvt");
			statement.execute();
			statement.close();
			statement = con.prepareStatement("INSERT INTO tvt (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
			statement.setInt(13, _teams.size());
			statement.setInt(14, _joinTime);
			statement.setInt(15, _eventTime);
			statement.setInt(16, _minPlayers);
			statement.setInt(17, _maxPlayers);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("Delete from tvt_teams");
			statement.execute();
			statement.close();
			for (String teamName : _teams)
			{
				int index = _teams.indexOf(teamName);
				if (index == -1)
					return;
				statement = con.prepareStatement("INSERT INTO tvt_teams (teamId ,teamName, teamX, teamY, teamZ, teamColor) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
				statement.setInt(3, _teamsX.get(index));
				statement.setInt(4, _teamsY.get(index));
				statement.setInt(5, _teamsZ.get(index));
				statement.setInt(6, _teamColors.get(index));
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
			String html = HtmCache.getInstance().getHtm(eventPlayer.getHtmlPrefix(), "data/html/custom/Event/registering.htm");
			html = html.replace("%eventName%", _dispEventName);
			html = html.replace("%minLevel%", String.valueOf(_minlvl));
			html = html.replace("%maxLevel%", String.valueOf(_maxlvl));
			html = html.replace("%minPvP%", "%minPvP%");
			html = html.replace("%minFame%", "%minFame%");
			html = html.replace("%wRewardAmount%", String.valueOf(_rewardAmount));
			html = html.replace("%lRewardAmount%", String.valueOf(_rewardAmount / 2));
			html = html.replace("%tRewardAmount%", String.valueOf(Math.round((_rewardAmount / 1.5))));
			html = html.replace("%rewardName%", ItemTable.getInstance().getTemplate(_rewardId).getName());
			html = html.replace("%rewardIcon%", ItemTable.getInstance().getTemplate(_rewardId).getIcon());
			html = html.replace("%time%", Communicator.getInstance().getTimeRemaining());
			html = html.replace("%earlyBirds%", earlyBirdPlayers.size() +"/16");
			html = html.replace("%earlyBirdsReward%", String.valueOf((int)(_rewardAmount * 1.25)) + "/" + String.valueOf(Math.round((_rewardAmount / 2) * 1.25)) + "/" +  String.valueOf(Math.round(((_rewardAmount / 1.5) * 1.25))));
			html = html.replace("%regPlayers%", String.valueOf(_players.size()));
			html = html.replace("%maxPlayers%", String.valueOf(_maxPlayers));
			html = html.replace("%objectId%", String.valueOf(objectId));
			NpcHtmlMessage msg = new NpcHtmlMessage(5);
			msg.setHtml(html);
			eventPlayer.sendPacket(msg);
			
			
//			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
//			TextBuilder replyMSG = new TextBuilder("<html><body>");
//			replyMSG.append("TvT Match<br><br><br>");
//			replyMSG.append("Current event...<br1>");
//			replyMSG.append("    ... name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font><br1>");
//			replyMSG.append("    ... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br>");
//			if (Config.TVT_ANNOUNCE_REWARD)
//				replyMSG.append("    ... reward: (" + _rewardAmount + ") " + ItemTable.getInstance().getTemplate(_rewardId).getName() + "<br>");
//			if (!_started && !_joining)
//				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
//			else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
//			{
//				if (!TvT._started)
//				{
//					replyMSG.append("<font color=\"FFFF00\">The event has reached its maximum capacity.</font><br>Keep checking, someone may crit and you can steal their spot.");
//				}
//			}
//			else if (eventPlayer.isCursedWeaponEquipped())
//			{
//				replyMSG.append("<font color=\"FFFF00\">You can't participate in this event with a cursed Weapon.</font><br>");
//			}
//			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() < _maxlvl)
//			{
//				if (_players.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
//				{
//					if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
//						replyMSG.append("You are already participating in team <font color=\"LEVEL\">" + eventPlayer._teamNameTvT + "</font><br><br>");
//					else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
//						replyMSG.append("You are already participating!<br><br>");
//					replyMSG.append("<table border=\"0\"><tr>");
//					replyMSG.append("<td width=\"200\">Wait till event start or</td>");
//					replyMSG.append("<td width=\"60\"><center><button value=\"remove\" action=\"bypass -h npc_" + objectId + "_tvt_player_leave\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
//					replyMSG.append("<td width=\"100\">your participation!</td>");
//					replyMSG.append("</tr></table>");
//				}
//				else
//				{
//					replyMSG.append("You want to participate in the event?<br><br>");
//					replyMSG.append("<td width=\"200\">Your level : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font></td><br>");
//					replyMSG.append("<td width=\"200\">Min level : <font color=\"00FF00\">" + _minlvl + "</font></td><br>");
//					replyMSG.append("<td width=\"200\">Max level : <font color=\"00FF00\">" + _maxlvl + "</font></td><br><br>");
//					if (TvT._sGrade)
//						replyMSG.append("<td width=\"200\"><font color=\"00FF00\">This particular event is set to S-Grade gear only! (Gear will auto-enchant to +25 during event)</font></td><br><br>");
//					if (Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE"))
//					{
//						replyMSG.append("<center><table border=\"0\">");
//						for (String team : _teams)
//						{
//							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined)</td>");
//							replyMSG.append("<td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_tvt_player_join " + team + "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
//						}
//						replyMSG.append("</table></center>");
//					}
//					else if (Config.TVT_EVEN_TEAMS.equals("SHUFFLE"))
//					{
//						replyMSG.append("<center><table border=\"0\">");
//						for (String team : _teams)
//							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font></td>");
//						replyMSG.append("</table></center><br>");
//						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_tvt_player_join eventShuffle\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
//						replyMSG.append("Teams will be randomly generated!");
//					}
//				}
//			}
//			else if (_started && !_joining)
//				replyMSG.append("<center>TvT match is in progress.</center>");
//			else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
//			{
//				replyMSG.append("Your level : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
//				replyMSG.append("Min level : <font color=\"00FF00\">" + _minlvl + "</font><br>");
//				replyMSG.append("Max level : <font color=\"00FF00\">" + _maxlvl + "</font><br><br>");
//				replyMSG.append("<font color=\"FFFF00\">You can't participate in this event.</font><br>");
//			}
//			// Show how many players joined & how many are still needed to join
//			replyMSG.append("<br>There are " + _playersShuffle.size() + " player(s) participating in this event.<br>");
//			if (_joining)
//			{
//				if (_playersShuffle.size() < _minPlayers)
//				{
//					int playersNeeded = _minPlayers - _playersShuffle.size();
//					replyMSG.append("The event will not start unless " + playersNeeded + " more player(s) joins!");
//				}
//			}
//			replyMSG.append("</body></html>");
//			adminReply.setHtml(replyMSG.toString());
//			eventPlayer.sendPacket(adminReply);
//			// Send a Server->Client ActionFailed to the L2PcInstance in order
//			// to avoid that the client wait another packet
//			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
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
				String HWID = player.getClient().getFullHwid();
				String HWID2 = eventPlayer.getClient().getFullHwid();
				if (eventPlayer instanceof Ghost || eventPlayer instanceof Ghost)
					continue;
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
					player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)));
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
		player._teamNameTvT = "";
		player._countTvTkills = 0;
		player._countTvTdies = 0;
		player._inEventTvT = false;
		player.setInstanceId(0);
		if ((Config.TVT_EVEN_TEAMS.equals("NO") || Config.TVT_EVEN_TEAMS.equals("BALANCE")) && _players.contains(player))
		{
			setTeamPlayersCount(player._teamNameTvT, teamPlayersCount(player._teamNameTvT) - 1);
		}
		_players.remove(player);
		_playersShuffle.remove(player);
	}
	
	public static void cleanTvT()
	{
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
		_log.info("Cleaning TvT done.");
		EventScheduler.getInstance().startNextEvent();
	}
	
	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;
		try
		{
			_npcSpawn.getLastSpawn().deleteMe();
			_npcSpawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void teleportFinish()
	{
		AnnounceToPlayers(false, _eventName + "(TvT): Teleport back to participation NPC in 20 seconds!");
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
							player.teleToLocation(_npcX + Rnd.get(-600, 600), _npcY + Rnd.get(-600, 600), _npcZ + 5, false);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().tvtPlayed++;
							}
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
	
	final public static void onDeath(final L2PcInstance player, final L2Character killa)
	{
		if (player == null || player.isOnline() == 0)
			return;
		player.sendMessage("You will be revived and teleported to team spot in " + Config.TVT_REVIVE_DELAY / 1000 + " seconds");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventTvT && _started)
				{
					if (player.isDead())
					{
						player.setInstanceId(_sGrade ? 3 : FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameTvT)), _teamsY.get(_teams.indexOf(player._teamNameTvT)), _teamsZ.get(_teams.indexOf(player._teamNameTvT)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, Config.TVT_REVIVE_DELAY);
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
}