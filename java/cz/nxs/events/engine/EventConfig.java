package cz.nxs.events.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.Configurable;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.GlobalConfigModel;
import cz.nxs.events.engine.html.EventHtmlManager;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.ScheduleInfo;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - manages all configurations for Events, EventMaps and MainEvent Instances (those use ConfigModel)
 * - loads EventModes
 * - manages Global Configs
 */
public class EventConfig
{
	private Map<EventType, Config> _eventConfigs = new FastMap<EventType, Config>();
	private Map<String, List<GlobalConfigModel>> _globalConfigs = new FastMap<String, List<GlobalConfigModel>>();
	
	private EventConfig()
	{
		
	}
	
	public void loadEventConfigs()
	{
		loadMiniEventModes();
		loadEventConfigsFromDb();
	}
	
	public void loadGlobalConfigs()
	{
		loadGlobalConfigsFromDb();
		
		// check if all GlobalConfigs are in database, insert them there if not
		EventSQLManager.addMissingGlobalConfigs();
		
		NexusLoader.debug("Nexus Engine: Loaded GlobalConfigs engine.");
	}
	
	private void loadMiniEventModes()
	{
		int count = 0;
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT * FROM nexus_modes");
			ResultSet rset = statement.executeQuery();
			
			while(rset.next())
			{
				EventType type = EventType.getType(rset.getString("event"));
				
				if(type == null)
					continue;
				
				int modeId = rset.getInt("modeId");
				String modeName = rset.getString("name");
				String visibleName = rset.getString("visible_name");
				String parameters = rset.getString("params");
				boolean allowed = Boolean.parseBoolean(rset.getString("allowed"));
				String maps = rset.getString("disallowedMaps");
				String times = rset.getString("times");
				int npcId = rset.getInt("npcId");
				
				loadMode(type, modeId, modeName, visibleName, parameters, maps, times, allowed, npcId);
				count ++;
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
		
		NexusLoader.debug("Nexus Engine: Loaded " + count + " mini event modes.");
	}
	
	private void loadMode(EventType type, int modeId, String modeName, String visibleName, String parameters, String maps, String time, boolean allowed, int npcId)
	{
		MiniEventManager manager = EventManager.getInstance().createManager(type, modeId, modeName, visibleName, false);
		
		if(manager == null)
		{
			NexusLoader.debug("manager's null after it was created!", Level.WARNING);
			return;
		}
		
		manager.getMode().setAllowed(allowed);
		manager.getMode().getScheduleInfo().encrypt(time);
		
		if(!maps.equals("") && !maps.equals(" "))
		{
			for(String s : maps.split(";"))
			{
				manager.getMode().getDisMaps().add(Integer.parseInt(s));
			}
		}
		
		FeatureType featureType = null;
		String[] featuresAndConfigs = parameters.split(";");
		
		if(featuresAndConfigs.length > 0 && featuresAndConfigs[0] != "" && featuresAndConfigs[0] != " ")
		{
			String[] splitted;
			
			for(String features : featuresAndConfigs)
			{
				splitted = features.split(":");
				
				for(FeatureType t : FeatureType.values())
				{
					if(t.toString().equals(splitted[0]))
					{
						featureType = t;
						break;
					}
				}
				
				if(featureType == null)
				{
					NexusLoader.debug("feature type - " + splitted[0] + " doesn't exist. " +
							"(event " + type.getAltTitle() + ", modeId " + modeId + ")", Level.WARNING);
					continue;
				}
				
				manager.getMode().addFeature(null, featureType, splitted[1]);
			}
		}
		
		manager.getMode().refreshScheduler();
		manager.getMode().setNpcId(npcId);
	}
	
	public MiniEventManager createDefaultMode(EventType type)
	{
		MiniEventManager manager = EventManager.getInstance().createManager(type, 1, "Default", "Default", false);

		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement;
			
			statement = con.prepareStatement("DELETE FROM nexus_modes WHERE event = '" + type.getAltTitle() + "' AND modeId = " + 1);
			statement.execute();
			statement.close();
			
			MiniEventManager event =  EventManager.getInstance().getMiniEvents().get(type).get(1);
			
			statement = con.prepareStatement("INSERT INTO nexus_modes VALUES (?,?,?,?,?,?,?,?,?)");
			
			statement.setString(1, type.getAltTitle());
			statement.setInt(2, 1);
			statement.setString(3, event.getMode().getModeName().replaceAll("'", ""));
			statement.setString(4, event.getMode().getVisibleName().replaceAll("'", ""));
			statement.setString(5, String.valueOf(event.getMode().isAllowed()));
			statement.setString(6, getParams(event));
			statement.setString(7, getDisMaps(event));
			statement.setString(8, getTimesAviable(event));
			statement.setInt(9, 0);
			
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		return manager;
	}
	
	public void updateEventModes(EventType type, int modeId)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement;
			
			// updates all modes
			if(modeId <= 0)
			{
				statement = con.prepareStatement("DELETE FROM nexus_modes WHERE event = '" + type.getAltTitle() + "'");
				statement.execute();
				statement.close();
				
				for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(type).entrySet())
				{
					statement = con.prepareStatement("INSERT INTO nexus_modes VALUES (?,?,?,?,?,?,?,?,?)");
					
					statement.setString(1, type.getAltTitle());
					statement.setInt(2, e.getKey());
					statement.setString(3, e.getValue().getMode().getModeName().replaceAll("'", ""));
					statement.setString(4, e.getValue().getMode().getVisibleName().replaceAll("'", ""));
					statement.setString(5, String.valueOf(e.getValue().getMode().isAllowed()));
					statement.setString(6, getParams(e.getValue()));
					statement.setString(7, getDisMaps(e.getValue()));
					statement.setString(8, getTimesAviable(e.getValue()));
					statement.setInt(9, e.getValue().getMode().getNpcId());
					
					statement.execute();
					statement.close();
				}
			}
			else
			{
				statement = con.prepareStatement("DELETE FROM nexus_modes WHERE event = '" + type.getAltTitle() + "' AND modeId = " + modeId);
				statement.execute();
				statement.close();
				
				MiniEventManager event =  EventManager.getInstance().getMiniEvent(type, modeId);
				
				if(event != null)
				{
					statement = con.prepareStatement("INSERT INTO nexus_modes VALUES (?,?,?,?,?,?,?,?,?)");
					
					statement.setString(1, type.getAltTitle());
					statement.setInt(2, modeId);
					statement.setString(3, event.getMode().getModeName().replaceAll("'", ""));
					statement.setString(4, event.getMode().getVisibleName().replaceAll("'", ""));
					statement.setString(5, String.valueOf(event.getMode().isAllowed()));
					statement.setString(6, getParams(event));
					statement.setString(7, getDisMaps(event));
					statement.setString(8, getTimesAviable(event));
					statement.setInt(9, event.getMode().getNpcId());
					
					statement.execute();
					statement.close();
				}
				else
					NexusLoader.debug("Tried to save unexisting event mode - " + type.getAltTitle() + ", mode " + modeId, Level.WARNING);
			}
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
	
