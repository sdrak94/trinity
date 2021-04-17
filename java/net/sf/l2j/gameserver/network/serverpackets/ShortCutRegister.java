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

import net.sf.l2j.gameserver.model.L2ShortCut;
/**
 *
 *
 * sample
 *
 * 56
 * 01000000 04000000 dd9fb640 01000000
 *
 * 56
 * 02000000 07000000 38000000 03000000 01000000
 *
 * 56
 * 03000000 00000000 02000000 01000000
 *
 * format   dd d/dd/d d
 *
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public final class ShortCutRegister extends L2GameServerPacket
{
	private static final String _S__56_SHORTCUTREGISTER = "[S] 44 ShortCutRegister";

    private L2ShortCut _shortcut;

	/**
	 * Register new skill shortcut
	 * @param slot
	 * @param type
	 * @param typeId
	 * @param level
	 * @param dat2
	 */
	public ShortCutRegister(L2ShortCut shortcut)
	{
		_shortcut = shortcut;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x44);

		writeD(_shortcut.getType());
		writeD(_shortcut.getSlot() + _shortcut.getPage() * 12); // C4 Client
		switch(_shortcut.getType())
        {
        case L2ShortCut.TYPE_ITEM: //1
        	writeD(_shortcut.getId());
        	writeD(_shortcut.getCharacterType());
        	writeD(-1); // here should be item type
        	writeD(0x00);  // unknown
        	writeD(0x00);  // unknown
        	writeD(0x00);  // item augment id
        	break;
        case L2ShortCut.TYPE_SKILL: //2
        	writeD(_shortcut.getId());
        	writeD(_shortcut.getLevel());
        	writeC(0x00); // C5
        	writeD(_shortcut.getCharacterType());
        	break;
       /** these are same as default case, no need to duplicate, enable if packet get changed
        */ 
       /*	case L2ShortCut.TYPE_ACTION: //3
        *		writeD(_shortcut.getId());
        *		writeD(_shortcut.getUserCommand());
        *		break;
        *	case L2ShortCut.TYPE_MACRO: //4
        *		writeD(_shortcut.getId());
        *		writeD(_shortcut.getUserCommand());
        *		break;
        *	case L2ShortCut.TYPE_RECIPE: //5
        *		writeD(_shortcut.getId());
        *		writeD(_shortcut.getUserCommand());
        *		break;
        */
        default:
        {
        	writeD(_shortcut.getId());
        	writeD(_shortcut.getCharacterType());
        }
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__56_SHORTCUTREGISTER;
	}
}
