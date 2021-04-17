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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;

/**
 *
 * @author  -Wooden-
 */
public class PledgeReceiveSubPledgeCreated extends L2GameServerPacket
{
private static final String _S__FE_3F_PLEDGERECEIVESUBPLEDGECREATED = "[S] FE:40 PledgeReceiveSubPledgeCreated";
@SuppressWarnings("unused")
private static Logger _log = Logger.getLogger(PledgeReceiveSubPledgeCreated.class.getName());
private SubPledge _subPledge;
private L2Clan _clan;

/**
 * @param member
 */
public PledgeReceiveSubPledgeCreated(SubPledge subPledge, L2Clan clan)
{
	_subPledge = subPledge;
	_clan = clan;
}

/**
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
 */
@Override
protected void writeImpl()
{
	writeC(0xfe);
	writeH(0x40);
	
	writeD(0x01);
	writeD(_subPledge.getId());
	writeS(_subPledge.getName());
	writeS(getLeaderName());
}

private String getLeaderName()
{
	int LeaderId = _subPledge.getLeaderId();
	if (_subPledge.getId() == L2Clan.SUBUNIT_ACADEMY || LeaderId == 0)
		return "";
	else if (_clan.getClanMember(LeaderId) == null)
	{
		/*			_log.warning("SubPledgeLeader: "+ LeaderId + " is missing from clan: "+ _clan.getName()+"["+_clan.getClanId()+"]");
		 */			return "";
	}
	else
		return _clan.getClanMember(LeaderId).getName();
}

/**
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _S__FE_3F_PLEDGERECEIVESUBPLEDGECREATED;
}

}
