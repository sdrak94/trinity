package cz.nxs.events.engine.mini.tournament;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;

/**
 * Created by Lukas
 * TODO turn RegistrationData.message into html windows
 * TODO add info about upcoming matches, score board, etc
 * TODO rewrite announcements
 */
public class Tournament
{
	/**
	 * sorts participants by their wonMatches, descending
	 */
	public Comparator<Participant> compareParticipantsByWonMatches = new Comparator<Participant>()
	{
		@Override
		public int compare(Participant t1, Participant t2)
		{
			int wins1 = t1._numberOfWins;
			int wins2 = t2._numberOfWins;

			return wins1 == wins2 ? 0 : wins1 < wins2 ? 1 : -1;
		}
	};

	private MiniEventManager _event = null;
	private int _minCountToRegister = 2;
	private int _maxCountToRegister = 4;
	private String _rewardString = null;

	private boolean _registrationOpen;
	private boolean _matchesInProgress;

	private List<Participant> _originallyRegistered;
	private List<Participant> _registered;
	private int _timeForRegistration;

	private ScheduledFuture<?> _registrationEnd = null;
	private List<MiniEventGame> _activeMatches;

	private ScheduledFuture<?> _matchTimeLimit = null;

	public Tournament(MiniEventManager _event)
	{
		this._event = _event;
		_registrationOpen = false;
		_matchesInProgress = false;
	}

	public void setMinCountToRegister(int countToRegister)
	{
		this._minCountToRegister = countToRegister;
	}

	public void setMaxCountToRegister(int countToRegister)
	{
		this._maxCountToRegister = countToRegister;
	}

	public void setTimeForRegistration(int timeForRegistration)
	{
		this._timeForRegistration = timeForRegistration;
	}

	public void setRewardString(String s)
	{
		_rewardString = s;
	}

