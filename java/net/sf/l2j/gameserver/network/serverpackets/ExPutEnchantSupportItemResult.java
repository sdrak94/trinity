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
 *
 * @author nBd
 */
public class ExPutEnchantSupportItemResult extends L2GameServerPacket
{
	private static final String _S__82_EXPUTENCHANTSUPPORTITEMRESULT = "[S] 82 ExPutEnchantSupportItemResult";
	
	private int _result;

	/**
	 * 
	 */
	public ExPutEnchantSupportItemResult(int result)
	{
		_result = result;
	}
	
	/**
	 * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__82_EXPUTENCHANTSUPPORTITEMRESULT;
	}
	
	/**
	 * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x82);
		writeD(_result);
	}
}
