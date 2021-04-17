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

import java.util.Collection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestStartPledgeWar extends L2GameClientPacket
{
private static final String _C__4D_REQUESTSTARTPLEDGEWAR = "[C] 4D RequestStartPledgewar";
//private static Logger _log = Logger.getLogger(RequestStartPledgeWar.class.getName());

private String _pledgeName;
private L2Clan _clan;
private L2PcInstance player;

@Override
protected void readImpl()
{
	_pledgeName = readS();
}

@Override
protected void runImpl()
{
	player = getClient().getActiveChar();
	if (player == null) return;
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	_clan = getClient().getActiveChar().getClan();
	if (_clan == null) return;
	
	if (_clan.getLevel() < 6 || _clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		sm = null;
		return;
	}
	else if ((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) != L2Clan.CP_CL_PLEDGE_WAR )
	{
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
	if (clan == null)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_CANNOT_DECLARED_CLAN_NOT_EXIST);
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	else if (_clan.getAllyId() == clan.getAllyId() && _clan.getAllyId() != 0)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		sm = null;
		return;
	}
	else if (_clan.getClanId() == clan.getClanId())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		sm = null;
		return;
	}
	else if (clan.getLevel() < 6 || clan.getMembersCount() < Config.ALT_CLAN_MEMBERS_FOR_WAR)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		sm = null;
		return;
	}
	else if (_clan.isAtWarWith(clan.getClanId()))
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.ALREADY_AT_WAR_WITH_S1_WAIT_5_DAYS); //msg id 628
		sm.addString(clan.getName());
		player.sendPacket(sm);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		sm = null;
		return;
	}
	
	//_log.warning("RequestStartPledgeWar, leader: " + clan.getLeaderName() + " clan: "+ _clan.getName());
	
	//        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
	
	//        if(leader == null)
	//            return;
	
	//        if(leader != null && leader.isOnline() == 0)
	//        {
	//            player.sendMessage("Clan leader isn't online.");
	//            player.sendPacket(ActionFailed.STATIC_PACKET);
	//            return;
	//        }
	
	//        if (leader.isProcessingRequest())
	//        {
	//            SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
	//            sm.addString(leader.getName());
	//            player.sendPacket(sm);
	//            return;
	//        }
	
	//        if (leader.isTransactionInProgress())
	//        {
	//            SystemMessage sm = new SystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
	//            sm.addString(leader.getName());
	//            player.sendPacket(sm);
	//            return;
	//        }
	
	//        leader.setTransactionRequester(player);
	//        player.setTransactionRequester(leader);
	//        leader.sendPacket(new StartPledgeWar(_clan.getName(),player.getName()));
	
	ClanTable.getInstance().storeclanswars(player.getClanId(), clan.getClanId());
	if(ClanTable.getInstance().checkForTwoSidedClanWar(player.getClanId(), clan.getClanId()))
	{
		ClanTable.getInstance().storeTwoSidedWar(player.getClanId(), clan.getClanId());
	}
	Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
	//synchronized (L2World.getInstance().getAllPlayers())
	{
		for (L2PcInstance cha : pls)
			if (cha.getClan() == player.getClan() || cha.getClan() == clan)
				cha.broadcastUserInfo();
	}
}

@Override
public String getType()
{
	return _C__4D_REQUESTSTARTPLEDGEWAR;
}
}
