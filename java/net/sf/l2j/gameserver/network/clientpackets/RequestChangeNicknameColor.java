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
 *
 * @author  KenM
 */
public class RequestChangeNicknameColor extends L2GameClientPacket
{

	private int _unk1;
	private String _unk2;

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
     */
    @Override
    public String getType()
    {
	    return "[C] D0:52 RequestChangeNicknameColor";
    }

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
     */
    @Override
    protected void readImpl()
    {
    	_unk1 = readD();
    	_unk2 = readS();
    }

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
    	L2PcInstance activeChar = this.getClient().getActiveChar();
	    if (activeChar != null)
	    {
	    	activeChar.sendMessage("RequestChangeNicknameColor: "+_unk1+" - "+_unk2);
	    }
    }
	
}
