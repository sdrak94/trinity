package scripts.achievements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javolution.util.FastMap;
import luna.custom.handler.AchievementHolder;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holders.ItemHolder;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * @author Nik (total rework)
 * @author Midnex
 * @author Promo (htmls)
 */
public class Achievement
{
	private final int									_id;
	private final int									_level;
	private final String								_name;
	private final int									_categoryId;
	private final String								_icon;
	private final String								_desc;
	private final long									_pointsToComplete;
	private final String								_achievementType;
	private static final String							ACHIEVEMENTS_REWARDS_CLAIMED	= "INSERT INTO character_achievements (objectId,achievement,level,completed) VALUES (?,?,?,?)";
	private static final String							ACHIEVEMENTS_REWARDS_CHECK	= "SELECT * FROM character_achievements WHERE objectId = ? AND achievement = ? AND level = ? AND completed = 1";
	private final int									_fame;
	private final List<ItemHolder>						_rewards;
	private final Map<Integer, Map<Integer, Integer>>	_rewardsHolder					= new FastMap<Integer, Map<Integer, Integer>>();
	
	public Achievement(int id, int level, String name, int categoryId, String icon, String desc, long pointsToComplete, String achievementType, int fame)
	{
		_id = id;
		_level = level;
		_name = name;
		_categoryId = categoryId;
		_icon = icon;
		_desc = desc;
		_pointsToComplete = pointsToComplete;
		_achievementType = achievementType;
		_fame = fame;
		_rewards = new LinkedList<>();
	}
	
	public boolean isDone(long playerPoints)
	{
		return playerPoints >= _pointsToComplete;
	}
	
