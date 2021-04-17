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

import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) ddd
 * @author  -Wooden-
 */
public final class RequestDuelAnswerStart extends L2GameClientPacket
{
private static final String _C__D0_28_REQUESTDUELANSWERSTART = "[C] D0:28 RequestDuelAnswerStart";
private int _partyDuel;
@SuppressWarnings("unused")
private int _unk1;
private int _response;

@Override
protected void readImpl()
{
	_partyDuel = readD();
	_unk1 = readD();
	_response = readD();
}

/**
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
 */
@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null) return;
	
	L2PcInstance requestor = player.getActiveRequester();
	if (requestor == null) return;
	
	if (_response == 1)
	{
		SystemMessage msg1 = null, msg2 = null;
		if (requestor.isInDuel())
		{
			msg1 = new SystemMessage(SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL);
			msg1.addString(requestor.getDisplayName());
			player.sendPacket(msg1);
			return;
		}
		else if (player.isInDuel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME));
			return;
		}
		else if (player.getLevel() < 83)
		{
			player.sendMessage("You must be level 83 or higher to duel");
			return;
		}
		else if (requestor.getLevel() < 83)
		{
			player.sendMessage("Target must be level 83 or higher to duel");
			return;
		}
		
		if (_partyDuel == 1)
		{
			msg1 = new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_C1_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
			msg1.addString(requestor.getDisplayName());
			
			msg2 = new SystemMessage(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
			msg2.addString(player.getDisplayName());
		}
		else
		{
			msg1 = new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_C1_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
			msg1.addString(requestor.getDisplayName());
			
			msg2 = new SystemMessage(SystemMessageId.C1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
			msg2.addString(player.getDisplayName());
		}
		
		player.sendPacket(msg1);
		requestor.sendPacket(msg2);
		
		DuelManager.getInstance().addDuel(requestor, player, _partyDuel);
	}
	else
	{
		SystemMessage msg = null;
		if (_partyDuel == 1) msg = new SystemMessage(SystemMessageId.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
		else
		{
			msg = new SystemMessage(SystemMessageId.C1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
			msg.addString(player.getDisplayName());
		}
		requestor.sendPacket(msg);
	}
	
	player.setActiveRequester(null);
}

/**
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _C__D0_28_REQUESTDUELANSWERSTART;
}

}
