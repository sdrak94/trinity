package cz.nxs.events.engine.mini;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import cz.nxs.events.Configurable;
import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.Event;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.DelaysFeature;
import cz.nxs.events.engine.mini.features.StrenghtChecksFeature;
import cz.nxs.events.engine.mini.tournament.TournamentManager;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - a mother class of all mini event managers (managers are creating and managing mini event games)
 */
public abstract class MiniEventManager extends Event implements Runnable, Configurable
{
	protected static Logger _log = Logger.getLogger(MiniEventManager.class.getName());
	
	protected List<RegistrationData> _parties;
	protected int _lastGameId;
	
	protected List<MiniEventGame> _games;
	protected Map<Integer, Long> _loggedPlayers;
	
	protected boolean _locked;
	protected boolean _canRun;
	protected boolean _tournamentActive;
	
	protected EventMode _mode;
	
	// key, description
	private FastMap<String, ConfigModel> _configs;
	private FastMap<String, ConfigModel> _mapConfigs;
	
	private FastList<String> _configCategories;
	
	protected String _htmlDescription = null;
	
	// ------------------------------------------------------
	
	@SuppressWarnings("unused")
	private Comparator<RegistrationData> _compareByLevel = new Comparator<RegistrationData>() {
		
		@Override
		public int compare(RegistrationData p1,
				RegistrationData p2)
		{
			int level1 = p1.getAverageLevel();
			int level2 = p2.getAverageLevel();
			
			return level1 == level2 ? 0 : level1 < level2 ? -1 : 1;
		}
	};
	
	public MiniEventManager(EventType type)
	{
		super(type);
		
		_tournamentActive = false;
		
		_parties = new FastList<RegistrationData>();
		_games = new FastList<MiniEventGame>();
		_loggedPlayers = new FastMap<Integer, Long>();
		
		_mode = new EventMode(getEventType());
		
		_configs = new FastMap<String, ConfigModel>();
		_mapConfigs = new FastMap<String, ConfigModel>();
		_configCategories = new FastList<String>();
		
		loadConfigs();
		
		_lastGameId = 0;
		_canRun = false;
	}
	
