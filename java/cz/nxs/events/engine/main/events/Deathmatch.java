package cz.nxs.events.engine.main.events;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
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
public class Deathmatch extends AbstractMainEvent
{
	protected class DMData extends AbstractEventData
	{
		protected DMData(int instance)
		{
			super(instance);
		}
	}
	
	protected class DMEventInstance extends AbstractEventInstance
	{
		protected EventState _nextState;
		
		protected DMData _data;
		
		public DMEventInstance(InstanceData instance)
		{
			super(instance);
			
			_nextState = EventState.START;
			
			_data = createEventData(_instance.getId());
		}
		
		protected void setNextState(EventState state) {_nextState = state;}
		
		@Override
		public boolean isActive() { return _nextState != EventState.INACTIVE;} 
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				/**/ if(NexusLoader.detailedDebug) print("Event: running task of state " + _nextState.toString() + "...");
				
				switch (_nextState)
				{
					case START:
					{
						if(checkPlayers(_instance.getId()))
						{
							if(_antifeed)
							{
								for(PlayerEventInfo player : getPlayers(_instance.getId()))
									player.startAntifeedProtection(false);
							}
							
							teleportPlayers(_instance.getId(), SpawnType.Regular, true);
							
							setupTitles(_instance.getId());
							
							// action done for some hardcoded maps
							removeStaticDoors(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							unpartyPlayers(_instance.getId());
							
							forceSitAll(_instance.getId());
							
							setNextState(EventState.FIGHT);
							scheduleNextTask(10000);
						}
						
						break;
					}
					case FIGHT:
					{
						forceStandAll(_instance.getId());
						
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
								rewardAllPlayers(_instance.getId(), 0, getInt("killsForReward"));
							
							clearEvent(_instance.getId());
						}
						
						break;
					}
				}
				
				/**/ if(NexusLoader.detailedDebug) print("Event: ... finished running task. next state " + _nextState.toString());
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

	protected FastMap<Integer, DMEventInstance> _matches;
	
	protected boolean _waweRespawn;
	protected boolean _antifeed;

	public Deathmatch(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system.", InputType.Boolean));
		
		addConfig(new ConfigModel("firstBloodMessage", "true", "You can turn off/on the first blood announce in the event (first kill made in the event). This is also rewardable - check out reward type FirstBlood.", InputType.Boolean));
		
		addConfig(new ConfigModel("antifeedProtection", "true", "Enables the special anti-feed protection. This protection changes player's name, title, race, clan/ally crest, class and basically all of his apperance, sometimes also gender.", InputType.Boolean));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		// storing config value before event start - just to prevent the error that would cause a GM that changes 'waweRespawn' config while the event is running
		_waweRespawn = getBoolean("waweRespawn"); 
		_antifeed = getBoolean("antifeedProtection");
		
		if(_waweRespawn)
			initWaweRespawns(getInt("resDelay"));
		
		_runningInstances = 0;
	}
	
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		// creates only one team to store players in
		createTeams(1, type.getInstance().getId());
		return 1;
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
		
		DMEventInstance match;
		_matches = new FastMap<Integer, DMEventInstance>();
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
		
		rewardAllPlayers(-1, minKills, minKills);
		
		if(_antifeed)
		{
			for(PlayerEventInfo player : getPlayers(0))
				player.stopAntifeedProtection(false);
		}
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
		TextBuilder tb = new TextBuilder();
		
		int top = 0;
		for(PlayerEventInfo player : getPlayers(instance))
		{
			if(getPlayerData(player).getKills() > top)
				top = getPlayerData(player).getKills();
		}
		
		tb.append(LanguageEngine.getMsg("dm_topKills", top) + " ");
		
		tb.append("   " + LanguageEngine.getMsg("event_scorebar_time", _matches.get(instance).getClock().getTime()));
		
		return tb.toString();
	}

	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(_hideTitles)
			return "";
		
		if(pi.isAfk())
			return "AFK";
		
		return LanguageEngine.getMsg("event_title_pvppk", getPlayerData(pi).getScore(), getPlayerData(pi).getDeaths());
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		tryFirstBlood(player);
		giveOnKillReward(player);
		
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
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		if(player.getPlayersId() == target.getObjectId())
			return true;
		
		return false;
	}
	
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return true;
		else if(target.getEventInfo().getEvent() != player.getEvent())
			return false;

		return true;
	}
	
	@Override
	public boolean onSay(PlayerEventInfo player, String text, int channel)
	{
		if(text.equals(".scheme"))
		{
			EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "none", getEventType().getAltTitle());
			return false;
		}
		
		if(_antifeed)
		{
			player.sendMessage(LanguageEngine.getMsg("dm_cantChat"));
			return false;
		}
		else
			return true;
	}
	
	@Override
	public boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target)
	{
		return false;
	}
	
	@Override
	protected boolean checkIfEventCanContinue(int instanceId, PlayerEventInfo disconnectedPlayer)
	{
		int alive = 0;
		
		for(PlayerEventInfo pi : getPlayers(instanceId))
		{
			if(pi != null && pi.isOnline())
				alive ++;
		}
		
		return alive >= 2;
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
				for(DMEventInstance match : _matches.values())
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
			
			if(player.hasAntifeedProtection())
			{
				player.stopAntifeedProtection(false);
			}
			
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
		
		EventSpawn spawn = getSpawn(SpawnType.Regular, -1);
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
			debug("Error on respawnPlayer - no spawn type REGULAR, team -1 (FFA) has been found. Event aborted.");
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(DMEventInstance match : _matches.values())
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
	
	@Override
	public String getMissingSpawns(EventMap map)
	{
		if(!map.checkForSpawns(SpawnType.Regular, -1, 1))
			return addMissingSpawn(SpawnType.Regular, 0, 1);
		return "";
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		int top = 0;
		for(PlayerEventInfo player : getPlayers(instance))
		{
			if(getPlayerData(player).getKills() > top)
				top = getPlayerData(player).getKills();
		}
		
		String status = "<font color=ac9887>Top kills count: </font><font color=7f7f7f>" + top + "</font>";
		
		return("<table width=510 bgcolor=3E3E3E><tr><td width=510 align=center>" + status + "</td></tr></table>");
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
				_htmlDescription = "This is a free-for-all event, don't expect any help from teammates. Gain score by killing your opponents";
				
				_htmlDescription += " and if you die, you will be resurrected within " + getInt("resDelay") + " seconds. ";
				
				if(getBoolean("waweRespawn"))
					_htmlDescription += "Also, wawe-spawn system ensures that all dead players are spawned in the same moment (but in different spots). ";
				
				if(getBoolean("antifeedProtection"))
					_htmlDescription += "This event has a protection, which completely changes the appearance of all players and temporary removes their title and clan/ally crests. ";
				
				if(getInt("killsForReward") > 0)
					_htmlDescription += "In the end, you need at least " + getInt("killsForReward") + " kills to receive a reward.";
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
	protected DMData createEventData(int instance)
	{
		return new DMData(instance);
	}
	
	@Override
	protected DMEventInstance createEventInstance(InstanceData instance)
	{
		return new DMEventInstance(instance);
	}
	
	@Override
	protected DMData getEventData(int instance)
	{
		return _matches.get(instance)._data;
	}
}
