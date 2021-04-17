package cz.nxs.events.engine.main.events;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
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
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 */
public class Zombies extends HuntingGrounds
{
	private static final int	SURVIVOR_TEAM_ID	= 1;
	private static final int	ZOMBIE_TEAM_ID		= 2;
	
	protected class ZombiesEventData extends HGEventData
	{
		protected InactivityTimer _inactivityTimer = null;
		
		public ZombiesEventData(int instance)
		{
			super(instance);
		}
		
		private synchronized void startTimer()
		{
			if (_inactivityTimer == null)
				_inactivityTimer = new InactivityTimer(_instanceId);
			_inactivityTimer.schedule();
		}
		
		public void setKillMade()
		{
			if (_zombieInactivityTime > 0)
				startTimer();
		}
	}
	
	private class InactivityTimer implements Runnable
	{
		int					_instanceId;
		ScheduledFuture<?>	_future	= null;
		
		InactivityTimer(int instanceId)
		{
			_instanceId = instanceId;
		}
		
		@Override
		public void run()
		{
			zombiesInactive(_instanceId);
		}
		
		public void schedule()
		{
			if (_future != null)
				abort();
			_future = CallBack.getInstance().getOut().scheduleGeneral(this, _zombieInactivityTime * 1000);
		}
		
		private void abort()
		{
			if (_future != null)
			{
				_future.cancel(false);
				_future = null;
			}
		}
	}
	
	protected class ZombiesEventInstance extends HGEventInstance
	{
		protected ZombiesEventInstance(InstanceData instance)
		{
			super(instance);
		}
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				/**/ if (NexusLoader.detailedDebug)
					print("Event: running task of state " + _state.toString() + "...");
				switch (_state)
				{
					case START:
					{
						if (checkPlayers(_instance.getId()))
						{
							teleportPlayers(_instance.getId(), SpawnType.Regular, false);
							setupTitles(_instance.getId());
							// action done for some hardcoded maps
							removeStaticDoors(_instance.getId());
							enableMarkers(_instance.getId(), true);
							preparePlayers(_instance.getId(), true);
							scheduleSelectZombies(_instance.getId(), 10000, true, 0);
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
						setAllZombies(_instance.getId());
						if (!instanceEnded() && _canBeAborted)
						{
							if (_canRewardIfAborted)
							{
								int minScore = getInt("killsForReward");
								rewardAllPlayersFromTeam(_instance.getId(), minScore, 0, ZOMBIE_TEAM_ID);
							}
							clearEvent(_instance.getId());
						}
						break;
					}
				}
				/**/ if (NexusLoader.detailedDebug)
					print("Event: ... finished running task. next state " + _state.toString());
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}
	
	private FastMap<Integer, Integer>	_skillsForSurvivors;
	private FastMap<Integer, Integer>	_skillsForZombies;
	private String						_zombiesCount;
	private int							_zombieTransformId;
	protected int						_zombieInactivityTime;
	private int							_zombieMinLevel;
	private int							_zombieMinPvps;
	private int							_zombieKillScore		= 1;
	private int							_survivorKillScore		= 1;
	private int							_zombiesInitialScore	= 0;
	
