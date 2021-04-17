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


/**
 * @author zabbix
 * Lets drink to code!
 */
public final class DummyPacket extends L2GameClientPacket
{
	//private static Logger _log = Logger.getLogger(DummyPacket.class.getName());

	@Override
	protected void readImpl()
	{

	}

	@Override
	public void runImpl()
	{

	}

	@Override
	public String getType()
	{
		return "DummyPacket";
	}
}
