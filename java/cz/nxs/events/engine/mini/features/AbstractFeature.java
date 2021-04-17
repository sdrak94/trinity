package cz.nxs.events.engine.mini.features;

import java.util.List;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;
import javolution.text.TextBuilder;
import javolution.util.FastList;

/**
 * @author hNoke
 * a mother class of all EventMode's configs (=Features)
 */
public abstract class AbstractFeature
{
	protected EventType _event;
	protected String _params;
	
	//protected FastMap<String, String> _configs = new FastMap<String, String>();
	protected List<FeatureConfig> _configs = new FastList<FeatureConfig>();
	
	public abstract FeatureType getType();
	
	protected abstract void initValues();
	
	public AbstractFeature(EventType event)
	{
		_event = event;
	}
	
	// config, description
	public abstract boolean checkPlayer(PlayerEventInfo player);
	
	protected static String[] splitParams(String params)
	{
		return params.split(",");
	}
	
	public String getParams()
	{
		return _params;
	}
	
	protected void addConfig(String name, String desc, int inputFormType)
	{
		_configs.add(new FeatureConfig(name, desc, inputFormType));
	}
	
	public FeatureConfig getConfig(String name)
	{
		for(FeatureConfig c : _configs)
		{
			if(c.name.equals(name))
				return c;
		}
		return null;
	}
	
	public class FeatureConfig
	{
		public String name, desc;
		public int inputFormType;
		
		protected FeatureConfig(String name, String desc, int inputFormType)
		{
			this.name = name;
			this.desc = desc;
			this.inputFormType = inputFormType;
		}
	}
	
	public void setValueFor(String configName, String value)
	{
		String[] splitted = _params.split(",");
		int index = 0;
		for (FeatureConfig c : this._configs)
		{
			if (c.name.equals(configName))
				break;
			index++;
		}
		if (splitted.length < index)
			return;
		splitted[index] = value;
		TextBuilder tb = new TextBuilder();
		for (String s : splitted)
			tb.append(s + ",");
		String result = tb.toString();
		_params = result.substring(0, result.length() - 1);
		initValues();
	}
	
	public String getValueFor(String configName)
	{
		String[] splitted = _params.split(",");
		
		int index = 0;
		for(FeatureConfig c : _configs)
		{
			if(c.name.equals(configName))
				break;
			
			index++;
		}
		
		if(splitted.length < index)
		{
			return "N/A";
		}
		else
		{
			return splitted[index];
		}
	}
	
	public List<FeatureConfig> getConfigs()
	{
		return _configs;
	}
}
