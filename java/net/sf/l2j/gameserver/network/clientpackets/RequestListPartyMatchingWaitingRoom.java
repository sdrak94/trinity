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

/**
 * Format: (ch)
 * this is just a trigger : no data
 * @author  -Wooden-
 *
 */
public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
    protected static final Logger _log = Logger.getLogger(RequestListPartyMatchingWaitingRoom.class.getName());
    private static final String _C__D0_16_REQUESTLISTPARTYMATCHINGWAITINGROOM = "[C] D0:16 RequestListPartyMatchingWaitingRoom";

    @Override
	protected void readImpl()
    {
    	// trigger
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
	protected void runImpl()
    {
        // TODO
        _log.info("C5: RequestListPartyMatchingWaitingRoom");
    }

    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_16_REQUESTLISTPARTYMATCHINGWAITINGROOM;
    }

}