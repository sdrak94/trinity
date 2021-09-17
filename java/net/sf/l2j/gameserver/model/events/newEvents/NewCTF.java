package net.sf.l2j.gameserver.model.events.newEvents;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
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
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
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
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerStatsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventRewardManager;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

public class NewCTF
{
	protected static final Logger			_log					= Logger.getLogger(NewCTF.class.getName());
	public static final int					NPC_BUFFER_ID			= 90001;
	private static int						_FlagNPC				= 35062;
	private static int						_FLAG_IN_HAND_ITEM_ID	= 61000;
	public static String					_eventName				= "";
	public static String					_eventDesc				= "";
	public static String					_topTeam				= "";
	public static String					_joiningLocationName	= "";
	public static FastList<String>			_teams					= new FastList<String>();
	public static FastList<String>			_savePlayerTeams		= new FastList<String>();
	public static FastList<Integer>			_savePlayers			= new FastList<Integer>();
	public static FastList<L2PcInstance>	_players				= new FastList<L2PcInstance>();
	public static FastList<L2PcInstance>	_playersShuffle			= new FastList<L2PcInstance>();
	public static FastList<Integer>			_teamPlayersCount		= new FastList<Integer>();
	public static FastList<String>			_teamColors				= new FastList<String>();
	public static FastList<String>			_teamTColors			= new FastList<String>();
	public static FastList<Integer>			_teamsX					= new FastList<Integer>();
	public static FastList<Integer>			_teamsY					= new FastList<Integer>();
	public static FastList<Integer>			_teamsZ					= new FastList<Integer>();
	public static FastList<Location>		_team1Locs				= new FastList<Location>();
	public static FastList<Location>		_team2Locs				= new FastList<Location>();
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
	public static int						_minlvl					= 0;
	public static int						_maxlvl					= 0;
	public static int						_joinTime				= 0;
	public static int						_eventTime				= 0;
	public static int						_minPlayers				= 0;
	public static int						_maxPlayers				= 0;
	public static FastList<Integer>			_teamPointsCount		= new FastList<Integer>();
	public static FastList<Integer>			_flagIds				= new FastList<Integer>();
	public static FastList<Integer>			_flagsX					= new FastList<Integer>();
	public static FastList<Integer>			_flagsY					= new FastList<Integer>();
	public static FastList<Integer>			_flagsZ					= new FastList<Integer>();
	public static FastList<L2Spawn>			_flagSpawns				= new FastList<L2Spawn>();
	public static FastList<L2Spawn>			_throneSpawns			= new FastList<L2Spawn>();
	public static FastList<Boolean>			_flagsTaken				= new FastList<Boolean>();
	public static int						_topScore				= 0;
	public static int						_team1Score				= 0;
	public static int						_team2Score				= 0;
	public static int						eventCenterX			= 0;
	public static int						eventCenterY			= 0;
	public static int						eventCenterZ			= 0;
	public static int						eventOffset				= 0;
	/** Future flag timer */
	public static Future<?>					_flagTimer0				= null;
	public static Future<?>					_flagTimer1				= null;
	public static Future<?>					_flagTimer2				= null;
	public static Future<?>					_flagTimer3				= null;
	public static boolean					_running				= false;
	public static FastList<Integer>			_doors					= new FastList<Integer>();
	private static FastList<L2PcInstance>	earlyBirdPlayers		= new FastList<L2PcInstance>();
	private static int						_respawnDelay			= 20000;
	public static FastList<L2PcInstance>	_topPlayers				= new FastList<L2PcInstance>();
	public static boolean					_regAll					= false;
	public static boolean					_doublePvPs					= false;
	public static FastList<L2Spawn>			_buffers				= new FastList<L2Spawn>();
	public static FastList<L2FenceInstance> _fences					= new FastList<L2FenceInstance>();
	public static L2Npc						_registrationNpc		= null;
	public static boolean					_cleaned				= false;
	
	public synchronized static void showFlagHtml(L2PcInstance eventPlayer, String objectId, String teamName)
	{
		if (eventPlayer == null)
			return;
		try
		{
			eventPlayer.eventTicker = 0;
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body><center>");
			replyMSG.append("CTF Flag<br><br>");
			replyMSG.append("<font color=\"00FF00\">" + teamName + "'s Flag</font><br1>");
			if (eventPlayer._teamNameCTF != null && eventPlayer._teamNameCTF.equals(teamName))
				replyMSG.append("<font color=\"LEVEL\">This is your Flag</font><br1>");
			else
				replyMSG.append("<font color=\"LEVEL\">Enemy Flag!</font><br1>");
			if (_started)
			{
				processInFlagRange(eventPlayer);
			}
			else
				replyMSG.append("CTF match is not in progress yet.<br>Wait for a GM to start the event<br>");
			replyMSG.append("</center></body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.warning("" + "CTF Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception: " + e.getStackTrace());
		}
	}
	
	public static void CheckRestoreFlags()
	{
		FastList<Integer> teamsTakenFlag = new FastList<Integer>();
		try
		{
			for (L2PcInstance player : _players)
			{ // if there's a player with a flag
				// add the index of the team who's FLAG WAS TAKEN to the list
				if (player != null)
				{
					if (player.isOnline() == 0 && player._haveFlagCTF)// logged off with a flag in his hands
					{
						AnnounceToPlayers(false, "(CTF): " + player.getName() + " logged off with a CTF flag!");
						player._haveFlagCTF = false;
						if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
						{
							if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
							{
								_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
								spawnFlag(player._teamNameHaveFlagCTF);
								AnnounceToPlayers(false, "(CTF): " + player._teamNameHaveFlagCTF + " flag now returned to place.");
							}
						}
						removeFlagFromPlayer(player);
						final int indexOwn = _teams.indexOf(player._teamNameCTF);
						switch (indexOwn)
						{
							case 0:
								if (_flagTimer0 != null)
								{
									_flagTimer0.cancel(true);
									_flagTimer0 = null;
								}
								break;
							case 1:
								if (_flagTimer1 != null)
								{
									_flagTimer1.cancel(true);
									_flagTimer1 = null;
								}
								break;
							case 2:
								if (_flagTimer2 != null)
								{
									_flagTimer2.cancel(true);
									_flagTimer2 = null;
								}
								break;
							case 3:
								if (_flagTimer3 != null)
								{
									_flagTimer3.cancel(true);
									_flagTimer3 = null;
								}
								break;
						}
						player._teamNameHaveFlagCTF = null;
						return;
					}
					else if (player._haveFlagCTF)
						teamsTakenFlag.add(_teams.indexOf(player._teamNameHaveFlagCTF));
				}
			}
			// Go over the list of ALL teams
			for (String team : _teams)
			{
				if (team == null)
					continue;
				int index = _teams.indexOf(team);
				if (!teamsTakenFlag.contains(index))
				{
					if (_flagsTaken.get(index))
					{
						_flagsTaken.set(index, false);
						spawnFlag(team);
						AnnounceToPlayers(false, "(CTF): " + team + " flag returned due to player error.");
					}
				}
			}
			// Check if a player ran away from the event holding a flag:
			for (L2PcInstance player : _players)
			{
				if (player != null && player._haveFlagCTF)
				{
					if (isOutsideCTFArea(player))
					{
						AnnounceToPlayers(false, "(CTF): " + player.getName() + " escaped from the event holding a flag!");
						player._haveFlagCTF = false;
						if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
						{
							if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
							{
								_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
								spawnFlag(player._teamNameHaveFlagCTF);
								AnnounceToPlayers(false, "(CTF): " + player._teamNameHaveFlagCTF + " flag now returned to place.");
							}
						}
						removeFlagFromPlayer(player);
						player._teamNameHaveFlagCTF = null;
						player.teleToLocation(getRndTeamLoc(player._teamNameCTF), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
						player.sendMessage("You have been returned to your team spawn");
						return;
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.info("CTF.restoreFlags() Error:" + e.getMessage());
		}
	}

	public static void telePlayerToRndTeamSpot(L2PcInstance player)
	{
		if(player.getInstanceId() != 1)
		{
			player.setInstanceId(1);
		}
		player.teleToLocation(getRndTeamLoc(player._teamNameCTF), false);
	}
	public static void kickPlayerFromCTf(L2PcInstance playerToKick)
	{
		if (playerToKick == null)
			return;
		if (_joining)
		{
			_playersShuffle.remove(playerToKick);
			_players.remove(playerToKick);
			playerToKick._inEventCTF = false;
			playerToKick._teamNameCTF = "";
		}
		else if (_started || _teleport)
		{
			removePlayer(playerToKick);
			if (playerToKick.isOnline() != 0)
			{
				playerToKick.setKarma(playerToKick._originalKarmaCTF);
				playerToKick.broadcastUserInfo();
				playerToKick.sendMessage("You have been kicked from the CTF.");
				playerToKick.teleToLocation(_npcX, _npcY, _npcZ, false);
			}
		}
	}
	

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
	
	public static void Started(L2PcInstance player)
	{
		player._teamNameHaveFlagCTF = null;
		player._haveFlagCTF = false;
	}
	
	public static void StartEvent()
	{
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				player._teamNameHaveFlagCTF = null;
				player._haveFlagCTF = false;
				player.startKickFromEventTask();
			}
		}
		TvT.lastEventTime = System.currentTimeMillis();
		AnnounceToPlayers(false, _eventName + "(CTF): Started. Go Capture the Flags!");
	}
	
	public static void addFlagToPlayer(L2PcInstance _player)
	{
		if (_player.isInvisible() && !_player.isGM())
			_player.stopEffects(L2EffectType.INVISIBLE);
		// remove items from the player hands (right, left, both)
		// This is NOT a BUG, I don't want them to see the icon they have 8D
		L2ItemInstance wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (wpn != null)
				_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_RHAND);
		}
		else
		{
			_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_RHAND);
			wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (wpn != null)
				_player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_LHAND);
		}
		// add the flag in his hands
		_player.getInventory().equipItem(ItemTable.getInstance().createItem("", NewCTF._FLAG_IN_HAND_ITEM_ID, 1, _player, null));
		_player.broadcastPacket(new SocialAction(_player.getObjectId(), 16)); // amazing glow
		_player._haveFlagCTF = true;
		_player.broadcastUserInfo();
		CreatureSay cs = new CreatureSay(_player.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", "You got the flag! Run back! ::"); // 8D
		_player.sendPacket(cs);
	}
	
