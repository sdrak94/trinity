package guard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class HwidBanController
{
	private final HashMap<String, Long>	_bannedHwids	= new HashMap<String, Long>();
	private static final String			SELECT_HWIDS	= "SELECT * FROM hwid_bans";
	private static final String			INSERT_BAN		= "INSERT INTO hwid_bans VALUES (?, ?, ?, ?, ?, ?)";
	
	public void collectBans()
	{
		_bannedHwids.clear();
		try (Connection con = L2DatabaseFactory.getConnectionS(); PreparedStatement st = con.prepareStatement(SELECT_HWIDS))
		{
			try (ResultSet rs = st.executeQuery())
			{
				while (rs.next())
				{
					String hwid = rs.getString("hwid");
					long ban = rs.getDate("banned_till").getTime();
					_bannedHwids.put(hwid, ban);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void liftBans()
	{
		for (String hwid : _bannedHwids.keySet())
		{
			long ban = _bannedHwids.get(hwid);
			if (ban <= System.currentTimeMillis())
			{
				_bannedHwids.remove(hwid);
			}
			continue;
		}
	}
	
	public boolean checkIfBanned(String hwid)
	{
		boolean isBanned = false;
		try (Connection con = L2DatabaseFactory.getConnectionS(); PreparedStatement st = con.prepareStatement("SELECT * FROM hwid_bans WHERE hwid = ? "))
		{
			st.setString(1, hwid);
			try (ResultSet rs = st.executeQuery())
			{
				if (rs.next())
				{
					isBanned = true;
				}
				else
				{
					isBanned = false;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return isBanned;
	}
	
	public void storeBan(String hwid, String account, String char_name, String reason, long banDate, long bannedtill)
	{
		try (final var con = L2DatabaseFactory.getConnectionS(); final var pst = con.prepareStatement(INSERT_BAN))
		{
			// con.setAutoCommit(false);
			pst.setString(1, hwid);
			pst.setString(2, account);
			pst.setString(3, char_name);
			pst.setString(4, reason);
			pst.setTimestamp(5, new Timestamp(banDate));
			pst.setTimestamp(6, new Timestamp(bannedtill));
			pst.executeUpdate();
			// pst.addBatch();
			// pst.executeBatch();
			// con.commit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void ban(L2PcInstance bannedP, int days)
	{
		final Long time = TimeUnit.DAYS.toMillis(days);
		final String hwid = bannedP.getClient().getFullHwid().toString();
		final String bannedOut = "Player [Name:{" + bannedP.getName() + "} HWID:{" + bannedP.getClient().getFullHwid() + "}] banned for [" + time + "] days";
		GmListTable.broadcastMessageToAdvancedGMs(bannedOut);
		bannedP.closeNetConnection();
		storeBan(hwid, bannedP.getAccountName(), bannedP.getName(), "Etsi", System.currentTimeMillis(), System.currentTimeMillis() + time);
	}
	
	public void ban(L2PcInstance bannedP, int days, String reason)
	{
		final Long time = TimeUnit.DAYS.toMillis(days);
		final String hwid = bannedP.getClient().getFullHwid().toString();
		final String bannedOut = "Player [Name:{" + bannedP.getName() + "} HWID:{" + bannedP.getClient().getFullHwid() + "}] banned for [" + time + "] days";
		GmListTable.broadcastMessageToAdvancedGMs(bannedOut);
		bannedP.closeNetConnection();
		storeBan(hwid, bannedP.getAccountName(), bannedP.getName(), reason, System.currentTimeMillis(), System.currentTimeMillis() + time);
	}
	
	public void ban(String hwid, int days, String reason)
	{
		final Long time = TimeUnit.DAYS.toMillis(days);
		final String bannedOut = " HWID:{" + hwid + "}] banned for [" + time + "] days";
		GmListTable.broadcastMessageToAdvancedGMs(bannedOut);
		L2PcInstance bannedP;
		if (L2World.getInstance().getPlayerByHwid(hwid) != null)
		{
			bannedP = L2World.getInstance().getPlayerByHwid(hwid);
			bannedP.closeNetConnection();
			storeBan(hwid, bannedP.getAccountName(), bannedP.getName(), reason, System.currentTimeMillis(), System.currentTimeMillis() + time);
		}
		else
		{
			storeBan(hwid, "empty", "empty", reason, System.currentTimeMillis(), System.currentTimeMillis() + time);
		}
	}
	
	public static class InstanceHolder
	{
		private static final HwidBanController _instance = new HwidBanController();
	}
	
	public static HwidBanController getInstance()
	{
		return InstanceHolder._instance;
	}
}
