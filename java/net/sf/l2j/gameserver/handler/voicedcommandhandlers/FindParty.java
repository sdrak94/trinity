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

import java.util.Collection;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 *
 *
 */

public class FindParty implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"findparty",
		"fp"
	};
	private static final String[] TRADE_WORDS = {""};
	
	/**
	 * 
	 * @param args 
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target, String args)
	{
		if ((command.equalsIgnoreCase("findparty") || command.equalsIgnoreCase("fp")))
		{
			if (activeChar.isInParty() && !activeChar.getParty().isLeader(activeChar))
			{
				activeChar.sendMessage("Not Leader");
				return false;
			}
			if (activeChar.isInJail())
			{
				activeChar.sendMessage("wtf, you are not here to make friends.");
				return false;
			}

			Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
			
			int currmembers = activeChar.getParty() != null ? activeChar.getParty().size() : 0;
			
			if ((args == null) || args.isEmpty())
			{
				{
					for (L2PcInstance player : pls);
					{
						if (!activeChar.isInParty() && !activeChar.isInOlympiadMode() && !activeChar.getVarB("findparty"))
						{
							activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), 3, activeChar.getName(), "	Type=1 	ID=" + activeChar.getObjectId() + " Color=0 	Underline=0 	Title=[PARTY]Free slots (" + currmembers + "/9) "));			
						}
					}
				}
			}
			else
			{
				for (String s : TRADE_WORDS)
				{
					if (args.contains(s))
					{
						activeChar.sendChatMessage(0, 3, "FINDPARTY", "Dont use party find command for trade!");
						return false;
					}
				}
				
				if (args.length() > 22)
				{
					args = args.substring(0, 22);
				}
				
				{
					for (@SuppressWarnings("unused") L2PcInstance player : pls);
					{
						if (!activeChar.isInParty() && !activeChar.isInOlympiadMode() && !activeChar.getVarB("findparty"))
						{
							activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), 3, activeChar.getName(), "	Type=1 	ID=" + activeChar.getObjectId() + " Color=0 	Underline=0 	Title=[PARTY]Free slots (" + currmembers + "/9)  for " + args + ""));		
						}
					}
				}
			}
			
			activeChar.setPartyFindValid(true);
		}
		return true;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
		// TODO Auto-generated method stub
		return false;
	}


	
}
