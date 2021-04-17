package net.sf.l2j.gameserver.model.events.manager;

import java.util.Map;

import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.util.Rnd;

public class EventCommander
{
	int										_eventId;
	static String[]							nummeric	=
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	public void selectEvent(String event)
	{
		if (containsNumeric(event))
		{
			_eventId = EventsParser.getInstance().getEvents().get(Integer.parseInt(event)).getId();
		}
		else
		{
			Map<Integer, Event> collection = EventsCollector.getInstance().getEventCollection(event);
			int size = collection.size();
			int rng = Rnd.get(0, size-1);
			_eventId = collection.get(rng).getId();
			// System.out.println("Selected Event:" + collection.get(rng).getName());
		}
	}
	

	
	public boolean containsNumeric(String eventId)
	{
		boolean found = false;
		for (int i = 0; i < nummeric.length; i++)
		{
			if (!eventId.contains(nummeric[i]))
				continue;
			else
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	public Event selectEventById(int id)
	{
		return EventsParser.getInstance().getEvents().get(id);
	}
	
	public int getEventId()
	{
		return _eventId;
	}
	
	public static EventCommander getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventCommander _instance = new EventCommander();
	}
}
