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
package cz.nxs.events.engine.base;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.events.Battlefield;
import cz.nxs.events.engine.main.events.CaptureTheFlag;
import cz.nxs.events.engine.main.events.Deathmatch;
import cz.nxs.events.engine.main.events.Domination;
import cz.nxs.events.engine.main.events.HuntingGrounds;
import cz.nxs.events.engine.main.events.LastManStanding;
import cz.nxs.events.engine.main.events.LuckyChests;
import cz.nxs.events.engine.main.events.MassDomination;
import cz.nxs.events.engine.main.events.Mutant;
import cz.nxs.events.engine.main.events.TeamVsTeam;
import cz.nxs.events.engine.main.events.TreasureHunt;
import cz.nxs.events.engine.main.events.TreasureHuntPvp;
import cz.nxs.events.engine.main.events.VIPTeamVsTeam;
import cz.nxs.events.engine.main.events.Zombies;
import cz.nxs.events.engine.mini.events.KoreanManager;
import cz.nxs.events.engine.mini.events.MiniTvTManager;
import cz.nxs.events.engine.mini.events.OnevsOneManager;
import cz.nxs.events.engine.mini.events.PartyvsPartyManager;
import javolution.util.FastList;

/**
 * @author hNoke
 * - stores main events identificator - EventType
 * - loads events
 */
public enum  EventType
{
	// Null event for maps, that hasn't been given a map
	Unassigned(0, "", "", Category.MainTeam, true, false),
	
	// Events finised and ought to be working correctly
	TvT(1, "TvT", "Team vs Team",  Category.MainTeam, true, false, TeamVsTeam.class),
	CTF(2, "CTF", "Capture the Flag",  Category.MainTeam, true, false, CaptureTheFlag.class),
	Domination(3, "Domination", "Domination",  Category.MainTeam, true, false, Domination.class),
	MassDomination(4, "MassDom", "Mass Domination",  Category.MainTeam, true, false, MassDomination.class),
	DM(5, "DM", "Deathmatch",  Category.MainFFA, true, false, Deathmatch.class),
	LastMan(6, "LastMan", "Last Man Standing",  Category.MainFFA, true, false, LastManStanding.class),
	TvTAdv(7, "TvTAdv", "TvT Advanced",  Category.MainTeam, true, false, VIPTeamVsTeam.class),
	LuckyChests(8, "Chests", "Lucky Chests",  Category.MainFFA, true, false, LuckyChests.class),
	Zombies(9, "Zombies", "Zombies",  Category.MainTeam, true, false, Zombies.class),
	Mutant(10, "Mutant", "Mutant",  Category.MainTeam, true, false, Mutant.class),
	TreasureHunt(11, "THunt", "Treasure Hunt",  Category.MainTeam, true, false, TreasureHunt.class),
	TreasureHuntPvp(12, "THuntPvP", "Treasure Hunt PvP",  Category.MainTeam, true, false, TreasureHuntPvp.class),
	HuntingGround(13, "HuntGround", "Hunting Grounds",  Category.MainTeam, true, false, HuntingGrounds.class),
	Battlefields(14, "Battlefields", "Battlefields",  Category.MainTeam, true, false, Battlefield.class),
	Commanders(14, "Commanders", "Commanders",  Category.MainTeam, true, false),
	
	// Main events to be done
	BombFight(15, "Bomb", "Bomb Fight",  Category.MainTeam, true, false),
	RussianRoulette(16, "Russian", "Russian Roulette",  Category.MainTeam, true, false),
	Simon(17, "Simon", "Simon Says",  Category.MainTeam, true, false),
	//VIP(16, "VIP", "VIP", true, true, false, -1),
	
	// Mini events finished
	Classic_1v1(50, "1v1", "Single players fights", Category.Mini, true, false, OnevsOneManager.class),
	PartyvsParty(51, "PTvsPT", "Party fights", Category.Mini, true, false, PartyvsPartyManager.class),
	Korean(52, "Korean", "Korean Style", Category.Mini, true, false, KoreanManager.class),
	MiniTvT(53, "MiniTvT", "Mini TvT", Category.Mini, true, true, MiniTvTManager.class),

	// Mini events to be finished
	LMS(54, "LMS", "Last Man", Category.Mini, true, false),
	LTS(55, "LTS", "Last Team", Category.Mini, true, false),
	Classic_2v2(56, "2v2", "2v2 event", Category.Mini, true, false),
	Tournament(57, "TournamentManager", "TournamentManager", Category.Mini, false, false),
	Underground_Coliseum(58, "UC", "Tower Crush", Category.Mini, true, false),
	Hitman(59, "Hitman", "Hitman", Category.Mini, false, false),
	RBHunt(60, "RBH", "Raid Hunt", Category.Mini, true, false),
	SurvivalArena(61, "Survival", "Survival Arena", Category.Mini, true, true),
	
	PvpZone(62, "Zone", "PvP Zone", Category.Global, true, true),
	
	;

	private int _order;
	private Category _category;
	private String _shortName;
	private String _longName;
	private boolean _allowEdits;
	private boolean _allowConfig;
	
	private Class <? extends Event> eventClass;
	
	private EventType(int order, String shortName, String longName, Category category, boolean allowEdits, boolean allowConfig, Class <? extends Event> eventClass)
	{
		_order = order;
		
		_category = category;
		_shortName = shortName;
		_longName = longName;
		
		_allowEdits = allowEdits;
		_allowConfig = allowConfig;
		
		this.eventClass = eventClass;
	}
	
