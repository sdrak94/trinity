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
package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * A chat handler
 *
 * @author  durgus
 */
public class ChatPartyRoomAll implements IChatHandler
{
private static final int[] COMMAND_IDS =
{
	16
};

/**
 * Handle chat type 'party room all'
 * @see net.sf.l2j.gameserver.handler.IChatHandler#handleChat(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
 */
public void handleChat(int type, L2PcInstance activeChar, String target, String text)
{
	if ((activeChar._inEventFOS && FOS._started) || (activeChar._inEventFOS && NewFOS._started))
	{
		if (activeChar.getFame() >= 2000)
		{
			if (!activeChar.getFloodProtectors().getPartyRoom().tryPerformAction("party room"))
			{
				activeChar.sendMessage("Wait 200 seconds");
				return;
			}
			
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
			
			for (L2PcInstance player : FOS._players)
			{
				if (player != null)
				{
					if (activeChar._teamNameFOS.equals(player._teamNameFOS))
						player.sendPacket(cs);
				}
			}
		}
		else
		{
			activeChar.sendMessage("You need 2000 fame");
		}
		
		return;
	}
	
	boolean did = true;
	
	if (activeChar.isInParty())
	{
		if (activeChar.getParty().isInCommandChannel() && activeChar.getParty().isLeader(activeChar))
		{
			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
			activeChar.getParty().getCommandChannel().broadcastCSToChannelMembers(cs, activeChar);
		}
		else did = false;
	}
	else did = false;
	
	if (!did && activeChar.isGM())
	{
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		Broadcast.toAllOnlinePlayers(cs);
	}
}

/**
 * Returns the chat types registered to this handler
 * @see net.sf.l2j.gameserver.handler.IChatHandler#getChatTypeList()
 */
public int[] getChatTypeList()
{
	return COMMAND_IDS;
}
}