/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.nxs.events.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.events.manager.FenceManager;

/**
 * @author hNoke
 * - loads and manages EventMaps and their configs
 */
public class EventMapSystem
{
	private Map<EventType, Map<Integer, EventMap>> _maps;
	
	/** used for creating new maps */
	private int _lastMapId = 0;
	
	public EventMapSystem()
	{
		_maps = new FastMap<EventType, Map<Integer, EventMap>>();
		
		// init values
		for(EventType type : EventType.values())
		{
			_maps.put(type, new FastMap<Integer, EventMap>());
		}
		
		//loadMaps();
	}
	
	private EventType[] getTypes(String s)
	{
		String[] splits = s.split(";");
		List<EventType> types = new FastList<EventType>();
		
		EventType t;
		for(String typeString : splits)
		{
			t = EventType.getType(typeString);
			if(t != null)
				types.add(t);
		}
		
		return types.toArray(new EventType[types.size()]);
	}
	
	public String convertToString(List<EventType> types)
	{
		TextBuilder tb = new TextBuilder();
		int i = 1;
		for(EventType t : types)
		{
			tb.append(t.toString());
			
			if(i < types.size())
				tb.append(";");
			
			i++;
		}
		return tb.toString();
	}
	
	public void loadMaps()
	{
		Connection con = null;

		String type;
		int count = 0;

		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT mapId, mapName, eventType, configs, description FROM nexus_maps");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				type = rset.getString("eventType");
				
				List<EventType> types = new FastList<EventType>();
				for(EventType t : getTypes(type))
					types.add(t);

				EventMap map = new EventMap(rset.getInt("mapId"), rset.getString("mapName"), rset.getString("description"), types, loadSpawns(rset.getInt("mapId")), rset.getString("configs"));
				
				if(map.getMapDesc() == null)
				{
					map.setMapDesc("");
					map.setSaved(true);
				}
				
				map.loadConfigs();
				
				if(map.getGlobalId() > _lastMapId)
					_lastMapId = map.getGlobalId();
				
				// add this map for all map types
				for(EventType t : types)
				{
					_maps.get(t).put(_maps.get(t).size() + 1, map);
				}
				count++;
			}
			rset.close();
			statement.close();
		}

		catch (SQLException e)
		{
			e.printStackTrace();
		}

		finally
		{
			try
			{
				con.close();
			}

			catch (Exception e)
			{
			}
		}
		
		NexusLoader.debug("Nexus Engine: Loaded " + count + " EventMaps.");
	}
	
	public List<EventSpawn> loadSpawns(int arenaId)
	{
		Connection con = null;
		
		EventSpawn spawn;
		List<EventSpawn> spawns = new FastList<EventSpawn>();
		
		String note;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT mapId, spawnId, x, y, z, teamId, type, note FROM nexus_spawns WHERE mapId = " + arenaId);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				spawn = new EventSpawn(rset.getInt("mapId"), rset.getInt("spawnId"), new Loc(rset.getInt("x"), rset.getInt("y"), rset.getInt("z")), rset.getInt("teamId"), rset.getString("type"));
				note = rset.getString("note");
				
				if(note != null)
					spawn.setNote(note);
				
				spawn.setSaved(true);
				
				spawns.add(spawn);
			}
			rset.close();
			statement.close();
		}

		catch (SQLException e)
		{
			e.printStackTrace();
		}

		finally
		{
			try {con.close();}

			catch (Exception e){}
		}
		return spawns;
	}
	
	public void addSpawnToDb(EventSpawn spawn)
	{
		if(spawn.isSaved())
			return;
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_spawns VALUES (" + spawn.getMapId() + ", " + spawn.getSpawnId() + ", " + spawn.getLoc().getX() + ", " + spawn.getLoc().getY() + ", " + spawn.getLoc().getZ() + ", " + spawn.getSpawnTeam() + ", '" + spawn.getSpawnType().toString() + "', " + (spawn.getNote() == null ? "''" : ("'" +spawn.getNote()+ "'")) + ")");
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		spawn.setSaved(true);
	}
	public void addFenceToDb(EventSpawn spawn)
	{
		if(spawn.isSaved())
			return;
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_spawns VALUES (" + FenceManager.getInstance().getNextFenceId() + ", " + spawn.getLoc().getX() + ", " + spawn.getLoc().getY() + ", " + spawn.getLoc().getZ() + ", " + (spawn.getNote() == null ? "''" : ("'" +spawn.getNote()+ "'")) + ")");
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		spawn.setSaved(true);
	}
	public void removeSpawnFromDb(EventSpawn spawn)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM nexus_spawns WHERE mapId = " + spawn.getMapId() + " AND spawnId = " + spawn.getSpawnId());
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
	}
	
	public void removeMapFromDb(EventMap map)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM nexus_maps WHERE mapId = " + map.getGlobalId());
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
	}
	
	public void addMapToDb(EventMap map, boolean force)
	{
		if(map.isSaved() && !force)
			return;
		
		map.setConfigs(EventConfig.getInstance().convertMapConfigs(map));
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_maps VALUES (" + map.getGlobalId() + ", '" + map.getMapName().replaceAll("'", "") + "', '" + convertToString(map.getEvents()) + "', '" + map.getConfigs() + "', '" + map.getMapDesc() + "')");
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		map.setSaved(true);
	}
	
	public EventMap getNextMap(MiniEventManager manager, int lastId, EventMode mode)
	{
		EventType type = manager.getEventType();
		int nextMapId = lastId;
		EventMap map = null;
		int limit = 0;
		
		while(limit < _maps.get(type).size() + 99)
		{
			limit++;
			
			nextMapId++;
			map = _maps.get(type).get(nextMapId);
			
			if(map == null) // no next map is aviable
			{
				nextMapId = 0;
				continue;
			}
			else
			{
				if(!manager.canRun(map) || mode.getDisMaps().contains(map.getGlobalId()))
				{
					map = null;
					continue;
				}
				else
					return map;
			}
		}
		
		if(map == null)
			NexusLoader.debug("No map aviable for event " + type.getAltTitle() + " and mode " + mode.getModeName(), Level.WARNING);
		
		return map;
	}
	
	public int getMapIndex(EventType event, EventMap map)
	{
		for(Entry<Integer, EventMap> e : _maps.get(event).entrySet())
		{
			if(e.getValue().getGlobalId() == map.getGlobalId())
			{
				return e.getKey();
			}
		}
		return 0;
	}
	
	public EventMap getMapById(int id)
	{
		for(Map<Integer, EventMap> map : _maps.values())
		{
			for(Map.Entry<Integer, EventMap> m : map.entrySet())
			{
				if(m.getValue().getGlobalId() == id)
					return m.getValue();
			}
		}
		
		return null;
	}
	
	public int getNewMapId()
	{
		return ++_lastMapId;
	}
	
	public int getMapsCount(EventType type)
	{
		return _maps.get(type).size();
	}
	
	public Map<Integer, EventMap> getMaps(EventType type)
	{
		return _maps.get(type);
	}
	
	public boolean removeMap(int id)
	{
		EventMap map = getMapById(id);
		
		if(map == null)
			return false;
		
		removeMapFromDb(map);
		
		if(map.getGlobalId() >= _lastMapId)
			_lastMapId--;
		
		for(EventType type : map.getEvents())
		{
			for(Map.Entry<Integer, EventMap> e : _maps.get(type).entrySet())
			{
				if(e.getValue().getGlobalId() == id)
				{
					_maps.get(type).remove(e.getKey());
					reorganizeMaps(type);
				}
			}
		}
		
		for(EventSpawn spawn : map.getSpawns())
		{
			removeSpawnFromDb(spawn);
		}
		
		return true;
	}
	
	private void reorganizeMaps(EventType type)
	{
		Collection<EventMap> maps = _maps.get(type).values();
		Map<Integer, EventMap> mapping = new FastMap<Integer, EventMap>();
		
		for(EventMap map : maps)
		{
			mapping.put(mapping.size() + 1, map);
		}
		
		_maps.put(type, mapping);
	}
	
	public void addMap(EventMap map)
	{
		for(EventType type : map.getEvents())
		{
			_maps.get(type).put(_maps.get(type).size() + 1, map);
		}
	}
	
	public void addMapToEvent(EventMap map, EventType type)
	{
		List<EventMap> maps = new FastList<EventMap>();
		maps.addAll(_maps.get(type).values());
		maps.add(map);
		
		_maps.get(type).clear();
		
		int i = 0;
		for(EventMap m : maps)
		{
			_maps.get(type).put(i, m);
			i++;
		}
	}
	
	public void removeMapFromEvent(EventMap map, EventType type)
	{
		for(Map.Entry<Integer, EventMap> e : _maps.get(type).entrySet())
		{
			if(e.getValue().getGlobalId() == map.getGlobalId())
				_maps.get(type).remove(e.getKey());
		}
	}
	
	public List<EventMap> getMainEventMaps(EventType type)
	{
		if(!type.isRegularEvent())
			return null;
		
		List<EventMap> maps = new FastList<EventMap>();
		maps.addAll(_maps.get(type).values());
		
		return maps;
	}
	
	public EventMap getMap(EventType type, String mapName)
	{
		for(EventMap map : _maps.get(type).values())
		{
			if(map.getMapName().equals(mapName))
			return map;
		}
		return null;
	}

	public static EventMapSystem getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventMapSystem _instance = new EventMapSystem();
	}
}
