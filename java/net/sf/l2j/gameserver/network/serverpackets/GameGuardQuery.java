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
 * @author zabbix
 * Lets drink to code!
 */
public class GameGuardQuery extends L2GameServerPacket
{
    private static final String _S__F9_GAMEGUARDQUERY = "[S] 74 GameGuardQuery";

    public GameGuardQuery()
    {
        
    }

    @Override
	public void runImpl()
    {
        
    }

    @Override
	public void writeImpl()
    {
        writeC(0x74);
        writeD(0x27533DD9);
        writeD(0x2E72A51D);
        writeD(0x2017038B);
        writeD(0xC35B1EA3);
    }

    @Override
	public String getType()
    {
        return _S__F9_GAMEGUARDQUERY;
    }
}
