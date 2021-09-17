package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class DM
{
protected static final Logger _log = Logger.getLogger(DM.class.getName());

public static String _eventName = "",
_eventDesc = "",
_joiningLocationName = "";
public static FastList<Integer> _savePlayers = new FastList<Integer>();
public static FastList<L2PcInstance> _players = new FastList<L2PcInstance>();
public static boolean _joining = false,
_teleport = false,
_started = false,
_sitForced = false;
public static L2Spawn _npcSpawn;
public static FastList<L2PcInstance> _winners = new FastList<L2PcInstance>();
public static FastList<L2PcInstance> _losers = new FastList<L2PcInstance>();
public static int _npcId = 0,
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
_minlvl = 0,
_maxlvl = 0,
_playerColors = 0,
_playerX = 0,
_playerY = 0,
_playerZ = 0,
_joinTime = 0,
_eventTime = 0;

public static boolean _running = false;

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
	
	_joining = true;
	spawnEventNpc();
	Announcements.getInstance().announceToAll(_eventName + "(DM): Joinable in " + _joiningLocationName + "!");
	Announcements.getInstance().announceToAll(_eventName + "(DM): Reward: " + _rewardAmount + " " + ItemTable.getInstance().getTemplate(_rewardId).getName());
}

private static boolean startJoinOk()
{
	return !(_started || _teleport || _joining || _eventName.isEmpty() ||
			_joiningLocationName.isEmpty() || _eventDesc.isEmpty() || _npcId == 0 ||
			_npcX == 0 || _npcY == 0 || _npcZ == 0 || _rewardId == 0 || _rewardAmount == 0 ||
			_playerX == 0 || _playerY == 0 || _playerZ == 0 || _joinTime == 0 || _eventTime == 0);
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
		_npcSpawn.setRespawnDelay(1);
		
		SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
		
		_npcSpawn.init();
		_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
		_npcSpawn.getLastSpawn().setTitle(_eventName);
		_npcSpawn.getLastSpawn()._isEventMobDM = true;
		_npcSpawn.getLastSpawn().isAggressive();
		_npcSpawn.getLastSpawn().decayMe();
		_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
		
		_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
}

