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
package net.sf.l2j.loginserver.loginserverpackets;

import net.sf.l2j.loginserver.L2LoginServer;
import net.sf.l2j.loginserver.serverpackets.ServerBasePacket;

/**
 * @author -Wooden-
 *
 */
public class InitLS extends ServerBasePacket
{
	// ID 0x00
	// format
	// d proto rev
	// d key size
	// b key

    public InitLS(byte[] publickey)
    {
    	writeC(0x00);
    	writeD(L2LoginServer.PROTOCOL_REV);
    	writeD(publickey.length);
    	writeB(publickey);
    }

	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}
