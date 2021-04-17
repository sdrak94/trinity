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
package net.sf.l2j.loginserver.serverpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;

/**
 * Fromat: d
 * d: response
 */
public final class GGAuth extends L2LoginServerPacket
{
    static final Logger _log = Logger.getLogger(GGAuth.class.getName());
    public static final int SKIP_GG_AUTH_REQUEST = 0x0b;

    private int _response;

    public GGAuth(int response)
    {
    	_response = response;
        if (Config.DEBUG)
        {
            _log.warning("Reason Hex: "+(Integer.toHexString(response)));
        }
    }

	/**
	 * @see com.l2jserver.mmocore.network.SendablePacket#write()
	 */
	@Override
	protected void write()
	{
		writeC(0x0b);
        writeD(_response);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
        writeD(0x00);
	}
}

