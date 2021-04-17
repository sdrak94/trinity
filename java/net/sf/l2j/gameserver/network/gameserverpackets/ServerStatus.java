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
package net.sf.l2j.gameserver.network.gameserverpackets;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author -Wooden-
 *
 */
public class ServerStatus extends GameServerBasePacket
{
    private ArrayList<Attribute> _attributes;

    public static final String[] STATUS_STRING = {"Auto", "Good", "Normal", "Full", "Down", "Gm Only"};

    public static final int SERVER_LIST_STATUS = 0x01;
    public static final int SERVER_LIST_CLOCK = 0x02;
    public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
    public static final int MAX_PLAYERS = 0x04;
    public static final int TEST_SERVER = 0x05;

    public static final int STATUS_AUTO = 0x00;
    public static final int STATUS_GOOD = 0x01;
    public static final int STATUS_NORMAL = 0x02;
    public static final int STATUS_FULL = 0x03;
    public static final int STATUS_DOWN = 0x04;
    public static final int STATUS_GM_ONLY = 0x05;

    public static final int ON = 0x01;
    public static final int OFF = 0x00;

    class Attribute
    {
        public int id;
        public int value;

        Attribute(int pId, int pValue)
        {
            id = pId;
            value = pValue;
        }
    }

    public ServerStatus()
    {
        _attributes = new ArrayList<Attribute>();
    }

    public void addAttribute(int id, int value)
    {
        _attributes.add(new Attribute(id, value));
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.gameserverpackets.GameServerBasePacket#getContent()
     */
    @Override
    public byte[] getContent() throws IOException
    {
        writeC(0x06);
        writeD(_attributes.size());
        for (Attribute temp: _attributes)
        {
            writeD(temp.id);
            writeD(temp.value);
        }

        return getBytes();
    }

}