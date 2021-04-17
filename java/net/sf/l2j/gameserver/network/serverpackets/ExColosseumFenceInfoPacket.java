/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceNexusInstance;

/**
 * OP: 0xFE<br>
 * OP2: 0x0003<br>
 * Format: ddddddd<br>
 * - d: object id<br>
 * - d: state(0=hidden, 1=unconnected corners, 2=connected corners)<br>
 * - d: x<br>
 * - d: y<br>
 * - d: z<br>
 * - d: a side length<br>
 * - d: b side length<br>
 */
public class ExColosseumFenceInfoPacket extends L2GameServerPacket
{
	private final L2FenceInstance _fence;
	private final L2FenceNexusInstance _fence2;
	
	public ExColosseumFenceInfoPacket(L2FenceInstance fence)
	{
		_fence = fence;
		_fence2 = null;
	}

	public ExColosseumFenceInfoPacket(L2FenceNexusInstance fence2)
	{
		_fence = null;
		_fence2 = fence2;
	}
	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		if(_fence != null)
		{
			writeC(0xfe);
			writeH(0x0003);
			writeD(_fence.getObjectId());
			writeD(_fence.getState());
			writeD(_fence.getX());
			writeD(_fence.getY());
			writeD(_fence.getZ());
			writeD(_fence.getWidth());
			writeD(_fence.getLength());
		}
		else if (_fence2 != null)
		{
			writeC(0xfe);
			writeH(0x03);
			writeD(_fence2.getObjectId());
			writeD(_fence2.getType());
			writeD(_fence2.getX());
			writeD(_fence2.getY());
			writeD(_fence2.getZ());
			writeD(_fence2.getWidth());
			writeD(_fence2.getLength());
		}
	}
	
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}
}