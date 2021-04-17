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

/**
 *
 * @author  KenM
 */
public final class ExBasicActionList extends L2GameServerPacket
{
    private static final String _S__FE_5E_EXBASICACTIONLIST = "[S] FE:5F ExBasicActionList";
    
    private final int[] _actionIds;
    
    public static final ExBasicActionList DEFAULT_ACTION_LIST = new ExBasicActionList();
    
    private ExBasicActionList()
    {
        this(ExBasicActionList.getDefaultActionList());
    }
    
    public static int[] getDefaultActionList()
    {
        int count1 = 71; // 0 <-> (count1 - 1)
        int count2 = 89; // 1000 <-> (1000 + count2 - 1) //Update by rocknow
        int[] actionIds = new int[count1 + count2];
        
        int index = 0;
        for (int i = 0; i < count1; i++)
        {
            actionIds[index++] = i;
        }
        for (int i = 0; i < count2; i++)
        {
            actionIds[index++] = 1000 + i;
        }
        return actionIds;
    }
    
    public ExBasicActionList(int... actionIds)
    {
        _actionIds = actionIds;
    }
    
    
    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return _S__FE_5E_EXBASICACTIONLIST;
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x5f);
        writeD(_actionIds.length);
        for (int i = 0; i < _actionIds.length; i++)
        {
            writeD(_actionIds[i]);
        }
    }
    
}
