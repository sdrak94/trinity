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

import org.strixplatform.StrixPlatform;
import org.strixplatform.utils.StrixClientData;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public final class KeyPacket extends L2GameServerPacket
{
	private static final String _S__01_KEYPACKET = "[S] 2e KeyPacket";

	private byte[] _key;
	private int _id;
	private final StrixClientData clientData;

	public KeyPacket(final byte[] key, final int id)
	{
		this._key = key;
		this._id = id;
		this.clientData = null;
	}
	
    public KeyPacket(final byte[] key, final StrixClientData clientData)
    {
        this._key = key;
        this.clientData = clientData;
    }

	@Override
	public void writeImpl()
	{
		writeC(0x2e);
		writeC(_id); //0 - wrong protocol, 1 - protocol ok
		for (int i = 0; i < 8; i++)
		{
			writeC(_key[i]); // key
	        if(StrixPlatform.getInstance().isBackNotificationEnabled() && clientData != null)
	        {
	             writeC(clientData.getServerResponse().ordinal() + 1);
	                //body.writeQ(); Resolved to send ban time expire.
	        }
		}
		writeD(0x01);
		writeD(0x01); // server id
		writeC(0x01);
		writeD(0x00); // obfuscation key
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__01_KEYPACKET;
	}

}
