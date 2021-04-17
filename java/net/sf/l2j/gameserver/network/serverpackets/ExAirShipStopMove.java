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
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ExAirShipStopMove extends L2GameServerPacket
{
	
	private static final String _S__FE_66_EXAIRSHIPSTOPMOVE = "[S] FE:66 ExAirShipStopMove";

	private final int _playerId, _airShipId, _x, _y, _z;
	
	public ExAirShipStopMove (L2PcInstance player, L2AirShipInstance ship, int x, int y, int z)
	{
		_playerId = player.getObjectId();
		_airShipId = ship.getObjectId();
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
    protected void writeImpl()
    {
	    writeC(0xfe);
	    writeH(0x66);
	    
	    writeD(_airShipId);
	    writeD(_playerId);	    
	    writeD(_x);
	    writeD(_y);
	    writeD(_z);
	}

	@Override
    public String getType()
    {
	    return _S__FE_66_EXAIRSHIPSTOPMOVE;
    }
}