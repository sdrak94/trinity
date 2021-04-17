package cz.nxs.events.engine.main.events;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
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
import cz.nxs.interf.delegate.SkillData;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 *
 *
 * TODO: permit using striders (dismount on enter of event, disallow using the collar item id
 */
public class Commanders extends TeamVsTeam
{
	protected class ComsEventData extends TvTEventData
	{
		private final Map<Integer, PlayerEventInfo> _commanders;
		private final Map<Integer, NpcData> _bases;
		
		public ComsEventData(int instance)
		{
			super(instance);
			
			_commanders = new FastMap<Integer, PlayerEventInfo>();
			_bases = new FastMap<Integer, NpcData>();
		}
		
		public PlayerEventInfo getCommander(int team)
		{
			return _commanders.get(team);
		}
		
		public void setCommander(int team, PlayerEventInfo commander)
		{
			_commanders.put(team, commander);
		}
		
		public NpcData getBase(int team)
		{
			return _bases.get(team);
		}
		
		public void setBase(int team, NpcData base)
		{
			_bases.put(team, base);
		}
	}
	
	protected int _baseNpcId;
	protected int _countOfSuperiorTeams;
	
	protected class ComsEventInstance extends TvTEventInstance
	{
		protected ComsEventInstance(InstanceData instance)
		{
			super(instance);
		}
		
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
							
							setupTeams(_instance.getId(), _countOfSuperiorTeams);
							
							// action done for some hardcoded maps
							removeStaticDoors(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							spawnCommanderStuff(_instance.getId(), true);
							handleSkills(_instance.getId(), true);
							preparePlayers(_instance.getId(), true);
							
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
	
	protected int _tick;
	
	private FastMap<Integer, Integer> _skillsForAll;

	public Commanders(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("skillsForAllPlayers", "35100-1", "IDs of skills which will be given to players on the event. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35000-1').", InputType.MultiAdd));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		// load skills for all players
		if(!getString("skillsForAllPlayers").equals(""))
		{
			String[] splits = getString("skillsForAllPlayers").split(",");
			_skillsForAll = new FastMap<Integer, Integer>();
			
			try
			{
				String id, level;
				for(int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForAll.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForAllPlayers' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		_tick = 0;
	}
	
	protected int getCountOfTeams(int instanceId)
	{
		final int countOfPlayersUnderOneCommander = 50;
		int countOfPlayers = getPlayers(instanceId).size();
		int countOfTeams = countOfPlayers / countOfPlayersUnderOneCommander;
		
		if(countOfTeams % 2 != 0)
		{
			countOfTeams -= 1;
		}
		
		return countOfTeams;
	}
	
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		_teamsCount = getCountOfTeams(type.getInstance().getId());
		
		if(_teamsCount < 2) 
			_teamsCount = 2;
		
		createTeams(_teamsCount, type.getInstance().getId());
		
		return _teamsCount;
	}
	
	//TODO
	@Override
	protected void createTeams(int count, int instanceId)
	{
		try
		{
			/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: creating " + count + " teams for instanceId " + instanceId);

			for(int i = 0; i < count; i++)
			{
				createNewTeam(instanceId, count+1, "Noneyet", "Noneyet"); //TODO set teams name later
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void createNewTeam(int instanceId, int id, String name, String fullName)
	{
		_teams.get(instanceId).put(id, new EventTeam(null, id, name, fullName));
	}
	
	protected void preparePlayers(int instanceId, boolean start)
	{
		//
	}
	
	protected void setupTeams(int instanceId, int maxSuperTeams)
	{
		int superTeam = 1;
		
		for(EventTeam team : _teams.get(instanceId).values())
		{
			for(PlayerEventInfo player : team.getPlayers())
			{
				getPlayerData(player).setSuperTeam(superTeam);
			}
			
			if(superTeam == maxSuperTeams)
				superTeam = 1;
			superTeam ++;
		}
	}
	
	/** if spawn==false, it despawns all */
	protected void spawnCommanderStuff(int instanceId, boolean spawn)
	{
		if(spawn)
		{
			clearMapHistory(-1, SpawnType.Base);
			
			for(EventTeam team : _teams.get(instanceId).values())
			{
				EventSpawn sp = getSpawn(SpawnType.Base, team.getTeamId());
				NpcData base = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _baseNpcId, instanceId, "Base", "Team Base");
				
				getEventData(instanceId).setBase(team.getTeamId(), base);
			}
		}
		else
		{
			NpcData base = null;
			for(EventTeam team : _teams.get(instanceId).values())
			{
				base = getEventData(instanceId).getBase(team.getTeamId());
				if(base != null)
				{
					base.deleteMe();
				}
			}
		}
	}
	
	protected void setNewCommander(int instanceId, PlayerEventInfo newCommander, int teamId)
	{
		announce(instanceId, "*** Your commander is " + newCommander.getPlayersName(), teamId);
	}
	
	protected void commanderAction(int instanceId, int teamid, String action)
	{
		
	}
	
	protected void hiveDead(int instanceId, PlayerEventInfo newCommander, int teamId)
	{
		
	}
	
	protected void handleSkills(int instanceId, boolean add)
	{
		if(_skillsForAll != null)
		{
			SkillData skill = null;
			
			for(PlayerEventInfo player : getPlayers(instanceId))
			{
				if(add)
				{
					for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						
						if(skill.exists())
							player.addSkill(skill, false);
					}
					
					player.sendSkillList();
				}
				else
				{
					for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if(skill.exists())
							player.removeSkill(skill.getId());
					}
				}
			}
		}
	}
	
	@Override
	protected void clockTick()
	{
		
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
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{

	}
	
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null) // TODO make exception for the base npc
			return true;
		else if(target.getEventInfo().getEvent() != player.getEvent())
			return false;
		else if(getPlayerData(target.getEventInfo()).getSuperTeam() == getPlayerData(player).getSuperTeam())
			return false;
		
		if(isCommander(player))
		{
			player.sendMessage("The commander can't attack.");
			return false;
		}
		
		return true;
	}
	
	protected boolean isCommander(PlayerEventInfo player)
	{
		if(getPlayerData(player).isCommander())
			return true;
		return false;
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null || target.getEventInfo().getEvent() != player.getEvent())
			return false;
		
		if(getPlayerData(player).getSuperTeam() == getPlayerData(target.getEventInfo()).getSuperTeam())
			return true;
		
		return false;
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		return false;
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
					{
						match.abort();
						
						spawnCommanderStuff(match.getInstance().getId(), false);
						handleSkills(match.getInstance().getId(), false);
						preparePlayers(match.getInstance().getId(), false);
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
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new CommandersPlayerData(player, this);
	}

	@Override
	public CommandersPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (CommandersPlayerData) player.getEventData();
	}
	
	public class CommandersPlayerData extends PvPEventPlayerData
	{
		boolean _commander;
		int _superTeam;
		
		public CommandersPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
			
			_commander = false;
		}
		
		protected void setSuperTeam(int i)
		{
			_superTeam = i;
		}
		
		protected int getSuperTeam()
		{
			return _superTeam;
		}
		
		@SuppressWarnings("unused")
		private void setCommander(boolean b)
		{
			_commander = b;
		}
		
		protected boolean isCommander()
		{
			return _commander;
		}
	}
	
	@Override
	protected TvTEventData createEventData(int instanceId)
	{
		return new ComsEventData(instanceId);
	}
	
	@Override
	protected ComsEventInstance createEventInstance(InstanceData instance)
	{
		return new ComsEventInstance(instance);
	}
	
	@Override
	protected ComsEventData getEventData(int instance)
	{
		return (ComsEventData) _matches.get(instance)._data;
	}
}