	public void openRegistration()
	{
		_registered = new FastList<>();
		_registrationOpen = true;

		announce("The registration for the tournament has been opened for " + _timeForRegistration/60000 + " minutes.");

		_registrationEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				closeRegistration();
			}
		}, _timeForRegistration);
	}

	public boolean skipRegistration()
	{
		boolean success = false;
		if(_registrationOpen && _event != null)
		{
			if(_registrationEnd != null)
			{
				_registrationEnd.cancel(false);
				_registrationEnd = null;
			}

			closeRegistration();
			success = true;
		}
		return success;
	}

	public void closeRegistration()
	{
		_registrationOpen = false;

		announce("The tournament registration has been closed.");

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				startMatches();
			}
		}, 10000);
	}

	public boolean canStartTournament()
	{
		return _registered.size() >= _minCountToRegister;
	}

	public void startMatches()
	{
		if(canStartTournament())
		{
			announce("The tournament has started! The matches will begin soon.");

			// will be used when giving rewards
			_originallyRegistered = new FastList<>();
			_originallyRegistered.addAll(_registered);

			nextRound(false);
		}
	}

	private void startTimeLimit()
	{
		_matchTimeLimit = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				abortMatches("Match aborted due to time limit of 15 minutes.");
			}
		}, 900000);
	}

	private void abortTimeLimit()
	{
		if(_matchTimeLimit != null)
		{
			_matchTimeLimit.cancel(false);
			_matchTimeLimit = null;
		}
	}

	private void abortMatches(String reason)
	{
		for(MiniEventGame game : _activeMatches)
		{
			if(game != null)
			{
				game.abortDueToError(reason);
			}
		}
	}

	public void nextRound(boolean removeNotWon)
	{
		_activeMatches = new FastList<>();

		// first remove invalid participants and those who lost their previous match
		for(Participant participant : _registered)
		{
			if(!participant.canFight())
			{
				participant._data.message("You've been removed from the event because you don't meet the tournament conditions.", true);
				participant._data.register(false, null);

				_registered.remove(participant);
			}

			if(removeNotWon)
			{
				if(!participant._wonMatch)
				{
					participant._data.message("You dropped out from the tournament.", true);
					participant._data.register(false, null);

					_registered.remove(participant);
				}
			}

			participant._data.register(true, _event);

			participant.notifyNewMatch();
		}

		if(_registered.size() < 2)
		{
			finishTournament();
			return;
		}

		List<Matched> matchedList = new FastList<Matched>();

		List<Participant> toFight = new FastList<>();
		toFight.addAll(_registered);
		Collections.shuffle(toFight);

		Participant opponent;
		for(Participant participant : toFight)
		{
			opponent = findOpponent(participant, toFight);

			if(opponent != null)
			{
				toFight.remove(participant);
				toFight.remove(opponent);

				Matched matched = new Matched(participant, opponent);
				matchedList.add(matched);
			}
		}

		// remaining players who didn't receive any opponent
		for(Participant participant : toFight)
		{
			participant.setWonMatch();
			participant._data.message("There is no opponent for you in this round, you will automatically advance to the next round. Please wait till the current matches end.", false);
		}

		String roundName = TournamentRoundName.getRoundName(_registered.size());
		announce(roundName + " has just started.");
		for(Matched matched : matchedList)
		{
			startMatch(matched);
		}

		startTimeLimit();

		_matchesInProgress = true;
	}

	private Participant findOpponent(Participant challenger, List<Participant> toFight)
	{
		for(Participant opponent : toFight)
		{
			if(opponent._data.getKeyPlayer().getPlayersId() == challenger._data.getKeyPlayer().getPlayersId())
				continue;

			return opponent;
		}

		return null;
	}

	private class Matched
	{
		Participant p1, p2;

		private Matched(Participant p1, Participant p2)
		{
			this.p1 = p1;
			this.p2 = p2;
		}
	}

	private void startMatch(Matched matched)
	{
		List<RegistrationData> players = new FastList<RegistrationData>();

		players.add(matched.p1._data);
		players.add(matched.p2._data);

		_event.launchGame(players.toArray(new RegistrationData[players.size()]), null);
	}

	public void onMatchEnd(MiniEventGame game, RegistrationData winner)
	{
		if(game.getEvent().getEventType() != _event.getEventType())
			return;

		// happens mostly if both teams did nothing during the event
		if(winner == null)
		{
			if(game.getRegisteredTeams() != null)
			{
				for(RegistrationData data : game.getRegisteredTeams())
				{
					if(data != null)
					{
						data.message("The last match didn't have a winner, so you've been removed from the tournament. Try harder next time!", true);

						// remove the team from the tournament
						Participant participant = getParticipantData(data);
						if(participant != null)
						{
							participant._data.message("You have been unregistered from the tournament.", false);
							participant._data.register(false, null);

							_registered.remove(participant);
						}
					}
				}
			}
		}
		// set this player as a winner
		else
		{
			Participant winnerData = getParticipantData(winner);

			winnerData.setWonMatch();
		}

		_activeMatches.remove(game);

		if(_activeMatches.isEmpty())
		{
			announce("All tournament matches have finished.");
			abortTimeLimit();

			_matchesInProgress = false;

			if(_registered.size() >= 2)
			{
				announce("The next round will begin in 60 seconds.");

				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						nextRound(true);
					}
				}, 60000);
			}
			else if(_registered.size() == 1)
			{
				finishTournament();
			}
			else
				clean();
		}
	}

	public void finishTournament()
	{
		_matchesInProgress = false;

		rewardParticipants();
	}

	public void rewardParticipants()
	{
		announce("The tournament has ended.");
		announce("The participants have received their rewards.");

		Collections.sort(_originallyRegistered, compareParticipantsByWonMatches);

		Map<Integer, Integer> rewards = new FastMap<Integer, Integer>();

		String[] positions = _rewardString.split(";");

		String positionNumber;
		int rewardCount;

		for(String position : positions)
		{
			if(position.length() == 0)
				continue;

			try
			{
				positionNumber = position.split(":")[0];
				rewardCount = Integer.parseInt(position.split(":")[1]);

				// format #-#
				if(positionNumber.split("-").length == 2)
				{
					int from = Integer.parseInt(positionNumber.split("-")[0]);
					int to = Integer.parseInt(positionNumber.split("-")[1]);

					for(int i = from; from <= to; i++)
					{
						rewards.put(i, rewardCount);
					}
				}
				// format #
				else
				{
					rewards.put(Integer.parseInt(positionNumber), rewardCount);
				}
			}
			catch (Exception e )
			{
				e.printStackTrace();
			}
		}

		int position = 0;
		for(Participant participant : _originallyRegistered)
		{
			position ++;

			if(position == 1 && participant._numberOfWins < 2)
			{
				// the tournament was invalid, all teams most likely disconnected
				return;
			}
			else if(participant._numberOfWins == 0)
				continue;

			//TODO html result
			participant._data.message("Your team beated " + participant._numberOfWins + " opponents.", false);

			if(rewards.containsKey(position))
			{
				rewardCount = rewards.get(position);

				for(PlayerEventInfo player : participant._data.getPlayers())
				{
					if(player != null)
						player.addItem(6392, rewardCount, true);
				}
			}
		}
	}

	public boolean stopByGm(PlayerEventInfo gm)
	{
		if(_registrationOpen || _matchesInProgress)
		{
			abortMatches("The tournament aborted by a GM.");

			for(Participant participant : _registered)
			{
				participant._data.message("The tournament has been cancelled by a GM.", true);
				participant._data.register(false, null);

				_registered.remove(participant);
			}

			return true;
		}

		gm.sendMessage("Can't be aborted now.");
		return false;
	}

	public void clean()
	{
		TournamentManager.cleanTournament();
	}

	public boolean isOpenRegistration()
	{
		return _registrationOpen;
	}

	public void register(PlayerEventInfo player)
	{
		if(_registrationOpen && _event != null)
		{
			if(_registered.size() >= _maxCountToRegister)
			{
				player.sendMessage("The tournament is already full.");
				return;
			}

			if(!_event.checkCanRegister(player))
			{
				player.sendMessage("You may not register.");
				return;
			}

			RegistrationData regData = _event.createRegistrationData(player);
			regData.register(true, _event);

			Participant participant = new Participant(regData);

			_registered.add(participant);
			regData.message("You've been registered to the tournament.", true);
		}
	}

	public void unregister(PlayerEventInfo player, boolean forced)
	{
		if(!forced)
		{
			if(_registrationOpen && _event != null)
			{
				if(_event.checkCanUnregisterTeam(player))
				{
					for(Participant participant : _registered)
					{
						if(participant._data.getKeyPlayer().getPlayersId() == player.getPlayersId())
						{
							participant._data.message("You've been unregistered from the tournament.", true);
							participant._data.register(false, null);

							_registered.remove(participant);
						}
					}
				}
			}
		}
		else
		{
			if(_event != null)
			{
				for(Participant participant : _registered)
				{
					if(participant._data.getKeyPlayer().getPlayersId() == player.getPlayersId())
					{
						participant._data.message("You've been unregistered from the tournament.", true);
						participant._data.register(false, null);

						_registered.remove(participant);
					}
				}
			}
		}
	}

	public void announce(String text)
	{
		CallBack.getInstance().getOut().announceToAllScreenMessage(text, _event.getMode().getVisibleName());
	}

	public Participant getParticipantData(RegistrationData data)
	{
		for(Participant participant : _registered)
		{
			if(participant._data.getId() == data.getId() || (participant._data.getKeyPlayer() != null && data.getKeyPlayer() != null && participant._data.getKeyPlayer().getPlayersId() == data.getKeyPlayer().getPlayersId()))
			{
				return participant;
			}
		}
		return null;
	}

	public class Participant
	{
		private RegistrationData _data;
		private boolean _wonMatch;
		private int _numberOfWins;

		public Participant(RegistrationData _data)
		{
			this._data = _data;
			_wonMatch = false;
			_numberOfWins = 0;
		}

		// returns false if players disconnected, etc
		public boolean canFight()
		{
			return _event != null && _event.canFight(_data);
		}

		//TODO call this before starting a match
		public void notifyNewMatch()
		{
			_wonMatch = false;
		}

		public void setWonMatch()
		{
			_wonMatch = true;
			_numberOfWins ++;
		}
	}
}
