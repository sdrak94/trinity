package cz.nxs.events.engine.base;

import cz.nxs.events.EventGame;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 * stores data related to one player and event he's in
 */
public class EventPlayerData
{
	private final PlayerEventInfo _owner;

	protected GlobalStatsModel _globalStats;
	
	private int _score;
	
	public EventPlayerData(PlayerEventInfo owner, EventGame event, GlobalStatsModel stats)
	{
		_owner = owner;
		_globalStats = stats;
	}
	
	public PlayerEventInfo getOwner()
	{
		return _owner;
	}
	
	// Score
	public int getScore() 
	{ 
		return _score; 
	}
	
	public int raiseScore(int i) 
	{ 
		_score += i;
		_globalStats.raise(GlobalStatType.SCORE, i);
		return _score;
	}
	
	public void setScore(int i)
	{
		_score = i;
		_globalStats.set(GlobalStatType.SCORE, i);
	}
	
	public GlobalStatsModel getGlobalStats()
	{
		return _globalStats;
	}
}
