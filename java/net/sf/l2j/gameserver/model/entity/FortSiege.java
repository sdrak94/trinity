/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeGuardManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager.SiegeSpawn;
import net.sf.l2j.gameserver.model.CombatFlag;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2SiegeClan.SiegeClanType;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FortCommanderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class FortSiege
{
	protected static final Logger _log = Logger.getLogger(FortSiege.class.getName());

	boolean preventDoorsFromClosing = false;
	
	public boolean getPreventDoorsFortSiege()
	{
		return preventDoorsFromClosing;
	}
	public static enum TeleportWhoType
	{
		All,
		Attacker,
		Owner,
	}

	public class ScheduleEndSiegeTask implements Runnable
	{
		private final Fort _fortInst;

		public ScheduleEndSiegeTask(Fort pFort)
		{
			_fortInst = pFort;
		}

		public void run()
		{
			if (!getIsInProgress())
				return;

			try
			{
				_siegeEnd = null;
				_fortInst.getSiege().endSiege();
			}
			catch (Exception e)
			{
				_log.warning("Exception: ScheduleEndSiegeTask() for Fort: "+_fortInst.getName()+" " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Fort _fortInst;
		private final int _time;

		public ScheduleStartSiegeTask(Fort pFort, int time)
		{
			_fortInst = pFort;
			_time = time;
		}

		public void run()
		{
			if (getIsInProgress())
				return;

			try
			{
				final SystemMessage sm;
				if (_time == 3600) // 1hr remains
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,600), 3000000); // Prepare task for 10 minutes left.
				}
				else if (_time == 600) // 10min remains
				{
					getFort().despawnNpcs(getFort()._siegeNpcs);
					sm = new SystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS); 
					sm.addNumber(10);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,300), 300000); // Prepare task for 5 minutes left.
				}
				else if (_time == 300) // 5min remains
				{
					sm = new SystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS); 
					sm.addNumber(5);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,60), 240000); // Prepare task for 1 minute left.
				}
				else if (_time == 60) // 1min remains
				{
					sm = new SystemMessage(SystemMessageId.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS); 
					sm.addNumber(1);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,30), 30000); // Prepare task for 30 seconds left.
				}
				else if (_time == 30) // 30seconds remains
				{
					sm = new SystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(30);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,10), 20000); // Prepare task for 10 seconds left.
				}
				else if (_time == 10) // 10seconds remains
				{
					sm = new SystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(10);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,5), 5000); // Prepare task for 5 seconds left.
				}
				else if (_time == 5) // 5seconds remains
				{
					sm = new SystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(5);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,1), 4000); // Prepare task for 1 seconds left.
				}
				else if (_time == 1) // 1seconds remains
				{
					sm = new SystemMessage(SystemMessageId.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS);
					sm.addNumber(1);
					announceToPlayer(sm);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst,0), 1000); // Prepare task start siege.
				}
				else if (_time == 0)// start siege
				{
					_fortInst.getSiege().startSiege();
				}
				else
					_log.warning("Exception: ScheduleStartSiegeTask(): unknown siege time: "+String.valueOf(_time));
			}
			catch (Exception e)
			{
				_log.warning("Exception: ScheduleStartSiegeTask() for Fort: "+_fortInst.getName()+" " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public class ScheduleSuspiciousMerchantSpawn implements Runnable
	{
		private final Fort _fortInst;

		public ScheduleSuspiciousMerchantSpawn(Fort pFort)
		{
			_fortInst = pFort;
		}

		public void run()
		{
			if (!getIsInProgress())
				return;

			try
			{
				_fortInst.spawnNpcs(_fortInst._siegeNpcs);
			}
			catch (Exception e)
			{
				_log.warning("Exception: ScheduleSuspicoiusMerchantSpawn() for Fort: "+_fortInst.getName()+" " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public class ScheduleSiegeRestore implements Runnable
	{
		private final Fort _fortInst;

		public ScheduleSiegeRestore(Fort pFort)
		{
			_fortInst = pFort;
		}

		public void run()
		{
			if (!getIsInProgress())
				return;

			try
			{
				_siegeRestore = null;
				_fortInst.getSiege().resetSiege();
				announceToPlayer(new SystemMessage(SystemMessageId.BARRACKS_FUNCTION_RESTORED));
			}
			catch (Exception e)
			{
				_log.warning("Exception: ScheduleSiegeRestore() for Fort: "+_fortInst.getName()+" " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private final List<L2SiegeClan> _attackerClans = new FastList<L2SiegeClan>();

	// Fort setting
	protected FastMap<Integer, FastList<L2Spawn>> _commanders = new FastMap<Integer, FastList<L2Spawn>>();
	protected FastList<L2Spawn> _commandersSpawns;
	private final Fort[] _fort;
	private boolean _isInProgress = false;
	private FortSiegeGuardManager _siegeGuardManager;
	ScheduledFuture<?> _siegeEnd = null;
	ScheduledFuture<?> _siegeRestore = null;
	ScheduledFuture<?> _siegeStartTask = null;

	public FortSiege(Fort[] fort)
	{
		_fort = fort;

		checkAutoTask();
		FortSiegeManager.getInstance().addSiege(this);
	}

	/**
	 * When siege ends<BR><BR>
	 */
	public void endSiege()
	{
		if (getIsInProgress())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED);
			sm.addFortId(getFort().getFortId());
			announceToPlayer(sm);

			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
			unSpawnFlags();

			teleportPlayer(FortSiege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town);
			_isInProgress = false; // Flag so that siege instance can be started
			getFort().getZone().updateZoneStatusForCharactersInside();

			saveFortSiege(); // Save fort specific data
			clearSiegeClan(); // Clear siege clan from db
			removeCommanders(); // Remove commander from this fort

			getFort().spawnNpcs(getFort()._npcCommanders); // Spawn NPC commanders
			getSiegeGuardManager().unspawnSiegeGuard(); // Remove all spawned siege guard from this fort
			getFort().resetDoors(); // Respawn door to fort

			updatePlayerSiegeStateFlags(true);

			ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleSuspiciousMerchantSpawn(getFort()), FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay()*60*1000); // Prepare 3hr task for suspicious merchant respawn
			if (_siegeEnd != null)
				_siegeEnd.cancel(true);
			if (_siegeRestore != null)
				_siegeRestore.cancel(true);

			if (getFort().getOwnerClan() != null && getFort().getFlagPole().getMeshIndex() == 0)
				getFort().setVisibleFlag(true);
			
			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				L2Clan owner = getFort().getOwnerClan();
				if (owner != null)
				{
					for (L2PcInstance clanMember : owner.getOnlineMembers(0))
					{
						clanMember.getCounters().fortSiegesWon++;
					}
				}
			}
		}
	}

	/**
	 * When siege starts<BR><BR>
	 */
	public void startSiege()
	{
		if (!getIsInProgress())
		{
			if (_siegeStartTask != null) // used admin command "admin_startfortsiege"
				_siegeStartTask.cancel(true);
			_siegeStartTask = null;

			if (getAttackerClans().isEmpty())
				return;

			_isInProgress = true; // Flag so that same siege instance cannot be started again

			loadSiegeClan(); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(FortSiege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town

			getFort().despawnNpcs(getFort()._npcCommanders); // Despawn NPC commanders
			spawnCommanders(); // Spawn commanders
			getFort().resetDoors(); // Spawn door
			spawnSiegeGuard(); // Spawn siege guard
			getFort().setVisibleFlag(false);

			getFort().getZone().updateZoneStatusForCharactersInside();

			// Schedule a task to prepare auto siege end
			_siegeEnd = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(getFort()), FortSiegeManager.getInstance().getSiegeLength()*60*1000); // Prepare auto end task

			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_S1_HAS_BEGUN);
			sm.addFortId(getFort().getFortId());
			announceToPlayer(sm);
			saveFortSiege();
		}
	}
	
	/**
	 * Announce to player.<BR><BR>
	 * @param message The String of the message to send to player
	 */
	public void announceToPlayer(SystemMessage sm)
	{
		// announce messages only for participants
		L2Clan clan;
		for (L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (member != null)
					member.sendPacket(sm);
			}
		}
		if (getFort().getOwnerClan() != null)
		{
			clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (member != null)
					member.sendPacket(sm);
			}
		}
	}

	public void announceToPlayer(SystemMessage sm, String s)
	{
		sm.addString(s);
		announceToPlayer(sm);
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;
		for (L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (member == null)
					continue;

				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setIsInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 1);
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.sendPacket(new UserInfo(member));
				member.sendPacket(new ExBrExtraUserInfo(member));
				Collection<L2PcInstance> plrs = member.getKnownList().getKnownPlayers().values();
				//synchronized (member.getKnownList().getKnownPlayers())
				{
					for (L2PcInstance player : plrs)
					{
						try
						{
							player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
							if (member.getPet() != null)
								player.sendPacket(new RelationChanged(member.getPet(), member.getRelation(player), member.isAutoAttackable(player)));
						}
						catch (NullPointerException e)
						{
						}
					}
				}
			}
		}
		if (getFort().getOwnerClan() != null)
		{
			clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (member == null)
					continue;

				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setIsInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 2);
					if (checkIfInZone(member))
					{
						member.setIsInSiege(true);
						member.startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				member.sendPacket(new UserInfo(member));
				member.sendPacket(new ExBrExtraUserInfo(member));
				Collection<L2PcInstance> plrs = member.getKnownList().getKnownPlayers().values();
				//synchronized (member.getKnownList().getKnownPlayers())
				{
					for (L2PcInstance player : plrs)
					{
						try
						{
							player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
							if (member.getPet() != null)
								player.sendPacket(new RelationChanged(member.getPet(), member.getRelation(player), member.isAutoAttackable(player)));
						}
						catch (NullPointerException e)
						{
						}
					}
				}
			}
		}
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(L2Object object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return (getIsInProgress() && (getFort().checkIfInZone(x, y, z))); // Fort zone during siege
	}

	/**
	 * Return true if clan is attacker<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public boolean checkIsAttacker(L2Clan clan)
	{
		return (getAttackerClan(clan) != null);
	}

	/**
	 * Return true if clan is defender<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public boolean checkIsDefender(L2Clan clan)
	{
		if (clan != null && getFort().getOwnerClan() == clan)
			return true;
		return false;
	}

	/** Clear all registered siege clans from database for fort */
	public void clearSiegeClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");
			statement.setInt(1, getFort().getFortId());
			statement.execute();
			statement.close();

			if (getFort().getOwnerClan() != null)
			{
				statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE clan_id=?");
				statement.setInt(1, getFort().getOwnerClan().getClanId());
				statement.execute();
			}

			getAttackerClans().clear();

			// if siege is in progress, end siege
			if (getIsInProgress())
				endSiege();
			preventDoorsFromClosing = false;
			// if siege isnt in progress (1hr waiting time till siege starts), cancel waiting time and spawn Suspicious Merchant
			if (_siegeStartTask != null)
			{
				_siegeStartTask.cancel(true);
				_siegeStartTask = null;
				ThreadPoolManager.getInstance().executeTask(new ScheduleSuspiciousMerchantSpawn(getFort()));
			}
		}
		catch (Exception e)
		{
			_log.warning("Exception: clearSiegeClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning("" + e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	/** Set the date for the next siege. */
	private void clearSiegeDate()
	{
		getFort().getSiegeDate().setTimeInMillis(0);
	}

	/** Return list of L2PcInstance registered as attacker in the zone. */
	public List<L2PcInstance> getAttackersInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;
		for (L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (L2PcInstance player : clan.getOnlineMembers(0))
			{
				if (player == null)
					continue;

				if (player.isInSiege())
					players.add(player);
			}
		}
		return players;
	}

	/** Return list of L2PcInstance in the zone. */
	public List<L2PcInstance> getPlayersInZone()
	{
		return getFort().getZone().getAllPlayers();
	}

	/** Return list of L2PcInstance owning the fort in the zone. */
	public List<L2PcInstance> getOwnersInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;
		if (getFort().getOwnerClan() != null)
		{
			clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getClanId());
			if (clan != getFort().getOwnerClan())
				return null;
			for (L2PcInstance player : clan.getOnlineMembers(0))
			{
				if (player == null)
					continue;

				if (player.isInSiege())
					players.add(player);
			}
		}
		return players;
	}

	/** Commander was killed */
	public void killedCommander(L2FortCommanderInstance instance)
	{
		if (_commanders != null && getFort() != null && _commanders.get(getFort().getFortId()).size() != 0)
		{
			L2Spawn spawn = instance.getSpawn();
			if (spawn != null)
			{
				FastList<SiegeSpawn> commanders = FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getFortId());
				for (SiegeSpawn spawn2 : commanders)
    			{
					if (spawn2.getNpcId() == spawn.getNpcid())
					{
						String text = "";
						switch (spawn2.getId())
						{
							case 1:
								text = "You may have broken our arrows, but you will never break our will! Archers retreat!";
								break;
							case 2:
								text = "Aieeee! Command Center! This is guard unit! We need backup right away!";
								break;
							case 3:
								text = "At last! The Magic Field that protects the fortress has weakened! Volunteers, stand back!";
								break;
							case 4:
								text = "I feel so much grief that I can't even take care of myself. There isn't any reason for me to stay here any longer.";
								break;
						}
						if (!text.isEmpty())
							instance.broadcastPacket(new NpcSay(instance.getObjectId(), 1, instance.getNpcId(), text));
					}
    			}
				_commanders.get(getFort().getFortId()).remove(spawn);
				if (_commanders.get(getFort().getFortId()).isEmpty())
				{
					// spawn fort flags
					spawnFlag(getFort().getFortId());
					// cancel door/commanders respawn
					if (_siegeRestore != null)
					{
						_siegeRestore.cancel(true);
					}
					// open doors in main building
					for (L2DoorInstance door : getFort().getDoors())
					{
						preventDoorsFromClosing = true;
						if (!door.getIsCommanderDoor())
							continue;
						door.openMe();
					}
					getFort().getSiege().announceToPlayer(new SystemMessage(SystemMessageId.ALL_BARRACKS_OCCUPIED));
				}
				// schedule restoring doors/commanders respawn 
				else if (_siegeRestore == null)
				{
					getFort().getSiege().announceToPlayer(new SystemMessage(SystemMessageId.SEIZED_BARRACKS));
					_siegeRestore  = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleSiegeRestore(getFort()), FortSiegeManager.getInstance().getCountDownLength()*60*1000);
				}
				else
					getFort().getSiege().announceToPlayer(new SystemMessage(SystemMessageId.SEIZED_BARRACKS));
			}
			else
				_log.warning("FortSiege.killedCommander(): killed commander, but commander not registered for fortress. NpcId: "+instance.getNpcId()+" FortId: "+getFort().getFortId());
		}
	}

	/** Remove the flag that was killed */
	public void killedFlag(L2Npc flag)
	{
		if (flag == null)
			return;

		for (L2SiegeClan clan: getAttackerClans())
		{
			if (clan.removeFlag(flag))
				return;
		}
	}

	/**
	 * Register clan as attacker<BR><BR>
	 * @param player The L2PcInstance of the player trying to register
	 */
	public boolean registerAttacker(L2PcInstance player, boolean force)
	{
		if (player.getClan() == null)
			return false;

		if (force || checkIfCanRegister(player))
		{
			saveSiegeClan(player.getClan()); // Save to database
			// if the first registering we start the timer
			if (getAttackerClans().size() == 1)
			{
				if (!force)
					player.reduceAdena("siege", adena_required_for_reg, null, true);
				startAutoTask(true);
			}
			return true;
		}
		return false;
	}
	
	public final static long adena_required_for_reg = 20000000000L;

	/**
	 * Remove clan from siege<BR><BR>
	 * @param clanId The int of player's clan id
	 */
	public void removeSiegeClan(int clanId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			if (clanId != 0)
				statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=? AND clan_id=?");
			else
				statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");

			statement.setInt(1, getFort().getFortId());
			if (clanId != 0)
				statement.setInt(2, clanId);
			statement.execute();

			loadSiegeClan();
			if (getAttackerClans().isEmpty())
			{
				if (getIsInProgress())
					endSiege();
				if (_siegeStartTask != null)
				{
					_siegeStartTask.cancel(true);
					_siegeStartTask = null;
					ThreadPoolManager.getInstance().executeTask(new ScheduleSuspiciousMerchantSpawn(getFort()));
				}
			}
		}
		catch (Exception e)
		{
			_log.warning(""+e.getMessage());
        	e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * @param player The L2PcInstance of player/clan being removed
	 */
	public void removeSiegeClan(L2Clan clan)
	{
		if (clan == null || clan.getHasFort() == getFort().getFortId() || !FortSiegeManager.getInstance().checkIsRegistered(clan, getFort().getFortId()))
			return;
		removeSiegeClan(clan.getClanId());
	}

	/**
	 * Remove clan from siege<BR><BR>
	 * @param player The L2PcInstance of player/clan being removed
	 */
	public void removeSiegeClan(L2PcInstance player)
	{
		removeSiegeClan(player.getClan());
	}

	/**
	 * Start the auto tasks<BR>
	 * <BR>
	 */
	public void checkAutoTask()
	{
		if (getFort().getSiegeDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			clearSiegeDate();
			saveSiegeDate();
			removeSiegeClan(0); // remove all clans
			return;
		}

		startAutoTask(false);
	}

	/**
	 * Start the auto tasks<BR><BR>
	 */
	public void startAutoTask(boolean setTime)
	{
		if (setTime)
			setSiegeDateTime();
		if (getFort().getOwnerClan() != null)
		{
			L2Clan clan = ClanTable.getInstance().getClan(getFort().getOwnerClan().getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				member.sendPacket(new SystemMessage(SystemMessageId.A_FORTRESS_IS_UNDER_ATTACK));
			}
		}
		loadSiegeClan();
		// Execute siege auto start
		_siegeStartTask = ThreadPoolManager.getInstance().scheduleGeneral(new FortSiege.ScheduleStartSiegeTask(getFort(),3600), 0);
	}

	/**
	 * Teleport players
	 */
	public void teleportPlayer(TeleportWhoType teleportWho, MapRegionTable.TeleportWhereType teleportWhere)
	{
		List<L2PcInstance> players;
		switch (teleportWho)
		{
			case Owner:
				players = getOwnersInZone();
				break;
			case Attacker:
				players = getAttackersInZone();
				break;
			default:
				players = getPlayersInZone();
		}

		for (L2PcInstance player : players)
		{
			if (player.isGM() || player.isInJail())
				continue;
			player.teleToLocation(teleportWhere);
		}
	}

	// =========================================================
	// Method - Private
	/**
	 * Add clan as attacker<BR><BR>
	 * @param clanId The int of clan's id
	 */
	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
	}

	/**
	 * Return true if the player can register.<BR><BR>
	 * @param player The L2PcInstance of the player trying to register
	 */
	public boolean checkIfCanRegister(L2PcInstance player)
	{
		boolean b = true;
		if (player.getClan() == null || player.getClan().getLevel() < FortSiegeManager.getInstance().getSiegeClanMinLevel())
		{
			b = false;
			player.sendMessage("Only clans with Level " + FortSiegeManager.getInstance().getSiegeClanMinLevel() + " and higher may register for a fortress siege.");
		}
		else if (!player.isClanLeader())
		{
			b = false;
			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));			
		}
		else if (player.getClan() == getFort().getOwnerClan())
		{
			b = false;
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING));
		}
		else if (getFort().getOwnerClan() != null && player.getClan().getHasCastle() > 0 && player.getClan().getHasCastle() == getFort().getCastleId())
		{
			b = false;
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_REGISTER_TO_SIEGE_DUE_TO_CONTRACT));
		}
		else if (getFort().getSiege().getAttackerClans().isEmpty() && player.getInventory().getAdena() < adena_required_for_reg)
        {
        	player.sendMessage("You need "+adena_required_for_reg+" adena to register"); // replace me with html
        }
		else
		{
			for (Fort fort : FortManager.getInstance().getForts())
			{
				if (fort.getSiege().getAttackerClan(player.getClanId())!= null)
				{
					b = false;
					player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE));
					break;
				}
				if (fort.getOwnerClan() == player.getClan() && (fort.getSiege().getIsInProgress()||fort.getSiege()._siegeStartTask != null))
				{
					b = false;
					player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_REQUESTED_SIEGE_BATTLE));
					break;
				}
			}
		}
		return b;
	}

	/**
	 * Return true if the clan has already registered to a siege for the same day.<BR><BR>
	 * @param clan The L2Clan of the player trying to register
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(L2Clan clan)
	{
		for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
		{
			if (siege == this)
				continue;
			if (siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == this.getSiegeDate().get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
					return true;
				if (siege.checkIsDefender(clan))
					return true;
			}
		}
		return false;
	}

	private void setSiegeDateTime()
	{
		Calendar newDate = Calendar.getInstance();
		newDate.add(Calendar.MINUTE, 60);
		getFort().setSiegeDate(newDate);
		saveSiegeDate();
	}

	/** Load siege clans. */
	private void loadSiegeClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			getAttackerClans().clear();

			ResultSet rs = null;

			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT clan_id FROM fortsiege_clans WHERE fort_id=?");
			statement.setInt(1, getFort().getFortId());
			rs = statement.executeQuery();

			while (rs.next())
			{
					addAttacker(rs.getInt("clan_id"));
			}

			rs.close();
		}
		catch (Exception e)
		{
			_log.warning("Exception: loadSiegeClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	/** Remove commanders. */
	private void removeCommanders()
	{
		if (_commanders != null && !_commanders.isEmpty())
		{
			// Remove all instance of commanders for this fort
			for (L2Spawn spawn : _commanders.get(getFort().getFortId()))
			{
				if (spawn != null)
				{
					spawn.stopRespawn();
					spawn.getLastSpawn().deleteMe();
				}
			}
			_commanders.clear();
		}
	}

	/** Remove all flags. */
	private void removeFlags()
	{
		for (L2SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
				sc.removeFlags();
		}
	}

	/** Save fort siege related to database. */
	private void saveFortSiege()
	{
		clearSiegeDate(); // clear siege date
		saveSiegeDate(); // Save the new date
	}

	/** Save siege date to database. */
	private void saveSiegeDate()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE fort SET siegeDate = ? WHERE id = ?");
			statement.setLong(1, getSiegeDate().getTimeInMillis());
			statement.setInt(2, getFort().getFortId());
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warning("Exception: saveSiegeDate(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	/**
	 * Save registration to database.<BR><BR>
	 * @param clan The L2Clan of player
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 */
	private void saveSiegeClan(L2Clan clan)
	{
		if (getAttackerClans().size() >= FortSiegeManager.getInstance().getAttackerMaxClans())
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO fortsiege_clans (clan_id,fort_id) values (?,?)");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, getFort().getFortId());
			statement.execute();

			addAttacker(clan.getClanId());
		}
		catch (Exception e)
		{
			_log.warning("Exception: saveSiegeClan(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}

			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
	}

	/** Spawn commanders. */
	private void spawnCommanders()
	{
		//Set commanders array size if one does not exist
		try
		{
			_commanders.clear();
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			_commandersSpawns = new FastList<L2Spawn>();
			for (SiegeSpawn _sp : FortSiegeManager.getInstance().getCommanderSpawnList(getFort().getFortId()))
			{
				template1 = NpcTable.getInstance().getTemplate(_sp.getNpcId());
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(1);
					spawnDat.setLocx(_sp.getLocation().getX());
					spawnDat.setLocy(_sp.getLocation().getY());
					spawnDat.setLocz(_sp.getLocation().getZ());
					spawnDat.setHeading(_sp.getLocation().getHeading());
					spawnDat.setRespawnDelay(60);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
					_commandersSpawns.add(spawnDat);
				}
				else
				{
					_log.warning("FortSiege.spawnCommander: Data missing in NPC table for ID: "
				        + _sp.getNpcId() + ".");
				}
				_commanders.put(getFort().getFortId(), _commandersSpawns);
			}
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("FortSiege.spawnCommander: Spawn could not be initialized: "
			        + e.getMessage());
			e.printStackTrace();
		}
	}

	private void spawnFlag(int Id)
	{
		for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(Id))
		{
			cf.spawnMe();
		}
	}

	private void unSpawnFlags()
	{
		if (FortSiegeManager.getInstance().getFlagList(getFort().getFortId()) == null)
			return;
		for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(getFort().getFortId()))
		{
			cf.unSpawnMe();
		}
	}

	/**
	 * Spawn siege guard.<BR><BR>
	 */
	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();
	}

	public final L2SiegeClan getAttackerClan(L2Clan clan)
	{
		if (clan == null)
			return null;

		return getAttackerClan(clan.getClanId());
	}

	public final L2SiegeClan getAttackerClan(int clanId)
	{
		for (L2SiegeClan sc : getAttackerClans())
			if (sc != null && sc.getClanId() == clanId)
				return sc;

		return null;
	}

	public final List<L2SiegeClan> getAttackerClans()
	{
		return _attackerClans;
	}

	public final Fort getFort()
	{
		if (_fort == null || _fort.length <= 0)
			return null;

		return _fort[0];
	}

	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}

	public final Calendar getSiegeDate()
	{
		return getFort().getSiegeDate();
	}

	public List<L2Npc> getFlag(L2Clan clan)
	{
		if (clan != null)
		{
			L2SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
				return sc.getFlag();
		}
		return null;
	}

	public final FortSiegeGuardManager getSiegeGuardManager()
	{
		if (_siegeGuardManager == null)
		{
			_siegeGuardManager = new FortSiegeGuardManager(getFort());
		}
		return _siegeGuardManager;
	}

	public void resetSiege()
	{
		// reload commanders and repair doors
		removeCommanders();
		spawnCommanders();
		getFort().resetDoors();
	}

	public FastMap<Integer, FastList<L2Spawn>> getCommanders()
	{
		return _commanders;
	}
}
