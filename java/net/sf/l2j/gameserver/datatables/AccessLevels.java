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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2AccessLevel;

/**
 * @author FBIagent<br>
 */
public class AccessLevels
{
	/** The logger<br> */
	private static Logger _log = Logger.getLogger(AccessLevels.class.getName());
	/** Reserved master access level<br> */
	public static final int _masterAccessLevelNum = Config.MASTERACCESS_LEVEL;
	/** The master access level which can use everything<br> */
	public static L2AccessLevel _masterAccessLevel = new L2AccessLevel(_masterAccessLevelNum, "Master Access", Config.MASTERACCESS_NAME_COLOR, Config.MASTERACCESS_TITLE_COLOR, null, true, true, true, true, true, true, true, true);
	/** Reserved user access level<br> */
	public static final int _userAccessLevelNum = 0;
	/** The user access level which can do no administrative tasks<br> */
	public static L2AccessLevel _userAccessLevel = new L2AccessLevel(_userAccessLevelNum, "User", Integer.decode("0xFFFFFF"), Integer.decode("0xFFFFFF"), null, false, false, false, true, false, true, true, true);
	/** FastMap of access levels defined in database<br> */
	private TIntObjectHashMap<L2AccessLevel> _accessLevels;
	
	/**
	 * Returns the one and only instance of this class<br><br>
	 * 
	 * @return AccessLevels: the one and only instance of this class<br>
	 */
	public static AccessLevels getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private AccessLevels()
	{
		loadAccessLevels();
	}
	
	/**
	 * Loads the access levels from database<br>
	 */
	private void loadAccessLevels()
	{
		_accessLevels = new TIntObjectHashMap<L2AccessLevel>();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM `access_levels` ORDER BY `accessLevel` DESC");
			ResultSet rset = stmt.executeQuery();
			int accessLevel = 0;
			String name = null;
			int nameColor = 0;
			int titleColor = 0;
			String childs = null;
			boolean isGm = false;
			boolean allowPeaceAttack = false;
			boolean allowFixedRes = false;
			boolean allowTransaction = false;
			boolean allowAltG = false;
			boolean giveDamage = false;
			boolean takeAggro = false;
			boolean gainExp = false;
			
			while (rset.next())
			{
				accessLevel = rset.getInt("accessLevel");
				name = rset.getString("name");
				
				if (accessLevel == _userAccessLevelNum)
				{
					_log.warning("AccessLevels: Access level with name " + name + " is using reserved user access level "
							+ _userAccessLevelNum + ". Ignoring it!");
					continue;
				}
				else if (accessLevel == _masterAccessLevelNum)
				{
					_log.warning("AccessLevels: Access level with name " + name + " is using reserved master access level "
							+ _masterAccessLevelNum + ". Ignoring it!");
					continue;
				}
				else if (accessLevel < 0)
				{
					_log.warning("AccessLevels: Access level with name " + name
							+ " is using banned access level state(below 0). Ignoring it!");
					continue;
				}
				
				try
				{
					nameColor = Integer.decode("0x" + rset.getString("nameColor"));
				}
				catch (NumberFormatException nfe)
				{
					try
					{
						nameColor = Integer.decode("0xFFFFFF");
					}
					catch (NumberFormatException nfe2)
					{
						
					}
				}
				
				try
				{
					titleColor = Integer.decode("0x" + rset.getString("titleColor"));
					
				}
				catch (NumberFormatException nfe)
				{
					try
					{
						titleColor = Integer.decode("0x77FFFF");
					}
					catch (NumberFormatException nfe2)
					{
						
					}
				}
				
				childs = rset.getString("childAccess");
				isGm = rset.getBoolean("isGm");
				allowPeaceAttack = rset.getBoolean("allowPeaceAttack");
				allowFixedRes = rset.getBoolean("allowFixedRes");
				allowTransaction = rset.getBoolean("allowTransaction");
				allowAltG = rset.getBoolean("allowAltg");
				giveDamage = rset.getBoolean("giveDamage");
				takeAggro = rset.getBoolean("takeAggro");
				gainExp = rset.getBoolean("gainExp");
				
				_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, name, nameColor, titleColor, childs.isEmpty() ? null : childs, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp));
			}
			
			rset.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("AccessLevels: Error loading from database:" + e);
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
		_log.info("AccessLevels: Loaded " + _accessLevels.size() + " from database.");
	}
	
	/**
	 * Returns the access level by characterAccessLevel<br><br>
	 * 
	 * @param accessLevelNum as int<br><br>
	 *
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public L2AccessLevel getAccessLevel(int accessLevelNum)
	{
		L2AccessLevel accessLevel = null;
		
		synchronized (_accessLevels)
		{
			accessLevel = _accessLevels.get(accessLevelNum);
		}
		return accessLevel;
	}
	
	public void addBanAccessLevel(int accessLevel)
	{
		synchronized (_accessLevels)
		{
			if (accessLevel > -1)
			{
				return;
			}
			
			_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, "Banned", Integer.decode("0x000000"), Integer.decode("0x000000"), null, false, false, false, false, false, false, false, false));
		}
	}
	
	public void reloadAccessLevels()
	{
		loadAccessLevels();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final AccessLevels _instance = new AccessLevels();
	}
}