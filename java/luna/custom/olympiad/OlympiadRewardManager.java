package luna.custom.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

public class OlympiadRewardManager
{
	protected static final Logger			_log	= Logger.getLogger(OlympiadRewardManager.class.getName());
	private static final SimpleDateFormat	format	= new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	public static OlympiadRewardManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadRewardManager INSTANCE = new OlympiadRewardManager();
	}
	
	public void proccess(L2PcInstance activeChar)
	{
		if (checkForAlreadyRewarded(activeChar, activeChar.getObjectId()))
		{
			activeChar.sendMessage("You have already claimed your reward, you may come back on next season.");
			return;
		}
		rewardPlayer(activeChar);
		registerPlayer(activeChar);
	}
	
	public static boolean checkForHeroStatus(L2PcInstance activeChar, int objid)
	{
		boolean isHero = false;
		if (!activeChar.isFakeHero() && activeChar.isHero() && !activeChar.isFOSHero() && !activeChar._tempHero)
		{
			isHero = true;
		}
		return isHero;
	}
	
	public static int checkForPoints(L2PcInstance activeChar, int objid)
	{
		int olympiadPoints = 0;
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles WHERE charId = ?");
				statement.setInt(1, objid);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					olympiadPoints = rset.getInt(1);
					rset.close();
					statement.close();
				}
				else
				{
					return 0;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed checking for hero status on player: " + String.valueOf(objid), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return olympiadPoints;
	}
	
	public static int getCycle()
	{
		int cycle = 0;
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT current_cycle FROM olympiad_data");
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					cycle = rset.getInt(1);
					rset.close();
					statement.close();
				}
				else
				{
					return 0;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed checking for olympiad cycle: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return cycle;
	}
	
	public void registerPlayer(L2PcInstance activeChar)
	{
		int points = 0;
		points = checkForPoints(activeChar, activeChar.getObjectId());
		Connection con = null;
		PreparedStatement statement;
		if (points != 0)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				{
					statement = con.prepareStatement("INSERT INTO olympiad_reward_memo(charId,char_name,claimed_ammount,claimed_date,olympiad_cycle) VALUES (?,?,?,?,?)");
					statement.setInt(1, activeChar.getObjectId());
					statement.setString(2, activeChar.getName());
					statement.setInt(3, points);
					statement.setString(4, format.format(new Date(System.currentTimeMillis())));
					statement.setInt(5, getCycle());
					statement.execute();
					statement.close();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed storing olympiad_reward_memo for player: " + String.valueOf(activeChar.getObjectId()), e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
	}
	
	public static boolean checkForAlreadyRewarded(L2PcInstance activeChar, int objid)
	{
		boolean isRegistered = false;
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT olympiad_cycle FROM olympiad_reward_memo WHERE charId = ? AND olympiad_cycle = ?");
				statement.setInt(1, objid);
				statement.setInt(2, getCycle());
				ResultSet rset = statement.executeQuery();
				while (rset.next())
				{
					if (rset.getInt(1) < getCycle())
					{
						isRegistered = false;
					}
					if (rset.getInt(1) == getCycle())
					{
						isRegistered = true;
					}
				}
				rset.close();
				statement.close();
				return isRegistered;
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed checking for hero status on player: " + String.valueOf(objid), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return isRegistered;
	}
	
	public void rewardPlayer(L2PcInstance activeChar)
	{
		boolean rewardClan = false;
		if (checkForAlreadyRewarded(activeChar, activeChar.getObjectId()))
		{
			activeChar.sendMessage("You have already claimed your reward, you may come back on next season.");
			return;
		}
		int points = 0;
		points = checkForPoints(activeChar, activeChar.getObjectId());
		if (checkForHeroStatus(activeChar, activeChar.getObjectId()))
		{
			if (activeChar.getClan() != null)
			{
				rewardClan = true;
			}
			points += 100;
		}
		if (rewardClan)
		{
			activeChar.getClan().setReputationScore(activeChar.getClan().getReputationScore() + 500, true);
			String baseClassName = CharTemplateTable.getInstance().getClassNameById(activeChar.getBaseClassId());
			for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
			{
				if (member == null)
					continue;
				String text = ": +500 Clan Reputation Score added by " + activeChar.getName() + " gaining " + baseClassName + " hero.";
				CreatureSay cs = new CreatureSay(0, Say2.CLAN, activeChar.getClan().getName(), text);
				member.sendPacket(cs);
			}
		}
		activeChar.addItem("OlympiadReward", 800101, points, activeChar, true);
		InventoryUpdate iu = new InventoryUpdate();
		activeChar.sendPacket(iu);
		activeChar.getInventory().updateDatabase();
	}
}