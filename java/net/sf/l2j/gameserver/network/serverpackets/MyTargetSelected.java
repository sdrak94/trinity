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

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;

/**
 * format dh (objectid, color)<br>
 * color legend : usually the color equals the level difference to the selected target<br>
 * -xx -> -9 red<br>
 * -8 -> -6 light red<br>
 * -5 -> -3 yellow<br>
 * 2 -> 2 white<br>
 * 3 -> 5 green<br>
 * 6 -> 8 light blue<br>
 * 9 -> xx dark blue
 */



public class MyTargetSelected extends L2GameServerPacket
{
	public static int HIDE    = 1;
	public static int HIDE_EX = 2;
	public static int SHOW    = 3;
	public static int SHOW_EX = 4;
	private final int _objectId, _flags;
	
	/**
	 * @param objectId int objectId of the target
	 * @param color level difference, the color is calculated from that.
	 */
	public MyTargetSelected(final int objectId, final int flags)
	{
		_objectId = objectId;
		_flags = flags;
	}

	public MyTargetSelected(final L2Character activeChar, final L2Object target)
	{
		_objectId = target.getObjectId();
		
		int flags = HIDE;
		
		if (target instanceof L2Decoy)
			flags = SHOW;
		
		final var player = target.getActingPlayer();
		if (player != null && player.getClanId() > 0)
			flags = flags + 1;
		
		_flags = flags;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xB9); /** H5 **/
		writeD(_objectId);
		writeH(_flags); // target type
		writeD(0x00);
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}
}