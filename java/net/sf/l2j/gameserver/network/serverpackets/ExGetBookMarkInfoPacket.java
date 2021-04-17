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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TeleportBookmark;



/**
 *
 * @author ShanSoft
 * @Structure d dd (ddddSdS)
 */
public class ExGetBookMarkInfoPacket extends L2GameServerPacket
{
	private static final String _S__FE_84_EXGETBOOKMARKINFOPACKET = "[S] FE:84 ExGetBookMarkInfoPacket";
	
	private L2PcInstance player;
	public ExGetBookMarkInfoPacket(L2PcInstance cha)
	{
		player = cha;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x84);
		writeD(0x00); // Dummy
		writeD(player._bookmarkslot);
		writeD(player.tpbookmark.size());
      

		for (TeleportBookmark tpbm : player.tpbookmark) 
		{
			writeD(tpbm._id);
			writeD(tpbm._x);
			writeD(tpbm._y);
			writeD(tpbm._z);
			writeS(tpbm._name);
			writeD(tpbm._icon);
			writeS(tpbm._tag);
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_84_EXGETBOOKMARKINFOPACKET;
	}
}