	@Override
	public void loadConfigs()
	{
		addConfig(new ConfigModel("DelayToWaitSinceLastMatchMs", "600000", "The delay the player has to wait to join this event again, after the his last event ended. In miliseconds."));
		addConfig(new ConfigModel("TimeLimitMs", "600000", "The delay after the match will be automatically aborted. In ms (miliseconds)."));
		addConfig(new ConfigModel("MaxLevelDifference", "5", "Maximum level difference between opponents in the event."));
		addConfig(new ConfigModel("MinLevelToJoin", "0", "Minimum level for players participating the event (playerLevel >= value)."));
		addConfig(new ConfigModel("MaxLevelToJoin", "100", "Maximum level for players participating the event (playerLevel <= value)."));
		
		addConfig(new ConfigModel("notAllowedSkills", "", "Put here skills that won't be aviable for use in this event <font color=7f7f7f>(write one skill's ID and click Add, to remove the skill, simply click on it's ID in the list)</font>", InputType.MultiAdd));
		addConfig(new ConfigModel("notAllowedItems", "", "Put here items that won't be aviable for use in this event <font color=7f7f7f>(write one skill's ID and click Add; to remove the skill, simply click on it's ID in the list)</font>", InputType.MultiAdd));
		
		addConfig(new ConfigModel("setOffensiveSkills", "", "Skills written here will be usable only on player's opponents/enemies (not teammates) during events. <font color=7f7f7f>(write one skill's ID and click Add; to remove the skill, simply click on it's ID in the list)</font>", InputType.MultiAdd));
		addConfig(new ConfigModel("setNotOffensiveSkills", "", "Skills written here will be usable only on player's teammates (not opponents/enemies) during events. <font color=7f7f7f>(write one skill's ID and click Add; to remove the skill, simply click on it's ID in the list).", InputType.MultiAdd));
		addConfig(new ConfigModel("setNeutralSkills", "994", "Skills written here will be usable on both teammates and enemies Useful for example for skill Rush (ID 994), which is by default not offensive, and thus the engine doesn't allow the player to cast it on his opponent <font color=7f7f7f>(write one skill's ID and click Add; to remove the skill, simply click on it's ID in the list)</font>", InputType.MultiAdd));
		
		addConfig(new ConfigModel("allowPotions", "false", "Put false if you want to disable potions on this event.", InputType.Boolean));
		addConfig(new ConfigModel("allowSummons", "true", "Put false if you want to disable summons on this event.", InputType.Boolean));
		addConfig(new ConfigModel("allowPets", "true", "Put false if you want to disable pets on this event.", InputType.Boolean));
		addConfig(new ConfigModel("allowHealers", "true", "Put false if you want to disable healers/buffers on this event.", InputType.Boolean));
		addConfig(new ConfigModel("removeCubics", "false", "Put true to remove cubics upon teleportation to the event.", InputType.Boolean));
		
		addConfig(new ConfigModel("dualboxCheckForEnemies", "true", "If enabled, only players with different IPs can be enemies in this event.", InputType.Boolean));
		addConfig(new ConfigModel("maxPlayersPerIp", "1", "You can specify here how many players with the same IP are allowed to be in the event. Put -1 to disable this feature."));
		
		addConfig(new ConfigModel("removeBuffsOnStart", "true", "If 'true', all buffs will be removed from players on first teleport to the event.", InputType.Boolean));
		addConfig(new ConfigModel("removeBuffsOnRespawn", "false", "If 'true', all buffs will be removed from players when they respawn (or when the next round starts).", InputType.Boolean));
	}
	
