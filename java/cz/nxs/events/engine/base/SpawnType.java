package cz.nxs.events.engine.base;

import java.util.List;


/**
 * @author hNoke
 * enum that defines all types of EventSpawns and events, that are using them
 */
public enum SpawnType
{
	// Spawns for all events
	Regular("CD9F36", null, "Adds place where the players of team %TEAM% will be spawned."),	// classic spawn used to teleport players to
	Door("916406", new EventType[] { EventType.Classic_1v1, EventType.PartyvsParty, EventType.Korean, EventType.MiniTvT }, "Adds door to the event's instance."),		// call for getDoorId() to get door's ID
	Npc("FFFFFF", null, "Adds an NPC to the event with ID you specify."),	// spawns npcs
	Fence("878578", null, "Adds fence to the event."),		// defines fences in event
	Buffer("68AFB3", new EventType[] { EventType.Classic_1v1, EventType.PartyvsParty, EventType.Korean, EventType.MiniTvT }, "A buffer NPC will be spawned here during the event period."),		// defines buffer's spawnloc
	Spectator("FFFFFF", new EventType[] { EventType.Classic_1v1, EventType.PartyvsParty, EventType.Korean, EventType.MiniTvT }, "Defines an observation spot for spectators."),	// defines where the Spectator will be spawned
	MapGuard("FFFFFF", null, "Adds a map guard to the event's instance. Map guard kills everyone who gets near."),	// spawns aggresive guard
	Radar("FFFFFF", null, "Players from spawn's team will be guided to this location."),	// adds radar marker
	
	// Characteristic spawns for events
	Safe("5BB84B", new EventType[] { EventType.Korean }, "Players will stay in this loc during the safe preparation phase. Don't forget to put fences arround this spot."),
	Flag("867BC4", new EventType[] { EventType.CTF, EventType.Underground_Coliseum }, "Defines the position of a flag."),
	Zombie("7C9B59", new EventType[] { EventType.Zombies, EventType.Mutant }, "Defines where the zombies and mutants (re)spawn."),
	Monster("879555", new EventType[] { EventType.SurvivalArena }, ""),
	Boss("BE2C49", new EventType[] { EventType.RBHunt }, ""),
	Zone("68AFB3", new EventType[] { EventType.Domination, EventType.MassDomination, }, "Adds a Domination zone here. Teams have to get near to this place in order to score."),
	Chest("68AFB3", new EventType[] { EventType.LuckyChests, EventType.TreasureHunt, EventType.TreasureHuntPvp }, "Defines where the chests will be spawned."),
	Simon("68AFB3", new EventType[] { EventType.Simon }, "Spawns Simon the NPC here."),
	Russian("68AFB3", new EventType[] { EventType.RussianRoulette }, ""),
	Base("68AFB3", new EventType[] { EventType.Battlefields }, "Adds a conquerable base here. Teams have to get near to this place in order to score."),
	VIP("68AFB3", new EventType[] { EventType.TvTAdv }, "VIPs will be spawned in this spawn.");
	
	private String htmlColor;
	private EventType[] events;
	private String desc;
	private SpawnType(String htmlColor, EventType[] allowedEvents, String description)
	{
		this.htmlColor = htmlColor;
		this.events = allowedEvents;
		this.desc = description;
	}
	
	public String getHtmlColor()
	{
		return htmlColor;
	}
	
	public String getDefaultDesc()
	{
		return desc;
	}
	
	public boolean isForEvents(List<EventType> events)
	{
		if(this.events == null)
			return true;
		
		for(EventType t : events)
		{
			if(isForEvent(t))
				return true;
		}
		return false;
	}
	
	private boolean isForEvent(EventType type)
	{
		for(EventType t : events)
		{
			if(t.getId() == type.getId())
				return true;
		}
		return false;
	}
}
