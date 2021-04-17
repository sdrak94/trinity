package net.sf.l2j.gameserver.model.base;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Nik
 */
public class PlayerCounters
{
	public static final Logger		_log						= Logger.getLogger(PlayerCounters.class.getName());
	public static PlayerCounters	DUMMY_COUNTER				= new PlayerCounters(null);
	// Custom Counters
	// events Played
	public volatile int				tvtPlayed					= 0;												// implemented
	public volatile int				ctfPlayed					= 0;												// implemented
	public volatile int				dmPlayed					= 0;												// implemented
	public volatile int				domiPlayed					= 0;												// implemented
	public volatile int				hgPlayed					= 0;												// implemented
	public volatile int				zombiePlayed				= 0;
	public volatile int				lmsPlayed					= 0;
	public volatile int				ltsPlayed					= 0;
	public volatile int				koreanPlayed				= 0;
	public volatile int				siegeEvPlayed				= 0;												// implemented
	// events Won
	public volatile int				tvtWon						= 0;												// implemented
	public volatile int				ctfWon						= 0;												// implemented
	public volatile int				dmWon						= 0;												// implemented
	public volatile int				domiWon						= 0;												// implemented
	public volatile int				hgWon						= 0;												// implemented
	public volatile int				zombieWon					= 0;
	public volatile int				lmsWon						= 0;
	public volatile int				ltsWon						= 0;
	public volatile int				koreanWon					= 0;
	public volatile int				siegeEvWon					= 0;												// implemented
	// events RegFirst
	public volatile int				tvtReg						= 0;												// implemented
	public volatile int				ctfReg						= 0;												// implemented
	public volatile int				dmReg						= 0;												// implemented
	public volatile int				domiReg						= 0;												// implemented
	public volatile int				hgReg						= 0;												// implemented
	public volatile int				zombieReg					= 0;
	public volatile int				lmsReg						= 0;
	public volatile int				ltsReg						= 0;
	public volatile int				koreanReg					= 0;
	public volatile int				siegeEvReg					= 0;												// implemented
	// events Score
	public volatile int				ctfScore					= 0;												// implemented
	public volatile int				siegeEvScore				= 0;												// NOT implemented
	// instances
	public volatile int				soloDone					= 0;												// implemented
	public volatile int				kamaDone					= 0;												// implemented
	public volatile int				embryoDone					= 0;												// implemented
	public volatile int				adenDone					= 0;												// implemented
	// boxes
	public volatile int				bonanzoOpened				= 0;												// implemented
	public volatile int				capsulesOpened				= 0;												// implemented
	public volatile int				lootBoxesOpened				= 0;												// implemented
	// level
	public volatile int				maxPlayerLevelReach			= 0;												// implemented
	public volatile int				rebirth						= 0;												// implemented
	// ench
	public volatile int				titWeaponEnch				= 0;												// implemented
	public volatile int				dreadWeaponEnch				= 0;												// implemented
	public volatile int				uniqueWeaponEnch			= 0;												// implemented
	public volatile int				relicJewEnch				= 0;												// implemented
	public volatile int				relicJew15EnchTimes			= 0;												// implemented
	// Items
	public volatile int				talismansBought				= 0;												// implemented
	public volatile int				haxedItems					= 0;												// implemented
	public volatile int				gemedItems					= 0;												// implemented
	public volatile int				dressUp						= 0;												// NOT implemented yet
	// Custom Counters end
	// Player
	public volatile int				pvpKills					= 0;
	public volatile int				pkInARowKills				= 0;
	public volatile int				highestKarma				= 0;
	public volatile int				timesDied					= 0;
	public volatile int				playersRessurected			= 0;
	public volatile int				duelsWon					= 0;
	public volatile int				fameAcquired				= 0;
	public volatile long			expAcquired					= 0;
	public volatile int				recipesSucceeded			= 0;
	public volatile int				recipesFailed				= 0;
	public volatile int				manorSeedsSow				= 0;
	public volatile int				fishCaught					= 0;
	public volatile int				treasureBoxesOpened			= 0;
	public volatile int				unrepeatableQuestsCompleted	= 0;
	public volatile int				repeatableQuestsCompleted	= 0;
	public volatile long			adenaDestroyed				= 0;
	public volatile int				recommendsMade				= 0;
	public volatile int				foundationItemsMade			= 0;
	public volatile long			distanceWalked				= 0;
	// Enchants
	public volatile int				enchantNormalSucceeded		= 0;
	public volatile int				enchantBlessedSucceeded		= 0;
	public volatile int				highestEnchant				= 0;
	public volatile int				maxSoulCrystalLevel			= 0;												// Not done in quest. Its in python.
	// Clan & Olympiad
	public volatile int				olyHiScore					= 0;
	public volatile int				olyGamesWon					= 0;
	public volatile int				olyGamesLost				= 0;
	public volatile int				timesNoble					= 0;												// Not done in quest. Its in python. But its done in another place.
	public volatile int				timesHero					= 0;
	public volatile int				timesMarried				= 0;
	public volatile int				castleSiegesWon				= 0;
	public volatile int				fortSiegesWon				= 0;
	// Monsters
	public volatile int				horrorKilled				= 0;												// 96020
	public volatile int				holyKnightKilled			= 0;												// 96019
	public volatile int				majinHorrorKilled			= 0;												// 800001
	public volatile int				majinOblivionKilled			= 0;												// 800000
	public volatile int				pusKilled					= 0;												// 95103
	public volatile int				titaniumDreadKilled			= 0;												// 960180
	public volatile int				glabKilled					= 0;												// 95609
	// Epic Bosses.
	public volatile int				antharasKilled				= 0;
	public volatile int				baiumKilled					= 0;
	public volatile int				valakasKilled				= 0;
	public volatile int				orfenKilled					= 0;
	public volatile int				antQueenKilled				= 0;
	public volatile int				coreKilled					= 0;
	public volatile int				belethKilled				= 0;
	public volatile int				sailrenKilled				= 0;
	public volatile int				baylorKilled				= 0;
	public volatile int				zakenKilled					= 0;
	public volatile int				tiatKilled					= 0;
	public volatile int				freyaKilled					= 0;
	public volatile int				frintezzaKilled				= 0;
	// Other kills
	public volatile int				mobsKilled					= 0;
	public volatile int				raidsKilled					= 0;
	public volatile int				championsKilled				= 0;
	public volatile int				townGuardsKilled			= 0;
	public volatile int				siegeGuardsKilled			= 0;
	public volatile int				playersKilledInSiege		= 0;
	public volatile int				timesVoted					= 0;
	// Here comes the code...
	private int						_playerObjId				= 0;
	
