package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2RebirthMasterInstance extends L2NpcInstance
{
	/** Basically, this will act as a cache so it doesnt have to read DB information on relog. */
	private static FastMap<Integer, Integer>	_playersRebirthInfo	= new FastMap<Integer, Integer>();
	private static Random						random				= new Random();
	private static int							playerRebirthCount	= 0;
	
	public L2RebirthMasterInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equals("Rebirth"))
		{
			requestRebirth(player);
		}
		else
		{
			return;
		}
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/custom/Rebirth/rebirthmenu.htm";
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	/** Checks to see if the player is eligible for a Rebirth, if so it grants it and stores information */
	public void requestRebirth(L2PcInstance player)
	{
		int currBirth = getRebirthLevel(player);
		int pvpsNeeded = 1000;
		if (player.getLevel() < 94)
		{
			player.sendMessage("You need to be at least level 94");
			return;
		}
		else if (player.isSubClassActive())
		{
			player.sendMessage("Rebirthing can be only done on your main class");
			return;
		}
		else if (player.getClassId().level() < 3)
		{
			player.sendMessage("You must be on your final profession to Rebirth");
			return;
		}
		else if (currBirth >= 4)
		{
			player.sendMessage("You are at your maximum rebirth count");
			return;
		}
		int itemNeeded = 99986;
		int itemAmount = 1;
		if (itemNeeded != 0)
		{
			if (!playerIsEligible(player, itemNeeded, itemAmount))
			{
				return;
			}
		}
		boolean firstBirth = currBirth == 0;
		grantRebirth(player, (currBirth + 1), firstBirth);
	}
	
	/** Physically rewards player and resets status to nothing. */
	public void grantRebirth(L2PcInstance player, int newBirthCount, boolean firstBirth)
	{
		try
		{
			player.removeExpAndSp(player.getExp() - Experience.LEVEL[92], 0);// Set player to level 1.
			player.store(); // Updates the player's information in the Character Database.
			if (firstBirth)
				storePlayerBirth(player);
			else
				updatePlayerBirth(player, newBirthCount);
			grantRebirthSkills(player);// Give the player his new Skills.
			TextBuilder html1 = new TextBuilder("<html><body>Congratulations");
			html1.append("</body></html>");
			insertObjectIdAndShowChatWindow(player, html1.toString());
			displayCongrats(player);// Displays a congratulation message to the player.

			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				player.getCounters().rebirth++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/** Special effects when the player levels. */
	public void displayCongrats(L2PcInstance player)
	{
		player.broadcastPacket(new SocialAction(player.getObjectId(), 3));// Victory Social Action.
		MagicSkillUse MSU = new MagicSkillUse(player, player, 2024, 1, 1, 0);// Fireworks Display
		player.broadcastPacket(MSU);
		ExShowScreenMessage screen = new ExShowScreenMessage("Congratulations " + player.getName() + "! You have been Reborn!", 15000);
		player.sendPacket(screen);
	}
	
	/** Check and verify the player DOES have the item required for a request. Also, remove the item if he has. */
	public boolean playerIsEligible(L2PcInstance player, int itemId, int itemAmount)
	{
		String itemName = ItemTable.getInstance().getTemplate(itemId).getName();
		L2ItemInstance itemNeeded = player.getInventory().getItemByItemId(itemId);
		if (itemNeeded == null || itemNeeded.getCount() < itemAmount)
		{
			player.sendMessage("You need atleast " + itemAmount + "  [ " + itemName + " ] to request a Rebirth!");
			return false;
		}
		// Player has the required items, so we're going to take them!
		player.getInventory().destroyItem("Rebirth Engine", itemNeeded, itemAmount, player, null);
		player.sendMessage("Removed " + itemAmount + " " + itemName + " from your inventory!");
		return true;
	}
	
	/** Gives the available Bonus Skills to the player. */
	private void grantRebirthSkills(L2PcInstance player)
	{
		int rebirthLevel = getRebirthCount(player); // returns the current Rebirth Level
		final boolean isMage = player.isMageClass(); // Returns true if BASE CLASS is a mage.
		// Simply return since no bonus skills are granted.
		if (rebirthLevel == 0)
			return;
		// Load the bonus skills unto the player.
		CreatureSay rebirthText = null;
		// int stat = random.nextInt(3);
		L2Skill skill = null;
		if (isMage || player.getClassId().getId() == 0x74)
		{
			if (rebirthLevel == 1)
			{
				skill = SkillTable.getInstance().getInfo(9002, getSkillLevel(player, 9002)); // wit
			}
			if (rebirthLevel == 2)
			{
				skill = SkillTable.getInstance().getInfo(9001, getSkillLevel(player, 9001)); // int
			}
			if (rebirthLevel == 3)
			{
				skill = SkillTable.getInstance().getInfo(9003, getSkillLevel(player, 9003)); // men
			}
			if (rebirthLevel == 4)
			{
				skill = SkillTable.getInstance().getInfo(9004, getSkillLevel(player, 9004)); // con
			}
		}
		else
		{
			if (rebirthLevel == 1)
			{
				skill = SkillTable.getInstance().getInfo(9006, getSkillLevel(player, 9006)); // str
			}
			if (rebirthLevel == 2)
			{
				skill = SkillTable.getInstance().getInfo(9005, getSkillLevel(player, 9005)); // dex
			}
			if (rebirthLevel == 3)
			{
				skill = SkillTable.getInstance().getInfo(9004, getSkillLevel(player, 9004)); // con
			}
			if (rebirthLevel == 4)
			{
				skill = SkillTable.getInstance().getInfo(9003, getSkillLevel(player, 9003)); // men
			}
		}
		if (skill != null)
		{
			player.addSkill(skill, true);
			rebirthText = new CreatureSay(0, 18, player.getName(), "Rebirthing has granted you [" + skill.getName() + "] level " + skill.getLevel() + "!");
			player.sendPacket(rebirthText);
		}
	}
	
	/** Return the player's current Rebirth Level */
	public static int getRebirthLevel(L2PcInstance player)
	{
		int playerId = player.getObjectId();
		if (_playersRebirthInfo.get(playerId) == null)
		{
			loadRebirthInfo(player);
		}
		return _playersRebirthInfo.get(playerId);
	}
	
	private static int getSkillLevel(L2PcInstance player, int skillId)
	{
		int lvl = player.getSkillLevel(skillId);
		if (lvl == -1)
			return 1;
		return lvl + 1;
	}
	
	/**
	 * Database caller to retrieve player's current Rebirth Level
	 * 
	 * @return
	 */
	public static int loadRebirthInfo(L2PcInstance player)
	{
		int playerId = player.getObjectId();
		int rebirthCount = 0;
		java.sql.Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM rebirth_manager WHERE playerId = ?");
			statement.setInt(1, playerId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				rebirthCount = rset.getInt("rebirthCount");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		_playersRebirthInfo.put(playerId, rebirthCount);
		return rebirthCount;
	}
	
	public static int getRebirthCount(L2PcInstance player)
	{
		int playerId = player.getObjectId();
		java.sql.Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `rebirth_manager` WHERE playerId = ?");
			statement.setInt(1, playerId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				playerRebirthCount = rset.getInt("rebirthCount");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return playerRebirthCount;
	}
	
	/** Stores the player's information in the DB. */
	public void storePlayerBirth(L2PcInstance player)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `rebirth_manager` (playerId,rebirthCount) VALUES (?,1)");
			statement.setInt(1, player.getObjectId());
			statement.execute();
			_playersRebirthInfo.put(player.getObjectId(), 1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
	
	/** Updates the player's information in the DB. */
	public void updatePlayerBirth(L2PcInstance player, int newRebirthCount)
	{
		java.sql.Connection con = null;
		try
		{
			int playerId = player.getObjectId();
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE `rebirth_manager` SET rebirthCount = ? WHERE playerId = ?");
			statement.setInt(1, newRebirthCount);
			statement.setInt(2, playerId);
			statement.execute();
			_playersRebirthInfo.put(playerId, newRebirthCount);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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