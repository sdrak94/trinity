package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.item.L2Henna;

public class HennaTreeTable
{
	private static Logger _log = Logger.getLogger(HennaTreeTable.class.getName());
	private final Map<Integer, List<L2Henna>> _hennaTrees;
	private boolean _initialized = true;
	
	public static HennaTreeTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private HennaTreeTable()
	{
		_hennaTrees = new FastMap<Integer, List<L2Henna>>();
		int classId = 0;
		int count = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id FROM class_list");
			ResultSet classlist = statement.executeQuery();
			
			while (classlist.next())
			{
				classId = classlist.getInt("id");
				FastList<L2Henna> list = new FastList<L2Henna>();
				
				PreparedStatement statement2 = con.prepareStatement("SELECT symbol_id from henna");
				ResultSet hennatree = statement2.executeQuery();
				
				while (hennatree.next())
				{
					int id = hennatree.getInt("symbol_id");
					
					L2Henna template = HennaTable.getInstance().getTemplate(id);
					
					if (template == null)
						continue;
					
					list.add(template);
				}
				
				hennatree.close();
				statement2.close();
				
				count += list.size();
				_hennaTrees.put(classId, list);
			}		
			
			classlist.close();
			statement.close();
			_log.info("HennaTreeTable: Loaded " + count + " Henna Tree Templates.");
		}
		catch (Exception e)
		{
			_log.warning("Error while creating henna tree for classId "+classId+" "+e);
		}
		finally { try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); } }
	}
	
	public List<L2Henna> getAvailableHenna(ClassId classId)
	{
		return _hennaTrees.get(classId.getId());
	}
	
/*	public L2HennaInstance[] getAvailableHenna(ClassId classId)
	{
		List<L2HennaInstance> result = new FastList<L2HennaInstance>();
		List<L2HennaInstance> henna = _hennaTrees.get(classId);
		if (henna == null)
		{
			// the hennatree for this class is undefined, so we give an empty list
			_log.warning("Hennatree for class " + classId + " is not defined !");
			return new L2HennaInstance[0];
		}
		
		for (L2HennaInstance temp : henna)
		{
			result.add(temp);
		}
		
		return result.toArray(new L2HennaInstance[result.size()]);
	}*/
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HennaTreeTable _instance = new HennaTreeTable();
	}
}
