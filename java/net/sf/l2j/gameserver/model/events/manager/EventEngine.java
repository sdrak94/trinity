package net.sf.l2j.gameserver.model.events.manager;

import luna.custom.globalScheduler.GlobalEventsParser;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.manager.controler.DelaysController;
import net.sf.l2j.gameserver.model.events.manager.controler.EventMakerController;
import net.sf.l2j.gameserver.model.events.manager.controler.RealTimeController;
import net.sf.l2j.gameserver.model.events.newEvents.playerStatistics.EventStats;

public class EventEngine
{
	public boolean	_status = true;
	public boolean	_isEventActive = false;
	public Event	_activeEvent;
	
	public static void load()
	{
		try
		{
			// load XMLs
			EventsParser.getInstance().Reload(null);
			GlobalEventsParser.getInstance().reload();
			
			System.out.println("   -EventParser initialized.");
			// load controllers
			RealTimeController.load();
			System.out.println("   -RealTimeController initialized.");
			EventMakerController.getInstance();
			System.out.println("   -EventMakerController initialized.");
			DelaysController.getInstance();
			System.out.println("   -DelaysController initialized.");
			System.out.println("Loaded Event Engine.");
			Communicator.getInstance().getTodayEvents();
			EventStats.getInstance().generateTopStats();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setEventEngineStatus(Boolean status)
	{
		_status = status;
	}
	
	public boolean getEventEngineStatus()
	{
		return _status;
	}
	
	public void setEventActive(Boolean eventActive)
	{
		_isEventActive = eventActive;
	}
	
	public boolean getEventActive()
	{
		return _isEventActive;
	}
	
	public void setActiveEvent(Event e)
	{
		_activeEvent = e;
	}
	
	public Event getActiveEvent()
	{
		return _activeEvent;
	}
	
	public void terminateEvent()
	{
		_activeEvent = null;
		_isEventActive = false;
	}
	public static class InstanceHolder
	{
		private static final EventEngine _instance = new EventEngine();
	}
	
	public static EventEngine getInstance()
	{
		return InstanceHolder._instance;
	}
}
