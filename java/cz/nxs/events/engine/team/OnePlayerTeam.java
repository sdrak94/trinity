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

import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class OnePlayerTeam extends EventTeam
{
	public OnePlayerTeam(RegistrationData regData, int teamId, String teamName)
	{
		super(regData, teamId, teamName);
	}

	public PlayerEventInfo getPlayer()
	{
		if(getPlayers().isEmpty())
			return null;
		return getPlayers().getFirst();
	}
	
	@Override
	protected int getTeamSize()
	{
		return 1;
	}
}
