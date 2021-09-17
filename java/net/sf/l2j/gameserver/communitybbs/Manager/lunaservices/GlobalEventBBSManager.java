/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import luna.custom.globalScheduler.template.FuturedGlobalEvent;
import luna.util.LunaUtil;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.util.StringUtil;

public class GlobalEventBBSManager extends BaseBBSManager
{
	public static final String PATH = "data/html/CommunityBoard/global_events";
	private static final SimpleDateFormat	sdf				= new SimpleDateFormat("HH:mm");

	private StringBuilder eventsSchedule = StringUtil.startAppend(1000, "");
	String teleport_to_npc_button = "";
	/**
	 * @return
	 */
	public static GlobalEventBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private GlobalEventBBSManager()
	{}
	
	/**
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
				
				String filename = "";
				filename = "/bbs_global_events.htm";
				String html = HtmCache.getInstance().getHtmForce(PATH + filename);
				
				
				generateScheduleLine();
				
				html = html.replace("%eventsSchedule%", eventsSchedule);
				separateAndSend(html, activeChar);
	}

	public void generateScheduleLine()
	{
		eventsSchedule.setLength(0);
		StringUtil.startAppend(1000, "");
		int count = 0;
		for (FuturedGlobalEvent evt : Communicator.getInstance().getGlobalEventList())
		{

			boolean lol = true;
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
			count++;
			String eventIcon = "";
			String eventId = "";
			String evtEventId = evt.getEventType();
			
			switch(evtEventId)
			{
				case "pagan_open":
					eventId = "Pagan Opens";
					eventIcon = "L2UI_CT12.Minimap.map_inzone_gate_i00";
					break;
				case "pagan_close":
					eventId = "Pagan Close";
					eventIcon = "L2UI_CT12.map_inzone_gateInactive_i00_Over";
					break;
			}
 			Date date = new Date(System.currentTimeMillis());
			String timeStr = sdf.format(date);
			LocalTime start = LocalTime.parse(timeStr);
			LocalTime stop = LocalTime.parse(evt.getTime());
			Duration duration = Duration.between(start, stop);
			String timeLeft = "" + LunaUtil.formatTime((int) duration.toSeconds());
			String timeLeftForEventToBegin = timeLeft;
			String timeLeftForEventToBegin2 = timeLeft;

			if(evt.getDay() != Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
			{

		        int daysDiff = 0;
		        
				Calendar c = Calendar.getInstance();
		        c.add(Calendar.DAY_OF_WEEK, evt.getDay());
		        c.set(Calendar.HOUR_OF_DAY, 0);
		        c.set(Calendar.MINUTE, 0);
		        c.set(Calendar.SECOND, 0);
		        c.set(Calendar.MILLISECOND, 0);
		        
		        
		        
		        if(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7 && evt.getDay() == 1)
		        {
			        daysDiff = 1;
		        }
		        else
		        {
		        	daysDiff = evt.getDay() - Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		        }
		        int hoursDiff = daysDiff *24;
		        long howMany = (c.getTimeInMillis()-System.currentTimeMillis());
	        
				date = new Date(System.currentTimeMillis());
				timeStr = sdf.format(date);
				start = LocalTime.parse(timeStr);
				stop = LocalTime.parse(evt.getTime());
				duration = Duration.between(start, stop);
				duration = duration.plusHours(hoursDiff);
				timeLeft = "" + LunaUtil.formatTime((int) duration.toSeconds());
				timeLeftForEventToBegin = timeLeft;
				timeLeftForEventToBegin2 = timeLeft;
				
			}
			
			if(timeLeftForEventToBegin.contains("-"))
			{
				count--;
				continue;
			}
			if(count <= 10)
				eventsSchedule.append( 
			"<tr>" + 
			" <td>" + 
			"	<table width=245 height=35 "+bgcolor+">" + 
			"		<tr>" + 
			"			<td width=35>" + 
			"				<img src="+eventIcon+" width=32 height=32>" + 
			"			</td>" + 
			"			<td width=180>" + 
			"				<font color=\"8AC0F6\">"+eventId+"</font>" + 
			"			</td>" + 
			"			<td width=180 align=\"center\">" + 
			"				<font color=\"57799B\">In "/*+evt.getTime()+ */+timeLeftForEventToBegin+"</font>" + 
			"			</td>" + 
			"		</tr>" + 
			"		<tr>" + 
			"			<td width=35>" + 
			"			</td>" + 
			"		</tr>" + 
			"	</table>" + 
			" </td>" + 
			"</tr>");
		}
	}
	
	
	/**
	 * @see BaseBBSManager#parsewrite(String, String, String, String, String, String, L2PcInstance)
	 */
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GlobalEventBBSManager _instance = new GlobalEventBBSManager();
	}
}