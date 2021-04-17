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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;


/**
 *  @author ShanSoft
 *  Packets Structure: chddd
 */
public final class RequestBookMarkSlotInfo extends L2GameClientPacket
{
	private static final String _C__51_REQUESTBOOKMARKSLOTINFO = "[C] 51 RequestBookMarkSlotInfo";

	@Override
	protected void readImpl()
	{
		//There is nothing to read.
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		player.sendPacket(new ExGetBookMarkInfoPacket(player));
	}


	@Override
	public String getType()
	{
		return _C__51_REQUESTBOOKMARKSLOTINFO;
	}
}
