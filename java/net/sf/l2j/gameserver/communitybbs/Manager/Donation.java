/*
 * Copyright (C) 2004-2015 L2J DataPack
 * This file is part of L2J DataPack.
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.List;
import java.util.Map;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.donation.DonationManager;
import net.sf.l2j.gameserver.donation.DonationManager.PaypalData;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class Donation extends BaseBBSManager
{
	@Override
	public void parsecmd(String commands, L2PcInstance player)
	{
		//player.sendMessage("" + commands);
		String[] command = getCommands(commands);
		//player.sendMessage(commands);
		switch (command[0])
		{
			default:
			case "paypal":
			{
				// player.sendPacket(new ExPCCafePointInfo(player.getPcPoints()));
				if (command.length > 1)
				{
					if (command.length == 6)
					{
						String id = command[1] + command[2] + command[3] + command[4] + command[5];
						if (id.length() == 17 && isAlphaNumeric(id))
						{
							DonationManager.getInstance().queueTransactionId(player, id);
						}
						else
						{
							//player.sendMessage("1");
							player.sendPacket(new ExShowScreenMessage("Transaction ID rejected as not valid.", 5000));
						}
					}
					else
					{
						//player.sendMessage("2");
						player.sendPacket(new ExShowScreenMessage("Transaction ID rejected as not valid.", 5000));
					}
				}
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setHtml(getPage(player, "donation", "paypal"));
				final String trans1;
				if (player.isGM())
				{
					trans1 = "<button value=Admin action=\"bypass bbs_add_fav paypalpage\" width=60 height=24 fore=L2UI_CT1.Tab_DF_Tab_Unselected back=L2UI_CT1.Tab_DF_Tab_Selected>";
				}
				else
				{
					trans1 = "<img src=\"\" width=0 height=20>";
				}
				html.replace("%trans1%", trans1);
				player.sendPacket(html);
				player.sendPacket(new ShowBoard()); // close
				break;
			}
			case "paypalpage":
			{
				if (!player.isGM())
				{
					break;
				}
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setHtml(getPage(player, "donation", "paypalpage"));
				int page = 0;
				if (command.length > 1)
				{
					page = Integer.parseInt(command[1]);
				}
				int type = 0;
				if (command.length > 2)
				{
					if (isDigit(command[2]))
					{
						type = Integer.parseInt(command[2]);
					}
				}
				String match = "";
				if (command.length > 3)
				{
					match = command[3];
				}
				Map<Integer, List<PaypalData>> list = DonationManager.getInstance().getGeneratedPaypalList(type, match);
				html.replace("%size%", "" + list.size());
				html.replace("%list%", getPaypalList(list.get(page), page, type, match));
				html.replace("%page%", "" + (page + 1));
				html.replace("%prev%", "" + Math.max(0, page - 1));
				html.replace("%next%", "" + Math.min(list.size() - 1, page + 1));
				html.replace("%type%", "" + type);
				html.replace("%match%", match);
				player.sendPacket(html);
				break;
			}
			case "paypalview":
			{
				if (!player.isGM())
				{
					break;
				}
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setHtml(getPage(player, "donation", "paypalview"));
				if (command.length > 2)
				{
					int id = Integer.parseInt(command[1]);
					int page = Integer.parseInt(command[2]);
					String type = "";
					if (command.length > 3)
					{
						type = command[3];
					}
					String match = "";
					if (command.length > 4)
					{
						match = command[4];
					}
					PaypalData donation = DonationManager.getInstance().getPaypalData(id);
					html.replace("%id%", split(donation.getTransactionId()));
					html.replace("%currency%", donation.getCurrency());
					html.replace("%date%", donation.getDate());
					html.replace("%email%", donation.getEmail());
					if (donation.getReceiverId() > 0)
					{
						html.replace("%receiver_id%", "" + donation.getReceiverId());
						html.replace("%receiver_name%", donation.getReceiverName());
						html.replace("%claimed_date%", donation.getClaimedDate());
					}
					else
					{
						html.replace("%receiver_id%", "");
						html.replace("%receiver_name%", "");
						html.replace("%claimed_date%", "");
					}
					html.replace("%amount%", "" + donation.getAmount());
					html.replace("%page%", "" + page);
					html.replace("%type%", type);
					html.replace("%match%", match);
					player.sendPacket(html);
				}
				break;
			}
		}
	}
	
	private String getPaypalList(List<PaypalData> donationList, int page, int type, String match)
	{
		final StringBuilder list = new StringBuilder();
		for (PaypalData donation : donationList)
		{
			list.append("<table width=293 cellspacing=1 cellpadding=0 background=L2UI_CT1.Windows_DF_TooltipBG>");
			list.append("<tr>");
			list.append("<td width=15>");
			list.append("<button value=\" \" action=\"bypass bbs_add_fav paypalview " + donation.getId() + " " + page + " " + type + " " + match + "\" width=10 height=26 back=L2UI_CT1.Button_DF_BuffMore_Down fore=L2UI_CT1.Button_DF_BuffMore />");
			list.append("</td>");
			list.append("<td width=200>" + split(donation.getTransactionId()) + "</td>");
			list.append("<td width=60 align=center>" + donation.getAmount() + "</td>");
			list.append("<td width=22>");
			list.append("<img src=l2ui." + (donation.isClaimed() ? "Attack" : "Normal") + " width=20 height=24>");
			list.append("</td>");
			list.append("</tr>");
			list.append("</table>");
		}
		return list.toString();
	}
	
	private String split(String text)
	{
		String[] part = text.split("(?<=\\G.{4})");
		String split = "";
		for (String str : part)
		{
			split += str;
			split += " ";
		}
		return split;
	}
	
	public String[] getCommands(String cmd)
	{
		int lenght = cmd.split(" ").length - 1;
		String[] command = new String[lenght];
		for (int i = 0; i < lenght; i++)
		{
			command[i] = cmd.split(" ")[i + 1];
		}
		return command;
	}
	
	public String getPage(L2PcInstance player, String section, String page)
	{
		String content = HtmCache.getInstance().getHtm("data/html/CommunityBoard/" + section + "/" + page + ".htm");
		if (content == null)
		{
			content = "<html><title>Community</title><br><br><center>404 :File Not found: '" + page + "' </center></html>";
		}
		return content;
	}
	
	public static boolean isDigit(String text)
	{
		if (text == null || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isDigit(c))
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isAlphaNumeric(String text)
	{
		if (text == null || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isLetterOrDigit(c))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{}
	
	public static Donation getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Donation _instance = new Donation();
	}
}
