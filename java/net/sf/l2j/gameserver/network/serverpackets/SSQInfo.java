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

import net.sf.l2j.gameserver.SevenSigns;
/**
 * Changes the sky color depending on the outcome
 * of the Seven Signs competition.
 *
 * packet type id 0xf8
 * format: c h
 *
 * @author Tempy
 */
public class SSQInfo extends L2GameServerPacket
{
    private static final String _S__F8_SSQINFO = "[S] 73 SSQInfo";

    private static int _state = 0;

    public SSQInfo()
    {
        int compWinner = SevenSigns.getInstance().getCabalHighestScore();

        if (SevenSigns.getInstance().isSealValidationPeriod())
            if (compWinner == SevenSigns.CABAL_DAWN)
                _state = 2;
            else if (compWinner == SevenSigns.CABAL_DUSK)
                _state = 1;
    }

    public SSQInfo(int state)
    {
        _state = state;
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0x73);

        if (_state == 2) // Dawn Sky
        {
            writeH(258);
        }
        else if (_state == 1) // Dusk Sky
        {
            writeH(257);
        }
        else
        {
            writeH(256);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _S__F8_SSQINFO;
    }
}
