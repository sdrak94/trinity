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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class handles following admin commands:
 * - admin = shows menu
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminEventEngine implements IAdminCommandHandler
{
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_event",
		"admin_event_new",
		"admin_event_choose",
		"admin_event_store",
		"admin_event_set",
		"admin_event_change_teams_number",
		"admin_event_announce",
		"admin_event_panel",
		"admin_event_control_begin",
		"admin_event_control_teleport",
		"admin_add", "admin_event_see",
		"admin_event_del",
		"admin_delete_buffer",
		"admin_event_control_sit",
		"admin_event_name",
		"admin_event_control_kill",
		"admin_event_control_res",
		"admin_event_control_poly",
		"admin_event_control_unpoly",
		"admin_event_control_prize",
		"admin_event_control_chatban",
		"admin_event_control_finish"
	};
	
	private static String tempBuffer = "";
	private static String tempName = "";
	private static boolean npcsDeleted = false;
	
	@SuppressWarnings("resource")
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_event"))
			showMainPage(activeChar);
		
		else if (command.equals("admin_event_new"))
		{
			showNewEventPage(activeChar);
		}
		else if (command.startsWith("admin_add"))
		{
			tempBuffer += command.substring(10);
			showNewEventPage(activeChar);
			
		}
		else if (command.startsWith("admin_event_see"))
		{
			String eventName = command.substring(16);
			try
			{
				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				
				DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("data/events/" + eventName)));
				BufferedReader inbr = new BufferedReader(new InputStreamReader(in));

                                final String replyMSG = StringUtil.concat(
                                        "<html><body>" +
                                        "<center><font color=\"LEVEL\">",
                                        eventName,
                                        "</font><font color=\"FF0000\"> bY ",
                                        inbr.readLine(),
                                        "</font></center><br>" +
                                        "<br>",
                                        inbr.readLine(),
                                        "</body></html>"
                                        );
				adminReply.setHtml(replyMSG);
				activeChar.sendPacket(adminReply);
			}
			catch (Exception e)
			{
				
				e.printStackTrace();
				
			}
			
		}
		else if (command.startsWith("admin_event_del"))
		{
			String eventName = command.substring(16);
			File file = new File("data/events/" + eventName);
			file.delete();
			showMainPage(activeChar);
			
		}
		
		else if (command.startsWith("admin_event_name"))
		{
			tempName += command.substring(17);
			showNewEventPage(activeChar);
			
		}
		
		else if (command.equalsIgnoreCase("admin_delete_buffer"))
		{
			try
			{
				tempBuffer += tempBuffer.substring(0, tempBuffer.length() - 10);
				showNewEventPage(activeChar);
			}
			catch (Exception e)
			{
				tempBuffer = "";
			}
		}
		
		else if (command.startsWith("admin_event_store"))
		{
			
			try
			{
				FileOutputStream file = new FileOutputStream("data/events/" + tempName);
				PrintStream p = new PrintStream(file);
				p.println(activeChar.getName());
				p.println(tempBuffer);
				file.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			tempBuffer = "";
			tempName = "";
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_event_set"))
		{
			L2Event.eventName = command.substring(16);
			showEventParameters(activeChar, 2);
			
		}
		else if (command.startsWith("admin_event_change_teams_number"))
		{
			showEventParameters(activeChar, Integer.parseInt(command.substring(32)));
		}
		else if (command.startsWith("admin_event_panel"))
		{
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_begin"))
		{
			
			try
			{
				
				L2Event.active = true;
				L2Event.players.clear();
				L2Event.connectionLossData.clear();
				
				for (int j = 0; j < L2Event.teamsNumber; j++)
				{
					LinkedList<String> link = new LinkedList<String>();
					L2Event.players.put(j + 1, link);
					
				}
				int i = 0;
				
				while (!L2Event.participatingPlayers.isEmpty())
				{
					String target = getMaxLeveledPlayer();
					
					if (!target.isEmpty())
					{
						
						L2Event.players.get(i + 1).add(target);
						i = (i + 1) % L2Event.teamsNumber;
					}
					else
					{
						break;
					}
				}
				
				destroyEventNpcs();
				npcsDeleted = true;
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_teleport"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(29), "-");
			
			while (st.hasMoreElements())
			{
				teleportTeam(activeChar, Integer.parseInt(st.nextToken()));
			}
			showEventControl(activeChar);
		}
		
		else if (command.startsWith("admin_event_control_sit"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(24), "-");
			
			while (st.hasMoreElements())
			{
				sitTeam(Integer.parseInt(st.nextToken()));
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_kill"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(25), "-");
			
			while (st.hasMoreElements())
			{
				killTeam(activeChar, Integer.parseInt(st.nextToken()));
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_res"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(24), "-");
			
			while (st.hasMoreElements())
			{
				resTeam(Integer.parseInt(st.nextToken()));
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_poly"))
		{
			StringTokenizer st0 = new StringTokenizer(command.substring(25));
			StringTokenizer st = new StringTokenizer(st0.nextToken(), "-");
			String id = st0.nextToken();
			while (st.hasMoreElements())
			{
				polyTeam(Integer.parseInt(st.nextToken()), id);
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_unpoly"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(27), "-");
			
			while (st.hasMoreElements())
			{
				unpolyTeam(Integer.parseInt(st.nextToken()));
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_prize"))
		{
			StringTokenizer st0 = new StringTokenizer(command.substring(26));
			StringTokenizer st = new StringTokenizer(st0.nextToken(), "-");
			String n = st0.nextToken();
			StringTokenizer st1 = new StringTokenizer(n, "*");
			n = st1.nextToken();
			String type = "";
			if (st1.hasMoreElements())
				type = st1.nextToken();
			
			String id = st0.nextToken();
			while (st.hasMoreElements())
			{
				regardTeam(activeChar, Integer.parseInt(st.nextToken()), Integer.parseInt(n), Integer.parseInt(id), type);
			}
			showEventControl(activeChar);
		}
		else if (command.startsWith("admin_event_control_finish"))
		{
			for (int i = 0; i < L2Event.teamsNumber; i++)
			{
				telePlayersBack(i + 1);
			}
			
			L2Event.eventName = "";
			L2Event.teamsNumber = 0;
			L2Event.names.clear();
			L2Event.participatingPlayers.clear();
			L2Event.players.clear();
			L2Event.id = 12760;
			L2Event.npcs.clear();
			L2Event.active = false;
			npcsDeleted = false;
			
		}
		
		else if (command.startsWith("admin_event_announce"))
		{
			StringTokenizer st = new StringTokenizer(command.substring(21));
			L2Event.id = Integer.parseInt(st.nextToken());
			L2Event.teamsNumber = Integer.parseInt(st.nextToken());
			String temp = " ";
			String temp2 = "";
			while (st.hasMoreElements())
			{
				temp += st.nextToken() + " ";
			}
			
			st = new StringTokenizer(temp, "-");
			
			Integer i = 1;
			
			while (st.hasMoreElements())
			{
				temp2 = st.nextToken();
				if (!temp2.equals(" "))
				{
					L2Event.names.put(i++, temp2.substring(1, temp2.length() - 1));
				}
			}
			
			L2Event.participatingPlayers.clear();
			
			muestraNpcConInfoAPlayers(activeChar, L2Event.id);
			
			PlaySound _snd = new PlaySound(1, "B03_F", 0, 0, 0, 0, 0);
			activeChar.sendPacket(_snd);
			activeChar.broadcastPacket(_snd);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

                        final String replyMSG = StringUtil.concat(
                                "<html><body>" +
                                "<center><font color=\"LEVEL\">[ L2J EVENT ENGINE</font></center><br>" +
                                "<center>The event <font color=\"LEVEL\">",
                                L2Event.eventName,
                                "</font> has been announced, now you can type //event_panel to see the event panel control</center><br>" +
                                "</body></html>"
                                );
			adminReply.setHtml(replyMSG);
			activeChar.sendPacket(adminReply);
		}
		
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	String showStoredEvents()
	{
		File dir = new File("data/events");
		String[] files = dir.list();

		if (files == null) {
			return "No 'data/events' directory!";
		}

                final StringBuilder result = new StringBuilder(files.length * 500);
                
		for (int i = 0; i < files.length; i++) {
			final File file = new File("data/events/" + files[i]);
                        final String fileName = file.getName();
                        StringUtil.append(result,
                                "<font color=\"LEVEL\">",
                                fileName,
                                " </font><br><button value=\"select\" action=\"bypass -h admin_event_set ",
                                fileName,
                                "\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"ver\" action=\"bypass -h admin_event_see ",
                                fileName,
                                "\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><button value=\"delete\" action=\"bypass -h admin_event_del ",
                                fileName,
                                "\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>"
                                );
		}

		return result.toString();
	}
	
	public void showMainPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

                final String replyMSG = StringUtil.concat(
                        "<html><body>" +
                        "<center><font color=\"LEVEL\">[ L2J EVENT ENGINE ]</font></center><br>" +
                        "<br><center><button value=\"Create NEW event \" action=\"bypass -h admin_event_new\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
                        "<center><br>Stored Events<br></center>",
                        showStoredEvents(),
                        "</body></html>"
                        );
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}
	
	public void showNewEventPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

                final StringBuilder replyMSG = StringUtil.startAppend(500,
                        "<html><body>" +
                        "<center><font color=\"LEVEL\">[ L2J EVENT ENGINE ]</font></center><br>" +
                        "<br><center>Event's Title <br><font color=\"LEVEL\">"
                        );

                if (tempName.isEmpty())
			replyMSG.append("Use //event_name text to insert a new title");
		else
			replyMSG.append(tempName);
		replyMSG.append("</font></center><br><br>Event's description<br>");
		if (tempBuffer.isEmpty())
			replyMSG.append("Use //add text o //delete_buffer to modify this text field");
		else
			replyMSG.append(tempBuffer);
		
		if (!(tempName.isEmpty() && tempBuffer.isEmpty()))
			replyMSG.append("<br><button value=\"Crear\" action=\"bypass -h admin_event_store\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		
		replyMSG.append("</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void showEventParameters(L2PcInstance activeChar, int teamnumbers)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

                final StringBuilder replyMSG = StringUtil.startAppend(
                        1000 + teamnumbers * 150,
                        "<html><body>" +
                        "<center><font color=\"LEVEL\">[ L2J EVENT ENGINE ]</font></center><br>" +
                        "<center><font color=\"LEVEL\">",
                        L2Event.eventName,
                        "</font></center><br>" +
                        "<br><center><button value=\"Change number of teams to\" action=\"bypass -h admin_event_change_teams_number $event_teams_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"> <edit var=\"event_teams_number\" width=100 height=20><br><br>" +
                        "<font color=\"LEVEL\">Team's Names</font><br>"
                        );

                for (int i = 0; i < teamnumbers; i++) {
                    StringUtil.append(replyMSG,
                            String.valueOf(i + 1),
                            ".- <edit var=\"event_teams_name",
                            String.valueOf(i + 1),
                            "\" width=100 height=20><br>");
		}

                StringUtil.append(replyMSG,
                        "<br><br>Announcer NPC id<edit var=\"event_npcid\" width=100 height=20><br><br><button value=\"Announce Event!!\" action=\"bypass -h admin_event_announce $event_npcid ",
                        String.valueOf(teamnumbers),
                        " ");

		for (int i = 0; i < teamnumbers; i++) {
                    StringUtil.append(replyMSG,
                            "$event_teams_name",
                            String.valueOf(i + 1),
                            " - ");
		}
		replyMSG.append("\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
                        "</body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	void muestraNpcConInfoAPlayers(L2PcInstance activeChar, int id)
	{
		L2Event.npcs.clear();
		LinkedList<L2PcInstance> temp = new LinkedList<L2PcInstance>();
		temp.clear();
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		//synchronized (L2World.getInstance().getAllPlayers())
		{
			for (L2PcInstance player : pls)
			{
				if (!temp.contains(player))
				{
					L2Event.spawn(player, id);
					temp.add(player);
				}
				Collection<L2PcInstance> plrs = player.getKnownList().getKnownPlayers().values();
				//synchronized (player.getKnownList().getKnownPlayers())
				{
					for (L2PcInstance playertemp : plrs)
					{
						if ((Math.abs(playertemp.getX() - player.getX()) < 500) && (Math.abs(playertemp.getY() - player.getY()) < 500) && (Math.abs(playertemp.getZ() - player.getZ()) < 500))
							temp.add(playertemp);
					}
				}
			}
			
		}
		L2Event.announceAllPlayers(activeChar.getName() + " wants to make an event !!! (you'll find a npc with the details around)");
	}
	
	void showEventControl(L2PcInstance activeChar)
	{
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

                final StringBuilder replyMSG = StringUtil.startAppend(1000,
                        "<html><body>" +
                        "<center><font color=\"LEVEL\">[ L2J EVENT ENGINE ]</font></center><br><font color=\"LEVEL\">",
                        L2Event.eventName,
                        "</font><br><br><table width=200>" +
                        "<tr><td>Apply this command to teams number </td><td><edit var=\"team_number\" width=100 height=15></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>"
                        );
		if (!npcsDeleted) {
			replyMSG.append("<tr><td><button value=\"Start\" action=\"bypass -h admin_event_control_begin\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Destroys all event npcs so no more people can't participate now on</font></td></tr>");
                }

		replyMSG.append(
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Teleport\" action=\"bypass -h admin_event_control_teleport $team_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Teleports the specified team to your position</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Sit\" action=\"bypass -h admin_event_control_sit $team_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Sits/Stands up the team</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Kill\" action=\"bypass -h admin_event_control_kill $team_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Finish with the life of all the players in the selected team</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Resurrect\" action=\"bypass -h admin_event_control_res $team_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Resurrect Team's members</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Polymorph\" action=\"bypass -h admin_event_control_poly $team_number $poly_id\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><edit var=\"poly_id\" width=100 height=15><font color=\"LEVEL\">Polymorphs the team into the NPC with the id specified</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"UnPolymorph\" action=\"bypass -h admin_event_control_unpoly $team_number\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Unpolymorph the team</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"Give Item\" action=\"bypass -h admin_event_control_prize $team_number $n $id\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"> number <edit var=\"n\" width=100 height=15> item id <edit var=\"id\" width=100 height=15></td><td><font color=\"LEVEL\">Give the specified item id to every single member of the team, you can put 5*level, 5*kills or 5 in the number field for example</font></td></tr>" +
                        "<tr><td>&nbsp;</td></tr>" +
                        "<tr><td><button value=\"End\" action=\"bypass -h admin_event_control_finish\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><font color=\"LEVEL\">Will finish the event teleporting back all the players</font></td></tr>" +
                        "</table></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	String getMaxLeveledPlayer()
	{
		L2PcInstance pc = null;
		int max = 0;
		String name = "";
		for (String tempName2: L2Event.participatingPlayers)
		{
			try
			{
				pc = L2World.getInstance().getPlayer(tempName2);
				if (max < pc.getLevel())
				{
					max = pc.getLevel();
					name = tempName2;
				}
			}
			catch (Exception e)
			{
				L2Event.participatingPlayers.remove(tempName2);
			}
		}
		L2Event.participatingPlayers.remove(name);
		return name;
	}
	
	void destroyEventNpcs()
	{
		L2Npc npc;
		while (!L2Event.npcs.isEmpty())
		{
			try
			{
				npc = (L2Npc) L2World.getInstance().findObject(Integer.parseInt(L2Event.npcs.getFirst()));
				
				L2Spawn spawn = npc.getSpawn();
				
				if (spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, true);
				}
				npc.deleteMe();
				L2Event.npcs.removeFirst();
			}
			catch (Exception e)
			{
				L2Event.npcs.removeFirst();
			}
		}
	}
	
	void teleportTeam(L2PcInstance activeChar, int team)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance pc = L2World.getInstance().getPlayer(it.next());
				pc.setTitle(L2Event.names.get(team));
				pc.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), true);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	void sitTeam(int team)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance pc = L2World.getInstance().getPlayer(it.next());
				pc.eventSitForced = !pc.eventSitForced;
				if (pc.eventSitForced)
					pc.sitDown();
				else
					pc.standUp();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	void killTeam(L2PcInstance activeChar, int team)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance target = L2World.getInstance().getPlayer(it.next());
				target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	void resTeam(int team)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			L2PcInstance character = L2World.getInstance().getPlayer(it.next());
			if (character == null || !character.isDead())
				continue;
			character.restoreExp(100.0);
			character.doRevive();
			character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
			character.setCurrentCp(character.getMaxCp());
		}
	}
	
	void polyTeam(int team, String id)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance target = L2World.getInstance().getPlayer(it.next());
				target.getPoly().setPolyInfo("npc", id);
				target.teleToLocation(target.getX(), target.getY(), target.getZ(), true);
				CharInfo info1 = new CharInfo(target);
				target.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(target);
				target.sendPacket(info2);
				target.broadcastPacket(new ExBrExtraUserInfo(target));
			}
			catch (Exception e)
			{
			}
		}
	}
	
	void unpolyTeam(int team)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance target = L2World.getInstance().getPlayer(it.next());
				
				target.getPoly().setPolyInfo(null, "1");
				target.decayMe();
				target.spawnMe(target.getX(), target.getY(), target.getZ());
				CharInfo info1 = new CharInfo(target);
				target.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(target);
				target.sendPacket(info2);
				target.broadcastPacket(new ExBrExtraUserInfo(target));
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void createItem(L2PcInstance activeChar, L2PcInstance player, int id, int num)
	{
		player.getInventory().addItem("Event", id, num, player, activeChar);
		ItemList il = new ItemList(player, true);
		player.sendPacket(il);
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(
                        "<html><body>" +
                        "CONGRATULATIONS, you should have a present in your inventory" +
                        "</body></html>");
		player.sendPacket(adminReply);
	}
	
	void regardTeam(L2PcInstance activeChar, int team, int n, int id, String type)
	{
		LinkedList<String> linked = L2Event.players.get(team);
		int temp = n;
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance target = L2World.getInstance().getPlayer(it.next());
				if (type.equalsIgnoreCase("level"))
					temp = n * target.getLevel();
				else if (type.equalsIgnoreCase("kills"))
					temp = n * target.kills.size();
				else
					temp = n;
				createItem(activeChar, target, id, temp);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	void telePlayersBack(int team)
	{
		resTeam(team);
		unpolyTeam(team);
		LinkedList<String> linked = L2Event.players.get(team);
		Iterator<String> it = linked.iterator();
		while (it.hasNext())
		{
			try
			{
				L2PcInstance target = L2World.getInstance().getPlayer(it.next());
				target.setTitle(target.eventTitle);
				target.setKarma(target.eventkarma);
				target.setPvpKills(target.eventpvpkills);
				target.setPkKills(target.eventpkkills);
				target.teleToLocation(target.eventX, target.eventY, target.eventZ, true);
				target.kills.clear();
				target.eventSitForced = false;
				target.atEvent = false;
			}
			catch (Exception e)
			{
			}
		}
	}
}