	public void setConfigs(Configurable template)
	{
		try
		{
			for(ConfigModel templateModel : template.getConfigs().values())
			{
				for(ConfigModel thisModel : getConfigs().values())
				{
					if(templateModel.getKey().equals(thisModel.getKey()))
					{
						thisModel.setValue(templateModel.getValue());
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public abstract boolean checkCanFight(PlayerEventInfo gm, RegistrationData[] teams);
	
	// ==========================================================================
	// AUTOMATIC EVENT METHODS
	// ==========================================================================
	
	public void check()
	{
		if(!checkCanRun())
		{
			cleanMe(false);
			
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					check();
				}
			}, 30000);
			
			return;
		}
		
		if(getStartGameInterval() > 0)
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					createGame();
				}
			}, getStartGameInterval());
		}
	}
	
	public boolean checkCanRun()
	{
		int workingMapsCount = 0;
		
		for(EventMap map : EventMapSystem.getInstance().getMaps(getEventType()).values())
		{
			if(_mode.getDisMaps().contains(map.getGlobalId()))
				continue;
			
			if(canRun(map))
			{
				workingMapsCount ++;
			}
		}
		
		_canRun = (workingMapsCount > 0);
		return _canRun;
	}
	
	@Override
	public void run()
	{
		check();
	}
	
	public void createGame()
	{
		// default implementation
	}

	protected RegistrationData findOpponent(RegistrationData team)
	{
		for(RegistrationData opponent : _parties)
		{
			if(opponent.isChosen())
				continue;

			if(opponent.getKeyPlayer().getPlayersId() == team.getKeyPlayer().getPlayersId())
				continue;

			if(strenghtChecks(team, opponent) && ipChecks(team, opponent))
				return opponent;
		}
		return null;
	}
	
	public boolean launchGame(RegistrationData[] teams, EventMap map)
	{
		// default implementation
		return false;
	}
	
	public void cleanMe(boolean abortMatches)
	{
		_locked = true;
		
		if(abortMatches)
		{
			for(MiniEventGame game : _games)
			{
				game.abortDueToError(LanguageEngine.getMsg("game_aborted"));
			}
		}
		
		for(RegistrationData data : _parties)
		{
			data.message(LanguageEngine.getMsg("game_unregistered", getEventName()), false);
			data.register(false, null);
		}
		
		_games.clear();
		_parties.clear();
		_loggedPlayers.clear();
		
		_locked = false;
	}

	// ==========================================================================
	// AUTOMATIC EVENT CONFIGS
	// ==========================================================================
	
	protected int getStartGameInterval()
	{
		return 30000;
	}
	
	public int getDefaultPartySizeToJoin()
	{
		return 5;
	}
	
	protected int getNextGameId()
	{
		return ++_lastGameId;
	}
	
	public int getJoinTimeRestriction()
	{
		for(AbstractFeature f : _mode.getFeatures())
		{
			if(f.getType() == FeatureType.Delays)
			{
				return ((DelaysFeature) f).getRejoinDealy();
			}
		}
		
		return getInt("DelayToWaitSinceLastMatchMs");
	}
	
	// ==========================================================================
	// REGISTRATION, ETC
	// ==========================================================================

	public boolean checkCanRegister(PlayerEventInfo player)
	{
		if(player == null)
			return false;

		if(!EventManager.getInstance().canRegister(player))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_status"));
			return false;
		}

		if(player.isRegistered()) //TODO main events check
		{
			player.sendMessage(LanguageEngine.getMsg("registering_alreadyRegistered"));
			return false;
		}

		int i = EventWarnings.getInstance().getPoints(player);
		if(i >= EventWarnings.MAX_WARNINGS)
		{
			player.sendMessage(LanguageEngine.getMsg("registering_warningPoints", EventWarnings.MAX_WARNINGS, i));
			return false;
		}

		if(!_mode.checkPlayer(player))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_notAllowed"));
			return false;
		}

		int playerLevel = player.getLevel();
		int maxLevel = getInt("MaxLevelToJoin");
		int minLevel = getInt("MinLevelToJoin");
		if(playerLevel < minLevel || playerLevel > maxLevel)
		{
			if(playerLevel < minLevel)
				player.sendMessage(LanguageEngine.getMsg("registering_lowLevel"));
			else
				player.sendMessage(LanguageEngine.getMsg("registering_highLevel"));
			return false;
		}

		if(!allowHealers() && player.isPriest())
		{
			player.sendMessage("Healers cannot register to this event.");
			return false;
		}

		// should never happen to players, maybe with very high ammount of players registered
		if(isTemporaryLocked())
		{
			player.sendMessage("Try it again in few seconds. If this thing keeps showing up, then there's propably something fucked up with this event, contact a GameMaster for fix.");
			return false;
		}

		if(!timeChecks(player))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_timeCheckFailed"));
			return false;
		}

		if(!ipChecks2(player))
		{
			return false;
		}

		if(requireParty()) // party checks
		{
			if(player.getParty() == null)
			{
				player.sendMessage("You must have a party to join the event.");
				return false;
			}
			else
			{
				if(player.getParty().getLeadersId() != player.getPlayersId())
				{
					player.sendMessage(LanguageEngine.getMsg("registering_partyLeader"));
					return false;
				}
				else
					if(player.getParty().getMemberCount() != getDefaultPartySizeToJoin())
					{
						player.sendMessage(LanguageEngine.getMsg("registering_partyMembers", getDefaultPartySizeToJoin()));
						return false;
					}
					else
					{
						if(!checkPartyStatus(player.getParty()))
						{
							player.sendMessage(LanguageEngine.getMsg("registering_partyCantRegister"));
							return false;
						}
					}
			}
		}

		return true;
	}
	
	public boolean registerTeam(PlayerEventInfo player)
	{
		if(!checkCanRegister(player))
		{
			return false;
		}
		
		if(EventConfig.getInstance().getGlobalConfigBoolean("eventSchemeBuffer"))
		{
			if (!EventBuffer.getInstance().hasBuffs(player))
			{
				player.sendMessage(LanguageEngine.getMsg("registering_buffs"));
			}
			
			EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "none", getEventType().getAltTitle());
		}

		return true;
	}
	
	protected void addParty(RegistrationData playerData)
	{
		synchronized(_parties)
		{
			_parties.add(playerData);
		}
	}

	public boolean checkCanUnregisterTeam(PlayerEventInfo player)
	{
		if(player == null || !player.isOnline())
			return false;

		if(player.getRegisteredMiniEvent() == null || player.getRegisteredMiniEvent().getEventType() != getEventType())
		{
			player.sendMessage(LanguageEngine.getMsg("unregistering_notRegistered"));
			return false;
		}
		else
			if(_locked)
			{
				player.sendMessage("Try it again in few seconds. If this thing keeps showing up, then there's propably something fucked up with this event, contact GameMaster for fix.");
				return false;
			}

		if(requireParty())
		{
			if(player.getParty() == null)
			{
				player.sendMessage(LanguageEngine.getMsg("registering_noParty"));
				return false;
			}
			if(player.getParty().getLeadersId() != player.getPlayersId())
			{
				player.sendMessage(LanguageEngine.getMsg("registering_partyLeader_unregister"));
				return false;
			}
		}

		return true;
	}
	
	public boolean unregisterTeam(PlayerEventInfo player)
	{
		return checkCanUnregisterTeam(player);
	}

	public void deleteTeam(RegistrationData team)
	{
		team.message(LanguageEngine.getMsg("unregistering_unregistered2", getEventType().getHtmlTitle()), false);
		team.register(false, null);
		
		synchronized(_parties)
		{
			_parties.remove(team);
		}
	}

	private boolean checkPartyStatus(PartyData party)
	{
		final boolean buffs = EventConfig.getInstance().getGlobalConfigBoolean("eventSchemeBuffer");
		for(PlayerEventInfo member : party.getPartyMembers())
		{
			if(member != null)
			{
				if(member.isRegistered())
				{
					party.getLeader().sendMessage(LanguageEngine.getMsg("registering_party_memberAlreadyRegistered", member.getPlayersName()));
					return false;
				}
				else if(!timeChecks(member))
				{
					party.getLeader().sendMessage(LanguageEngine.getMsg("registering_party_timeCheckFail", member.getPlayersName()));
					return false;
				}
				//TODO: add config IG
				else if(!allowHealers() && member.isPriest())
				{
					party.getLeader().sendMessage(LanguageEngine.getMsg("registering_party_noHealer"));
					return false;				
				}
				else if(!checkPlayer(member))
				{
					party.getLeader().sendMessage("Player " + member.getPlayersName() + " cannot register.");
					return false;	
				}
				
				if(buffs)
				{
					if (!EventBuffer.getInstance().hasBuffs(member))
					{
						member.sendMessage(LanguageEngine.getMsg("registering_buffs"));
					}
					
					EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(member, "mini", getEventType().getAltTitle());
				}
			}
		}
		return true;
	}

	protected boolean timeChecks(PlayerEventInfo player)
	{
		final int delay = getJoinTimeRestriction();
		
		int id = player.getPlayersId();
		long time = System.currentTimeMillis();
		
		for(Entry<Integer, Long> e : _loggedPlayers.entrySet())
		{
			if(e.getKey() == id)
			{
				if(time - delay > e.getValue())
				{
					_loggedPlayers.remove(e);
					return true;
				}
				else if(!player.isGM())
				{
					player.sendMessage(LanguageEngine.getMsg("registering_timeCheckFail", (((e.getValue()+delay) - time)/60000)));
					return false;
				}
				else
					return true;
			}
		}
		
		return true;
	}
	
	public int getDelayHaveToWaitToJoinAgain(PlayerEventInfo player)
	{
		final int delay = getJoinTimeRestriction();
		
		int id = player.getPlayersId();
		long time = System.currentTimeMillis();
		
		for(Entry<Integer, Long> e : _loggedPlayers.entrySet())
		{
			if(e.getKey() == id)
			{
				if(time - delay > e.getValue())
				{
					_loggedPlayers.remove(e);
					return 0;
				}
				else
				{
					return (int) (e.getValue() - (time - delay));
				}
			}
		}
		return 0;
	}

	public boolean canFight(RegistrationData data)
	{
		boolean canFight = true;

		int playersAmmount = 0;

		// party leader is offline
		if(!data.getKeyPlayer().isOnline(true)) // party leader / key player must be online
		{
			canFight = false;
		}

		if(!checkPlayer(data.getKeyPlayer()))
		{
			canFight = false;
		}

		if(requireParty())
		{
			// party was deleted
			if(data.getParty() == null)
			{
				canFight = false;
			}

			if(data.getParty().getMemberCount() > getDefaultPartySizeToJoin())
			{
				data.message(LanguageEngine.getMsg("unregistering_unregistered_partyBig", getEventType().getHtmlTitle()), false);
				canFight = false;
			}

			for(PlayerEventInfo pi : data.getPlayers())
			{
				if(!pi.isOnline(true))
				{
					canFight = false;
				}
				else
				{
					// registered on another event
					if(!pi.isRegistered() || pi.getRegisteredMiniEvent().getEventType() != getEventType())
					{
						data.getPlayers().remove(pi);
						data.getKeyPlayer().sendMessage(LanguageEngine.getMsg("unregistering_memberKicked_anotherEvent", pi.getPlayersName()));
						continue;
					}

					// not in party
					if(pi.getParty() == null || pi.getParty().getLeadersId() != data.getParty().getLeadersId())
					{
						data.getPlayers().remove(pi);
						data.getKeyPlayer().sendMessage(LanguageEngine.getMsg("unregistering_memberKicked_leftParty", pi.getPlayersName()));
						continue;
					}

					if(!checkPlayer(pi))
					{
						data.getPlayers().remove(pi);
						data.getKeyPlayer().sendMessage(LanguageEngine.getMsg("unregistering_memberKicked", pi.getPlayersName()));
					}

					playersAmmount++;
				}
			}

			if(playersAmmount < getDefaultPartySizeToJoin() / 2)
			{
				canFight = false;
			}
		}

		return canFight;
	}
	
	protected void removeInactiveTeams()
	{
		@SuppressWarnings("unused")
		int playersAmmount = 0;
		
		for(RegistrationData data : _parties)
		{
			if(!canFight(data))
			{
				deleteTeam(data);
			}
		}
	}
	
	private boolean checkPlayer(PlayerEventInfo pi)
	{
		if(!EventManager.getInstance().canRegister(pi))
		{
			pi.sendMessage(LanguageEngine.getMsg("unregistering_unregistered"));
			return false;
		}
		
		if(!_mode.checkPlayer(pi))
		{
			pi.sendMessage(LanguageEngine.getMsg("unregistering_unregistered"));
			return false;
		}
		
		if(!allowHealers() && pi.isPriest())
		{
			pi.sendMessage(LanguageEngine.getMsg("unregistering_memberKicked"));
			return false;
		}
		
		return true;
	}

	protected boolean strenghtChecks(RegistrationData t1, RegistrationData t2)
	{
		for(AbstractFeature feature : getMode().getFeatures())
		{
			if(feature.getType() == FeatureType.StrenghtChecks)
			{
				return ((StrenghtChecksFeature) feature).canFight(t1, t2);
			}
		}
		
		if(Math.abs(t1.getAverageLevel() - t2.getAverageLevel()) > getMaxLevelDifference())
			return false;
		
		return true;
	}

	protected boolean ipChecks(RegistrationData p1, RegistrationData p2)
	{
		if(getBoolean("dualboxCheckForEnemies"))
		{
			String ip1, ip2;
			for(PlayerEventInfo player : p1.getPlayers())
			{
				if(player != null && player.isOnline() && !player.isGM())
				{
					ip1 = player.getIp();
					
					if(ip1 != null)
					{
						for(PlayerEventInfo player2 : p2.getPlayers())
						{
							if(player2 != null && player2.isOnline() && !player2.isGM())
							{
								ip2 = player2.getIp();
								
								if(ip2 != null && ip1.equals(ip2))
								{
									if(p1.getPlayers().size() > 1)
									{
										p1.message("Player " + player.getPlayersName() + " has the same IP as someone in " + p2.getKeyPlayer().getPlayersName() + "'s team.", false);
										p2.message("Player " + player2.getPlayersName() + " has the same IP as someone in " + p1.getKeyPlayer().getPlayersName() + "'s team.", false);
									}
									else
									{
										p1.message("Your IP appears to be same as " + p2.getKeyPlayer().getPlayersName() + "'s IP. You can't go against him.", false);
										p2.message("Your IP appears to be same as " + p1.getKeyPlayer().getPlayersName() + "'s IP. You can't go against him.", false);
									}
										
									return false;
								}
							}
						}
					}
				}
			}
		}
		
		return true;
	}
	
	protected boolean ipChecks2(PlayerEventInfo player)
	{
		int i = getInt("maxPlayersPerIp");
		
		if(i == -1 || player.isGM())
			return true;
		
		if(!player.isOnline(true))
			return false;
		
		int occurences = 0;
		String ip1 = player.getIp();
		
		if(ip1 == null)
			return false;
		
		for(RegistrationData data : _parties)
		{
			for(PlayerEventInfo p : data.getPlayers())
			{
				if(p != null && p.isOnline() && ip1.equals(p.getIp()))
				{
					occurences ++;
				}
			}
		}
		
		if(occurences >= i)
		{
			player.sendMessage("There is already " + i + " players using your IP. You may not register. Try it again later.");
			return false;
		}
		
		return true;
	}

	public void logPlayer(PlayerEventInfo pi, int position)
	{
		long time = System.currentTimeMillis();
		
		int rejoin = getJoinTimeRestriction() / 60000;
		
		if(position > 1) // waiting time reduced by half for losers
		{
			time -= getJoinTimeRestriction() / position;
			rejoin /= position;
		}
		
		_loggedPlayers.put(pi.getPlayersId(), time);

		if(pi.isOnline())
			pi.sendMessage(LanguageEngine.getMsg("game_delayMsg", rejoin));
	}
	
	public void notifyDisconnect(PlayerEventInfo player)
	{
		// default implementation
	}
	
	public EventMode getMode()
	{
		return _mode;
	}

	public boolean isTemporaryLocked()
	{
		return _locked;
	}
	
	public void setIsTemporaryLocked(boolean b)
	{
		_locked = b;
	}

	public final void notifyGameEnd(MiniEventGame game, RegistrationData winner)
	{
		if(isTournamentActive())
		{
			//TODO add winner data
			TournamentManager.onMatchEnd(game, winner);
		}

		_games.remove(game);
	}
	
	public String getString(String propName)
	{
		if(_configs.containsKey(propName))
		{
			return _configs.get(propName).getValue();
		}
		
		debug("Wrong String config for event " + getEventType().getAltTitle() + ", name " + propName);
		return "";
	}
	
	public int getInt(String propName)
	{
		if(_configs.containsKey(propName))
		{
			return _configs.get(propName).getValueInt();
		}
		
		debug("Wrong int config for event " + getEventType().getAltTitle() + ", name " + propName);
		return 0;
	}
	
	public boolean getBoolean(String propName)
	{
		if(_configs.containsKey(propName))
		{
			return _configs.get(propName).getValueBoolean();
		}
		
		debug("Wrong boolean config for event " + getEventType().getAltTitle() + ", name " + propName);
		return false;
	}
	
	protected void addConfig(ConfigModel model)
	{
		_configs.put(model.getKey(), model);
	}
	
	protected void removeConfig(String key)
	{
		_configs.remove(key);
	}
	
	protected void addConfig(String category, ConfigModel model)
	{
		if(!_configCategories.contains(_configCategories))
			_configCategories.add(category);
		
		_configs.put(model.getKey(), model.setCategory(category));
	}
	
	protected void addMapConfig(ConfigModel model)
	{
		_mapConfigs.put(model.getKey(), model);
	}
	
	protected void removeMapConfigs()
	{
		_mapConfigs.clear();
	}
	
	protected void removeConfigs()
	{
		_configCategories.clear();
		_configs.clear();
	}
	
	@Override
	public Map<String, ConfigModel> getConfigs()
	{
		return _configs;
	}
	
	@Override
	public void clearConfigs()
	{
		removeConfigs();
		removeMapConfigs();
	}
	
	@Override
	public FastList<String> getCategories()
	{
		return _configCategories;
	}
	
	@Override
	public void setConfig(String key, String value, boolean addToValue)
	{
		if(!_configs.containsKey(key))
			return;
		
		if(!addToValue)
			_configs.get(key).setValue(value);
		else
			_configs.get(key).addToValue(value);
	}
	
	@Override
	public Map<String, ConfigModel> getMapConfigs()
	{
		return _mapConfigs;
	}
	
	public boolean canRun()
	{
		return _canRun;
	}
	
	@Override
	public boolean canRun(EventMap map)
	{
		return getMissingSpawns(map).length() == 0;
	}
	
	protected String addMissingSpawn(SpawnType type, int team, int count)
	{
		return "<font color=bfbfbf>" + getMode().getModeName() + " </font><font color=696969>mode</font> -> <font color=9f9f9f>No</font> <font color=B46F6B>" + type.toString().toUpperCase() + "</font> <font color=9f9f9f>spawn for team " + team + " " + (team == 0 ? "(team doesn't matter)" : "") + " count " + count + " (or more)</font><br1>";
	}
	
	public String getMapConfig(EventMap map, String name)
	{
		return EventConfig.getInstance().getMapConfig(map, getEventType(), name);
	}
	
	public int getMapConfigInt(EventMap map, String name)
	{
		return EventConfig.getInstance().getMapConfigInt(map, getEventType(), name);
	}
	
	public boolean getMapConfigBoolean(EventMap map, String name)
	{
		return EventConfig.getInstance().getMapConfigBoolean(map, getEventType(), name);
	}
	
	protected int getMaxLevelDifference()
	{
		return getInt("MaxLevelDifference");
	}
	
	@Override
	public String getDescriptionForReward(RewardPosition reward)
	{
		return null;
	}
	
	public boolean isTournamentActive()
	{
		return _tournamentActive;
	}
	
	public void setTournamentActive(boolean b)
	{
		_tournamentActive = b;
	}

	// ==========================================================================
	// CONFIGS - methods which can be overriden
	// ==========================================================================

	public abstract String getHtmlDescription();

	public abstract RegistrationData createRegistrationData(PlayerEventInfo player);
	
	public List<MiniEventGame> getActiveGames()
	{
		return _games;
	}

	public int getRegisteredTeamsCount()
	{
		if(_parties == null)
			return 0;
		
		return _parties.size();
	}
	
	public List<RegistrationData> getRegistered()
	{
		return _parties;
	}
	
	@Override
	public String getEventName()
	{
		return getEventType().getAltTitle();
	}
	
	public boolean requireParty()
	{
		return true;
	}
	
	public boolean allowTournament()
	{
		return true;
	}
	
	public int getMaxGamesCount()
	{
		return 99;
	}
	
	protected boolean allowHealers()
	{
		return getBoolean("allowHealers");
	}
}
