package net.sf.l2j.gameserver.handler.admincommandhandlers;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminVIPEngine implements IAdminCommandHandler
{
	private static final String[]	ADMIN_COMMANDS	=
													{
			"admin_vip",
			"admin_vip_setteam",
			"admin_vip_randomteam",
			"admin_vip_settime",
			"admin_vip_endnpc",
			"admin_vip_setdelay",
			"admin_vip_joininit",
			"admin_vip_joinnpc",
			"admin_vip_joinlocxyz",
			"admin_vip_setarea",
			"admin_vip_vipreward",
			"admin_vip_viprewardamount",
			"admin_vip_thevipreward",
			"admin_vip_theviprewardamount",
			"admin_vip_notvipreward",
			"admin_vip_notviprewardamount",		};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_vip"))
			showMainPage(activeChar);

		else if (command.startsWith("admin_vip_setteam "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_setteam <team>");
				return false;
			}

			VIP.setTeam(params[1], activeChar);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_randomteam"))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			VIP.setRandomTeam(activeChar);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_settime "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_settime <time>");
				return false;
			}

			VIP._time = Integer.valueOf(params[1]) * 60 * 1000;
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_endnpc "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_endnpc <npc>");
				return false;
			}

			VIP.endNPC(Integer.valueOf(params[1]), activeChar);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_joinnpc "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_joinnpc <npc>");
				return false;
			}

			VIP.joinNPC(Integer.valueOf(params[1]), activeChar);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_setdelay "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_setdelay <time>");
				return false;
			}

			VIP._delay = Integer.valueOf(params[1]) * 60 * 1000;
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_joininit"))
			VIP.startJoin(activeChar);

		else if (command.startsWith("admin_vip_joinlocxyz "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 4)
			{
				activeChar.sendMessage("Wrong usage: //vip_joinlocxyz <x> <y> <z>");
				return false;
			}

			VIP.setJoinLOC(params[1], params[2], params[3]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_joinnpc "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_joinnpc <npc>");
				return false;
			}

			VIP.joinNPC(Integer.valueOf(params[1]), activeChar);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_setarea "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_setarea <Area>");
				return false;
			}
			VIP._joinArea = params[1];
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_vipreward "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_vipreward <id>");
				return false;
			}

			VIP._vipReward = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_viprewardamount "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_viprewardamount <amount>");
				return false;
			}

			VIP._vipRewardAmount = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_notvipreward "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_notvipreward <id>");
				return false;
			}

			VIP._notVipReward = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_notviprewardamount "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_notviprewardamount <amount>");
				return false;
			}

			VIP._notVipRewardAmount = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_thevipreward "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_thevipreward <id>");
				return false;
			}

			VIP._theVipReward = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		else if (command.startsWith("admin_vip_theviprewardamount "))
		{
			if (VIP._started || VIP._joining)
			{
				activeChar.sendMessage("Cannot change variables when event has already started");
				return false;
			}

			String[] params;
			params = command.split(" ");

			if (params.length != 2)
			{
				activeChar.sendMessage("Wrong usage: //vip_theviprewardamount <amount>");
				return false;
			}

			VIP._theVipRewardAmount = Integer.valueOf(params[1]);
			showMainPage(activeChar);
		}

		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	public void showMainPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		
		int howLongAgoInMinutes = 0;
        
        if (TvT.lastEventTime > 0)
        {        	
	        long time = System.currentTimeMillis() - TvT.lastEventTime;
	        
        	howLongAgoInMinutes = (int)((time/1000)/60);
        	
        	if (howLongAgoInMinutes < 1)
        		howLongAgoInMinutes = 1;	        
        }

        replyMSG.append("<center><font color=\"LEVEL\">[VIP Engine] (last event time: </font>"+howLongAgoInMinutes+" minutes ago)"+"</center><br><br><br>");
		replyMSG.append("<table><tr><td><edit var=\"input1\" width=\"50\"></td><td><edit var=\"input2\" width=\"50\"></td><td><edit var=\"input3\" width\"50\"></td></tr></table>");

		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG		.append("<td width=\"100\"><button value=\"Set Team\" action=\"bypass -h admin_vip_setteam $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Random Team\" action=\"bypass -h admin_vip_randomteam\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr><tr>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Set Time\" action=\"bypass -h admin_vip_settime $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Set Delay\" action=\"bypass -h admin_vip_setdelay $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Set Area\" action=\"bypass -h admin_vip_setarea $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr><tr>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"End NPC\" action=\"bypass -h admin_vip_endnpc $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Join NPC\" action=\"bypass -h admin_vip_joinnpc $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Join LOC\" action=\"bypass -h admin_vip_joinlocxyz $input1 $input2 $input3\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr><tr>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"VIP Reward\" action=\"bypass -h admin_vip_vipreward $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"VIP Reward Am\" action=\"bypass -h admin_vip_viprewardamount $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Not VIP Reward\" action=\"bypass -h admin_vip_notvipreward $input1\" width=130 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Not VIP Reward Am\" action=\"bypass -h admin_vip_notviprewardamount $input1\" width=130 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"The VIP Reward\" action=\"bypass -h admin_vip_thevipreward $input1\" width=130 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG
				.append("<td width=\"100\"><button value=\"The VIP Reward Am\" action=\"bypass -h admin_vip_theviprewardamount $input1\" width=130 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr><tr>");

		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG
				.append("<td width=\"100\"><button value=\"Start Join\" action=\"bypass -h admin_vip_joininit\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table><br><br>");

		replyMSG.append("<font color=\"LEVEL\">VIP Settings...</font><br1>");
		replyMSG.append("    ... Team:&nbsp;<font color=\"FFFFFF\">" + VIP._teamName + "</font><br>");
		replyMSG.append("    ... Delay:&nbsp;<font color=\"FFFFFF\">" + VIP._delay / 1000 / 60 + "</font><br1>");
		replyMSG.append("    ... Time:&nbsp;<font color=\"FFFFFF\">" + VIP._time / 1000 / 60 + "</font><br>");
		replyMSG.append("    ... End NPC ID:&nbsp;<font color=\"FFFFFF\">" + VIP._endNPC + " (" + VIP.getNPCName(VIP._endNPC, activeChar) + ")</font><br1>");
		replyMSG.append("    ... Join NPC ID:&nbsp;<font color=\"FFFFFF\">" + VIP._joinNPC + " (" + VIP.getNPCName(VIP._joinNPC, activeChar) + ")</font><br>");
		replyMSG.append("    ... Start Location:&nbsp;<font color=\"FFFFFF\">" + VIP._startX + "," + VIP._startY + "," + VIP._startZ + "</font><br1>");
		replyMSG.append("    ... End Location:&nbsp;<font color=\"FFFFFF\">" + VIP._endX + "," + VIP._endY + "," + VIP._endZ + "</font><br1>");
		replyMSG.append("    ... Join Location:&nbsp;<font color=\"FFFFFF\">" + VIP._joinX + "," + VIP._joinY + "," + VIP._joinZ + "</font><br>");
		replyMSG.append("    ... VIP Team Reward:&nbsp;<font color=\"FFFFFF\">" + VIP.getItemName(VIP._vipReward, activeChar) + " (" + VIP._vipRewardAmount
				+ ")</font><br1>");
		replyMSG.append("    ... Not VIP Team Reward:&nbsp;<font color=\"FFFFFF\">" + VIP.getItemName(VIP._notVipReward, activeChar) + " ("
				+ VIP._notVipRewardAmount + ")</font><br1>");
		replyMSG.append("    ... VIP Reward:&nbsp;<font color=\"FFFFFF\">" + VIP.getItemName(VIP._theVipReward, activeChar) + " (" + VIP._theVipRewardAmount
				+ ")</font><br>");

		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}
