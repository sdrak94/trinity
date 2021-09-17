package luna.custom.extensions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import luna.custom.email.DonationCodeGenerator;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;

public class DonateGiverTaskManager
{
	private static Logger _log = Logger.getLogger(DonateGiverTaskManager.class.getName());
	
	public static DonateGiverTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonateGiverTaskManager _instance = new DonateGiverTaskManager();
	}
	
	protected DonateGiverTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() -> start(), 5000, 5000);
		_log.info("DonateGiver: started.");
	}
	
	private static void start()
	{
		String charName = null;
		int no = 0;
		int id = 0;
		int count = 0;
		String playerName = "";
		String email = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT no, id, count, playername, email FROM donate_holder WHERE order_status='1';"))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					no = rset.getInt("no");
					id = rset.getInt("id");
					count = rset.getInt("count");
					playerName = rset.getString("playername");
					email = rset.getString("email");
					if (id > 0 && count > 0 && playerName != "")
					{
						DonationCodeGenerator.getInstance();
						DonationCodeGenerator.storeCode(email, count);
						RemoveDonation(no);
					}
				}
			}
			catch (Exception e)
			{
				_log.warning("Donate rewarder fail: for character: " + charName + " " + count + " Donate Coins! " + e.getMessage());
			}
		}
		catch (Exception e)
		{
			_log.warning("Check donate items failed. " + e.getMessage());
		}
		return;
	}
	
	/**
	 * @param no
	 */
	private static void RemoveDonation(int no)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("update donate_holder set order_status='3' where no=?"))
		{
			statement.setInt(1, no);
			statement.execute();
		}
		catch (SQLException e)
		{
			_log.warning("Failed to remove donation from database char: " + no);
			_log.warning(e.getMessage());
		}
	}
}