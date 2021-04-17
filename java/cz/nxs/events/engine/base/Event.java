package cz.nxs.events.engine.base;

import cz.nxs.events.NexusLoader;
import cz.nxs.l2j.CallBack;

/**
 * @author hNoke
 * a mother class of all Nexus events
 */
public abstract class Event
{
	protected EventType _type;
	
	public Event(EventType type)
	{
		_type = type;
	}
	
	public final EventType getEventType()
	{
		return _type;
	}
	
	public String getEventName()
	{
		return _type.getAltTitle();
	}
	
	public void announce(String text)
	{
		CallBack.getInstance().getOut().announceToAllScreenMessage(text, getEventType().getAltTitle());
	}
	
	// utilities
	public void debug(String text)
	{
		NexusLoader.debug(text);
	}
	
	/** detailed debug */
	public void print(String msg)
	{
		NexusLoader.detailedDebug(msg);
	}
}
