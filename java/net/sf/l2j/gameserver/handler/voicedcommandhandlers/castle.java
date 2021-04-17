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
package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;

/**
 *
 *
 */
public class castle implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"open doors",
		"close doors",
		"ride wyvern",
		"clanrecall"
	};
	
	/**
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (command.startsWith("open doors") && target.equals("castle") && (activeChar.isClanLeader()))
		{
			L2DoorInstance door = (L2DoorInstance) activeChar.getTarget();
			Castle castle = CastleManager.getInstance().getCastleById(activeChar.getClan().getHasCastle());
			if (door == null || castle == null)
				return false;
			if (castle.checkIfInZone(door.getX(), door.getY(), door.getZ()))
			{
				door.openMe();
			}
		}
		else if (command.startsWith("close doors") && target.equals("castle") && (activeChar.isClanLeader()))
		{
			L2DoorInstance door = (L2DoorInstance) activeChar.getTarget();
			Castle castle = CastleManager.getInstance().getCastleById(activeChar.getClan().getHasCastle());
			if (door == null || castle == null)
				return false;
			if (castle.checkIfInZone(door.getX(), door.getY(), door.getZ()))
			{
				door.closeMe();
			}
		}
		else if (command.startsWith("ride wyvern") && target.equals("castle"))
		{
			if (activeChar.getClan().getHasCastle() > 0 && activeChar.isClanLeader())
			{
				activeChar.mount(12621, 0, true);
			}
		}
		else if (command.startsWith("clanrecall"))
		{
			if (activeChar.getClan().getHasCastle() > 0 && activeChar.isClanLeader())
			{
				if (activeChar.getInstanceId() != 0 || activeChar.isInFunEvent() || activeChar.isInCombat())
				{
					activeChar.sendMessage("You are in a state that doesn't allow recalls (Instance, Event, Combat)");
					return false;
				}

				if (!(activeChar.isInsideZone(L2Character.ZONE_PEACE) || activeChar.isInsideZone(L2Character.ZONE_CLANHALL) || activeChar.isInsideZone(L2Character.ZONE_CASTLE) || activeChar.isInsideZone(L2Character.ZONE_FORT)))
				{
					activeChar.sendMessage("You can use that command on while in Peace Zone,Clan Hall,Castle,Fortress");
					return false;
				}
				Connection con = null;
				boolean exit = false;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("SELECT time FROM clan_recalls WHERE clan_id=?");
					statement.setInt(1, activeChar.getClanId());
					ResultSet rset = statement.executeQuery();
					
					if (rset.next())
					{
						final long penalty = rset.getLong("time");
						
						if (penalty > 0 && penalty > System.currentTimeMillis())
						{
							long hours = TimeUnit.MILLISECONDS.toHours(penalty) - TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
							
							activeChar.sendMessage("You need to wait an additional "+ hours + " Hours before recalling your clan");
							exit = true;
						}
					}
					
					rset.close();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Error trying to restoring clanwar penalty for clan "+activeChar.getClan().getName(), e);
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception e)
					{
					}
				}
				if (!exit)
				{
					clanRecall(activeChar, activeChar.getClanId());
				}
				
			}
		}
		return true;
	}
	public void clanRecall(L2PcInstance activeChar, int clanId)
	{
		int x = activeChar.getX(), y = activeChar.getY(), z = activeChar.getZ();
		try
		{
			L2Clan clan = activeChar.getClan();
			L2PcInstance[] members = clan.getOnlineMembers(0);
			for (L2PcInstance member : members)
			{
				if (member.getInstanceId() != 0 || member.isInFunEvent() || member.isInCombat())
				{
					activeChar.sendMessage(member.getName() + " is in a state that doesn't allow recalls (Instance, Event, Combat)");
					continue;
				}
				if (member == activeChar)
				{
					continue;
				}
				member.teleToLocation(x, y, z, true);
				member.sendMessage("Your clan leader is teleporting you.");
			}
			storeClanRecall(activeChar.getClanId());
		}
		catch (Exception e)
		{}
	}
	public void storeClanRecall(int clanId1)
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			PreparedStatement statement2;

			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("SELECT * FROM clan_recalls WHERE clan_id = ?");
			statement.setInt(1, clanId1);
			
			ResultSet rset = statement.executeQuery();
			
			if(!rset.next())
			{
				statement = con.prepareStatement("INSERT INTO clan_recalls (clan_id, time) VALUES(?,?)");
				statement.setInt(1, clanId1);
				statement.setLong(2, System.currentTimeMillis() + (12 * 60 * 60 * 1000));
				statement.execute();
				statement.close();
				rset.close();
				
			}
			else
			{
				long time = System.currentTimeMillis() + (12 * 60 * 60 * 1000);
				
				statement2 = con.prepareStatement("UPDATE clan_recalls SET time = ?  WHERE clan_id = ?");
				statement2.setLong(1, time);
				statement2.setInt(2, clanId1);
				statement2.execute();
				statement2.close();
				rset.close();
			}
			return;
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error storing clan recall data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
