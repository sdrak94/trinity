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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.18.2.7.2.9 $ $Date: 2005/03/27 15:29:30 $
 */
public final class UseItem extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private static Logger		_log			= Logger.getLogger(UseItem.class.getName());
	private static final String	_C__14_USEITEM	= "[C] 14 UseItem";
	private int					_objectId;
	private boolean _ctrlPressed = false;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_ctrlPressed = readD() != 0;
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (activeChar.getInventory().getItemByObjectId(_objectId).getItemId() == 5283 || activeChar.getInventory().getItemByObjectId(_objectId).getItemId() == 1539 || activeChar.getInventory().getItemByObjectId(_objectId).getItemId() == 5592)
		{
			if(_ctrlPressed)
			{
				_ctrlPressed = true; 
			}
		}
		else
		{
			_ctrlPressed = false; 
		}
		activeChar.useItem(_objectId, _ctrlPressed);
	}
	
	@Override
	public String getType()
	{
		return _C__14_USEITEM;
	}
}
