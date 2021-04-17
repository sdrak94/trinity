package cz.nxs.events.engine.mini;

import java.util.Map;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode.FeatureCategory;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - stores types of Features (configs for EventModes)
 */
public class FeatureBase
{
	private static FeatureBase _instance = new FeatureBase();
	public static FeatureBase getInstance()
	{
		return _instance;
	}
	
	public FeatureBase()
	{
		add(EventMode.FeatureType.Delays, "Delays", FeatureCategory.Configs, EventType.getMiniEvents(), "specifies all delays for this event.");
		add(EventMode.FeatureType.Enchant, "Enchant", FeatureCategory.Items, EventType.getMiniEvents(), "specifies all enchant related settings.");
		add(EventMode.FeatureType.ItemGrades, "Item Grades", FeatureCategory.Items, EventType.getMiniEvents(), "allows you to specify allowed item grades.");
		add(EventMode.FeatureType.Items, "Items", FeatureCategory.Items, EventType.getMiniEvents(), "specifies which items will be allowed and which disabled.");
		add(EventMode.FeatureType.Level, "Level", FeatureCategory.Players, EventType.getMiniEvents(), "specifies max/min level allowed to participate this mode");
		add(EventMode.FeatureType.TimeLimit, "Time Limit", FeatureCategory.Configs, EventType.getMiniEvents(), "specifies all time-based settings.");
		add(EventMode.FeatureType.Skills, "Skills", FeatureCategory.Players, EventType.getMiniEvents(), "specifies all skills-related settings.");
		add(EventMode.FeatureType.Buffer, "Buffer", FeatureCategory.Configs, EventType.getMiniEvents(), "specifies all buffs-related settings.");
		
		add(EventMode.FeatureType.Rounds, "Rounds", FeatureCategory.Configs, new EventType[] { EventType.Classic_1v1, EventType.PartyvsParty, EventType.MiniTvT }, "allows you to edit the ammount of rounds only for this mode.");
		add(EventMode.FeatureType.TeamsAmmount, "Teams Ammount", FeatureCategory.Configs, new EventType[] { EventType.Classic_1v1, EventType.PartyvsParty, EventType.MiniTvT }, "allows you to edit the ammount of teams only for this mode.");
		add(EventMode.FeatureType.TeamSize, "Team Size", FeatureCategory.Players, new EventType[] { EventType.Korean, EventType.PartyvsParty, EventType.MiniTvT }, "allows you to edit the ammount of players in one team only for this mode.");
		add(EventMode.FeatureType.StrenghtChecks, "Strenght Checks", FeatureCategory.Players, new EventType[] { EventType.Korean, EventType.PartyvsParty, EventType.Classic_1v1 }, "allows you to edit the automatic match making strenght difference checks");
	}
	
	private Map<FeatureType, FeatureInfo> _data = new FastMap<FeatureType, FeatureInfo>();
	
	public void add(FeatureType type, String visibleName, FeatureCategory cat, EventType[] events, String desc)
	{
		FeatureInfo info = new FeatureInfo(cat, visibleName, events, desc);
		_data.put(type, info);
	}
	
	public FeatureInfo get(FeatureType type)
	{
		return _data.get(type);
	}
	
	public class FeatureInfo
	{
		private FeatureCategory _category;
		private EventType[] _events;
		private String _desc;
		private String _visibleName;
		
		public FeatureInfo(FeatureCategory cat, String visName, EventType[] events, String desc)
		{
			_category = cat;
			_events = events;
			_desc = desc;
			_visibleName = visName;
		}
		
		public FeatureCategory getCategory()
		{
			return _category;
		}
		
		public String getVisibleName()
		{
			return _visibleName;
		}
		
		public String getDesc()
		{
			return _desc;
		}
		
		public boolean isForEvent(EventType event)
		{
			for(EventType t : _events)
			{
				if(t == event)
					return true;
			}
			return false;
		}
	}
}
