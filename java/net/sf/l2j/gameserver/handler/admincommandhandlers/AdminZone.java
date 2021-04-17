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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.StringUtil;



public class AdminZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_zone_check",
		"admin_zone_visual",
		"admin_zone_visual_clear"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (actualCommand.equalsIgnoreCase("admin_zone_check"))
			showHtml(activeChar);
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual"))
		{
			final String next = st.nextToken();
			if (next.equalsIgnoreCase("all"))
			{
				for (final L2ZoneType zone : ZoneManager.getInstance().getZones(activeChar))
					zone.visualizeZone(activeChar.getZ());
				showHtml(activeChar);
			}
			else
			{
				final int zoneId = Integer.parseInt(next);
				ZoneManager.getInstance().getZoneById(zoneId).visualizeZone(activeChar.getZ());
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_zone_visual_clear"))
		{
			ZoneManager.getInstance().clearDebugItems();
			showHtml(activeChar);
		}
		return true;
	}
	
	private static void showHtml(final L2PcInstance activeChar)
	{
		final int worldX = activeChar.getX();
		final int worldY = activeChar.getY();
		final int geoX = (worldX - (-327680) >> 4 >> 11) + 10;
		final int geoY = (worldY - (-262144) >> 4 >> 11) + 10;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile("data/html/admin/zone.htm");
		adminReply.replace("%MAPREGION%", "[x:" + MapRegionTable.getInstance().getMapRegionX(activeChar.getX()) + " y:" + MapRegionTable.getInstance().getMapRegionX(activeChar.getY()) + "]");
		adminReply.replace("%GEOREGION%", "" + geoX + "_" + geoY);
		adminReply.replace("%CLOSESTTOWN%", MapRegionTable.getInstance().getClosestTownName(activeChar));
		adminReply.replace("%CURRENTLOC%", "" + activeChar.getX() + ", " + activeChar.getY() + ", " + activeChar.getZ());
		adminReply.replace("%PVP%", activeChar.isInsideZone(L2Character.ZONE_PVP) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%PEACE%", activeChar.isInsideZone(L2Character.ZONE_PEACE) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%SIEGE%", activeChar.isInsideZone(L2Character.ZONE_SIEGE) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%MOTHERTREE%", activeChar.isInsideZone(L2Character.ZONE_MOTHERTREE) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%CLANHALL%", activeChar.isInsideZone(L2Character.ZONE_CLANHALL) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%NOLANDING%", activeChar.isInsideZone(L2Character.ZONE_NOLANDING) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%WATER%", activeChar.isInsideZone(L2Character.ZONE_WATER) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%JAIL%", activeChar.isInsideZone(L2Character.ZONE_JAIL) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%MONSTERTRACK%", activeChar.isInsideZone(L2Character.ZONE_MONSTERTRACK) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%CASTLE%", activeChar.isInsideZone(L2Character.ZONE_CASTLE) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%SWAMP%", activeChar.isInsideZone(L2Character.ZONE_SWAMP) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%NOSUMMONFRIEND%", activeChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%NOSTORE%", activeChar.isInsideZone(L2Character.ZONE_NOSTORE) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%TOWN%", activeChar.isInsideZone(L2Character.ZONE_TOWN) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%HQ%", activeChar.isInsideZone(L2Character.ZONE_HQ) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		adminReply.replace("%FARM%", activeChar.isInsideZone(L2Character.ZONE_FARM) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		//adminReply.replace("%DANGERAREA%", activeChar.isInsideZone(L2Character.ZONE_DANGERAREA) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		//adminReply.replace("%CASTONARTIFACT%", activeChar.isInsideZone(L2Character.ZONE_CASTONARTIFACT) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		//adminReply.replace("%NORESTART%", activeChar.isInsideZone(L2Character.ZONE_NORESTART) ? "<font color=\"LEVEL\">YES</font>" : "NO");
		final StringBuilder zones = new StringBuilder(100);
		final L2WorldRegion region = L2World.getInstance().getRegion(activeChar.getX(), activeChar.getY());
		for (final L2ZoneType zone : region.getZones())
			if (zone.isCharacterInZone(activeChar))
			{
				StringUtil.append(zones, String.valueOf(zone.getId()));
				StringUtil.append(zones, " ");
			}
		adminReply.replace("%ZLIST%", zones.toString());
		activeChar.sendPacket(adminReply);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}