	public Zombies(EventType type, MainEventManager manager)
	{
		super(type, manager);
		setRewardTypes(new RewardPosition[]
		{
			RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree
		});
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		removeConfig("skillsForAllPlayers");
		removeConfig("bowWeaponId");
		removeConfig("arrowItemId");
		removeConfig("teamsCount");
		removeConfig("createParties");
		removeConfig("maxPartySize");
		removeConfig("teamsCount");
		removeConfig("firstBloodMessage");
		removeConfig("waweRespawn");
		addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system for zombies.", InputType.Boolean));
		addConfig(new ConfigModel("skillsForPlayers", "35101-1", "IDs of skills which will be given to every survivor (non zombie player) on the event. The purpose of this is to make all survivors equally strong. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35001-1').", InputType.MultiAdd));
		addConfig(new ConfigModel("skillsForZombies", "35102-1", "IDs of skills which will be given to every zombie on the event. The purpose of this is to make all zombies equally strong. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35002-1').", InputType.MultiAdd));
		addConfig(new ConfigModel("bowWeaponId", "271", "The ID of the bow item which will be given to the survivors (non zombies) and will be the only weapon most players will use during the event. This weapon kills zombies with just one hit."));
		addConfig(new ConfigModel("arrowItemId", "17", "The ID of the arrows which will be given to the player in the event."));
		addConfig(new ConfigModel("enableAmmoSystem", "true", "Enable/disable the ammo system based on player's mana. Player's max MP is defaultly modified by a custom passive skill and everytime a player shots and arrow, his MP decreases by a value which is calculated from the ammount of ammo. There is also a MP regeneration system - see the configs below.", InputType.Boolean));
		addConfig(new ConfigModel("ammoAmmount", "10", "Works if ammo system is enabled. Specifies the max ammount of ammo every player can have."));
		addConfig(new ConfigModel("ammoRestoredPerTick", "1", "Works if ammo system is enabled. Defines the ammount of ammo given to every player each <font color=LEVEL>'ammoRegTickInterval'</font> (configurable) seconds."));
		addConfig(new ConfigModel("ammoRegTickInterval", "10", "Works if ammo system is enabled. Defines the interval of restoring player's ammo. The value is in seconds (eg. value 10 will give ammo every 10 seconds to every player - the ammount of restored ammo is configurable (config <font color=LEVEL>ammoRestoredPerTick</font>)."));
		addConfig(new ConfigModel("countOfZombies", "1/10", "Defines the count of players transformed to zombies in the start of the event. Format: #ZOMBIES/#PLAYERS - <font color=LEVEL>eg. 1/10</font> means there's <font color=LEVEL>1</font> zombie when there are <font color=LEVEL>10</font> players in the event (20 players - 2 zombies, 100 players - 10 zombies, ...). There's always at least one zombie in the event."));
		addConfig(new ConfigModel("zombieTransformId", "303", "The ID of transformation used to morph players into zombies."));
		addConfig(new ConfigModel("zombieInactivityTime", "300", "In seconds. If no player is killed (by zombie) during this time, one random player will be transformed into a zombie and respawned on Zombie respawn (away from other players). Write 0 to disable this feature."));
		addConfig(new ConfigModel("zombieMinLevel", "0", "The minimum level required to become a zombie IN THE START OF THE EVENT."));
		addConfig(new ConfigModel("zombieMinPvPs", "0", "The minimum count of pvps required to become a zombie IN THE START OF THE EVENT."));
		addConfig(new ConfigModel("zombieKillScore", "1", "The count of score points given to a zombie when he kills a player."));
		addConfig(new ConfigModel("survivorKillScore", "1", "The count of score points given to a survivor when he kills a zombie."));
		addConfig(new ConfigModel("zombiesInitialScore", "1", "The initial score given to every zombie who gets automatically transformed in the beginning of the event."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		_bowItemId = getInt("bowWeaponId");
		_arrowItemId = getInt("arrowItemId");
		_ammoSystem = getBoolean("enableAmmoSystem");
		_ammoAmmount = getInt("ammoAmmount");
		_ammoRegPerTick = getInt("ammoRestoredPerTick");
		_tickLength = getInt("ammoRegTickInterval");
		_zombiesCount = getString("countOfZombies");
		_zombieTransformId = getInt("zombieTransformId");
		_zombieInactivityTime = getInt("zombieInactivityTime");
		_zombieMinLevel = getInt("zombieMinLevel");
		_zombieMinPvps = getInt("zombieMinPvPs");
		_zombieKillScore = getInt("zombieKillScore");
		_survivorKillScore = getInt("survivorKillScore");
		_zombiesInitialScore = getInt("zombiesInitialScore");
		// load skills for survivors
		if (!getString("skillsForPlayers").equals(""))
		{
			String[] splits = getString("skillsForPlayers").split(",");
			_skillsForSurvivors = new FastMap<Integer, Integer>();
			try
			{
				String id, level;
				for (int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForSurvivors.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForPlayers' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		// load skills for survivors
		if (!getString("skillsForZombies").equals(""))
		{
			String[] splits = getString("skillsForZombies").split(",");
			_skillsForZombies = new FastMap<Integer, Integer>();
			try
			{
				String id, level;
				for (int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForZombies.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForZombies' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
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
		createNewTeam(instanceId, SURVIVOR_TEAM_ID, "Survivors", "Survivors");
		createNewTeam(instanceId, ZOMBIE_TEAM_ID, "Zombies", "Zombies");
	}
	
	@Override
	protected void dividePlayersToTeams(int instanceId, FastList<PlayerEventInfo> players, int teamsCount)
	{
		for (PlayerEventInfo pi : players)
		{
			pi.onEventStart(this);
			_teams.get(instanceId).get(SURVIVOR_TEAM_ID).addPlayer(pi, true);
		}
	}
	
	@Override
	public void runEvent()
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: started runEvent()");
		if (!dividePlayers())
		{
			clearEvent();
			return;
		}
		TvTEventInstance match;
		_matches = new FastMap<Integer, TvTEventInstance>();
		for (InstanceData instance : _instances)
		{
			/**/ if (NexusLoader.detailedDebug)
				print("Event: creating eventinstance for instance " + instance.getId());
			match = createEventInstance(instance);
			_matches.put(instance.getId(), match);
			_runningInstances++;
			match.scheduleNextTask(0);
			/**/ if (NexusLoader.detailedDebug)
				print("Event: event instance started");
		}
		/**/ if (NexusLoader.detailedDebug)
			print("Event: finished runEvent()");
	}
	
	private void scheduleSelectZombies(final int instanceId, long delay, final boolean firstRun, final int forceAddNewZombieCount)
	{
		if (delay == 0)
		{
			CallBack.getInstance().getOut().executeTask(new Runnable()
			{
				@Override
				public void run()
				{
					List<PlayerEventInfo> newZombies = calculateZombies(instanceId, ((forceAddNewZombieCount > 0) ? forceAddNewZombieCount : -1), firstRun);
					if (newZombies != null)
					{
						for (PlayerEventInfo zombie : newZombies)
						{
							transformToZombie(zombie);
							try
							{
								if (firstRun && _zombiesInitialScore > 0)
								{
									// update player's team's stats
									zombie.getEventTeam().raiseScore(_zombiesInitialScore);
									// update player's own stats
									getPlayerData(zombie).raiseScore(_zombiesInitialScore);
								}
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
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
					List<PlayerEventInfo> newZombies = calculateZombies(instanceId, ((forceAddNewZombieCount > 0) ? forceAddNewZombieCount : -1), firstRun);
					if (newZombies != null)
					{
						for (PlayerEventInfo zombie : newZombies)
						{
							transformToZombie(zombie);
							try
							{
								if (firstRun && _zombiesInitialScore > 0)
								{
									// update player's team's stats
									zombie.getEventTeam().raiseScore(_zombiesInitialScore);
									// update player's own stats
									getPlayerData(zombie).raiseScore(_zombiesInitialScore);
								}
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}, delay);
		}
	}
	
	private List<PlayerEventInfo> calculateZombies(int instanceId, int countToSpawn, boolean start)
	{
		int playersCount = getPlayers(instanceId).size();
		int survivorsCount = _teams.get(instanceId).get(SURVIVOR_TEAM_ID).getPlayers().size();
		int zombiesCount = _teams.get(instanceId).get(ZOMBIE_TEAM_ID).getPlayers().size();
		if (countToSpawn <= 0)
		{
			int zombies = Integer.parseInt(_zombiesCount.split("/")[0]);
			int players = Integer.parseInt(_zombiesCount.split("/")[1]);
			if (start)
			{
				countToSpawn = (int) Math.floor(((double) playersCount / (double) players) * (double) zombies);
				if (countToSpawn < 1)
					countToSpawn = 1;
			}
			else
			{
				countToSpawn = (countToSpawn = (int) Math.floor(((double) playersCount / (double) players) * (double) zombies)) - zombiesCount;
			}
		}
		int i = 0;
		List<PlayerEventInfo> newZombies = new FastList<PlayerEventInfo>();
		if (countToSpawn >= survivorsCount)
			countToSpawn = survivorsCount - 1;
		if (countToSpawn > 0)
		{
			for (PlayerEventInfo player : getPlayers(instanceId))
			{
				if (!start || (player.getLevel() >= _zombieMinLevel && player.getPvpKills() >= _zombieMinPvps))
				{
					newZombies.add(player);
					i++;
					if (i >= countToSpawn)
						break;
				}
			}
		}
		return newZombies;
	}
	
	protected void preparePlayers(int instanceId, boolean start)
	{
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			player.removeCubics();
			preparePlayer(player, start);
		}
	}
	
	/** gives/removes player skills and items */
	protected void preparePlayer(PlayerEventInfo player, boolean start)
	{
		SkillData skill = null;
		// survivors
		if (player.getEventTeam().getTeamId() == SURVIVOR_TEAM_ID)
		{
			if (start)
			{
				// give skills
				if (_skillsForSurvivors != null)
				{
					for (Entry<Integer, Integer> e : _skillsForSurvivors.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if (skill.exists())
							player.addSkill(skill, false);
					}
					player.sendSkillList();
				}
				// equip bow
				ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
				if (wpn != null)
					player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());
				wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
				if (wpn != null)
					player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());
				ItemData flagItem = player.addItem(_bowItemId, 1, false);
				player.equipItem(flagItem);
				player.addItem(_arrowItemId, 400, false);
			}
			else
			{
				if (_skillsForSurvivors != null)
				{
					// remove skills
					for (Entry<Integer, Integer> e : _skillsForSurvivors.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if (skill.exists())
							player.removeSkill(skill.getId());
					}
				}
				// remove bow
				ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
				if (wpn.exists())
				{
					ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
					player.destroyItemByItemId(_bowItemId, 1);
					player.inventoryUpdate(unequiped);
				}
			}
		}
		// zombies
		else if (player.getEventTeam().getTeamId() == ZOMBIE_TEAM_ID)
		{
			if (start)
			{
				if (_skillsForZombies != null)
				{
					for (Entry<Integer, Integer> e : _skillsForZombies.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if (skill.exists())
							player.addSkill(skill, false);
					}
					player.sendSkillList();
				}
				// TODO equip dagger?
			}
			else
			{
				if (_skillsForZombies != null)
				{
					for (Entry<Integer, Integer> e : _skillsForZombies.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if (skill.exists())
							player.removeSkill(skill.getId());
					}
				}
				// TODO remove dagger?
			}
		}
	}
	
	protected void zombiesInactive(int instanceId)
	{
		scheduleSelectZombies(instanceId, 0, false, 1);
	}
	
	private void transformToZombie(PlayerEventInfo player)
	{
		if (!player.isDead())
			player.doDie();
		preparePlayer(player, false);
		player.getEventTeam().removePlayer(player);
		_teams.get(player.getInstanceId()).get(ZOMBIE_TEAM_ID).addPlayer(player, true);
		preparePlayer(player, true);
		player.transform(_zombieTransformId);
		getEventData(player.getInstanceId()).setKillMade();
		if (checkIfAnyPlayersLeft(player.getInstanceId()))
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if (player != null && player.isOnline() && getMatch(player.getInstanceId()).isActive())
					{
						respawnPlayer(player, player.getInstanceId());
						player.sendMessage("You will be respawned in 10 seconds.");
					}
				}
			}, 10000);
		}
		player.setTitle(getTitle(player), true);
	}
	
	private void transformToPlayer(PlayerEventInfo player, boolean endOfEvent)
	{
		if (endOfEvent)
		{
			player.untransform(true);
		}
		else
		{
			try
			{
				if (player.getTeamId() == ZOMBIE_TEAM_ID)
				{
					preparePlayer(player, false);
					player.untransform(true);
					player.getEventTeam().removePlayer(player);
					_teams.get(player.getInstanceId()).get(SURVIVOR_TEAM_ID).addPlayer(player, true);
					preparePlayer(player, true);
					if (player.isDead())
					{
						CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								if (player != null && player.isOnline())
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
				NexusLoader.debug("error while untransforming zombie:");
				clearEvent();
				e.printStackTrace();
			}
		}
	}
	
	protected void untransformAll(int instanceId)
	{
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			if (player.getTeamId() == ZOMBIE_TEAM_ID)
				transformToPlayer(player, true);
		}
	}
	
	protected void setAllZombies(int instanceId)
	{
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			try
			{
				if (player.getTeamId() == SURVIVOR_TEAM_ID)
				{
					player.getEventTeam().removePlayer(player);
					_teams.get(player.getInstanceId()).get(ZOMBIE_TEAM_ID).addPlayer(player, true);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private boolean checkIfAnyPlayersLeft(int instanceId)
	{
		synchronized (_teams)
		{
			if (_teams.get(instanceId).get(SURVIVOR_TEAM_ID).getPlayers().size() <= 0)
			{
				announce(instanceId, "All survivors have died!");
				endInstance(instanceId, true, true, false);
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: onEventEnd()");
		int minScore = getInt("killsForReward");
		rewardAllPlayersFromTeam(-1, minScore, 0, ZOMBIE_TEAM_ID);
	}
	
	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if (pi.isAfk())
			return "AFK";
		if (pi.getTeamId() == ZOMBIE_TEAM_ID)
			return ("~ ZOMBIE ~");
		return "Score: " + getPlayerData(pi).getScore();
	}
	
	@Override
	protected String getScorebar(int instance)
	{
		final int count = _teams.get(instance).size();
		TextBuilder tb = new TextBuilder();
		for (EventTeam team : _teams.get(instance).values())
		{
			tb.append(team.getTeamName() + ": " + team.getPlayers().size() + "  ");
		}
		if (count <= 3)
			tb.append(LanguageEngine.getMsg("event_scorebar_time", _matches.get(instance).getClock().getTime()));
		return tb.toString();
	}
	
	@Override
	protected void clockTick()
	{
		_tick++;
		if (_tick % _tickLength != 0)
			return;
		if (_ammoSystem)
		{
			int toAdd, currentMp, mpToRegenerate;
			int oneAmmoMp = 0;
			for (TvTEventInstance match : _matches.values())
			{
				for (PlayerEventInfo player : getPlayers(match.getInstance().getId()))
				{
					if (player.getTeamId() == SURVIVOR_TEAM_ID)
					{
						try
						{
							oneAmmoMp = player.getMaxMp() / _ammoAmmount;
							mpToRegenerate = _ammoRegPerTick * oneAmmoMp;
							currentMp = (int) player.getCurrentMp();
							if (currentMp < player.getMaxMp())
							{
								toAdd = mpToRegenerate;
								if (currentMp + mpToRegenerate > player.getMaxMp())
									toAdd = player.getMaxMp() - currentMp;
								player.setCurrentMp(currentMp + toAdd);
							}
						}
						catch (NullPointerException e)
						{
							continue;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if (target.getEventInfo() == null)
			return;
		if (player.getPlayersId() != target.getObjectId())
		{
			if (player.getTeamId() == ZOMBIE_TEAM_ID)
			{
				// update player's team's stats
				player.getEventTeam().raiseScore(_zombieKillScore);
				player.getEventTeam().raiseKills(_zombieKillScore);
				// update player's own stats
				getPlayerData(player).raiseScore(_zombieKillScore);
				getPlayerData(player).raiseKills(_zombieKillScore);
				getPlayerData(player).raiseSpree(1);
			}
			else if (player.getTeamId() == SURVIVOR_TEAM_ID)
			{
				// update player's team's stats
				player.getEventTeam().raiseScore(_survivorKillScore);
				player.getEventTeam().raiseKills(_survivorKillScore);
				// update player's own stats
				getPlayerData(player).raiseScore(_survivorKillScore);
				getPlayerData(player).raiseKills(_survivorKillScore);
				getPlayerData(player).raiseSpree(1);
			}
			giveKillingSpreeReward(getPlayerData(player));
			// update title
			if (player.isTitleUpdated())
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
		/**/ if (NexusLoader.detailedDebug)
			print("/// Event: onDie - player " + player.getPlayersName() + " (instance " + player.getInstanceId() + "), killer " + killer.getName());
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		setDeathsStats(player, getPlayerData(player).getDeaths());
		if (player.getTeamId() == ZOMBIE_TEAM_ID)
		{
			if (_waweRespawn)
				_waweScheduler.addPlayer(player);
			else
				scheduleRevive(player, getInt("resDelay") * 1000);
		}
		else
		{
			transformToZombie(player);
		}
	}
	
	@Override
	public boolean onAttack(CharacterData cha, CharacterData target)
	{
		// TODO make this more awesome for zombies event
		if (_ammoSystem && cha.isPlayer() && target.isPlayer())
		{
			final PlayerEventInfo player = cha.getEventInfo();
			if (player.getTeamId() == SURVIVOR_TEAM_ID)
			{
				final int oneShotMp = player.getMaxMp() / _ammoAmmount;
				if (player.getCurrentMp() >= oneShotMp)
				{
					player.setCurrentMp((int) (player.getCurrentMp() - oneShotMp));
				}
				else
				{
					player.sendMessage("Not enought MP.");
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if (player.getTeamId() == ZOMBIE_TEAM_ID)
			return false;
		else if (item.getItemId() == _bowItemId && item.isEquipped())
			return false;
		else if (item.isWeapon())
			return false;
		return super.canUseItem(player, item);
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		if (getEventType() == EventType.Zombies)
			return false;
		else
			return super.canUseSkill(player, skill);
	}
	
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		try
		{
			if (cha.isPlayer() && target.isPlayer())
			{
				PlayerEventInfo targetPlayer = target.getEventInfo();
				PlayerEventInfo player = cha.getEventInfo();
				if (player.getTeamId() != targetPlayer.getTeamId())
				{
					targetPlayer.abortCasting();
					targetPlayer.doDie(cha);
				}
			}
		}
		catch (NullPointerException e)
		{}
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		if (item.getItemId() == _bowItemId || player.getTeamId() == ZOMBIE_TEAM_ID)
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
		// check if there are enought zombies and if not, transform some
		scheduleSelectZombies(player.getInstanceId(), 0, false, 0);
	}
	
	@SuppressWarnings("unused")
	@Override
	protected boolean checkIfEventCanContinue(int instanceId, PlayerEventInfo disconnectedPlayer)
	{
		int survivors = 0;
		int zombies = 0;
		for (EventTeam team : _teams.get(instanceId).values())
		{
			if (team.getTeamId() == SURVIVOR_TEAM_ID)
			{
				for (PlayerEventInfo pi : team.getPlayers())
				{
					survivors++;
					continue;
				}
			}
			if (team.getTeamId() == ZOMBIE_TEAM_ID)
			{
				for (PlayerEventInfo pi : team.getPlayers())
				{
					zombies++;
					continue;
				}
			}
		}
		if (zombies == 0)
		{
			if (survivors >= 2)
				return true;
		}
		else if (zombies >= 1)
		{
			if (survivors >= 1)
				return true;
		}
		return false;
	}
	
	@Override
	public void teleportPlayers(int instanceId, SpawnType type, boolean ffa)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("AbstractMainEvent: ========================================");
		/**/ if (NexusLoader.detailedDebug)
			print("AbstractMainEvent: STARTING TO TELEPORT PLAYERS (ffa = " + ffa + ")");
		EventSpawn spawn;
		int radius;
		Loc loc;
		final boolean removeBuffs = false;
		/**/ if (NexusLoader.detailedDebug)
			print("AbstractMainEvent: removeBuffs = " + removeBuffs);
		int i = 0;
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			spawn = getSpawn(type, -1);
			if (spawn == null)
			{
				/**/ if (NexusLoader.detailedDebug)
					print("AbstractMainEvent: ! Missing spawn for team " + (_teams.get(instanceId).size() == 1 ? -1 : player.getTeamId()) + ", map " + _manager.getMap().getMapName() + ", event " + getEventType().getAltTitle() + " !!");
				NexusLoader.debug("Missing spawn for team " + (_teams.get(instanceId).size() == 1 ? -1 : player.getTeamId()) + ", map " + _manager.getMap().getMapName() + ", event " + getEventType().getAltTitle() + " !!", Level.SEVERE);
				// and let's throw an npe
			}
			radius = spawn.getRadius();
			if (radius == -1)
				radius = 50;
			loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(radius);
			player.teleport(loc, 0, false, instanceId);
			/**/ if (NexusLoader.detailedDebug)
				print("AbstractMainEvent: /// player " + player.getPlayersName() + " teleported to " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ() + " (radius = " + radius + "), SPAWN ID " + spawn.getSpawnId() + ", SPAWN TEAM " + spawn.getSpawnTeam());
			if (removeBuffs)
				player.removeBuffs();
			i++;
		}
		/**/ if (NexusLoader.detailedDebug)
			print("AbstractMainEvent: " + i + " PLAYERS TELEPORTED");
		clearMapHistory(-1, type);
	}
	
	public class ZombiesEventPlayerData extends PvPEventPlayerData
	{
		public ZombiesEventPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
		}
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new ZombiesEventPlayerData(player, this);
	}
	
	@Override
	public ZombiesEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (ZombiesEventPlayerData) player.getEventData();
	}
	
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: called CLEAREVENT for instance " + instanceId);
		try
		{
			if (_matches != null)
			{
				for (TvTEventInstance match : _matches.values())
				{
					if (instanceId == 0 || instanceId == match.getInstance().getId())
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
			if (!player.isOnline())
				continue;
			if (player.isParalyzed())
				player.setIsParalyzed(false);
			if (player.isImmobilized())
				player.unroot();
			if (!player.isGM())
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
		/**/ if (NexusLoader.detailedDebug)
			print("/// Event: respawning player " + pi.getPlayersName() + ", instance " + instance);
		EventSpawn spawn = null;
		if (pi.getTeamId() == SURVIVOR_TEAM_ID)
			spawn = getSpawn(SpawnType.Regular, -1);
		else if (pi.getTeamId() == ZOMBIE_TEAM_ID)
			spawn = getSpawn(SpawnType.Zombie, -1);
		if (spawn != null)
		{
			Loc loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(spawn.getRadius());
			pi.teleport(loc, 0, true, instance);
			pi.sendMessage(LanguageEngine.getMsg("event_respawned"));
		}
		else
		{
			// abortEvent("An error occured with Event Map.");
			debug("Error on respawnPlayer - no spawn type REGULAR/ZOMBIE, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}
	
	@Override
	public String getHtmlDescription()
	{
		if (_htmlDescription == null)
		{
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if (desc != null)
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
		return new ZombiesEventData(instanceId);
	}
	
	@Override
	protected ZombiesEventInstance createEventInstance(InstanceData instance)
	{
		return new ZombiesEventInstance(instance);
	}
	
	@Override
	protected ZombiesEventData getEventData(int instance)
	{
		return (ZombiesEventData) _matches.get(instance)._data;
	}
}
