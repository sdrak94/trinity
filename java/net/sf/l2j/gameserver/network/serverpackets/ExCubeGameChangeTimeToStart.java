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
 * Format: (chd) d
 * d: seconds left
 * @author mrTJO
 *
 */
public class ExCubeGameChangeTimeToStart extends L2GameServerPacket
{
	private static final String _S__FE_97_03_EXCUBEGAMECHANGETIMETOSTART = "[S] FE:97:03 ExCubeGameChangeTimeToStart";
	int _seconds;
	
	public ExCubeGameChangeTimeToStart(int seconds)
	{
		_seconds = seconds;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x97);
		writeD(0x03);
		
		writeD(_seconds);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_97_03_EXCUBEGAMECHANGETIMETOSTART;
	}

}