	public static void removeFlagFromPlayer(L2PcInstance player)
	{
		L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		player._haveFlagCTF = false;
		if (wpn != null)
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			player.getInventory().destroyItemByItemId("", NewCTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
				iu.addModifiedItem(element);
			player.sendPacket(iu);
			player.sendPacket(new ItemList(player, true)); // get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
		else
		{
			player.getInventory().destroyItemByItemId("", NewCTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
			player.sendPacket(new ItemList(player, true)); // get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
	}
	
	public static void setTeamFlag(String teamName, L2PcInstance activeChar)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		addOrSet(_teams.indexOf(teamName), null, false, _FlagNPC, activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}
	
	public static void setTeamFlag(String teamName, int x, int y, int z)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		addOrSet(_teams.indexOf(teamName), null, false, _FlagNPC, x, y, z);
	}
	
	public static void spawnAllFlags()
	{
		while (_flagSpawns.size() < _teams.size())
			_flagSpawns.add(null);
		while (_throneSpawns.size() < _teams.size())
			_throneSpawns.add(null);
		for (String team : _teams)
		{
			int index = _teams.indexOf(team);
			L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_flagIds.get(index));
			L2NpcTemplate throne = NpcTable.getInstance().getTemplate(32027);
			try
			{
				// spawn throne
				_throneSpawns.set(index, new L2Spawn(throne));
				_throneSpawns.get(index).setLocx(_flagsX.get(index));
				_throneSpawns.get(index).setLocy(_flagsY.get(index));
				_throneSpawns.get(index).setLocz(_flagsZ.get(index) - 10);
				_throneSpawns.get(index).setAmount(1);
				_throneSpawns.get(index).setHeading(0);
				_throneSpawns.get(index).setRespawnDelay(1);
				_throneSpawns.get(index).setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				SpawnTable.getInstance().addNewSpawn(_throneSpawns.get(index), false);
				_throneSpawns.get(index).init();
				_throneSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
				_throneSpawns.get(index).getLastSpawn().decayMe();
				_throneSpawns.get(index).getLastSpawn().spawnMe(_throneSpawns.get(index).getLastSpawn().getX(), _throneSpawns.get(index).getLastSpawn().getY(), _throneSpawns.get(index).getLastSpawn().getZ());
				_throneSpawns.get(index).getLastSpawn().setTitle(team + " Throne");
				_throneSpawns.get(index).getLastSpawn().broadcastPacket(new MagicSkillUse(_throneSpawns.get(index).getLastSpawn(), _throneSpawns.get(index).getLastSpawn(), 1036, 1, 5500, 1));
				_throneSpawns.get(index).getLastSpawn()._isCTF_throneSpawn = true;
				// spawn flag
				_flagSpawns.set(index, new L2Spawn(tmpl));
				_flagSpawns.get(index).setLocx(_flagsX.get(index));
				_flagSpawns.get(index).setLocy(_flagsY.get(index));
				_flagSpawns.get(index).setLocz(_flagsZ.get(index));
				_flagSpawns.get(index).setAmount(1);
				_flagSpawns.get(index).setHeading(0);
				_flagSpawns.get(index).setRespawnDelay(1);
				_flagSpawns.get(index).setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				SpawnTable.getInstance().addNewSpawn(_flagSpawns.get(index), false);
				_flagSpawns.get(index).init();
				_flagSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
				_flagSpawns.get(index).getLastSpawn().setTitle(team + "'s Flag");
				_flagSpawns.get(index).getLastSpawn()._CTF_FlagTeamName = team;
				_flagSpawns.get(index).getLastSpawn().decayMe();
				_flagSpawns.get(index).getLastSpawn().spawnMe(_flagSpawns.get(index).getLastSpawn().getX(), _flagSpawns.get(index).getLastSpawn().getY(), _flagSpawns.get(index).getLastSpawn().getZ());
				_flagSpawns.get(index).getLastSpawn()._isCTF_Flag = true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		calculateOutSideOfCTF(); // sets event boundaries so players don't run with the flag.
	}
	
	public static void processTopTeam()
	{
		boolean announceTopScorers = false;
		_topTeam = null;
		if (_team1Score > _team2Score)
		{
			announceTopScorers = true;
			_topScore = _team1Score;
			_topTeam = _teams.get(0);
		}
		else if (_team2Score > _team1Score)
		{
			announceTopScorers = true;
			_topScore = _team2Score;
			_topTeam = _teams.get(1);
		}
		else if (_team1Score == _team2Score || (_team1Score <= 0 && _team2Score <= 0))
		{
			if (_team1Score > 0 || _team2Score > 0)
				announceTopScorers = true;
			_topTeam = null;
			AnnounceToPlayers(true, "(CTF): " + _teams.get(0) + " Team flags taken : " + _team1Score + " flags!");
			AnnounceToPlayers(true, "(CTF): " + _teams.get(1) + " Team flags taken : " + _team2Score + " flags!");
			AnnounceToPlayers(true, "(CTF): None won the match, but both are rewarded.");
			rewardTeam(null, true);
		}
		if (_topTeam != null)
		{
			announceTopScorers = true;
			AnnounceToPlayers(true, _eventName + "(CTF): Team " + _topTeam + " wins the match, with " + _topScore + " flags taken!");
			rewardTeam(_topTeam);
		}
		if (announceTopScorers)
		{
			AnnounceToPlayers(true, _eventName + "(CTF): Top scorers of the CTF event:");
			L2PcInstance[] topScorers = new L2PcInstance[3];
			int[] topScores =
			{
				0, 0, 0
			};
			synchronized (topScorers)
			{
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countCTFflags > topScores[0])
					{
						topScorers[0] = player;
						topScores[0] = player._countCTFflags;
					}
				}
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countCTFflags > topScores[1] && player != topScorers[0])
					{
						topScorers[1] = player;
						topScores[1] = player._countCTFflags;
					}
				}
				for (L2PcInstance player : _players)
				{
					if (player != null && player._countCTFflags > topScores[2] && player != topScorers[0] && player != topScorers[1])
					{
						topScorers[2] = player;
						topScores[2] = player._countCTFflags;
					}
				}
			}
			for (int i = 0; i < topScores.length; i++)
			{
				if (topScorers[i] != null)
					AnnounceToPlayers(true, "(CTF): " + String.valueOf(i+1) + "." + topScorers[i].getName() + " - " + topScorers[i]._teamNameCTF + " team - " + topScores[i] + " Score!");
			}
		}
	}
	
	public static void unspawnAllFlags()
	{
		try
		{
			if (_throneSpawns == null || _flagSpawns == null || _teams == null)
				return;
			for (String team : _teams)
			{
				int index = _teams.indexOf(team);
				if (_throneSpawns.get(index) != null)
				{
					_throneSpawns.get(index).getLastSpawn().deleteMe();
					_throneSpawns.get(index).stopRespawn();
					SpawnTable.getInstance().deleteSpawn(_throneSpawns.get(index), true);
				}
				if (_flagSpawns.get(index) != null)
				{
					_flagSpawns.get(index).getLastSpawn().deleteMe();
					_flagSpawns.get(index).stopRespawn();
					SpawnTable.getInstance().deleteSpawn(_flagSpawns.get(index), true);
				}
			}
			_throneSpawns.clear();
		}
		catch (RuntimeException e)
		{
			_log.warning("CTF Engine[unspawnAllFlags()]: exception: " + e.getMessage());
		}
	}
	
	private static void unspawnFlag(String teamName)
	{
		int index = _teams.indexOf(teamName);
		_flagSpawns.get(index).getLastSpawn().deleteMe();
		_flagSpawns.get(index).stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_flagSpawns.get(index), true);
	}
	
	public static void spawnFlag(String teamName)
	{
		int index = _teams.indexOf(teamName);
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_flagIds.get(index));
		try
		{
			_flagSpawns.set(index, new L2Spawn(tmpl));
			_flagSpawns.get(index).setLocx(_flagsX.get(index));
			_flagSpawns.get(index).setLocy(_flagsY.get(index));
			_flagSpawns.get(index).setLocz(_flagsZ.get(index));
			_flagSpawns.get(index).setAmount(1);
			_flagSpawns.get(index).setHeading(0);
			_flagSpawns.get(index).setRespawnDelay(1);
			_flagSpawns.get(index).setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
			SpawnTable.getInstance().addNewSpawn(_flagSpawns.get(index), false);
			_flagSpawns.get(index).init();
			_flagSpawns.get(index).getLastSpawn().getStatus().setCurrentHp(999999999);
			_flagSpawns.get(index).getLastSpawn().setTitle(teamName + "'s Flag");
			_flagSpawns.get(index).getLastSpawn()._CTF_FlagTeamName = teamName;
			_flagSpawns.get(index).getLastSpawn()._isCTF_Flag = true;
			_flagSpawns.get(index).getLastSpawn().decayMe();
			_flagSpawns.get(index).getLastSpawn().spawnMe(_flagSpawns.get(index).getLastSpawn().getX(), _flagSpawns.get(index).getLastSpawn().getY(), _flagSpawns.get(index).getLastSpawn().getZ());
		}
		catch (Exception e)
		{
			_log.warning("CTF Engine[spawnFlag(" + teamName + ")]: exception: " + e.getStackTrace());
		}
	}
	
	public static boolean InRangeOfFlag(L2PcInstance _player, int flagIndex, int offset)
	{
		return _player.getX() > NewCTF._flagsX.get(flagIndex) - offset && _player.getX() < NewCTF._flagsX.get(flagIndex) + offset && _player.getY() > NewCTF._flagsY.get(flagIndex) - offset && _player.getY() < NewCTF._flagsY.get(flagIndex) + offset && _player.getZ() > NewCTF._flagsZ.get(flagIndex) - offset && _player.getZ() < NewCTF._flagsZ.get(flagIndex) + offset;
	}
	
	public static void processInFlagRange(L2PcInstance _player)
	{
		try
		{
			CheckRestoreFlags();
			for (String team : _teams)
			{
				if (team.equals(_player._teamNameCTF))
				{
					int indexOwn = _teams.indexOf(_player._teamNameCTF);
					// if player is near his team flag holding the enemy flag
					if (InRangeOfFlag(_player, indexOwn, 100) && !_flagsTaken.get(indexOwn) && _player._haveFlagCTF)
					{
						int indexEnemy = _teams.indexOf(_player._teamNameHaveFlagCTF);
						// return enemy flag to place
						_flagsTaken.set(indexEnemy, false);
						spawnFlag(_player._teamNameHaveFlagCTF);
						// remove the flag from this player
						_player.broadcastPacket(new SocialAction(_player.getObjectId(), 16)); // amazing glow
						_player.broadcastUserInfo();
						_player.broadcastPacket(new SocialAction(_player.getObjectId(), 3)); // Victory
						_player.broadcastUserInfo();
						removeFlagFromPlayer(_player);
						// _teamPointsCount.set(indexOwn, teamPointsCount(team) + 1);
						switch (indexOwn)
						{
							case 0:
								if (_flagTimer0 != null)
								{
									_flagTimer0.cancel(true);
									_flagTimer0 = null;
								}
								break;
							case 1:
								if (_flagTimer1 != null)
								{
									_flagTimer1.cancel(true);
									_flagTimer1 = null;
								}
								break;
							case 2:
								if (_flagTimer2 != null)
								{
									_flagTimer2.cancel(true);
									_flagTimer2 = null;
								}
								break;
							case 3:
								if (_flagTimer3 != null)
								{
									_flagTimer3.cancel(true);
									_flagTimer3 = null;
								}
								break;
						}
						_player.broadcastPacket(new PlaySound(0, "ItemSound.quest_finish", 1, _player.getObjectId(), _player.getX(), _player.getY(), _player.getZ()));
						_player.broadcastUserInfo();
						_player._countCTFflags++;
						AnnounceToPlayers(false, "(CTF): " + _player.getName() + " scores for " + _player._teamNameCTF + ".");
						_player.addItem("CTF: " + NewCTF._eventName, 6392, 1, _player, true);
						ItemList il = new ItemList(_player, true);
						incPoints(team);
						_player.sendPacket(il);
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							_player.getCounters().ctfScore++;
						}
					}
				}
				else
				{
					int indexEnemy = _teams.indexOf(team);
					// if the player is near a enemy flag
					if (InRangeOfFlag(_player, indexEnemy, 100) && !_flagsTaken.get(indexEnemy) && !_player._haveFlagCTF && !_player.isDead())
					{
						_flagsTaken.set(indexEnemy, true);
						unspawnFlag(team);
						_player._teamNameHaveFlagCTF = team;
						addFlagToPlayer(_player);
						_player.broadcastUserInfo();
						_player._haveFlagCTF = true;
						_player.sendMessage("You have opponent's flag! You have 70 seconds to score or you will lose the flag!");
						AnnounceToPlayers(false, "(CTF): " + team + " flag taken by " + _player.getName() + "...");
						int indexOwn = _teams.indexOf(_player._teamNameCTF);
						switch (indexOwn)
						{
							case 0:
								_flagTimer0 = ThreadPoolManager.getInstance().scheduleGeneral(new flagTimer0(_player), 70000);
								break;
							case 1:
								_flagTimer1 = ThreadPoolManager.getInstance().scheduleGeneral(new flagTimer1(_player), 70000);
								break;
							case 2:
								_flagTimer2 = ThreadPoolManager.getInstance().scheduleGeneral(new flagTimer2(_player), 70000);
								break;
							case 3:
								_flagTimer3 = ThreadPoolManager.getInstance().scheduleGeneral(new flagTimer3(_player), 70000);
								break;
						}
						pointTeamTo(_player, team);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			return;
		}
	}
	
	static class flagTimer0 implements Runnable
	{
		L2PcInstance noob = null;
		
		public flagTimer0(L2PcInstance flagBearer)
		{
			noob = flagBearer;
		}
		
		public void run()
		{
			if (noob != null && noob._inEventCTF && noob._haveFlagCTF)
			{
				try
				{
					AnnounceToPlayers(false, "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
						}
					removeFlagFromPlayer(noob);
					noob._teamNameHaveFlagCTF = null;
				}
				catch (Throwable t)
				{}
			}
		}
	}
	
	static class flagTimer1 implements Runnable
	{
		L2PcInstance noob = null;
		
		public flagTimer1(L2PcInstance flagBearer)
		{
			noob = flagBearer;
		}
		
		public void run()
		{
			if (noob != null && noob._inEventCTF && noob._haveFlagCTF)
			{
				try
				{
					AnnounceToPlayers(false, "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
						}
					removeFlagFromPlayer(noob);
					noob._teamNameHaveFlagCTF = null;
				}
				catch (Throwable t)
				{}
			}
		}
	}
	
	static class flagTimer2 implements Runnable
	{
		L2PcInstance noob = null;
		
		public flagTimer2(L2PcInstance flagBearer)
		{
			noob = flagBearer;
		}
		
		public void run()
		{
			if (noob != null && noob._inEventCTF && noob._haveFlagCTF)
			{
				try
				{
					AnnounceToPlayers(false, "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
						}
					removeFlagFromPlayer(noob);
					noob._teamNameHaveFlagCTF = null;
				}
				catch (Throwable t)
				{}
			}
		}
	}
	
	static class flagTimer3 implements Runnable
	{
		L2PcInstance noob = null;
		
		public flagTimer3(L2PcInstance flagBearer)
		{
			noob = flagBearer;
		}
		
		public void run()
		{
			if (noob != null && noob._inEventCTF && noob._haveFlagCTF)
			{
				try
				{
					AnnounceToPlayers(false, "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
						}
					removeFlagFromPlayer(noob);
					noob._teamNameHaveFlagCTF = null;
				}
				catch (Throwable t)
				{}
			}
		}
	}
	
	public static void pointTeamTo(L2PcInstance hasFlag, String ourFlag)
	{
		try
		{
			for (L2PcInstance player : _players)
			{
				if (player != null && player.isOnline() != 0)
				{
					if (player._teamNameCTF.equals(ourFlag))
					{
						player.sendMessage(hasFlag.getName() + " took your flag!");
						if (player._haveFlagCTF)
						{
							player.sendMessage("You can not return the flag to headquarters, until your flag is returned to it's place.");
						}
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			_log.warning(e.getMessage());
		}
	}
	
	public static int teamPointsCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		if (_teamPointsCount.isEmpty())
			return -1;
		switch (index)
		{
			case 0:
				_team1Score = _teamPointsCount.get(0);
				break;
			case 1:
				_team2Score = _teamPointsCount.get(1);
				break;
		}
		return _teamPointsCount.get(index);
	}
	
	public static void incPoints(String teamName)
	{
		String team1 = _teams.get(0);
		String team2 = _teams.get(1);
		if (teamName.equalsIgnoreCase(team1))
		{
			_team1Score = _team1Score + 1;
		}
		else if (teamName.equalsIgnoreCase(team2))
		{
			_team2Score = _team2Score + 1;
		}
	}
	
	public static void setTeamPointsCount(String teamName, int teamPointCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		switch (index)
		{
			case 0:
				_team1Score = _teamPointsCount.get(0);
				break;
			case 1:
				_team2Score = _teamPointsCount.get(1);
				break;
		}
		_teamPointsCount.set(index, teamPointCount);
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
	
	private static void addOrSet(int listSize, L2Spawn flagSpawn, boolean flagsTaken, int flagId, int flagX, int flagY, int flagZ)
	{
		while (_flagsX.size() <= listSize)
		{
			_flagSpawns.add(null);
			_flagsTaken.add(false);
			_flagIds.add(_FlagNPC);
			_flagsX.add(0);
			_flagsY.add(0);
			_flagsZ.add(0);
		}
		_flagSpawns.set(listSize, flagSpawn);
		_flagsTaken.set(listSize, flagsTaken);
		_flagIds.set(listSize, flagId);
		_flagsX.set(listSize, flagX);
		_flagsY.set(listSize, flagY);
		_flagsZ.set(listSize, flagZ);
	}
	
	public static boolean checkMaxLevel(int maxlvl)
	{
		return _minlvl < maxlvl;
	}
	
	public static boolean checkMinLevel(int minlvl)
	{
		return _maxlvl > minlvl;
	}
	
	/** returns true if participated players is higher or equal then minimum needed players */
	public static boolean checkMinPlayers(int players)
	{
		return _minPlayers <= players;
	}
	
	/** returns true if max players is higher or equal then participated players */
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
		// _teamPointsCount.remove(index);
		_teamPlayersCount.remove(index);
		_teams.remove(index);
		_flagSpawns.remove(index);
		_flagsTaken.remove(index);
		_flagIds.remove(index);
		_flagsX.remove(index);
		_flagsY.remove(index);
		_flagsZ.remove(index);
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
	
	public static void setTeamColor(String teamName, String color)
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
	
	public static boolean startAutoJoin()
	{
		if (!startJoinOk())
			return false;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, "========================");
		AnnounceToPlayers(true, "Registration for Capture the Flag event: " + _eventName + " has been opened");
		AnnounceToPlayers(true, "The registration NPC is available in " + _joiningLocationName);
		if(_doublePvPs)
		AnnounceToPlayers(true, "Double PvPs During Event's Phase");
		AnnounceToPlayers(true, "Check Community Board (ALT+B) at Events Tab for more info.");
		AnnounceToPlayers(true, "========================");
		return true;
	}
	
	public static boolean startJoinOk()
	{
		if (_started)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _started: " + _started);
			return false;
		}
		if (_teleport)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _teleport: " + _teleport);
			return false;
		}
		if (_joining)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _joining: " + _joining);
			return false;
		}
		if (_teams.size() < 2)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _teams.size(): " + _teams.size());
			return false;
		}
		if (_eventName.isEmpty())
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _eventName: " + _eventName);
			return false;
		}
		if (_joiningLocationName.isEmpty())
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _joiningLocationName: " + _joiningLocationName);
			return false;
		}
		if (_eventDesc.isEmpty())
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _eventDesc: " + _eventDesc);
			return false;
		}
		if (_npcId == 0)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _npcId: " + _npcId);
			return false;
		}
		if (_npcX == 0)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _npcX: " + _npcX);
			return false;
		}
		if (_npcY == 0)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _npcY: " + _npcY);
			return false;
		}
		if (_npcZ == 0)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _npcZ: " + _npcZ);
			return false;
		}
		if (_team1Locs.isEmpty())
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _team1Locs: " + _team1Locs.size());
			return false;
		}
		if (_team2Locs.isEmpty())
		{
			GmListTable.broadcastMessageToAdvancedGMs2("EventId: " + EventEngine.getInstance().getActiveEvent().getId() + " - " + "CTF: _team2Locs: " + _team2Locs.size());
			return false;
		}
		try
		{
			if (_flagsX.contains(0) || _flagsY.contains(0) || _flagsZ.contains(0) || _flagIds.contains(0))
				return false;
			if (_flagsX.size() < _teams.size() || _flagsY.size() < _teams.size() || _flagsZ.size() < _teams.size() || _flagIds.size() < _teams.size())
				return false;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return false;
		}
		return true;
	}
	
	private static void regAll()
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : pls)
		{
			addPlayer(p, "eventShuffle");
			// p.sendPacket(new ExEventMatchMessage(1, "Test 1Message"));
			p.sendMessage("ok");
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
		if (_regAll)
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
		}
		catch (Exception e)
		{
			_log.severe("CTF Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	public static boolean teleportAutoStart()
	{
		if (!_joining || _started || _teleport)
			return false;
		if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			removeOfflinePlayers();
			shuffleTeams();
		}
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			AnnounceToPlayers(true, "Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _playersShuffle.size());
			return false;
		}
		_joining = false;
		AnnounceToPlayers(false, _eventName + "(CTF): Teleport to team spot in 20 seconds!");
		setUserData();
		spawnBuffers();
		spawnFences();
		setPara(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				// sit();
				spawnAllFlags();
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
						player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(getRndTeamLoc(player._teamNameCTF), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
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
	
	public static boolean startAutoEvent()
	{
		if (!startEventOk())
			return false;
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		// sit();
		setPara(false);
		AnnounceToPlayers(true, _eventName + "(CTF): Started. Go Capture the Flags!");
		_started = true;
		for (L2PcInstance player : _players)
		{
			if (player != null)
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
				waiter(30 * 1000); // 30 seconds wait time untill start fight after teleported
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
			seconds--; // here because we don't want to see two time announce at the same time
			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							AnnounceToPlayers(true, _eventName + "(CTF): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "CTF Event: " + seconds / 60 / 60 + " hour(s) till registration close!");
						}
						else if (_started)
							AnnounceToPlayers(false, "CTF Event: " + seconds / 60 / 60 + " hour(s) till event finish!");
						break;
					case 1800: // 30 minutes left
					case 600: // 10 minutes left
					case 180: // 3 minutes left
					case 120: // 2 minutes left
					case 60: // 1 minute left
						if (_joining)
						{
							removeOfflinePlayers();
							AnnounceToPlayers(true, _eventName + "(CTF): Joinable in " + _joiningLocationName + "!");
							AnnounceToPlayers(true, "CTF Event: " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
							AnnounceToPlayers(false, "CTF Event: " + seconds / 60 + " minute(s) till event ends!");
						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
							AnnounceToPlayers(true, "CTF Event: " + seconds + " second(s) till registration close!");
						else if (_teleport)
							AnnounceToPlayers(false, "CTF Event: " + seconds + " seconds(s) till fight starts!");
						else if (_started)
							AnnounceToPlayers(false, "CTF Event: " + seconds + " second(s) till event ends!");
						break;
				}
			}
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String text = "" + mins + ":" + secs + "  ";
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
				Communicator.getInstance().setRegPlayers(_joining ? _playersShuffle.size() : _players.size());
				Communicator.getInstance().setTeam1Name(_teams.get(0));
				Communicator.getInstance().setTeam2Name(_teams.get(1));
			}
			if (_started)
			{
				processTopPlayerTemp();
				Communicator.getInstance().setTeam1Score("" + _team1Score);
				Communicator.getInstance().setTeam2Score("" + _team2Score);
			}
			if (_started)
			{
				String team1 = _teams.get(0);
				String team2 = _teams.get(1);
				text += team1 + ": " + _team1Score + " - " + team2 + ": " + _team2Score;
				for (L2PcInstance player : _players)
				{
					if (player == null || player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
			}
			if (!_started && !_joining && !_teleport)
			{
				cleanCTF();
				run = false;
			}
			if (!run)
			{
				if (seconds > 5)
				{
					interv = 5000;
					seconds = 5;
					run = true;
					toBeFinished = true;
				}
			}
			if (toBeFinished)
			{
				GmListTable.broadcastMessageToAdvancedGMs2("Event will end in " + seconds + " seconds.");
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
	
	private static boolean startEventOk()
	{
		if (_joining || !_teleport || _started)
			return false;
		if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
		{
			if (_teamPlayersCount.contains(0))
				return false;
		}
		/*
		 * else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
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
		 * // if (_playersShuffle.size() < (_teams.size()*2)){
		 * // return false;
		 * // }
		 * }
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
			player._originalKarmaCTF = player.getKarma();
			_players.add(player);
			_players.get(playersCount)._teamNameCTF = _teams.get(teamCount);
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
				if (player._teamNameCTF == _teams.get(0))
				{
					System.out.println("Healer belongs to team 0");
					team1healers++;
					team1healersIndex.add(_players.indexOf(player));
				}
				else if (player._teamNameCTF == _teams.get(1))
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
		 * int healers = 0;
		 * FastList<Integer> healersIndex = new FastList<Integer>();
		 * for (L2PcInstance player : _playersShuffle)
		 * {
		 * if(player == null)
		 * continue;
		 * if(player.getActiveClass() == 97){
		 * healers++;
		 * healersIndex.add(_playersShuffle.indexOf(player));
		 * }
		 * }
		 * if(healers > 1)
		 * {
		 * for (int i = 0; i < healersIndex.size();i++)
		 * {
		 * L2PcInstance player = null;
		 * player = _playersShuffle.get(healersIndex.get(i));
		 * player._originalKarmaCTF = player.getKarma();
		 * _players.add(player);
		 * _players.get(playersCount)._teamNameCTF = _teams.get(teamCount);
		 * _savePlayers.add(_players.get(playersCount).getObjectId());
		 * _savePlayerTeams.add(_teams.get(teamCount));
		 * playersCount++;
		 * if (teamCount == _teams.size() - 1)
		 * teamCount = 0;
		 * else
		 * teamCount++;
		 * }
		 * }
		 * FastMap<Float,L2PcInstance> playerStats = new FastMap<Float,L2PcInstance>();
		 * for(L2PcInstance player : _playersShuffle){
		 * if(player.isOnline() == 0)
		 * continue;
		 * float gearLvl = player.getGearLevel();
		 * int charID = player.getCharId();
		 * String finalkey = String.valueOf(gearLvl) + String.valueOf(charID);
		 * Float finalFloatKey = Float.valueOf(finalkey);
		 * playerStats.put(finalFloatKey, player);
		 * }
		 * FastList<L2PcInstance> _sortedPlayers = sortPlayers(playerStats);
		 * _playersShuffle = _sortedPlayers;
		 * int indexNo = 0;
		 * for (;;)
		 * {
		 * if (indexNo >= _playersShuffle.size())
		 * break;
		 * L2PcInstance player = null;
		 * player = _playersShuffle.get(indexNo);
		 * indexNo++;
		 * if(player.isOnline() == 0)
		 * {
		 * continue;
		 * }
		 * if(player.getActiveClass() != 97 || healers <= 1){
		 * player._originalKarmaCTF = player.getKarma();
		 * _players.add(player);
		 * _players.get(playersCount)._teamNameCTF = _teams.get(teamCount);
		 * _savePlayers.add(_players.get(playersCount).getObjectId());
		 * _savePlayerTeams.add(_teams.get(teamCount));
		 * playersCount++;
		 * if (teamCount == _teams.size() - 1)
		 * teamCount = 0;
		 * else
		 * teamCount++;
		 * }
		 * }
		 * _playersShuffle.clear();
		 */
	}
	
	private static void moveHealer(int teamFrom, int TeamTo, int index)
	{
		_players.get(index)._teamNameCTF = _teams.get(TeamTo);
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
		if (!finishEventOk())
			return;
		_started = false;
		unspawnEventNpc();
		unspawnAllFlags();
		processTopTeam();
		if (_topScore != 0)
			playKneelAnimation(_topTeam);
		if (Config.TVT_ANNOUNCE_TEAM_STATS)
		{
			AnnounceToPlayers(true, _eventName + " Team Statistics:");
			AnnounceToPlayers(true, "Team: " + _teams.get(0) + " - Flags taken: " + _team1Score);
			AnnounceToPlayers(true, "Team: " + _teams.get(1) + " - Flags taken: " + _team2Score);
		}
		teleportFinish();
	}
	
	// show loosers and winners animations
	public static void playKneelAnimation(String teamName)
	{
		for (L2PcInstance player : _players)
		{
			if (player != null && player.isOnline() != 0 && player._inEventCTF)
			{
				if (!player._teamNameCTF.equals(teamName))
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
				}
				else if (player._teamNameCTF.equals(teamName))
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
	
	public static void rewardTeam(String teamName)
	{
		rewardTeam(teamName, false);
	}
	
	public static void rewardTeam(String teamName, boolean tied)
	{
		for (L2PcInstance player : _players)
		{
			boolean earlyReg = false;
			if (player != null && player.isOnline() == 1 && player._inEventCTF && (player._countCTFkills > 0 || player._countCTFflags > 0) )
			{
				if (player._teamNameCTF.equals(teamName) && !tied)
				{
					if (earlyBirdPlayers.contains(player))
					{
						earlyReg = true;
						player.sendMessage("You received 25% more reward for being early to the event");
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							player.getCounters().ctfReg++;
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
						player.getCounters().ctfWon++;
						player.getCounters().ctfPlayed++;
					}
					player.wonEvent = true;
				}
				else if (teamName == null && tied)
				{
					if (earlyBirdPlayers.contains(player))
					{
						earlyReg = true;
						player.sendMessage("You received 25% more reward for being early to the event");
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							player.getCounters().ctfReg++;
						}
					}
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().ctfPlayed++;
					}
					EventRewardManager.getInstance().rewardPlayer(3, player, earlyReg);
					NpcHtmlMessage nhm = new NpcHtmlMessage(5);
					TextBuilder replyMSG = new TextBuilder("");
					replyMSG.append("<html><body>Nobody won this event, therefore the prize is split between the teams.</body></html>");
					nhm.setHtml(replyMSG.toString());
					player.sendPacket(nhm);
					player.wonEvent = false;
				}
				else
				{
					if (earlyBirdPlayers.contains(player))
					{
						earlyReg = true;
						player.sendMessage("You received 25% more reward for being early to the event");
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							player.getCounters().ctfReg++;
						}
					}
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().ctfPlayed++;
					}
					EventRewardManager.getInstance().rewardPlayer(2, player, earlyReg);
					NpcHtmlMessage nhm = new NpcHtmlMessage(5);
					TextBuilder replyMSG = new TextBuilder("");
					replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 1/2 of the event prize for trying.</body></html>");
					nhm.setHtml(replyMSG.toString());
					player.sendPacket(nhm);
					player.wonEvent = false;
				}
			}
		}
	}
	
	public static void abortAndFinish()
	{
		if (_started)
		{
			unspawnEventNpc();
			unspawnBuffers();
			unspawnAllFlags();
			processTopTeam();
			if (Config.TVT_ANNOUNCE_TEAM_STATS)
			{
				AnnounceToPlayers(true, _eventName + " Team Statistics:");
				AnnounceToPlayers(true, "Team: " + _teams.get(0) + " - Flags taken: " + _team1Score);
				AnnounceToPlayers(true, "Team: " + _teams.get(1) + " - Flags taken: " + _team2Score);
			}
			teleportFinishFast();
			_started = false;
		}
		else
			abortFinish();
	}
	
	public static void abortFinish()
	{
		if (!_joining && !_teleport && !_started)
		{
			cleanCTF();
			return;
		}
		else if (_joining && !_teleport && !_started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + "(CTF): Match aborted!");
			unspawnEventNpc();
			cleanCTF();
			return;
			// 1
		}
		if (!_joining && _teleport || _started)
		{
			_joining = false;
			AnnounceToPlayers(true, _eventName + "(CTF): Match aborted!");
			unspawnEventNpc();
			teleportFinishFast();
			cleanCTF();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
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
			AnnounceToPlayers(true, _eventName + "(CTF): Match aborted!");
			cleanCTF();
			return;
		}
		_joining = false;
		_teleport = false;
		_started = false;
		unspawnEventNpc();
		unspawnAllFlags();
		AnnounceToPlayers(true, _eventName + "(CTF): Match aborted!");
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
					if (player.isMounted())
						player.dismount();
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
			_log.info(">> CTF Engine infos dump (INACTIVE) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (_joining && !_teleport && !_started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> CTF Engine infos dump (JOINING) <<");
			_log.info("<<--^----^^-----^----^^------^----->>");
		}
		else if (!_joining && _teleport && !_started)
		{
			_log.info("<<---------------------------------->>");
			_log.info(">> CTF Engine infos dump (TELEPORT) <<");
			_log.info("<<--^----^^-----^----^^------^^----->>");
		}
		else if (!_joining && !_teleport && _started)
		{
			_log.info("<<--------------------------------->>");
			_log.info(">> CTF Engine infos dump (STARTED) <<");
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
			_log.info(team + " Flags Taken :" /* _teamPointsCount.get(_teams.indexOf(team)) */);
		if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
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
				_log.info("Name: " + player.getName() + "   Team: " + player._teamNameCTF + "  Flags :" + player._countCTFflags);
		}
		_log.info("");
		_log.info("#####################################################################");
		_log.info("# _savePlayers(FastList<String>) and _savePlayerTeams(FastList<String>) #");
		_log.info("#####################################################################");
		/*
		 * for (String player : _savePlayers)
		 * _log.info("Name: " + player + "	Team: " + _savePlayerTeams.get(_savePlayers.indexOf(player)));
		 */
		_log.info("");
		_log.info("");
		_log.info("**********==CTF==************");
		// _log.info("CTF._teamPointsCount:" + _teamPointsCount.toString());
		_log.info("CTF._flagIds:" + _flagIds.toString());
		_log.info("CTF._flagSpawns:" + _flagSpawns.toString());
		_log.info("CTF._throneSpawns:" + _throneSpawns.toString());
		_log.info("CTF._flagsTaken:" + _flagsTaken.toString());
		_log.info("CTF._flagsX:" + _flagsX.toString());
		_log.info("CTF._flagsY:" + _flagsY.toString());
		_log.info("CTF._flagsZ:" + _flagsZ.toString());
		_log.info("************EOF**************");
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
		// _teamPointsCount = new FastList<Integer>();
		_teamColors = new FastList<String>();
		_teamTColors = new FastList<String>();
		_teamsX = new FastList<Integer>();
		_teamsY = new FastList<Integer>();
		_teamsZ = new FastList<Integer>();
		_throneSpawns = new FastList<L2Spawn>();
		_flagSpawns = new FastList<L2Spawn>();
		_flagsTaken = new FastList<Boolean>();
		_flagIds = new FastList<Integer>();
		_flagsX = new FastList<Integer>();
		_flagsY = new FastList<Integer>();
		_flagsZ = new FastList<Integer>();
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
		_team1Score = 0;
		_team2Score = 0;
		_players.clear();
	}
	
	public static void loadData(int id)
	{
		initData();
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
		_respawnDelay = e.getRespawnDelay();
		eventCenterX = e.getCenterLoc().getX();
		eventCenterY = e.getCenterLoc().getY();
		eventCenterZ = e.getCenterLoc().getZ();
		_team1Locs.clear();
		_team2Locs.clear();
		_regAll = e.getRegAll();
		_doublePvPs = e.getDoublePvPs();
		for (int team = 0; team < EventsParser.getInstance().getEvents().get(id).getEvTeams().size(); team++)
		{
			_teams.add(e.getEvTeams().get(team).getName());
			_teamPlayersCount.add(0);
			// _teamPointsCount.add(0);
			_teamColors.add("0");
			_teamTColors.add("0");
			_teamColors.set(team, e.getEvTeams().get(team).getNameColor());
			_teamTColors.set(team, e.getEvTeams().get(team).getTitleColor());
		}
		for (int teamFlag = 0; teamFlag < EventsParser.getInstance().getEvents().get(id).getEvTeamFlags().size(); teamFlag++)
		{
			int x = EventsParser.getInstance().getEvents().get(id).getEvTeamFlags().get(teamFlag).getSpawnX();
			int y = EventsParser.getInstance().getEvents().get(id).getEvTeamFlags().get(teamFlag).getSpawnY();
			int z = EventsParser.getInstance().getEvents().get(id).getEvTeamFlags().get(teamFlag).getSpawnZ();
			_flagsX.add(x);
			_flagsY.add(y);
			_flagsZ.add(z);
			_flagSpawns.add(null);
			_flagIds.add(_FlagNPC);
			_flagsTaken.add(false);
		}
		for (int teamLoc = 0; teamLoc < EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().size(); teamLoc++)
		{
			String teamName = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getId();
			int x = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnX();
			int y = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnY();
			int z = EventsParser.getInstance().getEvents().get(id).getEvTeamLocs().get(teamLoc).getSpawnZ();
			Location teamLocation = new Location(x, y, z);
			_teamsX.add(x);
			_teamsY.add(y);
			_teamsZ.add(z);
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
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("CTF Match<br><br><br>");
			replyMSG.append("Current event...<br1>");
			replyMSG.append("   ... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br>");
			if (Config.CTF_ANNOUNCE_REWARD)
				replyMSG.append("   ... reward: (" + _rewardAmount + ") " + ItemTable.getInstance().getTemplate(_rewardId).getName() + "<br>");
			if (!_started && !_joining)
				replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
			else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !checkMaxPlayers(_playersShuffle.size()))
			{
				if (!NewCTF._started)
				{
					replyMSG.append("<font color=\"FFFF00\">The event has reached its maximum capacity.</font><br>Keep checking, someone may crit and you can steal their spot.");
				}
			}
			else if (eventPlayer.isCursedWeaponEquipped() && !Config.TVT_JOIN_CURSED)
			{
				replyMSG.append("<font color=\"FFFF00\">You can't participate in this event with a cursed Weapon.</font><br>");
			}
			else if (!_started && _joining && eventPlayer.getLevel() >= _minlvl && eventPlayer.getLevel() < _maxlvl)
			{
				if (_players.contains(eventPlayer) || checkShufflePlayers(eventPlayer))
				{
					if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
						replyMSG.append("You are already participating in team <font color=\"LEVEL\">" + eventPlayer._teamNameCTF + "</font><br><br>");
					else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
						replyMSG.append("You are already participating!<br><br>");
					replyMSG.append("<table border=\"0\"><tr>");
					replyMSG.append("<td width=\"200\">Wait till event start or</td>");
					replyMSG.append("<td width=\"60\"><center><button value=\"remove\" action=\"bypass -h npc_" + objectId + "_ctf_player_leave\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
					replyMSG.append("<td width=\"100\">your participation!</td>");
					replyMSG.append("</tr></table>");
				}
				else
				{
					replyMSG.append("<td width=\"200\">Your level : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font></td><br>");
					replyMSG.append("<td width=\"200\">Min level : <font color=\"00FF00\">" + _minlvl + "</font></td><br>");
					replyMSG.append("<td width=\"200\">Max level : <font color=\"00FF00\">" + _maxlvl + "</font></td><br><br>");
					if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
					{
						replyMSG.append("<center><table border=\"0\">");
						for (String team : _teams)
						{
							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font>&nbsp;(" + teamPlayersCount(team) + " joined)</td>");
							replyMSG.append("<td width=\"60\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_ctf_player_join " + team + "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
						}
						replyMSG.append("</table></center>");
					}
					else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
					{
						replyMSG.append("<center><table border=\"0\">");
						for (String team : _teams)
							replyMSG.append("<tr><td width=\"100\"><font color=\"LEVEL\">" + team + "</font></td>");
						replyMSG.append("</table></center><br>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_ctf_player_join eventShuffle\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("Teams will be randomly generated!");
					}
				}
			}
			else if (_started && !_joining)
				replyMSG.append("<center>CTF match is in progress.</center>");
			else if (eventPlayer.getLevel() < _minlvl || eventPlayer.getLevel() > _maxlvl)
			{
				replyMSG.append("Your lvl : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
				replyMSG.append("Min level : <font color=\"00FF00\">" + _minlvl + "</font><br>");
				replyMSG.append("Max level : <font color=\"00FF00\">" + _maxlvl + "</font><br><br>");
				replyMSG.append("<font color=\"FFFF00\">You can't participatein this event.</font><br>");
			}
			// Show how many players joined & how many are still needed to join
			replyMSG.append("<br>There are " + _playersShuffle.size() + " player(s) participating in this event.<br>");
			if (_joining)
			{
				if (_playersShuffle.size() < _minPlayers)
				{
					int playersNeeded = _minPlayers - _playersShuffle.size();
					replyMSG.append("The event will not start unless " + playersNeeded + " more player(s) joins!");
				}
			}
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.warning("CTF Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception: " + e);
		}
	}
	
	public static void addPlayer(L2PcInstance player, String teamName)
	{
		if (!addPlayerOk(teamName, player))
			return;
		if (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE"))
		{
			player._teamNameCTF = teamName;
			_players.add(player);
			setTeamPlayersCount(teamName, teamPlayersCount(teamName) + 1);
		}
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
			_playersShuffle.add(player);
		player._inEventCTF = true;
		player._countCTFflags = 0;
		player._countCTFkills = 0;
		player._countCTFDeats = 0;
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
			if (_playersShuffle != null && !_playersShuffle.isEmpty())
			{
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
		}
		catch (Exception e)
		{
			_log.warning(e.getMessage());
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
					eventPlayer._inEventCTF = false;
					continue;
				}
				else if (player.getObjectId() == eventPlayer.getObjectId())
				{
					eventPlayer._inEventCTF = true;
					eventPlayer._countCTFflags = 0;
					eventPlayer._countCTFkills = 0;
					eventPlayer._countCTFDeats = 0;
					return true;
				}
				// this 1 is incase player got new objectid after DC or reconnect
				else if (player.getName().equals(eventPlayer.getName()))
				{
					_playersShuffle.remove(player);
					_playersShuffle.add(eventPlayer);
					eventPlayer._inEventCTF = true;
					eventPlayer._countCTFflags = 0;
					eventPlayer._countCTFkills = 0;
					eventPlayer._countCTFDeats = 0;
					return true;
				}
			}
		}
		catch (Exception e)
		{}
		return false;
	}
	
	public static boolean addPlayerOk(String teamName, L2PcInstance eventPlayer)
	{
		try
		{
			if (Olympiad.getInstance().isRegistered(eventPlayer))
			{
				return false;
			}
			if (checkShufflePlayers(eventPlayer) || eventPlayer._inEventCTF)
			{
				eventPlayer.sendMessage("You are already participating in the event!");
				return false;
			}
			if (eventPlayer._inEventTvT || eventPlayer._inEventDM || eventPlayer._inEventVIP)
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
			if (TvT._savePlayers.contains(eventPlayer.getObjectId()))
			{
				eventPlayer.sendMessage("You are already participating in another event!");
				return false;
			}
		}
		catch (Exception e)
		{
			_log.warning("CTF Siege Engine exception: " + e.getMessage());
		}
		if (Config.CTF_EVEN_TEAMS.equals("NO"))
			return true;
		else if (Config.CTF_EVEN_TEAMS.equals("BALANCE"))
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
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE"))
			return true;
		eventPlayer.sendMessage("Too many players in team \"" + teamName + "\"");
		return false;
	}
	
	public static synchronized void addDisconnectedPlayer(L2PcInstance player)
	{
		/*
		 * !!! CAUTION !!!
		 * Do NOT fix multiple object Ids on this event or you will ruin the flag reposition check!!!
		 * All Multiple object Ids will be collected by the Garbage Collector, after the event ends, memory sweep is made!!!
		 */
		if ((Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && (_teleport || _started)) || (Config.CTF_EVEN_TEAMS.equals("NO") || Config.CTF_EVEN_TEAMS.equals("BALANCE") && (_teleport || _started)))
		{
			player._teamNameCTF = _savePlayerTeams.get(_savePlayers.indexOf(player.getObjectId()));
			for (L2PcInstance p : _players)
			{
				if (p == null)
				{
					continue;
				}
				// check by name incase player got new objectId
				else if (p.getObjectId() == player.getObjectId())
				{
					player._originalKarmaCTF = player.getKarma();
					player._inEventCTF = true;
					player._countCTFflags = p._countCTFflags;
					_players.remove(p); // removing old object id from FastList
					_players.add(player); // adding new objectId to FastList
					player.setKarma(0);
					player.setIsPendingRevive(true);
					player.sendMessage("You will be ported back to event in 5 seconds");
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						public void run()
						{
							player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
							player.teleToLocation(getRndTeamLoc(player._teamNameCTF), false);
						}
					}, 5000);
					// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
					Started(player);
					player.startKickFromEventTask();
					break;
				}
			}
			CheckRestoreFlags();
		}
	}
	
	public static void removePlayer(L2PcInstance player)
	{
		if (!_joining)
		{
			player.setKarma(player._originalKarmaCTF);
			player.broadcastUserInfo();
			final int index = _savePlayers.indexOf(player.getObjectId());
			if (index >= 0)
			{
				if (_savePlayerTeams.get(index) != null)
					_savePlayerTeams.remove(index);
			}
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
		kills = player._countCTFkills;
		deaths = player._countCTFDeats;
		score = player._countCTFflags;
		EventStats.getInstance().parseStats(player, new PlayerStatsTemplate("Capture The Flag", 1, wins, loses, kills, deaths, score));
		player._teamNameCTF = "";
		player._countCTFflags = 0;
		player._countCTFkills = 0;
		player._countCTFDeats = 0;
		player._inEventCTF = false;
		player.setInstanceId(0);
		player.wonEvent = false;
		player.setIsParalyzed(false);
		player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
		_players.remove(player);
		_playersShuffle.remove(player);
	}
	
	public static void cleanCTF()
	{
		//if(_cleaned)
		//	return;
		_log.info("CTF : Cleaning players.");
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				if (player._haveFlagCTF)
					removeFlagFromPlayer(player);
				else
					player.getInventory().destroyItemByItemId("", NewCTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
				player._haveFlagCTF = false;
				removePlayer(player);
				if (_savePlayers.contains(player.getObjectId()))
					_savePlayers.remove((Integer) player.getObjectId());
				player._inEventCTF = false;
			}
		}
		if (_playersShuffle != null && !_playersShuffle.isEmpty())
		{
			for (L2PcInstance player : _playersShuffle)
			{
				if (player != null)
					player._inEventCTF = false;
			}
		}
		_log.info("CTF : Cleaning teams and flags.");
		_topScore = 0;
		_topTeam = "";
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		// _teamPointsCount = new FastList<Integer>();
		_flagSpawns = new FastList<L2Spawn>();
		_flagsTaken = new FastList<Boolean>();
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_regAll = false;
		_players.clear();
		_team1Score = 0;
		_team2Score = 0;
		_buffers = new FastList<L2Spawn>();
		_log.info("Cleaning CTF done.");
		// EventScheduler.getInstance().startNextEvent();
		EventEngine.getInstance().terminateEvent();
		EventStats.getInstance().generateTopStats();
		Communicator.getInstance().resetStats();
		unSpawnFences();
		LunaGlobalVariablesHolder.getInstance().endEventDoublePvP();
		_cleaned = true;
	}
	
	public static void unspawnEventNpc()
	{
		if (_registrationNpc != null)
			_registrationNpc.deleteMe();
	}
	
	public static void teleportFinishFast()
	{
		AnnounceToPlayers(false, _eventName + "(CTF): Teleport back to participation NPC in 20 seconds!");
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
		cleanCTF();
	}
	
	public static void teleportFinish()
	{
		AnnounceToPlayers(false, _eventName + "(CTF): Teleport back to participation NPC in 20 seconds!");
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
							player.setKarma(0);
							player.setInstanceId(0);
							player.teleToLocation(_npcX + Rnd.get(-400, 400), _npcY + Rnd.get(-400, 400), _npcZ, false);
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
				cleanCTF();
			}
		}, 20000);
	}
	
	public static int teamFlagCount(String teamName)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return -1;
		return _teamPointsCount.get(index);
	}
	
	public static void setTeamFlagCount(String teamName, int teamFlagCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
		_teamPointsCount.set(index, teamFlagCount);
	}
	
	/**
	 * Used to calculate the event CTF area, so that players don't run off with the flag.
	 * Essential, since a player may take the flag just so other teams can't score points.
	 * This function is Only called upon ONE time on BEGINING OF EACH EVENT right after we spawn the flags.
	 */
	private static void calculateOutSideOfCTF()
	{
		if (_teams == null || _flagSpawns == null || _teamsX == null || _teamsY == null || _teamsZ == null)
			return;
		int division = _teams.size() * 2, pos = 0;
		int[] locX = new int[division], locY = new int[division], locZ = new int[division];
		// Get all coordinates inorder to create a polygon:
		for (L2Spawn flag : _flagSpawns)
		{
			locX[pos] = flag.getCurX();
			locY[pos] = flag.getCurY();
			locZ[pos] = flag.getCurZ();
			pos++;
			if (pos > division / 2)
				break;
		}
		for (int x = 0; x < _teams.size(); x++)
		{
			locX[pos] = _teamsX.get(x);
			locY[pos] = _teamsY.get(x);
			locZ[pos] = _teamsZ.get(x);
			pos++;
			if (pos > division)
				break;
		}
		// find the polygon center, note that it's not the mathematical center of the polygon,
		// rather than a point which centers all coordinates:
		int centerX = 0, centerY = 0, centerZ = 0;
		for (int x = 0; x < pos; x++)
		{
			centerX += (locX[x] / division);
			centerY += (locY[x] / division);
			centerZ += (locZ[x] / division);
		}
		// now let's find the furthest distance from the "center" to the egg shaped sphere
		// surrounding the polygon, size x1.5 (for maximum logical area to wander...):
		int maxX = 0, maxY = 0, maxZ = 0;
		for (int x = 0; x < pos; x++)
		{
			if (maxX < 2 * Math.abs(centerX - locX[x]))
				maxX = (2 * Math.abs(centerX - locX[x]));
			if (maxY < 2 * Math.abs(centerY - locY[x]))
				maxY = (2 * Math.abs(centerY - locY[x]));
			if (maxZ < 2 * Math.abs(centerZ - locZ[x]))
				maxZ = (2 * Math.abs(centerZ - locZ[x]));
		}
		// centerX,centerY,centerZ are the coordinates of the "event center".
		// so let's save those coordinates to check on the players:
		eventCenterX = centerX;
		eventCenterY = centerY;
		eventCenterZ = centerZ;
		eventOffset = maxX;
		if (eventOffset < maxY)
			eventOffset = maxY;
		if (eventOffset < maxZ)
			eventOffset = maxZ;
	}
	
	public static boolean isOutsideCTFArea(L2PcInstance _player)
	{
		if (_player == null || _player.isOnline() == 0)
			return true;
		return !(_player.getX() > eventCenterX - eventOffset && _player.getX() < eventCenterX + eventOffset && _player.getY() > eventCenterY - eventOffset && _player.getY() < eventCenterY + eventOffset && _player.getZ() > eventCenterZ - eventOffset && _player.getZ() < eventCenterZ + eventOffset);
	}
	
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
						kickPlayerFromCTf(player);
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
					kickPlayerFromCTf(player);
					continue;
				}
				synchronized (_playersShuffle)
				{
					for (L2PcInstance same : _playersShuffle)
					{
						if (same == null || same.isOnline() == 0 || same.isInJail())
						{
							kickPlayerFromCTf(same);
							continue;
						}
						if (same == player)
							continue; // lolz checks if it's actually the same char
						final String sameIP = same.getIP();
						if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
						{
							kickPlayerFromCTf(same);
							continue;
						}
						if (sameIP.equalsIgnoreCase(playerIP))
						{
							final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
							player.sendMessage(msg);
							same.sendMessage(msg);
							kickPlayerFromCTf(same);
						}
					}
				}
			}
		}
		catch (Throwable t)
		{}
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
	
	private static void unspawnBuffers()
	{
		for (int i = 0; i < _buffers.size(); i++)
		{
			_buffers.get(i).stopRespawn();
			_buffers.get(i).getLastSpawn().deleteMe();
		}
	}
	
	public static Comparator<L2PcInstance> comparePlayersScore = new Comparator<L2PcInstance>()
	{
		@Override
		public int compare(L2PcInstance p1, L2PcInstance p2)
		{
			int score1 = p1._countCTFkills;
			int score2 = p2._countCTFkills;
			if (score1 == score2)
			{
				int deaths1 = p1._countCTFDeats;
				int deaths2 = p2._countCTFDeats;
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
		if (player._haveFlagCTF)
			player.removeCTFFlagOnDie();
		player.sendMessage("You will be revived and teleported to team spot in " + _respawnDelay + " seconds");
		player._countCTFDeats++;
		killa.getActingPlayer()._countCTFkills++;
		killa.getActingPlayer().broadcastTitleInfo();
		killa.getActingPlayer().broadcastUserInfo();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventCTF && _started)
				{
					if (player.isDead())
					{
						player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(getRndTeamLoc(player._teamNameCTF), false);
						// player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, _respawnDelay * 1000);
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
}