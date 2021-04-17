package cz.nxs.events.engine.base;

import cz.nxs.events.EventGame;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 * EventPlayerData implementation for PvP/PvE events
 */
public class PvPEventPlayerData extends EventPlayerData
{
	private int _kills = 0;
	private int _deaths = 0;
	private int _spree = 0;
	
	public PvPEventPlayerData(PlayerEventInfo owner, EventGame event, GlobalStatsModel stats)
	{
		super(owner, event, stats);
	}
	
	// Kills
	public int getKills() 
	{ 
		return _kills; 
	}
	
	public int raiseKills(int i) 
	{ 
		_kills += i;
		_globalStats.raise(GlobalStatType.KILLS, i);
		return _kills;
	}
	
	public void setKills(int i)
	{
		_kills = i;
		_globalStats.set(GlobalStatType.KILLS, i);
	}
	
	// Deaths
	public int getDeaths() 
	{ 
		return _deaths; 
	}
	
	public int raiseDeaths(int i) 
	{ 
		_deaths += i;
		_globalStats.raise(GlobalStatType.DEATHS, i);
		return _deaths;
	}
	
	public void setDeaths(int i)
	{
		_deaths = i;
		_globalStats.set(GlobalStatType.DEATHS, i);
	}
	
	// Spree
	public int getSpree() 
	{ 
		return _spree; 
	}
	
	public int raiseSpree(int i) 
	{ 
		_spree += i;
		return _spree;
	}
	
	public void setSpree(int i)
	{
		_spree = i;
	}
}
