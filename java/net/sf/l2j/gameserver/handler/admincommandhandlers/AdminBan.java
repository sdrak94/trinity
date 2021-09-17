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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;

import guard.HwidBanController;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.GMAudit;

/**
 * This class handles following admin commands:
 * - ban_acc <account_name> = changes account access level to -100 and logs him off. If no account is specified target's account is used.
 * - ban_char <char_name> = changes a characters access level to -100 and logs him off. If no character is specified target is used.
 * - ban_chat <char_name> <duration> = chat bans a character for the specified duration. If no name is specified the target is chat banned indefinitely.
 * - unban_acc <account_name> = changes account access level to 0.
 * - unban_char <char_name> = changes specified characters access level to 0.
 * - unban_chat <char_name> = lifts chat ban from specified player. If no player name is specified current target is used.
 * - jail charname [penalty_time] = jails character. Time specified in minutes. For ever if no time is specified.
 * - unjail charname = Unjails player, teleport him to Floran.
 *
 * @version $Revision: 1.1.6.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminBan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ban", // returns ban commands
		"admin_ban_acc",
		"admin_ban_char",
		"admin_ban_chat",
		"admin_unban", // returns unban commands
		"admin_unban_acc",
		"admin_unban_char",
		"admin_unban_chat",
		"admin_jail",
		"admin_unjail",
		"admin_ban_hwid",
		"admin_unban_hwid",
		"admin_ban_xmenu",
		"admin_xban_hwid",
		"admin_xban_hwid_char",
		"admin_xban_hwid_hwid"
	};
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		String player = "";
		int duration = -1;
		L2PcInstance targetPlayer = null;

		if (command.startsWith("admin_ban_xmenu"))
		{
			String content = HtmCache.getInstance().getHtmForce("data/html/admin/ban_menu.htm");
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setHtml(content);
			activeChar.sendPacket(adminReply);
			return false;
		}
		if (command.startsWith("admin_xban_hwid_char"))
		{
			player = st.nextToken();
			targetPlayer = L2World.getInstance().getPlayer(player);
			duration = Integer.parseInt(st.nextToken());
			String reason = "";
			if (st.hasMoreTokens())
			{
				reason = st.nextToken();
			}
			HwidBanController.getInstance().ban(targetPlayer, duration, reason);
			return false;
		}

		if (command.startsWith("admin_xban_hwid_hwid"))
		{
			String hwid = st.nextToken();
			duration = Integer.parseInt(st.nextToken());
			String reason = "";
			if (st.hasMoreTokens())
			{
				reason = st.nextToken();
			}
			HwidBanController.getInstance().ban(hwid, duration, reason);
			return false;
		}
		
		if (st.hasMoreTokens())
		{
			player = st.nextToken();
			targetPlayer = L2World.getInstance().getPlayer(player);
			if (st.hasMoreTokens())
			{
				try
				{
					duration = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Invalid number format used: " + nfe);
					return false;
				}
			}
		}
		else
		{
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
			{
				targetPlayer = (L2PcInstance) activeChar.getTarget();
			}
		}
		if (command.startsWith("admin_ban ") || command.equalsIgnoreCase("admin_ban"))
		{
			activeChar.sendMessage("Available ban commands: //ban_acc, //ban_char, //ban_chat");
			return false;
		}
		else if (command.startsWith("admin_ban_acc"))
		{
			if (targetPlayer != null && targetPlayer.equals(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
				return false;
			}
			// May need to check usage in admin_ban_menu as well.
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_acc <account_name> (if none, target char's account gets banned)");
				return false;
			}
			else if (targetPlayer == null)
			{
				LoginServerThread.getInstance().sendAccessLevel(player, -100);
				activeChar.sendMessage("Ban request sent for account " + player);
				auditAction(command, activeChar, player);
			}
			else
			{
				targetPlayer.setAccountAccesslevel(-100);
				targetPlayer.logout();
				RegionBBSManager.getInstance().changeCommunityBoard();
				activeChar.sendMessage("Account " + targetPlayer.getAccountName() + " banned.");
				auditAction(command, activeChar, targetPlayer.getAccountName());
			}
		}
		else if (command.startsWith("admin_ban_char"))
		{
			if (targetPlayer != null && targetPlayer.equals(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
				return false;
			}
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_char <char_name> (if none, target char is banned)");
				return false;
			}
			else
			{
				auditAction(command, activeChar, (targetPlayer == null ? player : targetPlayer.getName()));
				return changeCharAccessLevel(targetPlayer, player, activeChar, -100);
			}
		}
		else if (command.startsWith("admin_ban_chat"))
		{
			if (targetPlayer == null)
			{
				activeChar.sendMessage("Usage: //ban_chat <char_name> [penalty_minutes]");
				return false;
			}
			else if (targetPlayer.getPunishLevel().value() > L2PcInstance.PunishLevel.CHAT.value())
			{
				activeChar.sendMessage(targetPlayer.getName() + " is already jailed or banned.");
				return false;
			}
			if (targetPlayer != null && targetPlayer.equals(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
				return false;
			}
			String banLengthStr = "";
			targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.CHAT, duration);
			if (duration > 0)
				banLengthStr = " for " + duration + " minutes";
			activeChar.sendMessage(targetPlayer.getName() + " is now chat banned" + banLengthStr + ".");
			auditAction(command, activeChar, targetPlayer.getName());
		}
		else if (command.startsWith("admin_unban ") || command.equalsIgnoreCase("admin_unban"))
		{
			activeChar.sendMessage("Available unban commands: //unban_acc, //unban_char, //unban_chat");
			return false;
		}
		else if (command.startsWith("admin_unban_acc"))
		{
			// Need to check admin_unban_menu command as well in AdminMenu.java handler.
			if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so must not be banned.");
				return false;
			}
			else if (!player.equals(""))
			{
				LoginServerThread.getInstance().sendAccessLevel(player, 0);
				activeChar.sendMessage("Unban request sent for account " + player);
				auditAction(command, activeChar, player);
			}
			else
			{
				activeChar.sendMessage("Usage: //unban_acc <account_name>");
				return false;
			}
		}
		else if (command.startsWith("admin_unban_char"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //unban_char <char_name>");
				return false;
			}
			else if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so must not be banned.");
				return false;
			}
			else
			{
				auditAction(command, activeChar, player);
				return changeCharAccessLevel(null, player, activeChar, 0);
			}
		}
		else if (command.startsWith("admin_unban_chat"))
		{
			if (targetPlayer == null)
			{
				activeChar.sendMessage("Usage: //unban_chat <onlinecharname>");
				return false;
			}
			else if (targetPlayer.isChatBanned())
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
				activeChar.sendMessage(targetPlayer.getName() + "'s chat ban has now been lifted.");
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				activeChar.sendMessage(targetPlayer.getName() + " is not currently chat banned.");
			}
		}
		else if (command.startsWith("admin_jail"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //jail <charname> [penalty_minutes] (if no name is given, selected target is jailed indefinitely)");
				return false;
			}
			if (targetPlayer != null)
			{
				if (targetPlayer.isFlyingMounted())
					targetPlayer.stopTransformation(null);
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.JAIL, duration);
				activeChar.sendMessage("Character " + targetPlayer.getName() + " jailed for " + (duration > 0 ? duration + " minutes." : "ever!"));
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				jailOfflinePlayer(activeChar, player, duration);
				auditAction(command, activeChar, player);
			}
		}
		else if (command.startsWith("admin_unjail"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //unjail <charname> (If no name is given target is used)");
				return false;
			}
			else if (targetPlayer != null)
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
				activeChar.sendMessage("Character " + targetPlayer.getName() + " removed from jail");
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				unjailOfflinePlayer(activeChar, player);
				auditAction(command, activeChar, player);
			}
		}
		return true;
	}
	
	private void auditAction(String fullCommand, L2PcInstance activeChar, String target)
	{
		if (!Config.GMAUDIT)
			return;
		String[] command = fullCommand.split(" ");
		GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
	}
	
	private void jailOfflinePlayer(L2PcInstance activeChar, String name, int delay)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, -114356);
			statement.setInt(2, -249645);
			statement.setInt(3, -2984);
			statement.setInt(4, L2PcInstance.PunishLevel.JAIL.value());
			statement.setLong(5, (delay > 0 ? delay * 60000L : 0));
			statement.setString(6, name);
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
				activeChar.sendMessage("Character not found!");
			else
				activeChar.sendMessage("Character " + name + " jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing player");
			if (Config.DEBUG)
				se.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				if (Config.DEBUG)
					e.printStackTrace();
			}
		}
	}
	
	private void unjailOfflinePlayer(L2PcInstance activeChar, String name)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, 17836);
			statement.setInt(2, 170178);
			statement.setInt(3, -3507);
			statement.setInt(4, 0);
			statement.setLong(5, 0);
			statement.setString(6, name);
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
				activeChar.sendMessage("Character not found!");
			else
				activeChar.sendMessage("Character " + name + " removed from jail");
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing player");
			if (Config.DEBUG)
				se.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				if (Config.DEBUG)
					e.printStackTrace();
			}
		}
	}
	
	private boolean changeCharAccessLevel(L2PcInstance targetPlayer, String player, L2PcInstance activeChar, int lvl)
	{
		if (targetPlayer != null)
		{
			targetPlayer.setAccessLevel(lvl);
			targetPlayer.logout();
			RegionBBSManager.getInstance().changeCommunityBoard();
			activeChar.sendMessage("The character " + targetPlayer.getName() + " has now been banned.");
		}
		else
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
				statement.setInt(1, lvl);
				statement.setString(2, player);
				statement.execute();
				int count = statement.getUpdateCount();
				statement.close();
				if (count == 0)
				{
					activeChar.sendMessage("Character not found or access level unaltered.");
					return false;
				}
				else
					activeChar.sendMessage(player + " now has an access level of " + lvl);
			}
			catch (SQLException se)
			{
				activeChar.sendMessage("SQLException while changing character's access level");
				if (Config.DEBUG)
					se.printStackTrace();
				return false;
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
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}