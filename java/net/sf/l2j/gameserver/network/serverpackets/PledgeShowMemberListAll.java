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

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import java.util.logging.Logger;
/**
 * sample from gracia final:
 * 
 * 5A // packet id
 * 
 * 00 00 00 00 // pledge = 1 subpledge = 0
 * D0 2D 00 00 // clan ID
 * 00 00 00 00 // pledge Id
 * 54 00 68 00 65 00 4B 00 6E 00 69 00 67 00 68 00 74 00 73 00 4F 00 66 00 47 00 6F 00 64 00 00 00 // clan name
 * 54 00 68 00 65 00 47 00 72 00 65 00 65 00 6E 00 44 00 72 00 61 00 67 00 30 00 6E 00 00 00 // clan leader
 * 
 * 9D 4F 01 00 // crest ID
 * 03 00 00 00 // level
 * 00 00 00 00 // castle id
 * 00 00 00 00 // hideout id
 * 00 00 00 00 // fort id
 * 00 00 00 00 // rank
 * 00 00 00 00 // reputation
 * 00 00 00 00 // ?
 * 00 00 00 00 // ?
 * 00 00 00 00 // ally id
 * 00 00       // ally name
 * 00 00 00 00 // ally crest id
 * 00 00 00 00 // is at war
 * 00 00 00 00 // territory castle ID
 * 
 * 01 00 00 00 // member count
 * 
 * 51 00 75 00 65 00 65 00 70 00 68 00 00 00 // member name
 * 22 00 00 00 // member level
 * 07 00 00 00 // member class id
 * 01 00 00 00 // member sex
 * 00 00 00 00 // member race
 * 00 00 00 00 // member object id (if online)
 * 00 00 00 00 // member sponsor
 * 
 *
 * format   dddSS ddddddddddSddd d (Sdddddd)
 *
 * @version $Revision: 1.6.2.2.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class PledgeShowMemberListAll extends L2GameServerPacket
{
	private static final String _S__68_PLEDGESHOWMEMBERLISTALL = "[S] 5a PledgeShowMemberListAll";
	private final L2Clan _clan;
	private final L2PcInstance _activeChar;
	private final L2ClanMember[] _members;
	private int _pledgeType;
	//private static Logger _log = Logger.getLogger(PledgeShowMemberListAll.class.getName());
	
	public PledgeShowMemberListAll(L2Clan clan, L2PcInstance activeChar)
	{
		_clan = clan;
		_activeChar = activeChar;
		_members = _clan.getMembers();
	}
	
	@Override
	protected final void writeImpl()
	{
		
		_pledgeType = 0;
		writePledge(0);
		
		for (SubPledge subPledge: _clan.getAllSubPledges())
		{
			_activeChar.sendPacket(new PledgeReceiveSubPledgeCreated(subPledge, _clan));
		}
		
		for (L2ClanMember m : _members)
		{
			if (m.getPledgeType() == 0) continue;
			_activeChar.sendPacket(new PledgeShowMemberListAdd(m));
		}
		
		// unless this is sent sometimes, the client doesn't recognise the player as the leader
		if (_activeChar.canSendUserInfo)
		{
			_activeChar.sendPacket(new UserInfo(_activeChar));
			_activeChar.sendPacket(new ExBrExtraUserInfo(_activeChar));
		}
	}
	
	void writePledge(int mainOrSubpledge)
	{
		writeC(0x5a);
		
		writeD(mainOrSubpledge);
		writeD(_clan.getClanId());
		writeD(_pledgeType);
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		
		writeD(_clan.getCrestId()); // crest id .. is used again
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(_clan.getHasFort());
		writeD(_clan.getRank());
		writeD(_clan.getReputationScore());
		writeD(0); //0
		writeD(0); //0
		writeD(_clan.getAllyId());
		writeS(_clan.getAllyName());
		writeD(_clan.getAllyCrestId());
		writeD(_clan.isAtWar()? 1 : 0);// new c3
		writeD(0); // Territory castle ID
		writeD(_clan.getSubPledgeMembersCount(_pledgeType));
		
		for (L2ClanMember m : _members)
		{
			if(m.getPledgeType() != _pledgeType) continue;
			writeS(m.getName());
			writeD(m.getLevel());
			writeD(m.getClassId());
			L2PcInstance player;
			if ((player = m.getPlayerInstance()) != null)
			{
				writeD(player.getAppearance().getSex() ? 1 : 0); // no visible effect
				writeD(player.getRace().getRealOrdinal());//writeD(1);
			}
			else
			{
				writeD(1); // no visible effect
				writeD(1); //writeD(1);
			}
			writeD(m.isOnline() ? m.getObjectId() : 0);  // objectId=online 0=offline
			writeD(m.getSponsor() != 0 ? 1 : 0);
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__68_PLEDGESHOWMEMBERLISTALL;
	}
	
}
