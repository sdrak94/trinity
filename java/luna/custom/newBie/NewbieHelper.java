package luna.custom.newBie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Experience;

public class NewbieHelper
{
	public void giveNewbieRewards(L2PcInstance player)
	{
		if (player == null)
			return;
		if (player.getCounters().kamaDone >= 9)
		{
			player.sendMessage("You have more than 9 kamalokas done, you can't claim the newbie reward.");
			return;
		}
		if (player.getCounters().soloDone >= 9)
		{
			player.sendMessage("You have more than 9 Solo Instances done, you can't claim the newbie reward.");
			return;
		}
		if (player.getAccountChars().size() > 0)
		{
			//player.sendMessage("Total Chars in account: " + player.getAccountChars().size());
			player.sendMessage("You have more than 1 char in this account, you can't claim the newbie reward.");
			return;
		}
		if (alreadyClaimed(player.getAccountName()))
		{
			player.sendMessage("You have already claimed the newbie reward with this account, you can't claim the newbie reward more than once.");
			return;
		}
		if (player.getPvpKills() < 100)
		{
			player.setPvpKills(100);
			if (player.getInventory().getItemByItemId(99004) == null)
			{
				player.addItem("Newbie", 99004, 100, player, true); // PvP Token
			}
		}
		player.getCounters().kamaDone = player.getCounters().kamaDone + 5;
		player.getCounters().soloDone = player.getCounters().soloDone + 5;
		player.addItem("Newbie", 50024, 10, player, true); // Titanium Rune
		player.addItem("Newbie", 13002, 10, player, true); // Dread Essence
		player.addItem("Newbie", 51003, 5, player, true); // Kamaloka Essence
		player.addItem("Newbie", 62003, 10, player, true); // EXP BOOST - Medium
		player.addItem("Newbie", 62022, 10, player, true); // DROP BOOST - Medium
		if (player.getLevel() < 89) // Set Level 89
		{
			byte lvl = Byte.parseByte("89");
			if (lvl >= 1 && lvl <= Experience.MAX_LEVEL)
			{
				try
				{
					long pXp = player.getExp();
					long tXp = Experience.LEVEL[lvl];
					if (pXp > tXp)
					{
						player.removeExpAndSp(pXp - tXp, 0);
					}
					else if (pXp < tXp)
					{
						player._ignoreLevel = true;
						player.addExpAndSp(tXp - pXp, 1000000000);
					}
				}
				catch (Exception e)
				{}
				finally
				{
					player._ignoreLevel = false;
				}
			}
		}
		storeChar(player.getAccountName());
	}
	
	private void storeChar(String accName)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO newbie_rewards (account_name,claimed) VALUES (?,?)");
			statement.setString(1, accName);
			statement.setString(2, "true");
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private boolean alreadyClaimed(String accName)
	{
		boolean claimed = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM newbie_rewards WHERE account_name=?");
			statement.setString(1, accName);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				claimed = true;
			}
			else
				claimed = false;
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return claimed;
	}
	
	public static NewbieHelper getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NewbieHelper _instance = new NewbieHelper();
	}
}