	private EventType(int order, String shortName, String longName, Category category, boolean allowEdits, boolean allowConfig)
	{
		this(order, shortName, longName, category, allowEdits, allowConfig, null);
	}
	
	public Event loadEvent(MainEventManager manager)
	{
		if(eventClass != null)
		{
			try 
			{
				Constructor<? extends Event> constructor;
				
				if(isRegularEvent())
				{
					constructor = eventClass.getConstructor(EventType.class, MainEventManager.class);
					
					if(constructor == null)
					{
						NexusLoader.debug("Wrong constructor for event " + getAltTitle() + ".", Level.SEVERE);
						return null;
					}
					
					return constructor.newInstance(this, manager);
				}
				
				// Mini events are loaded from databases (EventModes engine)
				/*else if(isMiniEvent())
				{
					constructor = eventClass.getConstructor(EventType.class);
					return constructor.newInstance(this);
				}*/
			} 
			catch (Exception e) 
			{
				System.out.println(getAltTitle() + " event load error");
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		return null;
	}
	
	/** same as getOrder() */
	public int getId()
	{
		return getOrder();
	}
	
	/** same as getId() */
	public int getOrder()
	{
		return _order;
	}
	
	public int getMainEventId()
	{
		return 0;
	}
	
	public boolean isRegularEvent()
	{
		return _category == Category.MainTeam || _category == Category.MainFFA;
	}
	
	public boolean isMiniEvent()
	{
		return _category == Category.Mini;
	}
	
	public boolean isGlobalEvent()
	{
		return _category == Category.Global;
	}
	
	public boolean isFFAEvent()
	{
		return _category == Category.MainFFA;
	}
	
	public Category getCategory()
	{
		return _category;
	}
	
	public boolean allowConfig()
	{
		return _allowConfig;
	}
	
	public boolean allowEdits()
	{
		return _allowEdits;
	}
	
	public String getAltTitle()
	{
		return _shortName;
	}
	
	public String getHtmlTitle()
	{
		return _longName;
	}
	
	public static EventType getById(int id)
	{
		for(EventType t : values())
		{
			if(t.getId() == id)
				return t;
		}
		return Unassigned;
	}

	public static EventType getType(String value)
	{
		for(EventType t : values())
		{
			if(t.toString().equalsIgnoreCase(value) || t.getAltTitle().equalsIgnoreCase(value) || t.getHtmlTitle().equalsIgnoreCase(value) || String.valueOf(t.getId()).equals(value))
				return t;
		}
		return null;
	}
	
	public static EventType[] getMiniEvents()
	{
		List<EventType> types = new FastList<EventType>();
		for(EventType t : EventType.values())
		{
			types.add(t);
		}
		return types.toArray(new EventType[types.size()]);
	}
	
	public static EventType getEventByMainId(int id)
	{
		for(EventType t : EventType.values())
		{
			if(t.getMainEventId() == id)
				return t;
		}
		return null;
	}
	
	public static int lastGivenEvent = 0;
	public static EventType getNextRegularEvent()
	{
		EventType t = EventManager.getInstance().getMainEventManager().nextAvailableEvent(false);
		if(t == null)
		{
			NexusLoader.debug("getNextRegularEvent() returned null, all events are disabled (or have no maps).", Level.WARNING);
			return null;
		}
		
		lastGivenEvent = t.getId();
		return t;
		
		// OLD CODE which doesnt use admin panel
		/*
		int i = 0;
		int next = lastGivenEvent + 1;
		boolean wentFromStart = false;
		
		while(true)
		{
			if(i >= values().length)
			{
				// start looking for next event again from the start of the list
				if(!wentFromStart)
				{
					lastGivenEvent = EventType.values()[1].getId();
					next = lastGivenEvent;
					wentFromStart = true;
					i = 0;
				}
				else
				{
					break;
				}
			}
			
			for(EventType t : values())
			{
				if(t.getId() == next && t.isRegularEvent() && EventConfig.getInstance().isEventAllowed(t) && EventManager.getInstance().getMainEvent(t) != null && EventMapSystem.getInstance().getMapsCount(t) > 0)
				{
					lastGivenEvent = t.getId();
					return t;
				}
			}
			
			next++;
			i++;
		}

		NexusLoader.debug("getNextRegularEvent() returned null, all events are disabled (or have no maps).", Level.WARNING);
		return null;*/
	}
	
	public static EventType getNextRegularEvent(int lastId)
	{
		int i = 0;
		int next = lastId + 1;
		
		while(true)
		{
			if(i >= values().length)
				break;
			
			for(EventType t : values())
			{
				if(t.getId() == next && t.isRegularEvent() && EventConfig.getInstance().isEventAllowed(t) && EventManager.getInstance().getMainEvent(t) != null && EventMapSystem.getInstance().getMapsCount(t) > 0)
				{
					return t;
				}
			}
			
			next++;
			i++;
		}

		// just return first aviable event
		for(EventType t : values())
		{
			if(t.isRegularEvent() && EventConfig.getInstance().isEventAllowed(t) && EventManager.getInstance().getMainEvent(t) != null && EventMapSystem.getInstance().getMapsCount(t) > 0)
			{
				return t;
			}
		}
		
		NexusLoader.debug("getNextRegularEvent(int lastId) returned null, all events are DISABLED (or have no maps).", Level.WARNING);
		return null;
	}
	
	public enum Category
	{
		MainTeam, // Team based main events
		MainFFA,  // FFA main events
		Mini, 	  // Mini events
		Global      // Global events
	}
}
