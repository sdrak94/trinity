package net.sf.l2j.gameserver.model.events;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import luna.custom.globalScheduler.GlobalScheduleTables;
import luna.custom.globalScheduler.template.FuturedGlobalEvent;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.dataTables.FuturedEvent;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventScheduleTables;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;

public class Communicator
{
	private String[]						_eventStats				=
	{
		"Event Name", "Phase", "Team 1 Score", "Team 2 Score", "Time Remaining", "Team 1 Name", "Team 2 Name"
	};
	private String							_currentEvent			= "N/A";
	private int								_regPlayers				= 0;
	private static final SimpleDateFormat	sdf						= new SimpleDateFormat("HH:mm");
	final ArrayList<FuturedEvent>			eventsList				= new ArrayList<FuturedEvent>();
	final ArrayList<FuturedEvent>			eventsListNextDay		= new ArrayList<FuturedEvent>();
	final ArrayList<FuturedGlobalEvent>			globalEventsList		= new ArrayList<FuturedGlobalEvent>();
	final ArrayList<FuturedGlobalEvent>			globalEventsListNextDay	= new ArrayList<FuturedGlobalEvent>();
	public String							_lastEvent				= "";
	public String							_nextEvent				= "";
	public String							_nextEventIcon			= "";
	public String[]							button					=
	{
		"Registration", "Name"
	};
	
