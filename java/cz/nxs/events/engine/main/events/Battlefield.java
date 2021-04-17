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
public class Battlefield extends AbstractMainEvent
{
	protected class Tower
	{
		private final NpcData _npc;
		private final EventSpawn _spawn;
		private final int _radius;
		private int _owningTeam;
		private int _ownedTime;
		private int _capturingTime;
		
		private final List<PlayerEventInfo> _effects;
		
		
		public Tower(EventSpawn spawn, NpcData npc, int radius)
		{
			_spawn = spawn;
			_npc = npc;
			_radius = radius;
			_owningTeam = 0;
			_capturingTime = 0;
			
			_effects = new FastList<PlayerEventInfo>();
		}
		
		public void setOwningTeam(int team, boolean updateTime)
		{
			_owningTeam = team;
			
			if(updateTime)
				setOwnedTime(0);
		}
		
		public boolean setCapturingTime(int i )
		{
			_capturingTime = i;
			if(_capturingTime >= _timeToHoldTowerToCapture)
				return true;
			else return false;
		}
		
		public int getCapturingTime()
		{
			return _capturingTime;
		}
		
		public void addEffectedPlayer(PlayerEventInfo player)
		{
			_effects.add(player);
		}
		
		public void removeEffectedPlayer(PlayerEventInfo player)
		{
			_effects.remove(player);
		}
		
		public boolean containsEffectedPlayer(PlayerEventInfo player)
		{
			return _effects.contains(player);
		}
		
		public List<PlayerEventInfo> getEffectedPlayers()
		{
			return _effects;
		}
		
		public void resetEffectedPlayers()
		{
			_effects.clear();
		}
		
		public int getOwningTeam()
		{
			return _owningTeam;
		}
		
		public int getOwnedTime()
		{
			return _ownedTime;
		}
		
		public void setOwnedTime(int i)
		{
			_ownedTime = i;
		}
		
		public void raiseOwnedTime(int count)
		{
			_ownedTime += count;
		}
		
		public NpcData getNpc()
		{
			return _npc;
		}
		
		public int getRadius()
		{
			return _radius;
		}
		
		public EventSpawn getSpawn()
		{
			return _spawn;
		}
		
		public Loc getLoc()
		{
			return _npc.getLoc();
		}
	}
	
	protected class TowerData extends AbstractEventData
	{
		protected final Tower[] _towers;
		
		private int _order;
		protected int _dominatingTeam;
		protected int _holdingAllTowersFor;
		
		protected TowerData(int instance)
		{
			super(instance);
			
			_towers = new Tower[countOfTowers];
			_dominatingTeam = 0;
			_holdingAllTowersFor = 0;
			
			_order = 0;
		}
		
		protected void addTower(NpcData base, int radius, EventSpawn spawn)
		{
			if(_order < countOfTowers)
			{
				_towers[_order] = new Tower(spawn, base, radius > 0 ? ((int) Math.pow(radius, 2)) : _towerRadius);
				_order ++;
			}
			else
				NexusLoader.debug("too many towers for TowerData (" + _order + "; " + countOfTowers + ")");
		}
		
		protected void setDominatingTeam(int team)
		{
			_dominatingTeam = team;
		}
		
		protected int getDominatingTeam()
		{
			return _dominatingTeam;
		}
		
		protected int raiseDominatingTime(int time)
		{
			_holdingAllTowersFor += time;
			return _holdingAllTowersFor;
		}
		
		protected int getDominatingTime()
		{
			return _holdingAllTowersFor;
		}
		
		protected void resetDominatingTime()
		{
			_holdingAllTowersFor = 0;
		}
		
		protected Tower getTower(int index)
		{
			return _towers[index];
		}
	}
	
	protected class BattlefieldEventInstance extends AbstractEventInstance
	{
		protected EventState _state;
		
		protected TowerData _towerData;
		
