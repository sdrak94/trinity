package cz.nxs.events.engine.main.events;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.base.MainEventInstanceType;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class Mutant extends Zombies
{
	private static final int PLAYERS_TEAM_ID = 1;
	private static final int MUTANT_TEAM_ID = 2;
	
	protected class MutantEventData extends ZombiesEventData
	{
		public MutantEventData(int instance)
		{
			super(instance);
		}
		
		@Override
		public void setKillMade()
		{
		}
	}
	
	protected class MutantEventInstance extends ZombiesEventInstance
	{
		protected MutantEventInstance(InstanceData instance)
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
							
							// action done for some hardcoded maps
							removeStaticDoors(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							preparePlayers(_instance.getId(), true);
							scheduleSelectMutants(_instance.getId(), 10000, true, 0);
							
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
						
						untransformAll(_instance.getId());
						setAllPlayers(_instance.getId());
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted) 
							{
								int minScore = getInt("killsForReward");
								rewardAllPlayersFromTeam(_instance.getId(), minScore, 0, PLAYERS_TEAM_ID);
							}
							
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
	
	private FastMap<Integer, Integer> _skillsForPlayers;
	private FastMap<Integer, Integer> _skillsForMutant;
	
	private String _mutantCount;
	private int _mutantTransformId;
	private int _mutantMinLevel;
	private int _mutanteMinPvps;
	
	private int _mutantWeaponId;
	
	private int _mutantKillScore = 1;
	private int _playerKillScore = 1;

	public Mutant(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		removeConfig("minPlayers");
		
		addConfig(new ConfigModel("minPlayers", "3", "The minimum count of players required to start one instance of the event. Min for Mutant is 3 (2 players and one mutant)."));
		addInstanceTypeConfig(new ConfigModel("minPlayers", "3", "Count of players required to start this instance. If there's less players, then the instance tries to divide it's players to stronger instances (check out config <font color=LEVEL>joinStrongerInstIfNeeded</font>) and if it doesn't success (the config is set to false or all possible stronger instances are full), it will unregister the players from the event. Check out other configs related to this. Min for mutant is 3."));
		
		removeConfig("skillsForAllPlayers");
		removeConfig("bowWeaponId");
		removeConfig("arrowItemId");
		removeConfig("teamsCount");
		
		removeConfig("createParties");
		removeConfig("maxPartySize");
		removeConfig("teamsCount");
		removeConfig("firstBloodMessage");
		
		removeConfig("waweRespawn");
		
		removeConfig("countOfZombies");
		removeConfig("zombieTransformId");
		removeConfig("zombieInactivityTime");
		removeConfig("zombieMinLevel");
		removeConfig("zombieMinPvPs");
		
		removeConfig("zombieKillScore");
		removeConfig("survivorKillScore");
		removeConfig("zombiesInitialScore");
		
		removeConfig("bowWeaponId");
		removeConfig("arrowItemId");
		
		removeConfig("enableAmmoSystem");
		removeConfig("ammoAmmount");
		removeConfig("ammoRestoredPerTick");
		removeConfig("ammoRegTickInterval");
		
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system for zombies.", InputType.Boolean));
		
		addConfig(new ConfigModel("skillsForPlayers", "", "IDs of skills which will be given to every player, who is not a mutant in the event. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35001-1').", InputType.MultiAdd));
		addConfig(new ConfigModel("skillsForMutant", "35103-1", "IDs of skills which will be given to every mutant in the event. This skill should contain stats which make the mutant extra strong. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35002-1').", InputType.MultiAdd));
		addConfig(new ConfigModel("mutantWeaponId", "271", "The ID of the weapon which will be given to all mutants and will be the only weapon most the mutant will be able to use."));
		
		addConfig(new ConfigModel("countOfMutants", "1/10", "Defines the count of mutants in the the event. Format: #MUTANTS/#PLAYERS - <font color=LEVEL>eg. 1/10</font> means there's <font color=LEVEL>1</font> mutant when there are <font color=LEVEL>10</font> players in the event (20 players - 2 mutants, 100 players - 10 mutants, ...). There's always at least one mutant in the event."));
		addConfig(new ConfigModel("mutantTransformId", "303", "The ID of transformation used to morph players into zombies."));
		addConfig(new ConfigModel("mutantMinLevel", "0", "The minimum level required to become a zombie IN THE START OF THE EVENT."));
		addConfig(new ConfigModel("mutantMinPvPs", "0", "The minimum count of pvps required to become a zombie IN THE START OF THE EVENT."));
		
		addConfig(new ConfigModel("mutantKillScore", "1", "The count of score points given to a zombie when he kills a player."));
		addConfig(new ConfigModel("playerKillScore", "1", "The count of score points given to a survivor when he kills a zombie."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		_mutantWeaponId = getInt("mutantWeaponId");
		
		_mutantCount = getString("countOfMutants");
		_mutantTransformId = getInt("mutantTransformId");
		_mutantMinLevel = getInt("mutantMinLevel");
		_mutanteMinPvps = getInt("mutantMinPvPs");
		
		_mutantKillScore = getInt("mutantKillScore");
		_playerKillScore = getInt("playerKillScore");
		
		// load skills for survivors
		if(!getString("skillsForPlayers").equals(""))
		{
			String[] splits = getString("skillsForPlayers").split(",");
			_skillsForPlayers = new FastMap<Integer, Integer>();
			
			try
			{
				String id, level;
				for(int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForPlayers.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForPlayers' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		// load skills for survivors
		if(!getString("skillsForMutant").equals(""))
		{
			String[] splits = getString("skillsForMutant").split(",");
			_skillsForMutant = new FastMap<Integer, Integer>();
			
			try
			{
				String id, level;
				for(int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForMutant.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForMutant' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		_tick = 0;
	}
	
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		_teamsCount = 2;
		
		createTeams(_teamsCount, type.getInstance().getId());
		
		return _teamsCount;
	}
	
	@Override
	protected void createTeams(int count, int instanceId)
	{
		createNewTeam(instanceId, PLAYERS_TEAM_ID, "Players", "Players");
		createNewTeam(instanceId, MUTANT_TEAM_ID, "Mutants", "Mutants");
	}
	
	@Override
	protected void dividePlayersToTeams(int instanceId, FastList<PlayerEventInfo> players, int teamsCount)
	{
		for(PlayerEventInfo pi : players)
		{
			pi.onEventStart(this);
			_teams.get(instanceId).get(PLAYERS_TEAM_ID).addPlayer(pi, true);
		}
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
	
	protected void scheduleSelectMutants(final int instanceId, long delay, final boolean firstRun, final int forceAddNewMutantCount)
	{
		if(delay == 0)
		{
			CallBack.getInstance().getOut().executeTask(new Runnable()
			{
				@Override
				public void run()
				{
					List<PlayerEventInfo> newZombies = calculateMutants(instanceId, ((forceAddNewMutantCount > 0) ? forceAddNewMutantCount : -1), firstRun);
					
					if(newZombies != null)
					{
						for(PlayerEventInfo zombie : newZombies)
						{
							transformToMutant(zombie);
						}
					}
				}
			});
		}
		else
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					List<PlayerEventInfo> newZombies = calculateMutants(instanceId, ((forceAddNewMutantCount > 0) ? forceAddNewMutantCount : -1), firstRun);
					
					if(newZombies != null)
					{
						for(PlayerEventInfo zombie : newZombies)
						{
							transformToMutant(zombie);
						}
					}
				}
			}, delay);
		}
	}
	
	protected List<PlayerEventInfo> calculateMutants(int instanceId, int countToSpawn, boolean start)
	{
		int playersCount = getPlayers(instanceId).size();
		int survivorsCount = _teams.get(instanceId).get(PLAYERS_TEAM_ID).getPlayers().size();
		int mutantCount = _teams.get(instanceId).get(MUTANT_TEAM_ID).getPlayers().size();
		
		if(countToSpawn <= 0)
		{
			int mutants = Integer.parseInt(_mutantCount.split("/")[0]);
			int players = Integer.parseInt(_mutantCount.split("/")[1]);
			
			if(start)
			{
				countToSpawn = (int) Math.floor(((double)playersCount / (double)players) * (double)mutants);
				
				if(countToSpawn < 1)
					countToSpawn = 1;
			}
			else
			{
				countToSpawn = (countToSpawn = (int) Math.floor(((double)playersCount / (double)players) * (double)mutants)) - mutantCount;
			}
		}
		
		int i = 0;
		List<PlayerEventInfo> newMutants = new FastList<PlayerEventInfo>();

		if(survivorsCount >= 2)
		{
			if(countToSpawn >= survivorsCount)
				countToSpawn = survivorsCount-1;
			
			if(countToSpawn > 0)
			{
				for(PlayerEventInfo player : getPlayers(instanceId))
				{
					if(!start || (player.getLevel() >= _mutantMinLevel && player.getPvpKills() >= _mutanteMinPvps))
					{
						newMutants.add(player);
						
						i++;
						if(i >= countToSpawn)
							break;
					}
				}
			}
		}
		
		return newMutants;
	}
	
	/** gives/removes player skills and items */
	@Override
	protected void preparePlayer(PlayerEventInfo player, boolean start)
	{
		SkillData skill = null;
		
		// survivors
		if(player.getEventTeam().getTeamId() == PLAYERS_TEAM_ID)
		{
			if(start)
			{
				// give skills
				if(_skillsForPlayers != null)
				{
					for(Entry<Integer, Integer> e : _skillsForPlayers.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						
						if(skill.exists())
							player.addSkill(skill, false);
					}
					
					player.sendSkillList();
				}
			}
			else
			{
				if(_skillsForPlayers != null)
				{
					// remove skills
					for(Entry<Integer, Integer> e : _skillsForPlayers.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if(skill.exists())
							player.removeSkill(skill.getId());
					}
				}
			}
		}
		// zombies
		else if(player.getEventTeam().getTeamId() == MUTANT_TEAM_ID)
		{
			if(start)
			{
				if(_skillsForMutant != null)
				{
					for(Entry<Integer, Integer> e : _skillsForMutant.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						
						if(skill.exists())
							player.addSkill(skill, false);
					}
					
					player.sendSkillList();
				}
				
				if(_mutantWeaponId > 0)
				{
					// equip mutant weapon
					ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
					if (wpn != null)
						player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());

					wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
					if (wpn != null)
						player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());

					ItemData flagItem = player.addItem(_mutantWeaponId, 1, false);
					player.equipItem(flagItem);
				}
			}
			else
			{
				if(_skillsForMutant != null)
				{
					for(Entry<Integer, Integer> e : _skillsForMutant.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if(skill.exists())
							player.removeSkill(skill.getId());
					}
				}
				
				if(_mutantWeaponId > 0)
				{
					// remove mutant weapon
					ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
					if (wpn.exists())
					{
						ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
						player.destroyItemByItemId(_mutantWeaponId, 1);
						
						player.inventoryUpdate(unequiped);
					}
				}
			}
		}
	}
	
	private void transformToMutant(PlayerEventInfo player)
	{
		preparePlayer(player, false);
		
		player.getEventTeam().removePlayer(player);
		_teams.get(player.getInstanceId()).get(MUTANT_TEAM_ID).addPlayer(player, true);
		
		preparePlayer(player, true);
		
		player.transform(_mutantTransformId);
		
		getEventData(player.getInstanceId()).setKillMade();
		
		if(player.isDead())
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if(player != null && player.isOnline() && getMatch(player.getInstanceId()).isActive())
					{
						respawnPlayer(player, player.getInstanceId());
						player.sendMessage("You will be respawned in 10 seconds.");
					}
				}
			}, 10000);
		}
		
		player.setTitle(getTitle(player), true);
	}
	
	private void transformToPlayer(final PlayerEventInfo player, boolean endOfEvent)
	{
		if(endOfEvent)
		{
			player.untransform(true);
			
			player.setTitle(getTitle(player), true);
			player.broadcastTitleInfo();
		}
		else
		{
			try
			{
				if(player.getTeamId() == MUTANT_TEAM_ID)
				{
					preparePlayer(player, false);
					player.untransform(true);
					player.getEventTeam().removePlayer(player);
					
					_teams.get(player.getInstanceId()).get(PLAYERS_TEAM_ID).addPlayer(player, true);
					preparePlayer(player, true);
					
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
					
					if(player.isDead())
					{
						CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								if(player != null && player.isOnline())
								{
									respawnPlayer(player, player.getInstanceId());
								}
							}
						}, 10000);
					}
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("error while untransforming mutant:");
				clearEvent();
				e.printStackTrace();
			}
		}
	}
	
	protected void untransformAll(int instanceId)
	{
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			if(player.getTeamId() == MUTANT_TEAM_ID)
				transformToPlayer(player, true);
		}
	}
	
	protected void setAllPlayers(int instanceId)
	{
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			try
			{
				if(player.getTeamId() == MUTANT_TEAM_ID)
				{
					player.getEventTeam().removePlayer(player);
					_teams.get(player.getInstanceId()).get(PLAYERS_TEAM_ID).addPlayer(player, true);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				continue;
			}
		}
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 

		int minScore = getInt("killsForReward");
		rewardAllPlayersFromTeam(-1, minScore, 0, PLAYERS_TEAM_ID);
	}
	
	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(pi.isAfk())
			return "AFK";
		
		if(pi.getTeamId() == MUTANT_TEAM_ID)
			return ("~ MUTANT ~");
		
		return "Score: " + getPlayerData(pi).getScore();
	}
	
	@Override
	protected String getScorebar(int instance)
	{
		final int count = _teams.get(instance).size();
		
		TextBuilder tb = new TextBuilder();
		
		for(EventTeam team : _teams.get(instance).values())
		{
			tb.append(team.getTeamName() + ": " + team.getPlayers().size() + "  ");
		}
		
		if(count <= 3)
			tb.append(LanguageEngine.getMsg("event_scorebar_time", _matches.get(instance).getClock().getTime()));
		
		return tb.toString();
	}
	
	@Override
	protected void clockTick() 
	{
		// no action yet, TODO? 
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		if(player.getPlayersId() != target.getObjectId())
		{
			if(player.getTeamId() == MUTANT_TEAM_ID)
			{
				// update player's team's stats
				player.getEventTeam().raiseScore(_mutantKillScore);
				player.getEventTeam().raiseKills(_mutantKillScore);
				
				// update player's own stats
				getPlayerData(player).raiseScore(_mutantKillScore);
				getPlayerData(player).raiseKills(_mutantKillScore);
				getPlayerData(player).raiseSpree(1);
			}
			else if(player.getTeamId() == PLAYERS_TEAM_ID)
			{
				// update player's team's stats
				player.getEventTeam().raiseScore(_playerKillScore);
				player.getEventTeam().raiseKills(_playerKillScore);
				
				// update player's own stats
				getPlayerData(player).raiseScore(_playerKillScore);
				getPlayerData(player).raiseKills(_playerKillScore);
				getPlayerData(player).raiseSpree(1);
			}
			
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
		
		if(killer != null && killer.getEventInfo() != null)
		{
			if(player.getTeamId() == PLAYERS_TEAM_ID)
			{
				if(_waweRespawn)
					_waweScheduler.addPlayer(player);
				else
					scheduleRevive(player, getInt("resDelay") * 1000);
			}
			else
			{
				transformToPlayer(player, false);
				
				PlayerEventInfo killerInfo = killer.getEventInfo();
				transformToMutant(killerInfo);
			}
		}
	}
	
	@Override
	public boolean onAttack(CharacterData cha, CharacterData target)
	{
		return true;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if(player.getTeamId() == MUTANT_TEAM_ID)
		{
			if(item.getItemId() == _mutantWeaponId && item.isEquipped())
				return false;
			else if(_mutantWeaponId > 0 && item.isWeapon())
				return false;
		}
		
		if (notAllovedItems != null && Arrays.binarySearch(notAllovedItems, item.getItemId()) >= 0)
		{
			player.sendMessage(LanguageEngine.getMsg("event_itemNotAllowed"));
			return false;
		}

		if (item.isPotion() && !getBoolean("allowPotions"))
			return false;

		if (item.isScroll())
			return false;
		
		if(item.isPetCollar() && !_allowPets)
		{
			player.sendMessage(LanguageEngine.getMsg("event_petsNotAllowed"));
			return false;
		}
		
		return true;
	}
	
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		//TODO any special actions on damage?
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		if(item.getItemId() == _mutantWeaponId || player.getTeamId() == MUTANT_TEAM_ID)
			return false;
		
		return super.canDestroyItem(player, item);
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		return false;
	}
	
	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		super.onDisconnect(player);

		// check if there are enought mutants and if not, transform some
		scheduleSelectMutants(player.getInstanceId(), 0, false, 0);
	}
	
	
	@SuppressWarnings("unused")
	@Override
	protected boolean checkIfEventCanContinue(int instanceId, PlayerEventInfo disconnectedPlayer)
	{
		int currentPlayers = 0;
		int currentMutants = 0;
		for(EventTeam team : _teams.get(instanceId).values())
		{
			if(team.getTeamId() == PLAYERS_TEAM_ID)
			{
				for(PlayerEventInfo pi : team.getPlayers())
				{
					currentPlayers ++;
					continue;
				}
			}
			
			if(team.getTeamId() == MUTANT_TEAM_ID)
			{
				for(PlayerEventInfo pi : team.getPlayers())
				{
					currentMutants ++;
					continue;
				}
			}
		}
		
		if(currentMutants == 0)
		{
			if(currentPlayers >= 3)
				return true;
			else return false; // at least 3 players are required (one of them will become mutant)
		}
		else if(currentMutants == 1)
		{
			if(currentPlayers >= 2)
				return true;
			else return false;
		}
		else
		{
			if(currentPlayers + currentMutants >= 3)
			{
				int mutants = Integer.parseInt(_mutantCount.split("/")[0]);
				int players = Integer.parseInt(_mutantCount.split("/")[1]);
				
				int countToHaveMutants = (int) Math.floor(((double)currentPlayers / (double)players) * (double)mutants);
				if(countToHaveMutants < 1)
					countToHaveMutants = 1;
				
				int toUntransform = 0;
				if(currentMutants > countToHaveMutants)
				{
					toUntransform = currentMutants - countToHaveMutants;
				}
				
				if(toUntransform > 0)
				{
					for(PlayerEventInfo mutant : _teams.get(instanceId).get(MUTANT_TEAM_ID).getPlayers())
					{
						if(toUntransform <= 0)
							break;
						
						transformToPlayer(mutant, false);
						
						toUntransform--;
					}
				}
				
				return true;
			}
			else return false;
		}
	}
	
	@Override
	public void teleportPlayers(int instanceId, SpawnType type, boolean ffa)
	{
		/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: ========================================");
		/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: STARTING TO TELEPORT PLAYERS (ffa = " + ffa + ")");
		
		EventSpawn spawn;
		int radius;
		Loc loc;
		
		final boolean removeBuffs = getBoolean("removeBuffsOnStart");
		
		/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: removeBuffs = " + removeBuffs);
		
		int i = 0;
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			spawn = getSpawn(type, -1);
			
			if(spawn == null)
			{
				/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: ! Missing spawn for team " + (_teams.get(instanceId).size() == 1 ? -1 : player.getTeamId()) + ", map " + _manager.getMap().getMapName() + ", event " + getEventType().getAltTitle() + " !!");
				NexusLoader.debug("Missing spawn for team " + (_teams.get(instanceId).size() == 1 ? -1 : player.getTeamId()) + ", map " + _manager.getMap().getMapName() + ", event " + getEventType().getAltTitle() + " !!", Level.SEVERE);
				// and let's throw an npe
			}
			
			radius = spawn.getRadius();
			if(radius == -1)
				radius = 50;
			
			loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(radius);
			
			player.teleport(loc, 0, false, instanceId);
			
			/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: /// player " + player.getPlayersName() + " teleported to " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " (radius = " + radius + "), SPAWN ID " + spawn.getSpawnId() + ", SPAWN TEAM " + spawn.getSpawnTeam());
			
			if(removeBuffs)
				player.removeBuffs();
			
			i ++;
		}
		
		/**/ if(NexusLoader.detailedDebug) print("AbstractMainEvent: " + i + " PLAYERS TELEPORTED");
		
		clearMapHistory(-1, type);
	}
	
	public class MutantEventPlayerData extends ZombiesEventPlayerData
	{
		public MutantEventPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event);
		}
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new MutantEventPlayerData(player, this);
	}

	@Override
	public MutantEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (MutantEventPlayerData) player.getEventData();
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
						
						preparePlayers(match.getInstance().getId(), false);
						untransformAll(match.getInstance().getId());
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
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: respawning player " + pi.getPlayersName() + ", instance " + instance);
		
		EventSpawn spawn = null;
		
		if(pi.getTeamId() == PLAYERS_TEAM_ID)
			spawn = getSpawn(SpawnType.Regular, -1);
		else if(pi.getTeamId() == MUTANT_TEAM_ID)
			spawn = getSpawn(SpawnType.Zombie, -1);
		
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
			debug("Error on respawnPlayer - no spawn type REGULAR/ZOMBIE, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
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
				_htmlDescription = "No information about this event yet.";
			}
		}
		return _htmlDescription;
	}
	
	public boolean allowsRejoinOnDisconnect()
	{
		return false;
	}
	
	@Override
	protected TvTEventData createEventData(int instanceId)
	{
		return new MutantEventData(instanceId);
	}
	
	@Override
	protected MutantEventInstance createEventInstance(InstanceData instance)
	{
		return new MutantEventInstance(instance);
	}
	
	@Override
	protected MutantEventData getEventData(int instance)
	{
		return (MutantEventData) _matches.get(instance)._data;
	}
}
