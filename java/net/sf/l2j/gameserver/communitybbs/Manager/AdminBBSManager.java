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
package net.sf.l2j.gameserver.communitybbs.Manager;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventsCollector;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class AdminBBSManager extends BaseBBSManager
{
	public static final String PATH = "data/html/admin/trinity_events/";
	private StringBuilder tvts = StringUtil.startAppend(1000, "");
	private StringBuilder ctfs = StringUtil.startAppend(1000, "");
	private StringBuilder dms = StringUtil.startAppend(1000, "");
	private StringBuilder sieges = StringUtil.startAppend(1000, "");
	private StringBuilder domis = StringUtil.startAppend(1000, "");
	private StringBuilder hgs = StringUtil.startAppend(1000, "");
/**
 * @return
 */
public static AdminBBSManager getInstance()
{
	return SingletonHolder._instance;
}

private AdminBBSManager()
{
}

/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	if (!activeChar.isGM())
	{
		return;
	}
	if (command.startsWith("admin_bbs"))
	{
		
		String filename = "bbs_events_manage.htm";
		
		String html = HtmCache.getInstance().getHtmForce(PATH + filename);
		
		


		

		String type = EventEngine.getInstance().getEventActive()?EventEngine.getInstance().getActiveEvent().getType():"";
		switch(type)
		{
			case "CTF":
				type = "Capture The Flag";
				break;
			case "DM":
				type = "Deathmatch";
				break;
			case "TeamVsTeam":
				type = "Team VS Team";
				break;
			case "SiegeEvent":
				type = "Siege Event";
				break;
			case "Domination":
				type = "Domination";
				break;
			case "HuntingGrounds":
				type = "Hunting Grounds";
				break;
		}
		generateTvts();
		generateCtfs();
		generateDMs();
		generateDominations();
		generateSiegeEvent();
		generateHuntingGrounds();
		
		html = html.replace("%tvts%", tvts);
		html = html.replace("%ctfs%", ctfs);
		html = html.replace("%dms%", dms);
		html = html.replace("%domis%", domis);
		html = html.replace("%sieges%", sieges);
		html = html.replace("%hgs%", hgs);
		separateAndSend(html, activeChar);
		//generateTopStatsGlobal();
	}
	else
	{
		
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
	
}
public void generateTvts()
{
	tvts.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	tvts.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_tvta\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection("TeamVsTeam").size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection("TeamVsTeam").get(i).getId() ? "639E9F" : "00080b";
		}
		tvts.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection("TeamVsTeam").get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection("TeamVsTeam").get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection("TeamVsTeam").get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
public void generateCtfs()
{
	String collection = "CTF";
	ctfs.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	ctfs.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_ctfa\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection(collection).size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection(collection).get(i).getId() ? "639E9F" : "00080b";
		}
		ctfs.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
public void generateDMs()
{
	String collection = "DM";
	dms.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	dms.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_dma\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection(collection).size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection(collection).get(i).getId() ? "639E9F" : "00080b";
		}
		dms.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
public void generateDominations()
{
	String collection = "Domination";
	domis.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	domis.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_domia\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection(collection).size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection(collection).get(i).getId() ? "639E9F" : "00080b";
		}
		domis.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
public void generateSiegeEvent()
{
	String collection = "SiegeEvent";
	sieges.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	sieges.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_fosa\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection(collection).size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection(collection).get(i).getId() ? "639E9F" : "00080b";
		}
		sieges.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
public void generateHuntingGrounds()
{
	String collection = "HuntingGrounds";
	hgs.setLength(0);
	StringUtil.startAppend(1000, "");
	int count = 0;
	String active = "00080b";
	hgs.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=35 bgcolor=011118>" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">ID</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">NAME</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"stop\" action=\"bypass -h admin_hga\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	for (int i = 0; i < EventsCollector.getInstance().getEventCollection(collection).size(); i++)
	{
		Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
		if (e != null)
		{
			active = EventEngine.getInstance().getActiveEvent().getId() == EventsCollector.getInstance().getEventCollection(collection).get(i).getId() ? "639E9F" : "00080b";
		}
		hgs.append( 
		"<tr>" + 
		" <td>" + 
		"	<table width=235 height=25 bgcolor="+active+">" + 
		"		<tr>" + 
		"			<td width=80>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"</font>" + 
		"			</td>" + 
		"			<td width=180>" + 
		"				<font color=\"8AC0F6\">"+EventsCollector.getInstance().getEventCollection(collection).get(i).getName()+"</font>" + 
		"			</td>" + 
		"			<td width=80>" + 
		"				<button value=\"start\" action=\"bypass -h admin_run_ev "+EventsCollector.getInstance().getEventCollection(collection).get(i).getId()+"\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>" +
		"			</td>" + 
		"		</tr>" + 
		"	</table>" + 
		" </td>" + 
		"</tr>");
	}		
}
/**
 * 
 * @see BaseBBSManager#parsewrite(String, String, String, String, String, String, L2PcInstance)
 */
@Override
public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
{
	if (!activeChar.isGM())
	{
		return;
	}
	
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final AdminBBSManager _instance = new AdminBBSManager();
}
}