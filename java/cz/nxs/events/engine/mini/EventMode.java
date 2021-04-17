package cz.nxs.events.engine.mini;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.ScheduleInfo.RunTime;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;

/**
 * @author hNoke
 * each Mini event can have more 'copies' (=EventModes) of itself with different configs (Features)
 */
public class EventMode implements Runnable
{
	@SuppressWarnings("unused")
	private static final Logger _log = Logger.getLogger(EventMode.class.getName());
	
	public enum FeatureType
	{
		// All mini events
		Level, ItemGrades, Enchant, Items, Delays, TimeLimit, Skills, Buffer, StrenghtChecks,
		
		// Round-based mini events
		Rounds, 
		
		// Events with multi-team support
		TeamsAmmount, 
		
		// Team-based events
		TeamSize,
	}
	
	public enum FeatureCategory
	{
		Configs, Items, Players
	}
	
	private EventType _event;
	private boolean _gmAllowed;
	private String _name;
	private String _visibleName;
	private int _npcId;
	private List<AbstractFeature> _features;
	private FastList<Integer> _disallowedMaps;
	private ScheduleInfo _scheduleInfo;
	
	private boolean _running;
	private ScheduledFuture<?> _future;
	
	public EventMode(EventType event)
	{
		_event = event;
		_name = "Default"; // default value
		_npcId = 0;
		_visibleName = _name;
		_features = new FastList<AbstractFeature>();
		_disallowedMaps = new FastList<Integer>();
		_scheduleInfo = new ScheduleInfo(_event, _name);
		
		refreshScheduler();
	}
	
	@Override
	public void run()
	{
		if(_running)
		{
			_running = false;
			
			MiniEventManager manager = EventManager.getInstance().getMiniEvent(_event, getModeId());
			if(manager != null)
				manager.cleanMe(false);
			
			scheduleRun();
		}
		else
		{
			_running = true;
			scheduleStop();
		}
	}
	
	public void refreshScheduler()
	{
		if(isNonstopRun())
		{
			_running = true;
			return;
		}
		
		if(_running)
		{
			boolean running = false;
			for(RunTime time : _scheduleInfo.getTimes().values())
			{
				if(time.isActual())
				{
					running = true;
					run();
				}
			}
			
			if(running)
				scheduleStop();
			else
				run();
		}
		else
		{
			boolean running = false;
			for(RunTime time : _scheduleInfo.getTimes().values())
			{
				if(time.isActual())
				{
					running = true;
					run();
				}
			}
			
			if(!running)
				scheduleRun();
		}
	}
	
	public void scheduleRun()
	{
		long runTime = _scheduleInfo.getNextStart(false);
		
		if(!isNonstopRun() && runTime > -1)
			_future = CallBack.getInstance().getOut().scheduleGeneral(this, runTime);
		else
			_running = true;
	}
	
	public void scheduleStop()
	{
		long endTime = _scheduleInfo.getEnd(false);
		
		if(!isNonstopRun() && endTime != -1)
		{
			_future = CallBack.getInstance().getOut().scheduleGeneral(this, endTime);
		}
	}
	
	public boolean isNonstopRun()
	{
		return _scheduleInfo.isNonstopRun();
	}
	
	public List<AbstractFeature> getFeatures()
	{
		return _features;
	}
	
	public void addFeature(PlayerEventInfo gm, FeatureType type, String parameters)
	{
		Constructor<?> _constructor = null;
		AbstractFeature feature = null;
		
		Class<?>[] classParams = {EventType.class, PlayerEventInfo.class, String.class};
		try
		{
			_constructor = Class.forName("cz.nxs.events.engine.mini.features." + type.toString() + "Feature").getConstructor(classParams);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		
		try
		{
			Object[] objectParams = {_event, gm, parameters};
			
			Object tmp = _constructor.newInstance(objectParams);
			feature = (AbstractFeature) tmp;
			
			_features.add(feature);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public void addFeature(AbstractFeature feature)
	{
		_features.add(feature);
	}
	
	public boolean checkPlayer(PlayerEventInfo player)
	{
		for(AbstractFeature feature : _features)
		{
			if(!feature.checkPlayer(player))
				return false;
		}
		return true;
	}
	
	public long getFuture()
	{
		return _future == null ? -1 : _future.getDelay(TimeUnit.MILLISECONDS);
	}
	
	public FastList<Integer> getDisMaps()
	{
		return _disallowedMaps;
	}
	
	public String getModeName()
	{
		return _name;
	}
	
	public String getVisibleName()
	{
		if(_visibleName == null || _visibleName.length() == 0)
			return _name;
		return _visibleName;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public void setNpcId(int id)
	{
		_npcId = id;
	}
	
	public void setVisibleName(String name)
	{
		_visibleName = name;
	}
	
	public void setModeName(String s)
	{
		_name = s;
	}
	
	public boolean isAllowed()
	{
		return _gmAllowed;
	}
	
	public boolean isRunning()
	{
		return _running;
	}
	
	public void setAllowed(boolean b)
	{
		_gmAllowed = b;
	}
	
	public ScheduleInfo getScheduleInfo()
	{
		return _scheduleInfo;
	}
	
	public int getModeId()
	{
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_event).entrySet())
		{
			if(e.getValue().getMode().getModeName().equals(getModeName()))
				return e.getKey();
		}
		
		return 0;
	}
}
