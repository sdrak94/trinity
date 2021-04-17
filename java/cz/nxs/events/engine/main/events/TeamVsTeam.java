package cz.nxs.events.engine.main.events;

import cz.nxs.events.NexusLoader;
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
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.base.MainEventInstanceType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.PartyData;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class TeamVsTeam extends AbstractMainEvent
{
	protected class TvTEventData extends AbstractEventData
	{
		public TvTEventData(int instance)
		{
			super(instance);
		}
	}
	
	protected class TvTEventInstance extends AbstractEventInstance
	{
		protected EventState _state;
		
		protected TvTEventData _data;
		
		protected TvTEventInstance(InstanceData instance)
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
				/**/ if(NexusLoader.detailedDebug) print("Event: running task of state " + _state.toString() + "...");
				
				switch (_state)
				{
					case START:
					{
						if(checkPlayers(_instance.getId()))
						{
							teleportPlayers(_instance.getId(), SpawnType.Regular, false);
							
							setupTitles(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							forceSitAll(_instance.getId());
							
							setNextState(EventState.FIGHT);
							scheduleNextTask(10000);
						}
						
						break;
					}
					case FIGHT:
					{
						forceStandAll(_instance.getId());
						
						if(getBoolean("createParties"))
							createParties(getInt("maxPartySize"));
						
						setNextState(EventState.END);

						_clock.startClock(_manager.getRunTime());
						
						break;
					}
					case END:
					{
						_clock.setTime(0, true);
						
						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), getInt("killsForReward"), getInt("killsForReward"));
							
							clearEvent(_instance.getId());
						}
							
						break;
					}
				}
				
				/**/ if(NexusLoader.detailedDebug) print("Event: ... finished running task. next state " + _state.toString());
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}

	protected enum EventState
	{
		START, FIGHT, END, TELEPORT, INACTIVE
	}

	protected FastMap<Integer, TvTEventInstance> _matches;
	
	protected boolean _waweRespawn;
	protected int _teamsCount;

	public TeamVsTeam(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system.", InputType.Boolean));
		
		addConfig(new ConfigModel("createParties", "true", "Put 'True' if you want this event to automatically create parties for players in each team.", InputType.Boolean));
		addConfig(new ConfigModel("maxPartySize", "9", "The maximum size of party, that can be created. Works only if <font color=LEVEL>createParties</font> is true."));
		
		addConfig(new ConfigModel("teamsCount", "2", "The count of teams in the event. Max is 5. <font color=FF0000>In order to change the count of teams in the event, you must also edit this config in the Instance's configuration.</font>"));
		
		addConfig(new ConfigModel("firstBloodMessage", "true", "You can turn off/on the first blood announce in the event (first kill made in the event). This is also rewardable - check out reward type FirstBlood.", InputType.Boolean));
		
		addInstanceTypeConfig(new ConfigModel("teamsCount", "2", "You may specify the count of teams only for this instance. This config overrides event default teams count."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		// storing config value before event start - just to prevent the error that would cause a GM that changes 'waweRespawn' config while the event is running
		_waweRespawn = getBoolean("waweRespawn"); 
		
		if(_waweRespawn)
			initWaweRespawns(getInt("resDelay"));
		
		_runningInstances = 0;
	}
	
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
	
	@Override
	public void runEvent()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: started runEvent()");
		
		if(!dividePlayers())
		{
			clearEvent();
			return;
		}
		
		TvTEventInstance match;
		_matches = new FastMap<Integer, TvTEventInstance>();
		for(InstanceData instance : _instances)
		{
			/**/ if(NexusLoader.detailedDebug) print("Event: creating eventinstance for instance " + instance.getId());
			
			match = createEventInstance(instance);
			_matches.put(instance.getId(), match);
			
			_runningInstances ++;
			
			match.scheduleNextTask(0);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: event instance started");
		}
		
		/**/ if(NexusLoader.detailedDebug) print("Event: finished runEvent()");
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 

		int minKills = getInt("killsForReward");
		rewardAllTeams(-1, minKills, minKills);
	}
	
	@Override
	protected synchronized boolean instanceEnded()
	{
		_runningInstances--;
		
		/**/ if(NexusLoader.detailedDebug) print("Event: notifying instance ended: runningInstances = " + _runningInstances);
		
 		if(_runningInstances == 0)
		{
			_manager.end();
			return true;
		}
		else 
			return false;
	}
	
	@Override
	protected synchronized void endInstance(int instance, boolean canBeAborted, boolean canRewardIfAborted, boolean forceNotReward)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: endInstance() " + instance + ", canBeAborted " + canBeAborted + ", canReward.. " + canRewardIfAborted + " forceNotReward " + forceNotReward); 

		if(forceNotReward)
			_matches.get(instance).forceNotRewardThisInstance();
		
		_matches.get(instance).setNextState(EventState.END);
		if(canBeAborted)
			_matches.get(instance).setCanBeAborted();
		if(canRewardIfAborted)
			_matches.get(instance).setCanRewardIfAborted();
		_matches.get(instance).scheduleNextTask(0);
	}

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

	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(_hideTitles)
			return "";
		
		if(pi.isAfk())
			return "AFK";
		
		return "Kills: " + getPlayerData(pi).getScore() + " Deaths: " + getPlayerData(pi).getDeaths();
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		if (player.getTeamId() != target.getEventInfo().getTeamId())
		{
			tryFirstBlood(player);
			giveOnKillReward(player);
			
			// update player's team's stats
			player.getEventTeam().raiseScore(1);
			player.getEventTeam().raiseKills(1);
			
			// update player's own stats
			getPlayerData(player).raiseScore(1);
			getPlayerData(player).raiseKills(1);
			getPlayerData(player).raiseSpree(1);
			
			giveKillingSpreeReward(getPlayerData(player));
			
			// update title
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

	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: onDie - player " + player.getPlayersName() + " (instance " + player.getInstanceId() + "), killer " + killer.getName());
		
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		if(_waweRespawn)
			_waweScheduler.addPlayer(player);
		else
			scheduleRevive(player, getInt("resDelay") * 1000);
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new PvPEventPlayerData(player, this, new GlobalStatsModel(getEventType()));
	}

	@Override
	public PvPEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (PvPEventPlayerData) player.getEventData();
	}
	
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: called CLEAREVENT for instance " + instanceId);
		
		try
		{
			if(_matches != null)
			{
				for(TvTEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
						match.abort();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			if(!player.isOnline())
				continue;
			
			if(player.isParalyzed())
				player.setIsParalyzed(false);
			
			if(player.isImmobilized())
				player.unroot();
			
			if(!player.isGM())
				player.setIsInvul(false);
			
			player.removeRadarAllMarkers();
			
			player.setInstanceId(0);
			
			player.restoreData();
			
			player.teleport(player.getOrigLoc(), 0, true, 0);
			player.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
			
			if (player.getParty() != null)
			{
				PartyData party = player.getParty();
				party.removePartyMember(player);
			}
			
			player.broadcastUserInfo();
		}
		
		clearPlayers(true, instanceId);
	}

	@Override
	public synchronized void clearEvent()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: called global clearEvent()"); 

		// 0 = all instances
		clearEvent(0);
	}
	
	@Override
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: respawning player " + pi.getPlayersName() + ", instance " + instance);

		EventSpawn spawn = getSpawn(SpawnType.Regular, pi.getTeamId());
		if(spawn != null)
		{
			Loc loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(spawn.getRadius());
			
			pi.teleport(loc, 0, true, instance);
			
			pi.sendMessage(LanguageEngine.getMsg("event_respawned"));
		}
		else
		{
			//abortEvent("An error occured with Event Map.");
			debug("Error on respawnPlayer - no spawn type REGULAR, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(TvTEventInstance match : _matches.values())
		{
			if(match.isActive())
				return match.getClock().getTime();
		}
				
		return "N/A";
	}
	
	@Override
	public int getTeamsCount()
	{
		return getInt("teamsCount");
	}
	
	@Override
	public String getMissingSpawns(EventMap map)
	{
		TextBuilder tb = new TextBuilder();
		
		for(int i = 0; i < getTeamsCount(); i++)
		{
			if(!map.checkForSpawns(SpawnType.Regular, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.Regular, i+1, 1));
		}
		
		return tb.toString();
	}
	
	@Override
	public String getHtmlDescription()
	{
		if(_htmlDescription == null)
		{
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs());
			}
			else
			{
				_htmlDescription = getInt("teamsCount") + " teams fighting against each other. ";
				_htmlDescription += "Gain score by killing your opponents";
				
				if(getInt("killsForReward") > 0)
				{
					_htmlDescription += " (at least " + getInt("killsForReward") + " kill(s) is required to receive a reward)";
				}
				
				if(getBoolean("waweRespawn"))
					_htmlDescription += " and dead players are resurrected by an advanced wawe-spawn engine each " + getInt("resDelay") + " seconds";
				else
					_htmlDescription += " and if you die, you will be resurrected in " + getInt("resDelay") + " seconds";
				
				
				if(getBoolean("createParties"))
					_htmlDescription += ". The event automatically creates parties on start";
				
				_htmlDescription += ".";
			}
		}
		return _htmlDescription;
	}
	
	@Override
	protected AbstractEventInstance getMatch(int instanceId)
	{
		return _matches.get(instanceId);
	}
	
	@Override
	protected TvTEventData createEventData(int instanceId)
	{
		return new TvTEventData(instanceId);
	}
	
	@Override
	protected TvTEventInstance createEventInstance(InstanceData instance)
	{
		return new TvTEventInstance(instance);
	}
	
	@Override
	protected TvTEventData getEventData(int instance)
	{
		return _matches.get(instance)._data;
	}
}
