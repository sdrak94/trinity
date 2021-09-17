package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
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
import net.sf.l2j.util.Rnd;

public class CTF
{
	protected static final Logger			_log				= Logger.getLogger(CTF.class.getName());
	public static final int					NPC_BUFFER_ID		= 90001;
	private static int						_FlagNPC			= 35062, _FLAG_IN_HAND_ITEM_ID = 61000;
	public static String					_eventName			= "", _eventDesc = "", _topTeam = "", _joiningLocationName = "";
	public static FastList<String>			_teams				= new FastList<String>(), _savePlayerTeams = new FastList<String>();
	public static FastList<Integer>			_savePlayers		= new FastList<Integer>();
	public static FastList<L2PcInstance>	_players			= new FastList<L2PcInstance>(), _playersShuffle = new FastList<L2PcInstance>();
	public static FastList<Integer>			_teamPlayersCount	= new FastList<Integer>(), _teamColors = new FastList<Integer>(), _teamsX = new FastList<Integer>(),
	_teamsY = new FastList<Integer>(), _teamsZ = new FastList<Integer>();
	public static boolean					_joining			= false, _teleport = false, _started = false, _sitForced = false;
	public static L2Spawn					_npcSpawn;
	public static int						_npcId				= 0, _npcX = 0, _npcY = 0, _npcZ = 0, _npcHeading = 0, _rewardId = 0, _rewardAmount = 0,
	_minlvl = 0, _maxlvl = 0, _joinTime = 0, _eventTime = 0, _minPlayers = 0, _maxPlayers = 0;
	public static FastList<Integer>			_teamPointsCount	= new FastList<Integer>();
	public static FastList<Integer>			_flagIds			= new FastList<Integer>(), _flagsX = new FastList<Integer>(), _flagsY = new FastList<Integer>(),
	_flagsZ = new FastList<Integer>();
	public static FastList<L2Spawn>			_flagSpawns			= new FastList<L2Spawn>(), _throneSpawns = new FastList<L2Spawn>();
	public static FastList<Boolean>			_flagsTaken			= new FastList<Boolean>();
	public static int						_topScore			= 0, eventCenterX = 0, eventCenterY = 0, eventCenterZ = 0, eventOffset = 0;
	/** Future flag timer */
	public static Future<?>					_flagTimer0			= null;
	public static Future<?>					_flagTimer1			= null;
	public static Future<?>					_flagTimer2			= null;
	public static Future<?>					_flagTimer3			= null;
	public static boolean					_running			= false;
	private static FastList<L2PcInstance>	earlyBirdPlayers	= new FastList<L2PcInstance>();
	
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
						AnnounceToPlayers(false, _eventName + "(CTF): " + player.getName() + " logged off with a CTF flag!");
						player._haveFlagCTF = false;
						if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
						{
							if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
							{
								_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
								spawnFlag(player._teamNameHaveFlagCTF);
								AnnounceToPlayers(false, _eventName + "(CTF): " + player._teamNameHaveFlagCTF + " flag now returned to place.");
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
						AnnounceToPlayers(false, _eventName + "(CTF): " + team + " flag returned due to player error.");
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
						AnnounceToPlayers(false, _eventName + "(CTF): " + player.getName() + " escaped from the event holding a flag!");
						player._haveFlagCTF = false;
						if (_teams.indexOf(player._teamNameHaveFlagCTF) >= 0)
						{
							if (_flagsTaken.get(_teams.indexOf(player._teamNameHaveFlagCTF)))
							{
								_flagsTaken.set(_teams.indexOf(player._teamNameHaveFlagCTF), false);
								spawnFlag(player._teamNameHaveFlagCTF);
								AnnounceToPlayers(false, _eventName + "(CTF): " + player._teamNameHaveFlagCTF + " flag now returned to place.");
							}
						}
						removeFlagFromPlayer(player);
						player._teamNameHaveFlagCTF = null;
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
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
		_player.getInventory().equipItem(ItemTable.getInstance().createItem("", CTF._FLAG_IN_HAND_ITEM_ID, 1, _player, null));
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
			player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
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
			player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
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
		_topTeam = null;
		for (String team : _teams)
		{
			if (teamPointsCount(team) == _topScore && _topScore > 0)
				_topTeam = null;
			if (teamPointsCount(team) > _topScore)
			{
				_topTeam = team;
				_topScore = teamPointsCount(team);
			}
		}
		if (_topScore <= 0)
		{
			AnnounceToPlayers(true, _eventName + "(CTF): No flags taken, but both teams are rewarded!.");
			for (String team : _teams)
			{
				rewardTeam(team, true);
			}
		}
		else
		{
			if (_topTeam == null)
			{
				AnnounceToPlayers(true, _eventName + "(CTF): Maximum flags taken : " + _topScore + " flags! No one won, but both teams are rewarded!");
				for (String team : _teams)
				{
					rewardTeam(team, true);
				}
			}
			else
			{
				AnnounceToPlayers(true, _eventName + "(CTF): Team " + _topTeam + " wins the match, with " + _topScore + " flags taken!");
				rewardTeam(_topTeam);
			}
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
					AnnounceToPlayers(true, _eventName + "(CTF): " + topScorers[i] + " - " + topScorers[i]._teamNameCTF + " team - " + topScores[i] + " flag captures!");
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
		return _player.getX() > CTF._flagsX.get(flagIndex) - offset && _player.getX() < CTF._flagsX.get(flagIndex) + offset && _player.getY() > CTF._flagsY.get(flagIndex) - offset && _player.getY() < CTF._flagsY.get(flagIndex) + offset && _player.getZ() > CTF._flagsZ.get(flagIndex) - offset && _player.getZ() < CTF._flagsZ.get(flagIndex) + offset;
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
						_teamPointsCount.set(indexOwn, teamPointsCount(team) + 1);
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
						AnnounceToPlayers(false, _eventName + "(CTF): " + _player.getName() + " scores for " + _player._teamNameCTF + ".");
						_player.addItem("CTF: " + CTF._eventName, 6392, 1, _player, true);
						ItemList il = new ItemList(_player, true);
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
						Announcements.getInstance().announceToAll(_eventName + "(CTF): " + team + " flag taken by " + _player.getName() + "...");
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
					AnnounceToPlayers(false, _eventName + "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, _eventName + "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
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
					AnnounceToPlayers(false, _eventName + "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, _eventName + "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
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
					AnnounceToPlayers(false, _eventName + "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, _eventName + "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
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
					AnnounceToPlayers(false, _eventName + "(CTF): noob " + noob.getName() + " did not manage to score within the time limit!");
					noob._haveFlagCTF = false;
					if (_teams.indexOf(noob._teamNameHaveFlagCTF) >= 0)
						if (_flagsTaken.get(_teams.indexOf(noob._teamNameHaveFlagCTF)))
						{
							_flagsTaken.set(_teams.indexOf(noob._teamNameHaveFlagCTF), false);
							spawnFlag(noob._teamNameHaveFlagCTF);
							AnnounceToPlayers(false, _eventName + "(CTF): " + noob._teamNameHaveFlagCTF + " flag now returned to place.");
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
		return _teamPointsCount.get(index);
	}
	
	public static void setTeamPointsCount(String teamName, int teamPointCount)
	{
		int index = _teams.indexOf(teamName);
		if (index == -1)
			return;
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
	
	public static void addTeam(String teamName)
	{
		if (!checkTeamOk())
			return;
		if (teamName.equals(" "))
			return;
		_teams.add(teamName);
		_teamPlayersCount.add(0);
		_teamColors.add(0);
		_teamsX.add(0);
		_teamsY.add(0);
		_teamsZ.add(0);
		_teamPointsCount.add(0);
		addOrSet(_teams.indexOf(teamName), null, false, _FlagNPC, 0, 0, 0);
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
		_teamPointsCount.remove(index);
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
			activeChar.sendMessage("Event not setted propertly.");
			return;
		}
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc(activeChar);
		AnnounceToPlayers(true, _eventName + " (CTF)!");
		AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
	}
	
	public static void startJoin()
	{
		if (!startJoinOk())
		{
			_log.warning("Event not setted propertly.");
			return;
		}
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, "Recruiting levels " + _minlvl + " to " + _maxlvl);
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
		AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
	}
	
	public static boolean startAutoJoin()
	{
		if (!startJoinOk())
			return false;
		earlyBirdPlayers.clear();
		_joining = true;
		spawnEventNpc();
		AnnounceToPlayers(true, _eventName + " (CTF)!");
		if (Config.CTF_ANNOUNCE_REWARD)
			AnnounceToPlayers(true, "Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
		AnnounceToPlayers(true, "Recruiting levels " + _minlvl + " to " + _maxlvl);
		AnnounceToPlayers(true, "Joinable in " + _joiningLocationName + "!");
		return true;
	}
	
	public static boolean startJoinOk()
	{
		if (_started || _teleport || _joining || _teams.size() < 2 || _eventName.isEmpty() || _joiningLocationName.isEmpty() || _eventDesc.isEmpty() || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0 || _teamsX.contains(0) || _teamsY.contains(0) || _teamsZ.contains(0))
			return false;
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
			_npcSpawn.getLastSpawn()._isEventMobCTF = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("CTF Engine[spawnEventNpc(" + activeChar.getName() + ")]: exception: " + e.getMessage());
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
			_npcSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventName);
			_npcSpawn.getLastSpawn()._isEventMobCTF = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("CTF Engine[spawnEventNpc(exception: " + e.getMessage());
		}
	}
	
	public static void teleportStart()
	{
		if (!_joining || _started || _teleport)
			return;
		if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && checkMinPlayers(_playersShuffle.size()))
		{
			removeOfflinePlayers();
			shuffleTeams();
		}
		else if (Config.CTF_EVEN_TEAMS.equals("SHUFFLE") && !checkMinPlayers(_playersShuffle.size()))
		{
			AnnounceToPlayers(true, "Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _playersShuffle.size());
			return;
		}
		_joining = false;
		AnnounceToPlayers(true, _eventName + "(CTF): Teleport to team spot in 20 seconds!");
		setUserData();
		spawnBuffers();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				CTF.sit();
				CTF.spawnAllFlags();
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
						player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
					}
				}
			}
		}, 20000);
		_teleport = true;
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
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				sit();
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
						player.doRevive();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
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
		_teleport = false;
		sit();
		_started = true;
		StartEvent();
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
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		sit();
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
			if (_started)
			{
				text += " Cats: " + teamFlagCount("Cats") + " - Dogs: " + teamFlagCount("Dogs");
				for (L2PcInstance player : _players)
				{
					if (player == null || player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
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
			for (String team : _teams)
			{
				int _flags_ = teamFlagCount(team);
				AnnounceToPlayers(true, "Team: " + team + " - Flags taken: " + _flags_);
			}
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
		int rewardAmount = _rewardAmount;
		for (L2PcInstance player : _players)
		{
			if (player != null && player.isOnline() == 1 && player._inEventCTF)
			{
				if (player._teamNameCTF.equals(teamName))
				{
					if (earlyBirdPlayers.contains(player))
					{
						rewardAmount = (int) (_rewardAmount * 1.25);
						if (rewardAmount == _rewardAmount)
							rewardAmount++;
						player.sendMessage("You received 25% more reward for being early to the event.");
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							player.getCounters().ctfReg++;
						}
					}
					else
						rewardAmount = _rewardAmount;
					if (tied)
						rewardAmount /= 2;
					if (rewardAmount < 1)
						rewardAmount = 1;
					player.addItem("CTF Event: " + _eventName, _rewardId, rewardAmount, player, true);
					NpcHtmlMessage nhm = new NpcHtmlMessage(5);
					TextBuilder replyMSG = new TextBuilder("");

					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().ctfReg++;
					}
					replyMSG.append("<html><body>Your team wins the event. Look in your inventory for the reward.</body></html>");
					nhm.setHtml(replyMSG.toString());
					player.sendPacket(nhm);
					if (!tied)
					{
						player.addItem("CTF Event: " + _eventName, TvT.MEDAL_OF_VICTORY, 2, player, true);
					}
					if (tied)
					{
						player.addItem("CTF Event: " + _eventName, 97000, 2, player, true);
					}
				}
				else
				{
					if (tied)
						continue;
					if (_rewardAmount == 1) // if the prize is just 1 item, then don't give it to loser team
						continue;
					rewardAmount = _rewardAmount / 2;
					if (rewardAmount < 1)
						rewardAmount = 1;
					if (earlyBirdPlayers.contains(player))
					{
						rewardAmount = (int) (rewardAmount * 1.25);
						if (rewardAmount == _rewardAmount)
							rewardAmount++;
						player.sendMessage("You received 25% more reward for being early to the event.");
					}
					player.addItem("CTF Event: " + _eventName, _rewardId, rewardAmount, player, true);
					player.addItem("CTF Event: " + _eventName, 97000, 2, player, true);
					NpcHtmlMessage nhm = new NpcHtmlMessage(5);
					TextBuilder replyMSG = new TextBuilder("");
					replyMSG.append("<html><body>Your team did not win the event, but you are rewarded 1/2 of the event prize for trying.</body></html>");
					nhm.setHtml(replyMSG.toString());
					player.sendPacket(nhm);
				}
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
					if (!player.isSitting())
						player.sitDown();
					player.startAbnormalEffect(AbnormalEffect.HOLD_1);
					player.setIsParalyzed(true);
				}
				else
				{
					if (player.isSitting())
						player.standUp();
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
			_log.info(team + " Flags Taken :" + _teamPointsCount.get(_teams.indexOf(team)));
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
		_log.info("CTF._teamPointsCount:" + _teamPointsCount.toString());
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
		_teamPointsCount = new FastList<Integer>();
		_teamColors = new FastList<Integer>();
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
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from ctf WHERE id=?");
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
				statement = con.prepareStatement("Select * from ctf_teams where id=? AND teamId = ?");
				statement.setInt(1, id);
				statement.setInt(2, index);
				rs = statement.executeQuery();
				while (rs.next())
				{
					_teams.add(rs.getString("teamName"));
					_teamPlayersCount.add(0);
					_teamPointsCount.add(0);
					_teamColors.add(0);
					_teamsX.add(0);
					_teamsY.add(0);
					_teamsZ.add(0);
					_teamsX.set(index, rs.getInt("teamX"));
					_teamsY.set(index, rs.getInt("teamY"));
					_teamsZ.set(index, rs.getInt("teamZ"));
					_teamColors.set(index, rs.getInt("teamColor"));
					_flagsX.add(0);
					_flagsY.add(0);
					_flagsZ.add(0);
					_flagsX.set(index, rs.getInt("flagX"));
					_flagsY.set(index, rs.getInt("flagY"));
					_flagsZ.set(index, rs.getInt("flagZ"));
					_flagSpawns.add(null);
					_flagIds.add(_FlagNPC);
					_flagsTaken.add(false);
				}
				index++;
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.severe("Exception: CTF.loadData(): " + e.getMessage());
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
	
	public static void saveData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("Delete from ctf");
			statement.execute();
			statement.close();
			statement = con.prepareStatement("INSERT INTO ctf (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, npcHeading, rewardId, rewardAmount, teamsCount, joinTime, eventTime, minPlayers, maxPlayers) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
			statement = con.prepareStatement("Delete from ctf_teams");
			statement.execute();
			statement.close();
			for (String teamName : _teams)
			{
				int index = _teams.indexOf(teamName);
				if (index == -1)
					return;
				statement = con.prepareStatement("INSERT INTO ctf_teams (teamId ,teamName, teamX, teamY, teamZ, teamColor, flagX, flagY, flagZ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, index);
				statement.setString(2, teamName);
				statement.setInt(3, _teamsX.get(index));
				statement.setInt(4, _teamsY.get(index));
				statement.setInt(5, _teamsZ.get(index));
				statement.setInt(6, _teamColors.get(index));
				statement.setInt(7, _flagsX.get(index));
				statement.setInt(8, _flagsY.get(index));
				statement.setInt(9, _flagsZ.get(index));
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.severe("Exception: CTF.saveData(): " + e.getMessage());
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
				if (!CTF._started)
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
					return true;
				}
				// this 1 is incase player got new objectid after DC or reconnect
				else if (player.getName().equals(eventPlayer.getName()))
				{
					_playersShuffle.remove(player);
					_playersShuffle.add(eventPlayer);
					eventPlayer._inEventCTF = true;
					eventPlayer._countCTFflags = 0;
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
					player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
					player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)));
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
		player._teamNameCTF = "";
		player._countCTFflags = 0;
		player._inEventCTF = false;
		player.setInstanceId(0);
		_players.remove(player);
		_playersShuffle.remove(player);
	}
	
	public static void cleanCTF()
	{
		_log.info("CTF : Cleaning players.");
		for (L2PcInstance player : _players)
		{
			if (player != null)
			{
				if (player._haveFlagCTF)
					removeFlagFromPlayer(player);
				else
					player.getInventory().destroyItemByItemId("", CTF._FLAG_IN_HAND_ITEM_ID, 1, player, null);
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
		for (String team : _teams)
		{
			int index = _teams.indexOf(team);
			_teamPointsCount.set(index, 0);
			_flagSpawns.set(index, null);
			_flagsTaken.set(index, false);
			_teamPlayersCount.set(index, 0);
			_teamPointsCount.set(index, 0);
		}
		_topScore = 0;
		_topTeam = "";
		_players = new FastList<L2PcInstance>();
		_playersShuffle = new FastList<L2PcInstance>();
		_savePlayers = new FastList<Integer>();
		_savePlayerTeams = new FastList<String>();
		_teamPointsCount = new FastList<Integer>();
		_flagSpawns = new FastList<L2Spawn>();
		_flagsTaken = new FastList<Boolean>();
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_teamPlayersCount = new FastList<Integer>();
		_log.info("Cleaning CTF done.");
		EventScheduler.getInstance().startNextEvent();
	}
	
	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;
		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	public static void teleportFinish()
	{
		AnnounceToPlayers(false, _eventName + "(CTF): Teleport back to participation NPC in 20 seconds!");
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
							player.teleToLocation(_npcX + Rnd.get(-600, 600), _npcY + Rnd.get(-600, 600), _npcZ, false);
							if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
							{
								player.getCounters().ctfPlayed++;
							}
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
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(NPC_BUFFER_ID);
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
		if (player._haveFlagCTF)
			player.removeCTFFlagOnDie();
		player.sendMessage("You will be revived and teleported to team spot in " + Config.CTF_REVIVE_DELAY / 1000 + " seconds");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (player._inEventCTF && _started)
				{
					if (player.isDead())
					{
						player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
						player.teleToLocation(_teamsX.get(_teams.indexOf(player._teamNameCTF)), _teamsY.get(_teams.indexOf(player._teamNameCTF)), _teamsZ.get(_teams.indexOf(player._teamNameCTF)), false);
						player.doRevive();
						player.eventTicker = 0;
					}
				}
			}
		}, Config.CTF_REVIVE_DELAY);
	}
}