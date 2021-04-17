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

public class ExAirShipInfo extends L2GameServerPacket
{
	
	private static final String _S__FE_60_EXAIRSHIPINFO = "[S] FE:60 ExAirShipInfo";
	
	private final int _x, _y, _z, _heading, _objectId, _speed1, _speed2;
	
	
	public ExAirShipInfo(L2AirShipInstance ship)
	{
		_x = ship.getX();
		_y = ship.getY();
		_z = ship.getZ();
		_heading = ship.getPosition().getHeading();
		_objectId = ship.getObjectId();
		_speed1 = ship.getSpeed1();
		_speed2 = ship.getSpeed2();
	}

	@Override
    protected void writeImpl()
    {
	    writeC(0xfe);
	    writeH(0x60);
	    
	    writeD(_objectId);
	    writeD(_x);
	    writeD(_y);
	    writeD(_z);
	    writeD(_heading);
	    
	    
	    writeD(0x00); // object id of player who control ship
	    writeD(_speed1);
	    writeD(_speed2);
	    
	    // clan airship related info
	    writeD(0x00); // owner object id?
	    writeD(0x00); // 366?
	    writeD(0x00); // 0
	    writeD(0x00); // 107:
	    writeD(0x00); // 348?
	    writeD(0x00); // 0?
	    writeD(0x00); // 105?
	    writeD(0x00); // current fuel
	    writeD(0x00); // max fuel
	}

	@Override
    public String getType()
    {
	    return _S__FE_60_EXAIRSHIPINFO;
    }
	
}