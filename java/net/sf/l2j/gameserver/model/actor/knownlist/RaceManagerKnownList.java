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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.MonsterRace;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaceManagerInstance;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
    protected static final Logger _log = Logger.getLogger(RaceManagerKnownList.class.getName());
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public RaceManagerKnownList(L2RaceManagerInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public boolean addKnownObject(L2Object object, boolean force)
    {
        if (!super.addKnownObject(object, force)) return false;

        /* DONT KNOW WHY WE NEED THIS WHEN RACE MANAGER HAS A METHOD THAT BROADCAST TO ITS KNOW PLAYERS
        if (object instanceof L2PcInstance) {
            if (packet != null)
                ((L2PcInstance) object).sendPacket(packet);
        }
        */

        return true;
    }
    
    @Override
	public boolean addKnownObject(L2Object object)
    {
        return addKnownObject(object, false);
    }

    @Override
	public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object)) return false;

        if (object instanceof L2PcInstance)
        {
            //_log.info("Sending delete monsrac info.");
            DeleteObject obj = null;
            for (int i=0; i<8; i++)
            {
                obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
                ((L2PcInstance)object).sendPacket(obj);
            }
        }

        return true;
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public L2RaceManagerInstance getActiveChar() { return (L2RaceManagerInstance)super.getActiveChar(); }
}
