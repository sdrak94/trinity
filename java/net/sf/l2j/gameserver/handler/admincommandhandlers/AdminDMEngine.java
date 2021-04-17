package net.sf.l2j.gameserver.handler.admincommandhandlers;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminDMEngine implements IAdminCommandHandler
{
private static final String[]	ADMIN_COMMANDS	=
{
	"admin_dmevent",
	"admin_dmevent_name",
	"admin_dmevent_desc",
	"admin_dmevent_join_loc",
	"admin_dmevent_minlvl",
	"admin_dmevent_maxlvl",
	"admin_dmevent_npc",
	"admin_dmevent_npc_pos",
	"admin_dmevent_reward",
	"admin_dmevent_reward_amount",
	"admin_dmevent_spawnpos",
	"admin_dmevent_color",
	"admin_dmevent_join",
	"admin_dmevent_teleport",
	"admin_dmevent_start",
	"admin_dmevent_abort",
	"admin_dmevent_finish",
	"admin_dmevent_jointime",
	"admin_dmevent_eventtime",
	"admin_dmevent_sit",
	"admin_dmevent_dump",
	"admin_dmevent_save",
	"admin_dmevent_autostart",
"admin_dmevent_load"					};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_dmevent"))
		showMainPage(activeChar);
	else if (command.startsWith("admin_dmevent_name "))
	{
		DM._eventName = command.substring(19);
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_desc "))
	{
		DM._eventDesc = command.substring(19);
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_minlvl "))
	{
		if (!DM.checkMinLevel(Integer.valueOf(command.substring(21))))
			return false;
		DM._minlvl = Integer.valueOf(command.substring(21));
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_maxlvl "))
	{
		if (!DM.checkMaxLevel(Integer.valueOf(command.substring(21))))
			return false;
		DM._maxlvl = Integer.valueOf(command.substring(21));
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_join_loc "))
	{
		DM._joiningLocationName = command.substring(23);
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_npc "))
	{
		DM._npcId = Integer.valueOf(command.substring(18));
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_npc_pos"))
	{
		DM.setNpcPos(activeChar);
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_reward "))
	{
		DM._rewardId = Integer.valueOf(command.substring(21));
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_reward_amount "))
	{
		DM._rewardAmount = Integer.valueOf(command.substring(28));
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_spawnpos"))
	{
		DM.setPlayersPos(activeChar);
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_color "))
	{
		DM._playerColors = Integer.decode("0x" + command.substring(20)); // name/title color in client is BGR, not RGB
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_join"))
	{
		DM.startJoin();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_teleport"))
	{
		DM.teleportStart();
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_jointime "))
	{
		DM._joinTime = Integer.valueOf(command.substring(23)) * 60 * 1000;
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_dmevent_eventtime "))
	{
		DM._eventTime = Integer.valueOf(command.substring(24)) * 60 * 1000;
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_start"))
	{
		DM.startEvent();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_autostart"))
	{
		DM.autoEvent();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_abort"))
	{
		activeChar.sendMessage("Aborting event");
		DM.abortEvent();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_finish"))
	{
		DM.finishEvent();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_sit"))
	{
		DM.sit();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_load"))
	{
		DM.loadData(0);
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_save"))
	{
		DM.saveData();
		showMainPage(activeChar);
	}
	else if (command.equals("admin_dmevent_dump"))
		DM.dumpData();
	
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
	
	replyMSG.append("<center><font color=\"LEVEL\">[dm Engine]</font></center><br><br><br>");
	replyMSG.append("<table><tr><td><edit var=\"input1\" width=\"125\"></td><td><edit var=\"input2\" width=\"125\"></td></tr></table>");
	replyMSG.append("<table border=\"0\"><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Name\" action=\"bypass -h admin_dmevent_name $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Description\" action=\"bypass -h admin_dmevent_desc $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Join Location\" action=\"bypass -h admin_dmevent_join_loc $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Max lvl\" action=\"bypass -h admin_dmevent_maxlvl $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Min lvl\" action=\"bypass -h admin_dmevent_minlvl $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"NPC\" action=\"bypass -h admin_dmevent_npc $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"NPC Pos\" action=\"bypass -h admin_dmevent_npc_pos\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	
	replyMSG
	.append("</tr><tr><td width=\"100\"><button value=\"Join Time\" action=\"bypass -h admin_dmevent_jointime $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Event Time\" action=\"bypass -h admin_dmevent_eventtime $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Reward\" action=\"bypass -h admin_dmevent_reward $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Reward Amount\" action=\"bypass -h admin_dmevent_reward_amount $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"DM Color\" action=\"bypass -h admin_dmevent_color $input1\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"DM SpawnPos\" action=\"bypass -h admin_dmevent_spawnpos\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><table><br><br><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Join\" action=\"bypass -h admin_dmevent_join\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Teleport\" action=\"bypass -h admin_dmevent_teleport\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Start\" action=\"bypass -h admin_dmevent_start\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("</tr><tr><td width=\"100\"><button value=\"Auto Start\" action=\"bypass -h admin_dmevent_autostart\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Abort\" action=\"bypass -h admin_dmevent_abort\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Finish\" action=\"bypass -h admin_dmevent_finish\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Sit Force\" action=\"bypass -h admin_dmevent_sit\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Dump\" action=\"bypass -h admin_dmevent_dump\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><br><table><tr>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Save\" action=\"bypass -h admin_dmevent_save\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG
	.append("<td width=\"100\"><button value=\"Load\" action=\"bypass -h admin_dmevent_load\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	replyMSG.append("</tr></table><br><br>");
	replyMSG.append("Current event...<br1>");
	replyMSG.append("    ... name:&nbsp;<font color=\"00FF00\">" + DM._eventName + "</font><br1>");
	replyMSG.append("    ... description:&nbsp;<font color=\"00FF00\">" + DM._eventDesc + "</font><br1>");
	replyMSG.append("    ... joining location name:&nbsp;<font color=\"00FF00\">" + DM._joiningLocationName + "</font><br1>");
	replyMSG.append("    ... joining NPC ID:&nbsp;<font color=\"00FF00\">" + DM._npcId + " on pos " + DM._npcX + "," + DM._npcY + "," + DM._npcZ
			+ "</font><br1>");
	replyMSG.append("    ... reward ID:&nbsp;<font color=\"00FF00\">" + DM._rewardId + "</font><br1>");
	replyMSG.append("    ... reward Amount:&nbsp;<font color=\"00FF00\">" + DM._rewardAmount + "</font><br><br>");
	replyMSG.append("    ... Min lvl:&nbsp;<font color=\"00FF00\">" + DM._minlvl + "</font><br>");
	replyMSG.append("    ... Max lvl:&nbsp;<font color=\"00FF00\">" + DM._maxlvl + "</font><br><br>");
	replyMSG.append("    ... joining time:&nbsp;<font color=\"00FF00\">" + DM._joinTime/60/1000 + "</font><br>");
	replyMSG.append("    ... event time:&nbsp;<font color=\"00FF00\">" + DM._eventTime/60/1000 + "</font><br>");
	replyMSG.append("    ... Death Match Color:&nbsp;<font color=\"00FF00\">" + DM._playerColors + "</font><br>");
	replyMSG
	.append("    ... Death Match Spawn Pos:&nbsp;<font color=\"00FF00\">" + DM._playerX + "," + DM._playerY + "," + DM._playerZ + "</font><br><br>");
	replyMSG.append("Current players:<br1>");
	
	if (!DM._started)
	{
		replyMSG.append("<br1>");
		replyMSG.append(DM._players.size() + " players participating.");
		replyMSG.append("<br><br>");
	}
	else if (DM._started)
	{
		replyMSG.append("<br1>");
		replyMSG.append(DM._players.size() + " players in fighting event.");
		replyMSG.append("<br><br>");
	}
	
	replyMSG.append("</body></html>");
	adminReply.setHtml(replyMSG.toString());
	activeChar.sendPacket(adminReply);
}
}
