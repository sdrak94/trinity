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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import luna.util.LunaUtil;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.dataTables.FuturedEvent;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventCommander;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventRewardManager;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.util.Rnd;

public class EventBBSManager extends BaseBBSManager
{
	public static final String				PATH					= "data/html/CommunityBoard/events";
	private static final SimpleDateFormat	sdf						= new SimpleDateFormat("HH:mm");
	private StringBuilder					globalStats				= StringUtil.startAppend(1000, "");
	private StringBuilder					scoreLine				= StringUtil.startAppend(1000, "");
	private StringBuilder					winnerRewards			= StringUtil.startAppend(1000, "");
	private StringBuilder					loserRewards			= StringUtil.startAppend(1000, "");
	private StringBuilder					tieRewards				= StringUtil.startAppend(1000, "");
	private StringBuilder					topRewards				= StringUtil.startAppend(1000, "");
	private StringBuilder					playerStats				= StringUtil.startAppend(1000, "");
	private StringBuilder					eventsSchedule			= StringUtil.startAppend(1000, "");
	String									teleport_to_npc_button	= "";
	
	/**
	 * @return
	 */
	public static EventBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private EventBBSManager()
	{}
	
	/**
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		String filename = "";
		if (EventEngine.getInstance().getEventActive())
		{
			filename = "/bbs_events_active.htm";
			// filename = "/bbs_events_innactive.htm";
		}
		else
		{
			// filename = "/bbs_events_active.htm";
			filename = "/bbs_events_innactive.htm";
		}
		String html = HtmCache.getInstance().getHtmForce(PATH + filename);
		String type = EventEngine.getInstance().getEventActive() ? EventEngine.getInstance().getActiveEvent().getType() : "";
		switch (type)
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
		generateScheduleLine();
		generateScoreLine();
		generateRewardsLine();
		generateHeader();
		String rewards = "" + winnerRewards + tieRewards + loserRewards;
		html = html.replace("%activeEvent%", EventEngine.getInstance().getEventActive() ? "Active Event: " + type + " " + EventEngine.getInstance().getActiveEvent().getName() : "No active event at the moment.");
		html = html.replace("%activeEventTeleportToNpc%", EventEngine.getInstance().getEventActive() ? teleport_to_npc_button : "");
		html = html.replace("%eventType%", type);
		html = html.replace("%eventName%", EventEngine.getInstance().getEventActive() ? EventEngine.getInstance().getActiveEvent().getName() : "");
		html = html.replace("%eventPhase%", EventEngine.getInstance().getEventActive() ? Communicator.getInstance().getEventPhase() : "");
		html = html.replace("%timeRemaining%", EventEngine.getInstance().getEventActive() ? Communicator.getInstance().getTimeRemaining() : "");
		html = html.replace("%eralyRegSlots%", "");
		html = html.replace("%registered%", EventEngine.getInstance().getEventActive() ? "" + Communicator.getInstance().getRegPlayers() : "");
		html = html.replace("%eventsSchedule%", eventsSchedule);
		html = html.replace("%rewards%", EventEngine.getInstance().getEventActive() ? rewards : "");
		html = html.replace("%personalStats%", getPersonalStats(activeChar));
		html = html.replace("%topStats%", globalStats);
		html = html.replace("%scoreLine%", scoreLine);
		html = html.replace("%topPlayers%", Communicator.getInstance().getTopPlayers());
		html = html.replace("%nextEvent%", Communicator.getInstance().getNextEvent());
		separateAndSend(html, activeChar);
		generateTopStatsGlobal();
	}
	
	public void generateHeader()
	{
		if (EventEngine.getInstance().getEventActive())
		{
			boolean joining = false;
			boolean started = false;
			Event e = EventEngine.getInstance().getActiveEvent();
			if (e != null)
			{
				switch (e.getType())
				{
					case "HuntingGrounds":
						if (NewHuntingGrounds._joining)
						{
							joining = true;
						}
						if (NewHuntingGrounds._started)
						{
							started = true;
						}
						break;
					case "Domination":
						if (NewDomination._joining)
						{
							joining = true;
						}
						if (NewDomination._started)
						{
							started = true;
						}
						break;
					case "TeamVsTeam":
						if (NewTvT._joining)
						{
							joining = true;
						}
						if (NewTvT._started)
						{
							started = true;
						}
						break;
					case "CTF":
						if (NewCTF._joining)
						{
							joining = true;
						}
						if (NewCTF._started)
						{
							started = true;
						}
						break;
					case "SiegeEvent":
						if (NewFOS._joining)
						{
							joining = true;
						}
						if (NewFOS._started)
						{
							started = true;
						}
						break;
					case "DM":
						if (NewDM._joining)
						{
							joining = true;
						}
						if (NewDM._started)
						{
							started = true;
						}
						break;
				}
				if (joining)
				{
					int rndOffset = Rnd.get(-350, 450);
					int x = EventEngine.getInstance().getActiveEvent().getRegNpcLoc().getX() + rndOffset;
					int y = EventEngine.getInstance().getActiveEvent().getRegNpcLoc().getY() + rndOffset;
					int z = EventEngine.getInstance().getActiveEvent().getRegNpcLoc().getZ();
					teleport_to_npc_button = "<table border=0 width=760>" + "						<tr>" + "							<td align=\"center\">" + "								<button action=\"bypass _bbsteleto_exloc " + x + "," + y + "," + z + "\" value=\"Teleport To Registration Npc\" width=350 height=25 back=\"L2UI_CT1.Button_DF_Msn_Down\" fore=\"L2UI_CT1.Button_DF_Msn\">" + "							</td>" + "						</tr>" + "					</table>";
				}
				if (!joining && started)
				{
					teleport_to_npc_button = "";
					teleport_to_npc_button = scoreLine.toString();
				}
			}
			else
			{
				teleport_to_npc_button = "";
			}
		}
	}
	
	public void generateTopStatsGlobal()
	{
		globalStats.setLength(0);
		globalStats = StringUtil.startAppend(1000, "");
		globalStats.append(EventStats.getInstance().getTopStats("Team Vs Team", "kills"));
		globalStats.append(EventStats.getInstance().getTopStats("Capture The Flag", "kills"));
		globalStats.append(EventStats.getInstance().getTopStats("Deathmatch", "kills"));
		globalStats.append(EventStats.getInstance().getTopStats("Hunting Grounds", "kills"));
		globalStats.append(EventStats.getInstance().getTopStats("Domination", "kills"));
		globalStats.append(EventStats.getInstance().getTopStats("Siege Event", "kills"));
	}
	
	String getPersonalStats(L2PcInstance activeChar)
	{
		final StringBuilder playerStats = StringUtil.startAppend(1000, "");
		PreparedStatement statement;
		ResultSet rs;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM event_stats WHERE player = ?");
			boolean lol = false;
			String bgcolor;
			statement.setInt(1, activeChar.getObjectId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				String eventName = rs.getString("event");
				String countPlayed = String.valueOf(rs.getInt("count_played"));
				String wins = String.valueOf(rs.getInt("wins"));
				String loses = String.valueOf(rs.getInt("loses"));
				String kills = String.valueOf(rs.getInt("kills"));
				String deaths = String.valueOf(rs.getInt("deaths"));
				String score = String.valueOf(rs.getInt("score"));
				if (lol)
				{
					lol = false;
					// color = "817679";
					bgcolor = "bgcolor=00080b";
				}
				else
				{
					lol = true;
					// color = "FFF8C6";
					bgcolor = "bgcolor=011118";
				}
				playerStats.append("<tr>" + "	<td>" + "		<table width=496 height=15 " + bgcolor + ">" + "		   <tr>" + "			  <td width=110>" + "				 <font color=8AC0F6>" + eventName + "</font>" + "			  </td>" + "			  <td width=122 align=center>" + "				 <font color=57799B>" + countPlayed + "</font>" + "			  </td>" + "			  <td width=65 align=center><font color=57799B>" + wins + "</font></td>" + "			  <td width=65 align=center><font color=57799B>" + loses + "</font></td>" + "			  <td width=65 align=center><font color=57799B>" + kills + "</font></td>" + "			  <td width=65 align=center><font color=57799B>" + deaths + "</font></td>" + "			  <td width=65 align=center><font color=57799B>" + score + "</font></td>" + "		   </tr>" + "		</table>" + "	</td>" + "</tr>");
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
			{}
		}
		return playerStats.toString();
	}
	
	public void generateScheduleLine()
	{
		eventsSchedule.setLength(0);
		StringUtil.startAppend(1000, "");
		int count = 0;
		for (FuturedEvent evt : Communicator.getInstance().getEventList())
		{
			boolean lol = true;
			String bgcolor;
			if (lol)
			{
				lol = false;
				// color = "817679";
				bgcolor = "bgcolor=00080b";
			}
			else
			{
				lol = true;
				// color = "FFF8C6";
				bgcolor = "bgcolor=011118";
			}
			count++;
			String eventIcon = "";
			String eventId = "";
			String evtEventId = evt.getEventId();
			if (EventCommander.getInstance().containsNumeric(evtEventId))
			{
				evtEventId = EventsParser.getInstance().getEvents().get(Integer.parseInt(evtEventId)).getType();
			}
			switch (evtEventId)
			{
				case "CTF":
					eventId = "Capture The Flag";
					eventIcon = "icon.etc_alliance_flag_i00";
					break;
				case "DM":
					eventId = "Deathmatch";
					eventIcon = "icon.skill0444";
					break;
				case "TeamVsTeam":
					eventId = "Team VS Team";
					eventIcon = "icon.skill1584";
					break;
				case "SiegeEvent":
					eventId = "Siege Event";
					eventIcon = "icon.weapon_fort_flag_i00";
					break;
				case "Domination":
					eventId = "Domination";
					eventIcon = "icon.skill30028";
					break;
				case "HuntingGrounds":
					eventId = "Hunting Grounds";
					eventIcon = "icon.skill10757";
					break;
				default:
					eventId = evt.getEventId();
					eventIcon = "icon.skill30027";
					break;
			}
			Date date = new Date(System.currentTimeMillis());
			String timeStr = sdf.format(date);
			LocalTime start = LocalTime.parse(timeStr);
			LocalTime stop = LocalTime.parse(evt.getTime());
			Duration duration = Duration.between(start, stop);
			String timeLeft = " " + LunaUtil.formatTime((int) duration.toSeconds());
			String timeLeftForEventToBegin = timeLeft;
			String timeLeftForEventToBegin2 = timeLeft;
			if (evt.getDay() != Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
			{
				int daysDiff = 0;
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DAY_OF_WEEK, evt.getDay());
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.set(Calendar.MINUTE, 0);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7 && evt.getDay() == 1)
				{
					daysDiff = 1;
				}
				else
				{
					daysDiff = evt.getDay() - Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				}
				int hoursDiff = daysDiff * 24;
				long howMany = (c.getTimeInMillis() - System.currentTimeMillis());
				date = new Date(System.currentTimeMillis());
				timeStr = sdf.format(date);
				start = LocalTime.parse(timeStr);
				stop = LocalTime.parse(evt.getTime());
				duration = Duration.between(start, stop);
				duration = duration.plusHours(hoursDiff);
				timeLeft = " " + LunaUtil.formatTime((int) duration.toSeconds());
				timeLeftForEventToBegin = timeLeft;
				timeLeftForEventToBegin2 = timeLeft;
			}
			if (count == 1)
			{
				Communicator.getInstance().setNextEvent(eventId + " in" + timeLeftForEventToBegin2);
				Communicator.getInstance().setNextEventIcon(eventIcon);
			}
			if (timeLeftForEventToBegin.contains("-"))
			{
				count--;
				continue;
			}
			if (count <= 10)
				eventsSchedule.append("<tr>" + " <td>" + "	<table width=245 height=35 " + bgcolor + ">" + "		<tr>" + "			<td width=35>" + "				<img src=" + eventIcon + " width=32 height=32>" + "			</td>" + "			<td width=180>" + "				<font color=\"8AC0F6\">" + eventId + "</font>" + "			</td>" + "			<td width=180 align=\"center\">" + "				<font color=\"57799B\">Starts in"/*
																																																																																																																 * +
																																																																																																																 */ + timeLeftForEventToBegin + "</font>" + "			</td>" + "		</tr>" + "		<tr>" + "			<td width=35>" + "			</td>" + "		</tr>" + "	</table>" + " </td>" + "</tr>");
		}
	}
	
	public void generateScoreLine()
	{
		scoreLine.setLength(0);
		boolean started = false;
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			switch (e.getType())
			{
				case "HuntingGrounds":
					if (NewHuntingGrounds._started)
					{
						started = true;
					}
					break;
				case "Domination":
					if (NewDomination._started)
					{
						started = true;
					}
					break;
				case "TeamVsTeam":
					if (NewTvT._started)
					{
						started = true;
					}
					break;
				case "CTF":
					if (NewCTF._started)
					{
						started = true;
					}
					break;
				case "SiegeEvent":
					if (NewFOS._started)
					{
						started = true;
					}
					break;
				case "DM":
					if (NewDM._started)
					{
						started = true;
					}
					break;
			}
		}
		if (EventEngine.getInstance()._isEventActive && started)
		{
			scoreLine.setLength(0);
			scoreLine = StringUtil.startAppend(1000, "");
			String Team1C = "";
			String Team2C = "";
			if (EventEngine.getInstance().getActiveEvent().getType().equals("DM"))
			{
				return;
			}
			switch (Communicator.getInstance().getTeam1Name())
			{
				case "Red":
					Team1C = "851818";
					break;
				case "Green":
					Team1C = "36B234";
					break;
				case "Blue":
					Team1C = "3475B2";
					break;
				case "Orange":
					Team1C = "E59B22";
					break;
				case "Purple":
					Team1C = "6322E5";
					break;
				case "Yellow":
					Team1C = "LEVEL";
					break;
				default:
					Team2C = "3475B2";
					break;
			}
			switch (Communicator.getInstance().getTeam2Name())
			{
				case "Red":
					Team2C = "851818";
					break;
				case "Green":
					Team2C = "36B234";
					break;
				case "Blue":
					Team2C = "3475B2";
					break;
				case "Orange":
					Team2C = "E59B22";
					break;
				case "Purple":
					Team2C = "6322E5";
					break;
				case "Yellow":
					Team2C = "LEVEL";
					break;
				default:
					Team2C = "851818";
					break;
			}
			scoreLine.append("<table border=0 width=810 bgcolor=202531>" + "						<tr>" + "							<td align=\"center\">" + "								<font  name=\"hs12\"  color=" + Team1C + ">" + Communicator.getInstance().getTeam1Name() + " Score: " + Communicator.getInstance().getTeam1Score() + "</font>" + "							</td>" + "							<td align=\"center\">" + "								<font  name=\"hs12\"  color=LEVEL> VS </font>" + "							</td>" + "							<td align=\"center\">" + "								<font  name=\"hs12\"  color=" + Team2C + ">" + Communicator.getInstance().getTeam2Name() + " Score: " + Communicator.getInstance().getTeam2Score() + "</font>" + "							</td>" + "						</tr>" + "					</table>");
		}
	}
	
	public void generateRewardsLine()
	{
		boolean lol = true;
		winnerRewards.setLength(0);
		winnerRewards = StringUtil.startAppend(1000, "");
		winnerRewards.append("<tr>" + "	<td width=250 align=center>" + "		<br>" + "		<font name=\"hs12\" color=\"0099FF\">Winner Rewards</font>" + "		<br>" + "	</td>" + "</tr>");
		loserRewards.setLength(0);
		loserRewards = StringUtil.startAppend(1000, "");
		loserRewards.append("<tr>" + "	<td width=250 align=center>" + "		<br>" + "		<font name=\"hs12\" color=\"0099FF\">Loser Rewards</font>" + "		<br>" + "	</td>" + "</tr>");
		if (EventEngine.getInstance()._isEventActive)
		{
			if (!EventEngine.getInstance().getActiveEvent().getType().equalsIgnoreCase("DM"))
				;
			{
				tieRewards.setLength(0);
				tieRewards = StringUtil.startAppend(1000, "");
				tieRewards.append("<tr>" + "	<td width=250 align=center>" + "		<br>" + "		<font name=\"hs12\" color=\"0099FF\">Tie Rewards</font>" + "		<br>" + "	</td>" + "</tr>");
				for (RewardsTemplate rew : EventRewardManager.getInstance().getRewards(3))
				{
					String itemIcon = IconsTable.getInstance().getItemIcon(rew.getItemId());
					L2Item itemtmp = ItemTable.getInstance().getTemplate(rew.getItemId());
					String itemName = itemtmp.getName();
					String ammount = "x" + rew.getAmmount() + " ";
					String chance = "" + rew.getChance();
					String bgcolor;
					if (lol)
					{
						lol = false;
						// color = "817679";
						bgcolor = "bgcolor=00080b";
					}
					else
					{
						lol = true;
						// color = "FFF8C6";
						bgcolor = "bgcolor=011118";
					}
					tieRewards.append("<tr>" + " <td>" + "	<table width=245 height=35 " + bgcolor + ">" + "		<tr>" + "			<td width=35>" + "				<img src=" + itemIcon + " width=32 height=32>" + "			</td>" + "			<td width=180>" + "				<font color=\"8AC0F6\">" + ammount + itemName + "</font>" + "			</td>" + "			<td width=180 align=\"center\">" + "				<font color=\"57799B\">" + chance + "%</font>" + "			</td>" + "		</tr>" + "		<tr>" + "			<td width=35>" + "			</td>" + "		</tr>" + "	</table>" + " </td>" + "</tr>");
				}
			}
		}
		if (EventEngine.getInstance()._isEventActive)
		{
			if (EventEngine.getInstance().getActiveEvent().getType().equalsIgnoreCase("dm"))
			{
				tieRewards.setLength(0);
				tieRewards = StringUtil.startAppend(1000, "");
				tieRewards.append("<tr>" + "	<td width=250 align=center>" + "		<br>" + "		<font name=\"hs12\" color=\"0099FF\">Top Rewards</font>" + "		<br>" + "	</td>" + "</tr>");
				for (RewardsTemplate rew : EventRewardManager.getInstance().getRewards(5))
				{
					String itemIcon = IconsTable.getInstance().getItemIcon(rew.getItemId());
					L2Item itemtmp = ItemTable.getInstance().getTemplate(rew.getItemId());
					String itemName = itemtmp.getName();
					String ammount = "x" + rew.getAmmount() + " ";
					String chance = "" + rew.getChance();
					lol = true;
					String bgcolor;
					if (lol)
					{
						lol = false;
						// color = "817679";
						bgcolor = "bgcolor=00080b";
					}
					else
					{
						lol = true;
						// color = "FFF8C6";
						bgcolor = "bgcolor=011118";
					}
					tieRewards.append("<tr>" + " <td>" + "	<table width=245 height=35 " + bgcolor + ">" + "		<tr>" + "			<td width=35>" + "				<img src=" + itemIcon + " width=32 height=32>" + "			</td>" + "			<td width=180>" + "				<font color=\"8AC0F6\">" + ammount + itemName + "</font>" + "			</td>" + "			<td width=180 align=\"center\">" + "				<font color=\"57799B\">" + chance + "%</font>" + "			</td>" + "		</tr>" + "		<tr>" + "			<td width=35>" + "			</td>" + "		</tr>" + "	</table>" + " </td>" + "</tr>");
				}
			}
		}
		for (RewardsTemplate rew : EventRewardManager.getInstance().getRewards(1))
		{
			String itemIcon = IconsTable.getInstance().getItemIcon(rew.getItemId());
			L2Item itemtmp = ItemTable.getInstance().getTemplate(rew.getItemId());
			String itemName = itemtmp.getName();
			String ammount = "x" + rew.getAmmount() + " ";
			String chance = "" + rew.getChance();
			String bgcolor;
			if (lol)
			{
				lol = false;
				// color = "817679";
				bgcolor = "bgcolor=00080b";
			}
			else
			{
				lol = true;
				// color = "FFF8C6";
				bgcolor = "bgcolor=011118";
			}
			winnerRewards.append("<tr>" + " <td>" + "	<table width=245 height=35 " + bgcolor + ">" + "		<tr>" + "			<td width=35>" + "				<img src=" + itemIcon + " width=32 height=32>" + "			</td>" + "			<td width=180>" + "				<font color=\"8AC0F6\">" + ammount + itemName + "</font>" + "			</td>" + "			<td width=180 align=\"center\">" + "				<font color=\"57799B\">" + chance + "%</font>" + "			</td>" + "		</tr>" + "		<tr>" + "			<td width=35>" + "			</td>" + "		</tr>" + "	</table>" + " </td>" + "</tr>");
		}
		for (RewardsTemplate rew : EventRewardManager.getInstance().getRewards(2))
		{
			String itemIcon = IconsTable.getInstance().getItemIcon(rew.getItemId());
			L2Item itemtmp = ItemTable.getInstance().getTemplate(rew.getItemId());
			String itemName = itemtmp.getName();
			String ammount = "x" + rew.getAmmount() + " ";
			String chance = "" + rew.getChance();
			lol = false;
			String bgcolor;
			if (lol)
			{
				lol = false;
				// color = "817679";
				bgcolor = "bgcolor=00080b";
			}
			else
			{
				lol = true;
				// color = "FFF8C6";
				bgcolor = "bgcolor=011118";
			}
			loserRewards.append("<tr>" + " <td>" + "	<table width=245 height=35 " + bgcolor + ">" + "		<tr>" + "			<td width=35>" + "				<img src=" + itemIcon + " width=32 height=32>" + "			</td>" + "			<td width=180>" + "				<font color=\"8AC0F6\">" + ammount + itemName + "</font>" + "			</td>" + "			<td width=180 align=\"center\">" + "				<font color=\"57799B\">" + chance + "%</font>" + "			</td>" + "		</tr>" + "		<tr>" + "			<td width=35>" + "			</td>" + "		</tr>" + "	</table>" + " </td>" + "</tr>");
		}
	}
	
	/**
	 * @see BaseBBSManager#parsewrite(String, String, String, String, String, String, L2PcInstance)
	 */
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventBBSManager _instance = new EventBBSManager();
	}
}