package cz.nxs.events.engine.main.base;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.l2j.CallBack;

/**
 * @author hNoke
 * - loads and manages MainEventInstanceTypes
 */
public class MainEventInstanceTypeManager
{
	private int _highestId = 0;
	
	public MainEventInstanceTypeManager()
	{
		loadAll();
	}
	
	private void loadAll()
	{
		Connection con = null;

		int count = 0;

		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT event, id, name, visible_name, params FROM nexus_main_instances");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				EventType type = EventType.getType(rset.getString("event"));
				if(type == null)
				{
					//NexusLoader.debug("MainEventInstanceTypeManager - event type " + rset.getString("event") + " doesn't exist. Skipping..");
					continue;
				}
				
				int id = rset.getInt("id");
				String name = rset.getString("name");
				String visibleName = rset.getString("visible_name");
				String params = rset.getString("params");
				
				AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
				
				if(event == null)
				{
					NexusLoader.debug("MainEventInstanceTypeManager - event object of type " + rset.getString("event") + " doesn't exist. Skipping");
					continue;
				}
				
				MainEventInstanceType instance = new MainEventInstanceType(id, event, name, visibleName, params);
				
				event.insertConfigs(instance);
				instance.loadConfigs();
				
				addInstanceType(instance, false);
				
				count++;
			}
			rset.close();
			statement.close();
		}

		catch (SQLException e)
		{
			e.printStackTrace();
		}

//		finally
//		{
//			try {con.close(); }
//			catch (Exception e) {}
//		}
		
		NexusLoader.debug("Nexus Events: Loaded " + count + " main event InstanceTypes.");
		
		//TODO test this
		for(AbstractMainEvent event : EventManager.getInstance().getMainEvents().values())
		{
			if(event.getInstanceTypes().isEmpty())
			{
				MainEventInstanceType instance = new MainEventInstanceType(getNextId(), event, "Default", "Default Instance", null);
				event.insertConfigs(instance);
				addInstanceType(instance, true);

				NexusLoader.debug("Event " + event.getEventName() + " had no InstanceTypes set up. They either got deleted or you're starting the server with this event for the first time. This has been automatically fixed!");
			}
		}
	}
	
	public void updateInstanceType(MainEventInstanceType type)
	{
		type.setParams(type.encodeParams());
		addInstanceType(type, true);
	}
	
	public void addInstanceType(MainEventInstanceType type, boolean storeToDb)
	{
		AbstractMainEvent event = type.getEvent();
		event.addInstanceType(type);
		
		if(type.getId() > _highestId)
			_highestId = type.getId();
		
		if(storeToDb)
		{
			Connection con = null;

			try
			{
				con = CallBack.getInstance().getOut().getConnection();
				PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_main_instances VALUES (?,?,?,?,?)");
				
				statement.setString(1, event.getEventType().getAltTitle());
				statement.setInt(2, type.getId());
				statement.setString(3, type.getName());
				statement.setString(4, type.getVisibleName());
				statement.setString(5, type.getParams());
				
				statement.execute();
				statement.close();
			}

			catch (SQLException e)
			{
				e.printStackTrace();
			}

//			finally
//			{
//				try {con.close(); }
//				catch (Exception e) {}
//			}
		}
	}
	
	public void removeInstanceType(MainEventInstanceType type)
	{
		AbstractMainEvent event = type.getEvent();
		event.removeInstanceType(type);
		
		Connection con = null;

		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM nexus_main_instances WHERE event = '" + event.getEventType().getAltTitle() + "' AND id = " + type.getId());
			statement.execute();
			statement.close();
		}

		catch (SQLException e)
		{
			e.printStackTrace();
		}

//		finally
//		{
//			try {con.close(); }
//			catch (Exception e) {}
//		}
	}
	
	public synchronized int getNextId()
	{
		return ++_highestId;
	}
	
	public static MainEventInstanceTypeManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final MainEventInstanceTypeManager _instance = new MainEventInstanceTypeManager();
	}
}
