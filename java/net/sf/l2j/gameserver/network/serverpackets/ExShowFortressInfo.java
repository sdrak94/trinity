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

import java.util.List;

import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Fort;

/**
 * format: d (dSdd)
 * cnt:%d (fortressID:%d ownerName:%s, siegeState:%d, lastOwnedTime:%d)
 * 
 * @author  KenM
 */
public class ExShowFortressInfo extends L2GameServerPacket
{
    //private static final Logger _log = Logger.getLogger(ExShowFortressInfo.class.getName());
	
	/**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:15 ExShowFortressInfo";
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x15);
        List<Fort> forts = FortManager.getInstance().getForts();
        writeD(forts.size());
        for (Fort fort : forts)
        {
        	L2Clan clan = fort.getOwnerClan();
            writeD(fort.getFortId());
            if (clan != null)
            {
            	writeS(clan.getName());
            }
            else
            {
                writeS("");
            }
            
            if (fort.getSiege().getIsInProgress())
            {
                writeD(1);
            }
            else
            {
            	writeD(0);
            }
            
            // Time of possession
            writeD(fort.getOwnedTime());
        }
    }
    
}
