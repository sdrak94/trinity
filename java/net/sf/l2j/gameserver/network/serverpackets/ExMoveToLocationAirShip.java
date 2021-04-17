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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2AirShipInstance;

public class ExMoveToLocationAirShip extends L2GameServerPacket
{
	
	private static final String _S__FE_65_EXAIRSHIPMOVETOLOCATION = "[S] FE:65 ExGetOnAirShip";

	private final int _airShipId;
	private final int _x, _y, _z, _dx, _dy, _dz;
	
	public ExMoveToLocationAirShip(L2AirShipInstance cha, int x, int y, int z)
	{
		_airShipId = cha.getObjectId();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_dx = x;
		_dy = y;
		_dz = z;
	}
	
	@Override
    protected void writeImpl()
    {
	    writeC(0xfe);
	    writeH(0x65);
	    
	    writeD(_airShipId);
	    writeD(_dx);
	    writeD(_dy);
	    writeD(_dz);
	    writeD(_x);
	    writeD(_y);
	    writeD(_z);
	}
	
	@Override
    public String getType()
    {
	    return _S__FE_65_EXAIRSHIPMOVETOLOCATION;
    }
	
}