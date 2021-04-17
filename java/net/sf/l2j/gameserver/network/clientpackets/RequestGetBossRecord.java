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

import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBossRecord;
/**
 * Format: (ch) d
 * @author  -Wooden-
 *
 */
public class RequestGetBossRecord extends L2GameClientPacket
{
    protected static final Logger _log = Logger.getLogger(RequestGetBossRecord.class.getName());
    private static final String _C__D0_18_REQUESTGETBOSSRECORD = "[C] D0:18 RequestGetBossRecord";
    private int _bossId;

    @Override
	protected void readImpl()
    {
        _bossId = readD();
    }

    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
    	L2PcInstance activeChar = getClient().getActiveChar();
    	if(activeChar == null)
    		return;

        if (_bossId != 0)
        {
            _log.info("C5: RequestGetBossRecord: d: "+_bossId+" ActiveChar: "+activeChar); // should be always 0, log it if isnt 0 for furture research
        }

        int points = RaidBossPointsManager.getPointsByOwnerId(activeChar.getObjectId());
        int ranking = RaidBossPointsManager.calculateRanking(activeChar.getObjectId());
        
        Map<Integer, Integer> list = RaidBossPointsManager.getList(activeChar);

        // trigger packet
       	activeChar.sendPacket(new ExGetBossRecord(ranking, points, list));
    }

    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_18_REQUESTGETBOSSRECORD;
    }

}