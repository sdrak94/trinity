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

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Object;

/**
 *
 * @author  KenM
 */
public final class ExShowTrace extends L2GameServerPacket
{
    private final List<Trace> _traces = new FastList<Trace>();
    
    public void addTrace(int x, int y, int z, int time)
    {
        _traces.add(new Trace(x, y, z, time));
    }
    
    public void addTrace(L2Object obj, int time)
    {
        this.addTrace(obj.getX(), obj.getY(), obj.getZ(), time);
    }
    
    static final class Trace
    {
        public final int _x;
        public final int _y;
        public final int _z;
        public final int _time;
        
        public Trace(int x, int y, int z, int time)
        {
            _x = x;
            _y = y;
            _z = z;
            _time = time;
        }
    }
    
    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:67 ExShowTrace";
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x67);
        
        writeH(_traces.size());
        for (Trace t : _traces)
        {
            writeD(t._x);
            writeD(t._y);
            writeD(t._z);
            writeH(t._time);
        }
    }
    
}
