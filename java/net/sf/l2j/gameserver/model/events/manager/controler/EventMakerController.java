package net.sf.l2j.gameserver.model.events.manager.controler;

import java.util.ArrayList;
import java.util.HashMap;

import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.dataTables.FuturedEvent;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventScheduleTables;

public class EventMakerController implements ITimeTrigger
{
	private EventMakerController()
	{
		RealTimeController.registerHook(this);
	}
	
	@Override
	public void notify(int day, String trigger)
	{

		if(!EventEngine.getInstance().getEventEngineStatus())
			return;
		final HashMap<Integer, ArrayList<FuturedEvent>> scheduledEvents = EventScheduleTables.getInstance().getFuturedEvents();
		ArrayList<FuturedEvent> todayEvents = scheduledEvents.get(day);
		if (todayEvents == null)
			todayEvents = scheduledEvents.get(new Integer(0));
		if (todayEvents == null)
			return;
		if(EventEngine.getInstance().getEventActive())
		{
			//System.out.println("There is an active event atm so I don't check! bb");
			return;
		}
		for (FuturedEvent evt : todayEvents)
		{
			//System.out.println(evt.getEventId() + " - " + evt.getTime() + " Time now:" + trigger);
			if (evt.getTime().equals(trigger))
			{
				EventUtils.makeEvent(null, evt.getEventId());
			}
		}

		Communicator.getInstance().getTodayEvents();
	}
	
	private static final class InstanceHolder
	{
		private static final EventMakerController _instance = new EventMakerController();
	}
	
	public static EventMakerController getInstance()
	{
		return InstanceHolder._instance;
	}
}
