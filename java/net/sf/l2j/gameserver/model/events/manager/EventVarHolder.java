package net.sf.l2j.gameserver.model.events.manager;

public class EventVarHolder
{
	int	runningEventId			= 0;
	// used for domination
	int	teamOneScore			= 0;
	int	teamTwoScore			= 0;
	int	teamOneInRangePlayers	= 0;
	int	teamTwoInRangePlayers	= 0;
	
	// domination assistant Start
	public int getTeamOneScore()
	{
		return teamOneScore;
	}
	
	public int getTeamTwoScore()
	{
		return teamTwoScore;
	}
	
	public int getTeamOneInRangePlayers()
	{
		return teamOneInRangePlayers;
	}
	
	public int getTeamTwoInRangePlayers()
	{
		return teamTwoInRangePlayers;
	}
	
	public void incRangePlayers(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneInRangePlayers++;
				break;
			case 2:
				teamTwoInRangePlayers++;
				break;
		}
	}
	
	public void clearRangePlayers(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneInRangePlayers = 0;
				break;
			case 2:
				teamTwoInRangePlayers = 0;
				break;
		}
	}
	
	public void incScore(int teamId)
	{
		switch (teamId)
		{
			case 1:
				teamOneScore++;
				break;
			case 2:
				teamTwoScore++;
				break;
		}
	}
	
	// domination assistant End
	public int getRunningEventId()
	{
		return runningEventId;
	}
	
	public void setRunningEventId(int id)
	{
		runningEventId = id;
	}
	
	public void clear()
	{
		teamOneScore = 0;
		teamTwoScore = 0;
		teamOneInRangePlayers = 0;
		teamTwoInRangePlayers = 0;
		runningEventId = 0;
	}
	
	public static EventVarHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventVarHolder _instance = new EventVarHolder();
	}
}
