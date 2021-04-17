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

import java.util.Map;

import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class KoreanTeam extends EventTeam
{
	private Map<Integer, Integer> _players;

	private int _order;
	private int _nextPlayer;
	
	private PlayerEventInfo _fighting;
	
	public KoreanTeam(RegistrationData regData, int teamId, String teamName)
	{
		super(regData, teamId, teamName);
		
		_players = new FastMap<Integer, Integer>(getTeamSize());
		_order = 0;
		_nextPlayer = 0;
		_fighting = null;
	}
	
	public boolean isFighting(PlayerEventInfo player)
	{
		return _fighting != null && _fighting.getPlayersId() == player.getPlayersId();
	}
	
	@Override
	protected int getTeamSize()
	{
		return 4;
	}
	
	@Override
	public void addPlayer(PlayerEventInfo pi, boolean init)
	{
		super.addPlayer(pi, init);
		
		_order ++;
		_players.put(_order, pi.getPlayersId());
	}
	
	public PlayerEventInfo getNextPlayer()
	{
		if(getPlayers().isEmpty())
			return null;
		
		int next = 0;
		
		do
		{
			_nextPlayer++;
			next = _players.get(_nextPlayer);
		}  while(next == 0);
		
		for(PlayerEventInfo pi : getPlayers())
		{
			if(pi.getPlayersId() == next)
			{
				_fighting = pi;
				return pi;
			}
		}
		
		return null;
	}
}
