package cz.nxs.events.engine.main.events.template;

import cz.nxs.events.EventGame;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.base.MainEventInstanceType;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 * template used to create team based events; 
 * with some modifications, it can also be used to create a FFA event (refer to Deathmatch, for example)
 * 
 * places, that require your code, are marked as '///'
 */
public class FFAEventTemplate extends AbstractMainEvent
{
	/**
	 * Example of storing one event instance's data
	 * This style is used over all the engine, good usage of this is shown in CTF event (FlagData class)
	 * <br><b>call for this CustomEventData instance by calling <u>getEventData(instance ID)</u></b>
	 */
	private class CustomEventData extends AbstractEventData
	{
		//public int intVar;
		
		//you may use this NPC in your spawnStuff methods
		//public NpcData npcVar;
		
		//this playerVar could be for example some important temporary player, such as VIP, flag holder, etc.
		//public PlayerEventInfo playerVar;
		
		//eg. FlagReturnTask in CTF event
		//public SomeScheduledTask returnTask;
		
		public CustomEventData(int instance)
		{
			super(instance);
			
			// init vars
		}
		
		/*private void setIntVar(int i)
		{
			intVar = i;
		}*/
	}
	
	@Override
	protected CustomEventData createEventData(int instanceId)
	{
		return new CustomEventData(instanceId);
	}
	
	@Override
	protected CustomEventInstance createEventInstance(InstanceData instance)
	{
		return new CustomEventInstance(instance);
	}
	
	@Override
	protected CustomEventData getEventData(int instance)
	{
		return _matches.get(instance)._data;
	}
	
	/**
	 * This class manages one instance of this event.
	 * <br> The event doesn't end until at least one EventInstance isActive (refer to _runningInstances variable).
	 * <br> In some cases, one instance may be needed to be aborted earlier than other instances, for example when it's players disconnect
	 * or some round-based events (Last Man Standing), that end after certain ammount of rounds passed, require it too. 
	 * So there's a endInstance method for it.
	 */
	private class CustomEventInstance extends AbstractEventInstance
	{
		protected EventState _state;
		
		protected CustomEventData _data;
		
		protected CustomEventInstance(InstanceData instance)
		{
			super(instance);
			
			_state = EventState.START;
			
			_data = createEventData(instance.getId());
		}
		
		protected void setNextState(EventState state) {_state = state;}
		
		@Override
		public boolean isActive() { return _state != EventState.INACTIVE;} 
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				switch (_state)
				{
					// teleporting state; players will be teleported to the event, prepared for it (buffs, title, etc.) 
					// NPCs will be spawned and players will get forced sit down
					// this state takes 10 seconds
					case START:
					{
						// Check if the players didn't disconnect during the teleporting phase
						if(checkPlayers(_instance.getId()))
						{
							//Teleports players to the event - chooses Regular spawn type
							//Parameter FFA - if true, event doesn't look at spawn's team
							// (put true to Free for All event types (Deathmatch, etc.) with no teams)
							teleportPlayers(_instance.getId(), SpawnType.Regular, false);
							
							//Prepares players for the event:
							// currently makes players' titles to be getting updated during the event
							setupTitles(_instance.getId());
							
							/// Put all NPC spawning actions here (to the method)
							spawnStuff(_instance.getId());
							
							//Force players to sit down during the teleporting
							forceSitAll(_instance.getId());
							
							//Next state for next run() will be FIGHT
							setNextState(EventState.FIGHT);
							
							//Telelporting state takes 10 seconds
							scheduleNextTask(10000);
						}
						
						break;
					}
					// main and usually longest state of the event
					// players are forced to stand up and fight
					// this state takes _manager.getRunTime() seconds
					case FIGHT:
					{
						//Players stand up 10 seconds after teleported
						forceStandAll(_instance.getId());
						
						//Next state for next run() call will be END
						setNextState(EventState.END);

						//Schedules the end of the event
						_clock.startClock(_manager.getRunTime());
						
						break;
					}
					// state after runtime clock passes
					// unspawns npcs; marks this EventInstance as ended
					case END:
					{
						_clock.setTime(0, true);


						/// Put all npc unspawning actions here
						unspawnStuff(_instance.getId());

						setNextState(EventState.INACTIVE);
						
						//If this instance has ended too early due to too many disconnects, etc.:
						//-> abort event only for this instance
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), getInt("scoreForReward"), getInt("killsForReward"));
							
