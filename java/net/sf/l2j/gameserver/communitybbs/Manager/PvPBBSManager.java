package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class PvPBBSManager extends BaseBBSManager
{
protected static StringBuilder _pvpHTML = null;
protected static StringBuilder _fameHTML = null;
protected static StringBuilder _pkHTML = null;
protected static StringBuilder _rkHTML = null;
protected static StringBuilder _eventkHTML = null;
protected static StringBuilder _siegekHTML = null;
protected static StringBuilder _olywinsHTML = null;

private static final String LOAD_PVP_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY pvpkills DESC LIMIT 0, 15";
private static final String LOAD_FAME_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY fame DESC LIMIT 0, 15";
private static final String LOAD_PK_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY pkkills DESC LIMIT 0, 15";
private static final String LOAD_RK_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY raid_kills DESC LIMIT 0, 15";
private static final String LOAD_EV_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY event_kills DESC LIMIT 0, 15";
private static final String LOAD_SK_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY siege_kills DESC LIMIT 0, 15";
private static final String LOAD_OW_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY olympiad_wins DESC LIMIT 0, 15";

public static PvPBBSManager getInstance()
{
	return SingletonHolder._instance;
}

public void loadHTML(int whichone)
{
	final StringBuilder HTML = StringUtil.startAppend(1000, "");
	
	String info = null;
	switch (whichone)
	{
	default:
		info = "Most famous people of the server (updated every 5 minutes)";
		break;
	case 1:
		info = "Top PvPers of the server (updated every 5 minutes)";
		break;
	case 2:
		info = "The most \"hardcore\" players (updated every 5 minutes)";
		break;
	}

	HTML.append("<html><title>"+info+"</title>")
	.append("<body>")
	.append("<table cellspacing=0 cellpadding=0>")
	.append("<tr>")
	.append("<td><img src=l2trinity_cb.img1 width=765 height=525></td>")
	.append("</tr>")
	.append("</table>")
	.append("<table>")
	.append("<tr>")
	.append("<td width=505></td>")
	.append("<td>")
	.append("<br><br>")
	.append("<table cellspacing=-190 cellpadding=-355>")
	.append("<tr>")
	.append("<td>")
	.append("<table>")
	.append("<tr>")
	.append("<td align=center width=200>")
    .append("        <table border=0 cellpadding=0 cellspacing=0 width=770>")
    .append("            <tr>")
    .append("                <td width=4></td>")
    .append("                <td align=center>")
    .append("                    <table>")
    .append("                    <tr>")
    .append("                    <td height=10>")
    .append("                </td>")
    .append("            </tr>")
    .append("        </table>")
    .append("        <table border=0 cellpadding=0 cellspacing=0 width=765 height=518 bgcolor=000000>")
    .append("            <tr>")
    .append("                <td width=745>")
    .append("                    <table height=490>")
    .append("                    <tr>")
    .append("                    <td width=745 height=50 align=center>")
    .append("                    <br>")
    .append("                    <img src=Btns.header_rankings width=748 height=46>")
    .append("                </td>")
    .append("            </tr>")
    .append("            <tr>")
    .append("                <td>")
    .append("                    <table cellspacing=0 cellpadding=0>")
    .append("                    <tr>")
    .append("                    <td align=center height=60 width=745>")
    .append("                    <table width=542 height=315 >")
    .append("                    <tr>")
    .append("                    <td align=center>")
    .append("                    <table width=745 cellspacing=0 cellpadding=0 height=35>")
    .append("                    <tr>")
    .append("                    <td width=745 align=center>")
    .append("                    <table width=745 height=35 cellspacing=0 cellpadding=0>")
    .append("                    <tr>")
    .append("                    <td width=745 align=center>")
    .append("                    <table  cellspacing=0 cellpadding=0>")
    .append("                    <tr>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbspvp\" value=\"Player Vs Player\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbsfame\" value=\"Fame\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbspk\" value=\"Player Kills\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbsrk\" value=\"Raid Kills\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbsek\" value=\"Event Kills\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("                <td width=120 height=40 align=center>")
    .append("                    <button action=\"bypass _bbssk\" value=\"Siege Kills\" width=120 height=27 back=\"L2UI_ct1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\"/>")
    .append("                </td>")
    .append("            </tr>")
    .append("        </table>")
    .append("        <br>")
    .append("        </td>")
    .append("        </tr>")
    .append("        <tr>")
    .append("            <td align=center <font name=hs12 color=e7a689>");
	String title2 = "                PvP";
	switch (whichone)
	{
	default:
		title2 = "                PvP";
		break;
	case 1:
		title2 = "                PvP";
		break;
	case 2:
		title2 = "                Fame";
		break;
	case 3:
		title2 = "                PK";
		break;
	case 4:
		title2 = "                Raid Kills";
		break;
	case 5:
		title2 = "                Event Kills";
		break;
	case 6:
		title2 = "                Siege Kills";
		break;
	}
    HTML.append(title2)
    .append("                </font>")
    .append("            </td>")
    .append("        </tr>")
    .append("        </table>")
    .append("        </td>")
    .append("        </tr>")
    .append("        </table>")
    .append("        <table cellspacing=0 cellpadding=0 border=0 width=750>")
    .append("            <tr>")
    .append("                <td width=750>")
    .append("                    <br>")
    .append("                    <table width=750 height=25 border=0 bgcolor=011118>")
    .append("                    <tr>")
    .append("                    <td align=center>")
    .append("                    <table border=0>")
    .append("                    <tr>")
    .append("                    <td width=25 valign=top>")
    .append("                    <font color=bc2b0e>#</font>")
    .append("                </td>")
    .append("                <td width=230 valign=top>")
    .append("                    <font color=bc2b0e>Name</font>")
    .append("                </td>")
    .append("                <td width=220 align=center valign=top>")
    .append("                    <font color=bc2b0e>Clan</font>")
    .append("                </td>")
    .append("                <td width=260 align=center valign=top>")
    .append("                    <font color=bc2b0e>Main Class</font>")
    .append("                </td>")
    .append("                <td width=60 align=center valign=top>")
    .append("                    <font color=bc2b0e>Count</font>")
    .append("                </td>")
    .append("            </tr>")
    .append("        </table>")
    .append("        </td>")
    .append("        </tr>")
    .append("        </table>");
	
	
	
//	HTML.append("<html><title>"+info+"</title><body><br><center><table border=0 width=760>");
//	
//	HTML.append("<tr>");
//
//	HTML.append("<td width=50><font color=\"686868\">Rank</font></td>");
//	HTML.append("<td width=178><font color=\"686868\">Player Name</font></td>");
//	HTML.append("<td width=178><font color=\"686868\">Active Class</font></td>");
//	//HTML.append("<td width=150><font color=\"LEVEL\">Base Class</font></td>");
//	HTML.append("<td width=150><font color=\"686868\">Clan</font></td>");
//	HTML.append("<td width=50><font color=\"686868\">%PvP%</font></td>");
//	HTML.append("<td width=50><font color=\"686868\">%Fame%</font></td>");
//	HTML.append("<td width=50><font color=\"686868\">%PK%</font></td>");
//	
	HTML.append("</tr>");
	PreparedStatement statement;
	ResultSet rs;
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();

		switch (whichone)
		{
		default:
			statement = con.prepareStatement(LOAD_PVP_HTML);
			break;
		case 1:
			statement = con.prepareStatement(LOAD_PVP_HTML);
			break;
		case 2:
			statement = con.prepareStatement(LOAD_FAME_HTML);
			break;
		case 3:
			statement = con.prepareStatement(LOAD_PK_HTML);
			break;
		case 4:
			statement = con.prepareStatement(LOAD_RK_HTML);
			break;
		case 5:
			statement = con.prepareStatement(LOAD_EV_HTML);
			break;
		case 6:
			statement = con.prepareStatement(LOAD_SK_HTML);
			break;
		}
		
		boolean lol = true;
		String color = "FFF8C6";
		String colorName = "bdccd4";
		String colorClass = "bdccd4";
		String colorPvPs = "bdccd4";
		String colorFame = "686868";
		String bgcolor;
		

		if (lol)
		{
			lol = false;
			//color = "817679";
			bgcolor = "bgcolor=00080b";
		}
		else
		{
			lol = true;
			//color = "FFF8C6";
			bgcolor = "bgcolor=011118";
		}
		
		int pos = 0;
		rs = statement.executeQuery();
		
		while (rs.next())
		{

			if (lol)
			{
				lol = false;
				//color = "817679";
				bgcolor = "bgcolor=011118";
			}
			else
			{
				lol = true;
				//color = "FFF8C6";
				bgcolor = "bgcolor=00080b";
			}	
			
			String name = rs.getString("char_name");
			String title = rs.getString("title");
			if (title == null)
				title = "";
			title = title.replaceAll("<", "&lt;");
			title = title.replaceAll(">", "&gt;");
			String pvps = String.valueOf(rs.getInt("pvpkills"));
			String fame = String.valueOf(rs.getInt("fame"));
			String pks = String.valueOf(rs.getInt("pkkills"));
			String rks = String.valueOf(rs.getInt("raid_kills"));
			String eks = String.valueOf(rs.getInt("event_kills"));
			String sks = String.valueOf(rs.getInt("siege_kills"));
			String ow = String.valueOf(rs.getInt("olympiad_wins"));
			L2Clan clan = ClanTable.getInstance().getClan(rs.getInt("clanid"));
			String clanname = "-";
			if (clan != null)
				clanname = clan.getName()+ " (Lvl "+clan.getLevel()+")";
			
			//String baseclass = CharTemplateTable.getInstance().getClassNameById(rs.getInt("base_class"));
			String activeclass = CharTemplateTable.getInstance().getClassNameById(rs.getInt("classid"));
			
			/*if(rs.getBoolean("online"))
				name = "<a action=\"bypass _bbsloc;playerinfo;" +name+ "\">"+name+"</a>";
*/
			if(rs.getBoolean("online"))
			{
				colorName = "379d2d";
				colorClass = "916e27";
				colorPvPs = "8c4848";
				colorFame = "744ccf";
			}
				
			if(rs.getBoolean("online"))
				name = "<a action=\"bypass _bbsloc;playerinfo;" +name+ "\"><font color="+colorName+">"+name+"</font></a>";
			else
				name = "<font color="+colorName+">"+name+"</font>";
			
			HTML.append("<table width=750 border=0 height=25 "+bgcolor+">");
			HTML.append("    <tr>");
			HTML.append("        <td align=center>");
			HTML.append("            <table width=750 border=0>");
			HTML.append("            <tr>");
			HTML.append("            <td width=25 valign=top>");
			HTML.append("            <font name=__SystemeditBoxFont color=ff8e3b>"+(pos + 1)+"</font>");
			HTML.append("        </td>");
			HTML.append("        <td width=180 valign=top>");
			HTML.append(			name);
			HTML.append("        </td>");
			HTML.append("        <td width=188 align=center valign=top>");
			HTML.append("            <font color="+colorClass+">"+clanname+"</font>");
			HTML.append("        </td>");
			HTML.append("        <td width=200 align=center valign=top>");
			HTML.append("            <font color="+colorClass+">"+activeclass+" LvL ("+rs.getInt("level")+")</font>");
			HTML.append("        </td>");
			HTML.append("        <td width=60 align=center valign=top>");

			String count = "";
			switch (whichone)
			{
			default:
				count = pvps;
				break;
			case 1:
				count = pvps;
				break;
			case 2:
				count = fame;
				break;
			case 3:
				count = pks;
				break;
			case 4:
				count = rks;
				break;
			case 5:
				count = eks;
				break;
			case 6:
				count = sks;
				break;
			}
			
			HTML.append("            <font color="+colorPvPs+">"+count+"</font>");
			HTML.append("        </td>");
			HTML.append("    </tr>");
			HTML.append("</table>");
			HTML.append("</td>");
			HTML.append("</tr>");
			HTML.append("</table>");
			
//			HTML.append("</table>");
//			HTML.append("<table "+bgcolor+" width=760>");
//			HTML.append("<tr>");
//			HTML.append("<td width=50><font color=686868>"+(pos + 1)+"</font></td>");
//			HTML.append("<td width=178><font color="+colorName+">"+name+"</font></td>");
//			//HTML.append("<td width=150><font color="+color+">"+title+"</font></td>");
//			HTML.append("<td width=178><font color="+colorClass+">"+activeclass+" LvL ("+rs.getInt("level")+")</font></td>");
//			HTML.append("<td width=150><font color="+colorClass+">"+clanname+"</font></td>");
//			HTML.append("<td width=50><font color="+colorPvPs+">"+pvps+"</font></td>");
//			HTML.append("<td width=50><font color="+colorFame+">"+fame+"</font></td>");
//			HTML.append("<td width=50><font color=686868>"+pks+"</font></td>");
//			HTML.append("</tr>");
			pos++;
			colorName = "bdccd4";
			colorClass = "bdccd4";
			colorPvPs = "bdccd4";
			colorFame = "bdccd4";
		}
		
		rs.close();
		statement.close();
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}

HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("        </td>");
HTML.append("        </tr>");
HTML.append("        </table>");
HTML.append("</center>");
HTML.append("</td>");
HTML.append("</tr>");
HTML.append("</table>");
HTML.append("</td>");
HTML.append("</tr>");
HTML.append("</table>");
HTML.append("</td>");
HTML.append("</tr>");
HTML.append("</table>");
HTML.append("</body>");
HTML.append("</html> ");
		//HTML.append("    </body>");
		//HTML.append("</html>");
//		HTML.append("</table><br></body></html>");
	}
	
	switch (whichone)
	{
	default:
		_pvpHTML = HTML;
		break;
	case 1:
		_pvpHTML = HTML;
		break;
	case 2:
		_fameHTML = HTML;
		break;
	case 3:
		_pkHTML = HTML;
		break;
	case 4:
		_rkHTML = HTML;
		break;
	case 5:
		_eventkHTML = HTML;
		break;
	case 6:
		_siegekHTML = HTML;
		break;
	}
}

