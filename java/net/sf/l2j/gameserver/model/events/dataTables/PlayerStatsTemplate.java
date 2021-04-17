package net.sf.l2j.gameserver.model.events.dataTables;

public class PlayerStatsTemplate
{
	protected String	_event;
	protected int		_countPlayed;
	protected int		_wins;
	protected int		_loses;
	protected int		_kills;
	protected int		_deaths;
	protected int		_score;
	
	public PlayerStatsTemplate(String event, int countPlayed, int wins, int loses, int kills, int deaths, int score)
	{
		_event = event;
		_countPlayed = countPlayed;
		_wins = wins;
		_loses = loses;
		_kills = kills;
		_deaths = deaths;
		_score = score;
	}
	
	public String getEvent()
	{
		return _event;
	}
	
	public int getCountPlayed()
	{
		return _countPlayed;
	}
	
	public int getWins()
	{
		return _wins;
	}
	
	public int getLoses()
	{
		return _loses;
	}
	
	public int getKills()
	{
		return _kills;
	}
	
	public int getDeaths()
	{
		return _deaths;
	}
	
	public int getScore()
	{
		return _score;
	}
}
