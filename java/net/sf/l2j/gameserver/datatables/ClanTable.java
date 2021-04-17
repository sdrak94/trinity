/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.5.2.5 $ $Date: 2005/03/27 15:29:18 $
 */
public class ClanTable
{
	private static Logger			_log	= Logger.getLogger(ClanTable.class.getName());
	private Map<Integer, L2Clan>	_clans;
	
	public static ClanTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public L2Clan[] getClans()
	{
		return _clans.values().toArray(new L2Clan[_clans.size()]);
	}
	
	private ClanTable()
	{
		_clans = new FastMap<Integer, L2Clan>();
		L2Clan clan;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			ResultSet result = statement.executeQuery();
			// Count the clans
			int clanCount = 0;
			while (result.next())
			{
				int clanId = result.getInt("clan_id");
				_clans.put(clanId, new L2Clan(clanId));
				clan = getClan(clanId);
				if (clan.getDissolvingExpiryTime() != 0)
					scheduleRemoveClan(clan.getClanId());
				clanCount++;
			}
			result.close();
			statement.close();
			_log.config("Restored " + clanCount + " clans from the database.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring ClanTable.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		restorewars();
	}
	
	/**
	 * @param clanId
	 * @return
	 */
	public L2Clan getClan(int clanId)
	{
		L2Clan clan = _clans.get(Integer.valueOf(clanId));
		return clan;
	}
	
	public L2Clan getClanByName(String clanName)
	{
		for (L2Clan clan : getClans())
		{
			if (clan.getName().equalsIgnoreCase(clanName))
			{
				return clan;
			}
		}
		return null;
	}
	
	/**
	 * Creates a new clan and store clan info to database
	 *
	 * @param player
	 * @return NULL if clan with same name already exists
	 */
	public L2Clan createClan(L2PcInstance player, String clanName)
	{
		if (null == player)
			return null;
		if (Config.DEBUG)
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested a clan creation.");
		if (10 > player.getLevel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN));
			return null;
		}
		if (0 != player.getClanId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_CREATE_CLAN));
			return null;
		}
		if (System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return null;
		}
		if (!Util.isAlphaNumeric(clanName) || 2 > clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return null;
		}
		if (22 < clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return null;
		}
		if (null != getClanByName(clanName))
		{
			// clan name is already taken
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
			sm.addString(clanName);
			player.sendPacket(sm);
			sm = null;
			return null;
		}
		L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
		L2ClanMember leader = new L2ClanMember(clan, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle(), player.getAppearance().getSex(), player.getRace().getRealOrdinal());
		clan.setLeader(leader);
		leader.setPlayerInstance(player);
		clan.changeLevel(5);
		clan.store();
		player.setClan(clan);
		player.setPledgeClass(leader.calculatePledgeClass(player));
		player.setClanPrivileges(L2Clan.CP_ALL);
		if (Config.DEBUG)
			_log.fine("New clan created: " + clan.getClanId() + " " + clan.getName());
		_clans.put(Integer.valueOf(clan.getClanId()), clan);
		// should be update packet only
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new SystemMessage(SystemMessageId.CLAN_CREATED));
		return clan;
	}
	
	public synchronized void destroyClan(int clanId)
	{
		L2Clan clan = getClan(clanId);
		if (clan == null)
		{
			return;
		}
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));
		int castleId = clan.getHasCastle();
		if (castleId == 0)
		{
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clanId);
			}
		}
		int fortId = clan.getHasFort();
		if (fortId == 0)
		{
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clanId);
			}
		}
		L2ClanMember leaderMember = clan.getLeader();
		if (leaderMember == null)
			clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
		else
			clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
		for (L2ClanMember member : clan.getMembers())
		{
			clan.removeClanMember(member.getObjectId(), 0);
		}
		_clans.remove(clanId);
		IdFactory.getInstance().releaseId(clanId);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
			statement.setInt(1, clanId);
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			statement.close();
			if (castleId != 0)
			{
				statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
				statement.setInt(1, castleId);
				statement.execute();
				statement.close();
			}
			if (fortId != 0)
			{
				Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					L2Clan owner = fort.getOwnerClan();
					if (clan == owner)
						fort.removeOwner(true);
				}
			}
			if (Config.DEBUG)
				_log.fine("clan removed in db: " + clanId);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan from DB.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public void scheduleRemoveClan(final int clanId)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (getClan(clanId) == null)
				{
					return;
				}
				if (getClan(clanId).getDissolvingExpiryTime() != 0)
				{
					destroyClan(clanId);
				}
			}
		}, Math.max(getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis(), 300000));
	}
	
	public void scheduleStopWar(final int clanId1, final int clanId2)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (getClan(clanId1) == null || getClan(clanId2) == null)
				{
					return;
				}
			}
		}, Math.max(getClan(clanId1).getDissolvingExpiryTime() - System.currentTimeMillis(), 300000));
	}
	
	public boolean isAllyExists(String allyName)
	{
		for (L2Clan clan : getClans())
		{
			if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName))
			{
				return true;
			}
		}
		return false;
	}
	
	public void storeclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2, penalty) VALUES(?,?,?,?,?)");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.setLong(5, System.currentTimeMillis() + (12 * 60 * 60 * 1000));
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error storing clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		// SystemMessage msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_BEGUN);
		//
		SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		// msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_BEGUN);
		// msg.addString(clan1.getName());
		// clan2.broadcastToOnlineMembers(msg);
		// clan1 declared clan war.
		msg = new SystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public boolean checkForTwoSidedClanWar(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		boolean twoSidedWar = false;
		if (clan1.isAtWarWith(clanId2) && clan2.isAtWarWith(clanId1))
		{
			twoSidedWar = true;
		}
		return twoSidedWar;
	}
	
	public void storeTwoSidedWar(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO clan_wars_two_sided (warUniqueId, clan1, clan2, clan1pvps, clan2pvps, warFinishTime) VALUES(?,?,?,?,?,?)");
			statement.setInt(1, IdFactory.getInstance().getNextId());
			statement.setInt(2, clanId1);
			statement.setInt(3, clanId2);
			statement.setInt(4, 0);
			statement.setInt(5, 0);
			statement.setLong(6, System.currentTimeMillis() + (2 * 60 * 1000));
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error storing clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		// SystemMessage msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_BEGUN);
		//
		// Date dateToFinish = new Date(System.currentTimeMillis() + (0 * 5 * 60 * 1000));
		// DateFormat df = new SimpleDateFormat("dd:MM:yy:HH:mm:ss");
		Long currentDate = System.currentTimeMillis();
		Date dateToFinish = new Date(currentDate + (2 * 60 * 1000));
		clan1.broadcastToOnlineMembers("The war against " + clan2.getName() + " will end on: " + dateToFinish);
		clan2.broadcastToOnlineMembers("The war against " + clan1.getName() + " will end on: " + dateToFinish);
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
	}
	
	public void endTwoSidedWar(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		// clan1.tempWarClanIds.add(clan2.getClanId());
		clan1.deleteEnemyClan(clan2);
		clan2.deleteEnemyClan(clan1);
		clan1.deleteAttackerClan(clan2);
		clan2.deleteAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId2);
			statement.setInt(2, clanId1);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		// SystemMessage msg1 = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		// msg1.addString(clan2.getName());
		// SystemMessage msg2 = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		// msg1.addString(clan1.getName());
		// clan1.broadcastToOnlineMembers(msg1);
		// clan2.broadcastToOnlineMembers(msg2);
	}
	
	public void endTwoSidedWarByUniqueWarId(int warUniqueId)
	{
		int clan1id = 0;
		int clan2id = 0;
		int clan1pvps;
		int clan2pvps;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT clan1,clan2,clan1pvps,clan2pvps FROM clan_wars_two_sided where warUniqueId=?");
			statement.setInt(1, warUniqueId);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				clan1id = rset.getInt(1);
				clan2id = rset.getInt(2);
				clan1pvps = rset.getInt(3);
				clan2pvps = rset.getInt(4);
				L2Clan clan1 = ClanTable.getInstance().getClan(clan1id);
				L2Clan clan2 = ClanTable.getInstance().getClan(clan2id);
				if (clan1 != null && clan2 != null)
				{
					clan1.deleteEnemyClan(clan2);
					clan2.deleteEnemyClan(clan1);
					clan1.deleteAttackerClan(clan2);
					clan2.deleteAttackerClan(clan1);
					clan1.broadcastClanStatus();
					clan2.broadcastClanStatus();
					SystemMessage msg1 = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
					msg1.addString(clan2.getName());
					SystemMessage msg2 = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
					msg2.addString(clan1.getName());
					clan1.broadcastToOnlineMembers(msg1);
					clan2.broadcastToOnlineMembers(msg2);
					if (clan1pvps > clan2pvps)
					{
						clan1.broadcastToOnlineMembers("Your clan won the war against " + clan2.getName() + " [" + clan1pvps + "/" + clan2pvps + "].");
						clan2.broadcastToOnlineMembers("Your clan lost the war against " + clan1.getName() + " [" + clan2pvps + "/" + clan1pvps + "].");
					}
					else if (clan2pvps > clan1pvps)
					{
						clan2.broadcastToOnlineMembers("Your clan won the war against " + clan1.getName() + " [" + clan2pvps + "/" + clan1pvps + "].");
						clan1.broadcastToOnlineMembers("Your clan lost the war against " + clan2.getName() + " [" + clan1pvps + "/" + clan2pvps + "].");
					}
					else if (clan2pvps == clan1pvps)
					{
						clan2.broadcastToOnlineMembers("Clan war against " + clan1.getName() + " ended as a DRAW [" + clan2pvps + "/" + clan1pvps + "].");
						clan1.broadcastToOnlineMembers("Clan war against " + clan2.getName() + " ended as a DRAW [" + clan1pvps + "/" + clan2pvps + "].");
					}
					Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
					for (L2PcInstance cha : players)
					{
						L2PcInstance target = null;
						if (cha.getActingPlayer().getTarget() != null)
						{
							target = cha.getTarget().getActingPlayer();
							if (cha.getClan() == clan1 || cha.getClan() == clan2)
							{
								if (target != null)
								{
									if (target instanceof L2PcInstance)
									{
										if (target.getClan() != null)
										{
											if ((target.getClan() == clan1 || target.getClan() == clan2))
											{
												cha.abortAttack();
												cha.abortCast();
											}
										}
									}
								}
							}
						}
						cha.broadcastUserInfo();
					}
				}
			}
			rset.close();
			statement.close();
			statement = con.prepareStatement("DELETE FROM clan_wars_two_sided where warUniqueId=?");
			statement.setInt(1, warUniqueId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		if (clan1id != 0 && clan2id != 0)
		{
			endTwoSidedWar(clan1id, clan2id);
		}
	}
	
	public void deleteclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		clan1.tempWarClanIds.add(clan2.getClanId());
		// for(L2ClanMember player: clan1.getMembers())
		// {
		// if(player.getPlayerInstance()!=null)
		// player.getPlayerInstance().setWantsPeace(0);
		// }
		// for(L2ClanMember player: clan2.getMembers())
		// {
		// if(player.getPlayerInstance()!=null)
		// player.getPlayerInstance().setWantsPeace(0);
		// }
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();
			// statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			// statement.setInt(1,clanId2);
			// statement.setInt(2,clanId1);
			// statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		// SystemMessage msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_ENDED);
		SystemMessage msg = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		clan1.broadcastToOnlineMembers("This will be effective after the next server restart.");
		msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
		clan2.broadcastToOnlineMembers("This will be effective after the next server restart.");
		// msg = new SystemMessage(SystemMessageId.WAR_WITH_THE_S1_CLAN_HAS_ENDED);
		// msg.addString(clan1.getName());
		// clan2.broadcastToOnlineMembers(msg);
	}
	
	public void checkSurrender(L2Clan clan1, L2Clan clan2)
	{
		int count = 0;
		for (L2ClanMember player : clan1.getMembers())
		{
			if (player != null && player.getPlayerInstance().getWantsPeace() == 1)
				count++;
		}
		if (count == clan1.getMembers().length - 1)
		{
			clan1.deleteEnemyClan(clan2);
			clan2.deleteEnemyClan(clan1);
			deleteclanswars(clan1.getClanId(), clan2.getClanId());
		}
	}
	
	private void restorewars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT clan1, clan2, wantspeace1, wantspeace2 FROM clan_wars");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				getClan(rset.getInt("clan1")).setEnemyClan(rset.getInt("clan2"));
				getClan(rset.getInt("clan2")).setAttackerClan(rset.getInt("clan1"));
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan wars data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public final void addClanMember(L2Clan clan, L2PcInstance target, int pledgeType)
	{
		clan.addClanMember(target);
		target.setPledgeType(pledgeType);
		target.setClanPrivileges(clan.getRankPrivs(target.getPowerGrade()));
		target.sendPacket(new SystemMessage(SystemMessageId.ENTERED_THE_CLAN));
		if (clan.getHasCastle() > 0)
		{
			CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(target);
		}
		if (clan.getHasFort() > 0)
		{
			FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(target);
		}
		target.sendSkillList();
		clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(target), target);
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		// this activates the clan tab on the new member
		target.sendPacket(new PledgeShowMemberListAll(clan, target));
		target.setClanJoinExpiryTime(0);
		target.broadcastUserInfo();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ClanTable _instance = new ClanTable();
	}
}
