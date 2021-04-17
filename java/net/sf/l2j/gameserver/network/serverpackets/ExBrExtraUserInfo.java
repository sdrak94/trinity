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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * @author Kerberos
 */
public class ExBrExtraUserInfo extends L2GameServerPacket
{
private L2PcInstance _activeChar;
private int _charObjId;
private int _val;

public ExBrExtraUserInfo(L2PcInstance player)
{
	_activeChar = player;
	_charObjId = player.getObjectId();
	_val = player.getEventEffectId();
	_invisible = player.isInvisible();
}

public L2PcInstance getCharInfoActiveChar()
{
	return _activeChar;
}

@Override
/**
 * This packet should belong to Quest windows, not UserInfo in T3.
 */
protected final void writeImpl()
{
	writeC(0xfe);
	writeH(0xac);
	writeD(_charObjId);  //object id of player
	writeD(_val);  //	afro hair cut
	/*writeC(0x01);*/
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return "[S] FE:8D ExBrExtraUSerInfo";
}
}
