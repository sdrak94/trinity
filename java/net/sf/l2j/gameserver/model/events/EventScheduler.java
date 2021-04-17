package net.sf.l2j.gameserver.model.events;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.util.Rnd;

public class EventScheduler
{
	protected static final Logger	_log			= Logger.getLogger(EventScheduler.class.getName());
	private static final long		INITIAL_DELAY	= 1800 * 1000;
	private static final long		STANDARD_DELAY	= 7200 * 1000;
	private static final long		REDUCED_DELAY	= 7200 * 1000;
	private boolean					EngineStatus	= false;
	protected ScheduledFuture<?>	_scheduleEventStart;
	Event							eventToBeStarted;
	
	enum Event
	{
		TVT,
		CTF,
		DM,
		SIEGE
	}
	
	private EventScheduler()
	{
		_log.info("Initializing Event Scheduler...");
		EngineStatus = false;
		init();
	}
	
	public static EventScheduler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventScheduler _instance = new EventScheduler();
	}
	
	private void init()
	{
		eventToBeStarted = null;
		StartRandomEvent(INITIAL_DELAY);
	}
	
	private void StartRandomEvent(long delay)
	{
		if (!EngineStatus)
			return;
		int event = Rnd.get(10);
		switch (event)
		{
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				eventToBeStarted = Event.TVT;
				break;
			case 5:
			case 6:
				eventToBeStarted = Event.CTF;
				break;
			case 7:
			case 8:
				eventToBeStarted = Event.SIEGE;
				break;
			case 9:
				eventToBeStarted = Event.DM;
				break;
			default:
				return;
		}
		if (eventToBeStarted == Event.SIEGE)
		{
			int pplOnline = L2World.getInstance().getAllPlayersCount();
			if (pplOnline < 150)
			{
				int roll = Rnd.get(3);
				switch (roll)
				{
					case 0:
						eventToBeStarted = Event.TVT;
						break;
					case 1:
						eventToBeStarted = Event.CTF;
						break;
					case 2:
						eventToBeStarted = Event.DM;
						break;
				}
			}
		}
		switch (eventToBeStarted)
		{
			case TVT:
				_log.info("TvT Event has been set to run in 30 minutes");
				if (_scheduleEventStart != null)
					_scheduleEventStart.cancel(true);
				Announcements.getInstance().announceToAll("TvT Event scheduled to start in 30 minutes!");
				_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
				break;
			case CTF:
				_log.info("CTF Event has been set to run in 30 minutes");
				if (_scheduleEventStart != null)
					_scheduleEventStart.cancel(true);
				Announcements.getInstance().announceToAll("CTF Event scheduled to start in 30 minutes!");
				_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
				break;
			case DM:
				_log.info("DM Event has been set to run in 30 minutes");
				if (_scheduleEventStart != null)
					_scheduleEventStart.cancel(true);
				Announcements.getInstance().announceToAll("DM Event scheduled to start in 30 minutes!");
				_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
				break;
			case SIEGE:
				_log.info("SIEGE Event has been set to run in 30 minutes");
				if (_scheduleEventStart != null)
					_scheduleEventStart.cancel(true);
				Announcements.getInstance().announceToAll("Siege Event scheduled to start in 30 minutes!");
				_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
				break;
			default:
				return;
		}
	}
	
	public Event getEventToBeStarted()
	{
		return eventToBeStarted;
	}

	public void setEventToBeStarted(Event eventToBeStarted)
	{
		this.eventToBeStarted = eventToBeStarted;
	}

	public void startNextEvent()
	{
		if (!EngineStatus)
			return;
		long delay = STANDARD_DELAY;
		long red_delay = REDUCED_DELAY;
		int event = Rnd.get(10);
		switch (event)
		{
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				eventToBeStarted = Event.TVT;
				break;
			case 5:
			case 6:
			case 7:
				eventToBeStarted = Event.CTF;
				break;
			case 8:
				eventToBeStarted = Event.SIEGE;
				break;
			case 9:
				eventToBeStarted = Event.DM;
				break;
			default:
				return;
		}
		if (eventToBeStarted == Event.SIEGE)
		{
			int pplOnline = L2World.getInstance().getAllPlayersCount();
			if (pplOnline < 150)
			{
				int roll = Rnd.get(3);
				switch (roll)
				{
					case 0:
						eventToBeStarted = Event.TVT;
						break;
					case 1:
						eventToBeStarted = Event.CTF;
						break;
					case 2:
						eventToBeStarted = Event.DM;
						break;
				}
			}
		}
		boolean weekend = false;
		Calendar cal = Calendar.getInstance();
		switch (cal.get(Calendar.DAY_OF_WEEK))
		{
			case Calendar.SATURDAY:
			case Calendar.SUNDAY:
				weekend = false;
				break;
			default:
				weekend = false;
				break;
		}
		if (weekend)
		{
			switch (eventToBeStarted)
			{
				case TVT:
					_log.info("TvT Event has been set to run in 45 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("TvT Event scheduled to start in 45 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), red_delay);
					break;
				case CTF:
					_log.info("CTF Event has been set to run in 45 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("CTF Event scheduled to start in 45 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), red_delay);
					break;
				case DM:
					_log.info("DM Event has been set to run in 45 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("DM Event scheduled to start in 45 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), red_delay);
					break;
				case SIEGE:
					_log.info("SIEGE Event has been set to run in 45 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("Siege Event scheduled to start in 45 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), red_delay);
					break;
				default:
					return;
			}
		}
		else
		{
			switch (eventToBeStarted)
			{
				case TVT:
					_log.info("TvT Event has been set to run in 120 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("TvT Event scheduled to start in 120 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
					break;
				case CTF:
					_log.info("CTF Event has been set to run in 120 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("CTF Event scheduled to start in 120 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
					break;
				case DM:
					_log.info("DM Event has been set to run in 120 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("DM Event scheduled to start in 120 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
					break;
				case SIEGE:
					_log.info("SIEGE Event has been set to run in 120 minutes");
					if (_scheduleEventStart != null)
						_scheduleEventStart.cancel(true);
					Announcements.getInstance().announceToAll("Siege Event scheduled to start in 120 minutes!");
					_scheduleEventStart = ThreadPoolManager.getInstance().scheduleGeneral(new EventStartTask(), delay);
					break;
				default:
					return;
			}
		}
	}
	
	protected class EventStartTask implements Runnable
	{
		public void run()
		{
			try
			{
				switch (eventToBeStarted)
				{
					case TVT:
						int tvtChosen = Rnd.get(5);
						TvT.loadData(tvtChosen);
						TvT.autoEvent();
						break;
					case CTF:
						int ctfChosen = Rnd.get(4);
						CTF.loadData(ctfChosen);
						CTF.autoEvent();
						break;
					case DM:
						int dmChosen = Rnd.get(2);
						DM.loadData(dmChosen);
						DM.autoEvent();
						break;
					case SIEGE:
						int fosChosen = Rnd.get(3);
						FOS.loadData(fosChosen);
						FOS.autoEvent();
						break;
					default:
						return;
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void closeEventEngine()
	{
		if (_scheduleEventStart != null)
			_scheduleEventStart.cancel(true);
		EngineStatus = false;
	}
	
	public void startEventEngine()
	{
		if (_scheduleEventStart != null)
			_scheduleEventStart.cancel(true);
		EngineStatus = true;
		StartRandomEvent(INITIAL_DELAY);
	}
	
	public boolean getEventEngineStatus()
	{
		return EngineStatus;
	}
	
	public String getEventCb()
	{
		return "";
	}
}