							clearEvent(_instance.getId());
						}
							
						break;
					}
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace();

				//Aborts the event and sends warning message
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}

	private enum EventState
	{
		START, FIGHT, END, TELEPORT, INACTIVE
	}

	private FastMap<Integer, CustomEventInstance> _matches;
	
	private boolean _waweRespawn;
	private int _teamsCount;

	public FFAEventTemplate(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		//Sets all reward types (possible positions in the end of the event for players); visible in Admin panel
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.OnKill });
		
		//Adds configs to the event
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system.", InputType.Boolean));
		
		addConfig(new ConfigModel("createParties", "true", "Put 'True' if you want this event to automatically create parties for players in each team.", InputType.Boolean));
		addConfig(new ConfigModel("maxPartySize", "10", "The maximum size of party, that can be created. Works only if <font color=LEVEL>createParties</font> is true."));
		
		addConfig(new ConfigModel("teamsCount", "2", "The ammount of teams in the event. Max is 5."));
		
		//Adds configs for InstanceTypes
		addInstanceTypeConfig(new ConfigModel("teamsCount", "2", "You may specify the count of teams only for this instance. This config overrides event's default teams ammount."));
	}
	
	// Called right after Registration state ends; the players haven't been teleported yet
	// Used to store configs and initialize and start systems related to events
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		//Storing some important config values before event start, these configs cannot change during the event, or something can go wrong 
		_waweRespawn = getBoolean("waweRespawn"); 
		
		//Inits Wawe Spawn system, if allowed
		if(_waweRespawn)
			initWaweRespawns(getInt("resDelay"));
		
		//The count of running instances (InstanceTypes)
		_runningInstances = 0;
	}
	
	// Called for every InstanceType that will run on the event
	// Chooses count and creates EventTeams for this instance
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		_teamsCount = type.getConfigInt("teamsCount");
		
		if(_teamsCount < 2 || _teamsCount > 5) // wrong value for instance, use event's default
			_teamsCount = getInt("teamsCount");
		
		if(_teamsCount < 2 || _teamsCount > 5)
			_teamsCount = 2;
		
		createTeams(_teamsCount, type.getInstance().getId());
		
		return _teamsCount;
	}
	
	// Called 10 seconds after teleport, when players stand up and stop being paralyzed
	// Divides players into teams; creates parties; initializes and runs instances
	@Override
	public void runEvent()
	{
		// returns false if no instance has been created - simply not enought players to create at least one instance (based on given configs)
		if(!dividePlayers())
		{
			clearEvent();
			return;
		}
		
		// creates and launches EventInstance objects, each representing one event's instance
		CustomEventInstance match;
		_matches = new FastMap<Integer, CustomEventInstance>();
		for(InstanceData instance : _instances)
		{
			match = new CustomEventInstance(instance);
			_matches.put(instance.getId(), match);
			
			_runningInstances ++;
			
			match.scheduleNextTask(0);
		}
	}
	
	// Called when the event ends and before players get teleported back to their previous positions
	// Used to reward players
	@Override
	public void onEventEnd()
	{
		// gets min stats required to obtain the reward
		int minKills = getInt("killsForReward");
		int minScore = getInt("scoreForReward");
		rewardAllTeams(-1, minScore, minKills);
	}
	
	// Called when an instance ends
	// Ends the event if all instances ended
	@Override
	protected synchronized boolean instanceEnded()
	{
		_runningInstances--;
		
		if(_runningInstances == 0)
		{
		
			_manager.end();
			return true;
		}
		else 
			return false;
	}
	
	// Called when an instance ends earlier - due to players disconnected - if the instance cannot continue (other instances may be fine)
	// canBeAborted - put true to allow this instance's players to get unregistered from the event and teleported back, if other instances are still running. It will call clearEvent(instance's ID)
	// canRewardIfAborted - put true to reward players if the instance has ended (teleporting players back, etc.) - it will call rewardAllPlayers(instance ID)
	// forceNotReward - put true to make this instance's players not receive any final rewards under any circumstances. Useful in cases when the instance cannnot even start or is aborted very early (due to lack of players for example)
	@Override
	protected synchronized void endInstance(int instance, boolean canBeAborted, boolean canRewardIfAborted, boolean forceNotReward)
	{
		if(forceNotReward)
			_matches.get(instance).forceNotRewardThisInstance();
	
		_matches.get(instance).setNextState(EventState.END);
		if(canBeAborted)
			_matches.get(instance).setCanBeAborted();
		if(canRewardIfAborted)
			_matches.get(instance).setCanRewardIfAborted();
		_matches.get(instance).scheduleNextTask(0);
	}

	// Returns the text shown in all players scorebar (text over player's screen). Changes dynamically each clockTick.
	@Override
	protected String getScorebar(int instance)
	{
		final int count = _teams.get(instance).size();
		
		TextBuilder tb = new TextBuilder();
		
		for(EventTeam team : _teams.get(instance).values())
		{
			if(count <= 4)
				tb.append(team.getTeamName() + ": " + team.getScore() + "  ");
			else
				tb.append(team.getTeamName().substring(0, 1) + ": " + team.getScore() + "  ");
		}
		
		if(count <= 3)
			tb.append(LanguageEngine.getMsg("event_scorebar_time", _matches.get(instance).getClock().getTime()));
		
		return tb.toString();
	}

	// Called mostly when player dies or when he gets teleported to the event for the first time
	// Returns the new title for the player provided
	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(pi.isAfk())
			return "AFK";
		
		return "Kills: " + getPlayerData(pi).getScore() + " Deaths: " + getPlayerData(pi).getDeaths();
	}
	
	// Called when the player provided kills the target; the target doesn't have to be a player, it can be a mob, etc.
	// Raise killer's kill count and statistics
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		// target isn't L2Playable (player, summon, pet,..)
		if(target.getEventInfo() == null)
			return;
		
		if (player.getTeamId() != target.getEventInfo().getTeamId())
		{
			giveOnKillReward(player);
			
			// update player's team's stats
			player.getEventTeam().raiseScore(1);
			player.getEventTeam().raiseKills(1);
			
			// update player's own stats
			getPlayerData(player).raiseScore(1);
			getPlayerData(player).raiseKills(1);
			getPlayerData(player).raiseSpree(1);
			
			// update player's title
			if(player.isTitleUpdated())
			{
				player.setTitle(getTitle(player), true);
				player.broadcastTitleInfo();
			}
			
			CallbackManager.getInstance().playerKills(getEventType(), player, target.getEventInfo());
			
			// update Stats table
			setScoreStats(player, getPlayerData(player).getScore());
			setKillsStats(player, getPlayerData(player).getKills());
		}
	}

	// Called when a player dies
	// Raise player's death count; schedules his resurrection
	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		// update player's deaths
		getPlayerData(player).raiseDeaths(1);
		
		// update Stats table
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		// schedules respawnPlayer method for this player
		if(_waweRespawn)
			_waweScheduler.addPlayer(player);
		else
			scheduleRevive(player, getInt("resDelay") * 1000);
	}

	void spawnStuff(int instanceId)
	{
		/// Spawn your NPCs here	
	}

	protected void unspawnStuff(int instanceId)
	{
		/// unspawn your NPCs here	
	}
	
	// Called only when the player gets teleported for the first time
	// Creates an EventPlayerData, used to store player's event statistics
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		EventPlayerData d = new CustomEventPlayerData(player, this);
		return d;
	}

	// Returns casted EventPlayerData
	@Override
	public CustomEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (CustomEventPlayerData) player.getEventData();
	}
	
	// clears the Event in provided Instance
	// Clears players from effects obtained during the event, deletes parties, restores their original data and teleports them back
	// in some other events, it can unspawn NPCs, etc.
	// if instanceId == 0 - clears ALL instances
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		try
		{
			// Abort instance(s) (again) for sure
			if(_matches != null)
			{
				for(CustomEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();


						/// Put all cleaning methods for this instance here
						// For example removing flags from players; unspawning NPCs etc. it's here again in case the event got aborted (by GM) or didn't end properly
						unspawnStuff(instanceId);


						
	 
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		// Proceeds players from given instance(s)
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			if(!player.isOnline())
				continue;
			
			if(player.isParalyzed())
				player.setIsParalyzed(false);
			
			if(player.isImmobilized())
				player.unroot();
			
			// move him back to universe 
			player.setInstanceId(0);
			
			if(_removeBuffsOnEnd)
				player.removeBuffs();
			
			// restore player's original data (title, name, color, etc.)
			player.restoreData();
			
			// teleport back to his previous location
			player.teleport(player.getOrigLoc(), 0, true, 0);
			player.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
			
			// leave party if needed
			if (player.getParty() != null)
			{
				PartyData party = player.getParty();
				party.removePartyMember(player);
			}
			
			player.broadcastUserInfo();
		}
		
		// Unregisters players; cleans Event Managers, etc.
		clearPlayers(true, instanceId);
	}
	
	// Called after the event ends; if GM aborts the event, etc.
	// Clears all instances
	@Override
	public synchronized void clearEvent()
	{
		// 0 = all instances
		clearEvent(0);
	}
	
	// Called some time after onDie was called, to respawn the player
	@Override
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		// Returns a new EventSpawn of type Regular and the team same as player's team
		EventSpawn spawn = getSpawn(SpawnType.Regular, pi.getTeamId());

		if(spawn != null)
		{
			// gets the Loc (location) of the EventSpawn
			Loc loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());

			// adds a random radius to the Loc; if the EventSpawn has any
			loc.addRadius(spawn.getRadius());
			
			// teleports to player to the Loc (resurrection, cleaning buffs, etc. is handled in this method)
			pi.teleport(loc, 0, true, instance);
			
			// tells the player what happened if he didn't noticed
			pi.sendMessage(LanguageEngine.getMsg("event_respawned"));
		}
		else // no spawn found - shouldn't happen if you don't have extremely stupid GMs
		{
			//abortEvent("An error occured with Event Map."); //can't remember why did I comment this out, but i know it was important
			debug("Error on respawnPlayer - no spawn type REGULAR, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}

	// Called right before deleting L2PcInstance after a player disconnected from the server
	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		if(player.isOnline())
		{
			

			/// Remove neccessary stuff from this player, he has disconnected
			/// Example if this player was holding a flag or he was somehow important (VIP event)


		}

		// checks for aborting the event due to lack of players, etc. are handled in super class
		super.onDisconnect(player);
	}
	
	// Called in AbstractMainEvent.onDisconnect; when a player disconnects from the event
	// You may put your own checks here
	@Override
	protected boolean checkIfEventCanContinue(int instanceId, PlayerEventInfo disconnectedPlayer)
	{
		return super.checkIfEventCanContinue(instanceId, disconnectedPlayer);
	}

	// Called each tick (1 tick = 1 second)
	@Override
	protected void clockTick()
	{
		// this has it's use for example in Domination event, calculating who owns the zone each tick
		// some usage examples are in Last Man Standing, where we use it to make announcements
	}

	// Called when a player sends any chat message
	@Override
	public boolean onSay(PlayerEventInfo player, String text, int channel)
	{
		// this is useful for example in Simon Says event, where the player is writing text

		return true;
	}
	
	// Called when a player requests talking to NPC
	// return true if a HTML window has been shown by this engine, because you don't want any other HTML message from L2j Gameserver side shown to the player
	// return false if no html was shown by this engine, so a default HTML window by L2j side will be sent
	@Override
	public boolean onNpcAction(PlayerEventInfo player, NpcData npc)
	{
		// this was for example used in Capture the Flag event, checking when player clicks on NPCs (holders and flags)

		return false;
	}
	
	// Called when a player deals damage to a target
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		super.onDamageGive(cha, target, damage, isDOT);

		// used in for example in Zombies event, when a Zombie delas damage to a player, the player dies immediately
	}

	// Called when a player requests to cast a non-offensive skill to his target
	// return false if you want him to not allow him to do it
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		// Used in Deathmatch event - you can't support (heal, buff, etc.) anyone but you!

		return super.canSupport(player, target);
	}
	
	// Called when a player requests to attack/cast offensive skill to his target
	// return false if you want to not allow him to do it
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		// Used in Deathmatch event - you can attack everyone

		return super.canAttack(player, target);
	}

	// Called when a player requests to use item, not matter if the item is equipped or not
	// return false to not allow him to do it
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		// Can be useful in for example CTF - when a player equipped the Flag, he can not unequip it or use any other weapon item

		return super.canUseItem(player, item);
	}
	
	// Called when a player tries to destroy an item
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		// Can be useful in CTF - disallow player to delete the flag if he holds it

		return super.canDestroyItem(player, item);
	}
	
	// Called when a player tries to use an item
	@Override
	public void onItemUse(PlayerEventInfo player, ItemData item)
	{
		super.onItemUse(player, item);

		// Could be used when the player used some item that has no use in L2jSide but has use in this engine
		// ItemData supports deleting itself too
	}
	
	// Called when a player tries to use a skill
	// return false to disallow him to use this skill
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		// Could be useful if you want player disallow (some) skills

		return super.canUseSkill(player, skill);
	}
	
	// Called when a player used a skill (after he used it)
	@Override
	public void onSkillUse(PlayerEventInfo player, SkillData skill)
	{
		super.onSkillUse(player, skill);

		// No idea atm when this could be useful
	}
	
	
	// Called only in Admin panel menu, orientation time value for GMs to see when the event ends
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(CustomEventInstance match : _matches.values())
		{
			if(match.isActive())
				return match.getClock().getTime();
		}
				
		return "N/A";
	}
	
	@Override
	public int getTeamsCount()
	{
		return 1;
	}
	
	// Returns which EventSpawns are missing and required by this event in the provided map
	@Override
	public String getMissingSpawns(EventMap map)
	{
		if(!map.checkForSpawns(SpawnType.Regular, -1, 1))
			return addMissingSpawn(SpawnType.Regular, 0, 1);
		return "";
	}
	
	// This description is used in reg NPC HTML messages (visible to players)
	@Override
	public String getHtmlDescription()
	{
		if(_htmlDescription == null)
		{
			// change it here! (look at other events to get inspiration how to build informative descriptions based on current configs!)
			_htmlDescription = "No information about this event.";
		}
		return _htmlDescription;
	}
	
	@Override
	protected AbstractEventInstance getMatch(int instanceId)
	{
		return _matches.get(instanceId);
	}
	
	// Overriden EventPlayerData with data that can be used in your event
	// Example of usage: look inside of CTF event, class CTFEvnetPlayerData, that stores if this 'owner' player holds the flag
	// Call for casted player's EventPlayerData object by calling getEventData(PlayerEventInfo) in this event
	public class CustomEventPlayerData extends PvPEventPlayerData
	{
		
		
		/// Put your code here
		//private int newVariable;
		
		
		
		public CustomEventPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
		}
		
		/*public void setMyVariable(int b)
		{
			newVariable = b;
		}
		
		public int getMyVariable()
		{
			return newVariable;
		}*/
	}
}
