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

package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExColosseumFenceInfoPacket;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;

/**
 * @author KKnD
 */
public final class L2FenceNexusInstance extends L2Object
{
private int _type;
private int _width;
private int _length;
private int _xLoc, _yLoc, _zLoc, _mapId;

public L2FenceNexusInstance(int objectId, int type, int width, int length, int x, int y, int z, int eventId)
{
	super(objectId);
	_type = type;
	_width = width;
	_length = length;
	_xLoc = x;
	_yLoc = y;
	_zLoc = z;
	_mapId = eventId;
}

@Override
public void sendInfo(L2PcInstance activeChar)
{
	activeChar.sendPacket(new ExColosseumFenceInfoPacket(this));
}

public int getXLoc()
{
	return _xLoc;
}

public int getYLoc()
{
	return _yLoc;
}

public int getZLoc()
{
	return _zLoc;
}

public int getType()
{
	return _type;
}

public int getMapId()
{
	return _mapId;
}

public int getWidth()
{
	return _width;
}

public int getLength()
{
	return _length;
}

@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return false;
}

@Override
public void onActionShift(L2GameClient client)
{
	L2PcInstance player = client.getActiveChar();
	if (player == null)
		return;
	
	if (player.getAccessLevel().isGm())
	{
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		player.sendPacket(my);
		
		player.sendMessage("ObjectId of targeted fence: " + getObjectId());
	}
	else
		player.sendPacket(ActionFailed.STATIC_PACKET);
}


}