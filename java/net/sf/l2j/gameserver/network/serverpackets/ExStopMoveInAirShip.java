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
/**
 * @authos kerberos
 *
 */
public class ExStopMoveInAirShip extends L2GameServerPacket
{
	private L2PcInstance _activeChar;
    private L2AirShipInstance _ship;


    public ExStopMoveInAirShip(L2PcInstance player, L2AirShipInstance ship)
    {
    	_activeChar = player;
    	_ship = ship;
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0xfe);
        writeH(0x6e);
        writeD(_activeChar.getObjectId());
        writeD(_ship.getObjectId());
        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
        writeD(_activeChar.getHeading());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return "[S] FE:6e ExStopMoveAirShip";
    }
}
