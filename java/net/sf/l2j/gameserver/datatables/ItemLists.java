package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.util.Rnd;

public class ItemLists
{
protected static final Logger _log = Logger.getLogger(ItemLists.class.getName());
private FastMap<String, FastList<Integer>> _itemLists;

public static ItemLists getInstance()
{
	return SingletonHolder._instance;
}

private ItemLists()
{
	loadLists();
}

public void loadLists()
{
	_itemLists = new FastMap<String, FastList<Integer>>();
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT * FROM itemlists");
		ResultSet result = statement.executeQuery();
		
		int count = 0;
		
		while (result.next())
		{
			String list = result.getString("list");
			if (list == null)
				continue;
			
			if (list.equalsIgnoreCase(""))
				list = "0";
			
			final StringTokenizer st = new StringTokenizer(list, ";");
			FastList<Integer> fastlist = new FastList<Integer>();
			
			while (st.hasMoreTokens())
			{
				int itemId = 0;
				
				try
				{
					itemId = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					itemId = 0;
				}
				
				if (itemId != 0)
					fastlist.addLast(itemId);
			}
			
			final String name = result.getString("name");
			
			if (!_itemLists.containsKey(name))
			{
				_itemLists.put(name, fastlist);
				count++;
			}
		}
		
		result.close();
		statement.close();
		
		_log.config("Loaded " + count + " item lists from the database.");
		
		statement = con.prepareStatement("SELECT name, include FROM itemlists");
		result = statement.executeQuery();
		
		count = 0;
		
		while (result.next())
		{
			String include = result.getString("include");
			
			if (include == null || include.equalsIgnoreCase("0"))
				continue;
			
			final StringTokenizer st = new StringTokenizer(include, ";");
			FastList<Integer> fastlist = new FastList<Integer>();
			
			while (st.hasMoreTokens())
			{
				int listId = 0;
				
				try
				{
					listId = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					listId = 0;
				}
				
				if (listId != 0)
				{
					fastlist.addAll(_itemLists.get(getListName(listId)));
				}
			}
			
			_itemLists.get(result.getString("name")).addAll(fastlist);
			count++;
		}
		
		_log.config("....and loaded " + count + " combined item lists from the database.");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error loading item lists.", e);
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

public String getListName(int listId)
{
	int count = 1;
	
	if (listId > 1000000)
		listId -= 1000000;
	
	for (String val : _itemLists.keySet())
	{
		if (count == listId)
			return val;
		
		count++;
	}
	
	_log.warning("getListName() of ItemLists returned null!!!!!!!!!!!");
	return null;
}

public int generateRandomItemFromList(int listId)
{
	final String name = getListName(listId);
	
	if (name != null)
	{
		FastList<Integer> val = _itemLists.get(name);
		
		if (val != null && !val.isEmpty())
			return val.get(Rnd.get(val.size()));
	}
	
	_log.warning("generateRandomItemFromList() of ItemLists returned 0!!!!!!!!!!! list id: " + listId);
	return 0;
}

public FastList<Integer> getFirstListByItemId(int itemId)
{
	for (FastList<Integer> list : _itemLists.values())
	{
		if (list != null && list.size() > 0)
		{
			if (list.contains(itemId))
				return list;
		}
	}
	
	return null;
}

public void debug()
{
	System.out.println(_itemLists.toString());
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final ItemLists _instance = new ItemLists();
}
}