	public void init()
	{
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			_currentEvent = e.getName();
		}
	}
	
	public void resetStats()
	{
		_regPlayers = 0;
		_eventStats[0] = "Event Name";
		_eventStats[1] = "Phase";
		_eventStats[2] = "Tean 1 Score";
		_eventStats[3] = "Team 2 Score";
		_eventStats[4] = "Time Remaining";
		_eventStats[5] = "Team 1 Name";
		_eventStats[6] = "Team 2 Name";
	}
	
	public void importPlayers()
	{
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			switch (e.getType())
			{
				case "HuntingGrounds":
					if (NewHuntingGrounds._joining || NewHuntingGrounds._started)
					{
						_regPlayers = NewHuntingGrounds._players.size();
					}
					break;
				case "Domination":
					if (NewDomination._joining || NewDomination._started)
					{
						_regPlayers = NewDomination._players.size();
					}
					break;
				case "TeamVsTeam":
					if (NewTvT._joining || NewTvT._started)
					{
						_regPlayers = NewTvT._players.size();
					}
					break;
				case "CTF":
					if (NewCTF._joining || NewCTF._started)
					{
						_regPlayers = NewCTF._players.size();
					}
					break;
				case "SiegeEvent":
					if (NewFOS._joining || NewFOS._started)
					{
						_regPlayers = NewFOS._players.size();
					}
					break;
				case "DM":
					if (NewDM._joining || NewDM._started)
					{
						_regPlayers = NewDM._players.size();
					}
					break;
			}
		}
	}
	
	public String[] generateButton(L2PcInstance p)
	{
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			switch (e.getType())
			{
				case "HuntingGrounds":
					if (NewHuntingGrounds._joining && !NewHuntingGrounds._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewHuntingGrounds._joining && NewHuntingGrounds._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "Domination":
					if (NewDomination._joining && !NewDomination._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewDomination._joining && NewDomination._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "TeamVsTeam":
					if (NewTvT._joining && !NewTvT._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewTvT._joining && NewTvT._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "CTF":
					if (NewCTF._joining && !NewCTF._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewCTF._joining && NewCTF._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "SiegeEvent":
					if (NewFOS._joining && !NewFOS._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewFOS._joining && NewFOS._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "DM":
					if (NewDM._joining && !NewDM._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewDM._joining && NewDM._players.contains(p))
					{
						button[0] = "bypass -h event_ujoin_";
						button[1] = "Remove Participation";
					}
					break;
			}
		}
		return button;
	}
	
	public String[] getEventStats()
	{
		return _eventStats;
	}
	
	public void setEventName(String eventName)
	{
		_eventStats[0] = eventName;
	}
	
	public void setEventPhase(String eventPhase)
	{
		_eventStats[1] = eventPhase;
	}
	
	public void setTeam1Score(String score)
	{
		_eventStats[2] = score;
	}
	
	public void setTeam2Score(String score)
	{
		_eventStats[3] = score;
	}
	
	public void setTimeRemaining(String timeRem)
	{
		_eventStats[4] = timeRem;
	}
	
	public String getEventName()
	{
		return _eventStats[0];
	}
	
	public String getEventPhase()
	{
		return _eventStats[1];
	}
	
	public String getTeam1Score()
	{
		return _eventStats[2];
	}
	
	public String getTeam2Score()
	{
		return _eventStats[3];
	}
	
	public String getTimeRemaining()
	{
		return _eventStats[4];
	}
	
	public String getCurrentEvent()
	{
		return _currentEvent;
	}
	
	public void setTeam1Name(String teamName)
	{
		_eventStats[5] = teamName;
	}
	
	public void setTeam2Name(String teamName)
	{
		_eventStats[6] = teamName;
	}
	
	public String getTeam1Name()
	{
		return _eventStats[5];
	}
	
	public String getTeam2Name()
	{
		return _eventStats[6];
	}
	
	public static Communicator getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public int getRegPlayers()
	{
		return _regPlayers;
	}
	
	public void setRegPlayers(int regPlayers)
	{
		_regPlayers = regPlayers;
	}
	
	public void setLastEvent(String lastEvent)
	{
		_lastEvent = lastEvent;
	}
	
	public String getLastEvent()
	{
		return _lastEvent;
	}
	
	public void setNextEvent(String nextEvent)
	{
		_nextEvent = nextEvent;
	}
	
	public String getNextEvent()
	{
		return _nextEvent;
	}
	
	public void setNextEventIcon(String nextEventIcon)
	{
		_nextEventIcon = nextEventIcon;
	}
	
	public String getNextEventIcon()
	{
		return _nextEventIcon;
	}
	
	public String generateIcon(String evtEventId)
	{
		String eventIcon;
		switch (evtEventId)
		{
			case "CTF":
				eventIcon = "icon.etc_alliance_flag_i00";
				break;
			case "DM":
				eventIcon = "icon.skill0444";
				break;
			case "TeamVsTeam":
				eventIcon = "icon.skill1584";
				break;
			case "SiegeEvent":
				eventIcon = "icon.weapon_fort_flag_i00";
				break;
			case "Domination":
				eventIcon = "icon.skill30028";
				break;
			case "HuntingGrounds":
				eventIcon = "icon.skill10757";
				break;
			default:
				eventIcon = "icon.skill30027";
				break;
		}
		return eventIcon;
	}
	
	public void getTodayEvents()
	{
		eventsList.clear();
		final HashMap<Integer, ArrayList<FuturedEvent>> scheduledEvents = EventScheduleTables.getInstance().getFuturedEvents();
		ArrayList<FuturedEvent> todayEvents = scheduledEvents.get(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
		if (todayEvents == null)
			todayEvents = scheduledEvents.get(0);
		for (FuturedEvent evt : todayEvents)
		{
			final Date date = new Date(System.currentTimeMillis());
			final String timeStr = sdf.format(date);
			LocalTime start = LocalTime.parse(timeStr);
			LocalTime stop = LocalTime.parse(evt.getTime());
			Duration duration = Duration.between(start, stop);
			// System.out.println(duration.toMinutes());
			if (duration.toMinutes() >= 0)
			{
				eventsList.add(evt);
			}
		}
		if (eventsList.size() < 10)
		{
			for (FuturedEvent evt2 : getTommorowEvents(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), 10 - eventsList.size()))
			{
				eventsList.add(evt2);
			}
		}
	}
	
	public ArrayList<FuturedEvent> getTommorowEvents(int dayOfWeek, int howMany)
	{
		boolean run = true;
		int curDay = dayOfWeek;
		int nextDay = 0;
		if (curDay == 7)
		{
			nextDay = 1;
		}
		else
		{
			nextDay = curDay + 1;
		}
		int count = 0;
		eventsListNextDay.clear();
		while (run)
		{
			final HashMap<Integer, ArrayList<FuturedEvent>> scheduledEvents = EventScheduleTables.getInstance().getFuturedEvents();
			ArrayList<FuturedEvent> todayEvents = scheduledEvents.get(nextDay);
			if (todayEvents == null)
			{
				todayEvents = scheduledEvents.get(0);
			}
			if (todayEvents == null)
			{
				nextDay++;
				continue;
			}
			for (FuturedEvent evt : todayEvents)
			{
				if (count >= howMany)
					break;
				count++;
				eventsListNextDay.add(evt);
			}
			run = false;
			break;
		}
		return eventsListNextDay;
	}
	
	public String getTopPlayers()
	{
		String text = "";
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			final int countTopPlayers = 10;
			switch (e.getType())
			{
				case "HuntingGrounds":
					if (!NewHuntingGrounds._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewHuntingGrounds.getTopPlayers();
						Collections.sort(tempPlayers, NewHuntingGrounds.comparePlayersScore);
						int i = 0;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							switch (player._teamNameHG)
							{
								case "Red":
									TeamC = "851818";
									break;
								case "Green":
									TeamC = "36B234";
									break;
								case "Blue":
									TeamC = "3475B2";
									break;
								case "Orange":
									TeamC = "E59B22";
									break;
								case "Purple":
									TeamC = "6322E5";
									break;
								case "Yellow":
									TeamC = "LEVEL";
									break;
								default:
									TeamC = "3475B2";
									break;
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=83 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">Kills: " + player._countHGkills + " | Deaths: " + player._countHGdies + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
				case "Domination":
					if (!NewDomination._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewDomination.getTopPlayers();
						Collections.sort(tempPlayers, NewDomination.comparePlayersScore);
						int i = 0;
						String kd;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							switch (player._teamNameLunaDomi)
							{
								case "Red":
									TeamC = "851818";
									break;
								case "Green":
									TeamC = "36B234";
									break;
								case "Blue":
									TeamC = "3475B2";
									break;
								case "Orange":
									TeamC = "E59B22";
									break;
								case "Purple":
									TeamC = "6322E5";
									break;
								case "Yellow":
									TeamC = "LEVEL";
									break;
								default:
									TeamC = "3475B2";
									break;
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=83 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">Kills: " + player._countLunaDomiKills + " | Deaths: " + player._countLunaDomiDies + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
				case "TeamVsTeam":
					if (!NewTvT._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewTvT.getTopPlayers();
						Collections.sort(tempPlayers, NewTvT.comparePlayersScore);
						int i = 0;
						String kd;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							switch (player._teamNameTvT)
							{
								case "Red":
									TeamC = "851818";
									break;
								case "Green":
									TeamC = "36B234";
									break;
								case "Blue":
									TeamC = "3475B2";
									break;
								case "Orange":
									TeamC = "E59B22";
									break;
								case "Purple":
									TeamC = "6322E5";
									break;
								case "Yellow":
									TeamC = "LEVEL";
									break;
								default:
									TeamC = "3475B2";
									break;
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=50 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=180 align=\"center\">" + "					<font color=" + TeamC + ">Kills: " + player._countTvTkills + " | Deaths: " + player._countTvTdies + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
				case "CTF":
					if (!NewCTF._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewCTF.getTopPlayers();
						Collections.sort(tempPlayers, NewCTF.comparePlayersScore);
						int i = 0;
						String kd;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							switch (player._teamNameCTF)
							{
								case "Red":
									TeamC = "851818";
									break;
								case "Green":
									TeamC = "36B234";
									break;
								case "Blue":
									TeamC = "3475B2";
									break;
								case "Orange":
									TeamC = "E59B22";
									break;
								case "Purple":
									TeamC = "6322E5";
									break;
								case "Yellow":
									TeamC = "LEVEL";
									break;
								default:
									TeamC = "3475B2";
									break;
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=83 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">Kills: " + player._countCTFkills + " | Deaths: " + player._countCTFkills + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
				case "SiegeEvent":
					if (!NewFOS._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewFOS.getTopPlayers();
						Collections.sort(tempPlayers, NewFOS.comparePlayersScore);
						int i = 0;
						String kd;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							switch (player._teamNameFOS)
							{
								case "Red":
									TeamC = "851818";
									break;
								case "Green":
									TeamC = "36B234";
									break;
								case "Blue":
									TeamC = "3475B2";
									break;
								case "Orange":
									TeamC = "E59B22";
									break;
								case "Purple":
									TeamC = "6322E5";
									break;
								case "Yellow":
									TeamC = "LEVEL";
									break;
								default:
									TeamC = "3475B2";
									break;
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=83 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">Kills: " + player._countFOSKills + " | Deaths: " + player._countFOSdies + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
				case "DM":
					if (!NewDM._started)
					{
						text = "";
					}
					else
					{
						List<L2PcInstance> tempPlayers = NewDM.getTopPlayers();
						Collections.sort(tempPlayers, NewDM.comparePlayersScore);
						int i = 0;
						String kd;
						text = "";
						for (L2PcInstance player : tempPlayers)
						{
							String position = "";
							String TeamC = "";
							if (player._DMPos == 1)
							{
								TeamC = "E59B22";
							}
							else if (player._DMPos > 1 && player._DMPos <= 10)
							{
								TeamC = "6322E5";
							}
							else
							{
								TeamC = "851818";
							}
							if (i + 1 == 1)
							{
								position = i + 1 + "st";
							}
							else if (i + 1 == 2)
							{
								position = i + 1 + "nd";
							}
							else if (i + 1 == 3)
							{
								position = i + 1 + "rd";
							}
							else
								position = i + 1 + "th";
							text += "<tr>" + "	<td>" + "		<table width=475 height=20 bgcolor=00080b>" + "			<tr>" + "				<td width=83 align=\"left\">" + "					<font color=" + TeamC + ">" + position + ".</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + ellipsis(player.getName(), player.getName().length()) + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">" + player.getClanName() + "</font>" + "				</td>" + "				<td width=220 align=\"left\">" + "					<font color=" + TeamC + ">Kills: " + player._countDMkills + " | Deaths: " + player._countDMkills + "</font>" + "				</td>" + "			</tr>" + "		</table>" + "	</td>" + "</tr>";
							i++;
							if (i >= countTopPlayers)
								break;
						}
						return text;
					}
					break;
			}
		}
		return text;
	}
	
	public static String ellipsis(final String text, int length)
	{
		if (text.length() >= 13)
		{
			return text.substring(0, 8) + "...";
		}
		else
			return text;
	}
	
	public String getEventDescription()
	{
		String description = "N/A";
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			switch (e.getType())
			{
				case "HuntingGrounds":
					description = "2 teams fighting against each other. Gain score by killing your opponents at least  1 kill(s) is required to receive a reward";
					break;
				case "TeamVsTeam":
					description = "2 teams fighting against each other. Gain score by killing your opponents at least  1 kill(s) is required to receive a reward";
					break;
				case "Domination":
					description = "2 teams fighting against each other. The goal of this event is to capture and hold a zone. The zone is represented by an NPC and to capture it, you need to stand near the NPC and ensure that no other enemies are standing near the zone too.";
					break;
				case "CTF":
					description = "There are 2 teams in order to score you need  to steal enemy team's flag and bring itback your team's base (to the flag holder). If you hold the flag and don't manage to score within 70 seconds, the flag will be returned back to enemy's flag holder.";
					break;
				case "SiegeEvent":
					description = "There are 2 teams. Protect the Castle from the Invaders and don't let them engrave the Seal of Ruler or Castle Throne will be theirs.Only Siege Leaders are able to cast the Spell. Team which will possess the Castle will win the Event.";
					break;
				case "DM":
					description = "This is a free-for-all event, don't expect any help from teammates. Gain score by killing your opponents";
					break;
			}
		}
		return description;
	}

	
	public void getTodayGlobalEvents()
	{
		globalEventsList.clear();
		final HashMap<Integer, ArrayList<FuturedGlobalEvent>> scheduledEvents = GlobalScheduleTables.getInstance().getFuturedGlobalEvents();
		ArrayList<FuturedGlobalEvent> todayEvents = scheduledEvents.get(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
		if (todayEvents == null)
			todayEvents = scheduledEvents.get(new Integer(0));
		for (FuturedGlobalEvent evt : todayEvents)
		{
			final Date date = new Date(System.currentTimeMillis());
			final String timeStr = sdf.format(date);
			LocalTime start = LocalTime.parse(timeStr);
			LocalTime stop = LocalTime.parse(evt.getTime());
			Duration duration = Duration.between(start, stop);
			// System.out.println(duration.toMinutes());
			if (duration.toMinutes() >= 0)
			{
				globalEventsList.add(evt);
			}
		}
		if (globalEventsList.size() < 10)
		{
			for (FuturedGlobalEvent evt2 : getTommorowGlobalEvents(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), 10 - globalEventsList.size()))
			{
				globalEventsList.add(evt2);
			}
		}
	}
	
	public ArrayList<FuturedGlobalEvent> getTommorowGlobalEvents(int dayOfWeek, int howMany)
	{
		boolean run = true;
		int curDay = dayOfWeek;
		int nextDay = 0;
		if (curDay == 7)
		{
			nextDay = 1;
		}
		else
		{
			nextDay = curDay + 1;
		}
		int count = 0;
		globalEventsListNextDay.clear();
		while (run)
		{
			final HashMap<Integer, ArrayList<FuturedGlobalEvent>> scheduledEvents = GlobalScheduleTables.getInstance().getFuturedGlobalEvents();
			ArrayList<FuturedGlobalEvent> todayGlobalEvents = scheduledEvents.get(nextDay);
			if (todayGlobalEvents == null)
			{
				todayGlobalEvents = scheduledEvents.get(new Integer(0));
			}
			if (todayGlobalEvents == null)
			{
				nextDay++;
				continue;
			}
			for (FuturedGlobalEvent evt : todayGlobalEvents)
			{
				if (count >= howMany)
					break;
				count++;
				globalEventsListNextDay.add(evt);
			}
			run = false;
			break;
		}
		return globalEventsListNextDay;
	}
	

	public ArrayList<FuturedEvent> getEventList()
	{
		return eventsList;
	}

	public ArrayList<FuturedGlobalEvent> getGlobalEventList()
	{
		return globalEventsList;
	}
	private static class SingletonHolder
	{
		protected static final Communicator _instance = new Communicator();
	}
}
