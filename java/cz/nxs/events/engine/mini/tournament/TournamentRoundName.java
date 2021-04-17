package cz.nxs.events.engine.mini.tournament;

/**
 * Created by Lukas on 25.2.14.
 */
public enum TournamentRoundName
{
	Quarterfinals(8),
	Semifinals(4),
	Finals(2)
	;

	private final int _sizeTeam;
	TournamentRoundName(int size)
	{
		_sizeTeam = size;
	}

	public static String getRoundName(int countOfTeams)
	{
		for(TournamentRoundName tournamentRoundName : values())
		{
			if(tournamentRoundName._sizeTeam == countOfTeams)
				return tournamentRoundName.toString();
		}

		return ("The next round");
	}
}
