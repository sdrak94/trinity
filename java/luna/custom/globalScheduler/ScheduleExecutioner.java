package luna.custom.globalScheduler;

import java.util.ArrayList;
import java.util.HashMap;

import luna.custom.globalScheduler.template.FuturedGlobalEvent;
import net.sf.l2j.gameserver.model.events.Communicator;

public class ScheduleExecutioner implements ITimeTrigger
{
	private ScheduleExecutioner()
	{
		RealTimeController.registerHook(this);
	}
	
	@Override
	public void notify(int day, String trigger)
	{
		final HashMap<Integer, ArrayList<FuturedGlobalEvent>> scheduledEvents = GlobalScheduleTables.getInstance().getFuturedGlobalEvents();
		ArrayList<FuturedGlobalEvent> todayEvents = scheduledEvents.get(day);
		if (todayEvents == null)
			todayEvents = scheduledEvents.get(new Integer(0));
		if (todayEvents == null)
			return;
		for (FuturedGlobalEvent evt : todayEvents)
		{
			//System.out.println(evt.getEventId() + " - " + evt.getTime() + " Time now:" + trigger);
			if (evt.getTime().equals(trigger))
			{
				GlobalEventUtils.makeEvent(null, evt.getEventType());
			}
		}

		Communicator.getInstance().getTodayGlobalEvents();
	}
	
	private static final class InstanceHolder
	{
		private static final ScheduleExecutioner _instance = new ScheduleExecutioner();
	}
	
	public static ScheduleExecutioner getInstance()
	{
		return InstanceHolder._instance;
	}

	@Override
	public void notify(String dayName, String timeString)
	{
		// TODO Auto-generated method stub
		
	}
}
