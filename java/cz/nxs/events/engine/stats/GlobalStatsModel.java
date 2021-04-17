package cz.nxs.events.engine.stats;

import java.util.Map;
import java.util.Map.Entry;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import javolution.util.FastMap;

public class GlobalStatsModel
{
	protected EventType event;
	protected Map<GlobalStatType, Integer> stats;
	
	public GlobalStatsModel(EventType event, Map<GlobalStatType, Integer> stats)
	{
		this.event = event;
		this.stats = stats;
	}
	
	public GlobalStatsModel(EventType event)
	{
		this.event = event;
		this.stats = new FastMap<GlobalStatType, Integer>();
		
		for(GlobalStatType t : GlobalStatType.values())
		{
			stats.put(t, 0);
		}
	}
	
	public int get(GlobalStatType type)
	{
		return stats.get(type);
	}
	
	public void set(GlobalStatType type, int value)
	{
		stats.put(type, value);
	}
	
	public void raise(GlobalStatType type, int value)
	{
		set(type, get(type) + value);
	}
	
	public void add(GlobalStatsModel newStats)
	{
		for(Entry<GlobalStatType, Integer> e : newStats.stats.entrySet())
		{
			raise(e.getKey(), e.getValue());
		}
	}
	
	public EventType getEvent()
	{
		return event;
	}
	
	public String getFavoriteEvent()
	{
		return "N/A";
	}
}