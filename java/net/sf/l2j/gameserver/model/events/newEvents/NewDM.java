package net.sf.l2j.gameserver.model.events.newEvents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerStatsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventRewardManager;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

public class NewDM
{
	protected static final Logger			_log				= Logger.getLogger(NewDM.class.getName());
	public static String					_eventName			= "",
	_eventDesc = "",
	_joiningLocationName = "";
	public static FastList<Integer>			_savePlayers		= new FastList<Integer>();
	public static FastList<L2PcInstance>	_players			= new FastList<L2PcInstance>();
	public static boolean					_joining			= false,
	_teleport = false,
	_started = false,
	_sitForced = false;
	static boolean							massSpawn			= false;
	public static L2Spawn					_npcSpawn;
	public static FastList<L2PcInstance>	_winners			= new FastList<L2PcInstance>();
	public static FastList<L2PcInstance>	_winnersTemp		= new FastList<L2PcInstance>();
	public static FastList<L2PcInstance>	_losers				= new FastList<L2PcInstance>();
	public static int						_npcId				= 0,
	_npcX = 0,
	_npcY = 0,
	_npcZ = 0,
	_rewardId = 0,
	_rewardAmount = 0,
	_topKills = 0,
	_top2Kills = 0,
	_top3Kills = 0,
	_top4Kills = 0,
	_top5Kills = 0,
	_top6Kills = 0,
	_top7Kills = 0,
	_top8Kills = 0,
	_top9Kills = 0,
	_top10Kills = 0,
	_minlvl = 0,
	_maxlvl = 0,
	_playerX = 0,
	_playerY = 0,
	_playerZ = 0,
	_radiousSpawn = 0,
	_joinTime = 0,
	_eventTime = 0;
	public static String					_playerColors		= "0";
	public static String					_topPlayerColors	= "0";
	public static String					_firstPlayerColors	= "0";
	public static String					_playerTColors		= "0";
	public static String					_topPlayerTColors	= "0";
	public static String					_firstPlayerTColors	= "0";
	public static int						_instanceId			= 1;
	public static FastList<Location>		_locs				= new FastList<Location>();
	public static boolean					_running			= false;
	static String							text;
	protected static int					_countOfShownTopPlayers;
	public static int						eventCenterX		= 0;
	public static int						eventCenterY		= 0;
	public static int						eventCenterZ		= 0;
	public static int						eventOffset			= 2000;
	private static FastList<L2PcInstance>	earlyBirdPlayers	= new FastList<L2PcInstance>();
	private static int 						_respawnDelay		= 20000;
	public static FastList<L2PcInstance>	_topPlayers				= new FastList<L2PcInstance>();
	public static boolean 					_regAll					= false;
	public static boolean					_doublePvPs					= false;
	private static L2Npc _registrationNpc = null;
	public static boolean					_cleaned				= false;
	public static FastList<L2FenceInstance> _fences					= new FastList<L2FenceInstance>();
	
	public static void setNpcPos(L2PcInstance activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
	}
	
	public static boolean checkMaxLevel(int maxlvl)
	{
		return _minlvl < maxlvl;
	}
	
	public static boolean checkMinLevel(int minlvl)
	{
		return _maxlvl > minlvl;
	}
	
	public static void setPlayersPos(L2PcInstance activeChar)
	{
		_playerX = activeChar.getX();
		_playerY = activeChar.getY();
		_playerZ = activeChar.getZ();
	}
	
