package cz.nxs.events.engine.main.events;

import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 */
public class LastManStanding extends Deathmatch
{
	protected class LMSData extends DMData
	{
		protected boolean	_isActive;
		protected boolean	_waitingState;
		protected boolean	_roundActive;
		protected int		_waitingStateTime;
		protected int		_round;
		protected int		_alivePlayers;
		protected Timelimit	_timelimit;
		
		protected LMSData(int instance)
		{
			super(instance);
			_alivePlayers = 0;
			_round = 0;
			_isActive = true;
			_waitingState = false;
			_roundActive = true;
		}
		
		public void onTick()
		{
			_timelimit.onTick();
			if (_waitingState)
			{
				if (_waitingStateTime > 0)
				{
					_waitingStateTime--;
					switch (_waitingStateTime)
					{
						case 180:
						case 120:
						case 60:
						case 30:
						case 20:
						case 15:
						case 10:
						case 5:
						case 4:
						case 3:
						case 2:
						case 1:
							announce(_instanceId, LanguageEngine.getMsg("lms_roundStart", _round, _waitingStateTime));
					}
				}
			}
		}
		
		private class Timelimit
		{
			private int		limit;
			private boolean	aborted	= false;
			
			public Timelimit()
			{
				limit = _roundTimeLimit;
			}
			
			public void onTick()
			{
				if (limit > 0)
					limit -= 1;
				if (!aborted && limit <= 0)
				{
					aborted = true;
					endRoundDueToTime(_instanceId);
					/**/ if (NexusLoader.detailedDebug)
						print("Event: round ended due to time limit");
				}
			}
		}
		
		/** returns true if only one player is alive */
		private boolean playerDied()
		{
			if (_alivePlayers > 0)
				_alivePlayers--;
			return _alivePlayers == 1;
		}
		
		private boolean canStartNewRound()
		{
			return _isActive && _round < _maxRounds;
		}
		
		private void newRound()
		{
			_isActive = true;
			_round++;
			_timelimit = new Timelimit();
		}
		
		private void setWaitingState(boolean b)
		{
			_waitingState = b;
			if (b)
				_waitingStateTime = _roundWaitTime + 1;
		}
		
		protected boolean isActive()
		{
			return _isActive;
		}
		
		protected synchronized void setInactive()
		{
			_isActive = false;
		}
	}
	
	/** Overriden in Deathmatch event */
	protected class LMSEventInstance extends DMEventInstance
	{
		public LMSEventInstance(InstanceData instance)
		{
			super(instance);
			if (_disableAnnouncingCountdown)
				_clock.disableAnnouncingCountdown();
		}
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				/**/ if (NexusLoader.detailedDebug)
					print("Event: running task of state " + _nextState.toString() + "...");
				switch (_nextState)
				{
					case START:
					{
						if (checkPlayers(_instance.getId()))
						{
							if (_antifeed)
							{
								for (PlayerEventInfo player : getPlayers(_instance.getId()))
									player.startAntifeedProtection(false);
							}
							teleportPlayers(_instance.getId(), SpawnType.Regular, true);
							setupTitles(_instance.getId());
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
						startRound(_instance.getId());
						_clock.startClock(_manager.getRunTime());
						break;
					}
					case END:
					{
						_clock.setTime(0, true);
						setNextState(EventState.INACTIVE);
						if (!instanceEnded() && _canBeAborted)
						{
							if (_canRewardIfAborted)
								rewardAllPlayers(_instance.getId(), 0, getInt("killsForReward"));
							clearEvent(_instance.getId());
						}
						break;
					}
				}
				/**/ if (NexusLoader.detailedDebug)
					print("Event: ... finished running task. next state " + _nextState.toString());
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}
	
	private int			_roundWaitTime;
	private int			_maxRounds;
	private int			_roundTimeLimit;
	private int			_scoreForRoundWinner;
	private boolean		_disableAnnouncingCountdown;
	private String[]	_scorebarFormat;
	
