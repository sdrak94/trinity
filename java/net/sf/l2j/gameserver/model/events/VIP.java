package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.events.FOS.SiegeStatComparator;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.util.Rnd;

public class VIP
{
	protected static final Logger			_log				= Logger.getLogger(VIP.class.getName());
	public static String					_teamName			= "", _joinArea = "", _theVIPName = "";
	public static int						_time				= 0, _winners = 0, _minPlayers = Config.VIP_MIN_PARTICIPANTS,
	_vipReward = 0, _vipRewardAmount = 0,
	_notVipReward = 0, _notVipRewardAmount = 0,
	_theVipReward = 0, _theVipRewardAmount = 0,
	_endNPC = 0, _joinNPC = 0,
	_delay = 0,
	_endX = 0, _endY = 0, _endZ = 0,
	_startX = 0, _startY = 0, _startZ = 0,
	_joinX = 0, _joinY = 0, _joinZ = 0,
	_team = 0;																													// Human = 1
	// Elf = 2
	// Dark = 3
	// Orc = 4
	// Dwarf = 5
	// kamael = 6
	public static boolean					_started			= false,
	_joining = false,
	_sitForced = false;
	public static L2Spawn					_endSpawn, _joinSpawn;
	public static FastList<Integer>			_savePlayers		= new FastList<Integer>();
	public static FastList<L2PcInstance>	_playersVIP			= new FastList<L2PcInstance>(),
	_playersNotVIP = new FastList<L2PcInstance>();
	private static FastList<L2PcInstance>	earlyBirdPlayers	= new FastList<L2PcInstance>();
	
	public static void setTeam(String team, L2PcInstance activeChar)
	{
		if (team.compareToIgnoreCase("Human") == 0)
		{
			_team = 1;
			_teamName = "Human";
		}
		else if (team.compareToIgnoreCase("Elf") == 0)
		{
			_team = 2;
			_teamName = "Elf";
		}
		else if (team.compareToIgnoreCase("Dark") == 0)
		{
			_team = 3;
			_teamName = "Dark Elf";
		}
		else if (team.compareToIgnoreCase("Orc") == 0)
		{
			_team = 4;
			_teamName = "Orc";
		}
		else if (team.compareToIgnoreCase("Dwarf") == 0)
		{
			_team = 5;
			_teamName = "Dwarf";
		}
		else if (team.compareToIgnoreCase("Kamael") == 0)
		{
			_team = 6;
			_teamName = "Kamael";
		}
		else
		{
			activeChar.sendMessage("Invalid Team Name: //vip_setteam <human/elf/dark/orc/dwarf/kamael>");
			return;
		}
		setLoc();
	}
	
	public static void setTeam(String team)
	{
		if (team.compareToIgnoreCase("Human") == 0)
		{
			_team = 1;
			_teamName = "Human";
		}
		else if (team.compareToIgnoreCase("Elf") == 0)
		{
			_team = 2;
			_teamName = "Elf";
		}
		else if (team.compareToIgnoreCase("Dark") == 0)
		{
			_team = 3;
			_teamName = "Dark Elf";
		}
		else if (team.compareToIgnoreCase("Orc") == 0)
		{
			_team = 4;
			_teamName = "Orc";
		}
		else if (team.compareToIgnoreCase("Dwarf") == 0)
		{
			_team = 5;
			_teamName = "Dwarf";
		}
		else if (team.compareToIgnoreCase("Kamael") == 0)
		{
			_team = 6;
			_teamName = "Kamael";
		}
		setLoc();
	}
	
