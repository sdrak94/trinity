package luna.custom.eventengine.managers;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import luna.custom.eventengine.LunaEvent;
import luna.custom.eventengine.events.LunaTvT;
import net.sf.l2j.gameserver.ThreadPoolManager;
	
public class EventManager
{
	private LunaEvent _activeEvent = null;
	protected static final Logger _log = Logger.getLogger(EventManager.class.getName());
	public static EventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	private static class SingletonHolder
	{
		private static final EventManager INSTANCE = new EventManager();
	}
	
	private EventManager()
	{
		
	}
	public void setActiveEvent(LunaEvent le)
	{
		_activeEvent = le;
	}
	public LunaEvent getActiveEvent()
	{
		return _activeEvent;
	}
	public void scheduleNextEvent()
	{
		ThreadPoolManager.getInstance().scheduleGeneral( () -> {
			if (getActiveEvent()==null)
				scheduleProperEvent();
			scheduleNextEvent();
		}, TimeUnit.MINUTES.toMillis(1));
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 1);
		_log.info("Next event will run at " + calendar.getTime().toString());
	}
	private void scheduleProperEvent()
	{
		//todo more
		ThreadPoolManager.getInstance().executeTask( () -> LunaTvT.getInstance().startEvent());
	}
}