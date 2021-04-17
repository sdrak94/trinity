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

import net.sf.l2j.gameserver.GameTimeController;

public class ClientSetTime extends L2GameServerPacket
{
	private static final String _S__EC_CLIENTSETTIME = "[S] f2 ClientSetTime [dd]";
	public static final ClientSetTime STATIC_PACKET = new ClientSetTime();
	private int _time;

	public ClientSetTime(int time)
	{
		_time = time;
	}
	
	public ClientSetTime()
	{
		_time = GameTimeController.getInstance().getGameTime();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf2);
		writeD(_time); // time in client minutes
		writeD(6); //constant to match the server time( this determines the speed of the client clock)
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__EC_CLIENTSETTIME;
	}
}