	public PlayerCounters(L2PcInstance activeChar)
	{
		_playerObjId = activeChar == null ? 0 : activeChar.getObjectId();
	}
	
	public PlayerCounters(int playerObjId)
	{
		_playerObjId = playerObjId;
	}
	
	public long getPoints(String fieldName)
	{
		try
		{
			return getClass().getField(fieldName).getLong(this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	
	public void save()
	{
		// Because im SQL noob
		try (Connection con2 = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement2 = con2.prepareStatement("SELECT char_id FROM character_counters WHERE char_id = " + _playerObjId + ";"); PreparedStatement statement3 = con2.prepareStatement("INSERT INTO character_counters (char_id) values (?);"); ResultSet rs = statement2.executeQuery();)
		{
			if (!rs.next())
			{
				statement3.setInt(1, _playerObjId);
				statement3.execute();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE character_counters SET ");
			boolean firstPassed = false;
			for (Field field : getClass().getFields())
			{
				switch (field.getName())
				// Fields that we wont save.
				{
					case "_log":
					case "_activeChar":
					case "_playerObjId":
					case "DUMMY_COUNTER":
						continue;
				}
				if (firstPassed)
				{
					sb.append(",");
				}
				sb.append(field.getName());
				sb.append("=");
				try
				{
					sb.append(field.getInt(this));
				}
				catch (IllegalArgumentException | IllegalAccessException | SecurityException e)
				{
					sb.append(field.getLong(this));
				}
				firstPassed = true;
			}
			sb.append(" WHERE char_id=" + _playerObjId + ";");
			try (PreparedStatement statement = con.prepareStatement(sb.toString());)
			{
				statement.execute();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM character_counters WHERE char_id = ?");)
		{
			statement.setInt(1, _playerObjId);
			try (ResultSet rs = statement.executeQuery();)
			{
				while (rs.next())
				{
					for (Field field : getClass().getFields())
					{
						switch (field.getName())
						// Fields that we dont use here.
						{
							case "_log":
							case "_activeChar":
							case "_playerObjId":
							case "DUMMY_COUNTER":
								continue;
						}
						try
						{
							field.setInt(this, rs.getInt(field.getName()));
						}
						catch (SQLException sqle)
						{
							field.setLong(this, rs.getLong(field.getName()));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static String generateTopHtml(String fieldName, int maxTop, boolean asc)
	{
		Map<Integer, Long> tops = loadCounter(fieldName, maxTop, asc);
		int order = 1;
		StringBuilder sb = new StringBuilder(tops.size() * 100);
		sb.append("<table width=300 border=0>");
		for (Entry<Integer, Long> top : tops.entrySet())
		{
			sb.append("<tr><td><table border=0 width=294 bgcolor=" + ((order % 2) == 0 ? "1E1E1E" : "090909") + ">").append("<tr><td fixwidth=10%><font color=LEVEL>").append(order++).append(".<font></td>").append("<td fixwidth=45%>").append(CharNameTable.getInstance().getNameById(top.getKey())).append("</td><td fixwidth=45%><font color=777777>").append(top.getValue()).append("</font></td></tr>").append("</table></td></tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}
	
	public static Map<Integer, Long> loadCounter(String fieldName, int maxRetrieved, boolean asc)
	{
		Map<Integer, Long> ret = null;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT char_id, " + fieldName + " FROM character_counters ORDER BY " + fieldName + " " + (asc ? "ASC" : "DESC") + " LIMIT 0, " + maxRetrieved + ";");)
		{
			try (ResultSet rs = statement.executeQuery();)
			{
				ret = new LinkedHashMap<>(rs.getFetchSize());
				while (rs.next())
				{
					int charObjId = rs.getInt(1);
					long value = rs.getLong(2);
					ret.put(charObjId, value);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return ret == null ? Collections.emptyMap() : ret;
	}
	
	public static void checkTable()
	{
		// Generate used fields list.
		List<String> fieldNames = new ArrayList<>();
		for (Field field : PlayerCounters.class.getFields())
		{
			switch (field.getName())
			// Fields that we dont use here.
			{
				case "_log":
				case "_activeChar":
				case "_playerObjId":
				case "DUMMY_COUNTER":
					continue;
				default:
					fieldNames.add(field.getName());
			}
		}
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("DESC character_counters"); ResultSet rs = statement.executeQuery();)
		{
			while (rs.next())
			{
				// _log.info("Checking column: " + rs.getString(1));
				fieldNames.remove(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (!fieldNames.isEmpty())
		{
			StringBuilder sb = new StringBuilder(fieldNames.size() * 30);
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				sb.append("ALTER TABLE character_counters");
				for (String str : fieldNames)
				{
					_log.info("PlayerCounters Update: Adding missing column name: " + str);
					Class<?> fieldType = PlayerCounters.class.getField(str).getType();
					if ((fieldType == int.class) || (fieldType == Integer.class))
					{
						sb.append(" ADD COLUMN " + str + " int(11) NOT NULL DEFAULT 0,");
					}
					else if ((fieldType == long.class) || (fieldType == Long.class))
					{
						sb.append(" ADD COLUMN " + str + " bigint(20) NOT NULL DEFAULT 0,");
					}
					else
					{
						_log.log(Level.WARNING, "Unsupported data type: " + fieldType);
					}
				}
				sb.setCharAt(sb.length() - 1, ';');
				try (PreparedStatement statement = con.prepareStatement(sb.toString());)
				{
					statement.execute();
					_log.info("PlayerCounters Update: Changes executed!");
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
