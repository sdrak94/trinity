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
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestDismissAlly extends L2GameClientPacket
{
private static final String _C__86_REQUESTDISMISSALLY = "[C] 86 RequestDismissAlly";
//static Logger _log = Logger.getLogger(RequestDismissAlly.class.getName());

@Override
protected void readImpl()
{
	// trigger packet
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
	{
		return;
	}
	if (!activeChar.isClanLeader())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
		return;
	}
	
	if (activeChar.isAccountLockedDown())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	activeChar.getClan().dissolveAlly(activeChar);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__86_REQUESTDISMISSALLY;
}
}
