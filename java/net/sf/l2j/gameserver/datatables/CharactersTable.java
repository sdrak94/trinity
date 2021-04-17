/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Hero;

/**
 * @author Nik
 */
public class CharactersTable
{
	private static Logger _log = Logger.getLogger(CharactersTable.class.getName());
	
	public static class OfflinePlayerData
	{
		public String accountName;
		public String char_name;
		public String title = "";
		public int sex;
		public int pvpKills;
		public int activeClassId;
		public int baseClassId;
		public int pkKills;
		public int sevenSignsSide = 0;
		public int recomHave;
		public int fame;
		public int clanId;
		public int allyId;
		public int level;
		public int sp;
		public int karma;
		public int subpledge;
		public String clanName = "";
		public String allyName = "";
		public boolean isClanLeader;
		public boolean isNoble;
		public boolean isHero;
		public long onlineTime;
		public long adenaCount = 0L;
		public long lastAccess = 0L;
		public long exp = 0L;
		public long clanJoinPenaltyExpiryTime;
		public List<SubClass> subClasses = new ArrayList<>(Config.MAX_SUBCLASS);
		//public final Calendar createDate = Calendar.getInstance();
	}
	
	public static OfflinePlayerData getOfflinePlayerData(int playerId)
	{
		OfflinePlayerData data = new OfflinePlayerData();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM characters WHERE charId = ?"))
			{
				statement.setInt(1, playerId);
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.activeClassId = rset.getInt("classid");
						data.sex = rset.getInt("sex");
						// final L2PcTemplate template = PlayerTemplateData.getInstance().getTemplate(data.activeClassId);
						// PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), data.sex == Sex.FEMALE);
						
						data.char_name = rset.getString("char_name");
						data.lastAccess = rset.getLong("lastAccess");
						
						data.exp = rset.getLong("exp");
						// rset.getLong("expBeforeDeath");
						data.level = rset.getByte("level");
						data.sp = rset.getInt("sp");
						
						// rset.getInt("wantspeace");
						// rset.getInt("heading");
						
						data.karma = rset.getInt("karma");
						data.fame = rset.getInt("fame");
						data.pvpKills = rset.getInt("pvpkills");
						data.pkKills = rset.getInt("pkkills");
						data.onlineTime = rset.getLong("onlinetime");
						// rset.getInt("newbie");
						data.isNoble = rset.getInt("nobless") == 1;
						
						data.clanJoinPenaltyExpiryTime = rset.getLong("clan_join_expiry_time");
						// rset.getLong("clan_create_expiry_time");
						
						data.clanId = rset.getInt("clanid");
						// rset.getInt("power_grade");
						data.subpledge = rset.getInt("subpledge");
						// player.setApprentice(rset.getInt("apprentice"));
						
						data.isHero = Hero.getInstance().getHeroes().containsKey(playerId);
						
						// rset.getLong("deletetime");
						data.title = rset.getString("title");
						// rset.getInt("accesslevel");
						// int titleColor = rset.getInt("title_color");
						// rset.getDouble("curHp");
						// rset.getDouble("curCp");
						// rset.getDouble("curMp");
						
						data.baseClassId = rset.getInt("base_class");
						
						// rset.getInt("apprentice");
						// rset.getInt("sponsor"));
						// rset.getInt("lvl_joined_academy");
						// rset.getInt("isin7sdungeon") == 1;
						// rset.getInt("death_penalty_level");
						// rset.getInt("vitality_points");
						// rset.getInt("x"), rset.getInt("y"), rset.getInt("z");
						// rset.getInt("BookmarkSlot");
						//data.createDate.setTime(rset.getDate("createDate"));
						// rset.getString("language");
					}
				}
			}
			
			if (data.clanId > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(data.clanId);
				data.clanName = clan.getName();
				data.isClanLeader = clan.getLeaderId() == playerId;
				data.allyId = clan.getAllyId();
				data.allyName = clan.getAllyName();
			}
			
			try (PreparedStatement statement = con.prepareStatement("SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC"))
			{
				statement.setInt(1, playerId);
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						SubClass subClass = new SubClass();
						subClass.setClassId(rset.getInt("class_id"));
						subClass.setLevel(rset.getByte("level"));
						subClass.setExp(rset.getLong("exp"));
						subClass.setSp(rset.getInt("sp"));
						subClass.setClassIndex(rset.getInt("class_index"));
						data.subClasses.add(subClass);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not restore classes for " + playerId + ": " + e.getMessage(), e);
			}
			
			try (PreparedStatement statement = con.prepareStatement("SELECT rec_have FROM characters WHERE charId=? LIMIT 1"))
			{
				statement.setInt(1, playerId);
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.recomHave = rset.getInt("rec_have");
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not restore reccomends for " + playerId + ": " + e.getMessage(), e);
			}
			
			try (PreparedStatement st = con.prepareStatement("SELECT cabal FROM seven_signs WHERE charId = ?"))
			{
				st.setInt(1, playerId);
				try (ResultSet rset = st.executeQuery())
				{
					if (rset.next())
					{
						String playerCabal = rset.getString("cabal");
						if (playerCabal.equalsIgnoreCase("dawn"))
						{
							data.sevenSignsSide = SevenSigns.CABAL_DAWN;
						}
						else if (playerCabal.equalsIgnoreCase("dusk"))
						{
							data.sevenSignsSide = SevenSigns.CABAL_DUSK;
						}
						else
						{
							data.sevenSignsSide = SevenSigns.CABAL_NULL;
						}
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement("SELECT count FROM `items` where `owner_id` = ? AND item_id=?"))
			{
				statement.setInt(1, playerId);
				statement.setInt(2, 57);
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.adenaCount = rset.getLong("count");
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error in getOfflinePlayerData:", e);
		}
		
		return data;
	}
	
	public static class CharacterLoginData
	{
		public static final String SELECT_QUERY = "SELECT (obj_id, date, ip, hwid, onlinetime, log) FROM character_logindata ORDER BY `date` DESC";
		public static final String DELETE_BY_DATE_QUERY = "DELETE FROM character_logindata WHERE date < ?";
		
		private static final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		private static final SimpleDateFormat format2 = new SimpleDateFormat("HH:mm");
		
		private final boolean canChangeData; // Used to mark if this is loaded from 3rd party file for scanning, or its an actual in use data.
		private final int objId;
		private final long loginDate;
		private final String ip;
		private final String hwid;
		private long onlineTime;
		private String log;
		
		/**
		 * Creates a new loginData for the specified player to be saved in the database.
		 * @param player
		 */
		public CharacterLoginData(L2PcInstance player)
		{
			canChangeData = true;
			objId = player.getObjectId();
			loginDate = System.currentTimeMillis();
			ip = String.valueOf(player.getIP());
			hwid = "";
			log = "";
			insertData();
		}
		
		public CharacterLoginData(int objId, long loginDate, String ip, String hwid, long onlineTime, String log)
		{
			canChangeData = false;
			this.objId = objId;
			this.loginDate = loginDate;
			this.ip = ip == null ? "N/A" : ip;
			this.hwid = hwid == null ? "N/A" : hwid;
			this.onlineTime = onlineTime;
			this.log = log == null ? "" : log;
		}
		
		private void insertData()
		{
			if (canChangeData)
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("INSERT INTO character_logindata (obj_id, date, ip, hwid, onlinetime, log) VALUES (?, ?, ?, ?, ?, ?)"))
				{
					statement.setInt(1, objId);
					statement.setLong(2, loginDate);
					statement.setString(3, ip);
					statement.setString(4, hwid);
					statement.setLong(5, onlineTime);
					statement.setString(6, log);
					statement.execute();
				}
				catch (SQLException e)
				{
					_log.log(Level.WARNING, "Error while inserting logindata info: ", e);
				}
			}
		}
		
		/**
		 * @return The objectId of the player owner.
		 */
		public int getObjectId()
		{
			return objId;
		}
		
		/**
		 * @return The unix date in milisec that this player has logged in.
		 */
		public long getLoginDateMilis()
		{
			return loginDate;
		}
		
		/**
		 * @return The date that this player has logged in.
		 */
		public String getLoginDate()
		{
			return format.format(new Date(loginDate));
		}
		
		/**
		 * @return The ip from which this player has logged in.
		 */
		public String getIP()
		{
			return ip;
		}
		
		/**
		 * @return The hwid from which this player has logged in.
		 */
		public String getHWID()
		{
			return hwid;
		}
		
		/**
		 * Updates the onlinetime of this player and saves to database.
		 */
		public void updateOnlineTime()
		{
			if (!canChangeData)
			{
				return;
			}
			
			onlineTime = System.currentTimeMillis() - loginDate;
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE `character_logindata` SET `onlinetime` = ? WHERE `obj_id` = ? AND `date` = ? LIMIT 1"))
			{
				statement.setLong(1, onlineTime);
				statement.setInt(2, objId);
				statement.setLong(3, loginDate);
				statement.execute();
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "CharacterLoginData: Failed to update onlineTime. objId=" + objId + " loginDate=" + loginDate + " onlineTime=" + onlineTime, e);
			}
		}
		
		/**
		 * @return The time player has been online in miliseconds.
		 */
		public long getOnlineTimeMilis()
		{
			return onlineTime;
		}
		
		public String getLog(int maxLogs)
		{
			String result = "";
			try
			{
				result = log;
				int index = 0;
				int indexEnd = 0;
				while ((maxLogs > 0) && ((index = result.indexOf('[', index)) >= 0) && ((indexEnd = result.indexOf('[', indexEnd)) >= 0))
				{
					maxLogs--;
					index++; // Skip the [ char.
					String date = result.substring(index, indexEnd);
					indexEnd++; // Skip the ] char to not take the same index.
					
					String dateString = format2.format(new Date(Long.parseLong(date)));
					result.replaceFirst(date, dateString);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return result;
		}
		
		/**
		 * @return The time player has been online in HH-MM-SS.
		 */
		public String getOnlineTime()
		{
			final long remainingTime = onlineTime / 1000;
			final int hours = (int) (remainingTime / 3600);
			final int minutes = (int) ((remainingTime % 3600) / 60);
			final int seconds = (int) ((remainingTime % 3600) % 60);
			
			StringBuilder sb = new StringBuilder(10);
			if (hours > 0)
			{
				sb.append(hours).append("H");
			}
			if ((minutes > 0) || (hours > 0))
			{
				sb.append(" ").append(minutes).append("m");
			}
			if ((seconds > 0) || (minutes > 0) || (hours > 0))
			{
				sb.append(" ").append(seconds).append("s");
			}
			if (sb.length() == 0)
			{
				sb.append("none");
			}
			
			return sb.toString();
		}
		
		/**
		 * Each new append is seperated by [dateInMilis]. Saves the log to the database.
		 * @param appendToLog
		 * @return if the update is successfully added to database
		 */
		public boolean updateLog(String appendToLog)
		{
			if (canChangeData)
			{
				return false;
			}
			
			if (log == null)
			{
				log = "";
			}
			
			// Will fuck up database
			if ((log.length() + appendToLog.length()) > 65535)
			{
				return false;
			}
			
			log += "[" + System.currentTimeMillis() + "]";
			log += appendToLog;
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE `character_logindata` SET `log` = ? WHERE `obj_id` = ? AND `date` = ? LIMIT 1"))
			{
				statement.setString(1, log);
				statement.setInt(2, objId);
				statement.setLong(3, loginDate);
				statement.execute();
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "CharacterLoginData: Failed to update log. objId=" + objId + " loginDate=" + loginDate + " log=" + log, e);
				return false;
			}
			
			return true;
		}
	}
	
	public static List<CharacterLoginData> getAllLoginData(int objId, int... limit)
	{
		List<CharacterLoginData> result = new LinkedList<>();
		String query = "SELECT * FROM character_logindata WHERE obj_id = ? ORDER BY `date` DESC"; // LIMIT 0, 1000
		if (limit != null)
		{
			if (limit.length > 1)
			{
				query += " LIMIT " + limit[0] + ", " + limit[limit.length - 1];
			}
			else if (limit.length == 1)
			{
				query += " LIMIT 0, " + limit[0];
			}
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(query))
		{
			st.setInt(1, objId);
			ResultSet rset = st.executeQuery();
			while (rset.next())
			{
				final long loginDate = rset.getLong("date");
				final String ip = rset.getString("ip");
				final String hwid = rset.getString("hwid");
				final long onlineTime = rset.getLong("onlinetime");
				final String log = rset.getString("log");
				
				result.add(new CharacterLoginData(objId, loginDate, ip, hwid, onlineTime, log));
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Failed getting login data for objId: " + objId, e);
		}
		
		return result;
	}
}
