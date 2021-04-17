package net.sf.l2j.gameserver.model.events.TvTInstanced;

import java.util.Vector;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class TvTIMain
{
	protected static final Logger _log = Logger.getLogger(TvTIMain.class.getName());
	private static String				_eventTitle		= new String();
	private static String				_eventDesc		= new String();
	private static String				_joinLocName	= new String();
	private static String				_announceName	= new String();
	private static boolean				_isNpcSpawned	= false;
	private static L2Spawn				_npcSpawn;
	private static int					_npcId			= 0;
	private static int					_npcX			= 0;
	private static int					_npcY			= 0;
	private static int					_npcZ			= 0;
	private static int					_npcHeading		= 0;
	private static int					_spawnRadius	= 250;

	private static Vector<TVTInstance>	_instances		= new Vector<TVTInstance>();

	public static void announceToAll(String announce)
	{
		if (_announceName.equals(""))
			_announceName = "TvTi";

		Announcements.getInstance().announceToAll(announce);
	}

	public static void startJoin(L2PcInstance activeChar)
	{
		if (!canStart())
		{
			if (activeChar != null)
				activeChar.sendMessage("Event not set up propertly.");
			_log.warning("Event not setted propertly.");
			return;
		}

		for (TVTInstance i : _instances)
			if (!i.isJoining() && !i.isStarted() && !i.isTeleport())
				i.setJoining(true);

		if (!_isNpcSpawned)
		{
			if (activeChar != null)
				spawnEventNpc(activeChar);
			else
				spawnEventNpc(null);
		}
		announceToAll(_eventTitle);
		announceToAll("Joinable in " + _joinLocName + "!");
	}

	public static boolean startAutoJoin()
	{
		if (!canStart())
		{
			_log.warning("Event not setted propertly.");
			return false;
		}

		for (TVTInstance i : _instances)
		{
			if (!i.isJoining() && !i.isStarted() && !i.isTeleport())
			{
				i.setJoining(true);
				i.autoEvent();
			}
		}

		if (!_isNpcSpawned)
			spawnEventNpc(null);
		announceToAll(_eventTitle);
		announceToAll("Joinable in " + _joinLocName + "!");
		return true;
	}

	public static boolean canStart()
	{
		if (_eventTitle.equals("") || _joinLocName.equals("") || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _instances.size() < 1)
			return false;

		for (TVTInstance instance : _instances)
			if (!instance.isSetUp())
				return false;

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

			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventTitle);

			_npcSpawn.getLastSpawn()._isEventMobTvTi = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), Config.TVTI_JOIN_NPC_SKILL, 1, 1, 1));

			if (Config.TVTI_JOIN_NPC_DO_SKILL_AGAIN != 0)
				joinNpcSkillTask();
			_isNpcSpawned = true;
		}
		catch (Exception e)
		{
			if (activeChar == null)
				_log.severe("TvT Engine[spawnEventNpc(exception: "+e);
			else
				_log.severe("TvTi Engine[spawnEventNpc(" + activeChar.getName() + ")]: exception: "+e);
		}
	}

	private static void joinNpcSkillTask()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				if (_isNpcSpawned)
				{
					_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), Config.TVTI_JOIN_NPC_SKILL, 1, 1, 1));
					joinNpcSkillTask();
				}
			}
		}, Config.TVTI_JOIN_NPC_DO_SKILL_AGAIN);
	}

	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			if ((eventPlayer.getWeightPenalty() >= 3 || eventPlayer.getInventoryLimit() * 0.8 <= eventPlayer.getInventory().getSize()) && Config.TVTI_CHECK_WEIGHT_AND_INVENTORY)
			{
				eventPlayer.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}

			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");

			replyMSG.append("<title>Team vs Team Instanced</title>");
			replyMSG.append("<table width=\"300\"><tr>");
			replyMSG.append("<td>Current event...</td>");
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... name: <font color=\"00FF00\">" + _eventTitle + "</font></td>");
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... description: <font color=\"00FF00\">" + _eventDesc + "</font></td>");
			for (TVTInstance i : getInstances())
			{
				if (eventPlayer._inEventTvTi)
				{
					replyMSG.append("<tr></tr>");
					replyMSG.append("<td><br><br>The event is taking place... stop wasting time and go kill!</td>");
					break;
				}
				else if (i.getPlayers().contains(eventPlayer))
				{
					replyMSG.append("<tr></tr>");
					replyMSG.append("<td><button value=\"Remove Me!\" action=\"bypass -h npc_" + objectId + "_tvti_player_leave\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					replyMSG.append("<tr></tr>");
					replyMSG.append("<td><font color=\"FFFFFF\">Instance Name: " + i.getInstanceName() + "</font></td>");
					replyMSG.append("<tr></tr>");
					replyMSG.append("<td><font color=\"FFFFFF\">Players registered: " + i.getPlayers().size() + " / " + i.getMaxPlayers() + "</font></td>");
					replyMSG.append("<tr></tr>");
					replyMSG.append("<td><font color=\"FFFFFF\">Levels: " + i.getMinLvl() + " - " + i.getMaxLvl() + "</font></td>");
					break;
				}
				else if (!isPlayerInList(eventPlayer))
				{
					replyMSG.append("<tr></tr>");
					if (eventPlayer.isCursedWeaponEquipped() && !Config.TVTI_JOIN_CURSED)
						replyMSG.append("<td><font color=\"FFFF00\">You can't participate in this event with a cursed Weapon.</font></td>");
					else
						replyMSG.append("<td><button value=\"Show Instances\" action=\"bypass -h npc_" + objectId + "_tvti_player_join_page\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					break;
				}
			}
			replyMSG.append("</tr></table>");

			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);

			// Send a Server->Client ActionFailed to the L2PcInstance in order
			// to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.warning("TvT Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception"+e);
		}
	}

	public static void showInstancesHtml(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");

			replyMSG.append("<title>Team vs Team Instanced</title>");
			replyMSG.append("<table width=\"300\"><tr>");
			replyMSG.append("<td align=\"center\"><font color=\"LEVEL\">---------------------------------</font></td>");
			for (TVTInstance i : getInstances())
			{
				replyMSG.append("<tr></tr>");
				if (eventPlayer.getLevel() >= i.getMaxLvl())
					replyMSG.append("<td><font color=\"LEVEL\">Your level is too high.</font></td>");
				else if (eventPlayer.getLevel() < i.getMinLvl())
					replyMSG.append("<td><font color=\"LEVEL\">Your level is too low.</font></td>");
				else if (i.getPlayers().size() >= i.getMaxPlayers())
					replyMSG.append("<td><font color=\"LEVEL\">This TvT instance is full.</font></td>");
				else if (i.isTeleport() || i.isStarted())
					replyMSG.append("<td><font color=\"LEVEL\">This TvT instance has started.</font></td>");
				// else if (!i.isSetUp())
				// replyMSG.append("<td><font color=\"LEVEL\">This TvT instance is not setup.</font></td>");
				else if (i.isJoining())
					replyMSG.append("<td align=\"center\"><button value=\"Join\" action=\"bypass -h npc_" + objectId + "_tvti_player_join " + i.getInstanceId()
							+ "\" width=200 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				else
					replyMSG.append("<td><font color=\"LEVEL\">This TvT instance is not running.</font></td>");
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">Name: " + i.getInstanceName() + "</font></td>");
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">Players registered: " + i.getPlayers().size() + " / " + i.getMaxPlayers() + "</font></td>");
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">Levels: " + i.getMinLvl() + " - " + i.getMaxLvl() + "</font></td>");
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">Prize: (" + i.getRewardAmount() + ")" + ItemTable.getInstance().getTemplate(i.getRewardId()).getName() + "</font></td>");
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td align=\"center\"><font color=\"LEVEL\">---------------------------------</font></td>");
			}
			replyMSG.append("</tr></table>");

			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);

			// Send a Server->Client ActionFailed to the L2PcInstance in order
			// to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.warning("TvT Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception"+e);
		}
	}

	public synchronized static void addDisconnectedPlayer(L2PcInstance player)
	{
		if (Config.TVTI_ON_START_REMOVE_ALL_EFFECTS)
			player.stopAllEffects();
		
		for (TVTInstance i : _instances)
		{
			if (i.isJoining())
			{
				for (L2PcInstance p : i.getPlayers())
				{
					if (p == null)
						continue;
					else if (p.getName().equals(player.getName()))
					{
						i.getPlayers().remove(p); // Removing old object id from
													// fastlist
						i.getPlayers().add(player); // Adding new objectId to
													// fastlist
						return;
					}
				}
				continue;
			}

			for (TvTITeam t : i.getTeams())
				for (L2PcInstance p : t.getPlayers())
				{
					if (p == null)
						continue;

					// check by name incase player got new objectId
					else if (p.getName().equals(player.getName()))
					{
						player._originalKarmaTvTi = player.getKarma();
						player._inEventTvTi = true;
						t.getPlayers().remove(p); // Removing old object id from
													// fastlist
						t.getPlayers().add(player); // Adding new objectId to
													// fastlist
						player.getAppearance().setNameColor(t.getTeamColor());
						player.setKarma(0);
						player.broadcastUserInfo();
						player.setInstanceId(i.getInstanceId());
						player.teleToLocation(t.getSpawnX(), t.getSpawnY(), t.getSpawnZ());
						return;
					}
				}
		}
	}

	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;

		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		_isNpcSpawned = false;
	}

	public static void removePlayer(L2PcInstance player)
	{
		for (TVTInstance i : _instances)
			if (i.getPlayers().contains(player))
			{
				if (i.isJoining())
				{
					player._joiningTvTi = false;
					i.removePlayer(player);
				}
				else
					player.sendMessage("The event is already started. You can not leave now!");
			}
	}

	/**
	 * @param reason<br>
	 * <br>
	 *            0 = You have been kicked from event because your level is
	 *            greater then the level requirements!<br>
	 *            1 = You have been kicked from event because you have killed 2
	 *            team mates!<br>
	 *            2 = You have been kicked from event by a GM!<br>
	 *            3 = You have been kicked from event because you have obtained
	 *            a cursed weapon!<br>
	 */
	public static void kickPlayerFromEvent(L2PcInstance player, int reason)
	{
		L2PcInstance p = null;

		for (TVTInstance i : _instances)
		{
			if (i.getPlayers().contains(player))
			{
				p = player;
				player._joiningTvTi = false;
				i.removePlayer(player);
				break;
			}
			for (TvTITeam t : i.getTeams())
			{
				if (t.getPlayers().contains(player))
				{
					p = player;
					player.setKarma(player._originalKarmaTvTi);
					player.setKarmaFlag(0);
					player._inEventTvTi = false;
					player.broadcastUserInfo();
					t.removePlayer(player);
					player.setInstanceId(0);
					player.teleToLocation(TvTIMain.getNpcX(), TvTIMain.getNpcY(), TvTIMain.getNpcZ(), false);
					break;
				}
			}
			if (p != null)
				break;
		}

		if (p == null)
			return;

		switch (reason)
		{
			case 0:
				player.sendMessage("You have been kicked from event because your level is greater then the level requirements!");
				break;
			case 1:
				player.sendMessage("You have been kicked from event because you have killed 2 team mates!");
				break;
			case 2:
				player.sendMessage("You have been kicked from event by a GM!");
				break;
			case 3:
				player.sendMessage("You have been kicked from event because you have obtained a cursed weapon!");
				break;
		}
	}

	public static void addKill(L2PcInstance player)
	{
		for (TVTInstance i : _instances)
			for (TvTITeam t : i.getTeams())
				if (t.getPlayers().contains(player))
					t.setTeamScore(t.getTeamScore() + 1);
	}

	public static void removePoint(L2PcInstance player)
	{
		for (TVTInstance i : _instances)
			for (TvTITeam t : i.getTeams())
				if (t.getPlayers().contains(player))
					t.setTeamScore(t.getTeamScore() - 1);
	}

	public static boolean checkSameTeam(L2PcInstance target, L2PcInstance player)
	{
		for (TVTInstance i : _instances)
			for (TvTITeam t : i.getTeams())
				if (t.getPlayers().contains(player) && t.getPlayers().contains(target))
					return true;
		return false;
	}

	public static void respawnPlayer(final L2PcInstance player)
	{
		player.sendMessage("You will be revived and teleported to spot in " + Config.TVTI_REVIVE_DELAY / 1000 + " seconds!");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				for (TVTInstance i : _instances)
					for (TvTITeam t : i.getTeams())
						if (t.getPlayers().contains(player))
						{
							t.teleportToSpawn(player);
							player.doRevive();
						}
			}
		}, Config.TVTI_REVIVE_DELAY);
	}

	public static void resSummon(final L2Summon summon)
	{
		summon.getOwner().sendMessage("Your pet will be revived in " + Config.TVTI_REVIVE_DELAY / 1000 + " seconds!");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				for (TVTInstance i : _instances)
					for (TvTITeam t : i.getTeams())
						if (t.getPlayers().contains(summon.getOwner()))
						{
							summon.doRevive();
							summon.teleToLocation(summon.getOwner().getX(), summon.getOwner().getY(), summon.getOwner().getZ(), false);
						}
			}
		}, Config.TVTI_REVIVE_DELAY);
	}

	public static void addPlayer(L2PcInstance player, int instanceId)
	{		
		for (TVTInstance i : _instances)
			if (i.getInstanceId() == instanceId)
			{
				if (i.isJoining())
				{
					i.addPlayer(player);
					player._joiningTvTi = true;
				}
				else
					player.sendMessage("The event is already started. You can not join now!");
			}
	}

	public static boolean isPlayerInList(L2PcInstance player)
	{
		for (TVTInstance i : getInstances())
		{
			if (i.isJoining())
			{
				for (L2PcInstance eventPlayer : i.getPlayers())
				{
					if (eventPlayer.getName().equals(player.getName()))
						return true;
				}
			}
			else
			{
				for (TvTITeam t : i.getTeams())
				{
					for (L2PcInstance eventPlayer : t.getPlayers())
					{
						if (eventPlayer.getName().equals(player.getName()))
							return true;
					}
				}
			}
		}
		return false;
	}

	public static int createInstance()
	{
		TVTInstance i = new TVTInstance();
		getInstances().add(i);
		return i.getInstanceId();
	}

	public static void removeInstance(int instanceId)
	{
		for (TVTInstance i : _instances)
		{
			if (i.getInstanceId() == instanceId)
			{
				i.destroyInstance();
				getInstances().remove(i);
				break;
			}
		}
	}

	public static TVTInstance getTvTInstance(int instanceId)
	{
		for (TVTInstance i : getInstances())
			if (i.getInstanceId() == instanceId)
				return i;
		return null;
	}

	public static boolean canUnspawnEventNpc()
	{
		for (TVTInstance i : _instances)
		{
			if (i.isJoining() || i.isStarted() || i.isTeleport())
				return false;
		}
		return true;
	}

	public static boolean levelIncreased(L2PcInstance player)
	{
		for (TVTInstance i : _instances)
		{
			if (i.isJoining())
			{
				if (i.getPlayers().contains(player) && player.getLevel() >= i.getMaxLvl())
					return true;
			}
			else
			{
				for (TvTITeam t : i.getTeams())
				{
					if (t.getPlayers().contains(player) && player.getLevel() >= i.getMaxLvl())
						return true;
				}
			}
		}
		return false;
	}

	public static void duplicateInstance(int instanceId)
	{
		TVTInstance tempI = getTvTInstance(instanceId);
		TVTInstance newI = getTvTInstance(createInstance());
		newI.setEventTime(tempI.getEventTime());
		newI.setInstanceName(tempI.getInstanceName());
		newI.setJoinTime(tempI.getJoinTime());
		newI.setMaxLvl(tempI.getMaxLvl());
		newI.setMaxPlayers(tempI.getMaxPlayers());
		newI.setMinLvl(tempI.getMinLvl());
		newI.setMinPlayers(tempI.getMinPlayers());
		newI.setRewardAmount(tempI.getRewardAmount());
		newI.setRewardId(tempI.getRewardId());
		for (TvTITeam team : tempI.getTeams())
			newI.createTeam(team.getTeamName(), team.getTeamColor(), team.getSpawnX(), team.getSpawnY(), team.getSpawnZ(), team.getSpawnRadius());
	}

	public static void setEventTitle(String title)
	{
		_eventTitle = title;
	}

	public static String getEventTitle()
	{
		return _eventTitle;
	}

	public static void setEventDesc(String desc)
	{
		_eventDesc = desc;
	}

	public static String getEventDesc()
	{
		return _eventDesc;
	}

	public static void setJoinLocName(String name)
	{
		_joinLocName = name;
	}

	public static String getJoinLocName()
	{
		return _joinLocName;
	}

	public static void setAnnounceName(String name)
	{
		_announceName = name;
	}

	public static String getAnnounceName()
	{
		return _announceName;
	}

	public static L2Spawn getJoinNpc()
	{
		return _npcSpawn;
	}

	public static Vector<TVTInstance> getInstances()
	{
		return _instances;
	}

	public static void setNpcId(int npcId)
	{
		_npcId = npcId;
	}

	public static int getNpcId()
	{
		return _npcId;
	}

	public static void setSpawn(int locX, int locY, int locZ)
	{
		_npcX = locX;
		_npcY = locY;
		_npcZ = locZ;
	}

	public static void setSpawn(L2PcInstance activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
	}

	public static void setNpcX(int npcX)
	{
		_npcX = npcX;
	}

	public static int getNpcX()
	{
		return _npcX;
	}

	public static void setNpcY(int npcY)
	{
		_npcY = npcY;
	}

	public static int getNpcY()
	{
		return _npcY;
	}

	public static void setNpcZ(int npcZ)
	{
		_npcZ = npcZ;
	}

	public static int getNpcZ()
	{
		return _npcZ;
	}

	public static void setNpcHeading(int npcHeading)
	{
		_npcHeading = npcHeading;
	}

	public static int getNpcHeading()
	{
		return _npcHeading;
	}

	public static void setSpawnRadius(int radius)
	{
		_spawnRadius = radius;
	}

	public static int getSpawnRadius()
	{
		return _spawnRadius;
	}
}