	public LastManStanding(EventType type, MainEventManager manager)
	{
		super(type, manager);
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		ConfigModel scorebarFormat = new ConfigModel("screenScoreBarFormat", "AliveAndRounds", "Specify here how will the player's screen Score bar look like. " + "<br1><font color=LEVEL>Alive</font> shows the count of players that are still alive, excluding you." + "<br1><font color=LEVEL>Time</font> shows the time left for the event (using Run time value)" + "<br1><font color=LEVEL>Rounds</font> shows the current round / max rounds in the event." + "<br1><font color=LEVEL>Top</font> shows the score of the top player in the event." + "<br1>Example: <font color=LEVEL>AliveAndRounds</font> will show following text: 'Alive: 12, Round: 1/3', where 12 is the count of alive players excluding you, 1 is the current round and 3 si the total count of rounds in this event (configurable).", InputType.Enum);
		scorebarFormat.addEnumOptions(new String[]
		{
			"Alive", "Rounds", "Time", "Top", "AliveAndRounds", "AliveAndTime", "AliveAndTop", "RoundsAndTime", "RoundsAndTop", "TopAndTime"
		});
		addConfig(scorebarFormat);
		removeConfig("runTime"); // replace runTime
		removeConfig("rejoinAfterDisconnect");
		removeConfig("removeWarningAfterRejoin");
		addConfig(new ConfigModel("runTime", "30", "The run time of this event, launched automatically by the scheduler. Max value globally for all events is 120 minutes. <font color=699768>It is recommended to use a higher run time (30+ minutes) in combination with lower value of </font><font color=LEVEL>maxRounds</font> <font color=699768>(3-5).</font> In minutes!"));
		addConfig(new ConfigModel("maxRounds", "3", "The maximum count of rounds that will be runned in this event. One round ends when there's only one player alive. If an event instance reaches this rounds limit, the event instance will end. The event ends (meaning you can start/schedule a new event) only when all event instances have ended."));
		addConfig(new ConfigModel("roundTimeLimit", "600", "The time after it automatically ends current round. Useful to prevent afking on events or if any stupid player don't know what to do (even tho if a player goes afk, he will be killed automatically). In seconds."));
		addConfig(new ConfigModel("scoreForRoundWinner", "3", "Number of score points given to a round winner (the only player who survived). Remember, that one kill = 1 score."));
		addConfig(new ConfigModel("roundWaitTime", "5", "The time players have to wait when a new round started. They are rooted and can't attack anyone. There's a countdown too. This is here because it looks cool."));
		addConfig(new ConfigModel("disableCountdown", "true", "Put true to disable classic event's end countdown announcement. Good if you want to have this event only round-based, like it ends after 3 rounds and not look like there's a 20 minutes limit. Putting high run time (eg. 30 minutes) and lower rounds count (3-5) is recommended for this event.", InputType.Boolean));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		_maxRounds = getInt("maxRounds");
		_roundTimeLimit = getInt("roundTimeLimit");
		_scoreForRoundWinner = getInt("scoreForRoundWinner");
		_roundWaitTime = getInt("roundWaitTime");
		_disableAnnouncingCountdown = getBoolean("disableCountdown");
		_scorebarFormat = getString("screenScoreBarFormat").split("And");
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
		DMEventInstance match;
		_matches = new FastMap<Integer, DMEventInstance>();
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
	
	private void startRound(final int instanceId)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: trying to start new round for instance " + instanceId);
		if (getEventData(instanceId).canStartNewRound())
		{
			/**/ if (NexusLoader.detailedDebug)
				print("Event: starting new round; current round = " + getEventData(instanceId)._round);
			getEventData(instanceId)._roundActive = true;
			getEventData(instanceId)._alivePlayers = getPlayers(instanceId).size();
			// respawn all players
			for (PlayerEventInfo player : getPlayers(instanceId))
			{
				respawnPlayer(player, instanceId);
				if (_allowSchemeBuffer)
					EventBuffer.getInstance().buffPlayer(player, true);
			}
			getEventData(instanceId).newRound();
			// to start waitingState end's countdown
			getEventData(instanceId).setWaitingState(true);
			// apply effects on players
			waitingStateEffects(instanceId, true);
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if (getEventData(instanceId)._isActive && _matches.get(instanceId)._nextState == EventState.END)
					{
						announce(instanceId, LanguageEngine.getMsg("lms_roundStarted", getEventData(instanceId)._round));
						getEventData(instanceId).setWaitingState(false);
						// remove effects from players
						waitingStateEffects(instanceId, false);
					}
				}
			}, _roundWaitTime * 1000);
			/**/ if (NexusLoader.detailedDebug)
				print("Event: new round started!");
		}
		else
		{
			/**/ if (NexusLoader.detailedDebug)
				print("Event: CAN'T START new round!");
			announce(instanceId, "Configs are wrong for Last Man Standing event. Event aborted until fixed.");
			NexusLoader.debug("Rounds count config for LMS must be at least 1. Event has been aborted", Level.WARNING);
			endInstance(instanceId, true, false, true);
		}
	}
	
	private void waitingStateEffects(int instance, boolean apply)
	{
		for (PlayerEventInfo player : getPlayers(instance))
		{
			player.setIsParalyzed(apply);
			player.paralizeEffect(apply);
			player.setIsInvul(apply);
		}
	}
	
	private synchronized void endRound(final int instanceId, boolean aborted, boolean endInstance)
	{
		if (!getEventData(instanceId)._roundActive)
			return;
		/**/ if (NexusLoader.detailedDebug)
			print("Event: ending round of instance " + instanceId + " aborted = " + aborted + ", end instance " + endInstance);
		getEventData(instanceId)._roundActive = false;
		PlayerEventInfo winner = null;
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			if (!player.isDead())
			{
				winner = player;
			}
		}
		if (!aborted && winner != null)
		{
			getPlayerData(winner).raiseScore(_scoreForRoundWinner);
			setScoreStats(winner, getPlayerData(winner).getScore());
			announce(instanceId, LanguageEngine.getMsg("lms_roundWon", winner.getPlayersName(), getEventData(instanceId)._round));
		}
		if (getEventData(instanceId).canStartNewRound() && !endInstance)
		{
			announce(instanceId, LanguageEngine.getMsg("lms_roundStartsIn", 10));
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if (getEventData(instanceId).isActive() && _matches.get(instanceId)._nextState == EventState.END)
					{
						startRound(instanceId);
					}
				}
			}, 10000);
		}
		else
		{
			announce(instanceId, LanguageEngine.getMsg("lms_eventEnded"));
			synchronized (getEventData(instanceId))
			{
				getEventData(instanceId).setInactive();
			}
			endInstance(instanceId, true, true, false);
		}
	}
	
	private void endRoundDueToTime(int instanceId)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: end due to round time = " + instanceId);
		announce(instanceId, LanguageEngine.getMsg("lms_roundAborted_timeLimit", (_roundTimeLimit / 60)));
		endRound(instanceId, true, false);
	}
	
	private void endRoundDueToEventTimeLimit(int instanceId, boolean announceTimeLimit)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: ending round due to event time limit " + instanceId + ", announce time limit = " + announceTimeLimit);
		synchronized (getEventData(instanceId))
		{
			getEventData(instanceId).setInactive();
		}
		if (announceTimeLimit)
			announce(instanceId, LanguageEngine.getMsg("lms_roundAborted"));
		endRound(instanceId, true, true);
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if (NexusLoader.detailedDebug)
			print("Event: onEventEnd()");
		// end of runtime limit - abort running instances
		for (DMEventInstance match : _matches.values())
		{
			if (getEventData(match.getInstance().getId()).isActive())
			{
				endRoundDueToEventTimeLimit(match.getInstance().getId(), true);
			}
		}
		super.onEventEnd();
	}
	
	@Override
	protected String getScorebar(int instance)
	{
		final int countAlive = getEventData(instance)._alivePlayers - 1;
		final String time = _matches.get(instance).getClock().getTime();
		final String rounds = getEventData(instance)._round + "/" + _maxRounds;
		int top = 0;
		for (PlayerEventInfo player : getPlayers(instance))
		{
			if (getPlayerData(player).getScore() > top)
				top = getPlayerData(player).getScore();
		}
		TextBuilder tb = new TextBuilder();
		String[] types = _scorebarFormat;
		String type;
		for (int i = 0; i < types.length; i++)
		{
			type = types[i];
			if (type.equals("Alive"))
			{
				tb.append(LanguageEngine.getMsg("lms_scorebar_alive") + " " + countAlive);
			}
			else if (type.equals("Time"))
			{
				tb.append(LanguageEngine.getMsg("event_scorebar_time", time));
			}
			else if (type.equals("Rounds"))
			{
				tb.append(LanguageEngine.getMsg("lms_scorebar_rounds") + " " + rounds);
			}
			else if (type.equals("Top"))
			{
				tb.append(LanguageEngine.getMsg("lms_scorebar_top") + " " + top);
			}
			if (i + 1 < types.length)
			{
				tb.append("  ");
			}
		}
		return tb.toString();
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if (target.getEventInfo() == null)
			return;
		tryFirstBlood(player);
		giveOnKillReward(player);
		// update player's own stats
		getPlayerData(player).raiseScore(1);
		getPlayerData(player).raiseKills(1);
		getPlayerData(player).raiseSpree(1);
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
	
	@Override
	public synchronized void onDie(PlayerEventInfo player, CharacterData killer)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("/// Event: onDie - player " + player.getPlayersName() + " (instance " + player.getInstanceId() + "), killer " + killer.getName());
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		player.disableAfkCheck(true);
		setDeathsStats(player, getPlayerData(player).getDeaths());
		if (getEventData(player.getInstanceId()).playerDied())
		{
			endRound(player.getInstanceId(), false, false);
		}
		else
		{
			if (getEventData(player.getInstanceId()).canStartNewRound())
				player.sendMessage(LanguageEngine.getMsg("lms_notifyPlayerRespawn"));
		}
	}
	
	@Override
	public void playerWentAfk(PlayerEventInfo player, boolean warningOnly, int afkTime)
	{
		if (warningOnly)
		{
			player.sendMessage(LanguageEngine.getMsg("event_afkWarning_kill", (PlayerEventInfo.AFK_WARNING_DELAY / 1000), (PlayerEventInfo.AFK_KICK_DELAY / 1000)));
		}
		else
		{
			if (getEventData(player.getInstanceId())._roundActive)
			{
				announce(player.getInstanceId(), LanguageEngine.getMsg("event_afkMarked_andDied", player.getPlayersName()));
				player.doDie();
			}
		}
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
	public void onDisconnect(PlayerEventInfo player)
	{
		// also rework the full engine to make working with instances easier
		if (player.isOnline()) // still online
		{
			/**/ if (NexusLoader.detailedDebug)
				print("AbstractMainEvent: player " + player.getPlayersName() + " (instance id = " + player.getInstanceId() + ") disconnecting from the event");
			EventTeam team = player.getEventTeam();
			player.restoreData(); // restore title, etc. before logging out and saving
			player.setXYZInvisible(player.getOrigLoc().getX(), player.getOrigLoc().getY(), player.getOrigLoc().getZ());
			EventWarnings.getInstance().addPoints(player.getPlayersId(), 1);
			boolean running = false;
			AbstractEventInstance playersMatch = getMatch(player.getInstanceId());
			if (playersMatch == null)
			{
				NexusLoader.debug("Player's EventInstance is null, called onDisconnect", Level.WARNING);
				/**/ if (NexusLoader.detailedDebug)
					print("AbstractMainEvent: !!! -.- player's EVENT INSTANCE is null after calling onDisconnect. Player's instanceId is = " + player.getInstanceId());
				running = false;
			}
			else
				running = playersMatch.isActive();
			// remove the player from his team
			team.removePlayer(player);
			_manager.getPlayers().remove(player);
			CallBack.getInstance().getPlayerBase().playerDisconnected(player);
			// this event's instance is still active
			if (running)
			{
				/**/ if (NexusLoader.detailedDebug)
					print("AbstractMainEvent: -.- event is active");
				debug(getEventName() + ": Player " + player.getPlayersName() + " disconnected from main event, still enought players to continue the event.");
				// for sure
				if (team.getPlayers().isEmpty())
				{
					announce(player.getInstanceId(), LanguageEngine.getMsg("event_disconnect_team", team.getTeamName()));
					debug(getEventName() + ": all players from team " + team.getTeamName() + " have disconnected.");
					/**/ if (NexusLoader.detailedDebug)
						print("AbstractMainEvent: ALL PLAYERS FROM TEAM " + team.getTeamName() + " disconnected");
				}
				// not enought of players online (<2), event is to be aborted
				if (!checkIfEventCanContinue(player.getInstanceId(), player))
				{
					announce(player.getInstanceId(), LanguageEngine.getMsg("event_disconnect_all"));
					endInstance(player.getInstanceId(), true, false, false);
					debug(getEventName() + ": no players left in the teams, the fight cannot continue. The event has been aborted!");
					/**/ if (NexusLoader.detailedDebug)
						print("AbstractMainEvent: NO PLAYERS LEFT IN THE TEAMS, THE FIGHT CAN'T CONTINUE! (checkIfEventCanContinue = false)");
					return;
				}
				// still enought of players is online
				if (checkIfAllDied(player.getInstanceId()))
				{
					endRound(player.getInstanceId(), false, false);
				}
			}
			else
			/**/ if (NexusLoader.detailedDebug)
				print("AbstractMainEvent: -.- event IS NOT active anymore");
		}
	}
	
	private boolean checkIfAllDied(int instanceId)
	{
		int alive = 0;
		for (PlayerEventInfo pi : getPlayers(instanceId))
		{
			if (pi != null && !pi.isDead())
				alive++;
		}
		return alive < 2;
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
				for (DMEventInstance match : _matches.values())
				{
					if (instanceId == 0 || instanceId == match.getInstance().getId())
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
			if (!player.isOnline())
				continue;
			if (player.hasAntifeedProtection())
			{
				player.stopAntifeedProtection(false);
			}
			if (player.isParalyzed())
			{
				player.setIsParalyzed(false);
				player.paralizeEffect(false);
			}
			player.setIsInvul(false);
			player.removeRadarAllMarkers();
			if (player.isImmobilized())
				player.unroot();
			if (!player.isGM())
				player.setIsInvul(false);
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
		/**/ if (NexusLoader.detailedDebug)
			print("Event: called global clearEvent()");
		try
		{
			if (_matches != null)
			{
				// in case the event has been aborted by a GM
				for (DMEventInstance match : _matches.values())
				{
					if (getEventData(match.getInstance().getId()).isActive())
					{
						endRoundDueToEventTimeLimit(match.getInstance().getId(), true);
					}
				}
			}
		}
		catch (Exception e)
		{}
		clearEvent(0);
	}
	
	@Override
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		/**/ if (NexusLoader.detailedDebug)
			print("/// Event: respawning player " + pi.getPlayersName() + ", instance " + instance);
		EventSpawn spawn = getSpawn(SpawnType.Regular, -1);
		if (spawn != null)
		{
			Loc loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(spawn.getRadius());
			pi.disableAfkCheck(false);
			pi.teleport(loc, 0, true, instance);
		}
		else
		{
			// abortEvent("An error occured with Event Map.");
			debug("Error on respawnPlayer - no spawn type REGULAR, team -1 (FFA) has been found. Event aborted.");
		}
	}
	
	@Override
	public String getEstimatedTimeLeft()
	{
		if (_matches == null)
			return "Starting";
		// may be different in each instance
		for (DMEventInstance match : _matches.values())
		{
			if (getEventData(match.getInstance().getId())._isActive)
				return "+-" + ((_maxRounds - getEventData(match.getInstance().getId())._round) + 1) + " rounds";
		}
		return null;
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		final int countAlive = getEventData(instance)._alivePlayers - 1;
		final String rounds = getEventData(instance)._round + " of " + _maxRounds;
		String status = "<td align=center width=200><font color=ac9887>Round: </font><font color=9f9f9f>" + rounds + "</font></td><td align=center width=200><font color=ac9887>Alive: </font><font color=9f9f9f>" + countAlive + " players</font></td>";
		return ("<table width=510 bgcolor=3E3E3E><tr>" + status + "</tr></table>");
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
				_htmlDescription = "This is a free-for-all event, don't expect any help from teammates. ";
				_htmlDescription += "This event has " + getInt("maxRounds") + " rounds. You can gain score by killing your opponents (1 kill = 1 score), but if you die, you won't get resurrected until the next round starts. ";
				_htmlDescription += "The player, who wins the round (when all other players are dead) receives additional " + getInt("scoreForRoundWinner") + " score points. ";
				if (getBoolean("antifeedProtection"))
					_htmlDescription += "This event has a protection, which completely changes the appearance of all players and temporary removes their title and clan/ally crests. ";
				if (getInt("killsForReward") > 0)
					_htmlDescription += "In the end, you need at least " + getInt("killsForReward") + " kills to receive a reward.";
			}
		}
		return _htmlDescription;
	}
	
	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if (_hideTitles)
			return "";
		if (pi.isAfk())
			return "AFK";
		return "Score: " + getPlayerData(pi).getScore() + " Deaths: " + getPlayerData(pi).getDeaths();
	}
	
	@Override
	protected void clockTick()
	{
		for (DMEventInstance match : _matches.values())
		{
			((LMSData) match._data).onTick();
		}
	}
	
	@Override
	protected AbstractEventInstance getMatch(int instanceId)
	{
		return _matches.get(instanceId);
	}
	
	@Override
	protected DMData createEventData(int instance)
	{
		return new LMSData(instance);
	}
	
	@Override
	protected LMSEventInstance createEventInstance(InstanceData instance)
	{
		return new LMSEventInstance(instance);
	}
	
	@Override
	protected LMSData getEventData(int instance)
	{
		try
		{
			return (LMSData) _matches.get(instance)._data;
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error on getEventData for instance " + instance);
			e.printStackTrace();
			return null;
		}
	}
}
