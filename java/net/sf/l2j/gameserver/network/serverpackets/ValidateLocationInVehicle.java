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

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class ValidateLocationInVehicle extends L2GameServerPacket
{
    private static final String _S__73_ValidateLocationInVehicle = "[S] 80 ValidateLocationInVehicle";
    private L2PcInstance _activeChar;


    /**
     * 0x73 ValidateLocationInVehicle         hdd
     * @param _characters
     */
    public ValidateLocationInVehicle(L2Character player)
    {
    	if (!(player instanceof L2PcInstance)) return;

		_activeChar = (L2PcInstance)player;

		if (_activeChar.getBoat() == null) return;
    }

    @Override
	protected final void writeImpl()
    {
    	
        writeC(0x80);
        writeD(_activeChar.getObjectId());
        writeD(_activeChar.getBoat().getObjectId());
        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
        writeD(_activeChar.getHeading());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _S__73_ValidateLocationInVehicle;
    }
}
