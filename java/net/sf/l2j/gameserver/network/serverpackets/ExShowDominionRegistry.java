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

import java.util.Calendar;

/**
 * format: dSSSdddddddd (dd(d))
 * @author  GodKratos
 */
public class ExShowDominionRegistry extends L2GameServerPacket
{
	private int _territoryId;
	private int _clanReq = 0x00;
	private int _mercReq = 0x00;
	private int _warTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
	private int _currentTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
	
	public ExShowDominionRegistry(int terrId)
	{
		_territoryId = terrId;
	}
	
	/**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:90 ExShowDominionRegistry";
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x90);
        writeD(_territoryId); // Current Territory Id
        writeS("No Clan");    // Owners Clan
        writeS("No Owner");   // Owner Clan Leader
        writeS("No Ally");    // Owner Alliance
        writeD(_clanReq); // Clan Request
        writeD(_mercReq); // Merc Request
        writeD(_warTime); // War Time
        writeD(_currentTime); // Current Time
        writeD(0x00); // unknown
        writeD(0x00); // unknown
        writeD(0x01); // unknown
        writeD(0x09); // Territory Count
        for (int i = 0; i < 9; i++)
        {
        	writeD(0x51 + i); // Territory Id
        	writeD(0x01);     // Emblem Count
        	writeD(0x51 + i); // Emblem ID - should be in for loop for emblem count
        }
    }
    
}