	private static void spawnBuffers()
	{
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(CTF.NPC_BUFFER_ID);
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(_startX + 10);
			spawn.setLocy(_startY + 20);
			spawn.setLocz(_startZ + 15);
			spawn.setAmount(1);
			spawn.setHeading(0);
			spawn.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
			SpawnTable.getInstance().addNewSpawn(spawn, false);
			spawn.init();
			spawn.startUnspawnTimer(_time / 1000);
			final L2Spawn spawn2 = new L2Spawn(template);
			spawn2.setLocx(_endX + 10);
			spawn2.setLocy(_endY + 20);
			spawn2.setLocz(_endZ + 15);
			spawn2.setAmount(1);
			spawn2.setHeading(0);
			spawn2.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
			SpawnTable.getInstance().addNewSpawn(spawn2, false);
			spawn2.init();
			spawn2.startUnspawnTimer(_time / 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * @param activeChar
	 */
	public static void setRandomTeam(L2PcInstance activeChar)
	{
		int random = Rnd.nextInt(6) + 1; // (0 - 4) + 1
		switch (random)
		{
			case 1:
				_team = 1;
				_teamName = "Human";
				setLoc();
				break;
			case 2:
				_team = 2;
				_teamName = "Elf";
				setLoc();
				break;
			case 3:
				_team = 3;
				_teamName = "Dark Elf";
				setLoc();
				break;
			case 4:
				_team = 4;
				_teamName = "Orc";
				setLoc();
				break;
			case 5:
				_team = 5;
				_teamName = "Dwarf";
				setLoc();
				break;
			case 6:
				_team = 6;
				_teamName = "Kamael";
				setLoc();
				break;
			default:
				break;
		}
	}
	
	public static void setAutoRandomTeam()
	{
		int random = Rnd.nextInt(6) + 1; // (0 - 4) + 1
		switch (random)
		{
			case 1:
				_team = 1;
				_teamName = "Human";
				setLoc();
				break;
			case 2:
				_team = 2;
				_teamName = "Elf";
				setLoc();
				break;
			case 3:
				_team = 3;
				_teamName = "Dark Elf";
				setLoc();
				break;
			case 4:
				_team = 4;
				_teamName = "Orc";
				setLoc();
				break;
			case 5:
				_team = 5;
				_teamName = "Dwarf";
				setLoc();
				break;
			case 6:
				_team = 6;
				_teamName = "Kamael";
				setLoc();
				break;
			default:
				break;
		}
	}
	
	public static void setLoc()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM VIPinfo WHERE teamID = " + _team);
			ResultSet rset = statement.executeQuery();
			rset.next();
			_endX = rset.getInt("endx");
			_endY = rset.getInt("endy");
			_endZ = rset.getInt("endz");
			_startX = rset.getInt("startx");
			_startY = rset.getInt("starty");
			_startZ = rset.getInt("startz");
			_joinX = rset.getInt("joinx");
			_joinY = rset.getInt("joiny");
			_joinZ = rset.getInt("joinz");
			_time = rset.getInt("time") * 60 * 1000;
			_delay = rset.getInt("delay") * 60 * 1000;
			_vipReward = rset.getInt("reward");
			_theVipReward = _vipReward;
			_vipRewardAmount = rset.getInt("rewardAmount");
			_notVipReward = rset.getInt("notVIPreward");
			_notVipRewardAmount = rset.getInt("notVIPrewardAmount");
			_theVipRewardAmount = rset.getInt("THEVIPrewardAmount");
			_joinArea = rset.getString("joinName");
			_joinNPC = rset.getInt("joinNPC");
			_endNPC = rset.getInt("endNPC");
			joinNPC(_joinNPC);
			endNPC(_endNPC);
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.severe("Could not check End & Start LOC for team" + _team + " got: " + e);
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
	
	public static void endNPC(int npcId, L2PcInstance activeChar)
	{
		if (_team == 0)
		{
			activeChar.sendMessage("Please select a team first");
			return;
		}
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(npcId);
		_endNPC = npcId;
		try
		{
			_endSpawn = new L2Spawn(npctmp);
			_endSpawn.setLocx(_endX);
			_endSpawn.setLocy(_endY);
			_endSpawn.setLocz(_endZ);
			_endSpawn.setAmount(1);
			_endSpawn.setHeading(activeChar.getHeading());
			_endSpawn.setRespawnDelay(1);
			_endSpawn.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
		}
		catch (Exception e)
		{
			activeChar.sendMessage("VIP Engine[endNPC(" + activeChar.getName() + ")]: exception: " + e.getMessage());
		}
	}
	
	public static void endNPC(int npcId)
	{
		if (_team == 0)
		{
			return;
		}
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(npcId);
		_endNPC = npcId;
		try
		{
			_endSpawn = new L2Spawn(npctmp);
			_endSpawn.setLocx(_endX);
			_endSpawn.setLocy(_endY);
			_endSpawn.setLocz(_endZ);
			_endSpawn.setAmount(1);
			_endSpawn.setHeading(0);
			_endSpawn.setRespawnDelay(1);
			_endSpawn.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
		}
		catch (Exception e)
		{
			_log.severe("VIP Engine[endNPC]: exception: " + e);
		}
	}
	
	public static void joinNPC(int npcId, L2PcInstance activeChar)
	{
		if (_joinX == 0)
		{
			activeChar.sendMessage("Please set a join x,y,z first");
			return;
		}
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(npcId);
		_joinNPC = npcId;
		try
		{
			_joinSpawn = new L2Spawn(npctmp);
			_joinSpawn.setLocx(_joinX);
			_joinSpawn.setLocy(_joinY);
			_joinSpawn.setLocz(_joinZ);
			_joinSpawn.setAmount(1);
			_joinSpawn.setHeading(activeChar.getHeading());
			_joinSpawn.setRespawnDelay(1);
		}
		catch (Exception e)
		{
			activeChar.sendMessage("VIP Engine[joinNPC(" + activeChar.getName() + ")]: exception: " + e.getMessage());
		}
	}
	
	public static void joinNPC(int npcId)
	{
		if (_joinX == 0)
		{
			return;
		}
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(npcId);
		_joinNPC = npcId;
		try
		{
			_joinSpawn = new L2Spawn(npctmp);
			_joinSpawn.setLocx(_joinX);
			_joinSpawn.setLocy(_joinY);
			_joinSpawn.setLocz(_joinZ);
			_joinSpawn.setAmount(1);
			_joinSpawn.setHeading(0);
			_joinSpawn.setRespawnDelay(1);
		}
		catch (Exception e)
		{
			_log.severe("VIP Engine[joinNPC]: exception: " + e);
		}
	}
	
	public static void spawnEndNPC()
	{
		try
		{
			SpawnTable.getInstance().addNewSpawn(_endSpawn, false);
			_endSpawn.init();
			_endSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_endSpawn.getLastSpawn().setTitle("VIP Npc");
			_endSpawn.getLastSpawn()._isEventVIPNPCEnd = true;
			_endSpawn.getLastSpawn().isAggressive();
			_endSpawn.getLastSpawn().decayMe();
			_endSpawn.getLastSpawn().spawnMe(_endSpawn.getLastSpawn().getX(), _endSpawn.getLastSpawn().getY(), _endSpawn.getLastSpawn().getZ());
			_endSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_endSpawn.getLastSpawn(), _endSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("VIP Engine[spawnEndNPC()]: exception: " + e);
		}
	}
	
	public static void spawnJoinNPC()
	{
		try
		{
			SpawnTable.getInstance().addNewSpawn(_joinSpawn, false);
			_joinSpawn.init();
			_joinSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_joinSpawn.getLastSpawn().setTitle("VIP Npc");
			_joinSpawn.getLastSpawn()._isEventVIPNPC = true;
			_joinSpawn.getLastSpawn().isAggressive();
			_joinSpawn.getLastSpawn().decayMe();
			_joinSpawn.getLastSpawn().spawnMe(_joinSpawn.getLastSpawn().getX(), _joinSpawn.getLastSpawn().getY(), _joinSpawn.getLastSpawn().getZ());
			_joinSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_joinSpawn.getLastSpawn(), _joinSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.severe("VIP Engine[spawnJoinNPC()]: exception: " + e);
		}
	}
	
	/**
	 * @param id
	 * @param activeChar
	 */
	public static String getNPCName(int id, L2PcInstance activeChar)
	{
		if (id == 0)
			return "";
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(id);
		if (npctmp == null)
		{
			if (activeChar != null)
				activeChar.sendMessage("VIP Engine[joinNPC(" + activeChar.getName() + ")]: exception: wrong NPC Id");
			return "";
		}
		return npctmp.getName();
	}
	
	public static String getNPCName(int id)
	{
		if (id == 0)
			return "";
		L2NpcTemplate npctmp = NpcTable.getInstance().getTemplate(id);
		if (npctmp == null)
			return "";
		return npctmp.getName();
	}
	
	/**
	 * @param id
	 * @param activeChar
	 */
	public static String getItemName(int id, L2PcInstance activeChar)
	{
		if (id == 0)
			return "";
		L2Item itemtmp = ItemTable.getInstance().getTemplate(id);
		if (itemtmp == null)
		{
			activeChar.sendMessage("VIP Engine[joinNPC(" + activeChar.getName() + ")]: exception: wrong item Id");
			return "";
		}
		return itemtmp.getName();
	}
	
	public static String getItemName(int id)
	{
		if (id == 0)
			return "";
		L2Item itemtmp = ItemTable.getInstance().getTemplate(id);
		if (itemtmp == null)
			return "";
		return itemtmp.getName();
	}
	
	public static void setJoinLOC(String x, String y, String z)
	{
		_joinX = Integer.valueOf(x);
		_joinY = Integer.valueOf(y);
		_joinZ = Integer.valueOf(z);
	}
	
	public static void startJoin(L2PcInstance activeChar)
	{
		if (_teamName.isEmpty() || _joinArea.isEmpty() || _time == 0 || _vipReward == 0 || _vipRewardAmount == 0 || _notVipReward == 0 || _notVipRewardAmount == 0 || _theVipReward == 0 || _theVipRewardAmount == 0 || _endNPC == 0 || _joinNPC == 0 || _delay == 0 || _endX == 0 || _endY == 0 || _endZ == 0 || _startX == 0 || _startY == 0 || _startZ == 0 || _joinX == 0 || _joinY == 0 || _joinZ == 0 || _team == 0)
		{
			if (activeChar != null)
				activeChar.sendMessage("Cannot initiate join status of event, not all the values are filled in");
			return;
		}
		if (_joining)
		{
			if (activeChar != null)
				activeChar.sendMessage("Players are already allowed to join the event");
			return;
		}
		if (_started)
		{
			if (activeChar != null)
				activeChar.sendMessage("Event already started. Please wait for it to finish or finish it manually");
			return;
		}
		_joining = true;
		earlyBirdPlayers.clear();
		Announcements.getInstance().announceToAll("Attention all players. A VIP event is about to start in " + _delay / 1000 / 60 + " mins!");
		Announcements.getInstance().announceToAll("In this event the " + _teamName + " characters must safely escort a VIP player from one location to their starter town");
		Announcements.getInstance().announceToAll("Players will automatically be assigned to their respective teams");
		Announcements.getInstance().announceToAll("Please find " + getNPCName(VIP._joinNPC, activeChar) + " located in " + _joinArea + " to sign up.");
		spawnJoinNPC();
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				_joining = false;
				checkForSameIP();
				_started = true;
				startEvent();
			}
		}, _delay);
	}
	
	public static void startAutoJoin()
	{
		startJoin(null);
	}
	
	public static void startEvent()
	{
		if ((_playersVIP.size() + _playersNotVIP.size()) < _minPlayers)
		{
			Announcements.getInstance().announceToAll("Registration for the VIP event involving " + _teamName + " has ended.");
			Announcements.getInstance().announceToAll("Event aborted due not enought players : min players requested for event " + _minPlayers);
			_started = false;
			spawnEndNPC();
			unspawnEventNpcs();
			VIP.clean();
		}
		else
		{
			Announcements.getInstance().announceToAll("Registration for the VIP event involving " + _teamName + " has ended.");
			Announcements.getInstance().announceToAll("Players will be teleported to their locations in 20 seconds.");
			TvT.lastEventTime = System.currentTimeMillis();
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					teleportPlayers();
					chooseVIP();
					setUserData();
					spawnBuffers();
					spawnEndNPC();
					buffVIPs();
					Announcements.getInstance().announceToAll("Players have been teleported for the VIP event.");
					Announcements.getInstance().announceToAll("VIP event will start in 20 seconds.");
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						public void run()
						{
							Announcements.getInstance().announceToAll("VIP event has started. " + _teamName + "'s VIP must get to the starter city and talk with " + getNPCName(_endNPC, null) + ". The opposing team must kill the VIP. All players except the VIP will respawn at their current locations.");
							Announcements.getInstance().announceToAll("VIP event will end if the " + _teamName + " team makes it to their town or when " + _time / 1000 / 60 + " mins have elapsed.");
							VIP.sit();
							ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
							{
								public void run()
								{
									endEventTime();
								}
							}, _time);
						}
					}, 20000);
				}
			}, 20000);
		}
	}
	
	private static void buffVIPs()
	{
		if ((double) _playersVIP.size() / _playersNotVIP.size() > 0.5)
			return;
		final L2Skill skill = SkillTable.getInstance().getInfo(16000, 1);
		if (skill == null)
			return;
		for (L2PcInstance player : _playersVIP)
		{
			if (player != null && player.isOnline() > 0)
			{
				skill.getEffects(player, player);
				player.sendMessage("You have been buffed for this event");
			}
		}
	}
	
	public static void vipDied()
	{
		if (!_started)
		{
			_log.info("Could not finish the event. Event not started or event ended prematurly.");
			return;
		}
		_started = false;
		unspawnEventNpcs();
		Announcements.getInstance().announceToAll("The VIP has died. The opposing team has won.");
		rewardNotVIP();
		teleportFinish();
	}
	
	public static void endEventTime()
	{
		if (!_started)
		{
			_log.info("Could not finish the event. Event not started or event ended prematurly (VIP died)");
			return;
		}
		_started = false;
		unspawnEventNpcs();
		Announcements.getInstance().announceToAll("The time has run out and the " + _teamName + "'s have not made it to their goal. Everybody on the opposing team wins.");
		rewardNotVIP();
		teleportFinish();
	}
	
	public static void unspawnEventNpcs()
	{
		if (_endSpawn != null)
		{
			_endSpawn.getLastSpawn().deleteMe();
			_endSpawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(_endSpawn, true);
		}
		if (_joinSpawn != null)
		{
			_joinSpawn.getLastSpawn().deleteMe();
			_joinSpawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(_joinSpawn, true);
		}
	}
	
	public static void showEndHTML(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("VIP (End NPC)<br><br>");
			replyMSG.append("Current event...<br1>");
			replyMSG.append("    ... Team:&nbsp;<font color=\"FFFFFF\">" + _teamName + "</font><br><br>");
			if (!_started)
				replyMSG.append("<center>Please wait until the admin/gm starts the joining period.</center>");
			else if (eventPlayer._isTheVIP)
			{
				replyMSG.append("You have made it to the end. All you have to do is hit the finish button to reward yourself and your team. Congrats!<br>");
				replyMSG.append("<center>");
				replyMSG.append("<button value=\"Finish\" action=\"bypass -h npc_" + objectId + "_vip_finishVIP\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				replyMSG.append("</center>");
			}
			else
			{
				replyMSG.append("I am the character the VIP has to reach in order to win the event.<br>");
			}
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{}
	}
	
	/**
	 * @param activeChar
	 */
	public static void vipWin(L2PcInstance activeChar)
	{
		if (!_started)
		{
			_log.info("Could not finish the event. Event not started or event ended prematurly");
			return;
		}
		_started = false;
		unspawnEventNpcs();
		Announcements.getInstance().announceToAll("The VIP has made it to the goal. " + _teamName + " has won. Everybody on that team wins.");
		rewardVIP();
		teleportFinish();
	}
	
	public static void rewardNotVIP()
	{
		int rewardAmount = _notVipRewardAmount;
		for (L2PcInstance player : _playersNotVIP)
		{
			if (player != null && player.isOnline() != 0 && player._inEventVIP)
			{
				if (earlyBirdPlayers.contains(player))
				{
					rewardAmount = (int) (_notVipRewardAmount * 1.25);
					if (rewardAmount == _notVipRewardAmount)
						rewardAmount++;
					player.sendMessage("You received 25% more reward for being early to the event.");
				}
				else
					rewardAmount = _notVipRewardAmount;
				player.addItem("VIP Event: ", _notVipReward, rewardAmount, player, true);
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");
				replyMSG.append("<html><body>Your team won the event. Your inventory now contains your reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
				player.addItem("VIP Event: ", TvT.MEDAL_OF_VICTORY, 1, player, true);
			}
		}
	}
	
	public static void rewardVIP()
	{
		int rewardAmount = _vipRewardAmount;
		for (L2PcInstance player : _playersVIP)
		{
			if (player == null || player.isOnline() == 0 || !player._inEventVIP)
				continue;
			if (!player._isTheVIP)
			{
				if (earlyBirdPlayers.contains(player))
				{
					rewardAmount = (int) (_vipRewardAmount * 1.25);
					if (rewardAmount == _vipRewardAmount)
						rewardAmount++;
					player.sendMessage("You received 25% more reward for being early to the event.");
				}
				else
					rewardAmount = _vipRewardAmount;
				player.addItem("VIP Event: ", _vipReward, rewardAmount, player, true);
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");
				replyMSG.append("<html><body>Your team has won the event. Your inventory now contains your reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
			}
			else if (player._isTheVIP)
			{
				if (earlyBirdPlayers.contains(player))
				{
					rewardAmount = (int) (_theVipRewardAmount * 1.25);
					if (rewardAmount == _theVipRewardAmount)
						rewardAmount++;
					player.sendMessage("You received 25% more reward for being early to the event.");
				}
				else
					rewardAmount = _theVipRewardAmount;
				player.addItem("VIP Event: ", _theVipReward, rewardAmount, player, true);
				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");
				replyMSG.append("<html><body>Your team has won the event. Your inventory now contains your reward.</body></html>");
				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);
				player.addItem("VIP Event: ", TvT.MEDAL_OF_VICTORY, 1, player, true);
			}
			player.addItem("VIP Event: ", TvT.MEDAL_OF_VICTORY, 1, player, true);
		}
	}
	
	public static void teleportFinish()
	{
		Announcements.getInstance().announceToAll("Teleporting VIP players back to the Registration area in 20 seconds.");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				for (L2PcInstance player : _playersVIP)
				{
					if (player != null && player.isOnline() == 1)
					{
						player.stopSkillEffects(16000);
						player.doRevive();
						player.setInstanceId(0);
						player.teleToLocation(_joinX + Rnd.get(-600, 600), _joinY + Rnd.get(-600, 600), _joinZ, false);
					}
				}
				for (L2PcInstance player : _playersNotVIP)
				{
					if (player != null && player.isOnline() == 1)
					{
						player.doRevive();
						player.setInstanceId(0);
						player.teleToLocation(_joinX + Rnd.get(-600, 600), _joinY + Rnd.get(-600, 600), _joinZ, false);
					}
				}
				VIP.clean();
			}
		}, 20000);
	}
	
	public static void clean()
	{
		_time = _winners = _endNPC = _joinNPC = _delay = _endX = _endY = _endZ = _startX = _startY = _startZ = _joinX = _joinY = _joinZ = _team = 0;
		_vipReward = _vipRewardAmount = _notVipReward = _notVipRewardAmount = _theVipReward = _theVipRewardAmount = 0;
		_started = _joining = _sitForced = false;
		_teamName = _joinArea = _theVIPName = "";
		for (L2PcInstance player : _playersVIP)
		{
			player.stopSkillEffects(16000);
			player.setKarma(player._originalKarmaVIP);
			player.broadcastUserInfo();
			player._inEventVIP = false;
			player._isTheVIP = false;
			player._isNotVIP = false;
			player._isVIP = false;
		}
		for (L2PcInstance player : _playersNotVIP)
		{
			player.setKarma(player._originalKarmaVIP);
			player.broadcastUserInfo();
			player._inEventVIP = false;
			player._isTheVIP = false;
			player._isNotVIP = false;
			player._isVIP = false;
		}
		_savePlayers = new FastList<Integer>();
		earlyBirdPlayers = new FastList<L2PcInstance>();
		_playersVIP = new FastList<L2PcInstance>();
		_playersNotVIP = new FastList<L2PcInstance>();
	}
	
	public static void chooseVIP()
	{
		FastList<L2PcInstance> sortedPlayers = new FastList<L2PcInstance>();
		sortedPlayers.addAll(_playersVIP);
		Collections.sort(sortedPlayers, new SiegeStatComparator());
		for (L2PcInstance player : sortedPlayers)
		{
			if (player == null || player.isOnline() == 0 || !player._inEventVIP)
				continue;
			player._isTheVIP = true;
			_theVIPName = player.getName();
			break;
		}
	}
	
	public static void teleportPlayers()
	{
		VIP.sit();
		for (L2PcInstance player : _playersVIP)
		{
			if (player != null && player.isOnline() == 1)
			{
				if (player.isDead())
					player.doRevive();
				if (player.isTransformed())
					player.stopTransformation(null);
				player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				player.teleToLocation(_startX, _startY, _startZ);
			}
		}
		for (L2PcInstance player : _playersNotVIP)
		{
			if (player != null && player.isOnline() == 1)
			{
				if (player.isDead())
					player.doRevive();
				if (player.isTransformed())
					player.stopTransformation(null);
				player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				player.teleToLocation(_endX, _endY, _endZ);
			}
		}
	}
	
	public static void sit()
	{
		_sitForced = !_sitForced;
		for (L2PcInstance player : _playersVIP)
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
					player.setIsParalyzed(false);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
				}
			}
		}
		for (L2PcInstance player : _playersNotVIP)
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
					player.setIsParalyzed(false);
					player.stopAbnormalEffect(AbnormalEffect.HOLD_1);
				}
			}
		}
	}
	
	public static void setUserData()
	{
		for (L2PcInstance player : _playersVIP)
		{
			if (player.isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
			player.setKarma(0);
			player.broadcastUserInfo();
			if (Config.VIP_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
			}
			player.startKickFromEventTask();
		}
		for (L2PcInstance player : _playersNotVIP)
		{
			if (player.isCursedWeaponEquipped())
				CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
			player.setKarma(0);
			player.broadcastUserInfo();
			if (Config.VIP_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
			}
			player.startKickFromEventTask();
		}
	}
	
	public static void showJoinHTML(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");
			replyMSG.append("VIP (Join NPC)<br><br>");
			replyMSG.append("Current event...<br1>");
			replyMSG.append("    ... Team:&nbsp;<font color=\"FFFFFF\">" + _teamName + "</font><br><br>");
			if (!_joining && !_started) // PreEvent
				replyMSG.append("<center>Please wait until the admin/gm starts the joining period.</center>");
			else if (_joining && !_started) // Joining period
			{
				if (_playersVIP.contains(eventPlayer) || _playersNotVIP.contains(eventPlayer))
				{
					replyMSG.append("You are already on a team<br><br>");
				}
				else if (eventPlayer.getLevel() < Config.VIP_MIN_LEVEL || eventPlayer.getLevel() > Config.VIP_MAX_LEVEL)
				{
					replyMSG.append("Your level : <font color=\"00FF00\">" + eventPlayer.getLevel() + "</font><br>");
					replyMSG.append("Min level : <font color=\"00FF00\">" + Config.VIP_MIN_LEVEL + "</font><br>");
					replyMSG.append("Max level : <font color=\"00FF00\">" + Config.VIP_MAX_LEVEL + "</font><br><br>");
					replyMSG.append("<font color=\"FFFF00\">You can't participate in this event.</font><br>");
				}
				else
				{
					replyMSG.append("You want to participate in the event?<br><br>");
					if (eventPlayer.getRace().getRealOrdinal() == Race.Human.ordinal() && _team == 1)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else if (eventPlayer.getRace() == Race.Elf && _team == 2)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else if (eventPlayer.getRace() == Race.DarkElf && _team == 3)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else if (eventPlayer.getRace().getRealOrdinal() == Race.Orc.ordinal() && _team == 4)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else if (eventPlayer.getRace() == Race.Dwarf && _team == 5)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else if (eventPlayer.getRace() == Race.Kamael && _team == 6)
					{
						replyMSG.append("It seems you are on the VIP race! Be prepared to protect the VIP when it is decided<br1>");
						replyMSG.append("The VIP will be decided on when the event starts. It's completely random.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
					else
					{
						replyMSG.append("It seems you are not on the part of the VIP race.<br>");
						replyMSG.append("When the event starts you will be teleported to the " + _teamName + " town<br1>");
						replyMSG.append("Be sure to cooperate with your team to destroy the VIP.<br1>");
						replyMSG.append("The VIP will be announced when the event starts.<br>");
						replyMSG.append("<center>");
						replyMSG.append("<button value=\"Join\" action=\"bypass -h npc_" + objectId + "_vip_joinNotVIPTeam\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
						replyMSG.append("</center>");
					}
				}
			}
			else /* if (_started) */ // Event already Started
				replyMSG.append("<center>The event is already taking place. Please sign up for the next event.</center>");
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{}
	}
	
	public static void addPlayerVIP(L2PcInstance activeChar)
	{
		if (Olympiad.getInstance().isRegistered(activeChar))
		{
			return;
		}
		if (activeChar._inEventVIP)
		{
			activeChar.sendMessage("You are already participating in the event!");
			return;
		}
		if (activeChar._inEventTvT || activeChar._inEventDM || activeChar._inEventCTF)
		{
			activeChar.sendMessage("You are already participating in another event!");
			return;
		}
		activeChar._isVIP = true;
		_playersVIP.add(activeChar);
		_savePlayers.add(activeChar.getObjectId());
		activeChar._originalKarmaVIP = activeChar.getKarma();
		activeChar._inEventVIP = true;
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(activeChar);
			activeChar.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
	}
	
	public static void addPlayerNotVIP(L2PcInstance activeChar)
	{
		if (Olympiad.getInstance().isRegistered(activeChar))
		{
			return;
		}
		if (activeChar._inEventVIP)
		{
			activeChar.sendMessage("You are already participating in the event!");
			return;
		}
		if (activeChar._inEventTvT || activeChar._inEventDM || activeChar._inEventCTF)
		{
			activeChar.sendMessage("You are already participating in another event!");
			return;
		}
		activeChar._isNotVIP = true;
		_playersNotVIP.add(activeChar);
		_savePlayers.add(activeChar.getObjectId());
		activeChar._originalKarmaVIP = activeChar.getKarma();
		activeChar._inEventVIP = true;
		if (earlyBirdPlayers.size() < 15)
		{
			earlyBirdPlayers.add(activeChar);
			activeChar.sendMessage("Since you're one of the first 16 people to join this event, you'll be given 25% more event reward.");
		}
	}
	
	public static void addDisconnectedPlayer(L2PcInstance player)
	{
		boolean isInOly = false;
		if (Olympiad.getInstance().isRegistered(player))
		{
			isInOly = true;
		}
		for (L2PcInstance p : _playersVIP)
		{
			if (p == null)
				continue;
			else if (p.getObjectId() == player.getObjectId())
			{
				if (isInOly)
				{
					_playersVIP.remove(p);
					return;
				}
				player._isVIP = true;
				player._originalKarmaVIP = player.getKarma();
				player._inEventVIP = true;
				_playersVIP.remove(p); // removing old object id from FastList
				_playersVIP.add(player); // adding new objectId to FastList
				if (_started)
				{
					if (Config.VIP_ON_START_REMOVE_ALL_EFFECTS)
					{
						player.stopAllEffects();
					}
					if (p._isTheVIP || _theVIPName == player.getName())
						player._isTheVIP = true;
					player.setKarma(0);
					player.setIsPendingRevive(true);
					player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
					player.teleToLocation(_startX, _startY, _startZ);
					player.startKickFromEventTask();
				}
				return;
			}
		}
		for (L2PcInstance p : _playersNotVIP)
		{
			if (p == null)
				continue;
			// check by name incase player got new objectId
			else if (p.getObjectId() == player.getObjectId())
			{
				if (isInOly)
				{
					_playersNotVIP.remove(p);
					return;
				}
				player._isNotVIP = true;
				player._originalKarmaVIP = player.getKarma();
				player._inEventVIP = true;
				_playersNotVIP.remove(p); // removing old object id from FastList
				_playersNotVIP.add(player); // adding new objectId to FastList
				if (_started)
				{
					if (Config.VIP_ON_START_REMOVE_ALL_EFFECTS)
					{
						player.stopAllEffects();
					}
					player.setKarma(0);
					player.setIsPendingRevive(true);
					player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
					player.teleToLocation(_endX, _endY, _endZ);
					player.startKickFromEventTask();
				}
				return;
			}
		}
		_savePlayers.remove((Integer) player.getObjectId());
	}
	
	public static void removePlayer(L2PcInstance player)
	{
		if (player == null)
			return;
		player._inEventVIP = false;
		if (!_joining)
		{
			player.setKarma(player._originalKarmaVIP);
			player.broadcastUserInfo();
		}
		_playersNotVIP.remove(player);
		_playersVIP.remove(player);
		_savePlayers.remove((Integer) player.getObjectId());
		if (player.getInstanceId() == FOS.SIEGE_EVENT_INSTANCE_ID)
			player.setInstanceId(0);
	}
	
	private final static void checkForSameIP()
	{
		try
		{
			for (L2PcInstance player : _playersVIP)
			{
				if (player == null || player.isOnline() == 0 || player.isInJail())
				{
					try
					{
						removePlayer(player);
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
					removePlayer(player);
					continue;
				}
				for (L2PcInstance same : _playersVIP)
				{
					if (same == null || same.isOnline() == 0 || same.isInJail())
					{
						removePlayer(same);
						continue;
					}
					if (same == player)
						continue; // lolz checks if it's actually the same char
					final String sameIP = same.getIP();
					if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
					{
						removePlayer(same);
						continue;
					}
					if (sameIP.equalsIgnoreCase(playerIP))
					{
						final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
						player.sendMessage(msg);
						same.sendMessage(msg);
						removePlayer(same);
					}
				}
				for (L2PcInstance same : _playersNotVIP)
				{
					if (same == null || same.isOnline() == 0 || same.isInJail())
					{
						removePlayer(same);
						continue;
					}
					if (same == player)
						continue; // lolz checks if it's actually the same char
					final String sameIP = same.getIP();
					if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
					{
						removePlayer(same);
						continue;
					}
					if (sameIP.equalsIgnoreCase(playerIP))
					{
						final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
						player.sendMessage(msg);
						same.sendMessage(msg);
						removePlayer(same);
					}
				}
			}
			for (L2PcInstance player : _playersNotVIP)
			{
				if (player == null || player.isOnline() == 0 || player.isInJail())
				{
					try
					{
						removePlayer(player);
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
					removePlayer(player);
					continue;
				}
				for (L2PcInstance same : _playersNotVIP)
				{
					if (same == null || same.isOnline() == 0 || same.isInJail())
					{
						removePlayer(same);
						continue;
					}
					if (same == player)
						continue; // lolz checks if it's actually the same char
					final String sameIP = same.getIP();
					if (sameIP == null || sameIP.equalsIgnoreCase("") || sameIP.length() < 7)
					{
						removePlayer(same);
						continue;
					}
					if (sameIP.equalsIgnoreCase(playerIP))
					{
						final String msg = "Server prohibits two or more players with the same IP to participate in this event; Player " + player.getName() + " from IP " + playerIP + " is already joined. So player " + same.getName() + " may not join this event!";
						player.sendMessage(msg);
						same.sendMessage(msg);
						removePlayer(same);
					}
				}
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
	
	final public static void onDeath(final L2PcInstance player, final L2Character killa)
	{
		if (player == null || player.isOnline() == 0)
			return;
		final L2PcInstance pk = killa.getActingPlayer();
		if (pk != null)
		{
			if (player._isTheVIP && pk._inEventVIP)
			{
				vipDied();
			}
			else if (player._isTheVIP && !pk._inEventVIP)
			{
				Announcements.getInstance().announceToAll("VIP Killed by non-event character. VIP going back to initial spawn.");
				player.doRevive();
				player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				player.teleToLocation(_startX, _startY, _startZ);
			}
			else if (player._isTheVIP && pk._isVIP)
			{
				Announcements.getInstance().announceToAll("VIP Killed by same team player. VIP going back to initial spawn.");
				player.doRevive();
				player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
				player.teleToLocation(_startX, _startY, _startZ);
			}
			else
			{
				int time = 20;
				if (player._isNotVIP)
				{
					double multi = 1.5 * _playersNotVIP.size() / _playersVIP.size();
					time = (int) (20 * multi);
				}
				player.sendMessage("You will be revived and teleported to team spot in " + time + " seconds!");
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					public void run()
					{
						if (player._inEventVIP && _started)
						{
							if (player.isDead())
							{
								player.doRevive();
								player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
								if (player._isVIP)
									player.teleToLocation(_startX, _startY, _startZ);
								else
									player.teleToLocation(_endX, _endY, _endZ);
							}
						}
					}
				}, time * 1000);
			}
		}
		else
		{
			Announcements.getInstance().announceToAll("VIP got killed by a monster. VIP going back to the initial spawn.");
			player.doRevive();
			player.setInstanceId(FOS.SIEGE_EVENT_INSTANCE_ID);
			player.teleToLocation(_startX, _startY, _startZ);
		}
		player.eventTicker = 0;
	}
}