package net.sf.l2j.gameserver.handler.admincommandhandlers;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.TvTInstanced.TVTInstance;
import net.sf.l2j.gameserver.model.events.TvTInstanced.TvTIMain;
import net.sf.l2j.gameserver.model.events.TvTInstanced.TvTITeam;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminTvTiEngine implements IAdminCommandHandler
{
	private static final String[]	ADMIN_COMMANDS	= {
			/** Main Page Commands */
			"admin_tvti",
			"admin_tvti_title",
			"admin_tvti_desc",
			"admin_tvti_join_name",
			"admin_tvti_announce_name",
			"admin_tvti_npc_pos",
			"admin_tvti_npc",
			"admin_tvti_radius",
			"admin_tvti_control_all",
			"admin_tvti_create_instance",
			"admin_tvti_remove_instance",
			"admin_tvti_dupe_instance",
			"admin_tvti_instance_page",
			/** Instance Page Commands */
			"admin_tvti_control_instance",
			"admin_tvti_instance_name",
			"admin_tvti_instance_reward",
			"admin_tvti_instance_amount",
			"admin_tvti_instance_minplayers",
			"admin_tvti_instance_maxplayers",
			"admin_tvti_instance_minlvl",
			"admin_tvti_instance_maxlvl",
			"admin_tvti_instance_jointime",
			"admin_tvti_instance_eventtime",
			"admin_tvti_team_page",
			"admin_tvti_create_team",
			"admin_tvti_remove_team",
			/** Team Page Commands */
			"admin_tvti_team_name",
			"admin_tvti_team_color",
			"admin_tvti_team_spawn",
			"admin_tvti_team_radius",
			/** Control All Page Commands */
			"admin_tvti_control_all_join",
			"admin_tvti_control_all_tele",
			"admin_tvti_control_all_sit",
			"admin_tvti_control_all_start",
			"admin_tvti_control_all_fin",
			"admin_tvti_control_all_abort",
			"admin_tvti_control_all_auto",
			/** Control Instance Page Commands */
			"admin_tvti_control_instance_tele",
			"admin_tvti_control_instance_sit",
			"admin_tvti_control_instance_start",
			"admin_tvti_control_instance_fin",
			"admin_tvti_control_instance_abort",
			/** Misc Commands */
			"admin_tvtikick"						};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (!activeChar.isGM())
			return false;
			
		/** Main Page Commands */
		if (command.equals("admin_tvti"))
			showMainPage(activeChar);
		else if (command.startsWith("admin_tvti_title "))
		{
			TvTIMain.setEventTitle(command.substring(17));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_desc "))
		{
			TvTIMain.setEventDesc(command.substring(16));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_join_name "))
		{
			TvTIMain.setJoinLocName(command.substring(21));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_announce_name "))
		{
			TvTIMain.setAnnounceName(command.substring(25));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_npc "))
		{
			TvTIMain.setNpcId(Integer.valueOf(command.substring(15)));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_radius "))
		{
			TvTIMain.setSpawnRadius(Integer.valueOf(command.substring(18)));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_tvti_npc_pos"))
		{
			TvTIMain.setSpawn(activeChar);
			showMainPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all"))
		{
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_create_instance"))
		{
			showInstancePage(activeChar, TvTIMain.createInstance());
		}
		else if (command.startsWith("admin_tvti_remove_instance "))
		{
			TvTIMain.removeInstance(Integer.valueOf(command.substring(27)));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_tvti_instance_page "))
		{
			showInstancePage(activeChar, Integer.valueOf(command.substring(25)));
		}
		else if (command.startsWith("admin_tvti_dupe_instance "))
		{
			TvTIMain.duplicateInstance(Integer.valueOf(command.substring(25)));
			showMainPage(activeChar);
		}
		/** Instance Page Commands */
		else if (command.startsWith("admin_tvti_control_instance "))
		{
			showControlInstancePage(activeChar, Integer.valueOf(command.substring(28)));
		}
		else if (command.startsWith("admin_tvti_instance_name "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setInstanceName(command.substring(params[0].length() + params[1].length() + 2));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_reward "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setRewardId(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_amount "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setRewardAmount(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_minplayers "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setMinPlayers(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_maxplayers "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setMaxPlayers(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_minlvl "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setMinLvl(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_maxlvl "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setMaxLvl(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_jointime "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setJoinTime(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_instance_eventtime "))
		{
			String[] params;
			params = command.split(" ");
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).setEventTime(Integer.valueOf(command.substring(params[0].length() + params[1].length() + 2)));
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		else if (command.startsWith("admin_tvti_team_page "))
		{
			String[] params;
			params = command.split(" ");
			// admin_tvti_team_page 300000 Red
			String teamName = command.substring(params[0].length() + params[1].length() + 2);
			int teamIdx = 0;

			for (TvTITeam t : TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams())
			{
				if (t.getTeamName().equals(teamName))
				{
					teamIdx = TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams().indexOf(t);
					break;
				}
			}

			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		else if (command.startsWith("admin_tvti_create_team "))
		{
			String[] params;
			params = command.split(" ");
			String teamName = command.substring(params[0].length() + params[1].length() + 2);
			int teamIdx = 0;

			for (TvTITeam t : TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams())
				if (t.getTeamName().equals(teamName))
				{
					activeChar.sendMessage("A team with that name already exists...");
					return false;
				}
			teamIdx = TvTIMain.getTvTInstance(Integer.valueOf(params[1])).createTeam(teamName);
			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		else if (command.startsWith("admin_tvti_remove_team "))
		{
			String[] params;
			params = command.split(" ");
			String teamName = command.substring(params[0].length() + params[1].length() + 2);
			int teamIdx = 0;

			for (TvTITeam t : TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams())
				if (t.getTeamName().equals(teamName))
					teamIdx = TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams().indexOf(t);
			TvTIMain.getTvTInstance(Integer.valueOf(params[1])).getTeams().remove(teamIdx);
			showInstancePage(activeChar, Integer.valueOf(params[1]));
		}
		/** Team Page Commands */
		else if (command.startsWith("admin_tvti_team_name "))
		{
			String[] params;
			params = command.split(" ");
			String teamName = command.substring(params[0].length() + params[1].length() + params[2].length() + 3);
			int teamIdx = Integer.valueOf(params[2]);
			getTeam(Integer.valueOf(params[1]), teamIdx).setTeamName(teamName);
			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		else if (command.startsWith("admin_tvti_team_color "))
		{
			String[] params;
			params = command.split(" ");
			String teamColor = command.substring(params[0].length() + params[1].length() + params[2].length() + 3);
			int teamIdx = Integer.valueOf(params[2]);
			getTeam(Integer.valueOf(params[1]), teamIdx).setTeamColor(Integer.decode("0x" + (teamColor)));
			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		else if (command.startsWith("admin_tvti_team_spawn "))
		{
			String[] params;
			params = command.split(" ");
			int teamIdx = Integer.valueOf(params[2]);
			getTeam(Integer.valueOf(params[1]), teamIdx).setSpawn(activeChar);
			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		else if (command.startsWith("admin_tvti_team_radius "))
		{
			String[] params;
			params = command.split(" ");
			int radius = Integer.valueOf(command.substring(params[0].length() + params[1].length() + params[2].length() + 3));
			int teamIdx = Integer.valueOf(params[2]);
			getTeam(Integer.valueOf(params[1]), teamIdx).setSpawnRadius(radius);
			showTeamPage(activeChar, Integer.valueOf(params[1]), teamIdx);
		}
		/** Control All Page Commands */
		else if (command.equals("admin_tvti_control_all_join"))
		{
			TvTIMain.startJoin(activeChar);
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_tele"))
		{
			for (TVTInstance i : TvTIMain.getInstances())
				i.teleportStart();
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_sit"))
		{
			for (TVTInstance i : TvTIMain.getInstances())
				for (TvTITeam t : i.getTeams())
					t.sit();
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_start"))
		{
			for (TVTInstance i : TvTIMain.getInstances())
				i.startEvent();
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_fin"))
		{
			for (TVTInstance i : TvTIMain.getInstances())
				i.finishEvent();
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_abort"))
		{
			for (TVTInstance i : TvTIMain.getInstances())
				i.abortEvent();
			showControlAllPage(activeChar);
		}
		else if (command.equals("admin_tvti_control_all_auto"))
		{
			TvTIMain.startAutoJoin();
			showControlAllPage(activeChar);
		}
		/** Control Instance Page Commands */
		else if (command.startsWith("admin_tvti_control_instance_tele "))
		{
			TvTIMain.getTvTInstance(Integer.parseInt(command.substring(33))).teleportStart();
			showControlInstancePage(activeChar, Integer.parseInt(command.substring(33)));
		}
		else if (command.startsWith("admin_tvti_control_instance_sit "))
		{
			TVTInstance i = TvTIMain.getTvTInstance(Integer.parseInt(command.substring(32)));
			for (TvTITeam t : i.getTeams())
				t.sit();
			showControlInstancePage(activeChar, Integer.parseInt(command.substring(32)));
		}
		else if (command.startsWith("admin_tvti_control_instance_start "))
		{
			TvTIMain.getTvTInstance(Integer.parseInt(command.substring(34))).startEvent();
			showControlInstancePage(activeChar, Integer.parseInt(command.substring(34)));
		}
		else if (command.startsWith("admin_tvti_control_instance_fin "))
		{
			TvTIMain.getTvTInstance(Integer.parseInt(command.substring(32))).finishEvent();
			showControlInstancePage(activeChar, Integer.parseInt(command.substring(32)));
		}
		else if (command.startsWith("admin_tvti_control_instance_abort "))
		{
			TvTIMain.getTvTInstance(Integer.parseInt(command.substring(34))).abortEvent();
			showControlInstancePage(activeChar, Integer.parseInt(command.substring(34)));
		}
		/** Misc Commands */
		else if (command.startsWith("admin_tvtikick "))
		{
			L2PcInstance playerToKick = L2World.getInstance().getPlayer(command.substring(15));
			if (playerToKick != null)
			{
				TvTIMain.kickPlayerFromEvent(playerToKick, 2);
				activeChar.sendMessage("You kicked " + playerToKick.getName() + " from the TvTI event.");
			}
			else
				activeChar.sendMessage("Wrong usege: //tvtikick <player>");
		}
		else if (command.equals("admin_tvtikick"))
		{
			L2PcInstance playerToKick = (L2PcInstance) activeChar.getTarget();
			if (playerToKick != null)
			{
				TvTIMain.kickPlayerFromEvent(playerToKick, 2);
				activeChar.sendMessage("You kicked " + playerToKick.getName() + " from the TvTI event.");
			}
			else
				activeChar.sendMessage("Target invalid!");
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	public TvTITeam getTeam(int instanceId, int teamIdx)
	{
		return TvTIMain.getTvTInstance(instanceId).getTeams().get(teamIdx);
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

		replyMSG.append("<title>[TvT Instanced Engine] (last event time: "+howLongAgoInMinutes+" minutes ago)"+"</title>");
		replyMSG.append("<center><font color=\"LEVEL\">[Main Page]</font></center><br><br>");
		replyMSG.append("<table width=\"260\"><tr>");
		replyMSG.append("<td width=\"200\"><edit var=\"input\" width=\"200\"></td>");
		replyMSG.append("<td width=\"60\"><button value=\"Control All\" action=\"bypass -h admin_tvti_control_all\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_title $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Title: " + TvTIMain.getEventTitle() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_desc $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Discription: " + TvTIMain.getEventDesc() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_join_name $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Join Loc Name: " + TvTIMain.getJoinLocName() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_npc_pos\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Join Npc Loc: " + TvTIMain.getNpcX() + "," + TvTIMain.getNpcY() + "," + TvTIMain.getNpcZ() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_npc $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Join Npc: " + TvTIMain.getNpcId() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_radius $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Random Spawn Radius: " + TvTIMain.getSpawnRadius() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_announce_name $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Announce Name: " + TvTIMain.getAnnounceName() + "</td>");
		replyMSG.append("</tr></table>");

		if (!TvTIMain.getInstances().isEmpty())
			for (TVTInstance i : TvTIMain.getInstances())
			{
				replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
				replyMSG.append("<td align=\"right\"><button value=\"" + i.getInstanceName() + "\" action=\"bypass -h admin_tvti_instance_page " + i.getInstanceId()
						+ "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				replyMSG.append("<td align=\"left\"><button value=\"Remove\" action=\"bypass -h admin_tvti_remove_instance " + i.getInstanceId() + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				replyMSG.append("<td align=\"left\"><button value=\"Dupe\" action=\"bypass -h admin_tvti_dupe_instance " + i.getInstanceId() + "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				if (!i.isSetUp())
					replyMSG.append("<td align=\"left\"><font color=\"FF0000\">Not Setup!</font></td>");

				replyMSG.append("</tr></table>");
			}
		else
		{
			replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
			replyMSG.append("<tr><td>There are no instance, please create at least one.</td></tr>");
			replyMSG.append("</tr></table>");
		}

		replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td align=\"center\"><button value=\"Create new\" action=\"bypass -h admin_tvti_create_instance\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void showInstancePage(L2PcInstance activeChar, int instanceId)
	{
		TVTInstance i = TvTIMain.getTvTInstance(instanceId);

		if (i == null)
		{
			activeChar.sendMessage("TvT instance does not exist.");
			return;
		}

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
        
		replyMSG.append("<title>[TvT Instanced Engine] (last event time: "+howLongAgoInMinutes+" minutes ago)"+"</title>");
		replyMSG.append("<table width=\"300\"><tr><td align=\"right\"><button value=\"Back\" action=\"bypass -h admin_tvti\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		replyMSG.append("<center><font color=\"LEVEL\">[" + i.getInstanceName() + " Instance View]</font></center><br><br>");
		replyMSG.append("<table width=\"260\"><tr>");
		replyMSG.append("<td width=\"200\"><edit var=\"input\" width=\"200\"></td>");
		replyMSG.append("<td width=\"60\"><button value=\"Control This\" action=\"bypass -h admin_tvti_control_instance " + i.getInstanceId() + "\" width=80 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_name " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Name: " + i.getInstanceName() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("<td width=\"170\">Instance Id: " + i.getInstanceId() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_reward " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		if (i.getRewardId() == 0)
			replyMSG.append("<td width=\"170\">Reward: None</td>");
		else
			replyMSG.append("<td width=\"170\">Reward: (" + i.getRewardId() + ")" + ItemTable.getInstance().getTemplate(i.getRewardId()).getName() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_amount " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Reward Amount: " + i.getRewardAmount() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_minplayers " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Min Players: " + i.getMinPlayers() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_maxplayers " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Max Players: " + i.getMaxPlayers() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_minlvl " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Min Level: " + i.getMinLvl() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_maxlvl " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Max Level: " + i.getMaxLvl() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_jointime " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Join Time: " + i.getJoinTime() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_instance_eventtime " + instanceId + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Event Time: " + i.getEventTime() + "</td>");
		replyMSG.append("</tr></table>");

		if (!i.getTeams().isEmpty())
			for (TvTITeam t : i.getTeams())
			{
				replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
				replyMSG.append("<td align=\"right\"><button value=\"" + t.getTeamName() + "\" action=\"bypass -h admin_tvti_team_page " + instanceId + " " + t.getTeamName()
						+ "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				replyMSG.append("<td align=\"left\"><button value=\"Remove\" action=\"bypass -h admin_tvti_remove_team " + instanceId + " " + t.getTeamName()
						+ "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				if (!t.isSetUp())
					replyMSG.append("<td align=\"left\"><font color=\"FF0000\">Not Setup!</font></td>");

				replyMSG.append("</tr></table>");
			}
		else if (i.getTeams().size() == 1)
		{
			replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
			replyMSG.append("<tr><td>There are no teams, please create at least one more.</td></tr>");
			replyMSG.append("</tr></table>");
		}
		else
		{
			replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
			replyMSG.append("<tr><td>There are no teams, please create at least two.</td></tr>");
			replyMSG.append("</tr></table>");
		}

		replyMSG.append("<br><table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td align=\"center\">Team Name: <edit var=\"teamName\" width=\"200\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td align=\"center\"><button value=\"Create new\" action=\"bypass -h admin_tvti_create_team " + instanceId + " $teamName\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void showTeamPage(L2PcInstance activeChar, int instanceId, int teamIdx)
	{
		TvTITeam t = getTeam(instanceId, teamIdx);
		// String color;
		if (t == null)
		{
			activeChar.sendMessage("TvT team does not exist.");
			return;
		}

		String c = Integer.toHexString(t.getTeamColor());
		while (c.length() < 6)
			c = "0" + c;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<title>[TvT Instanced Engine]</title>");
		replyMSG.append("<table width=\"300\"><tr><td align=\"right\"><button value=\"Back\" action=\"bypass -h admin_tvti_instance_page " + TvTIMain.getTvTInstance(instanceId).getInstanceId()
				+ "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		replyMSG.append("<center><font color=\"LEVEL\">[" + t.getTeamName() + " Team View]</font></center><br><br>");
		replyMSG.append("<edit var=\"input\" width=\"200\">");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_team_name " + instanceId + " " + teamIdx + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Name: " + t.getTeamName() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_team_spawn " + instanceId + " " + teamIdx + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Team Spawn Loc: " + t.getSpawnX() + "," + t.getSpawnY() + "," + t.getSpawnZ() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_team_radius " + instanceId + " " + teamIdx + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Random Spawn Radius: " + t.getSpawnRadius() + "</td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"><button value=\"Set\" action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " $input\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		replyMSG.append("<td width=\"170\">Color: <font color=\"" + c + "\">" + c.toUpperCase() + "</font></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td align=\"center\">Pick a color or type in a R hex color</td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<table width=\"300\" border=\"0\"><tr>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"FF0000\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " FF0000\">Red</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"0000FF\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " 0000FF\">Blue</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"00FF00\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " 00FF00\">Green</a></font></td>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"FF00FF\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " FF00FF\">Pink</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"6600CC\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " 6600CC\">Purple</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"FF6600\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " FF6600\">Orange</a></font></td>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"FFFF00\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " FFFF00\">Yellow</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"99FFFF\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " 99FFFF\">Light Blue</a></font></td>");
		replyMSG.append("<td align=\"center\" width=\"80\"><font color=\"999999\"><a action=\"bypass -h admin_tvti_team_color " + instanceId + " " + teamIdx + " 999999\">Grey</a></font></td>");
		replyMSG.append("<td width=\"30\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("</tr></table>");

		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void showControlAllPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<title>[TvT Instanced Engine]</title>");
		replyMSG.append("<table width=\"300\"><tr><td align=\"right\"><button value=\"Back\" action=\"bypass -h admin_tvti\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		replyMSG.append("<button value=\"Join\" action=\"bypass -h admin_tvti_control_all_join\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_tvti_control_all_tele\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Sit\" action=\"bypass -h admin_tvti_control_all_sit\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Start\" action=\"bypass -h admin_tvti_control_all_start\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Finish\" action=\"bypass -h admin_tvti_control_all_fin\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Abort\" action=\"bypass -h admin_tvti_control_all_abort\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Auto\" action=\"bypass -h admin_tvti_control_all_auto\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");

		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void showControlInstancePage(L2PcInstance activeChar, int instanceId)
	{
		TVTInstance i = TvTIMain.getTvTInstance(instanceId);

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<title>[TvT Instanced Engine]</title>");
		replyMSG.append("<table width=\"300\"><tr><td align=\"right\"><button value=\"Back\" action=\"bypass -h admin_tvti_instance_page " + instanceId
				+ "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		replyMSG.append("<table width=\"300\"><tr>");
		replyMSG.append("<td>Name : " + i.getInstanceName() + "</td>");
		replyMSG.append("</tr><tr>");
		// Status of TvT instance
		if (i.isJoining())
			replyMSG.append("<td>Status : <font color=\"LEVEL\">Joining</font></td>");
		else if (i.isTeleport())
			replyMSG.append("<td>Status : <font color=\"LEVEL\">Teleporting</font></td>");
		else if (i.isStarted())
			replyMSG.append("<td>Status : <font color=\"00FF00\">Started</font></td>");
		else if (!i.isSetUp())
			replyMSG.append("<td>Status : <font color=\"FF0000\">Not Setup!</font></td>");
		else
			replyMSG.append("<td>Status : Set up</td>");
		replyMSG.append("</tr><tr>");

		replyMSG.append("<td>Levels : " + i.getMinLvl() + " - " + i.getMaxLvl() + "</td>");
		replyMSG.append("</tr><tr>");
		if (i.isJoining())
			replyMSG.append("<td>Players : " + i.getPlayers().size() + " / " + i.getMaxPlayers() + "</td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<center> --------------------------------- <br1>");
		replyMSG.append("<button value=\"Teleport\" action=\"bypass -h admin_tvti_control_instance_tele " + instanceId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Sit\" action=\"bypass -h admin_tvti_control_instance_sit " + instanceId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Start\" action=\"bypass -h admin_tvti_control_instance_start " + instanceId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Finish\" action=\"bypass -h admin_tvti_control_instance_fin " + instanceId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<button value=\"Abort\" action=\"bypass -h admin_tvti_control_instance_abort " + instanceId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}