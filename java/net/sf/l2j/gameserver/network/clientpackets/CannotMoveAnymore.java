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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.PartyMemberPosition;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class CannotMoveAnymore extends L2GameClientPacket
{
private static final String _C__36_STOPMOVE = "[C] 36 CannotMoveAnymore";

private static Logger _log = Logger.getLogger(CannotMoveAnymore.class.getName());

private int _x;
private int _y;
private int _z;
private int _heading;

@Override
protected void readImpl()
{
	_x = readD();
	_y = readD();
	_z = readD();
	_heading = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if (Config.DEBUG)
		_log.fine("client: x:" + _x + " y:" + _y + " z:" + _z
				+ " server x:" + player.getX() + " y:" + player.getY()
				+ " z:" + player.getZ());
	
	if (player.isOutOfControl())
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (player.getAI() != null)
	{
		player.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_BLOCKED, new L2CharPosition(_x, _y, _z, _heading));
	}
	if (player.getParty() != null)
	{
		player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
	}
	
	// player.stopMove();
	//
	// if (Config.DEBUG)
	// _log.fine("client: x:"+_x+" y:"+_y+" z:"+_z+
	// " server x:"+player.getX()+" y:"+player.getZ()+" z:"+player.getZ());
	// StopMove smwl = new StopMove(player);
	// getClient().getActiveChar().sendPacket(smwl);
	// getClient().getActiveChar().broadcastPacket(smwl);
	//
	// StopRotation sr = new StopRotation(getClient().getActiveChar(),
	// _heading);
	// getClient().getActiveChar().sendPacket(sr);
	// getClient().getActiveChar().broadcastPacket(sr);
}

/*
 * (non-Javadoc)
 *
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__36_STOPMOVE;
}
}
