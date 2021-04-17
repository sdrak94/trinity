/**
 * 
 */
package cz.nxs.events.engine.base.description;

import java.util.Map;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.EventType;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class EventDescriptionSystem
{
	private Map<EventType, EventDescription> _descriptions;
	
	public EventDescriptionSystem()
	{
		_descriptions = new FastMap<EventType, EventDescription>();
		NexusLoader.debug("Loaded editable Event Description system.", Level.INFO);
	}
	
	public void addDescription(EventType type, EventDescription description)
	{
		_descriptions.put(type, description);
	}
	
	public EventDescription getDescription(EventType type)
	{
		if(_descriptions.containsKey(type))
			return _descriptions.get(type);
		else return null;
	}
	
	public static EventDescriptionSystem getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventDescriptionSystem _instance = new EventDescriptionSystem();
	}
}
