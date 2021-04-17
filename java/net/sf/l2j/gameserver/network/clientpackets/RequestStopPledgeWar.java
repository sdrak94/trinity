package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestStopPledgeWar extends L2GameClientPacket
{
private static final String _C__4F_REQUESTSTOPPLEDGEWAR = "[C] 4F RequestStopPledgeWar";
//private static Logger _log = Logger.getLogger(RequestStopPledgeWar.class.getName());

private String _pledgeName;

@Override
protected void readImpl()
{
	_pledgeName = readS();
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null) return;
	L2Clan playerClan = player.getClan();
	if (playerClan == null) return;
	
	L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
	
	if (clan == null)
	{
		player.sendMessage("No such clan.");
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (!playerClan.isAtWarWith(clan.getClanId()))
	{
		player.sendMessage("You aren't at war with this clan.");
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (playerClan.tempWarClanIds.contains(clan.getClanId()))
	{
		player.sendMessage("You have already requested the war to be ended, which will be effective after the next server restart.");
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Check if player who does the request has the correct rights to do it
	if ((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) != L2Clan.CP_CL_PLEDGE_WAR )
	{
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
		return;
	}
	
	for (L2PcInstance playa : playerClan.getOnlineMembers(0))
	{
		if (playa.isInCombat())
		{
			player.sendMessage("At least one of your online members is in combat mode, denied");
			return;
		}
	}
	
	Connection con = null;
	boolean exit = false;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT penalty FROM clan_wars WHERE clan1=? AND clan2=?");
		statement.setInt(1, playerClan.getClanId());
		statement.setInt(2, clan.getClanId());
		ResultSet rset = statement.executeQuery();
		
		if (rset.next())
		{
			final long penalty = rset.getLong("penalty");
			
			if (penalty > 0 && penalty > System.currentTimeMillis())
			{
				long hours = TimeUnit.MILLISECONDS.toHours(penalty) - TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
				
				player.sendMessage("You need to wait an additional "+ hours + " Hours before ending war with this clan");
				exit = true;
			}
		}
		else
			exit = true;
		
		rset.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error trying to restoring clanwar penalty for clan "+playerClan.getName(), e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
	
	//_log.info("RequestStopPledgeWar: By leader or authorized player: " + playerClan.getLeaderName() + " of clan: "
	//	+ playerClan.getName() + " to clan: " + _pledgeName);
	
	//        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
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
	
	if (!exit)
		ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
}

@Override
public String getType()
{
	return _C__4F_REQUESTSTOPPLEDGEWAR;
}
}