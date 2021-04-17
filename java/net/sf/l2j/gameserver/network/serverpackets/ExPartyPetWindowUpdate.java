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

import net.sf.l2j.gameserver.model.actor.L2Summon;

/**
 *
 * @author  KenM
 */
public class ExPartyPetWindowUpdate extends L2GameServerPacket
{
    private final L2Summon _summon;
    
    public ExPartyPetWindowUpdate(L2Summon summon)
    {
        _summon = summon;
    }
    
    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:19 ExPartyPetWindowUpdate";
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x19);
        writeD(_summon.getObjectId());
        writeD(_summon.getTemplate().idTemplate + 1000000);
        writeD(_summon.getSummonType());
        writeD(_summon.getOwner().getObjectId());
        writeS(_summon.getName());
        writeD((int) _summon.getCurrentHp());
        writeD(_summon.getMaxHp());
        writeD((int) _summon.getCurrentMp());
        writeD(_summon.getMaxMp());
        writeD(_summon.getLevel());
    }
    
}
