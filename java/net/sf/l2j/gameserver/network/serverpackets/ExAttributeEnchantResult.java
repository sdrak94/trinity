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

public class ExAttributeEnchantResult extends L2GameServerPacket
{
	private static final String _S__FE_61_EXATTRIBUTEENCHANTRESULT = "[S] FE:61 ExAttributeEnchantResult [d]";
	
	private int _result;
	
	public ExAttributeEnchantResult (int result)
	{
		_result = result;
	}
	
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x61);
		
		writeD(_result);
	}
	
	public String getType()
	{
		return _S__FE_61_EXATTRIBUTEENCHANTRESULT;
	}
}
