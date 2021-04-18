package net.sf.l2j.gameserver.model.events.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.gameserver.model.events.dataTables.FuturedEvent;
import net.sf.l2j.util.Rnd;

public class EventScheduleTables
{
	private HashMap<Integer, ArrayList<FuturedEvent>>	futuredEvents	= new HashMap<>();
	List<String>										eventsPool		= new ArrayList<String>();
	
	public void loadNode(final Node n) throws Exception
	{
		futuredEvents.clear();
		eventsPool.clear();
		
		for (Node n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
		{
			if ("eventsPool".equalsIgnoreCase(n2.getNodeName()))
			{
				for (Node n3 = n2.getFirstChild(); n3 != null; n3 = n3.getNextSibling())
				{
					if ("pool".equalsIgnoreCase(n3.getNodeName()))
					{
						final NamedNodeMap nnm = n3.getAttributes();
						final String bucoPool = nnm.getNamedItem("bucoPool").getNodeValue();
						StringTokenizer tokenizer = new StringTokenizer(bucoPool, ",");
						while (tokenizer.hasMoreTokens())
						{
							
							String eventWs = tokenizer.nextToken();
							if(eventWs.equalsIgnoreCase(" "))
								continue;
							addPool(eventWs);
						}
						
					}
				}
			}
			if (!"day".equalsIgnoreCase(n2.getNodeName()))
				continue;
			final Node dayNode = n2.getAttributes().getNamedItem("day");
			final Integer targetDay = dayNode == null ? new Integer(0) : Integer.valueOf(dayNode.getNodeValue());
			final int day = Integer.valueOf(dayNode.getNodeValue());
			for (Node n3 = n2.getFirstChild(); n3 != null; n3 = n3.getNextSibling())
			{
				if ("event".equalsIgnoreCase(n3.getNodeName()))
				{
					final NamedNodeMap nnm = n3.getAttributes();
					final String eventId = nnm.getNamedItem("eventId").getNodeValue();
					final String time = nnm.getNamedItem("time").getNodeValue();
					
					addEvent(targetDay, new FuturedEvent(eventId, time, day));
				}
			}
		}
		scrableEvents();
	}
	
	public void report()
	{
		int size = 0;
		for (ArrayList<?> list : futuredEvents.values())
			size += list.size();
		System.out.println("EventMaker: Loaded " + size + " event timers!");
	}
	
	public void scrableEvents()
	{
//		String[] events =
//		{
//			"Domination", "SiegeEvent", "CTF", "TeamVsTeam", "DM", "HuntingGrounds", "TeamVsTeam", "TeamVsTeam", "TeamVsTeam",
//		};
		ArrayList<Integer> usedEvents = new ArrayList<Integer>();
		int count = 1;
		int countDM = 0;
		//System.out.println("------------");
		for (int i = 1; i <= 7; i++)
		{
			if(futuredEvents.get(i) == null)
			{
				continue;
			}
			for (FuturedEvent evt : futuredEvents.get(i))
			{
				if (evt == null)
					continue;
				if (evt.getEventId().equalsIgnoreCase("random"))
				{
					boolean found = false;
					int rndPicked = Rnd.get(0, eventsPool.size());
					while (!found)
					{
						rndPicked = Rnd.get(0, eventsPool.size() - 1);
						if (!usedEvents.contains(rndPicked))
						{
							evt.setEventId(eventsPool.get(rndPicked));
							usedEvents.add(rndPicked);
							found = true;
							count++;
						}
						if (count > 3 && usedEvents.size() >= 3)
						{
							usedEvents.remove(0);
						}
					}
				}
				else
					continue;
				//System.out.println("Day:" + i + " Event: " + evt.getEventId() + " - \t \t \t \t \t " + evt.getTime());
			}
			//System.out.println("------------");
			usedEvents.clear();
		}
	}
	
	public HashMap<Integer, ArrayList<FuturedEvent>> getFuturedEvents()
	{
		return futuredEvents;
	}
	
	private void addEvent(final int targetDay, final FuturedEvent schEvent)
	{
		ArrayList<FuturedEvent> events = futuredEvents.get(targetDay);
		if (events == null)
		{
			events = new ArrayList<>();
			futuredEvents.put(targetDay, events);
		}
		events.add(schEvent);
	}
	
	private void addPool(String pool)
	{
		eventsPool.add(pool);
	}
	
	protected void reload()
	{
		futuredEvents.clear();
	}
	
	private static final class InstanceHolder
	{
		private static final EventScheduleTables _instance = new EventScheduleTables();
	}
	
	public static EventScheduleTables getInstance()
	{
		return InstanceHolder._instance;
	}
}
