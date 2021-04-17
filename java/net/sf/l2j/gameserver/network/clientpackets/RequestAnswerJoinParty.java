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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.JoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 *  sample
 *  2a
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerJoinParty extends L2GameClientPacket
{
private static final String _C__2A_REQUESTANSWERPARTY = "[C] 2A RequestAnswerJoinParty";
//private static Logger _log = Logger.getLogger(RequestAnswerJoinParty.class.getName());

private int _response;

@Override
protected void readImpl()
{
	_response = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if(player != null)
	{
		L2PcInstance requestor = player.getActiveRequester();
		if (requestor == null)
			return;
		
		JoinParty join = new JoinParty(_response);
		requestor.sendPacket(join);
		
		if (_response == 1)
		{
			if (!player.isGM() && !requestor.isGM())
			{
				if (player.isInHuntersVillage() || requestor.isInHuntersVillage())
				{
					player.sendMessage("Cannot join parties when in Hunter's Village");
					requestor.sendMessage("Target is in Hunter's Village");
					return;
				}
				if (requestor.isInHuntersVillage())
				{
					player.sendMessage("Cannot join parties when in Hunter's Village");
					requestor.sendMessage("Target is in Hunter's Village");
					return;
				}
				if (player.isInOrcVillage() || requestor.isInOrcVillage())
				{
					player.sendMessage("Cannot join parties when in Hunter's Village");
					requestor.sendMessage("Target is in Hunter's Village");
					return;
				}
				if (requestor.isInOrcVillage())
				{
					player.sendMessage("Cannot join parties when in Hunter's Village");
					requestor.sendMessage("Target is in Hunter's Village");
					return;
				}
				if (DM._started && (requestor._inEventDM || player._inEventDM))
				{
					player.sendMessage("No parties in DM");
					requestor.sendMessage("No parties in DM");
					return;
				}
				if (NewDM._started && (requestor._inEventDM || player._inEventDM))
				{
					player.sendMessage("No parties in DM");
					requestor.sendMessage("No parties in DM");
					return;
				}
			}
			if (player.cannotMakePartyActions() || requestor.cannotMakePartyActions())
			{
				player.sendMessage("You have running instance where parties are not allowed");
				requestor.sendMessage("Target has a running instance where parties are not allowed");
				return;
			}
			if (requestor.getParty() != null)//Update by rocknow-Start
			{
				if (requestor.getParty().getMemberCount() >= 10)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
					requestor.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
					return;
				}
			}//Update by rocknow-End
			player.joinParty(requestor.getParty());
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.PLAYER_DECLINED);
			requestor.sendPacket(msg);
			msg = null;
			
			//activate garbage collection if there are no other members in party (happens when we were creating new one)
			if (requestor.getParty() != null && requestor.getParty().getMemberCount() == 1) requestor.setParty(null);
		}
		
		if (requestor.getParty() != null)
			requestor.getParty().onInviteAccept(); // if party is null, there is no need of decreasing
		
		player.setActiveRequester(null);
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__2A_REQUESTANSWERPARTY;
}
}