	private String getTimesAviable(MiniEventManager manager)
	{
		ScheduleInfo info = manager.getMode().getScheduleInfo();
		return info.decrypt();
	}
	
	private String getDisMaps(MiniEventManager manager)
	{
		TextBuilder tb = new TextBuilder();
		
		for(int mapId : manager.getMode().getDisMaps())
		{
			tb.append(mapId + ";");
		}
		
		String result = tb.toString();
		
		if(result.length() > 0)
			return result.substring(0, result.length() - 1);
		else
			return result;
	}
	
	private String getParams(MiniEventManager manager)
	{
		TextBuilder tb = new TextBuilder();
		for(AbstractFeature feature : manager.getMode().getFeatures())
		{
			tb.append(feature.getType().toString() + ":" + feature.getParams() + ";");
		}
		String result = tb.toString();
		
		if(result.length() == 0)
			return result;
		else
			return result.substring(0, result.length() - 1);
	}
	
	// *********************************************************************
	
	private void loadEventConfigsFromDb()
	{
		int count = 0;
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			// load event configs
			PreparedStatement statement = con.prepareStatement("SELECT * FROM nexus_configs");
			ResultSet rset = statement.executeQuery();
			
			while(rset.next())
			{
				EventType type = EventType.getType(rset.getString("event"));
				
				if(type == null)
					continue;
				
				_eventConfigs.put(type, new Config(type, Boolean.parseBoolean(rset.getString("allowed"))));
				
				count += deconvert(type, rset.getString("params"));
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
		
		for(EventType t : EventType.values())
		{
			if(!t.allowEdits() || t == EventType.Unassigned)
				continue;
			
			if(!_eventConfigs.containsKey(t))
			{
				_eventConfigs.put(t, new Config(t, true));
				addNew(t);
			}
		}
		
		NexusLoader.debug("Nexus Engine: Loaded " + count + " configs for events.");
	}
	
	private String convert(EventType type)
	{
		TextBuilder tb = new TextBuilder();
		
		Configurable event = EventManager.getInstance().getEvent(type);
		if(event == null)
		{
			NexusLoader.debug("null event on EventConfig.convert, event type " + type.getAltTitle(), Level.SEVERE);
			return "";
		}
		
		for(Map.Entry<String, ConfigModel> e : event.getConfigs().entrySet())
		{
			tb.append(e.getKey() + ":" + e.getValue().getValue());
			tb.append(";");
		}
		
		String result = tb.toString();
		
		if(result.length() > 0)
			return result.substring(0, result.length() - 1);
		else
			return "";
	}
	
	private int deconvert(EventType type, String params)
	{
		try
		{
			int count = 0;
			String[] configs = params.split(";");
			String key, value;
			
			for(String config : configs)
			{
				key = config.split(":")[0];
				
				if(config.split(":").length > 1)
					value = config.split(":")[1];
				else
					value = "";
				
				EventManager.getInstance().getEvent(type).setConfig(key, value, false);
				
				count ++;
			}
			
			return count;
		} catch (Exception e)
		{
			// means that there are no configs
			return 0;
		}
	}
	
	public boolean isEventAllowed(EventType type)
	{
		return _eventConfigs.get(type) != null && _eventConfigs.get(type).allowed;
	}
	
	public void setEventAllowed(EventType type, boolean b)
	{
		_eventConfigs.get(type).allowed = b;
		updateInDb(type);
	}
	
	public void updateInDb(EventType type)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("DELETE FROM nexus_configs WHERE event = '" + type.getAltTitle() + "'");
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("INSERT INTO nexus_configs VALUES ('" + type.getAltTitle() + "', '" + Boolean.toString(_eventConfigs.get(type).allowed) + "', ?)");
			statement.setString(1, convert(type));
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
	
	private void addNew(EventType type)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO nexus_configs VALUES ('" + type.getAltTitle() + "', 'true', ?)");
			statement.setString(1, convert(type));
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
	
	public void addConfig(EventType event, String param, String value, boolean addToValue)
	{
		if(event.isMiniEvent())
		{
			for(MiniEventManager mgr : EventManager.getInstance().getMiniEvents().get(event).values())
			{
				if(mgr != null)
					mgr.setConfig(param, value, addToValue);
			}
		}
		else
		{
			EventManager.getInstance().getEvent(event).setConfig(param, value, addToValue);
		}
		
		updateInDb(event);
	}
	
	public void removeConfigMultiAddValue(EventType event, String key, int index)
	{
		if(event.isMiniEvent())
		{
			for(MiniEventManager mgr : EventManager.getInstance().getMiniEvents().get(event).values())
			{
				if(mgr != null)
					mgr.getConfigs().get(key).removeMultiAddValueIndex(index);
			}
		}
		else
		{
			EventManager.getInstance().getEvent(event).getConfigs().get(key).removeMultiAddValueIndex(index);
		}
		
		updateInDb(event);
	}
	
	private class Config
	{
		private boolean allowed;
		public Config(EventType type, boolean allowed)
		{
			this.allowed = allowed;
		}
	}
	
	public String convertMapConfigs(EventMap map)
	{
		TextBuilder tb = new TextBuilder();
		
		int totalEvents = map.getConfigModels().size();
		int count = 1;
		for(Entry<EventType, Map<String, ConfigModel>> e : map.getConfigModels().entrySet())
		{
			if(!e.getValue().values().isEmpty())
			{
				tb.append(e.getKey().getAltTitle() + ":");
				
				int size = e.getValue().values().size();
				
				int i = 1;
				for(ConfigModel config : e.getValue().values())
				{
					tb.append(config.getKey() + "-" + config.getValue());
					if(i < size)
						tb.append(",");
					
					i++;
				}
				
				if(count < totalEvents)
					tb.append(";");
			}
			
			count ++;
		}
		
		return tb.toString();
	}
	
	public void saveMapConfigs(EventMap map)
	{
		EventMapSystem.getInstance().addMapToDb(map, true);
	}
	
	public void loadMapConfigs(EventMap map, String params)
	{
		try
		{
			if(params == null || params.isEmpty() || params.equals(" "))
				return;
			
			String[] events = params.split(";");
			
			EventType eventType;
			String[] configs;
			
			for(String event : events)
			{
				if(event.split(":").length > 1)
				{
					eventType = EventType.getType(event.split(":")[0]);
					configs = event.split(":")[1].split(",");
					
					if(eventType == null)
					{
						NexusLoader.debug("error while mapConfigs loading - event: " + event + " does not exist, map ID = " + map.getGlobalId(), Level.WARNING); 
						continue;
					}
					
					String key, value;
					for(String config : configs)
					{
						key = config.split("-")[0];
						
						if(config.split("-").length > 1)
							value = config.split("-")[1];
						else
							value = "";
						
						map.setConfigValue(eventType, key, value, false);
					}
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setMapConfig(EventMap map, EventType event, String param, String value, boolean addToValue)
	{
		map.setConfigValue(event, param, value, addToValue);
		saveMapConfigs(map);
	}
	
	public void removeMapConfigMultiAddValue(EventMap map, EventType event, String key, int index)
	{
		map.getConfigModel(event, key).removeMultiAddValueIndex(index);
		saveMapConfigs(map);
	}
	
	public String getMapConfig(EventMap map, EventType type, String key)
	{
		try
		{
			return map.getConfigModel(type, key).getValue();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public int getMapConfigInt(EventMap map, EventType type, String key)
	{
		try
		{
			return map.getConfigModel(type, key).getValueInt();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}
	
	public boolean getMapConfigBoolean(EventMap map, EventType type, String key)
	{
		try
		{
			return map.getConfigModel(type, key).getValueBoolean();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	private void loadGlobalConfigsFromDb()
	{
		_globalConfigs.clear();
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT * FROM nexus_globalconfigs");
			ResultSet rset = statement.executeQuery();
			
			while(rset.next())
			{
				String type = rset.getString("configType");
				String key = rset.getString("key");
				String desc = rset.getString("desc");
				String value = rset.getString("value");
				int input = rset.getInt("inputType");
				
				addGlobalConfig(type, key, desc, value, input);
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
	}
	
	public GlobalConfigModel addGlobalConfig(String type, String key, String desc, String value, int inputType)
	{
		if(!_globalConfigs.containsKey(type))
			_globalConfigs.put(type, new FastList<GlobalConfigModel>());
		
		GlobalConfigModel gc = new GlobalConfigModel(type, key, value, desc, inputType);
		_globalConfigs.get(type).add(gc);
		
		return gc;
	}
	
	public void removeGlobalConfig(String type, String key)
	{
		for(GlobalConfigModel c : _globalConfigs.get(type))
		{
			if(c.getKey().equals(key))
			{
				_globalConfigs.get(type).remove(c);
				break;
			}
		}
	}
	
	public List<GlobalConfigModel> getGlobalConfigs(String type)
	{
		return _globalConfigs.get(type);
	}
	
	public String getGlobalConfigValue(String type, String key)
	{
		if(NexusLoader.loadedOrBeingLoaded())
		{
			GlobalConfigModel gc = getGlobalConfig(type, key);
			if(gc != null)
				return gc.getValue();
			
			NexusLoader.debug("GlobalConfig '" + key + "' has not been found.", Level.WARNING);
		}
		
		return null;
	}
	
	public int getGlobalConfigInt(String type, String key)
	{
		if(NexusLoader.loadedOrBeingLoaded())
		{
			String val = getGlobalConfigValue(type, key);
			try
			{
				return Integer.parseInt(val);
			}
			catch (Exception e)
			{
				NexusLoader.debug("GlobalConfig '" + key + "' int cast error.", Level.WARNING);
				return 0;
			}
		}
		return 0;
	}
	
	public boolean getGlobalConfigBoolean(String type, String key)
	{
		if(NexusLoader.loadedOrBeingLoaded())
		{
			String val = getGlobalConfigValue(type, key);
			try
			{
				return Boolean.parseBoolean(val);
			}
			catch (Exception e)
			{
				NexusLoader.debug("GlobalConfig '" + key + "' boolean cast error.", Level.WARNING);
				return false;
			}
		}
		return false;
	}
	
	public int getGlobalConfigInt(String key)
	{
		return getGlobalConfigInt(null, key);
	}
	
	public String getGlobalConfigValue(String key)
	{
		return getGlobalConfigValue(null, key);
	}
	
	public boolean getGlobalConfigBoolean(String key)
	{
		return getGlobalConfigBoolean(null, key);
	}
	
	public String getGlobalConfigDesc(String type, String key)
	{
		GlobalConfigModel gc = getGlobalConfig(type, key);
		if(gc != null)
			return gc.getDesc();
		
		return null;
	}
	
	public String getGlobalConfigType(GlobalConfigModel config)
	{
		for(Entry<String, List<GlobalConfigModel>> list : _globalConfigs.entrySet())
		{
			for(GlobalConfigModel c : list.getValue())
			{
				if(config.getKey().equals(c.getKey()))
					return list.getKey();
			}
		}
		return null;
	}
	
	public boolean globalConfigExists(String key)
	{
		GlobalConfigModel gc = getGlobalConfig(null, key);
		return gc != null;
	}
	
	public void setGlobalConfigValue(GlobalConfigModel config, String key, String value)
	{
		String prev = config.getValue();
		config.setValue(value);
		
		try
		{
			if(key.equals("debug") && value.equals("true") && prev.equals("false"))
				NexusLoader.loadDebugConsole(false);
			else if(key.equals("detailedDebug"))
				NexusLoader.detailedDebug = Boolean.parseBoolean(value);
			else if(key.equals("detailedDebugToConsole"))
				NexusLoader.detailedDebugToConsole = Boolean.parseBoolean(value);
			else if(key.equals("logToFile"))
				NexusLoader.logToFile = Boolean.parseBoolean(value);
			else if(key.equals("maxWarnings"))
				EventWarnings.MAX_WARNINGS = Integer.parseInt(value);
			else if(key.equals("maxBuffsPerPage"))
				EventHtmlManager.BUFFS_PER_PAGE = Integer.parseInt(value);
			else if(key.equals("cbPage"))
				EventHtmlManager.BBS_COMMAND = value;
			
			else if(key.equals("allowVoicedCommands"))
				EventManager.ALLOW_VOICE_COMMANDS = Boolean.parseBoolean(value);
			else if(key.equals("registerVoicedCommand"))
				EventManager.REGISTER_VOICE_COMMAND = value;
			else if(key.equals("unregisterVoicedCommand"))
				EventManager.UNREGISTER_VOICE_COMMAND = value;
		}
		catch (Exception e)
		{
			NexusLoader.debug("Wrong value set for config " + key + " (value = " + value + ")");
		}

		saveGlobalConfig(config);
	}
	
	public void saveGlobalConfig(GlobalConfigModel config)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_globalconfigs VALUES (?,?,?,?,?)");
			
			statement.setString(1, config.getCategory());
			statement.setString(2, config.getKey());
			statement.setString(3, config.getDesc());
			statement.setString(4, config.getValue());
			statement.setInt(5, config.getInputType());
			
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
	
	public GlobalConfigModel getGlobalConfig(String type, String key)
	{
		if(type == null)
		{
			for(List<GlobalConfigModel> list : _globalConfigs.values())
			{
				for(GlobalConfigModel gc : list)
				{
					if(gc.getKey().equals(key))
					{
						return gc;
					}
				}
			}
			return null;
		}
		else
		{
			for(GlobalConfigModel gc : _globalConfigs.get(type))
			{
				if(gc.getKey().equals(key))
				{
					return gc;
				}
			}
			return null;
		}
	}
	
	public static EventConfig getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventConfig _instance = new EventConfig();
	}
}
