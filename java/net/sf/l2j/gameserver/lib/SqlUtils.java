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
package net.sf.l2j.gameserver.lib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;

public class SqlUtils
{
	private static Logger _log = Logger.getLogger(SqlUtils.class.getName());
	
	private SqlUtils()
	{
	}
	
	// =========================================================
	// Property - Public
	public static SqlUtils getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// =========================================================
	// Method - Public
	public static Integer getIntValue(String resultField, String tableName, String whereClause)
	{
		String query = "";
		Integer res = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[] { resultField }, tableName, whereClause, true);
			
			PreparedStatement statement = con.prepareStatement(query);
			ResultSet rset = statement.executeQuery();
			
			if (rset.next())
				res = rset.getInt(1);
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error in query '" + query + "':" + e);
			e.printStackTrace();
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
		
		return res;
	}
	
	public static Integer[] getIntArray(String resultField, String tableName, String whereClause)
	{
		String query = "";
		Integer[] res = null;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[] { resultField }, tableName, whereClause, false);
			PreparedStatement statement = con.prepareStatement(query);
			ResultSet rset = statement.executeQuery();
			
			int rows = 0;
			
			while (rset.next())
				rows++;
			
			if (rows == 0)
				return new Integer[0];
			
			res = new Integer[rows - 1];
			
			rset.first();
			
			int row = 0;
			while (rset.next())
			{
				res[row] = rset.getInt(1);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
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
		
		return res;
	}
	
	public static Integer[][] get2DIntArray(String[] resultFields, String usedTables, String whereClause)
	{
		long start = System.currentTimeMillis();
		
		String query = "";
		
		Connection con = null;
		
		Integer res[][] = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			query = L2DatabaseFactory.getInstance().prepQuerySelect(resultFields, usedTables, whereClause, false);
			PreparedStatement statement = con.prepareStatement(query);
			ResultSet rset = statement.executeQuery();
			
			int rows = 0;
			while (rset.next())
				rows++;
			
			res = new Integer[rows - 1][resultFields.length];
			
			rset.first();
			
			int row = 0;
			while (rset.next())
			{
				for (int i = 0; i < resultFields.length; i++)
					res[row][i] = rset.getInt(i + 1);
				row++;
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error in query '" + query + "':" + e);
			e.printStackTrace();
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
		
		_log.fine("Get all rows in query '" + query + "' in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SqlUtils _instance = new SqlUtils();
	}
}
