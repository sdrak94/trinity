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
package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.LastTeamStanding;

public class LastTeamStandingCommands implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"joinlts",
		"leavelts"
	};
	
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (!LastTeamStanding.getInstance().getState().equals(LastTeamStanding.State.REGISTER))
		{
			activeChar.sendMessage("LastTeamStanding Event Registration is currently closed.");
			return false;
		}
		if (command.equalsIgnoreCase("joinlts"))
        {
			LastTeamStanding.getInstance().register(activeChar);
        }
        else if (command.equalsIgnoreCase("leavelts"))
        {
        	LastTeamStanding.getInstance().unregister(activeChar);
        }
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}