	public static boolean checkPlayerOk()
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
		AnnounceToPlayers(true, "========================");
		AnnounceToPlayers(true, "Registration for DeathMatch event: " + _eventName + " has been opened");
		AnnounceToPlayers(true, "The registration NPC is available in " + _joiningLocationName);
		if(_doublePvPs)
		AnnounceToPlayers(true, "Double PvPs During Event's Phase");
		AnnounceToPlayers(true, "Check Community Board (ALT+B) at Events Tab for more info.");
		AnnounceToPlayers(true, "========================");
	}
	
	private static boolean startJoinOk()
	{
		return !(_started || _teleport || _joining || _eventName.isEmpty() || _joiningLocationName.isEmpty() || _eventDesc.isEmpty() || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _joinTime == 0 || _eventTime == 0);
	}
	
	private static void regAll()
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : pls)
		{
			addPlayer(p);
			p.sendMessage("ok");
			p._inEventDM = true;
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void unspawnEventNpc()
	{
		if(_registrationNpc != null)
			_registrationNpc.deleteMe();
	}
	private static Location getLocToTpPlayer()
	{
		Location finalLoc = null;
		if (_playerX == 0 && !_locs.isEmpty())
		{
			finalLoc = _locs.get(Rnd.get(1, _locs.size() - 1));
		}
		else if (_playerX != 0)
		{
			finalLoc = new Location(_playerX+Rnd.get(-_radiousSpawn,_radiousSpawn), _playerY+Rnd.get(-_radiousSpawn,_radiousSpawn), _playerZ);
		}
		return finalLoc;
	}
	public static void telePlayerToRndTeamSpot(L2PcInstance player)
	{
		if(player.getInstanceId() != 1)
		{
			player.setInstanceId(1);
		}
		player.teleToLocation(getLocToTpPlayer(), false);
	}
	public static boolean teleportStart()
	{
		if (!_joining || _started || _teleport)
			return false;
		_joining = false;
		AnnounceToPlayers(true, _eventName + "(DM): Teleport to spot in 20 seconds!");
		spawnFences();
		setUserData();
		setPara(true);
		DoorTable.getInstance().getDoor(24190001).closeMe();
		DoorTable.getInstance().getDoor(24190002).closeMe();
		DoorTable.getInstance().getDoor(24190003).closeMe();
		DoorTable.getInstance().getDoor(24190004).closeMe();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				//sit();
				for (L2PcInstance player : NewDM._players)
				{
					if (player != null)
					{
						// Remove Summon's buffs
						if (player.getPet() != null)
						{
							L2Summon summon = player.getPet();
							summon.stopAllEffects();
							summon.unSummon(player);
						}
						// Remove player from his party
						if (player.getParty() != null)
						{
							L2Party party = player.getParty();
							party.removePartyMember(player);
						}
						if (player.getParty() != null)
						{
							player.leaveParty();
						}
						player.removeCubics();
						player.doRevive();
						player.setInstanceId(_instanceId);
						player.teleToLocation(getLocToTpPlayer(), false);
						player.broadcastUserInfo();
					}
				}
			}
		}, 20000);
		_teleport = true;
		return true;
	}
	
	public static boolean startEvent()
	{
		if (!startEventOk())
			return false;
		_teleport = false;
		setPara(false);
		//sit();
		AnnounceToPlayers(true, _eventName + "(DM): Started. Go to kill your enemies!");
		_started = true;
		return true;
	}
	
	private static boolean startEventOk()
	{
		return !(_joining || !_teleport || _started);
	}
	
	public static void setUserData()
	{
		for (L2PcInstance player : _players)
		{
			if (player.isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
			player._originalKarmaDM = player.getKarma();
			player._inEventDM = true;
			player._countDMkills = 0;
			player._countDMDeaths = 0;
			player._DMPos = 0;
			player.setKarma(0);
			player.broadcastUserInfo();
		}
	}
	
	public static void removeUserData()
	{
		for (L2PcInstance player : _players)
		{
			player.setKarma(player._originalKarmaDM);
			player._inEventDM = false;
			player._countDMkills = 0;
			player._countDMDeaths = 0;
			player._DMPos = 0;
			player.broadcastUserInfo();
		}
	}
	
	public static boolean finishEvent()
	{
		if (!finishEventOk())
			return false;
		_started = false;
		unspawnEventNpc();
		processTopPlayer();
		if (_topKills == 0)
		{
			AnnounceToPlayers(false, _eventName + "(DM): No players win the match(nobody killed).");
		}
		else
		{
			try
			{
				AnnounceToPlayers(true, _winners.get(0) + " wins the match! " + _topKills + " kills.");
				AnnounceToPlayers(true, _winners.get(1) + " is second! " + _top2Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(2) + " is third! " + _top3Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(3) + " is forth! " + _top4Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(4) + " is fifth! " + _top5Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(5) + " is sixth! " + _top6Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(6) + " is seventh! " + _top7Kills + " kills.");
				AnnounceToPlayers(true, _winners.get(7) + " is eighth! " + _top8Kills + " kills.");
				AnnounceToPlayers(true, "All 8 players are rewarded!");
			}
			catch (Exception e)
			{}
			rewardPlayer();
		}
		teleportFinish();
		DoorTable.getInstance().getDoor(24190001).openMe();
		DoorTable.getInstance().getDoor(24190002).openMe();
		DoorTable.getInstance().getDoor(24190003).openMe();
		DoorTable.getInstance().getDoor(24190004).openMe();
		return true;
	}
	
	private static boolean finishEventOk()
	{
		return _started;
	}
	
	public static void checkPlayersPos()
	{
		for (L2PcInstance p : _players)
		{
			if (p == null)
				continue;
			if(p.isOnline() != 1)
				continue;
			if(p != null)
			{
				if (p.getInstanceId() != _instanceId)
				{
					p.setInstanceId(_instanceId);
					p.teleToLocation(getLocToTpPlayer(), false);
				}
				if (isOutsideDMArea(p))
				{
					p.setInstanceId(_instanceId);
					p.teleToLocation(getLocToTpPlayer(), false);
				}
				p.broadcastUserInfo();
			}
		}
	}
	
	public static void processTopPlayerTemp()
	{
		List<L2PcInstance> tempPlayers = new FastList<L2PcInstance>();
		tempPlayers.addAll(_players);
		Collections.sort(tempPlayers, comparePlayersScore);
		final int countTopPlayers = _countOfShownTopPlayers;
		int i = 0;
		String kd;
		text = "\n\r";
		_topPlayers.clear();
		for (L2PcInstance player : tempPlayers)
		{
			String position = "";
			if (i + 1 == 1)
			{
				position = i + 1 + "st";
			}
			else if (i + 1 == 2)
			{
				position = i + 1 + "nd";
			}
			else if (i + 1 == 3)
			{
				position = i + 1 + "rd";
			}
			else
				position = i + 1 + "th";
			player._DMPos = i + 1;
			text += position + ". " + player.getName() + " Kills: - " + player._countDMkills + "\n\r";
			i++;

			_topPlayers.add(player);
			if (i >= countTopPlayers)
				break;
		}
	}

	public static FastList<L2PcInstance> getTopPlayers()
	{
		return _topPlayers;
	}
	public static Comparator<L2PcInstance> comparePlayersScore = new Comparator<L2PcInstance>()
	{
		@Override
		public int compare(L2PcInstance p1, L2PcInstance p2)
		{
			int score1 = p1._countDMkills;
			int score2 = p2._countDMkills;
			if (score1 == score2)
			{
				int deaths1 = p1._countDMDeaths;
				int deaths2 = p2._countDMDeaths;
				return deaths1 == deaths2 ? 0 : deaths1 < deaths2 ? -1 : 1;
			}
			else
				return score1 < score2 ? 1 : -1;
		}
	};
	
	public static void processTopPlayer()
	{
		L2PcInstance temp = null;
		synchronized (_winners)
		{
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _topKills)
				{
					temp = player;
					_topKills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top2Kills && !_winners.contains(player))
				{
					temp = player;
					_top2Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top3Kills && !_winners.contains(player))
				{
					temp = player;
					_top3Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top4Kills && !_winners.contains(player))
				{
					temp = player;
					_top4Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top5Kills && !_winners.contains(player))
				{
					temp = player;
					_top5Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top6Kills && !_winners.contains(player))
				{
					temp = player;
					_top6Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top7Kills && !_winners.contains(player))
				{
					temp = player;
					_top7Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top8Kills && !_winners.contains(player))
				{
					temp = player;
					_top8Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top9Kills && !_winners.contains(player))
				{
					temp = player;
					_top9Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			for (L2PcInstance player : _players)
			{
				if (player != null && player._countDMkills > _top10Kills && !_winners.contains(player))
				{
					temp = player;
					_top10Kills = player._countDMkills;
				}
			}
			if (temp != null)
				_winners.addLast(temp);
			temp = null;
			// for (L2PcInstance player : _players)
			// {
			// if (player != null && player._countDMkills < _top8Kills && !_losers.contains(player))
			// {
			// temp = player;
			// }
			// }
			// if (temp != null)
			// _losers.addLast(temp);
			// temp = null;
		}
	}
	
	public static void rewardPlayer()
	{
		int count = 0;
		for (L2PcInstance player : _winners)
		{
			boolean earlyReg = false;
			if (player != null && player.isOnline() == 1 && player._inEventDM && player._countDMkills > 0)
			{
				if (earlyBirdPlayers.contains(player))
				{
					earlyReg = true;
					player.sendMessage("You received 25% more reward for being early to the event");
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().dmReg++;
					}
				}
				if (player == _winners.get(0))
				{
					player.wonEvent = true;
					EventRewardManager.getInstance().rewardPlayer(1, player, earlyReg); // reward only first player
				}
				else
				{
					EventRewardManager.getInstance().rewardPlayer(5, player, earlyReg); // reward the rest 9 players
					player.wonEvent = false;
				}
				if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					player.getCounters().dmWon++;
					player.getCounters().dmPlayed++;
				}
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");
				replyMSG.append("<html><body>You won the event. Look in your inventory for the reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
				count++;
				if (count >= 10)
					break;
			}
		}
		rewardLosersPlayer();
	}
	
	public static void rewardLosersPlayer()
	{
		int countLosers = _players.size() - _winners.size();
		int curLoser = 0;
		for (L2PcInstance player : _players)
		{
			boolean earlyReg = false;
			if (player != null && player.isOnline() == 1 && player._inEventDM && !_winners.contains(player))
			{
				if (earlyBirdPlayers.contains(player))
				{
					earlyReg = true;
					player.sendMessage("You received 25% more reward for being early to the event");
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().dmReg++;
					}
				}
				curLoser++;

				player.wonEvent = false;
				EventRewardManager.getInstance().rewardPlayer(2, player, earlyReg); // reward only first player
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");
				replyMSG.append("<html><body>You lost the event.You earned half of the reward amount Look in your inventory for the reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
				if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					player.getCounters().dmPlayed++;
				}
			}
			if (curLoser >= countLosers)
			{
				// Announcements.getInstance().announceToAll("checked: "+ String.valueOf(curLoser)+ " players." );
				break;
			}
		}
		_winners = new FastList<L2PcInstance>();
		_losers = new FastList<L2PcInstance>();
	}
	
	public static void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
		{
			//EventScheduler.getInstance().startNextEvent();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		unspawnEventNpc();
		AnnounceToPlayers(true, _eventName + "(DM): Match aborted!");
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
	
	public static void dumpData()
	{
		_log.info("");
		_log.info("");
		if (!_joining && !_teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> DM Engine infos dump (INACTIVE) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> DM Engine infos dump (JOINING) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> DM Engine infos dump (TELEPORT) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> DM Engine infos dump (STARTED) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		_log.info("Name: " + _eventName);
		_log.info("Desc: " + _eventDesc);
		_log.info("Join location: " + _joiningLocationName);
		_log.info("Min lvl: " + _minlvl);
		_log.info("Max lvl: " + _maxlvl);
		_log.info("");
		_log.info("##################################");
		_log.info("# _players(FastList<L2PcInstance>) #");
		_log.info("##################################");
		_log.info("Total Players : " + _players.size());
		for (L2PcInstance player : _players)
		{
			if (player != null)
				_log.info("Name: " + player.getName() + " kills :" + player._countDMkills);
		}
		_log.info("");
		_log.info("################################");
		_log.info("# _savePlayers(FastList<String>) #");
		_log.info("################################");
		/*
		 * for (String player : _savePlayers)
		 * _log.info("Name: " + player );
		 */
		_log.info("");
		_log.info("");
	}
	
	public static void loadData(int id)
	{
		_cleaned = false;
		Event e = EventsParser.getInstance().getEvents().get(id);
		_eventName = "";
		_eventDesc = "";
		_joiningLocationName = "";
		_savePlayers = new FastList<Integer>();
		_players = new FastList<L2PcInstance>();
		_winners = new FastList<L2PcInstance>();
		// _npcSpawn = null;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_npcId = 0;
		_npcX = 0;
		_npcY = 0;
		_npcZ = 0;
		_rewardId = 0;
		_rewardAmount = 0;
		_topKills = 0;
		_top2Kills = 0;
		_top3Kills = 0;
		_top4Kills = 0;
		_top5Kills = 0;
		_top6Kills = 0;
		_top7Kills = 0;
		_top8Kills = 0;
		_minlvl = 0;
		_maxlvl = 0;
		_playerX = 0;
		_playerY = 0;
		_playerZ = 0;
		_joinTime = 0;
		_eventTime = 0;
		_locs.clear();
		_playerColors = "0";
		_topPlayerColors = "0";
		_firstPlayerColors = "0";
		_playerTColors = "0";
		_topPlayerTColors = "0";
		_firstPlayerTColors = "0";
		_countOfShownTopPlayers = 10;
		eventCenterX = e.getCenterLoc().getX();
		eventCenterY = e.getCenterLoc().getY();
		eventCenterZ = e.getCenterLoc().getZ();
		eventOffset = e.getCenterLocRange();
		_respawnDelay = e.getRespawnDelay();
		_regAll = e.getRegAll();
		_doublePvPs = e.getDoublePvPs();
		if (EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().size() == 1)
		{
			massSpawn = true;
			_playerX = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(0).getSpawnX();
			_playerY = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(0).getSpawnY();
			_playerZ = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(0).getSpawnZ();
			_radiousSpawn = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(0).getRange();
		}
		if (_playerX == 0 && _playerY == 0 && _playerZ == 0)
		{
			for (int spawnLoc = 0; spawnLoc < EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().size(); spawnLoc++)
			{
				int x = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(spawnLoc).getSpawnX();
				int y = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(spawnLoc).getSpawnY();
				int z = EventsParser.getInstance().getEvents().get(id).getEvPlayerSpawns().get(spawnLoc).getSpawnZ();
				_locs.add(new Location(x, y, z));
			}
		}
		_eventName = e.getName();
		_eventDesc = e.getDesc();
		_joiningLocationName = e.getJoinLocName();
		_minlvl = e.getMinLvl();
		_maxlvl = e.getMaxLvl();
		_npcId = e.getRegNpcId();
		_npcX = e.getRegNpcLoc().getX();
		_npcY = e.getRegNpcLoc().getY();
		_npcZ = e.getRegNpcLoc().getZ();
		_playerColors = e.getEvPlayerNColor();
		_topPlayerColors = e.getEvTopPlayerNColor();
		_firstPlayerColors = e.getEvFirstPlayerNColor();
		_playerTColors = e.getEvPlayerTColor();
		_topPlayerTColors = e.getEvTopPlayerTColor();
		_firstPlayerTColors = e.getEvFirstPlayerTColor();
		_rewardId = 6393;
		_rewardAmount = 1;
		_joinTime = e.getJoinTime() * 60 * 1000;
		_eventTime = e.getEventTime() * 60 * 1000;
		_instanceId = e.getInstanceId();
		EventRewardManager.getInstance().loadRewards(EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId()));
	}
	
	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("DM Match<br><br><br>");
			replyMSG.append("Current event...<br1>");
			replyMSG.append("	... name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font><br1>");
			replyMSG.append("	... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br><br>");
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() < _maxlvl)
			{
				if (_players.contains(eventPlayer))
				{
					replyMSG.append("You are already participating!<br><br>");
					replyMSG.append("<table border=\"0\"><tr>");
					replyMSG.append("<td width=\"200\">Wait till event start or</td>");
					replyMSG.append("<td width=\"60\"><center><button value=\"remove\" action=\"bypass -h npc_" + objectId + "_dmevent_player_leave\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
					replyMSG.append("<td width=\"100\">your participation!</td>");
					replyMSG.append("</tr></table>");
				}
				else
				{
					replyMSG.append("You want to participate in the event?<br><br>");
					replyMSG.append("<td width=\"200\">Admin set min lvl : <font color=\"00FF00\">" + _minlvl + "</font></td><br>");
					replyMSG.append("<td width=\"200\">Admin set max lvl : <font color=\"00FF00\">" + _maxlvl + "</font></td><br><br>");
					replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_dmevent_player_join\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				}
			}
			else if (_started && !_joining)
				replyMSG.append("<center>DM match is in progress.</center>");
			else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
			{
				replyMSG.append("Your lvl : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
				replyMSG.append("Admin set min lvl : <font color=\"00FF00\">" + _minlvl + "</font><br>");
				replyMSG.append("Admin set max lvl : <font color=\"00FF00\">" + _maxlvl + "</font><br><br>");
				replyMSG.append("<font color=\"FFFF00\">You can't participate to this event.</font><br>");
			}
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.severe("DM Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception" + e);
		}
	}
	
	public static void addPlayer(L2PcInstance player)
	{
		if (!addPlayerOk(player))
			return;
		_players.add(player);
		player._originalKarmaDM = player.getKarma();
		player._inEventDM = true;
		player._countDMkills = 0;
		player._countDMDeaths = 0;
		player._DMPos = 0;
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(player);
			player.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
		_savePlayers.add(player.getObjectId());
	}
	
	public static boolean addPlayerOk(L2PcInstance eventPlayer)
	{
		if (Olympiad.getInstance().isRegistered(eventPlayer))
		{
			return false;
		}
		if (eventPlayer._inEventTvT)
		{
			eventPlayer.sendMessage("You are already participating to another event!");
			return false;
		}
		if (eventPlayer._inEventFOS)
		{
			eventPlayer.sendMessage("You are already participating to another event!");
			return false;
		}
		if (eventPlayer._inEventCTF)
		{
			eventPlayer.sendMessage("You are already participating to another event!");
			return false;
		}
		if (eventPlayer._inEventDM)
		{
			eventPlayer.sendMessage("You are already participating in the event!");
			return false;
		}
		if (eventPlayer._inEventVIP)
		{
			eventPlayer.sendMessage("You are already participating to another event!");
			return false;
		}
		for (L2PcInstance player : _players)
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
			for (L2PcInstance player : _players)
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
		return true;
	}
	
	public static boolean isOutsideDMArea(L2PcInstance _player)
	{
		if (_player == null || _player.isOnline() == 0)
			return true;
		return !(_player.getX() > eventCenterX - eventOffset && _player.getX() < eventCenterX + eventOffset && _player.getY() > eventCenterY - eventOffset && _player.getY() < eventCenterY + eventOffset && _player.getZ() > eventCenterZ - eventOffset && _player.getZ() < eventCenterZ + eventOffset);
	}
	
	public static synchronized void addDisconnectedPlayer(L2PcInstance player)
	{
		if ((_teleport || _started) || _savePlayers.contains(player.getObjectId()))
		{
			if (Config.DM_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
			}
			for (L2PcInstance p : _players)
			{
				if (p == null)
				{
					continue;
				} // check by name incase player got new objectId
				else if (p.getObjectId() == player.getObjectId())
				{
					player._originalKarmaDM = player.getKarma();
					player._inEventDM = true;
					player._countDMkills = p._countDMkills;
					player._countDMDeaths = p._countDMDeaths;
					player._DMPos = p._DMPos;
					_players.remove(p); // removing old object id from FastList
					_players.add(player); // adding new objectId to FastList
					player.setKarma(0);
					player.setInstanceId(_instanceId);
					player.setIsPendingRevive(true);
					player.teleToLocation(getLocToTpPlayer(), false);
					break;
				}
			}
		}
	}
	
	public static void autoEvent()
	{
		if (startJoinOk())
		{
			startJoin();
			if (_joinTime > 0)
				waiter(_joinTime); // minutes for join event
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
					waiter(_eventTime); // minutes for event time
					finishEvent();
				}
			}
			else
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
			seconds--; // here because we don't want to see two time announce at the same time
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							AnnounceToPlayers(true, "(DM): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "DM Event: " + seconds / 60 / 60 + " hour(s) till registration ends!");
						}
						else if (_started)
							AnnounceToPlayers(false, "DM Event: " + seconds / 60 / 60 + " hour(s) till event ends!");
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
							AnnounceToPlayers(true, "(DM): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "DM Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
							AnnounceToPlayers(false, "DM Event: " + seconds / 60 + " minute(s) till event ends!");
						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
							AnnounceToPlayers(true, "DM Event: " + seconds + " second(s) till registration ends!");
						else if (_teleport)
							AnnounceToPlayers(false, "DM Event: " + seconds + " seconds(s) till fight starts!");
						else if (_started)
							AnnounceToPlayers(false, "DM Event: " + seconds + " second(s) till event ends!");
						break;
				}
			}
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String timetext = "" + mins + ":" + secs + "  ";
			// Communicator			// Communicator

			String eventPhase = "none";
			if(_joining)
				eventPhase = "Registration";
			if(_started)
				eventPhase = "Event Started";
			if(_teleport)
				eventPhase = "Prep. Players for Battle";
			if (_started || _joining || _teleport)
			{
				Communicator.getInstance().setEventName(_eventName);
				Communicator.getInstance().setEventPhase(eventPhase);
				Communicator.getInstance().setTimeRemaining(mins + ":" + secs);
				Communicator.getInstance().setRegPlayers(_players.size());
			}
			
			if (_started)
			{
				processTopPlayerTemp();
				checkPlayersPos();
				for (L2PcInstance player : _players)
				{
					if (player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, "Time Left: " + timetext + text));
				}
			}
			if (!_started && !_joining && !_teleport)
			{
				cleanDM();
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
	
	public static void removeOfflinePlayers()
	{
		try
		{
			if (_players == null || _players.isEmpty())
				return;
			for (L2PcInstance player : _players)
			{
				if (player == null)
					_players.remove(player);
				else if (player.isOnline() == 0 || player.isInJail())
					removePlayer(player);
				if (_players.size() == 0 || _players.isEmpty())
					break;
			}
		}
		catch (Exception e)
		{}
	}
	
	// public static void AnnounceToPlayers(Boolean toall, String announce)
	// {
	// if (toall)
	// Announcements.getInstance().announceToAll(announce);
	// else
	// {
	// CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", announce);
	// if (_players != null && !_players.isEmpty())
	// {
	// for (L2PcInstance player : _players)
	// {
	// if (player != null && player.isOnline() != 0)
	// player.sendPacket(cs);
	// }
	// }
	// }
	// }
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
	
	public static void removePlayer(L2PcInstance player)
	{
		if (player != null)
		{

			int wins = 0;
			int loses = 1;
			int kills = 0;
			int deaths = 0;
			int score = 0;
			if(player.wonEvent)
			{
				wins = 1;
				loses = 0;
			}
			kills = player._countDMkills;
			deaths = player._countDMDeaths;
			score = 0;
			EventStats.getInstance().parseStats(player, new PlayerStatsTemplate("Deathmatch", 1, wins, loses, kills, deaths, score));

			removeUserData();
			player._countDMDeaths = 0;
			player._countDMkills = 0;
			player.wonEvent = false;
			_players.remove(player);
			_savePlayers.remove((Integer) player.getObjectId());
		}
	}
	
	public static void cleanDM()
	{
		if(_cleaned)
			return;
		for (L2PcInstance player : _players)
		{
			removePlayer(player);
		}
		_savePlayers = new FastList<Integer>();
		_winners = new FastList<L2PcInstance>();
		_npcSpawn = null;
		_joining = false;
		_teleport = false;
		_started = false;
		_sitForced = false;
		_topKills = 0;
		_top2Kills = 0;
		_top3Kills = 0;
		_top4Kills = 0;
		_top5Kills = 0;
		_top6Kills = 0;
		_top7Kills = 0;
		_top8Kills = 0;
		_players = new FastList<L2PcInstance>();
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_topPlayers.clear();
		_regAll = false;
		EventEngine.getInstance().terminateEvent();
		EventStats.getInstance().generateTopStats();
		Communicator.getInstance().resetStats();
		unSpawnFences();
		LunaGlobalVariablesHolder.getInstance().endEventDoublePvP();
		_cleaned = true;
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
	public static void teleportFinish()
	{
		AnnounceToPlayers(false, _eventName + "(DM): Teleport back to participation NPC in 20 seconds!");
		setPara(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				for (L2PcInstance player : _players)
				{
					if (player != null && player.isOnline() != 0)
					{
						player.doRevive();
						player.setInstanceId(0);
						player.teleToLocation(_npcX + Rnd.get(-400, 400), _npcY + Rnd.get(-400, 400), _npcZ, false);
						player._inEventDM = false;
						player.setKarma(player._originalKarmaDM);
						player.broadcastUserInfo();

						player.stopMove(null, false);
						player.abortAttack();
						player.abortCast();
						player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
						player.setIsParalyzed(false);
						player.setIsInvul(false);
						InertiaController.getInstance().fetchChill(player).addCredit(Config.EVENT_CREDIT);
					}
				}
				cleanDM();
			}
		}, 20000);
	}
	
	final public static void onDeath(final L2PcInstance player, final L2Character killa)
	{
		if (player == null || player.isOnline() == 0)
			return;
		final L2PcInstance killer = killa.getActingPlayer();
		if (killer != null && killer._inEventDM)
		{
			player._countDMDeaths++;
			killer._countDMkills++;
			killer.broadcastUserInfo();
			player.broadcastUserInfo();
		}
		player.sendMessage("You will be revived and teleported to spot in " + _respawnDelay +" seconds!");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventDM && _started)
				{
					if (player.isDead())
					{
						player.teleToLocation(getLocToTpPlayer(), false);
						player.doRevive();
					}
				}
			}
		}, _respawnDelay * 1000);
	}
	public static void abortAndFinish()
	{
		unspawnEventNpc();
		if (_started)
		{
			unspawnEventNpc();
			processTopPlayer();
			rewardPlayer();
			teleportFinishFast();
			_started = false;
			// finishEventAbort();
		}
		else
			abortFinish();
	}
	public static void abortFinish()
	{
		if (!_joining && !_teleport && !_started)
		{
			cleanDM();
			return;
		}
		else if (_joining && !_teleport && !_started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + ": Event aborted!");
			unspawnEventNpc();
			cleanDM();
			return;
		}
		if (!_joining && _teleport || _started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + ": Event aborted!");
			unspawnEventNpc();
			teleportFinishFast();
			cleanDM();
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
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().dmPlayed++;
					}
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
		cleanDM();
	}
}