private PvPBBSManager()
{
	ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
		public void run()
		{
			PvPBBSManager.getInstance().loadHTML(0);
			PvPBBSManager.getInstance().loadHTML(1);
			PvPBBSManager.getInstance().loadHTML(2);
			PvPBBSManager.getInstance().loadHTML(3);
			PvPBBSManager.getInstance().loadHTML(4);
			PvPBBSManager.getInstance().loadHTML(5);
			PvPBBSManager.getInstance().loadHTML(6);
		}
	}, 0, 10000);
}

/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{

	if (command.startsWith("_bbspvp") || command.startsWith("_bbsgetfav"))
	{
		String html = _pvpHTML.toString();
		
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbsfame"))
	{
		String html = _fameHTML.toString();
		
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbspk"))
	{
		String html = _pkHTML.toString();
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbsrk"))
	{
		String html = _rkHTML.toString();
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbsek"))
	{
		String html = _eventkHTML.toString();
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbssk"))
	{
		String html = _siegekHTML.toString();
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbsow"))
	{
		String html = _olywinsHTML.toString();
		separateAndSend(html, activeChar);
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}

/**
 * 
 * @see BaseBBSManager#parsewrite(String, String, String, String, String, String, L2PcInstance)
 */
@Override
public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
{
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final PvPBBSManager _instance = new PvPBBSManager();
}
}