		protected BattlefieldEventInstance(InstanceData instance)
		{
			super(instance);
			
			_state = EventState.START;
			
			_towerData = createEventData(instance.getId());
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
							
							spawnTowers(_instance.getId());
							
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
						
						unspawnTowers(_instance.getId());

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

	protected FastMap<Integer, BattlefieldEventInstance> _matches;
	
	protected boolean _waweRespawn;
	protected int _teamsCount;
	
	// domination
	protected int _towerNpcId;
	protected int _towerRadius;
	
	protected int _towerCheckInterval;
	
	protected int _scoreForCapturingTower;
	private int _timeToHoldTowerToCapture;
	private int _holdAllTowersFor;
	
	protected int _percentMajorityToCapture;
	
	protected String _scoreType;
	
	protected int _minPlayersToCaptureTheBase;
	protected boolean isMinPlayersToCaptureTheBaseInPercent;
	
	protected int _minTowersToOwnToScore;
	
	protected int tick;
	protected int countOfTowers;
	
	public Battlefield(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("scoreForReward", "0", "The minimum score required to get a reward (includes all possible rewards). Score in this event is gained by capturing bases."));
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system."));
		
		addConfig(new ConfigModel("countOfBases", "2", "Specifies how many bases will be in the event. In order to score, one team must capture more bases than the other team(s). If you have 2 or 4 teams set in this event, you should only use odd numbers for the count of towers, such as 3, 5, 7 or 9. Don't forget to create a same count of Base spawns in the map you are running this event in. ", 
				InputType.Enum).addEnumOptions(new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10"}));
		addConfig(new ConfigModel("baseNpcId", "8998", "The ID of NPC that symbolizes the base."));
		addConfig(new ConfigModel("baseRadius", "180", "The radius of base to count players inside."));
		
		addConfig(new ConfigModel("allowBaseNpcEffects", "true", "Enables Base NPC's special effects, if blue or red team owns it. Due to client limitations, this will only work if the event has 2 teams.", InputType.Boolean));
		addConfig(new ConfigModel("allowFireworkOnScore", "true", "Enables Base NPC's small firework effect, when a team scores. Working only if <font color=LEVEL>holdBaseFor</font> is higher than 5 (to prevent spamming this skill).", InputType.Boolean));
		addConfig(new ConfigModel("allowPlayerEffects", "true", "Enables special effects for players from the team owning the base and standing near the Base NPC (in <font color=LEVEL>baseRadius</font>). Only works if the event has 2 teams.", InputType.Boolean));
		
		addConfig(new ConfigModel("baseCheckInterval", "1", "In seconds. The time after it checks and counts players near the base(s) and adds score to the team, that has more players inside the base. Setting this to 1 is usually good (higher values make this event less expensive for cpu)"));
		
		addConfig(new ConfigModel("minPlayersToCaptureBase", "25%", "The min count of players the team must have near the base in order to capture it. You can set this value in percent by adding % (eg. 5%) - this will calculate the min count of players from the size of the team (eg. 20% and 50 players in the team = at least 10 players are needed to capture a base)."));
		
		addConfig(new ConfigModel("typeOfScoring", "AllTeams", "Define the way the event will give score to teams for capturing bases. If you select 'AllTeams', the event will score to all teams based on the count of bases they own (eg. team A has 2 bases - will receive 2 score, team B has 1 base - will receive 1 score). Setting 'DominatingTeam' will make it so that only the team which has MORE bases than the other teams will be receiving score points.", InputType.Enum).addEnumOptions(new String[]{ "AllTeams", "DominatingTeam"}));
		
		addConfig(new ConfigModel("scoreForCapturingBase", "1", "The ammount of points team gets each <font color=LEVEL>scoreCheckInterval</font> seconds if owns the base."));
		
		addConfig(new ConfigModel("holdBaseToCapture", "0", "In seconds. In order to capture a single base, the team needs to stay for this time near it."));
		
		addConfig(new ConfigModel("holdAllBasesToScore", "0", "In seconds. If the team captures enought bases to score, they will still need to hold them for this time in order to get <font color=LEVEL>scoreForCapturingBase</font> score."));
		
		addConfig(new ConfigModel("minTowersToOwnToScore", "1", "The min count of towers one team must own in order to get any score."));
		
		addConfig(new ConfigModel("percentMajorityToScore", "50", "In percent. In order to score a point, the team must have more players near the base NPC in <font color=LEVEL>baseRadius</font> radius, than the other team(s). " +
				"The ammount of players from the scoring team must be higher than the ammount of players from the other teams by this percent value. " +
				"Put 100 to make that all other team(s)' players in <font color=LEVEL>baseRadius</font> must be dead to score; or put 0 to make that it will give score to the team that has more players and not care about any percent counting (eg. if team A has 15 players and team B has 16, it will simply reward team B)."));
		
		addConfig(new ConfigModel("createParties", "true", "Put 'True' if you want this event to automatically create parties for players in each team."));
		addConfig(new ConfigModel("maxPartySize", "9", "The maximum size of party, that can be created. Works only if <font color=LEVEL>createParties</font> is true."));
		
		addConfig(new ConfigModel("teamsCount", "2", "The ammount of teams in the event. Max is 5. <font color=FF0000>In order to change the count of teams in the event, you must also edit this config in the Instance's configuration.</font>"));
		
		addConfig(new ConfigModel("firstBloodMessage", "true", "You can turn off/on the first blood announce in the event (first kill made in the event). This is also rewardable - check out reward type FirstBlood.", InputType.Boolean));
		
		addInstanceTypeConfig(new ConfigModel("teamsCount", "2", "You may specify the count of teams only for this instance. This config overrides events default teams count."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		// storing config value before event start - just to prevent the error that would cause a GM that changes 'waweRespawn' config while the event is running
		_waweRespawn = getBoolean("waweRespawn"); 
		
		if(_waweRespawn)
			initWaweRespawns(getInt("resDelay"));
		
		_towerNpcId = getInt("baseNpcId");
		_towerRadius = (int) Math.pow(getInt("baseRadius"), 2);
		
		_towerCheckInterval = getInt("baseCheckInterval");
		
		String s = getString("minPlayersToCaptureBase");
		if(s.endsWith("%"))
		{
			_minPlayersToCaptureTheBase = Integer.parseInt(s.substring(0, s.length()-1));
			isMinPlayersToCaptureTheBaseInPercent = true;
		}
		else
		{
			_minPlayersToCaptureTheBase = Integer.parseInt(s);
			isMinPlayersToCaptureTheBaseInPercent = false;
		}
		
		_scoreType = getString("typeOfScoring");
		
		_minTowersToOwnToScore = getInt("minTowersToOwnToScore");
		
		_timeToHoldTowerToCapture = getInt("holdBaseToCapture");
		_holdAllTowersFor = getInt("holdBaseFor");
		_scoreForCapturingTower = getInt("scoreForCapturingBase");
		_percentMajorityToCapture = getInt("percentMajorityToScore");
		
		countOfTowers = _manager.getMap().getSpawns(-1, SpawnType.Base).size();
		
		_runningInstances = 0;
		tick = 0;
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
		
		BattlefieldEventInstance match;
		_matches = new FastMap<Integer, BattlefieldEventInstance>();
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
	protected void enableMarkers(int instanceId, boolean createEventSpawnMarkers)
	{
		if(!_enableRadar)
			return;
		
		for(EventTeam team : _teams.get(instanceId).values())
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				pi.createRadar();
			}
			
			startRadar(instanceId, team);
		}
	}
	
	private void startRadar(int instanceId, EventTeam team)
	{
		EventSpawn zone = selectZoneForRadar(instanceId, team);
		
		if(zone != null)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				pi.getRadar().setLoc(zone.getLoc().getX(), zone.getLoc().getY(), zone.getLoc().getZ());
				pi.getRadar().setRepeat(true);
				pi.getRadar().enable();
			}
		}
		else
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				pi.getRadar().setRepeat(false);
				pi.getRadar().disable();
			}
		}
	}
	
