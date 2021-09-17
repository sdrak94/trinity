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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2TeleportLocation;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.2.2.3 $ $Date: 2005/03/27 15:29:18 $
 */
public class TeleportLocationTable
{
	private static Logger _log = Logger.getLogger(TeleportLocationTable.class.getName());
	
	private Map<Integer, L2TeleportLocation> _teleports;
	
	public static TeleportLocationTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private TeleportLocationTable()
	{
		reloadAll();
	}
	
	public void reloadAll()
	{
		_teleports = new FastMap<Integer, L2TeleportLocation>();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT Description, id, loc_x, loc_y, loc_z, price, fornoble FROM teleport");
			ResultSet rset = statement.executeQuery();
			L2TeleportLocation teleport;
			
			while (rset.next())
			{
				teleport = new L2TeleportLocation();
				teleport.setDesc(rset.getString("Description"));
				teleport.setTeleId(rset.getInt("id"));
				teleport.setLocX(rset.getInt("loc_x"));
				teleport.setLocY(rset.getInt("loc_y"));
				teleport.setLocZ(rset.getInt("loc_z"));
				teleport.setPrice(rset.getInt("price"));
				teleport.setIsForNoble(rset.getInt("fornoble") == 1);
				
				_teleports.put(teleport.getTeleId(), teleport);
			}
			
			rset.close();
			statement.close();
			
			_log.config("TeleportLocationTable: Loaded " + _teleports.size() + " Teleport Location Templates.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error loading Teleport Table.", e);
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
		
		if (Config.CUSTOM_TELEPORT_TABLE)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT Description, id, loc_x, loc_y, loc_z, price, fornoble FROM custom_teleport");
				ResultSet rset = statement.executeQuery();
				L2TeleportLocation teleport;
				int _cTeleCount = _teleports.size();
				while (rset.next())
				{
					teleport = new L2TeleportLocation();
					teleport.setDesc(rset.getString("Description"));
					teleport.setTeleId(rset.getInt("id"));
					teleport.setLocX(rset.getInt("loc_x"));
					teleport.setLocY(rset.getInt("loc_y"));
					teleport.setLocZ(rset.getInt("loc_z"));
					teleport.setPrice(rset.getInt("price"));
					teleport.setIsForNoble(rset.getInt("fornoble") == 1);
					_teleports.put(teleport.getTeleId(), teleport);
				}
				rset.close();
				statement.close();
				_cTeleCount = _teleports.size() - _cTeleCount;
				if (_cTeleCount > 0)
					_log.config("TeleportLocationTable: Loaded " + _cTeleCount + " Custom Teleport Location Templates.");
			}
			catch (Exception e)
			{
				_log.warning("error while creating custom teleport table " + e);
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
	}
	
	/**
	 * @param template id
	 * @return
	 */
	public L2TeleportLocation getTemplate(int id)
	{
		return _teleports.get(id);
	}
	
	public Map<Integer, L2TeleportLocation> getTeleports()
	{
		Map<Integer,L2TeleportLocation> teles = new FastMap<Integer, L2TeleportLocation>();
		
		teles.putAll(_teleports);
		
		return teles;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TeleportLocationTable _instance = new TeleportLocationTable();
	}
}
