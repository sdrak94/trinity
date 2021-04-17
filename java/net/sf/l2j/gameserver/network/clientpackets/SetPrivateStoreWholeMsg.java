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
import net.sf.l2j.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;

/**
 *
 * @author  KenM
 */
public class SetPrivateStoreWholeMsg extends L2GameClientPacket
{
    private String _msg;
    
    /**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[C] D0:4D SetPrivateStoreWholeMsg";
    }

    /**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
     */
    @Override
    protected void readImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        _msg = readS();

		if (_msg.length()>30)
		{
			if (_msg.length()>50)
			{
				_log.warning(player.getName() +"Tried to exploit with vendor message bug by inserting a "+ _msg.length() +" Text: "+_msg);
			}
			_msg = _msg.substring(0, 30);
		}
    }

    /**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null || player.getSellList() == null) return;

        player.getSellList().setTitle(_msg);
        sendPacket(new ExPrivateStoreSetWholeMsg(player));
    }
    
}
