package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.Rnd;

public class AdminFOSEngine implements IAdminCommandHandler {

private static final String[] ADMIN_COMMANDS = {"admin_fos","admin_fos_pg2","admin_fos_pg3",
	"admin_fos_name", "admin_fos_desc", "admin_fos_join_loc","admin_fos_tele1","admin_fos_tele2",
	"admin_fos_minlvl", "admin_fos_maxlvl","admin_fos_door6","admin_fos_tele3","admin_fos_tele4",
	"admin_fos_npc", "admin_fos_npc_pos", "admin_fos_door","admin_fos_reward", "admin_fos_reward_amount",
	"admin_fos_teamname", "admin_fos_team_pos", "admin_fos_team_color","admin_fos_team_flag","admin_fos_team_cent",
	"admin_fos_join", "admin_fos_teleport", "admin_fos_start", "admin_fos_abort", "admin_fos_finish",
	"admin_fos_sit", "admin_fos_dump", "admin_fos_save", "admin_fos_load", "admin_fos_jointime",
	"admin_fos_eventtime", "admin_fos_autoevent","admin_fos_minplayers","admin_fos_maxplayers",
	"admin_fos_starttype","admin_fos_sealers","admin_fos_sorttype"};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	try{
		if (command.equals("admin_fos"))
			showMainPage(activeChar);
		else if (command.startsWith("admin_fos_name ")){
			FOS._eventName = command.substring(15);
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_tele1")){
			if (!(FOS._teamsX.get(0)==0 && FOS._teamsY.get(0)==0 && FOS._teamsZ.get(0)==0))
				activeChar.teleToLocation(FOS._teamsX.get(0), FOS._teamsY.get(0), FOS._teamsZ.get(0));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_tele2")){
			if (!(FOS._teamsX.get(1)==0 && FOS._teamsY.get(1)==0 && FOS._teamsZ.get(1)==0))
				activeChar.teleToLocation(FOS._teamsX.get(1), FOS._teamsY.get(1), FOS._teamsZ.get(1));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_tele3")){
			if (!(FOS._flagX==0 && FOS._flagY==0 && FOS._flagZ==0))
				activeChar.teleToLocation(FOS._flagX, FOS._flagY, FOS._flagZ);
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_tele4")){
			if (!(FOS._npcX==0 && FOS._npcY==0 && FOS._npcZ==0))
				activeChar.teleToLocation(FOS._npcX, FOS._npcY, FOS._npcZ);
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_desc ")){
			FOS._eventDesc = command.substring(15);
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_minlvl ")){
			if (!FOS.checkMinLevel(Integer.valueOf(command.substring(17))))
				return false;
			FOS._minlvl = Integer.valueOf(command.substring(17));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_door")){
			L2Object target = activeChar.getTarget();
			if (target==null)
				activeChar.sendMessage("Nothing targeted!");
			else if (target instanceof L2DoorInstance){
				int doorId = ((L2DoorInstance)target).getDoorId();
				if (doorId>0)
				{
					if (!FOS._doors.contains(doorId))
						FOS._doors.addLast(doorId);
				}
			}
			else
				activeChar.sendMessage("Incorrect target.");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_team_flag")){
			FOS._flagX = activeChar.getX();
			FOS._flagY = activeChar.getY();
			FOS._flagZ = activeChar.getZ();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_team_cent")){
			FOS.eventCenterX = activeChar.getX();
			FOS.eventCenterY = activeChar.getY();
			FOS.eventCenterZ = activeChar.getZ();
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_maxlvl ")){
			if (!FOS.checkMaxLevel(Integer.valueOf(command.substring(17))))
				return false;
			FOS._maxlvl = Integer.valueOf(command.substring(17));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_minplayers ")){
			FOS._minPlayers = Integer.valueOf(command.substring(21));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_maxplayers ")){
			FOS._maxPlayers = Integer.valueOf(command.substring(21));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_join_loc ")){
			FOS._joiningLocationName = command.substring(19);
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_npc ")){
			FOS._npcId = Integer.valueOf(command.substring(14));
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_npc_pos")){
			FOS._npcX = activeChar.getX();
			FOS._npcY = activeChar.getY();
			FOS._npcZ = activeChar.getZ();
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_reward ")){
			FOS._rewardId = Integer.valueOf(command.substring(17));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_reward_amount ")){
			FOS._rewardAmount = Integer.valueOf(command.substring(24));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_reward_amount ")){
			FOS._rewardAmount = Integer.valueOf(command.substring(24));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_jointime ")){
			FOS._joinTime = Integer.valueOf(command.substring(19));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_eventtime ")){
			FOS._eventTime = Integer.valueOf(command.substring(20));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_teamname ")){
			String[] params;
			params = command.split(" ");
			if (params.length < 3){
				activeChar.sendMessage("Wrong usage: //fos_teamname <1 or 2> <team name>");
				return false;
			}
			FOS._teams.set(Integer.valueOf(params[1])-1, params[2]);
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_team_pos ")){
			String teamName = command.substring(19);
			FOS.setTeamPos(teamName, activeChar);
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_team_color ")){
			String[] params;
			params = command.split(" ");
			if (params.length < 3){
				activeChar.sendMessage("Wrong usage: //fos_team_color <colorHex> <team name>");
				return false;
			}
			FOS.setTeamColor(command.substring(params[0].length()+params[1].length()+2), Integer.decode("0x" + params[1]));
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_starttype ")){
			FOS._startType = command.split(" ", 2)[1];
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_sealers ")){
			try
			{
				FOS._sealersPerSide = Integer.valueOf(command.split(" ", 2)[1]);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("make sure you only type a number");
			}
			
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_multi ")){
			try
			{
				FOS._defenderRespawnMulti = Double.valueOf(command.split(" ", 2)[1]);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("make sure you only type a number");
			}
			
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_fos_sorttype "))
		{
			try
			{
				FOS._sortType = command.split(" ", 2)[1];
			}
			catch (Exception e)
			{
			}
			
			showMainPage(activeChar);
		}
		else if(command.equals("admin_fos_join"))
		{
			FOS.startJoin();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_teleport"))
		{
			if (FOS._joining)
				FOS.teleportStart();
			showMainPage(activeChar);
		}
		else if(command.equals("admin_fos_start"))
		{
			if (FOS._joining)
				FOS.teleportStart();
			else if (FOS._teleport)
				FOS.startEvent();
			showMainPage(activeChar);
		}
		else if(command.equals("admin_fos_abort"))
		{
			activeChar.sendMessage("Aborting event");
			FOS.abortEvent();
			showMainPage(activeChar);
		}
		else if(command.equals("admin_fos_finish"))
		{
			FOS.finishEvent();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_sit"))
		{
			FOS.sit();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_load"))
		{
			showSiegeLoadPage(activeChar,false);
		}
		else if (command.startsWith("admin_fos_load "))
		{
			int siegeName = Integer.parseInt(command.substring(15));
			FOS.loadData(siegeName);
			showMainPage(activeChar);
		}
		
		else if (command.equals("admin_fos_autoevent"))
		{
			if (FOS._joining || FOS._started || FOS._teleport)
			{
				activeChar.sendMessage("Event is already in progress. Wait until the event ends or Abort it.");
				return false;
			}
			if(FOS._joinTime>0 && FOS._eventTime>0)
			{
				FOS.autoEvent();
			}
			else
				activeChar.sendMessage("Wrong usage: join time or event time invallid.");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_save"))
		{
			FOS.saveData();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_dump")){
			FOS.dumpData();
			showMainPage(activeChar);
		}
		else if (command.equals("admin_fos_pg2")){
			showEditEventPage(activeChar);
		}
		else if (command.equals("admin_fos_pg3")){
			showControlEventPage(activeChar);
		}
		return true;
	}catch(Throwable t){
		activeChar.sendMessage("The command was not used correctly:"+t.toString());
		return false;
	}
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

public void showSiegeLoadPage(L2PcInstance activeChar,boolean autoLoad)
{NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
TextBuilder replyMSG = new TextBuilder("<html><body>");
replyMSG.append("<center><font color=\"LEVEL\">[Siege Engine]</font></center><br><br><br>");
Connection con = null;
try
{
    PreparedStatement statement;
    ResultSet rs;
    con = L2DatabaseFactory.getInstance()
        .getConnection();
    statement = con.prepareStatement("Select * from fortress_siege_event");
    rs = statement.executeQuery();
    String _eventName = "";
    int _eventId = 0;
    while (rs.next())
    {
    	_eventId = rs.getInt("id");
        if (autoLoad && Rnd.get(100) < 10)
        {
            statement.close();
            break;
        }
        _eventName = rs.getString("eventName");
        rs.getString("eventDesc");
        rs.getString("joiningLocation");
        rs.getInt("minlvl");
        rs.getInt("maxlvl");
        rs.getInt("npcId");
        rs.getInt("npcX");
        rs.getInt("npcY");
        rs.getInt("npcZ");
        rs.getInt("npcHeading");
        rs.getInt("rewardId");
        rs.getInt("rewardAmount");
        rs.getInt("joinTime");
        rs.getInt("eventTime");
        rs.getInt("minPlayers");
        rs.getInt("maxPlayers");
        rs.getInt("centerX");
        rs.getInt("centerY");
        rs.getInt("centerZ");
        rs.getString("team1Name");
        rs.getInt("team1X");
        rs.getInt("team1Y");
        rs.getInt("team1Z");
        rs.getInt("team1Color");
        rs.getString("team2Name");
        rs.getInt("team2X");
        rs.getInt("team2Y");
        rs.getInt("team2Z");
        rs.getInt("team2Color");
        rs.getInt("flagX");
        rs.getInt("flagY");
        rs.getInt("flagZ");
        if (FOS._eventName != null && FOS._eventName.equals(_eventName)) replyMSG.append("<a action=\"bypass -h admin_fos_load " + _eventName + "\"><font color=\"FF0000\">" + _eventName + "</font><font color=\"LEVEL\"><-- *loaded*</font></a><br1>");
        else replyMSG.append("<a action=\"bypass -h admin_fos_load " + _eventName + "\"><font color=\"00FF00\">" + _eventName + "</font></a><br1>");
    }
    statement.close();
    if (autoLoad)
    {
        FOS.loadData(0);
        if (activeChar != null) showMainPage(activeChar);
        return;
    }
    replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_fos\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
    replyMSG.append("</body></html>");
    adminReply.setHtml(replyMSG.toString());
    if (activeChar != null) activeChar.sendPacket(adminReply);
}
catch (Exception e)
{
    System.out.println("Exception: AdminFOSEngine.showSiegeLoadPage: " + e.getMessage());
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

public void showEditEventPage(L2PcInstance activeChar){
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	TextBuilder replyMSG = new TextBuilder("<html><body>");
	try{
		replyMSG.append("<center><font color=\"LEVEL\">[Siege Engine]</font></center><br><br><br>");
		replyMSG.append("<table><tr><td><edit var=\"input1\" width=\"125\"></td><td><edit var=\"input2\" width=\"125\"></td></tr></table>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Name\" action=\"bypass -h admin_fos_name $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Description\" action=\"bypass -h admin_fos_desc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Join Location\" action=\"bypass -h admin_fos_join_loc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Max level\" action=\"bypass -h admin_fos_maxlvl $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Min level\" action=\"bypass -h admin_fos_minlvl $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Max players\" action=\"bypass -h admin_fos_maxplayers $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Min players\" action=\"bypass -h admin_fos_minplayers $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"NPC\" action=\"bypass -h admin_fos_npc $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"NPC Pos\" action=\"bypass -h admin_fos_npc_pos\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Reward\" action=\"bypass -h admin_fos_reward $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Reward Amount\" action=\"bypass -h admin_fos_reward_amount $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Join Time\" action=\"bypass -h admin_fos_jointime $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Event Time\" action=\"bypass -h admin_fos_eventtime $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table>Position:<br1><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Siege Flag\" action=\"bypass -h admin_fos_team_flag\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Central\" action=\"bypass -h admin_fos_team_cent\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Name\" action=\"bypass -h admin_fos_teamname $input1 $input2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Color\" action=\"bypass -h admin_fos_team_color $input1 $input2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Team Pos\" action=\"bypass -h admin_fos_team_pos $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td></td></tr></table><br>");
		replyMSG.append("<table><tr><td>Doors: (target a door)</td>");
		replyMSG.append("<td width=\"100\"><button value=\"Add Door\" action=\"bypass -h admin_fos_door\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Sealers#\" action=\"bypass -h admin_fos_sealers $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Sortby\" action=\"bypass -h admin_fos_sorttype $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Starttype\" action=\"bypass -h admin_fos_starttype $input1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br>");
		replyMSG.append("<center><button value=\"Back\" action=\"bypass -h admin_fos\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}catch(Throwable t){
		try{
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Throwable e){return;}
	}
}

public void showControlEventPage(L2PcInstance activeChar){
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	TextBuilder replyMSG = new TextBuilder("<html><body>");
	try{
		replyMSG.append("<center><font color=\"LEVEL\">[Siege Engine]</font></center><br><br><br>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Join\" action=\"bypass -h admin_fos_join\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Teleport\" action=\"bypass -h admin_fos_teleport\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Start\" action=\"bypass -h admin_fos_start\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Abort\" action=\"bypass -h admin_fos_abort\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Finish\" action=\"bypass -h admin_fos_finish\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Sit Force\" action=\"bypass -h admin_fos_sit\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Dump\" action=\"bypass -h admin_fos_dump\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Save\" action=\"bypass -h admin_fos_save\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Load\" action=\"bypass -h admin_fos_load\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Auto Event\" action=\"bypass -h admin_fos_autoevent\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br><table><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Tele>Team1\" action=\"bypass -h admin_fos_tele1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Tele>Team2\" action=\"bypass -h admin_fos_tele2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Tele>Artif\" action=\"bypass -h admin_fos_tele3\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Tele>NPC\" action=\"bypass -h admin_fos_tele4\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("</table><br><center><button value=\"Back\" action=\"bypass -h admin_fos\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	catch(Throwable t){
		try{
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Throwable e){return;}
	}
}

public void showMainPage(L2PcInstance activeChar){
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	TextBuilder replyMSG = new TextBuilder("<html><body>");
	try{
		int howLongAgoInMinutes = 0;
		
		if (TvT.lastEventTime > 0)
		{
			long time = System.currentTimeMillis() - TvT.lastEventTime;
			
			howLongAgoInMinutes = (int)((time/1000)/60);
			
			if (howLongAgoInMinutes < 1)
				howLongAgoInMinutes = 1;
		}
		
		replyMSG.append("<center><font color=\"LEVEL\">[Siege Engine] (last event time: </font>"+howLongAgoInMinutes+" minutes ago)"+"</center><br><br><br>");
		replyMSG.append("<table border=\"0\"><tr>");
		replyMSG.append("<td width=\"100\"><button value=\"Edit\" action=\"bypass -h admin_fos_pg2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td></td>");
		replyMSG.append("<td width=\"100\"><button value=\"Control\" action=\"bypass -h admin_fos_pg3\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br>");
		replyMSG.append("Current event...<br1>");
		replyMSG.append("    ... name:&nbsp;<font color=\"00FF00\">" + FOS._eventName + "</font><br1>");
		replyMSG.append("    ... description:&nbsp;<font color=\"00FF00\">" + FOS._eventDesc + "</font><br1>");
		replyMSG.append("    ... joining location name:&nbsp;<font color=\"00FF00\">" + FOS._joiningLocationName + "</font><br1>");
		replyMSG.append("    ... joining NPC ID:&nbsp;<font color=\"00FF00\">" + FOS._npcId + " on pos " + FOS._npcX + "," + FOS._npcY + "," + FOS._npcZ + "</font>");
		replyMSG.append("<td width=\"100\"><button value=\"Tele>NPC\" action=\"bypass -h admin_fos_tele4\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><br1>");
		replyMSG.append("    ... reward ID  :&nbsp;<font color=\"00FF00\">" + FOS._rewardId + "</font><br1>");
		if ( ItemTable.getInstance().getTemplate(FOS._rewardId) != null)
			replyMSG.append("    ... reward Item:&nbsp;<font color=\"00FF00\">" +  ItemTable.getInstance().getTemplate(FOS._rewardId).getName() + "</font><br1>");
		else
			replyMSG.append("    ... reward Item:&nbsp;<font color=\"00FF00\">(unknown)</font><br1>");
		replyMSG.append("    ... reward Amount:&nbsp;<font color=\"00FF00\">" + FOS._rewardAmount + "</font><br><br>");
		replyMSG.append("    ... Min lvl:&nbsp;<font color=\"00FF00\">" + FOS._minlvl + "</font><br1>");
		replyMSG.append("    ... Max lvl:&nbsp;<font color=\"00FF00\">" + FOS._maxlvl + "</font><br>");
		replyMSG.append("    ... Min Players:&nbsp;<font color=\"00FF00\">" + FOS._minPlayers + "</font><br1>");
		replyMSG.append("    ... Max Players:&nbsp;<font color=\"00FF00\">" + FOS._maxPlayers + "</font><br>");
		replyMSG.append("    ... Joining Time:&nbsp;<font color=\"00FF00\">" + FOS._joinTime + "</font><br1>");
		replyMSG.append("    ... Event Time  :&nbsp;<font color=\"00FF00\">" + FOS._eventTime + "</font><br>");
		replyMSG.append("    ... Leaders/Side  :&nbsp;<font color=\"00FF00\">" + FOS._sealersPerSide + "</font><br>");
		replyMSG.append("    ... Leaders pick method  :&nbsp;<font color=\"00FF00\">" + FOS.getSortType() + "</font><br>");
		replyMSG.append("    ... Event mode  :&nbsp;<font color=\"00FF00\">" + FOS._startType + "</font><br>");
		replyMSG.append("    ... Defender respawn multi  :&nbsp;<font color=\"00FF00\">" + FOS._defenderRespawnMulti + "</font><br>");
		replyMSG.append("Current teams:<br1>");
		replyMSG.append("<center><table border=\"0\">");
		
		if (FOS._teams!=null && !FOS._teams.isEmpty())
			for (String team : FOS._teams){
				replyMSG.append("<tr><td width=\"100\">Name:<font color=\"LEVEL\">" + team + "</font>");
				if (Config.FortressSiege_EVEN_TEAMS.equals("NO") || Config.FortressSiege_EVEN_TEAMS.equals("BALANCE"))
					replyMSG.append(" (" + FOS._teamPlayersCount.get(FOS._teams.indexOf(team)) + " joined)");
				else if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE")){
					if (FOS._teleport || FOS._started)
						replyMSG.append(" (" + FOS._teamPlayersCount.get(FOS._teams.indexOf(team)) + " in)");
				}
				replyMSG.append("</td></tr><tr><td>");
				String c = Integer.toHexString(FOS._teamColors.get(FOS._teams.indexOf(team)));
				while (c.length()<6)
					c="0"+c;
				replyMSG.append("Color: <font color=\"00FF00\">0x"+c+"</font><font color=\""+c+"\"> 8D </font>");
				replyMSG.append("</td></tr><tr><td>");
				replyMSG.append("Position: <font color=\"00FF00\">("+FOS._teamsX.get(FOS._teams.indexOf(team)) + ", " + FOS._teamsY.get(FOS._teams.indexOf(team)) + ", " + FOS._teamsZ.get(FOS._teams.indexOf(team))+")</font>");
				replyMSG.append("</td></tr><tr>");
				if (team.equals(FOS._teams.get(0)))
					replyMSG.append("<td width=\"100\"><button value=\"Tele>Team1\" action=\"bypass -h admin_fos_tele1\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
				if (team.equals(FOS._teams.get(1)))
					replyMSG.append("<td width=\"100\"><button value=\"Tele>Team2\" action=\"bypass -h admin_fos_tele2\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			}
		replyMSG.append("<tr><td>Artifact:  <font color=\"00FF00\">("+FOS._flagX + ", " + FOS._flagY+ ", " + FOS._flagZ+")</font></td></tr>");
		replyMSG.append("<tr><td width=\"100\"><button value=\"Tele>Artif\" action=\"bypass -h admin_fos_tele3\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td>Center Room: <font color=\"00FF00\">("+FOS.eventCenterX + ", " + FOS.eventCenterY+ ", " + FOS.eventCenterZ+")</font></td></tr>");
		replyMSG.append("<tr><td>Door Ids : </td></tr>");
		replyMSG.append("<tr><td><font color=\"00FF00\">(" +FOS.getDoorListString()+ "),</td></tr>");
		replyMSG.append("</table></center>");
		if (Config.FortressSiege_EVEN_TEAMS.equals("SHUFFLE")){
			if (FOS._joining){
				replyMSG.append("<br1>");
				replyMSG.append(FOS._playersShuffle.size() + " players participating. Waiting to shuffle teams (done on teleport)");
				replyMSG.append("<br><br>");
			}
		}
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}catch(Throwable t){
		try{
			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Throwable e){return;}
	}
}
}