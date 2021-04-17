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


/**
 *  @author ShanSoft
 *  @structure chdSdS
 */
public final class RequestSaveBookMarkSlot extends L2GameClientPacket
{
	private static final String _C__51_REQUESTSAVEBOOKMARKSLOT = "[C] 51 RequestSaveBookMarkSlot";
	
	private int icon;
	private String name,tag;

	@Override
	protected void readImpl()
	{
		readH();
		name = readS();
		icon = readD();
		tag = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		activeChar.TeleportBookmarkAdd(activeChar.getX(), activeChar.getY(), activeChar.getZ(), icon, tag, name);
	}


	@Override
	public String getType()
	{
		return _C__51_REQUESTSAVEBOOKMARKSLOT;
	}
}
