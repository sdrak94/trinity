package cz.nxs.events.engine.main.events;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class Domination extends AbstractMainEvent
{
	protected class ZoneData extends AbstractEventData
	{
		protected NpcData _zone;
		
		protected int _holdingTeam;
		protected int _holdingTime;
		
		protected ZoneData(int instance)
		{
			super(instance);
		}
		
		protected void addZone(NpcData zone, int radius)
		{
			_zone = zone;
		}
	}
	
	protected class DominationEventInstance extends AbstractEventInstance
	{
		protected EventState _state;
		
		protected ZoneData _zoneData;
		
		protected DominationEventInstance(InstanceData instance)
		{
			super(instance);
			
			_state = EventState.START;
			
			_zoneData = createEventData(instance.getId());
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
							
							spawnZone(_instance.getId());
							
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
						
						unspawnZone(_instance.getId());

						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), getInt("scoreForReward"), getInt("killsForReward"));
							
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

	protected FastMap<Integer, DominationEventInstance> _matches;
	
	protected boolean _waweRespawn;
	protected int _teamsCount;
	
	// domination
	protected int _zoneNpcId;
	protected int _zoneRadius;
	
	protected int _zoneCheckInterval;
	
	protected int _scoreForCapturingZone;
	private int _holdZoneFor;
	
	protected int _percentMajorityToScore;
	
	protected int _tick;
	
	// TODO
	// special effects for the owning team? healing, buffs, etc.
	
	public Domination(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("scoreForReward", "0", "The minimum score required to get a reward (includes all possible rewards). Score in this event is gained by standing near the zone, if the player wasn't afk, he should always have some score."));
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system."));
		
		addConfig(new ConfigModel("zoneNpcId", "8992", "The ID of NPC that symbolizes the zone."));
		addConfig(new ConfigModel("zoneRadius", "180", "The radius of zone to count players inside."));
		
		addConfig(new ConfigModel("allowZoneNpcEffects", "true", "Enables Zone NPC's special effects, if blue or red team owns it. Due to client limitations, this will only work if the event has 2 teams.", InputType.Boolean));
		addConfig(new ConfigModel("allowFireworkOnScore", "true", "Enables Zone NPC's small firework effect, when a team scores. Working only if <font color=LEVEL>holdZoneFor</font> is higher than 5 (to prevent spamming this skill).", InputType.Boolean));
		addConfig(new ConfigModel("allowPlayerEffects", "true", "Enables special effects for players from the team owning the zone and standing near the Zone NPC (in <font color=LEVEL>zoneRadius</font>). Only works if the event has 2 teams.", InputType.Boolean));
		
		addConfig(new ConfigModel("zoneCheckInterval", "1", "In seconds. The time after it checks and counts players near the zone(s) and adds score to the team, that has more players inside the zone. Setting this to 1 is usually good (higher values make this event less expensive for cpu :)"));
		
		addConfig(new ConfigModel("scoreForCapturingZone", "1", "The ammount of points team gets each <font color=LEVEL>scoreCheckInterval</font> seconds if owns the zone."));
		addConfig(new ConfigModel("holdZoneFor", "0", "In seconds. The team needs to own this zone for this time to get <font color=LEVEL>scoreForCapturingZone</font> points. "));
		addConfig(new ConfigModel("percentMajorityToScore", "50", "In percent. In order to score a point, the team must have more players near the zone NPC in <font color=LEVEL>zoneRadius</font> radius, than the other team(s). " +
				"The ammount of players from the scoring team must be higher than the ammount of players from the other teams by this percent value. " +
				"Put 100 to make that all other team(s)' players in <font color=LEVEL>zoneRadius</font> must be dead to score; or put 0 to make that it will give score to the team that has more players and not care about any percent counting (eg. if team A has 15 players and team B has 16, it will simply reward team B).")); // TODO
		
		addConfig(new ConfigModel("createParties", "true", "Put 'True' if you want this event to automatically create parties for players in each team."));
		addConfig(new ConfigModel("maxPartySize", "9", "The maximum size of party, that can be created. Works only if <font color=LEVEL>createParties</font> is true."));
		
		addConfig(new ConfigModel("teamsCount", "2", "The ammount of teams in the event. Max is 5. <font color=FF0000>In order to change the count of teams in the event, you must also edit this config in the Instance's configuration.</font>"));
		
		addConfig(new ConfigModel("firstBloodMessage", "true", "You can turn off/on the first blood announce in the event (first kill made in the event). This is also rewardable - check out reward type FirstBlood.", InputType.Boolean));
		
		addInstanceTypeConfig(new ConfigModel("teamsCount", "2", "You may specify the count of teams only for this instance. This config overrides events default teams count."));
		
		//TODO: round based domination event settings
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		// storing config value before event start - just to prevent the error that would cause a GM that changes 'waweRespawn' config while the event is running
		_waweRespawn = getBoolean("waweRespawn"); 
		
		if(_waweRespawn)
			initWaweRespawns(getInt("resDelay"));
		
		_zoneNpcId = getInt("zoneNpcId");
		_zoneRadius = (int) Math.pow(getInt("zoneRadius"), 2);
		
		_zoneCheckInterval = getInt("zoneCheckInterval");
		
		_holdZoneFor = getInt("holdZoneFor");
		_scoreForCapturingZone = getInt("scoreForCapturingZone");
		_percentMajorityToScore = getInt("percentMajorityToScore");
		
		_runningInstances = 0;
		_tick = 0;
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
		
		DominationEventInstance match;
		_matches = new FastMap<Integer, DominationEventInstance>();
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
	
	protected void spawnZone(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning Zone for instance " + instanceId);

		clearMapHistory(-1, SpawnType.Zone);
		
		EventSpawn sp = getSpawn(SpawnType.Zone, -1);
		NpcData zone = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _zoneNpcId, instanceId, "Zone", "Domination event");
		
		int radius = sp.getRadius();
		if(radius > 0)
		{
			_zoneRadius = (int) Math.pow(radius, 2);
		}
		
		getEventData(instanceId).addZone(zone, _zoneRadius);
		
		getEventData(instanceId)._zone.getNpc().setTitle("No owner");
		getEventData(instanceId)._zone.getNpc().broadcastNpcInfo();
	}
	
	protected void unspawnZone(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: unspawning zone for instance " + instanceId);

		if(getEventData(instanceId)._zone != null)
		{
			getEventData(instanceId)._zone.deleteMe();
			/**/ if(NexusLoader.detailedDebug) print("Event: zone is not null and was deleted");
		}
		else
			/**/ if(NexusLoader.detailedDebug) print("Event: ... zone is already null!!!");
	}
	
	protected void setZoneEffects(int teamId, NpcData zoneNpc)
	{
		if(getBoolean("allowZoneNpcEffects") && _teamsCount == 2)
		{
			if(teamId == 1)
			{
				zoneNpc.stopAbnormalEffect(0x000004); // red circle effect
				zoneNpc.startAbnormalEffect(0x200000); // imprissing 1 effect
			}
			else if(teamId == 2)
			{
				zoneNpc.stopAbnormalEffect(0x200000); // imprissing 1 effect
				zoneNpc.startAbnormalEffect(0x000004); // red circle effect
			}
			else // stop effects
			{
				zoneNpc.stopAbnormalEffect(0x000004); // red circle effect
				zoneNpc.stopAbnormalEffect(0x200000); // imprissing 1 effect
			}
		}
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 
		
		//TODO give rewards here
		int minKills = getInt("killsForReward");
		int minScore = getInt("scoreForReward");
		
		rewardAllTeams(-1, minScore, minKills);
	}
	
	@Override
	protected synchronized boolean instanceEnded()
	{
		_runningInstances--;
		
		/**/ if(NexusLoader.detailedDebug) print("Event: notifying instance ended: runningInstances = " + _runningInstances);
		
		if(_runningInstances == 0)
		{
			_manager.end();
			return true; // MODIFIED
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
		
		return "Score: " + getPlayerData(pi).getScore();
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
			player.getEventTeam().raiseKills(1);
			
			// update player's own stats
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
				for(DominationEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();
						unspawnZone(match.getInstance().getId());
					}
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
			
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1());
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
			
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
			debug("Error on respawnPlayer - no spawn type REGULAR, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}
	
	@Override
	protected void clockTick()
	{
		_tick ++;
		if(_tick % _zoneCheckInterval != 0)
			return;
		
		Map<Integer, List<PlayerEventInfo>> players = new FastMap<Integer, List<PlayerEventInfo>>(_teamsCount);
		
		int instanceId;
		int zoneX, zoneY, zoneZ;
		
		for(DominationEventInstance match : _matches.values())
		{
			instanceId = match.getInstance().getId();
			
			zoneX = getEventData(instanceId)._zone.getLoc().getX();
			zoneY = getEventData(instanceId)._zone.getLoc().getY();
			zoneZ = getEventData(instanceId)._zone.getLoc().getZ();
			
			// calculate players near the zone
			for(PlayerEventInfo player : getPlayers(instanceId))
			{
				if(player.getDistanceSq(zoneX, zoneY, zoneZ) <= _zoneRadius)
				{
					if(player.isVisible() && !player.isDead())
					{
						if(!players.containsKey(player.getTeamId()))
							players.put(player.getTeamId(), new FastList<PlayerEventInfo>());
						
						players.get(player.getTeamId()).add(player);
					}
				}
			}
			
			// get the team with most players near the zone
			int highestCount = 0;
			int team = 0;
			boolean isThereMajorityTeam = true;
			
			for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
			{
				if(teamData.getValue().size() > highestCount)
				{
					highestCount = teamData.getValue().size();
					team = teamData.getKey();
				}
				
				// more teams have the same count of players in near the zone
				else if(highestCount != 0 && teamData.getValue().size() == highestCount)
				{
					isThereMajorityTeam = false;
					break;
				}
			}
			
			// if such team exists..
			if(isThereMajorityTeam && team != 0)
			{
				// calculate if the team really owns zone based on configs provided
				boolean ownsZone = false;
				
				// don't care about percents, this team already has more players than the others
				if(_percentMajorityToScore == 0)
				{
					ownsZone = true;
				}
				
				// check if there are only the team's players in the zone and nobody else
				else if(_percentMajorityToScore == 100)
				{
					boolean teamWithMorePlayers = false;
					for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
					{
						if(teamData.getKey() == team)
							continue;
						
						if(teamData.getValue().size() > 0)
						{
							teamWithMorePlayers = true;
							break;
						}
					}
					
					if(!teamWithMorePlayers)
						ownsZone = true;
				}
				
				// else calculate and check percentual majority
				else
				{
					int majorityTeamPlayers = players.get(team).size();
					boolean teamWithMorePlayers = false;
					
					for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
					{
						if(teamData.getKey() == team)
							continue;
						
						double d = ((double) teamData.getValue().size() / (double) majorityTeamPlayers);
						int percent = 100 - ((int) (d * 100));
						
						if(percent < _percentMajorityToScore)
						{
							teamWithMorePlayers = true;
							break;
						}
					}
					
					if(!teamWithMorePlayers)
						ownsZone = true;
				}
				
				if(ownsZone)
				{
					// set the owner of the zone and raise owning time
					if(getEventData(instanceId)._holdingTeam != team)
					{
						if(getEventData(instanceId)._holdingTeam != 0 && getBoolean("allowPlayerEffects") && _teamsCount == 2)
						{
							for(PlayerEventInfo player : _teams.get(instanceId).get(getEventData(instanceId)._holdingTeam).getPlayers())
								player.stopAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
						}
						
						announce(instanceId, LanguageEngine.getMsg("dom_gainedZone", _teams.get(instanceId).get(team).getFullName()));
						getEventData(instanceId)._zone.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_owner", _teams.get(instanceId).get(team).getTeamName()));
						getEventData(instanceId)._zone.getNpc().broadcastNpcInfo();
						
						getEventData(instanceId)._holdingTeam = team;
						getEventData(instanceId)._holdingTime = 0;
						
						setZoneEffects(team, getEventData(instanceId)._zone);
					}
					else
					{
						getEventData(instanceId)._holdingTime += _zoneCheckInterval;
					}
					
					// effects
					if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
					{
						for(PlayerEventInfo player : _teams.get(instanceId).get(team).getPlayers())
						{
							if(player.getDistanceSq(zoneX, zoneY, zoneZ) <= _zoneRadius && player.isVisible() && !player.isDead())
								player.startAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
							else
								player.stopAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
						}
					}
					
					// check if it's been running it enough long to give score
					if(getEventData(instanceId)._holdingTime >= _holdZoneFor)
					{
						_teams.get(instanceId).get(team).raiseScore(_scoreForCapturingZone);
						
						for(PlayerEventInfo player : players.get(team))
						{
							getPlayerData(player).raiseScore(_scoreForCapturingZone);
							setScoreStats(player, getPlayerData(player).getScore());
							
							// update title
							if(player.isTitleUpdated())
							{
								player.setTitle(getTitle(player), true);
								player.broadcastTitleInfo();
							}
							
							CallbackManager.getInstance().playerScores(getEventType(), player, _scoreForCapturingZone);
						}
						
						getEventData(instanceId)._holdingTime = 0;
						
						
						if(_holdZoneFor > 5)
						{
							announce(instanceId, "*** " + LanguageEngine.getMsg("dom_score", _teams.get(instanceId).get(team).getFullName()));
							
							if(getBoolean("allowFireworkOnScore"))
								getEventData(instanceId)._zone.broadcastSkillUse(getEventData(instanceId)._zone, getEventData(instanceId)._zone, 2024, 1);
						}
					}
					else
					{
						int toHold = _holdZoneFor - getEventData(instanceId)._holdingTime;
						boolean announce = false;
						
						if(getEventData(instanceId)._holdingTime == 0)
							announce = true;
						else
						{
							if(toHold >= 60 && toHold % 60 == 0)
								announce = true;
							else
							{
								switch(toHold)
								{
									case 45:
									case 30:
									case 20:
									case 10:
									case 5:
										announce = true;
										break;
								}
							}
						}
						
						if(announce)
						{
							boolean min = false;
							//if(min)
							//	toHold /= 60;
							
							announce(instanceId, "* " + LanguageEngine.getMsg("dom_leftToScore", toHold, (min ? "minute" : "second"), _teams.get(instanceId).get(team).getFullName()));
						}
					}
				}
				else
				{
					if(getEventData(instanceId)._holdingTeam != 0)
					{
						announce(instanceId, LanguageEngine.getMsg("dom_lostZone", _teams.get(instanceId).get(getEventData(instanceId)._holdingTeam).getFullName()));
						getEventData(instanceId)._zone.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_noOwner"));
						getEventData(instanceId)._zone.getNpc().broadcastNpcInfo();
						
						setZoneEffects(0, getEventData(instanceId)._zone);
						
						if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
						{
							for(PlayerEventInfo player : _teams.get(instanceId).get(getEventData(instanceId)._holdingTeam).getPlayers())
							{
								player.stopAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
							}
						}
					}
					
					getEventData(instanceId)._holdingTime = 0;
					getEventData(instanceId)._holdingTeam = 0;
				}
			}
			else
			{
				if(getEventData(instanceId)._holdingTeam != 0)
				{
					announce(instanceId, LanguageEngine.getMsg("dom_lostZone", _teams.get(instanceId).get(getEventData(instanceId)._holdingTeam).getFullName()));
					getEventData(instanceId)._zone.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_noOwner"));
					getEventData(instanceId)._zone.getNpc().broadcastNpcInfo();
					
					setZoneEffects(0, getEventData(instanceId)._zone);
					
					if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
					{
						for(PlayerEventInfo player : _teams.get(instanceId).get(getEventData(instanceId)._holdingTeam).getPlayers())
						{
							player.stopAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
						}
					}
				}
				
				getEventData(instanceId)._holdingTime = 0;
				getEventData(instanceId)._holdingTeam = 0;
			}
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(DominationEventInstance match : _matches.values())
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
		
		if(!map.checkForSpawns(SpawnType.Zone, -1, 1))
			tb.append(addMissingSpawn(SpawnType.Zone, 0, 1));
		
		return tb.toString();
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		int owningTeam = _matches.get(instance)._zoneData._holdingTeam;
		String status = "<font color=ac9887>Zone owned by:</font> <font color=" + EventManager.getInstance().getDarkColorForHtml(owningTeam) + ">" + EventManager.getInstance().getTeamName(owningTeam) + " team</font>";
		
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
				_htmlDescription = getInt("teamsCount") + " teams fighting against each other. ";
				_htmlDescription += "The goal of this event is to capture and hold ";
				_htmlDescription += "a zone. The zone is represented by an NPC and to capture it, you need to stand near the NPC and ensure that no other enemies are standing near the zone too. ";
				
				if(getInt("killsForReward") > 0)
					_htmlDescription += "At least " + getInt("killsForReward") + " kill(s) is required to receive a reward. ";
				
				if(getInt("scoreForReward") > 0)
					_htmlDescription += "At least " + getInt("scoreForReward") + " score (obtained when your team owns the zone and you stand near it) is required to receive a reward. ";
				
				if(getBoolean("waweRespawn"))
					_htmlDescription += "Dead players are resurrected by an advanced wawe-spawn engine each " + getInt("resDelay") + " seconds. ";
				else
					_htmlDescription += "If you die, you will get resurrected in " + getInt("resDelay") + " seconds. ";
				
				if(getBoolean("createParties"))
					_htmlDescription += "The event automatically creates parties on start.";
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
	protected ZoneData createEventData(int instance)
	{
		return new ZoneData(instance);
	}
	
	@Override
	protected DominationEventInstance createEventInstance(InstanceData instance)
	{
		return new DominationEventInstance(instance);
	}
	
	@Override
	protected ZoneData getEventData(int instance)
	{
		return _matches.get(instance)._zoneData;
	}
}
