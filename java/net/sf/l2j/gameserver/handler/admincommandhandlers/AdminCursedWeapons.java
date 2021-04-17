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

import java.util.Collection;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.CursedWeapon;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class handles following admin commands:
 * - cw_info = displays cursed weapon status
 * - cw_remove = removes a cursed weapon from the world, item id or name must be provided
 * - cw_add = adds a cursed weapon into the world, item id or name must be provided. Target will be the weilder
 * - cw_goto = teleports GM to the specified cursed weapon
 * - cw_reload = reloads instance manager
 * @version $Revision: 1.1.6.3 $ $Date: 2007/07/31 10:06:06 $
 */
public class AdminCursedWeapons implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cw_info",
		"admin_cw_remove",
		"admin_cw_goto",
		"admin_cw_reload",
		"admin_cw_add",
		"admin_cw_info_menu"
	};
	
	private int itemId;
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		
		CursedWeaponsManager cwm = CursedWeaponsManager.getInstance();
		int id = 0;
		
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.startsWith("admin_cw_info"))
		{
			if (!command.contains("menu"))
			{
				activeChar.sendMessage("====== Cursed Weapons: ======");
				for (CursedWeapon cw : cwm.getCursedWeapons())
				{
					activeChar.sendMessage("> " + cw.getName() + " (" + cw.getItemId() + ")");
					if (cw.isActivated())
					{
						L2PcInstance pl = cw.getPlayer();
						activeChar.sendMessage("  Player holding: " + pl == null ? "null" : pl.getName());
						activeChar.sendMessage("    Player karma: " + cw.getPlayerKarma());
						activeChar.sendMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendMessage("    Kills : " + cw.getNbKills());
					}
					else if (cw.isDropped())
					{
						activeChar.sendMessage("  Lying on the ground.");
						activeChar.sendMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendMessage("    Kills : " + cw.getNbKills());
					}
					else
					{
						activeChar.sendMessage("  Don't exist in the world.");
					}
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));
				}
			}
			else
			{
                            final Collection<CursedWeapon> cws = cwm.getCursedWeapons();
                            final StringBuilder replyMSG =
                                    new StringBuilder(cws.size() * 300);
                            NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
                            adminReply.setFile("data/html/admin/cwinfo.htm");
                            for (CursedWeapon cw : cwm.getCursedWeapons()) {
                                itemId = cw.getItemId();

                                StringUtil.append(replyMSG,
                                        "<table width=270><tr><td>Name:</td><td>",
                                        cw.getName(),
                                        "</td></tr>");

                                if (cw.isActivated()) {
                                    L2PcInstance pl = cw.getPlayer();
                                    StringUtil.append(replyMSG,
                                            "<tr><td>Weilder:</td><td>",
                                            (pl == null ? "null" : pl.getName()),
                                            "</td></tr>" +
                                            "<tr><td>Karma:</td><td>",
                                            String.valueOf(cw.getPlayerKarma()),
                                            "</td></tr>" +
                                            "<tr><td>Kills:</td><td>",
                                            String.valueOf(cw.getPlayerPkKills()),
                                            "/",
                                            String.valueOf(cw.getNbKills()),
                                            "</td></tr>" +
                                            "<tr><td>Time remaining:</td><td>",
                                            String.valueOf(cw.getTimeLeft() / 60000),
                                            " min.</td></tr>" +
                                            "<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ",
                                            String.valueOf(itemId),
                                            "\" width=73 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
                                            "<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ",
                                            String.valueOf(itemId),
                                            "\" width=73 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>"
                                            );
                                }
                                else if (cw.isDropped()) {
                                    StringUtil.append(replyMSG,
                                            "<tr><td>Position:</td><td>Lying on the ground</td></tr>" +
                                            "<tr><td>Time remaining:</td><td>",
                                            String.valueOf(cw.getTimeLeft() / 60000),
                                            " min.</td></tr>" +
                                            "<tr><td>Kills:</td><td>",
                                            String.valueOf(cw.getNbKills()),
                                            "</td></tr>" +
                                            "<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ",
                                            String.valueOf(itemId),
                                            "\" width=73 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
                                            "<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ",
                                            String.valueOf(itemId),
                                            "\" width=73 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>"
                                            );
                                } else {
                                    StringUtil.append(replyMSG,
                                            "<tr><td>Position:</td><td>Doesn't exist.</td></tr>" +
                                            "<tr><td><button value=\"Give to Target\" action=\"bypass -h admin_cw_add ",
                                            String.valueOf(itemId),
                                            "\" width=99 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td></td></tr>"
                                            );
                                }

                                replyMSG.append("</table><br>");
                            }
                            adminReply.replace("%cwinfo%", replyMSG.toString());
                            activeChar.sendPacket(adminReply);
			}
		}
		else if (command.startsWith("admin_cw_reload"))
		{
			cwm.reload();
		}
		else
		{
			CursedWeapon cw = null;
			try
			{
				String parameter = st.nextToken();
				if (parameter.matches("[0-9]*"))
					id = Integer.parseInt(parameter);
				else
				{
					parameter = parameter.replace('_', ' ');
					for (CursedWeapon cwp : cwm.getCursedWeapons())
					{
						if (cwp.getName().toLowerCase().contains(parameter.toLowerCase()))
						{
							id = cwp.getItemId();
							break;
						}
					}
				}
				cw = cwm.getCursedWeapon(id);
				if (cw == null)
				{
					activeChar.sendMessage("Unknown cursed weapon ID.");
					return false;
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //cw_remove|//cw_goto|//cw_add <itemid|name>");
			}
			
			if (command.startsWith("admin_cw_remove "))
			{
				cw.endOfLife();
			}
			else if (command.startsWith("admin_cw_goto "))
			{
				cw.goTo(activeChar);
			}
			else if (command.startsWith("admin_cw_add"))
			{
				if (cw == null)
				{
					activeChar.sendMessage("Usage: //cw_add <itemid|name>");
					return false;
				}
				else if (cw.isActive())
					activeChar.sendMessage("This cursed weapon is already active.");
				else
				{
					L2Object target = activeChar.getTarget();
					if (target instanceof L2PcInstance)
						((L2PcInstance) target).addItem("AdminCursedWeaponAdd", id, 1, target, true);
					else
						activeChar.addItem("AdminCursedWeaponAdd", id, 1, activeChar, true);
				}
			}
			else
			{
				activeChar.sendMessage("Unknown command.");
			}
		}
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}