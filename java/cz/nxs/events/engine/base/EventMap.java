package cz.nxs.events.engine.base;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.Configurable;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.interf.PlayerEventInfo;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class EventMap
{
	private int _globalId;
	
	private boolean _saved = true;
	private String _mapName;
	private String _configs;
	private String _mapDesc; // description

	private int _highestSpawnId;
	
	private List<EventType> _events;
	
	private List<EventSpawn> _spawns;
	private Map<Integer, Map<SpawnType, Integer>> _history;
	
	private Map<Integer, EventSpawn> _lastSpawns;
	
	private List<EventSpawn> _doorsSpawn;
	private boolean _hasDoors;
	
	private Map<EventType, Map<String, ConfigModel>> _configModels;
	
	public static Comparator<EventSpawn> compareByIdAsc = new Comparator<EventSpawn>()
	{
		@Override
		public int compare(EventSpawn s1, EventSpawn s2)
		{
			int id1 = s1.getSpawnId();
			int id2 = s2.getSpawnId();
			
			return id1 == id2 ? 0 : id1 < id2 ? -1 : 1;
		}
	};
	
	public static Comparator<EventSpawn> compareByIdDesc = new Comparator<EventSpawn>()
	{
		@Override
		public int compare(EventSpawn s1, EventSpawn s2)
		{
			int id1 = s1.getSpawnId();
			int id2 = s2.getSpawnId();
			
			return id1 == id2 ? 0 : id1 > id2 ? -1 : 1;
		}
	};
	
	public static Comparator<EventSpawn> compareByType = new Comparator<EventSpawn>()
	{
		@Override
		public int compare(EventSpawn s1, EventSpawn s2)
		{
			SpawnType t1 = s1.getSpawnType();
			SpawnType t2 = s2.getSpawnType();
			
			return t1.compareTo(t2);
		}
	};

	public EventMap(int mapId, String mapName, String mapDesc, List<EventType> events, List<EventSpawn> spawns, String configs)
	{
		_globalId = mapId;
		_mapName = mapName;
		_mapDesc = mapDesc;
		_configs = configs;

		_spawns = new FastList<EventSpawn>();
		_history = new FastMap<Integer, Map<SpawnType, Integer>>();
		
		_lastSpawns = new FastMap<Integer, EventSpawn>();
		_events = events;
		
		if(_events == null)
		{
			NexusLoader.debug("_events null in EventMap constructor");
			_events = new FastList<EventType>();
		}
		
		_configModels = new FastMap<EventType, Map<String, ConfigModel>>();
		
		addSpawns(spawns);
		initDoors();
	}
	
	public void loadConfigs()
	{
		for(EventType event : _events)
		{
			initEventsConfigs(event);
		}
		
		EventConfig.getInstance().loadMapConfigs(this, _configs);
	}
	
	private void initEventsConfigs(EventType event)
	{
		_configModels.put(event, new FastMap<String, ConfigModel>());
		
		Configurable conf = EventManager.getInstance().getEvent(event);
		
		// the event isn't implemented yet
		if(conf == null || conf.getMapConfigs() == null)
			return;
		
		for(ConfigModel config : conf.getMapConfigs().values())
		{
			_configModels.get(event).put(config.getKey(), new ConfigModel(config.getKey(), config.getValue(), config.getDesc(), config.getInput()));
		}
	}
	
	private void deleteEventsConfigs(EventType event)
	{
		_configModels.remove(event);
	}
	
	public void setConfigValue(EventType event, String key, String value, boolean addToValue)
	{
		try
		{
			if(!_configModels.containsKey(event))
			{
				NexusLoader.debug("Trying to set MapConfig's: map ID " + getGlobalId() + " event " + event.getAltTitle() + ", config's key = " + key + ". The map doesn't have such event.");
				return;
			}
			
			if(_configModels.get(event).get(key) == null)
			{
				NexusLoader.debug("Trying to set MapConfig's: map ID " + getGlobalId() + " event " + event.getAltTitle() + ", config's key = " + key + ", but this config doesn't exist for that map! Skipping...");
				return;
			}

			if(!addToValue)
				_configModels.get(event).get(key).setValue(value);
			else
				_configModels.get(event).get(key).addToValue(value);
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error setting map config's value to " + value + ", config's key = " + key + ", map ID = " + getGlobalId() + " and event = " + event.getAltTitle(), Level.WARNING);
			e.printStackTrace();
		}
	}
	
	public Map<EventType, Map<String, ConfigModel>> getConfigModels()
	{
		return _configModels;
	}
	
	public ConfigModel getConfigModel(EventType event, String key)
	{
		try
		{
			if(!_configModels.containsKey(event))
			{
				NexusLoader.debug("Trying to set MapConfig's value: map ID " + getGlobalId() + " event " + event.getAltTitle() + ", config's key = " + key + ". The map doesn't have such event.");
				return null;
			}

			return _configModels.get(event).get(key);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void addSpawns(List<EventSpawn> spawns)
	{
		if(spawns == null)
			return;
		
		_spawns.addAll(spawns);
		
		for(EventSpawn spawn : spawns)
		{
			if(!_history.containsKey(spawn.getSpawnTeam()))
				_history.put(spawn.getSpawnTeam(), new FastMap<SpawnType, Integer>());
			
			if(!_history.get(spawn.getSpawnTeam()).containsKey(spawn.getSpawnType()))
				_history.get(spawn.getSpawnTeam()).put(spawn.getSpawnType(), 0);
		}
		
		recalcLastSpawnId();
	}
	
	public FastList<EventSpawn> getSpawns(int teamId, SpawnType type)
	{
		FastList<EventSpawn> temp = new FastList<EventSpawn>();
		for(EventSpawn spawn : _spawns)
		{
			if((spawn.getSpawnTeam() == teamId || teamId == -1) && spawn.getSpawnType() == type)
				temp.add(spawn);
		}
		return temp;
	}
	
	public FastList<EventSpawn> getMarkers(int teamId)
	{
		return getSpawns(teamId, SpawnType.Radar);
	}
	
	public void clearHistory(int teamId, SpawnType type)
	{
		if(teamId == -1) // remove all teams' spawn history
		{
			for(Entry<Integer, Map<SpawnType, Integer>> e : _history.entrySet())
			{
				_history.get(e.getKey()).put(type, 0);
			}
		}
		else 
			_history.get(teamId).put(type, 0);
	}
	
	public EventSpawn getNextSpawn(int teamId, SpawnType type)
	{
		FastList<EventSpawn> spawns = getSpawns(teamId, type);
		
		if(spawns == null || spawns.isEmpty()) 
			return null;
		
		if(teamId == -1)
			teamId = 0;
		
		int lastId = 0;
		
		try
		{
			lastId = _history.get(teamId).get(type);
		} 
		catch (NullPointerException e)
		{
			lastId = 0;
		}
		
		EventSpawn nextSpawn = null;
		
		for(EventSpawn spawn : spawns)
		{
			if(spawn.getSpawnId() > lastId)
			{
				nextSpawn = spawn;
				break;
			}
		}
		
		if(nextSpawn == null)
			nextSpawn = spawns.getFirst();
		
		lastId = nextSpawn.getSpawnId();
		
		if(!_history.containsKey(teamId))
			_history.put(teamId, new FastMap<SpawnType, Integer>());
		
		_history.get(teamId).put(type, lastId);
		
		return nextSpawn;
	}
	
	public List<EventSpawn> getSpawns()
	{
		return _spawns;
	}
	
	public EventSpawn getSpawn(int spawnId)
	{
		for(EventSpawn spawn : _spawns)
		{
			if(spawn.getSpawnId() == spawnId)
				return spawn;
		}
		return null;
	}

	public boolean removeSpawn(int spawnId, boolean db)
	{
		for(EventSpawn spawn : _spawns)
		{
			if(spawn.getSpawnId() == spawnId)
			{
				_spawns.remove(spawn);
				
				if(getSpawns(spawn.getSpawnTeam(), spawn.getSpawnType()).isEmpty())
					_history.remove(spawn.getSpawnType());
				
				if(db)
					EventMapSystem.getInstance().removeSpawnFromDb(spawn);
				
				recalcLastSpawnId();
				
				return true;
			}
		}
		return false;
	}
	
	private void recalcLastSpawnId()
	{
		int highestId = 0;
		
		for(EventSpawn spawn : _spawns)
		{
			if(spawn.getSpawnId() > highestId)
				highestId = spawn.getSpawnId();
		}

		_highestSpawnId = highestId;
	}
	
	private void initDoors()
	{
		for(EventSpawn spawn : _spawns)
		{
			if(spawn.getSpawnType() == SpawnType.Door)
			{
				if(_doorsSpawn == null)
					_doorsSpawn = new FastList<EventSpawn>();
				
				_doorsSpawn.add(spawn);
				_hasDoors = true;
			}
		}
	}
	
	public String[] getAviableConfigs(EventType type)
	{
		if(type != EventType.Unassigned && _events.contains(type))
		{
			Configurable event = EventManager.getInstance().getEvent(type, 1);
			
			if(event == null)
			{
				System.out.println("null event at getAviableConfigs(EventType)");
				return null;
			}
			
			return event.getMapConfigs().keySet().toArray(new String[event.getMapConfigs().size()]);
		}
		
		System.out.println("getAviableConfigs - type " + type.getAltTitle() + " returned null.");
		return null;
	}
	
	public boolean hasDoor()
	{
		return _hasDoors;
	}
	
	public List<EventSpawn> getDoors()
	{
		return _doorsSpawn;
	}

	public EventSpawn getLastSpawn(int teamId)
	{
		return _lastSpawns.get(teamId);
	}

	public String getMapName()
	{
		return _mapName;
	}
	
	public String getMapDesc()
	{
		return _mapDesc;
	}

	public int getGlobalId()
	{
		return _globalId;
	}

	public List<EventType> getEvents()
	{
		return _events;
	}
	
	public String getConfigs()
	{
		return _configs;
	}
	
	public void setConfigs(String s)
	{
		_configs = s;
	}
	
	public void setMapName(String name)
	{
		_mapName = name;
		_saved = false;
	}
	
	public void setMapDesc(String desc)
	{
		_mapDesc = desc;
		_saved = false;
	}
	
	public int getNewSpawnId()
	{
		return _highestSpawnId + 1;
	}
	
	public void addEvent(EventType type)
	{
		_events.add(type);
		EventMapSystem.getInstance().addMapToEvent(this, type);
		initEventsConfigs(type);
		_saved = false;
	}
	
	public void removeEvent(EventType type)
	{
		if(_events.remove(type))
		{
			EventMapSystem.getInstance().removeMapFromEvent(this, type);
			deleteEventsConfigs(type);
			
			if(_events.isEmpty())
			{
				_events.add(EventType.Unassigned);	
				EventMapSystem.getInstance().addMapToEvent(this, EventType.Unassigned);
			}
			
			_saved = false;
		}
	}
	
	public boolean isSaved()
	{
		return _saved;
	}
	
	public void setSaved(boolean b)
	{
		_saved = b;
		
		// reinit values
		if(_saved)
		{
			initDoors();
		}
	}
	
	public boolean checkForSpawns(SpawnType type, int teamId, int count)
	{
		try
		{
			return getSpawns(teamId, type).size() >= count;
		}
		catch (NullPointerException npe)
		{
			return false;
		}
	}
	
	public String getMissingSpawns()
	{
		TextBuilder tb = new TextBuilder();
		Configurable event;
		
		for(EventType type : getEvents())
		{
			event = EventManager.getInstance().getEvent(type);
			if(event == null) 
				return "";
			
			if(type.isRegularEvent())
			{
				tb.append(event.getMissingSpawns(this));
			}
			else
			{
				//TODO ?
			}
		}
		
		return tb.toString();
	}
	
	public String getNotWorkingEvents()
	{
		TextBuilder tb = new TextBuilder();
		
		String temp;
		
		for(EventType type : getEvents())
		{
			if(type.isMiniEvent())
			{
				temp = "";
				
				for(MiniEventManager manager : EventManager.getInstance().getMiniEvents().get(type).values())
				{
					temp += manager.getMissingSpawns(this);
				}
				
				if(temp.length() > 0)
				{
					tb.append("<font color=LEVEL>" + type.getHtmlTitle() + "</font><br1>");
					tb.append(temp);
					tb.append("<br>");
				}
			}
		}
		
		return tb.toString();
	}
	
	public void checkMap(PlayerEventInfo gm)
	{
		for(EventType type : getEvents())
		{
			if(type == EventType.Classic_1v1 || type == EventType.Classic_2v2 || type == EventType.PartyvsParty || type == EventType.TvT || type == EventType.TvTAdv || type == EventType.MiniTvT)
			{
				if(!checkForSpawns(SpawnType.Regular, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1, count 1");
				if(!checkForSpawns(SpawnType.Regular, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 2, count 1");
			}
			else if(type == EventType.CTF)
			{
				if(!checkForSpawns(SpawnType.Flag, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type FLAG, team 1, count 1.");
				if(!checkForSpawns(SpawnType.Flag, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type FLAG, team 2, count 1.");
				
				if(!checkForSpawns(SpawnType.Regular, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1, count 1 or more.");
				if(!checkForSpawns(SpawnType.Regular, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 2, count 1 or more.");
			}
			else if(type == EventType.DM || type == EventType.LMS)
			{
				if(!checkForSpawns(SpawnType.Regular, 1, -1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, count 1 or more. (team doesn't matter)");
			}
			else if(type == EventType.Mutant || type == EventType.Zombies)
			{
				if(!checkForSpawns(SpawnType.Regular, 1, -1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR,count 1 or more. (team doesn't matter");
				if(!checkForSpawns(SpawnType.Zombie, 1, -1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type ZOMBIE, count 1 or more.");
			}
			else if(type == EventType.Korean)
			{
				if(!checkForSpawns(SpawnType.Safe, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type SAFE, team 1, count 1. This is initial spawn for Players.");
				if(!checkForSpawns(SpawnType.Safe, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type SAFE, team 2, count 1. This is initial spawn for Players.");
				
				if(!checkForSpawns(SpawnType.Regular, 1, 4))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1, count 4 (for each player one spot)");
				if(!checkForSpawns(SpawnType.Regular, 2, 4))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 2, count 4 (for each player one spot)");
			}
			else if(type == EventType.Underground_Coliseum)
			{
				if(!checkForSpawns(SpawnType.Regular, 1, 4))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1. count 1. This is initial spawn used to teleport players before event starts.");
				if(!checkForSpawns(SpawnType.Regular, 2, 4))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 2, count 1. This is initial spawn used to teleport players before event starts.");
				
				if(!checkForSpawns(SpawnType.Safe, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type SAFE, team 1, count 1. This is respawn spot.");
				if(!checkForSpawns(SpawnType.Safe, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type SAFE, team 2, count 1. This is respawn spot.");
			}
			else if(type == EventType.RBHunt)
			{
				if(!checkForSpawns(SpawnType.Boss, -1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type BOSS count 1.");
				
				if(!checkForSpawns(SpawnType.Regular, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1, count 1 or more.");
				if(!checkForSpawns(SpawnType.Regular, 2, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 2, count 1 or more.");
			}
			else if(type == EventType.SurvivalArena)
			{
				boolean round1 = false;
				boolean round2 = false;
				boolean round3 = false;
				
				//TODO
				/*for(EventSpawn sp : getSpawns(-1, SpawnType.Monster))
				{
					if(round1 && round2 && round3)
						break;
					
					if(!round1 && SurvivalArenaGame.getWaweNumber(sp.getNote()) == 1)
					{
						round1 = true;
						continue;
					}
					else if(!round2 && SurvivalArenaGame.getWaweNumber(sp.getNote()) == 2)
					{
						round2 = true;
						continue;
					}
					else if(!round3 && SurvivalArenaGame.getWaweNumber(sp.getNote()) == 3)
					{
						round3 = true;
						continue;
					}
				}*/
				
				if(!round1)
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type MONSTER for FIRST round!");
				if(!round2)
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type MONSTER for SECOND round!");
				if(!round3)
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type MONSTER for FINAL round!");
				
				if(!checkForSpawns(SpawnType.Regular, 1, 1))
					gm.sendMessage(type.getAltTitle() + ": Missing spawn type REGULAR, team 1, count 1.");
			}
		}
	}
}
