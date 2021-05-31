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
 * This class ...
 *
 * @version $Revision: 1.7.4.4 $ $Date: 2005/03/27 18:46:19 $
 */
public final class ObserverReturn extends L2GameClientPacket
{
	private static final String OBSRETURN__C__04 = "[C] b8 ObserverReturn";
	//private static Logger _log = Logger.getLogger(Action.class.getName());

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		else if (activeChar.inObserverMode()) 
			activeChar.leaveObserverMode();
		//activeChar.teleToLocation(activeChar.getObsX(), activeChar.getObsY(), activeChar.getObsZ());
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return OBSRETURN__C__04;
	}
}