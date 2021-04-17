package cz.nxs.events.engine.main.base;

import java.util.Map.Entry;

import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.interf.delegate.InstanceData;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - represents one MainEventInstanceType template
 */
public class MainEventInstanceType
{
	private final int _id;
	private String _name;
	private final String _visibleName;
	
	private AbstractMainEvent _event;
	
	private String _params;
	
	private InstanceData _tempInstance;
	
	// configs
	private final FastMap<String, ConfigModel> _configs;
	
	private int _min, _max, _rate;
	
	public MainEventInstanceType(int id, AbstractMainEvent event, String name, String visibleName, String params)
	{
		_id = id;
		_name = name;
		_visibleName = visibleName;
		
		_event = event;
		_params = params == null ? "" : params;
		
		_configs = new FastMap<String, ConfigModel>();
	}
	
	public MainEventInstanceType(int id, AbstractMainEvent event, String name, String visibleName, String params, int min, int max, int rate)
	{
		_id = id;
		_name = name;
		_visibleName = visibleName;
		
		_event = event;
		_params = params == null ? "" : params;
		
		_configs = new FastMap<String, ConfigModel>();
		
		_min = min;
		_max = max;
		_rate = rate;
	}
	
	@Deprecated
	public int getMinPlayers()
	{
		return _min;
	}
	
	@Deprecated
	public int getMaxPlayers()
	{
		return _max;
	}
	
	public void loadConfigs()
	{
		if(_params.length() == 0)
			return;
		
		String name;
		String value;
		for(String criteria : _params.split(";"))
		{
			name = criteria.split(":")[0];
			
			if(criteria.split(":").length > 1)
				value = criteria.split(":")[1];
			else
				value = "";
			
			setConfig(name, value, false);
		}
	}
	
	public String encodeParams()
	{
		TextBuilder tb = new TextBuilder();
		
		for(Entry<String, ConfigModel> e : _configs.entrySet())
		{
			tb.append(e.getValue().encode());
		}
		
		String result = tb.toString();
		return result.length() > 0 ? result.substring(0, result.length() - 1) : result;
	}
	
	public void addDefaultConfig(String name, String value, String desc, String defaultVal, InputType input, String inputParams)
	{
		addParam(name, value, desc, defaultVal, input, inputParams, true);
	}
	
	public void addParam(String name, String value, String desc, String defaultVal, InputType input, String inputParams, boolean override)
	{
		if(_configs.containsKey(name) && !override)
			return;
		
		ConfigModel config = new ConfigModel(name, value, desc, defaultVal, input, inputParams);
		_configs.put(name, config);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getVisibleName()
	{
		return _visibleName;
	}
	
	public AbstractMainEvent getEvent()
	{
		return _event;
	}
	
	public String getParams()
	{
		return _params;
	}
	
	public void setParams(String p)
	{
		_params = p;
	}
	
	public InstanceData getInstance()
	{
		return _tempInstance;
	}
	
	public void setInstance(InstanceData instance)
	{
		_tempInstance = instance;
	}
	
	public FastMap<String, ConfigModel> getConfigs()
	{
		return _configs;
	}
	
	public void setConfig(String name, String value, boolean addToValue)
	{
		if(!_configs.containsKey(name))
			return;
		
		if(!addToValue)
			_configs.get(name).setValue(value);
		else
			_configs.get(name).addToValue(value);
	}
	
	public String getConfig(String name)
	{
		return _configs.get(name).getValue();
	}
	
	public int getConfigInt(String name)
	{
		String v = getConfig(name);
		try
		{
			return Integer.parseInt(v);
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	public boolean getConfigBoolean(String name)
	{
		String v = getConfig(name);
		try
		{
			return Boolean.parseBoolean(v);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	@Deprecated
	public int getTempRate()
	{
		return _rate;
	}
	
	public int getStrenghtRate()
	{
		return getConfigInt("strenghtRate");
	}
}