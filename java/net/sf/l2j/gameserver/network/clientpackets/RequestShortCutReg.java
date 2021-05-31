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

import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestShortCutReg extends L2GameClientPacket
{
	private static final String _C__33_REQUESTSHORTCUTREG = "[C] 33 RequestShortCutReg";

	private int _type;
	private int _id;
	private int _slot;
	private int _page;
	private int _lvl;
	private int _characterType; // 1 - player, 2 - pet

	@Override
	protected void readImpl()
	{
		_type = readD();
		int slot = readD();
		_id = readD();
		_lvl = readD();
		_characterType = readD();

		_slot = slot % 12;
		_page = slot / 12;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		    return;

		if (_page > 12 || _page < 0)
			return;
		
		boolean saveToDb = true;
		

		switch (_type)
		{
			case 0x01:	// item
			case 0x02:	// skill
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, _lvl, _characterType);
				sendPacket(new ShortCutRegister(sc));
				activeChar.registerShortCut(sc, saveToDb);
				break;
			}
			case 0x03:	// action
			case 0x04:	// macro
            case 0x05:  // recipe
			{
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, _lvl, _characterType);
				sendPacket(new ShortCutRegister(sc));
				activeChar.registerShortCut(sc, saveToDb);
				break;
			}
            case 0x06: // Teleport Bookmark
            {
				L2ShortCut sc = new L2ShortCut(_slot, _page, _type, _id, _lvl, _characterType);
				sendPacket(new ShortCutRegister(sc));
				activeChar.registerShortCut(sc, saveToDb);
				break;
            }
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__33_REQUESTSHORTCUTREG;
	}
}