	private EventSpawn selectZoneForRadar(int instanceId, EventTeam team)
	{
		EventSpawn zone = null;
		int teamId = team.getTeamId();
		
		int topImportance = Integer.MAX_VALUE;
		Tower tempTopImportance = null;
		
		// towers with no owner; near to team's spawn
		for(Tower tower : getEventData(instanceId)._towers)
		{
			if(tower != null && tower.getOwningTeam() == 0 && tower.getSpawn().getSpawnTeam() == teamId)
			{
				if(tower.getSpawn().getImportance() < topImportance)
				{
					topImportance = tower.getSpawn().getImportance();
					tempTopImportance = tower;
				}
			}
		}
		
		// towers with no owner
		if(tempTopImportance == null)
		{
			topImportance = Integer.MAX_VALUE;
			
			for(Tower tower : getEventData(instanceId)._towers)
			{
				if(tower != null && tower.getOwningTeam() == 0)
				{
					if(tower.getSpawn().getImportance() < topImportance)
					{
						topImportance = tower.getSpawn().getImportance();
						tempTopImportance = tower;
					}
				}
			}
		}
		
		// enemy towers near to team's spawn - take from the lowest priority
		if(tempTopImportance == null)
		{
			topImportance = Integer.MAX_VALUE;
			
			for(Tower tower : getEventData(instanceId)._towers)
			{
				if(tower != null && tower.getSpawn().getSpawnTeam() == teamId && tower.getOwningTeam() != teamId)
				{
					if(tower.getSpawn().getImportance() < topImportance)
					{
						topImportance = tower.getSpawn().getImportance();
						tempTopImportance = tower;
					}
				}
			}
		}
		
		// enemy towers - take from the highest priority
		if(tempTopImportance == null)
		{
			topImportance = 0;
			
			for(Tower tower : getEventData(instanceId)._towers)
			{
				if(tower != null && tower.getOwningTeam() != teamId)
				{
					if(tower.getSpawn().getImportance() > topImportance)
					{
						topImportance = tower.getSpawn().getImportance();
						tempTopImportance = tower;
					}
				}
			}
		}
		
		if(tempTopImportance != null)
		{
			zone = tempTopImportance.getSpawn();
		}
		
		return zone;
	}
	
