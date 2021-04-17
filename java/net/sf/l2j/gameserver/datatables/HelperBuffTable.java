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
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.templates.L2HelperBuff;
import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class represents the Newbie Helper Buff list
 *
 * Author: Ayor
 *
 */

public class HelperBuffTable
{
	
	private static Logger _log = Logger.getLogger(HennaTable.class.getName());
	
	/** The table containing all Buff of the Newbie Helper */
	private List<L2HelperBuff> _helperBuff;
	
	/** The player level since Newbie Helper can give the fisrt buff <BR>
	 *  Used to generate message : "Come back here when you have reached level ...") */
	private int _magicClassLowestLevel = 100;
	private int _physicClassLowestLevel = 100;
	
	/** The player level above which Newbie Helper won't give any buff <BR>
	 *  Used to generate message : "Only novice character of level ... or less can receive my support magic.") */
	private int _magicClassHighestLevel = 1;
	private int _physicClassHighestLevel = 1;
	
	private int _servitorLowestLevel = 100;
	
	private int _servitorHighestLevel = 1;
	
	public static HelperBuffTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Create and Load the Newbie Helper Buff list from SQL Table helper_buff_list
	 */
	private HelperBuffTable()
	{
		_helperBuff = new FastList<L2HelperBuff>();
		restoreHelperBuffData();
		
	}
	
	/**
	 * Read and Load the Newbie Helper Buff list from SQL Table helper_buff_list
	 */
	private void restoreHelperBuffData()
	{
		Connection con = null;
		try
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM helper_buff_list");
				ResultSet helperbuffdata = statement.executeQuery();
				
				fillHelperBuffTable(helperbuffdata);
				helperbuffdata.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.severe("Table helper_buff_list not found : Update your DataPack" + e);
				e.printStackTrace();
			}
			
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
	}
	
	/**
	 * Load the Newbie Helper Buff list from SQL Table helper_buff_list
	 */
	private void fillHelperBuffTable(ResultSet HelperBuffData) throws Exception
	{
		
		while (HelperBuffData.next())
		{
			StatsSet helperBuffDat = new StatsSet();
			int id = HelperBuffData.getInt("id");
			
			helperBuffDat.set("id", id);
			helperBuffDat.set("skillID", HelperBuffData.getInt("skill_id"));
			helperBuffDat.set("skillLevel", HelperBuffData.getInt("skill_level"));
			helperBuffDat.set("lowerLevel", HelperBuffData.getInt("lower_level"));
			helperBuffDat.set("upperLevel", HelperBuffData.getInt("upper_level"));
			helperBuffDat.set("isMagicClass", HelperBuffData.getString("is_magic_class"));
			helperBuffDat.set("forSummon", HelperBuffData.getString("forSummon"));
			
			// Calulate the range level in wich player must be to obtain buff from Newbie Helper
			if ("false".equals(HelperBuffData.getString("is_magic_class")))
			{
				if (HelperBuffData.getInt("lower_level") < _physicClassLowestLevel)
					_physicClassLowestLevel = HelperBuffData.getInt("lower_level");
				
				if (HelperBuffData.getInt("upper_level") > _physicClassHighestLevel)
					_physicClassHighestLevel = HelperBuffData.getInt("upper_level");
			}
			else
			{
				if (HelperBuffData.getInt("lower_level") < _magicClassLowestLevel)
					_magicClassLowestLevel = HelperBuffData.getInt("lower_level");
				
				if (HelperBuffData.getInt("upper_level") > _magicClassHighestLevel)
					_magicClassHighestLevel = HelperBuffData.getInt("upper_level");
			}
			if ("true".equals(HelperBuffData.getString("forSummon")))
			{
				if (HelperBuffData.getInt("lower_level") < _servitorLowestLevel)
					_servitorLowestLevel = HelperBuffData.getInt("lower_level");
				
				if (HelperBuffData.getInt("upper_level") > _servitorHighestLevel)
					_servitorHighestLevel = HelperBuffData.getInt("upper_level");
			}
			// Add this Helper Buff to the Helper Buff List
			L2HelperBuff template = new L2HelperBuff(helperBuffDat);
			_helperBuff.add(template);
		}
		
		_log.config("Helper Buff Table: Loaded " + _helperBuff.size() + " Templates.");
		
	}
	
	/**
	 * Return the Helper Buff List
	 */
	public List<L2HelperBuff> getHelperBuffTable()
	{
		return _helperBuff;
	}
	
	/**
	 * @return Returns the magicClassHighestLevel.
	 */
	public int getMagicClassHighestLevel()
	{
		return _magicClassHighestLevel;
	}
	
	/**
	 * @return Returns the magicClassLowestLevel.
	 */
	public int getMagicClassLowestLevel()
	{
		return _magicClassLowestLevel;
	}
	
	/**
	 * @return Returns the physicClassHighestLevel.
	 */
	public int getPhysicClassHighestLevel()
	{
		return _physicClassHighestLevel;
	}
	
	/**
	 * @return Returns the physicClassLowestLevel.
	 */
	public int getPhysicClassLowestLevel()
	{
		return _physicClassLowestLevel;
	}
	
	/**
	 * @return Returns the servitorLowestLevel.
	 */
	public int getServitorLowestLevel()
	{
		return _servitorLowestLevel;
	}
	
	/**
	  @return Returns the servitorHighestLevel.
	 */
	public int getServitorHighestLevel()
	{
		return _servitorHighestLevel;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HelperBuffTable _instance = new HelperBuffTable();
	}
}