	public String getHtml(L2PcInstance pl, long playerPoints)
	{
		String oneAchievement = HtmCache.getInstance().getHtmForce("data/html/custom/Achievements/oneAchievement.htm");
		int greenbar = (int) ((24 * ((playerPoints * 100) / _pointsToComplete)) / 100);
		greenbar = greenbar < 0 ? 0 : greenbar > 24 ? 24 : greenbar; // Util.constrain(greenbar, 0, 24);
		oneAchievement = oneAchievement.replaceAll("%progress%", (isDone(playerPoints) ? "Completed" : (_pointsToComplete == 1 ? "0" : String.valueOf(playerPoints))));
		oneAchievement = oneAchievement.replaceAll("%bar1%", "" + greenbar);
		oneAchievement = oneAchievement.replaceAll("%bar2%", "" + (24 - greenbar));
		if (!_rewardsHolder.isEmpty())
		{
			String sb = _rewardsHolder.get(_id).toString();
			sb = sb.replace("{", "");
			sb = sb.replace("}", "");
			String sb1 = sb.split("=")[0];
			String amount = sb.split("=")[1];
			String itemName;
			String icon = "";
			if (ItemTable.getInstance().getTemplate(Integer.parseInt(sb1)) != null)
			{
				itemName = ItemTable.getInstance().getTemplate(Integer.parseInt(sb1)).getName();
				icon = ItemTable.getInstance().getTemplate(Integer.parseInt(sb1)).getIcon();
			}
			else
				itemName = "No ItemName";
			oneAchievement = oneAchievement.replaceAll("%rewardIcon%", icon);
			oneAchievement = oneAchievement.replaceAll("%reward%", "x" + amount + " " + itemName);
			oneAchievement = oneAchievement.replaceAll("%rewardTxt%", "Reward:");
		}
		else
		{
			oneAchievement = oneAchievement.replaceAll("%rewardIcon%", "");
			oneAchievement = oneAchievement.replaceAll("%reward%", "");
			oneAchievement = oneAchievement.replaceAll("%rewardTxt%", "");
		}
		int maxLevel = AchievementHolder.getInstance().getMaxLevel(_id);
		oneAchievement = oneAchievement.replaceAll("%stage%", "Cur. Stage: " + _level + "/" + maxLevel);
		oneAchievement = oneAchievement.replaceAll("%cap1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
		oneAchievement = oneAchievement.replaceAll("%cap2%", greenbar >= 24 ? "Gauge_DF_Food_Right" : "Gauge_DF_Exp_bg_Right");
		oneAchievement = oneAchievement.replaceAll("%desc%", _desc.replaceAll("%need%", String.valueOf(_pointsToComplete)));
		oneAchievement = oneAchievement.replaceAll("%icon%", _icon);
		oneAchievement = oneAchievement.replaceAll("%name%", _name + (_level > 1 ? (" Lv. " + _level) : ""));
		return oneAchievement;
	}
	
	public void reward(L2PcInstance player, Map<Integer, Integer> playerAchievementLevels)
	{
		if(checkIfAlreayClaimedReward(player, getId(), getLevel()))
			return;
		player.sendPacket(new CreatureSay(player.getObjectId(), 20, "Achievement Completed!", getName()));
		playerAchievementLevels.put(getId(), getLevel());
		if (getFame() > 0)
		{
			player.setFame(player.getFame() + getFame());
			SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
			sm.addNumber(getFame());
			player.sendPacket(sm);
			player.sendPacket(new UserInfo(player));
		}
		for (ItemHolder ih : getRewards())
		{
			if (ih.getId() == 98020)
			{
				player.setFame(player.getFame() + (int) ih.getCount());
				player.sendMessage("Your fame has increased by " + (int) ih.getCount());
			}
			else if (ih.getId() == 98021)
			{
				player.setPvpKills(player.getPvpKills() + (int) ih.getCount());
				player.sendMessage("Your PvP count has increased by " + (int) ih.getCount());
			}
			else if (ih.getId() == 98022)
			{
				if (player.getClan() != null)
				{
					player.getClan().setReputationScore(player.getClan().getReputationScore() + (int) ih.getCount(), true);
					player.getClan().broadcastToOnlineMembers("Clan reputation increased by " + (int) ih.getCount() + " with the help of " + getName() + "!");
				}
			}
			else
			{
				player.addItem("Achievement " + getName(), ih.getId(), ih.getCount(), player, true);
			}
		}
		updateAchievementState(player, getId(), getLevel(), true);
		player.broadcastPacket(new MagicSkillUse(player, player, 2527, 1, 0, 500));
	}
	
	public boolean checkIfAlreayClaimedReward(final L2PcInstance player, final int var, final int level)
	{
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection(); final PreparedStatement pst = con.prepareStatement(ACHIEVEMENTS_REWARDS_CHECK))
		{
			pst.setInt(1, player.getObjectId());
			pst.setInt(2, var);
			pst.setInt(3, level);
			try (ResultSet rs = pst.executeQuery())
			{
				while (rs.next())
				{
					return true;
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	@SuppressWarnings("unused")
	public void updateAchievementState(final L2PcInstance player, final int var, final int level, final boolean completed)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(ACHIEVEMENTS_REWARDS_CLAIMED);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, var);
			statement.setInt(3, level);
			statement.setBoolean(4, completed);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public List<ItemHolder> getRewards()
	{
		return _rewards;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getDesc()
	{
		return _desc;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public void addReward(int itemId, long itemCount)
	{
		_rewards.add(new ItemHolder(itemId, itemCount));
	}
	
	public void addRewardHolder(int i, int itemId, int itemCount)
	{
		FastMap<Integer, Integer> _rewardsHolder2 = new FastMap<Integer, Integer>();
		_rewardsHolder2.put(itemId, itemCount);
		_rewardsHolder.put(i, _rewardsHolder2);
	}
	
	public String getType()
	{
		return _achievementType;
	}
	
	public long getPointsToComplete()
	{
		return _pointsToComplete;
	}
	
	public int getCategoryId()
	{
		return _categoryId;
	}
	
	public String getIcon()
	{
		return _icon;
	}
	
	public int getFame()
	{
		return _fame;
	}
}
