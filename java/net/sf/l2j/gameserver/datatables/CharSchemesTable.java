package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;

/**
 * This class stores players' buff schemes into FastMap. On player login, his scheme
 * is loaded and on server shutdown all modified schemes are saved to DataBase. This avoids
 * too many unnecessary DataBase connections and queries. If server crashes, nothing important 
 * is lost :)
 *
 * @author  House
 */
public class CharSchemesTable
{
	private static FastMap<Integer, FastMap<String, FastList<Integer>>> _schemesTable;	
	private static CharSchemesTable _instance = null;
	private boolean _initialized = true;
	
	private static Logger _log = Logger.getLogger(CharSchemesTable.class.getName());
	
	private static final String SQL_LOAD_SCHEME = "SELECT * FROM mods_buffer_schemes WHERE ownerId=?";
	private static final String SQL_DELETE_SCHEME = "DELETE FROM mods_buffer_schemes WHERE ownerId=?";
	private static final String SQL_INSERT_SCHEME = "INSERT IGNORE INTO mods_buffer_schemes (ownerId, id, scheme) VALUES (?,?,?)";
	
	public CharSchemesTable()
	{
		_schemesTable = new FastMap<Integer, FastMap<String, FastList<Integer>>>();
	}
	
	/**
	 * This method loads player scheme and put into _schemesTable map.
	 * 
	 * @param objectId: player's objectId
	 */
	public void loadScheme(int objectId)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_LOAD_SCHEME);
			statement.setInt(1, objectId);
			
			ResultSet rs = statement.executeQuery();			
			
			FastMap<String, FastList<Integer>> map = new FastMap<String, FastList<Integer>>();
			
			while (rs.next())
			{
				int skillId = rs.getInt("id");
				String scheme = rs.getString("scheme");				
				
				if (!map.containsKey(scheme)/* && map.size() <= 6*/) 
					map.put(scheme, new FastList<Integer>());
				
				if (map.get(scheme) != null/* && map.get(scheme).size() < 35*/)
					map.get(scheme).add(skillId);
			}
			
			if (!map.isEmpty()) _schemesTable.put(objectId, map);
			
			statement.close();
			rs.close();			
		}
		catch (Exception e)
		{
			_log.warning("Error trying to load buff scheme from object id: "+objectId);
		}
		finally
		{
			try{con.close();} catch(Exception e){}
		}
	}
	public void onPlayerLogin(int playerId)
	{
		if (_schemesTable.get(playerId) == null) loadScheme(playerId);
	}
	/**
	 * Do necessary task when server is shutting down or restarting:<br>
	 * <li>Clears DataBase</li>
	 * <li>Saves new info</li>
	 */
	public void onServerShutdown()
	{
		clearDB();
		saveDataToDB();
	}
	
	public void clearDB()
	{
		if (_schemesTable.isEmpty()) return;
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();			
			
			for (FastMap.Entry<Integer, FastMap<String,FastList<Integer>>> e = _schemesTable.head(), end = _schemesTable.tail(); (e = e.getNext()) != end;)
			{
				PreparedStatement statement = con.prepareStatement(SQL_DELETE_SCHEME);
				statement.setInt(1, e.getKey());
				statement.execute();
			}
		}
		catch (Exception e)
		{
			_log.warning("CharSchemesTable: Error while trying to delete schemes");
		}
		finally
		{
			try{con.close();}catch (Exception e){}
		}		
	}
	public void saveDataToDB()
	{
		if (_schemesTable.isEmpty()) return;
		
		Connection con = null;
		int count = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();			
			
			for (FastMap.Entry<Integer, FastMap<String, FastList<Integer>>> e = _schemesTable.head(), end = _schemesTable.tail(); (e = e.getNext()) != end;)
			{
				// each profile
				if(e.getValue() == null || e.getValue().isEmpty()) continue;
				
				for (FastMap.Entry<String, FastList<Integer>> a = e.getValue().head(), enda = e.getValue().tail(); (a = a.getNext()) != enda;)
				{
					if (a.getValue() == null || a.getValue().isEmpty()) continue;
					// each skill
				
					for (int skillId : a.getValue())
					{
						PreparedStatement statement = con.prepareStatement(SQL_INSERT_SCHEME);
						statement.setInt(1, e.getKey());
						statement.setInt(2, skillId);
						statement.setString(3, a.getKey());
						statement.executeUpdate();
						statement.close();						
					}
				}
				
				count++;
			}
		}
		catch (Exception e)
		{
			_log.warning("CharSchemesTable: Error while trying to delete schemes");
		}		
		finally
		{
			try{con.close();}catch (Exception e){}
			System.out.println("CharSchemeTable: Saved "+String.valueOf(count+" scheme(s)"));
		}		
	}
	
	public FastList<Integer> getScheme(int playerid, String scheme_key)
	{
		if (_schemesTable.get(playerid) == null) return null;
		
		return _schemesTable.get(playerid).get(scheme_key);
	}
	
	public boolean getSchemeContainsSkill(int playerId, String scheme_key, int skillId)
	{
		for (int skId : getScheme(playerId, scheme_key))
		{
			if (skId == skillId) return true;
		}
		
		return false;
	}	
	public void setScheme(int playerId, String schemeKey, FastList<Integer> list)
	{
		_schemesTable.get(playerId).put(schemeKey, list);		
	}
	public FastMap<String, FastList<Integer>> getAllSchemes(int playerId)
	{
		return _schemesTable.get(playerId);
	}
	
	public FastMap<Integer, FastMap<String, FastList<Integer>>> getSchemesTable()
	{
		return _schemesTable;
	}
	public static CharSchemesTable getInstance()
	{
		if (_instance == null) _instance = new CharSchemesTable();
		return _instance;
	}
	public boolean isInitialized()
	{
		return _initialized;
	}
}