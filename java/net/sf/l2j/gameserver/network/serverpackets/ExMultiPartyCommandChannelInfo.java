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

import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.L2Party;


/**
 *
 * @author  chris_00
 * ch sdd d[sdd]
 */
public class ExMultiPartyCommandChannelInfo extends L2GameServerPacket
{
	private static final String _S__FE_31_EXMULTIPARTYCOMMANDCHANNELINFO = "[S] FE:31 ExMultiPartyCommandChannelInfo";
	private L2CommandChannel _channel;
	
	
	public ExMultiPartyCommandChannelInfo(L2CommandChannel channel)
	{
		this._channel = channel;
	}
	
	/**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
	    return _S__FE_31_EXMULTIPARTYCOMMANDCHANNELINFO;
    }

	/**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
    	if (_channel == null)
    		return;
    	
    	// L2PcInstance player = this.getClient().getActiveChar();
    	
	    writeC(0xfe);
	    writeH(0x31);
	    
	    writeS(_channel.getChannelLeader().getName()); // Channelowner
	    writeD(0); // Channelloot 0 or 1
	    writeD(_channel.getMemberCount());
	    
	    writeD(_channel.getPartys().size());
	    for(L2Party p : _channel.getPartys())
	    {
	    	writeS(p.getLeader().getName()); // Leadername
	    	writeD(p.getPartyLeaderOID()); // Leaders ObjId
	    	writeD(p.getMemberCount()); // Membercount
	    }
    }
	
	
}
