package cz.nxs.events.engine.main.events;

import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventRewardSystem;
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
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class CaptureTheFlag extends AbstractMainEvent
{
	private class FlagData extends AbstractEventData
	{
		public int _instance;
		
		public int team;
		
		public NpcData flagNpc;
		public NpcData flagHolder;
		
		/** 1 = flag is in the holder <br>
		 *  2 = enemy player has the flag <br>
		 *  3 = flag is dropped on the ground, waiting to be picked up <br>
		 */
		public int status;
		
		public PlayerEventInfo flagOwner;
		
		public FlagReturnTask returnTask;
		
		public FlagData(int instance)
		{
			super(instance);
			
			_instance = instance;
			
			this.status = 1;
			this.flagNpc = null;
			this.flagHolder = null;
			this.flagOwner = null;
		}
		
		protected void setTeam(int team)
		{
			this.team = team;
			this.returnTask = new FlagReturnTask(team, _instance);
		}
	}
	
	private class CTFEventInstance extends AbstractEventInstance
	{
		private CaptureTheFlag.EventState _state;
		
		// one FlagData (EventData) per flag
		protected final CaptureTheFlag.FlagData[] _flags;
		
		protected CTFEventInstance(InstanceData instance)
		{
			super(instance);
			
			_state = EventState.START;
			
			_flags = new FlagData[_teamsCount];
			
			for(EventTeam team : _teams.get(instance.getId()).values())
			{
				_flags[team.getTeamId() - 1] = new FlagData(instance.getId());
				_flags[team.getTeamId() - 1].setTeam(team.getTeamId());
			}
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
							
							spawnFlags(_instance.getId());
							
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
						
						unspawnFlags(_instance.getId());
						
						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), 0, getInt("killsForReward"));
							
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

	private enum EventState
	{
		START, FIGHT, END, TELEPORT, INACTIVE
	}
	
	private FastMap<Integer, CTFEventInstance> _matches;
	
	private boolean _waweRespawn;
	protected int _teamsCount;
	private int _flagNpcId;
	private int _holderNpcId;
	private int _flagItemId;
	
	private boolean _returnFlagOnDie;
	private boolean _interactDistCheck;
	
	// TODO implement this
	//private int _flagTakeDelay;

	public CaptureTheFlag(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FlagScore, RewardPosition.FlagReturn, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("killsForReward", "0", "The minimum kills count required to get a reward (includes all possible rewards)."));
		addConfig(new ConfigModel("resDelay", "15", "The delay after which the player is resurrected. In seconds."));
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system.", InputType.Boolean));
		
		addConfig(new ConfigModel("flagSkillId", "-1", "Skill given to all players holding a flag. Possible to create for example a slow effect using a passive skill. -1 to disable."));
		
		addConfig(new ConfigModel("flagNpcId", "8990", "Flag NPC Id. Same for all teams, only title/name will change."));
		addConfig(new ConfigModel("flagHolderNpcId", "8991", "Flag Holder NPC Id. Same for all teams, only title/name will change."));
		
		addConfig(new ConfigModel("teamsCount", "2", "The ammount of teams in the event. Max is 3 for CTF. <font color=FF0000>In order to change the count of teams in the event, you must also edit this config in the Instance's configuration.</font>"));
		
		addConfig(new ConfigModel("afkReturnFlagTime", "99999", "The time after which will the flag be returned from AFKing player back to it's holder. -1 to disable, value in ms. NOT WORKING CURRENTLY."));
		addConfig(new ConfigModel("flagReturnTime", "120000", "The time after which will the flag be returned from player back to it's holder. -1 to disable, value in ms."));
		
		addConfig(new ConfigModel("createParties", "true", "Put 'True' if you want this event to automatically create parties for players in each team.", InputType.Boolean));
		addConfig(new ConfigModel("maxPartySize", "9", "The maximum size of party, that can be created. Works only if <font color=LEVEL>createParties</font> is true."));
		
		addConfig(new ConfigModel("flagItemId", "13535", "The item ID of the flag item."));
		addConfig(new ConfigModel("returnFlagOnDie", "false", "Put true to return the flag when a player holding it dies back to enemy's flag holder. Put false to make it so the flag drops on ground and can be picked up by your teammate or returned by your enemy.", InputType.Boolean));
		addConfig(new ConfigModel("npcInteractDistCheck", "false", "You can turn off/on the Flag/Holder NPC interract distance checker here.", InputType.Boolean));
		
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
		
		_flagNpcId = getInt("flagNpcId");
		_holderNpcId = getInt("flagHolderNpcId");
		_flagItemId = getInt("flagItemId");
		_runningInstances = 0;
		
		_returnFlagOnDie = getBoolean("returnFlagOnDie");
		_interactDistCheck = getBoolean("npcInterractDistCheck");
		
		//_flagTakeDelay = getInt("config"); //TODO
	}
	
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		_teamsCount = type.getConfigInt("teamsCount");
		
		if(_teamsCount < 2 || _teamsCount > 5)
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
		
		CTFEventInstance match;
		_matches = new FastMap<Integer, CTFEventInstance>();
		
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
		rewardAllTeams(-1, 0, minKills);
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
		
		CTFEventInstance match = _matches.get(instance);
		
		if(count <= 3 && match != null && match.getClock() != null)
			tb.append(LanguageEngine.getMsg("event_scorebar_time", match.getClock().getTime()));
		
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
	public synchronized boolean onNpcAction(PlayerEventInfo player, NpcData npc)
	{
		int instance = player.getInstanceId();
		CTFEventInstance match = _matches.get(instance);
		
		if(_interactDistCheck && player.getPlanDistanceSq(npc.getLoc().getX(), npc.getLoc().getY()) > 30000)
		{
			player.sendMessage(LanguageEngine.getMsg("ctf_tooFar"));
			return false;
		}
		
		boolean isFlag = false;
		boolean isHolder = false;
		int npcTeam = 0;
		
		FlagData data = null;
		
		if(match._flags == null)
			return false;
		
		for(FlagData d : match._flags)
		{
			if(d.flagNpc != null && d.flagNpc.getObjectId() == npc.getObjectId())
			{
				isFlag = true;
				data = d;
				
				npcTeam = d.team;
			}
			else if(d.flagHolder.getObjectId() == npc.getObjectId())
			{
				isHolder = true;
				data = d;
				
				npcTeam = d.team;
			}
		}
		
		if(data == null)
			return false;
		
		int status = data.status;
		
		if(isHolder)
		{
			// SAME TEAM
			if(npcTeam == player.getTeamId())
			{
				if(status == 1)
				{
					if(getPlayerData(player).hasFlag > 0)
					{
						//TODO add delay
						
						screenAnnounce(instance, LanguageEngine.getMsg("ctf_score", player.getPlayersName(), player.getEventTeam().getFullName(), getTeamName(getPlayerData(player).hasFlag)));
						
						player.getEventTeam().raiseScore(1);
						
						getPlayerData(player).raiseScore(1);
						setScoreStats(player, getPlayerData(player).getScore());
						
						EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.FlagScore, null, player.getTotalTimeAfk(), 0, 0);
						
						returnFlag(getPlayerData(player).hasFlag, false, false, instance, false);
						
						if(player.isTitleUpdated())
						{
							player.setTitle(getTitle(player), true);
							player.broadcastTitleInfo();
						}
						
						CallbackManager.getInstance().playerFlagScores(getEventType(), player);
						
						player.sendMessage(LanguageEngine.getMsg("ctf_score_player"));
					}
					else
					{
						player.sendMessage(LanguageEngine.getMsg("ctf_goForEnemyFlag"));
					}
					
					return true;
				}
				else if(status == 2)
				{
					player.sendMessage(LanguageEngine.getMsg("ctf_yourFlagStolen"));
					return true;
				}
				else if(status == 3)
				{
					player.sendMessage(LanguageEngine.getMsg("ctf_yourFlagStolen"));
					return true;
				}
			}
			// ENEMY TEAM
			else
			{
				if(status == 1)
				{
					//TODO add delay
					
					equipFlag(player, npcTeam);
					player.creatureSay(LanguageEngine.getMsg("ctf_flagTaken"), "CTF", 15);
					
					return true;
				}
				else if(status == 2)
				{
					player.sendMessage(LanguageEngine.getMsg("ctf_flagStolen"));
					return true;
				}
				else if(status == 3)
				{
					player.sendMessage(LanguageEngine.getMsg("ctf_flagStolen"));
					return true;
				}
			}
		}
		else if(isFlag)
		{
			// SAME TEAM
			if(npcTeam == player.getTeamId())
			{
				if(status == 1)
				{
					if(getPlayerData(player).hasFlag > 0)
					{
						//TODO add delay
						
						screenAnnounce(instance, LanguageEngine.getMsg("ctf_score", player.getPlayersName(), player.getEventTeam().getFullName(), getTeamName(getPlayerData(player).hasFlag)));
						
						player.getEventTeam().raiseScore(1);
						
						getPlayerData(player).raiseScore(1);
						setScoreStats(player, getPlayerData(player).getScore());
						
						EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.FlagScore, null, player.getTotalTimeAfk(), 0, 0);
						
						returnFlag(getPlayerData(player).hasFlag, false, false, instance, false);
						
						if(player.isTitleUpdated())
						{
							player.setTitle(getTitle(player), true);
							player.broadcastTitleInfo();
						}
						
						CallbackManager.getInstance().playerFlagScores(getEventType(), player);
						
						player.sendMessage(LanguageEngine.getMsg("ctf_score_player"));
					}
					else
						player.sendMessage(LanguageEngine.getMsg("ctf_goForEnemyFlag"));
					
					return true;
				}
				else if(status == 3)
				{
					//TODO add delay
					
					EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.FlagReturn, null, player.getTotalTimeAfk(), 0, 0);
					
					returnFlag(npcTeam, false, false, instance, false);
					screenAnnounce(instance, LanguageEngine.getMsg("ctf_flagReturned", getTeamName(npcTeam), player.getPlayersName()));
					return true;
				}
			}
			// ENEMY TEAM
			else
			{
				if(status == 1)
				{
					//TODO add delay
					
					equipFlag(player, npcTeam);
					player.creatureSay(LanguageEngine.getMsg("ctf_flagTaken"), "CTF", 15);
					return true;
				}
				else if(status == 3)
				{
					//TODO add delay
					
					equipFlag(player, npcTeam);
					player.creatureSay(LanguageEngine.getMsg("ctf_flagTaken"), "CTF", 15);
					return true;
				}
			}
		}
		
		return false;
	}
	
	/** delay in ms */
	//TODO
	/*private void sendDelayBar(PlayerEventInfo player, int delay)
	{
		//TODO implement this here
		player.sendSetupGauge(delay);
	}*/
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		if (player.getTeamId() != target.getEventInfo().getTeamId())
		{
			tryFirstBlood(player);
			giveOnKillReward(player);
			
			player.getEventTeam().raiseKills(1);
			
			getPlayerData(player).raiseKills(1);
			getPlayerData(player).raiseSpree(1);
			
			giveKillingSpreeReward(getPlayerData(player));
			
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
		
		if(getPlayerData(player).hasFlag > 0)
		{
			if(_returnFlagOnDie)
			{
				screenAnnounce(player.getInstanceId(), LanguageEngine.getMsg("ctf_flagReturned2", getTeamName(getPlayerData(player).hasFlag)));
				
				EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.FlagReturn, null, player.getTotalTimeAfk(), 0, 0);
				
				returnFlag(getPlayerData(player).hasFlag, false, false, player.getInstanceId(), false);
			}
			else
			{
				dropFlag(getPlayerData(player).hasFlag, player.getInstanceId());
			}
		}
		
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		if(_waweRespawn)
			_waweScheduler.addPlayer(player);
		else
			scheduleRevive(player, getInt("resDelay") * 1000);
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if(getPlayerData(player).hasFlag > 0 && (item.isWeapon() || (item.isArmor() && item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_HAND())) || item.getItemId() == _flagItemId)
		{
			if(item.getItemId() == _flagItemId && !item.isEquipped())
				return true;
			return false;
		}
		
		if(getPlayerData(player).hasFlag == 0 && item.getItemId() == _flagItemId)
		{
			if(item.isEquipped())
				return true;
			else
				return false;
		}
		
		return super.canUseItem(player, item);
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		if(getPlayerData(player).hasFlag > 0 && item.getItemId() == _flagItemId)
			return false;
		
		return super.canDestroyItem(player, item);
	}
	
	@Override
	public boolean canBeDisarmed(PlayerEventInfo player)
	{
		if(getPlayerData(player).hasFlag > 0)
		{
			return false;
		}
		return true;
	}
	
	private void spawnFlags(int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning flags for instanceId " + instance); 

		clearMapHistory(-1, SpawnType.Flag);
		
		CTFEventInstance match = _matches.get(instance);
		
		EventSpawn sp;
		
		for(EventTeam team : _teams.get(instance).values())
		{
			sp = getSpawn(SpawnType.Flag, team.getTeamId());
			match._flags[team.getTeamId() - 1].flagNpc = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _flagNpcId, instance, getTeamName(team.getTeamId()) + " Flag", getTeamName(team.getTeamId()) + " Team");
			match._flags[team.getTeamId() - 1].flagHolder = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _holderNpcId, instance, getTeamName(team.getTeamId()) + " Holder", "");
			
			match._flags[team.getTeamId() - 1].flagNpc.setEventTeam(team.getTeamId());
			match._flags[team.getTeamId() - 1].flagHolder.setEventTeam(team.getTeamId());
		}
	}
	
	private void unspawnFlags(int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: unspawning flags for instanceId " + instance);

		for(FlagData data : _matches.get(instance)._flags)
		{
			data.returnTask.abort();
			
			if(data.flagNpc != null)
				data.flagNpc.deleteMe();
			
			if(data.flagHolder != null)
				data.flagHolder.deleteMe();
		}
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new CTFEventPlayerData(player, this);
	}

	@Override
	public CTFEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (CTFEventPlayerData) player.getEventData();
	}
	
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: called CLEAREVENT for instance " + instanceId);
		
		try
		{
			if(_matches != null)
			{
				for(CTFEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();
						
						for(FlagData flag : match._flags)
						{
							if(flag.flagOwner != null)
							{
								unequipFlag(flag.flagOwner);
							}
						}
						
						unspawnFlags(match.getInstance().getId());
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
			
			removeFlagFromPlayer(player);
			
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
			debug("Error on respawnPlayer - no spawn type REGULAR, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if(_matches == null)
			return "Starting";
		
		for(CTFEventInstance match : _matches.values())
		{
			if(match.isActive())
				return match.getClock().getTime();
		}
				
		return "N/A";
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		String flags = "";
		int width = 510/_matches.get(instance)._flags.length;
		
		for(FlagData flag : _matches.get(instance)._flags)
		{
			flags += "<td width=" + width + " align=center><font color=" + EventManager.getInstance().getDarkColorForHtml(flag.team) + ">" + EventManager.getInstance().getTeamName(flag.team) + " flag - " + (flag.status == 1 ? "<font color=7f7f7f>Safe</font>" : "<font color=7f7f7f>Stolen!</font>") + "</font></td>";
		}
		
		return("<table width=510 bgcolor=3E3E3E><tr>" + flags + "</tr></table>");
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
				_htmlDescription = "There are " + getInt("teamsCount") + " teams; in order to score you need to steal enemy team's flag and bring it back your team's base (to the flag holder). ";
				
				if(getInt("flagReturnTime") > -1)
					_htmlDescription += "If you hold the flag and don't manage to score within " + getInt("flagReturnTime")/1000 + " seconds, the flag will be returned back to enemy's flag holder. ";
						
				if(getBoolean("waweRespawn"))
					_htmlDescription += "Dead players are resurrected by an advanced wawe-spawn engine each " + getInt("resDelay") + " seconds.";
				else
					_htmlDescription += "If you die, you will be resurrected in " + getInt("resDelay") + " seconds. ";
				
				if(getBoolean("createParties"))
					_htmlDescription += "The event automatically creates parties on start.";
			}
		}
		return _htmlDescription;
	}
	
	private void dropFlag(int flagTeam, int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("/ Event: dropping flag of team " + flagTeam + " in instance " + instance);

		CTFEventInstance match = _matches.get(instance);
		FlagData data = match._flags[flagTeam - 1];
		
		data.returnTask.abort();
		
		if(data.flagNpc != null)
			data.flagNpc.deleteMe();
		
		data.flagNpc = spawnNPC(data.flagOwner.getX(), data.flagOwner.getY(), data.flagOwner.getZ(), _flagNpcId, instance, getTeamName(flagTeam) + " Flag", getTeamName(flagTeam) + " Team");
		data.flagNpc.setEventTeam(flagTeam);
		data.status = 3;
		
		screenAnnounce(instance, data.flagOwner.getPlayersName() + " dropped the " + getTeamName(flagTeam) + " flag.");
		
		if(data.flagOwner != null)
			unequipFlag(data.flagOwner);
		
		/**/ if(NexusLoader.detailedDebug) print("/ Event: drop flag finished");
	}
	
	protected void returnFlag(int flagTeam, boolean timeForced, boolean afkForced, int instance, boolean announce)
	{
		/**/ if(NexusLoader.detailedDebug) print("/ Event: returning flag of team " + flagTeam + " in instance " + instance + ". timeforced = " + timeForced + ", afk forced " + afkForced + ", announce " + announce); 

		CTFEventInstance match = _matches.get(instance);
		
		if(match == null)
			return;
		
		FlagData data = match._flags[flagTeam - 1];
		
		data.returnTask.abort();
		
		if(announce)
		{
			if(afkForced)
			{
				if(data.flagOwner != null)
					announce(instance, LanguageEngine.getMsg("ctf_flagReturn_afk1", data.flagOwner.getPlayersName(), getTeamName(flagTeam)));
				else
					announce(instance, LanguageEngine.getMsg("ctf_flagReturn_afk2", getTeamName(flagTeam)));
			}
			else if(!timeForced)
			{
				screenAnnounce(instance, LanguageEngine.getMsg("ctf_flagReturn", getTeamName(flagTeam)));
			}
			else
			{
				if(data.flagOwner != null)
					announce(instance, LanguageEngine.getMsg("ctf_flagReturn_timeOver1", data.flagOwner.getPlayersName(), getInt("flagReturnTime") / 1000, getTeamName(flagTeam)));
				else
					announce(instance, LanguageEngine.getMsg("ctf_flagReturn_timeOver2", getTeamName(flagTeam)), getInt("flagReturnTime") / 1000);
			}
		}
		
		if (data.flagOwner != null)
			unequipFlag(data.flagOwner);
		
		if (data.status == 3)
			data.flagNpc.deleteMe();
		
		EventSpawn sp = getSpawn(SpawnType.Flag, flagTeam);
		
		data.flagNpc = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _flagNpcId, instance, getTeamName(flagTeam) + " Flag", getTeamName(flagTeam) + " Team");
		data.flagNpc.setEventTeam(flagTeam);
		data.status = 1;
		
		/**/ if(NexusLoader.detailedDebug) print("/ Event: return flag finished");
	}
	
	private void equipFlag(PlayerEventInfo player, int flagTeamId)
	{
		/**/ if(NexusLoader.detailedDebug) print("/ Event: equipping flag of team " + flagTeamId + " on player " + player.getPlayersName() + " (instance " + player.getInstanceId() + ")");

		int instance = player.getInstanceId();
		
		CTFEventInstance match = _matches.get(instance);
		FlagData data = match._flags[flagTeamId - 1];
		
		ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
		if (wpn != null)
			player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());

		wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
		if (wpn != null)
			player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());

		ItemData flagItem = player.addItem(_flagItemId, 1, false);
		player.equipItem(flagItem);
		
		data.flagOwner = player;
		screenAnnounce(instance, LanguageEngine.getMsg("ctf_flagTaken_announce", player.getPlayersName(), getTeamName(flagTeamId)));
		
		data.flagNpc.deleteMe();
		
		data.status = 2;
		
		getPlayerData(player).hasFlag = flagTeamId;
		
		player.broadcastUserInfo();
		
		int id = getInt("flagSkillId");
		if(id != -1)
			player.addSkill(new SkillData(id, 1), false);
		
		data.returnTask.start();
		
		/**/ if(NexusLoader.detailedDebug) print("/ Event: finished equip flag");
	}
	
	private void unequipFlag(PlayerEventInfo player)
	{
		/**/ if(NexusLoader.detailedDebug) print("/ Event: unequipping flag from " + player.getPlayersName() + " in instance " + player.getInstanceId());

		removeFlagFromPlayer(player);
		
		int instance = player.getInstanceId();
		
		for(FlagData flag : _matches.get(instance)._flags)
		{
			if(flag.flagOwner != null && flag.flagOwner.getPlayersId() == player.getPlayersId())
			{
				flag.flagOwner = null;
				flag.returnTask.abort();
			}
		}
		
		getPlayerData(player).hasFlag = 0;
		
		/**/ if(NexusLoader.detailedDebug) print("/ Event: finished unequip flag");
	}
	
	private void removeFlagFromPlayer(PlayerEventInfo player)
	{
		/**/ if(NexusLoader.detailedDebug) print("/ Event: removing flag from player " + player.getPlayersName() + ", instance " + player.getInstanceId());

		ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
		if (wpn.exists())
		{
			ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
			player.destroyItemByItemId(_flagItemId, 1);
			
			player.inventoryUpdate(unequiped);
		}
		
		int id = getInt("flagSkillId");
		if(id != -1)
			player.removeSkill(id);
		
		/**/ if(NexusLoader.detailedDebug) print("/ Event: finished removing flag");
	}
	
	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		if(player.isOnline())
		{
			if(getPlayerData(player).hasFlag > 0)
			{
				/**/ if(NexusLoader.detailedDebug) print("/ Event: removing flag from player " + player.getPlayersName() + " - he disconnected"); 

				announce(LanguageEngine.getMsg("ctf_flagHolderDisconnect", player.getPlayersName(), getTeamName(getPlayerData(player).hasFlag)));
				screenAnnounce(player.getInstanceId(), LanguageEngine.getMsg("ctf_flagReturned2", getTeamName(getPlayerData(player).hasFlag)));
				returnFlag(getPlayerData(player).hasFlag, false, false, player.getInstanceId(), false);
			}
		}
		super.onDisconnect(player);
	}
	
	public String getTeamName(int id)
	{
		for(FastMap<Integer, EventTeam> i : _teams.values())
		{
			for(EventTeam team : i.values())
			{
				if(team.getTeamId() == id)
					return team.getTeamName();
			}
		}
		return "Unknown";
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
			
			if(!map.checkForSpawns(SpawnType.Flag, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.Flag, i+1, 1));
		}
		
		return tb.toString();
	}
	
	public class CTFEventPlayerData extends PvPEventPlayerData
	{
		protected int hasFlag = 0;
		public CTFEventPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
		}
		
		public void setHasFlag(int b)
		{
			hasFlag = b;
		}
		
		public int hasFlag()
		{
			return hasFlag;
		}
	}
	
	private class FlagReturnTask implements Runnable
	{
		private final int team;
		private final int instance;
		private ScheduledFuture<?> future;
		
		public FlagReturnTask(int team, int instance)
		{
			this.team = team;
			this.instance = instance;
		}
		
		public void start()
		{
			if(future != null)
				future.cancel(false);
			
			int time = getInt("flagReturnTime");
			if(time > 0)
				future = CallBack.getInstance().getOut().scheduleGeneral(this, time);
			else
				future = null;
		}
		
		public void abort()
		{
			if(future != null)
				future.cancel(false);
		}
		
		@Override
		public void run() 
		{
			returnFlag(team, true, false, instance, true);
		}
	}

	@Override
	protected AbstractEventInstance getMatch(int instanceId)
	{
		return _matches.get(instanceId);
	}
	
	@Override
	protected CTFEventInstance createEventInstance(InstanceData instance)
	{
		return new CTFEventInstance(instance);
	}
	
	@Override
	protected FlagData createEventData(int instance)
	{
		return new FlagData(instance);
	}
	
	@Override
	protected AbstractEventData getEventData(int instance)
	{
		// no single EventData for this event
		return null;
	}
}
