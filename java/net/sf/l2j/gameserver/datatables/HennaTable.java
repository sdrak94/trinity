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
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2Henna;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class HennaTable
{
	private static Logger _log = Logger.getLogger(HennaTable.class.getName());
	
	private FastMap<Integer, L2Henna> _henna;
	
	public static HennaTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private HennaTable()
	{
		_henna = new FastMap<Integer, L2Henna>();
		restoreHennaData();
	}
	
	/**
	 *
	 */
	private void restoreHennaData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT symbol_id, symbol_name, dye_id, dye_amount, price, stat_INT, stat_STR, stat_CON, stat_MEM, stat_DEX, stat_WIT FROM henna");
			ResultSet hennadata = statement.executeQuery();
			
			fillHennaTable(hennadata);
			hennadata.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("error while creating henna table " + e);
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
	}
	
	private void fillHennaTable(ResultSet HennaData) throws Exception
	{
		while (HennaData.next())
		{
			StatsSet hennaDat = new StatsSet();
			int id = HennaData.getInt("symbol_id");
			
			hennaDat.set("symbol_id", id);
			//hennaDat.set("symbol_name", HennaData.getString("symbol_name"));
			hennaDat.set("dye", HennaData.getInt("dye_id"));
			hennaDat.set("price", HennaData.getInt("price"));
			//amount of dye required
			hennaDat.set("amount", HennaData.getInt("dye_amount"));
			hennaDat.set("stat_INT", HennaData.getInt("stat_INT"));
			hennaDat.set("stat_STR", HennaData.getInt("stat_STR"));
			hennaDat.set("stat_CON", HennaData.getInt("stat_CON"));
			hennaDat.set("stat_MEM", HennaData.getInt("stat_MEM"));
			hennaDat.set("stat_DEX", HennaData.getInt("stat_DEX"));
			hennaDat.set("stat_WIT", HennaData.getInt("stat_WIT"));
			
			L2Henna template = new L2Henna(hennaDat);
			_henna.put(id, template);
		}
		_log.config("HennaTable: Loaded " + _henna.size() + " Templates.");
	}
	
	public L2Henna getTemplate(int id)
	{
		return _henna.get(id);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HennaTable _instance = new HennaTable();
	}
}
