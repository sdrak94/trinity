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
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.StopMoveInVehicle;
import net.sf.l2j.util.Point3D;

/**
 * @author Maktakien
 *
 */
public final class CannotMoveAnymoreInVehicle extends L2GameClientPacket
{
private int _x;
private int _y;
private int _z;
private int _heading;
private int _boatId;

@Override
protected void readImpl()
{
	_boatId = readD();
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
	{
		return;
	}
	if(player.isInBoat())
	{
		if(player.getBoat().getObjectId() == _boatId)
		{
			if (player.isOutOfControl())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			player.setInBoatPosition(new Point3D(_x,_y,_z));
			player.getPosition().setHeading(_heading);
			StopMoveInVehicle msg = new StopMoveInVehicle(player,_boatId);
			player.broadcastPacket(msg);
		}
	}
}

@Override
public String getType()
{
	return "[C] 5D CannotMoveAnymoreInVehicle";
}

}