public static boolean teleportStart()
{
	if (!_joining || _started || _teleport)
		return false;
	
	_joining = false;
	Announcements.getInstance().announceToAll(_eventName + "(DM): Teleport to team spot in 20 seconds!");
	
	setUserData();
	
	DoorTable.getInstance().getDoor(24190001).closeMe();
	DoorTable.getInstance().getDoor(24190002).closeMe();
	DoorTable.getInstance().getDoor(24190003).closeMe();
	DoorTable.getInstance().getDoor(24190004).closeMe();
	
	ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
	{
		public void run()
		{
			DM.sit();
			
			for (L2PcInstance player : DM._players)
			{
				if (player !=  null)
				{
					//Remove Summon's buffs
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
					
					player.doRevive();
					if (player.isTransformed())
						player.stopTransformation(null);
					player.teleToLocation(_playerX+Rnd.get(-650, 650), _playerY+Rnd.get(-650, 650), _playerZ, false);
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
	sit();
	Announcements.getInstance().announceToAll(_eventName + "(DM): Started. Go to kill your enemies!");
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
		Announcements.getInstance().announceToAll(_eventName + "(DM): No players win the match(nobody killed).");
	}
	else
	{
		try
		{
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(0) + " wins the match! " + _topKills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(1) + " is second! " + _top2Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(2) + " is third! " + _top3Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(3) + " is forth! " + _top4Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(4) + " is fifth! " + _top5Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(5) + " is sixth! " + _top6Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(6) + " is seventh! " + _top7Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): " + _winners.get(7) + " is eighth! " + _top8Kills + " kills.");
			Announcements.getInstance().announceToAll(_eventName + "(DM): All 8 players are rewarded!");
		}
		catch (Exception e)
		{
		}
		
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

//		for (L2PcInstance player : _players)
//		{
//			if (player != null && player._countDMkills < _top8Kills && !_losers.contains(player))
//			{
//				temp = player;
//			}
//		}
//		if (temp != null)
//			_losers.addLast(temp);
//		temp = null;
	}
}

public static void rewardPlayer()
{
	int count = 0;
	
	for (L2PcInstance player : _winners)
	{
		if (player != null && player.isOnline() == 1 && player._inEventDM)
		{
			player.addItem("DM Event: " + _eventName, _rewardId, _rewardAmount, player, true);
			
			NpcHtmlMessage nhm = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("");

			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				player.getCounters().dmWon++;
			}
			replyMSG.append("<html><body>You won the event. Look in your inventory for the reward.</body></html>");
			
			nhm.setHtml(replyMSG.toString());
			player.sendPacket(nhm);
			
			if (count <= 8)
				player.addItem("DM Event: " + _eventName, TvT.MEDAL_OF_VICTORY, 2, player, true);
			
			count++;
			
			if (count >= 8)
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
		//Announcements.getInstance().announceToAll("Checking loser : "+ player.getName());
		if (player != null && player.isOnline() == 1 && player._inEventDM && !_winners.contains(player))
		{

			//Announcements.getInstance().announceToAll(player.getName()+ ":True");
			curLoser++;

			player.addItem("DM Event: " + _eventName, _rewardId, _rewardAmount/2, player, true);
			player.addItem("DM Event: " + _eventName, 97000, 2, player, true);
			
			NpcHtmlMessage nhm = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("");
			
			replyMSG.append("<html><body>You lost the event.You earned half of the reward amount Look in your inventory for the reward.</body></html>");
			
			nhm.setHtml(replyMSG.toString());
			player.sendPacket(nhm);
			
		}

		if (curLoser >= countLosers)
		{
			//Announcements.getInstance().announceToAll("checked: "+ String.valueOf(curLoser)+ " players." );
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
		EventScheduler.getInstance().startNextEvent();
		return;
	}
	
	_joining = false;
	_teleport = false;
	_started = false;
	unspawnEventNpc();
	Announcements.getInstance().announceToAll(_eventName + "(DM): Match aborted!");
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
				
				/*player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);*/
				player.setIsParalyzed(true);
			}
			else
			{
				player.setIsParalyzed(false);
				/*player.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);*/
				
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
			_log.info("Name: " + player.getName()+ " kills :" + player._countDMkills);
	}
	
	_log.info("");
	_log.info("################################");
	_log.info("# _savePlayers(FastList<String>) #");
	_log.info("################################");
	
	/*	for (String player : _savePlayers)
		_log.info("Name: " + player );*/
	
	_log.info("");
	_log.info("");
}

public static void loadData(int id)
{
	_eventName = "";
	_eventDesc = "";
	_joiningLocationName = "";
	_savePlayers = new FastList<Integer>();
	_players = new FastList<L2PcInstance>();
	_winners = new FastList<L2PcInstance>();
	_npcSpawn = null;
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
	_playerColors = 0;
	_playerX = 0;
	_playerY = 0;
	_playerZ = 0;
	_joinTime = 0;
	_eventTime = 0;
	
	Connection con = null;
	try
	{
		PreparedStatement statement;
		ResultSet rs;
		
		con = L2DatabaseFactory.getInstance().getConnection();
		
		statement = con.prepareStatement("Select * from dm WHERE id=?");
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
			_rewardId = rs.getInt("rewardId");
			_rewardAmount = rs.getInt("rewardAmount");
			_playerColors = rs.getInt("color");
			_playerX = rs.getInt("playerX");
			_playerY = rs.getInt("playerY");
			_playerZ = rs.getInt("playerZ");
			_joinTime = rs.getInt("delay") * 60 * 1000;
			_eventTime = rs.getInt("time") * 60 * 1000;
		}
		statement.close();
	}
	catch (Exception e)
	{
		_log.severe("Exception: DM.loadData(): "+e);
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
		
		statement = con.prepareStatement("Delete from dm");
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("INSERT INTO dm (eventName, eventDesc, joiningLocation, minlvl, maxlvl, npcId, npcX, npcY, npcZ, rewardId, rewardAmount, color, playerX, playerY, playerZ, delay, time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		statement.setString(1, _eventName);
		statement.setString(2, _eventDesc);
		statement.setString(3, _joiningLocationName);
		statement.setInt(4, _minlvl);
		statement.setInt(5, _maxlvl);
		statement.setInt(6, _npcId);
		statement.setInt(7, _npcX);
		statement.setInt(8, _npcY);
		statement.setInt(9, _npcZ);
		statement.setInt(10, _rewardId);
		statement.setInt(11, _rewardAmount);
		statement.setInt(12, _playerColors);
		statement.setInt(13, _playerX);
		statement.setInt(14, _playerY);
		statement.setInt(15, _playerZ);
		statement.setInt(16, _joinTime/60/1000);
		statement.setInt(17, _eventTime/60/1000);
		
		statement.execute();
		statement.close();
	}
	catch (Exception e)
	{
		_log.severe("Exception: DM.saveData(): "+e);
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
		replyMSG.append("DM Match<br><br><br>");
		replyMSG.append("Current event...<br1>");
		replyMSG.append("	... name:&nbsp;<font color=\"00FF00\">" + _eventName + "</font><br1>");
		replyMSG.append("	... description:&nbsp;<font color=\"00FF00\">" + _eventDesc + "</font><br><br>");
		
		if (!_started && !_joining)
			replyMSG.append("<center>Wait till the admin/gm start the participation.</center>");
		else if (!_started && _joining && eventPlayer.getLevel()>=_minlvl && eventPlayer.getLevel()<_maxlvl)
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
		else if (eventPlayer.getLevel()<_minlvl || eventPlayer.getLevel()>_maxlvl )
		{
			replyMSG.append("Your lvl : <font color=\"00FF00\">" + eventPlayer.getLevel() +"</font><br>");
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
		_log.severe("DM Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception"+e);
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
		
		if(player == null || eventPlayer == null)
			continue;

		if (eventPlayer instanceof Ghost || eventPlayer instanceof Ghost)
			continue;
		String HWID = player.getClient().getFullHwid();
		String HWID2 = eventPlayer.getClient().getFullHwid();
		
		if(HWID == null || HWID2 == null)
			continue;
		
		if(HWID.equalsIgnoreCase(HWID2))
			return false;
	}
	
	return true;
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
			}				//check by name incase player got new objectId
			else if (p.getObjectId() == player.getObjectId())
			{
				player._originalKarmaDM = player.getKarma();
				player._inEventDM = true;
				player._countDMkills = p._countDMkills;
				
				_players.remove(p); //removing old object id from FastList
				_players.add(player); //adding new objectId to FastList
				
				player.setKarma(0);
				player.setIsPendingRevive(true);
				player.teleToLocation(_playerX+Rnd.get(-650, 650), _playerY+Rnd.get(-650, 650), _playerZ, false);
				
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
		
		long startOneSecondWaiterStartTime = System.currentTimeMillis();
		
		// only the try catch with Thread.sleep(1000) give bad countdown on high wait times
		while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException ie)
			{
			}
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
	{
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

public static void removePlayer(L2PcInstance player)
{
	if (player != null)
	{
		_players.remove(player);
		_savePlayers.remove((Integer)player.getObjectId());
	}
}

public static void cleanDM()
{
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
	Announcements.getInstance().announceToAll(_eventName + "(DM): Teleport back to participation NPC in 20 seconds!");
	
	removeUserData();
	ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
	{
		public void run()
		{
			for (L2PcInstance player : _players)
			{
				if (player !=  null && player.isOnline()!=0)
				{
					player.doRevive();
					player.teleToLocation(_npcX+Rnd.get(-600, 600), _npcY+Rnd.get(-600, 600), _npcZ, false);

					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().dmPlayed++;
					}
				}
			}
			cleanDM();
		}
	}, 20000);
}

final public static void onDeath(final L2PcInstance player, final L2Character killa)
{
	if (player == null || player.isOnline() == 0) return;
	
	final L2PcInstance killer = killa.getActingPlayer();
	
	if (killer != null && killer._inEventDM)
		killer._countDMkills++;
		
	player.sendMessage("You will be revived and teleported to spot in " + Config.DM_REVIVE_DELAY / 1000 + " seconds!");
	ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
	{
		public void run()
		{
			if (player._inEventDM && _started)
			{
				if (player.isDead())
				{
					player.teleToLocation(_playerX+Rnd.get(-750, 750), _playerY+Rnd.get(-750, 750), _playerZ, false);
					player.doRevive();
				}
			}
		}
	}, Config.DM_REVIVE_DELAY);
}
}