	protected void spawnTowers(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning bases for instance " + instanceId);

		clearMapHistory(-1, SpawnType.Base);
		
		NpcData base;
		
		int i = 0;
		for(EventSpawn sp : _manager.getMap().getSpawns(-1, SpawnType.Base))
		{
			i++;
			base = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _towerNpcId, instanceId, "Base " + (i), "Domination event");
			getEventData(instanceId).addTower(base, sp.getRadius(), sp);
		}
	}
	
	protected void unspawnTowers(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: unspawning base for instance " + instanceId);

		for(Tower tower : getEventData(instanceId)._towers)
		{
			if(tower.getNpc() != null)
			{
				tower.setOwningTeam(0, false);
				tower.getNpc().deleteMe();
			}
		}
	}
	
	protected void setBaseEffects(int teamId, NpcData baseNpc)
	{
		if(getBoolean("allowBaseNpcEffects") && _teamsCount == 2)
		{
			if(teamId == 1)
			{
				baseNpc.stopAbnormalEffect(0x000004); // red circle effect
				baseNpc.startAbnormalEffect(0x200000); // imprissing 1 effect
			}
			else if(teamId == 2)
			{
				baseNpc.stopAbnormalEffect(0x200000); // imprissing 1 effect
				baseNpc.startAbnormalEffect(0x000004); // red circle effect
			}
			else // stop effects
			{
				baseNpc.stopAbnormalEffect(0x000004); // red circle effect
				baseNpc.stopAbnormalEffect(0x200000); // imprissing 1 effect
			}
		}
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 
		
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
				for(BattlefieldEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();
						unspawnTowers(match.getInstance().getId());
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
		tick ++;
		if(tick % _towerCheckInterval != 0)
			return;
		
		// players which will be considered as standing near to a base
		Map<Integer, List<PlayerEventInfo>> players;

		int instanceId;
		int baseX, baseY, baseZ;
		int radius;
		
		TowerData towerData;
		Tower tower;
		NpcData towerNpc;
		
		Map<Integer, List<NpcData>> ownedTowers;
		
		// go through the instances
		for(BattlefieldEventInstance instance : _matches.values())
		{
			instanceId = instance.getInstance().getId();
			
			if(tick % 10 == 0)
			{
				for(EventTeam team : _teams.get(instanceId).values())
				{
					startRadar(instanceId, team);
				}
			}
			
			towerData = getEventData(instanceId);
			
			ownedTowers = new FastMap<Integer, List<NpcData>>();
			
			// go through the towers
			for(int i = 0; i < towerData._towers.length; i++)
			{
				players = new FastMap<Integer, List<PlayerEventInfo>>(_teamsCount);
				
				tower = towerData._towers[i];
				towerNpc = towerData._towers[i].getNpc();
				radius = towerData._towers[i].getRadius();
				
				baseX = towerNpc.getLoc().getX();
				baseY = towerNpc.getLoc().getY();
				baseZ = towerNpc.getLoc().getZ();
				
				// calculate players near the base
				for(PlayerEventInfo player : getPlayers(instanceId))
				{
					// see if the PLAYER stands in squared distance LOWER than the radius, it adds him to this collection
					if(player.getDistanceSq(baseX, baseY, baseZ) <= radius)
					{
						if(player.isVisible() && !player.isDead())
						{
							if(!players.containsKey(player.getTeamId()))
								players.put(player.getTeamId(), new FastList<PlayerEventInfo>());
							
							players.get(player.getTeamId()).add(player);
						}
					}
				}
				
				// get the team with most players near the base
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
					
					// more teams have the same count of players in near the base
					else if(highestCount != 0 && teamData.getValue().size() == highestCount)
					{
						isThereMajorityTeam = false;
						break;
					}
				}
				
				// if there is a team with more players near the base than the other teams
				if(isThereMajorityTeam && team != 0)
				{
					final int majorityTeamPlayersCount = players.get(team).size();
					
					// calculate if the team really owns base based on configs provided
					boolean dominatesBase = false;
					
					// don't care about percents, this team already has more players than the others
					if(_percentMajorityToCapture == 0)
					{
						dominatesBase = true;
					}
					
					// check if there are only the team's players in the base and nobody else
					else if(_percentMajorityToCapture == 100)
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
						{
							dominatesBase = true;
						}
					}
					
					// else calculate and check percentual majority
					else
					{
						boolean teamWithMorePlayers = false;
						
						for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
						{
							if(teamData.getKey() == team)
								continue;
							
							double d = ((double) teamData.getValue().size() / (double) majorityTeamPlayersCount);
							int percent = 100 - ((int) (d * 100));
							
							if(percent < _percentMajorityToCapture)
							{
								teamWithMorePlayers = true;
								break;
							}
						}
						
						if(!teamWithMorePlayers)
						{
							dominatesBase = true;
						}
					}
					
					// check if there's required count of players near the base to capture it (eg. you shouldn't be able to capture the base with just 1 player)
					if(dominatesBase)
					{
						int countInTeam = _teams.get(instanceId).get(team).getPlayers().size();
						int minCountOfPlayersNearTheBase;
						
						if(isMinPlayersToCaptureTheBaseInPercent)
							minCountOfPlayersNearTheBase = (int) Math.round(((double)countInTeam * ((double)_minPlayersToCaptureTheBase*0.01)));
						else
							minCountOfPlayersNearTheBase = _minPlayersToCaptureTheBase;
						
						if(minCountOfPlayersNearTheBase < 1)
							minCountOfPlayersNearTheBase = 1;
						
						if(majorityTeamPlayersCount < minCountOfPlayersNearTheBase)
						{
							if(tick % 2 == 0)
							{
								for(PlayerEventInfo player : players.get(team))
								{
									if(player != null && player.isOnline())
									{
										player.sendMessage("At least " + minCountOfPlayersNearTheBase + " players from your team are required to capture a base.");
									}
								}
							}
							
							dominatesBase = false;
						}
					}
					
					// one team has a majority of players near the base
					// do required checks and capture/recapture the base if needed
					if(dominatesBase)
					{
						// tower has no owner yet - set the owner to the dominating team
						if(tower.getOwningTeam() == 0)
						{
							if(tower.setCapturingTime(tower.getCapturingTime() + _towerCheckInterval))
							{
								announce(instanceId, "* " + _teams.get(instanceId).get(team).getFullName() + " has gained the control of base " + (i+1));

								if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
								{
									for(PlayerEventInfo player : tower.getEffectedPlayers())
									{
										if(player != null)
										{
											tower.removeEffectedPlayer(player);
											player.stopAbnormalEffect(player.getTeamId() == 1 ? CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1() : CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
										}
									}
									tower.resetEffectedPlayers();
								}
								
								tower.setOwningTeam(team, true);
								setBaseEffects(team, towerNpc);
								
								towerNpc.setTitle("Owner: " + _teams.get(instanceId).get(team).getFullName());
								towerNpc.broadcastNpcInfo();
								
								// give score to everyone who captured the base
								for(PlayerEventInfo player : players.get(team))
								{
									getPlayerData(player).raiseScore(_scoreForCapturingTower);
									setScoreStats(player, getPlayerData(player).getScore());
									
									// update title
									if(player.isTitleUpdated())
									{
										player.setTitle(getTitle(player), true);
										player.broadcastTitleInfo();
									}
									
									CallbackManager.getInstance().playerScores(getEventType(), player, _scoreForCapturingTower);
								}
							}
							else if(tower.getCapturingTime() == _towerCheckInterval)
								announce(instanceId, _teams.get(instanceId).get(team).getFullName() + " is now capturing base " + (i+1));
						}
						// tower has a different owner - re set the owner to the currently dominating team
						else if(tower.getOwningTeam() != team)
						{
							if(tower.setCapturingTime(tower.getCapturingTime() + _towerCheckInterval))
							{
								announce(instanceId, "* " + _teams.get(instanceId).get(team).getFullName() + " has gained the control of base " + (i+1));
								
								if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
								{
									for(PlayerEventInfo player : tower.getEffectedPlayers())
									{
										if(player != null)
										{
											tower.removeEffectedPlayer(player);
											player.stopAbnormalEffect(player.getTeamId() == 1 ? CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1() : CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
										}
									}
									tower.resetEffectedPlayers();
								}
								
								tower.setOwningTeam(team, true);
								setBaseEffects(team, towerNpc);
								
								towerNpc.setTitle("Owner: " + _teams.get(instanceId).get(team).getFullName());
								towerNpc.broadcastNpcInfo();
								
								// give score to everyone who captured the base
								for(PlayerEventInfo player : players.get(team))
								{
									getPlayerData(player).raiseScore(_scoreForCapturingTower);
									setScoreStats(player, getPlayerData(player).getScore());
									
									// update title
									if(player.isTitleUpdated())
									{
										player.setTitle(getTitle(player), true);
										player.broadcastTitleInfo();
									}
									
									CallbackManager.getInstance().playerScores(getEventType(), player, _scoreForCapturingTower);
								}
							}
							else if(tower.getCapturingTime() == _towerCheckInterval)
								announce(instanceId, _teams.get(instanceId).get(team).getFullName() + " is now capturing base " + (i+1));
						}
						else
						{
							// team still dominates its own tower, but it owns it already
						}
					}
					else
					{
						// dont do anything, towers don't need to be guarded by the players
						if(tower.getCapturingTime() > 0)
						{
							tower.setCapturingTime(0);
						}
					}
				}
				else
				{
					// dont do anything, towers don't need to be guarded by the players
					if(tower.getCapturingTime() > 0)
					{
						tower.setCapturingTime(0);
					}
				}
				
				if(tower.getOwningTeam() > 0)
				{
					// effect while staying close to your tower
					if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
					{
						if(players.containsKey(tower.getOwningTeam()))
						{
							for(PlayerEventInfo player : players.get(tower.getOwningTeam()))
							{
								if(!tower.containsEffectedPlayer(player))
								{
									tower.addEffectedPlayer(player);
									player.startAbnormalEffect(player.getTeamId() == 1 ? CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1() : CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
								}
							}
						}
						
						for(PlayerEventInfo player : tower.getEffectedPlayers())
						{
							if(!players.containsKey(tower.getOwningTeam()) || !players.get(tower.getOwningTeam()).contains(player))
							{
								tower.removeEffectedPlayer(player);
								player.stopAbnormalEffect(player.getTeamId() == 1 ? CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1() : CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
							}
						}
					}
					
					// just raise the owning time for the current owning team
					tower.raiseOwnedTime(_towerCheckInterval);
					
					// add this base to the team's owned bases
					if(!ownedTowers.containsKey(tower.getOwningTeam()))
						ownedTowers.put(tower.getOwningTeam(), new FastList<NpcData>());
					
					ownedTowers.get(tower.getOwningTeam()).add(towerNpc);
				}
			}
			
			if(_scoreType.equals("AllTeams"))
			{
				int countOfTowers;
				int team;
				
				_minTowersToOwnToScore = 1;
				
				for(Entry<Integer, List<NpcData>> e : ownedTowers.entrySet())
				{
					team = e.getKey();
					countOfTowers = e.getValue().size();
					
					if(countOfTowers >= _minTowersToOwnToScore && countOfTowers > 0)
					{
						_teams.get(instanceId).get(team).raiseScore(countOfTowers);
					}
				}
			}
			else if(_scoreType.equals("DominatingTeam"))
			{
				// then add proper announcements
				
				boolean ownsRequiredCountOfBases = false;
				
				int teamWithMostBases = 0;
				int mostBasesCount = 0;
				
				for(Entry<Integer, List<NpcData>> e : ownedTowers.entrySet())
				{
					if(e.getValue().size() > mostBasesCount)
					{
						teamWithMostBases = e.getKey();
						mostBasesCount = e.getValue().size();
						ownsRequiredCountOfBases = true;
					}
					
					// at least two teams with the same owned bases count
					else if(e.getValue().size() != 0 && e.getValue().size() == mostBasesCount)
					{
						ownsRequiredCountOfBases = false;
						break;
					}
				}
				
				if(ownsRequiredCountOfBases)
					ownsRequiredCountOfBases = mostBasesCount >= _minTowersToOwnToScore;
					
				if(ownsRequiredCountOfBases)
				{
					// new dominator
					if(teamWithMostBases != towerData._dominatingTeam)
					{
						announce(instanceId, "++ " + _teams.get(instanceId).get(teamWithMostBases).getFullName() + " owns most bases - " + mostBasesCount + "!");
						
						towerData.setDominatingTeam(teamWithMostBases);
						towerData.resetDominatingTime(); // new owner, reset dominating time
					}
					else // current owner continues in his domination - raise the time he owns it
					{
						towerData.raiseDominatingTime(_towerCheckInterval);
					}
					
					// check if it's been running it enough long to give score
					if(towerData.getDominatingTime() >= _holdAllTowersFor)
					{
						_teams.get(instanceId).get(teamWithMostBases).raiseScore(_scoreForCapturingTower);
						
						towerData.resetDominatingTime();
						
						if(_holdAllTowersFor > 5)
						{
							announce(instanceId, "*** " + _teams.get(instanceId).get(teamWithMostBases).getTeamName() + "s scored for owning " + mostBasesCount + " bases!");
							
							if(getBoolean("allowFireworkOnScore"))
							{
								for(Tower tow : towerData._towers)
								{
									if(tow.getNpc() != null && tow.getOwningTeam() == towerData.getDominatingTeam())
										tow.getNpc().broadcastSkillUse(tow.getNpc(), tow.getNpc(), 2024, 1);
								}
							}
						}
					}
					else // left to score announce
					{
						int toHold = _holdAllTowersFor - towerData._holdingAllTowersFor;
						boolean announce = false;
						
						if(towerData._holdingAllTowersFor == 0)
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
							
							announce(instanceId, " ** " + LanguageEngine.getMsg("mDom_leftToScore", toHold, (min ? "minutes" : "seconds"), _teams.get(instanceId).get(teamWithMostBases).getFullName()));
						}
					}
				}
				else
				{
					if(towerData.getDominatingTeam() != 0)
					{
						if(towerData.getDominatingTime() > 0)
						{
							announce(instanceId, "-- " +  _teams.get(instanceId).get(towerData._dominatingTeam).getFullName() + " has lost domination of bases.");
						}
					}
					
					towerData.setDominatingTeam(0);
					towerData.resetDominatingTime();
				}
			}
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(BattlefieldEventInstance match : _matches.values())
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
		
		if(!map.checkForSpawns(SpawnType.Base, -1, 1))
			tb.append(addMissingSpawn(SpawnType.Base, 0, 1));
		
		return tb.toString();
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		int owningTeam = _matches.get(instance)._towerData._dominatingTeam;
		String status = "<font color=ac9887>Dominates:</font> <font color=" + EventManager.getInstance().getDarkColorForHtml(owningTeam) + ">" + EventManager.getInstance().getTeamName(owningTeam) + " team</font>";
		
		return("<table width=510 bgcolor=3E3E3E><tr><td width=510 align=center>" + status + "</td></tr></table>");
	}
	
	@Override
	public String getHtmlDescription()
	{
		//TODO rewrite
		if(_htmlDescription == null)
		{
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs());
			}
			else
			{
				_htmlDescription = "No information about this event yet.";
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
	protected TowerData createEventData(int instance)
	{
		return new TowerData(instance);
	}
	
	@Override
	protected BattlefieldEventInstance createEventInstance(InstanceData instance)
	{
		return new BattlefieldEventInstance(instance);
	}
	
	@Override
	protected TowerData getEventData(int instance)
	{
		return _matches.get(instance)._towerData;
	}
}
