/**
 *
 */
package cz.nxs.events.engine.mini.tournament;

import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 */
public class TournamentManager
{
	private static Tournament _activeTournament = null;

	// configurations
	private static int _minTeamsCount;
	private static int _maxTeamsCount;
	private static int _timeForRegistration;
	private static String _rewardsString;
	private static MiniEventManager _selectedEvent;

	// no instances
	public TournamentManager() {}

	public static void setTournamentEvent(PlayerEventInfo gm, MiniEventManager event)
	{
		_selectedEvent = event;
		gm.sendMessage("The event was successfully set to " + event.getEventType().getAltTitle());
	}

	public static void setTournamentTeamsCount(PlayerEventInfo gm, int teamsCount, boolean min)
	{
		gm.sendMessage("Suggested counts of teams are: 4, 8, 16, 32, 64 (the ideal count for a tournament); acceptable also are 6, 10, 12, 20, 24,.. but those matches will end up have one player/team skip his match, because he will have no oppononents to fight with.");

		if(teamsCount > 4 && teamsCount % 2 == 0)
		{
			if(min)
			{
				_minTeamsCount = teamsCount;
			}
			else
			{
				_maxTeamsCount = teamsCount;
			}

			gm.sendMessage("Successfully set.");
		}
		else
			gm.sendMessage("Could not set, wrong number. It has to be dividable by 2.");
	}

	public static void setTimeForRegistration(PlayerEventInfo gm, int time)
	{
		if(time >= 1)
		{
			_timeForRegistration = time * 60000;
		}
		else
			gm.sendMessage("Time has to be longer than 1 (=1 minute).");
	}

	public static void setRewardString(PlayerEventInfo gm, String rewards)
	{
		_rewardsString = rewards;

		gm.sendMessage("The rewards were set successfully.");
	}

	public static void startTournament(PlayerEventInfo gm)
	{
		if(_selectedEvent != null && !_selectedEvent.isTournamentActive())
		{
			Tournament tournament = new Tournament(_selectedEvent);

			tournament.setMaxCountToRegister(_maxTeamsCount);
			tournament.setMinCountToRegister(_minTeamsCount);
			tournament.setTimeForRegistration(_timeForRegistration);
			tournament.setRewardString(_rewardsString);

			tournament.openRegistration();

			_activeTournament = tournament;
		}
		else
		{
			gm.sendMessage("You must first select an event.");
		}
	}

	public static void onMatchEnd(MiniEventGame game, RegistrationData winner)
	{
		if(_activeTournament != null)
		{
			_activeTournament.onMatchEnd(game, winner);
		}
	}

	public static void stopTournament(PlayerEventInfo gm)
	{
		if(_activeTournament == null)
		{
			gm.sendMessage("There is no tournament active.");
		}
		else
		{
			if(_activeTournament.stopByGm(gm))
			{
				cleanTournament();
				gm.sendMessage("Tournament successfully stopped.");
			}
		}
	}

	public static void cleanTournament()
	{
		_selectedEvent = null;
		_maxTeamsCount = 0;
		_minTeamsCount = 0;
		_timeForRegistration = 0;
	}

	public static void skipRegistration(PlayerEventInfo gm)
	{
		if(_activeTournament != null)
		{
			if(_activeTournament.skipRegistration())
			{
				gm.sendMessage("Registration was skipped.");
			}
		}
	}

	public static void register(PlayerEventInfo player)
	{
		if(_activeTournament != null && _activeTournament.isOpenRegistration())
		{
			_activeTournament.register(player);
		}
	}

	public MiniEventManager getEvent()
	{
		return _selectedEvent;
	}
}
