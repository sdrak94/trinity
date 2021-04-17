package cz.nxs.events.engine.configtemplate;

import cz.nxs.events.Configurable;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 * - work in progress -
 */
public abstract class ConfigTemplate 
{
	public abstract String getName();
	public abstract EventType getEventType();
	public abstract String getDescription();
	public abstract SetConfig[] getConfigs();
	
	public class SetConfig
	{
		String key;
		String value;
		
		public SetConfig(String key, String value)
		{
			this.key = key;
			this.value = value;
		}
	}
	
	//TODO implement this in admin panel
	public void applyTemplate(PlayerEventInfo gm, EventType type, Configurable event)
	{
		int changed = 0;
		for(SetConfig sc : getConfigs())
		{
			if(event.getConfigs().containsKey(sc.key))
			{
				if(event.getConfigs().get(sc.key).getValue().equals(sc.value))
					continue;
				
				event.getConfigs().get(sc.key).setValue(sc.value);
				changed ++;
			}
		}
		
		int total = event.getConfigs().size();
		
		EventConfig.getInstance().updateInDb(type);
		
		gm.sendMessage("Applied template " + getName() + " to event " + type.getAltTitle() + ". " + changed + "/" + total + " configs have been changed.");
	}
	
	//TODO check for missing configs somehow
	//add an array with those configs as template
}
