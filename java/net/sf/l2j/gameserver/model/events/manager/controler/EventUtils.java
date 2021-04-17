package net.sf.l2j.gameserver.model.events.manager.controler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.manager.EventCommander;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.util.Rnd;

public class EventUtils
{
	static String[] nummeric =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	public static String prepareString(String msg, L2PcInstance playerDriver)
	{
		return msg;
		// return msg.replaceAll("\\$player", String.valueOf(playerDriver.getPlayerName()))
		// .replaceAll("\\$kills", String.valueOf(playerDriver.getEventKills()))
		// .replaceAll("\\$flags", String.valueOf(playerDriver.getEventFlags()))
		// .replaceAll("\\$deaths", String.valueOf(playerDriver.getEventDeaths()))
		// .replaceAll("\\$top", formatPosition(playerDriver.getEventPosition()));
	}
	
	public static String getRandomEvent(String eventId, Boolean id)
	{
		List<Integer> eventBag = new FastList<Integer>();
		List<String> sEventBag = new FastList<String>();
		eventBag.clear();
		sEventBag.clear();
		String buc = "";
		if (id)
		{
			String eventList = eventId;
			String[] values = eventList.split("-");
			for (int i = 0; i < values.length; i++)
			{
				eventBag.add(Integer.parseInt(values[i]));
				System.out.println("Event ID: " + values[i]);
			}
		}
		else
		{
			String eventList = eventId;
			String[] values = eventList.split("-");
			for (int i = 0; i < values.length; i++)
			{
				sEventBag.add(values[i]);
				System.out.println("Event ID: " + values[i]);
			}
		}
		if (eventBag.size() != 0 && sEventBag.size() == 0)
		{
			buc = String.valueOf(Rnd.get(0, eventBag.size()));
		}
		else if (eventBag.size() == 0 && sEventBag.size() != 0)
		{
			buc = sEventBag.get(Rnd.get(0, sEventBag.size()));
		}
		return buc;
	}
	
	public static <T> boolean contains(final T[] array, final T element)
	{
		if (array == null || element == null)
			return false;
		for (T t : array)
			if (t == element)
				return true;
		return false;
	}
	
	private static boolean containsNumeric(String eventId)
	{
		boolean found = false;
		for (int i = 0; i < nummeric.length; i++)
			if (!eventId.contains(nummeric[i]))
			{
				continue;
			}
			else
			{
				found = true;
				break;
			}
		return found;
	}
	
	public static boolean makeEvent(final L2PcInstance caller, final String eventId)
	{
		try
		{
			if (caller != null)
			{
				
				if(containsNumeric(eventId))
				{
					int selEvId = Integer.parseInt(eventId);
					EventCommander.getInstance().selectEventById(selEvId);
					EventVarHolder.getInstance().setRunningEventId(selEvId);
					runEventById(selEvId);
				}
				else
				{
					//eventType = getRandomEvent(eventId, false);
					EventCommander.getInstance().selectEvent(eventId);
					EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
					runEventById(EventVarHolder.getInstance().getRunningEventId());
				}
			}
			else if (containsNumeric(eventId))
			{
				if (eventId.contains("-"))
				{
					int selEvId = Integer.parseInt(getRandomEvent(eventId, true));
					EventCommander.getInstance().selectEventById(selEvId);
					EventVarHolder.getInstance().setRunningEventId(selEvId);
					runEventById(selEvId);
				}
				else
				{
					EventCommander.getInstance().selectEventById(Integer.parseInt(eventId));
					EventVarHolder.getInstance().setRunningEventId(Integer.parseInt(eventId));
					runEventById(Integer.parseInt(eventId));
				}
			}
			else if (!containsNumeric(eventId))
			{
				if (eventId.contains("-"))
				{
					String eventType;
					eventType = getRandomEvent(eventId, false);
					EventCommander.getInstance().selectEvent(eventType);
					EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
					runEventById(EventVarHolder.getInstance().getRunningEventId());
				}
				else
				{
					EventCommander.getInstance().selectEvent(eventId);
					EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
					runEventById(EventVarHolder.getInstance().getRunningEventId());
				}
			}
			
			//System.out.println("Schedule Event here.");
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static void runEventById(int id)
	{
		if(EventEngine.getInstance().getEventActive())
		{
			return;
		}
		Event e = EventsParser.getInstance().getEvents().get(id);
		String eventType = e.getType();
		if(e != null)
		{
			EventEngine.getInstance().setActiveEvent(e);
			EventEngine.getInstance().setEventActive(true);
		}
		switch (eventType)
		{
			case "Domination":
				NewDomination.loadData(e.getId());
				NewDomination.autoEvent(e.getId());
				break;
			case "SiegeEvent":
				NewFOS.loadData(e.getId());
				NewFOS.autoEvent();
				break;
			case "TeamVsTeam":
				NewTvT.loadData(e.getId());
				NewTvT.autoEvent();
				break;
			case "CTF":
				NewCTF.loadData(e.getId());
				NewCTF.autoEvent();
				break;
			case "DM":
				NewDM.loadData(e.getId());
				NewDM.autoEvent();
				break;
			case "HuntingGrounds":
				NewHuntingGrounds.loadData(e.getId());
				NewHuntingGrounds.autoEvent();
				break;
		}
	}
	
	public static String formatPosition(int place)
	{
		switch (place)
		{
			case 1:
				return "1st";
			case 2:
				return "2nd";
			case 3:
				return "3rd";
			default:
				return place + "th";
		}
	}
	
	public static long getTimePassed(final long t0, TimeUnit time)
	{
		final long t1 = System.currentTimeMillis();
		if (t0 > t1)
			return 0;
		return time.convert(t1 - t0, TimeUnit.MILLISECONDS);
	}
}
