/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.nxs.events.engine.team;

import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.PartyData;
import javolution.util.FastList;

/**
 * @author hNoke
 * data about one team in Event
 */
public class EventTeam
{
	private RegistrationData _regData;

	protected int _teamId;
	private String _teamName;
	private String _fullName;
	
	private FastList<PlayerEventInfo> _players;
	private int _levelSum, _averageLevel;
	
	public int _nameColor;

	private int _kills;
	private int _deaths;
	private int _score;
	
	private int _finalPosition;
	
	public EventTeam(int teamId)
	{
		_teamId = teamId;
	
		initializePlayers();
	}
	
	public EventTeam(RegistrationData regData, int teamId, String teamName, String fullName)
	{
		_regData = regData;

		_teamId = teamId;
		_teamName = teamName;
		_fullName = fullName;
		
		_nameColor = EventManager.getInstance().getTeamColorForName(teamId);
		
		_levelSum = 0;
		_averageLevel = 0;
		
		_kills = 0;
		_deaths = 0;
		_score = 0;
		
		_finalPosition = -1;
		
		initializePlayers();
	}
	
	public EventTeam(RegistrationData regData, int teamId, String teamName)
	{
		this(regData, teamId, teamName, teamName);
	}
	
	protected void initializePlayers()
	{
		if(getTeamSize() > 0)
			_players = new FastList<PlayerEventInfo>(getTeamSize());
		else
			_players = new FastList<PlayerEventInfo>();
	}
	
	public synchronized boolean removePlayer(PlayerEventInfo pi)
	{
		return _players.remove(pi);
	}
	
	public void addPlayer(PlayerEventInfo pi, boolean init)
	{
		synchronized(_players)
		{
			_players.add(pi);
		}
		
		if(init)
			initPlayer(pi);
		
		_levelSum += pi.getLevel();
	}
	
	public void calcAverageLevel()
	{
		_averageLevel = (int) (_levelSum / (double) _players.size());
	}
	
	public int getAverageLevel()
	{
		return _averageLevel;
	}
	
	protected void initPlayer(PlayerEventInfo pi)
	{
		pi.setEventTeam(this); 
		pi.setNameColor(getNameColor());
		
		pi.broadcastUserInfo();
	}
	
	public void message(String msg, String name, boolean special)
	{
		for(PlayerEventInfo pi : _players)
		{
			pi.screenMessage(msg, name, special);
		}
	}
	
	public void createParties()
	{
		int count = 0;
		int size = getPlayers().size();
		PartyData party = null;
		
		if(size <= 1)
			return;
		
		for(PlayerEventInfo player : getPlayers())
		{
			if(player == null)
				size --;
		}

		if(size > 1)
		{
			for (PlayerEventInfo player : getPlayers())
			{
				if (count % 9 == 0 && size - count != 1)
					party = new PartyData(player);
				
				else if (count % 9 < 9)
					party.addPartyMember(player);
				
				count++;
			}
		}
	}
	
	public FastList<PlayerEventInfo> getPlayers()
	{
		return _players;
	}
	
	public int getTeamId()
	{
		return _teamId;
	}

	public int getDeaths()
	{
		return _deaths;
	}

	public int getKills()
	{
		return _kills;
	}
	
	public int getScore()
	{
		return _score;
	}
	
	public void raiseScore(int count)
	{
		_score += count;
	}

	public int getNameColor()
	{
		return _nameColor;
	}

	public String getTeamName()
	{
		return _teamName;
	}
	
	public String getFullName()
	{
		return _fullName;
	}
	
	public void raiseDeaths(int count)
	{
		_deaths += count;
	}

	public void raiseKills(int count)
	{
		_kills += count;
	}
	
	public void resetDeaths()
	{
		_deaths = 0;
	}
	
	public void resetScore()
	{
		_score = 0;
	}

	protected int getTeamSize()
	{
		return -1; // = infinity
	}
	
	// used for formating in HTMLs
	public String getNameColorInString()
	{
		return EventManager.getInstance().getTeamColorForHtml(_teamId);
	}
	
	
	public void setFinalPosition(int pos)
	{
		_finalPosition = pos;
	}
	
	public int getFinalPosition()
	{
		return _finalPosition;
	}

	public RegistrationData getRegData()
	{
		return _regData;
	}
}
