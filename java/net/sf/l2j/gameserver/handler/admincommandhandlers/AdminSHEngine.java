package net.sf.l2j.gameserver.handler.admincommandhandlers;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.SH;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminSHEngine implements IAdminCommandHandler
{
	private static final String[]	ADMIN_COMMANDS	= {
			/** Main Page Commands */
			"admin_sh",
			"admin_sh_title",
			"admin_sh_desc",
			"admin_sh_loc",
			"admin_sh_announce_name",
			"admin_sh_npc_pos",
			"admin_sh_npc",
			"admin_sh_add_item",
			"admin_sh_rem_item",
			"admin_sh_edit_item",
			"admin_sh_add_prize",
			"admin_sh_rem_prize",
			"admin_sh_edit_prize",
			"admin_sh_start",
			"admin_sh_end"							};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!activeChar.isGM())
			return false;
		
		/** Main Page Commands */
		if (command.equals("admin_sh"))
			showMainPage(activeChar);
		else if (command.startsWith("admin_sh_title "))
		{
			SH.setEventTitle(command.substring(15));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_desc "))
		{
			SH.setEventDesc(command.substring(14));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_loc "))
		{
			SH.setLocation(command.substring(13));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_announce_name "))
		{
			SH.setAnnounceName(command.substring(23));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_npc "))
		{
			SH.setNpcId(Integer.valueOf(command.substring(13)));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_sh_npc_pos"))
		{
			SH.setSpawn(activeChar.getX(), activeChar.getY(), activeChar.getZ());
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_add_item "))
		{
			String[] params;
			params = command.split(" ");
			SH.addNewItem(Integer.valueOf(params[1]), Integer.valueOf(params[2]));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_rem_item "))
		{
			SH.remItem(Integer.valueOf(command.substring(18)));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_edit_item "))
		{
			String[] params;
			params = command.split(" ");
			SH.editItem(Integer.valueOf(params[1]), Integer.valueOf(params[2]), Integer.valueOf(params[3]));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_add_prize "))
		{
			String[] params;
			params = command.split(" ");
			SH.addNewPrize(Integer.valueOf(params[1]), Integer.valueOf(params[2]));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_rem_prize "))
		{
			SH.remPrize(Integer.valueOf(command.substring(19)));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_sh_edit_prize "))
		{
			String[] params;
			params = command.split(" ");
			SH.editPrize(Integer.valueOf(params[1]), Integer.valueOf(params[2]), Integer.valueOf(params[3]));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_sh_start"))
		{
			SH.startEvent(activeChar);
			showMainPage(activeChar);
		}
		else if (command.equals("admin_sh_end"))
		{
			SH.endEvent();
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

		replyMSG.append("<title>[Scavenger Hunt Engine]</title>");
		replyMSG.append("<center><font color=\"LEVEL\">[Main Page]</font></center><br><br>");
		replyMSG.append("<table width=\"260\"><tr>");
		replyMSG.append("<td width=\"200\"><edit var=\"input\" width=\"200\"></td>");
		if (!SH.isStarted())
			replyMSG.append("<td width=\"60\"><button value=\"Start\" action=\"bypass -h admin_sh_start\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		else
			replyMSG.append("<td width=\"60\"><button value=\"End\" action=\"bypass -h admin_sh_end\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_title $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Title: " + SH.getEventTitle() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_desc $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Discription: " + SH.getEventDesc() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_loc $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Event Loc Name: " + SH.getLocation() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_npc_pos\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Location: " + SH.getNpcX() + "," + SH.getNpcY() + "," + SH.getNpcZ() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_npc $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Join Npc: " + SH.getNpcId() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_sh_announce_name $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Announce Name: " + SH.getAnnounceName() + "</td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"85\"><edit var=\"input2\" width=\"80\"></td>");
		replyMSG.append("<td width=\"35\"><edit var=\"input3\" width=\"30\"></td>");
		replyMSG.append("<td width=\"40\">Add:</td>");
		replyMSG.append("<td width=\"70\"><button value=\"Item\" action=\"bypass -h admin_sh_add_item $input2 $input3\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"70\"><button value=\"Prize\" action=\"bypass -h admin_sh_add_prize $input2 $input3\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td>Items to hunt:</td>");
		replyMSG.append("</tr></table>");
		if (SH.getItems().size() == 0)
			replyMSG.append("<center>Please add atleast one item!</center>");
		replyMSG.append("<table width=\"300\" border=\"0\">");
		for (int i = 0; i < SH.getItems().size(); i++)
		{
			replyMSG.append("<tr>");
			replyMSG.append("<td width=\"170\">(" + SH.getItems().get(i).getCount() + ") " + SH.getItems().get(i).getItem().getName() + "</td>");
			replyMSG.append("<td width=\"50\"><button value=\"Edit\" action=\"bypass -h admin_sh_edit_item " + i + " $input2 $input3\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			replyMSG.append("<td width=\"70\"><button value=\"Remove\" action=\"bypass -h admin_sh_rem_item " + i + "\" width=70 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			replyMSG.append("</tr>");
		}
		replyMSG.append("</table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td>Prizes:</td>");
		replyMSG.append("</tr></table>");
		if (SH.getPrizes().size() == 0)
			replyMSG.append("<center>Please add atleast one prize!</center>");
		replyMSG.append("<table width=\"300\" border=\"0\">");
		for (int i = 0; i < SH.getPrizes().size(); i++)
		{
			replyMSG.append("<tr>");
			replyMSG.append("<td width=\"170\">[" + SH.suffixTool(i + 1) + "] (" + SH.getPrizes().get(i).getCount() + ") " + SH.getPrizes().get(i).getItem().getName() + "</td>");
			replyMSG.append("<td width=\"50\"><button value=\"Edit\" action=\"bypass -h admin_sh_edit_prize " + i + " $input2 $input3\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			replyMSG.append("<td width=\"70\"><button value=\"Remove\" action=\"bypass -h admin_sh_rem_prize " + i + "\" width=70 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			replyMSG.append("</tr>");
		}
		replyMSG.append("</table>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}