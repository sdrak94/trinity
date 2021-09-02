/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.instancemanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Logger;

import javolution.io.UTF8StreamReader;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;
import javolution.xml.stream.XMLStreamReaderImpl;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;

/**
 * @author evill33t, GodKratos
 */
public class InstanceManager
{
	private final static Logger						_log					= Logger.getLogger(InstanceManager.class.getName());
	private final FastMap<Integer, Instance>		_instanceList			= new FastMap<Integer, Instance>().shared();
	private final FastMap<Integer, InstanceWorld>	_instanceWorlds			= new FastMap<Integer, InstanceWorld>().shared();
	private int										_dynamic				= 300000;
	// InstanceId Names
	private final static Map<Integer, String>		_instanceIdNames		= new FastMap<Integer, String>();
	private final Map<String, Map<Integer, Long>>	_playerInstanceTimes	= new FastMap<String, Map<Integer, Long>>();
	private static final String						ADD_INSTANCE_TIME		= "INSERT INTO character_instance_time (account,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?";
	private static final String						RESTORE_INSTANCE_TIMES	= "SELECT instanceId,time FROM character_instance_time WHERE account=?";
	private static final String						DELETE_INSTANCE_TIME	= "DELETE FROM character_instance_time WHERE account=? AND instanceId=?";

	public static final int SOLO = 2010;
	public static final int KAMALOKA_ID = 2000;
	public static final int EMBRYO_ID = 2001;
	public static final int ULTRAVERSE_ID = 2002;
	public static final int FAFURION_ID = 2005;
	public static final int ZAKEN_ID = 5002;
	public static final int FRINTEZZA_ID = 5003;
	public static final int FREYA_ID = 5004;
	
	
	
	public long getInstanceTime(String account, int id)
	{
		if (!_playerInstanceTimes.containsKey(account))
			restoreInstanceTimes(account);
		if (_playerInstanceTimes.get(account).containsKey(id))
			return _playerInstanceTimes.get(account).get(id);
		return -1;
	}
	
	public Map<Integer, Long> getAllInstanceTimes(String account)
	{
		if (!_playerInstanceTimes.containsKey(account))
			restoreInstanceTimes(account);
		return _playerInstanceTimes.get(account);
	}
	
	public void setInstanceTime(String account, int id, long time)
	{
		if (!_playerInstanceTimes.containsKey(account))
			restoreInstanceTimes(account);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			statement = con.prepareStatement(ADD_INSTANCE_TIME);
			statement.setString(1, account);
			statement.setInt(2, id);
			statement.setLong(3, time);
			statement.setLong(4, time);
			statement.execute();
			statement.close();
			_playerInstanceTimes.get(account).put(id, time);
		}
		catch (Exception e)
		{
			_log.warning("Could not insert character instance time data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public void deleteInstanceTime(String account, int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			statement = con.prepareStatement(DELETE_INSTANCE_TIME);
			statement.setString(1, account);
			statement.setInt(2, id);
			statement.execute();
			statement.close();
			_playerInstanceTimes.get(account).remove(id);
		}
		catch (Exception e)
		{
			_log.warning("Could not delete character instance time data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public void restoreInstanceTimes(String account)
	{
		if (_playerInstanceTimes.containsKey(account))
			return; // already restored
		_playerInstanceTimes.put(account, new FastMap<Integer, Long>());
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_INSTANCE_TIMES);
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int id = rset.getInt("instanceId");
				long time = rset.getLong("time");
				if (time < System.currentTimeMillis())
					deleteInstanceTime(account, id);
				else
					_playerInstanceTimes.get(account).put(id, time);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not delete character instance time data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public String getInstanceIdName(int id)
	{
		if (_instanceIdNames.containsKey(id))
			return _instanceIdNames.get(id);
		return ("UnknownInstance");
	}
	
	@SuppressWarnings("resource")
	private void loadInstanceNames()
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream(Config.DATAPACK_ROOT + "/data/instancenames.xml");
			XMLStreamReaderImpl xpp = new XMLStreamReaderImpl();
			xpp.setInput(new UTF8StreamReader().setInput(in));
			for (int e = xpp.getEventType(); e != XMLStreamConstants.END_DOCUMENT; e = xpp.next())
			{
				if (e == XMLStreamConstants.START_ELEMENT)
				{
					if (xpp.getLocalName().toString().equals("instance"))
					{
						Integer id = Integer.valueOf(xpp.getAttributeValue(null, "id").toString());
						String name = xpp.getAttributeValue(null, "name").toString();
						_instanceIdNames.put(id, name);
					}
				}
			}
		}
		catch (FileNotFoundException e)
		{
			_log.warning("instancenames.xml could not be loaded: file not found");
		}
		catch (XMLStreamException xppe)
		{
			xppe.printStackTrace();
		}
		finally
		{
			try
			{
				in.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public class InstanceWorld
	{
		public int					instanceId;
		public int					templateId	= -1;
		public FastList<Integer>	allowed		= new FastList<Integer>();
		public int					status;
	}
	
	public void addWorld(InstanceWorld world)
	{
		_instanceWorlds.put(world.instanceId, world);
	}
	
	public InstanceWorld getWorld(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	public InstanceWorld getPlayerWorld(L2PcInstance player)
	{
		for (InstanceWorld temp : _instanceWorlds.values())
		{
			if (temp == null)
				continue;
			// check if the player have a World Instance where he/she is allowed to enter
			if (temp.allowed.contains(player.getObjectId()))
				return temp;
		}
		return null;
	}
	
	private InstanceManager()
	{
		_log.info("Initializing InstanceManager");
		loadInstanceNames();
		_log.info("Loaded " + _instanceIdNames.size() + " instance names");
		createWorld();
	}
	
	public static final InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void createWorld()
	{
		Instance themultiverse = new Instance(-1);
		themultiverse.setName("multiverse");
		_instanceList.put(-1, themultiverse);
		_log.info("Multiverse Instance created");
		Instance universe = new Instance(0);
		universe.setName("universe");
		_instanceList.put(0, universe);
		_log.info("Universe Instance created");
		Instance alterverse = new Instance(1);
		alterverse.setName("alterverse");
		_instanceList.put(1, alterverse);
		_log.info("Alterverse Instance created");
		Instance extraverse = new Instance(2);
		extraverse.setName("extraverse");
		_instanceList.put(2, extraverse);
		_log.info("extraverse Instance created");
		Instance instance = new Instance(3);
		_instanceList.put(3, instance);
		try
		{
			instance.loadInstanceTemplate("event_s.xml");
			_log.info("event s Instance created");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		Instance hellbound1 = new Instance(20);
		hellbound1.setName("hellbound1");
		_instanceList.put(4, hellbound1);
		_log.info("hellbound1 Instance created");
	}
	
	public void destroyInstance(int instanceid)
	{
		if (instanceid <= 0)
			return;
		Instance temp = _instanceList.get(instanceid);
		if (temp != null)
		{
			temp.removeNpcs();
			temp.removePlayers();
			temp.removeDoors();
			temp.cancelTimer();
			_instanceList.remove(instanceid);
		}
		if (_instanceWorlds.containsKey(instanceid))
			_instanceWorlds.remove(instanceid);
	}
	
	public Instance getInstance(int instanceid)
	{
		return _instanceList.get(instanceid);
	}
	
	public FastMap<Integer, Instance> getInstances()
	{
		return _instanceList;
	}
	
	public int getPlayerInstanceId(int objectId)
	{
		for (Instance temp : _instanceList.values())
		{
			if (temp == null)
				continue;
			// check if the player is in any active instance
			if (temp.containsPlayer(objectId))
				return temp.getId();
		}
		// 0 is default instance aka the world
		return 0;
	}
	
	public Instance getPlayerInstance(int objectId)
	{
		for (Instance temp : _instanceList.values())
		{
			if (temp == null)
				continue;
			// check if the player is in any active instance
			if (temp.containsPlayer(objectId))
				return temp;
		}
		return null;
	}
	
	public boolean createInstance(int id)
	{
		if (getInstance(id) != null)
			return false;
		Instance instance = new Instance(id);
		_instanceList.put(id, instance);
		return true;
	}
	
	public boolean createInstanceFromTemplate(int id, String template) throws FileNotFoundException
	{
		if (getInstance(id) != null)
			return false;
		Instance instance = new Instance(id);
		_instanceList.put(id, instance);
		instance.loadInstanceTemplate(template);
		return true;
	}
	
	/**
	 * Create a new instance with a dynamic instance id based on a template (or null)
	 * 
	 * @param template
	 *            xml file
	 * @return
	 */
	public int createDynamicInstance(String template)
	{
		while (getInstance(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warning("InstanceManager: More then " + (Integer.MAX_VALUE - 300000) + " instances created");
				_dynamic = 300000;
			}
		}
		Instance instance = new Instance(_dynamic);
		_instanceList.put(_dynamic, instance);
		if (template != null)
		{
			try
			{
				instance.loadInstanceTemplate(template);
			}
			catch (FileNotFoundException e)
			{
				_log.warning("InstanceManager: Failed creating instance from template " + template + ", " + e.getMessage());
				e.printStackTrace();
			}
		}
		return _dynamic;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final InstanceManager _instance = new InstanceManager();
	}
	
	public static String getInstanceName(Integer instanceID)
	{
		switch (instanceID)
		{
			case 2000:
				return "Kamaloka";
			case 2001:
				return "Embryo";
			case 2002:
				return "Ultraverse";
			case 2003:
				return "Steel Citadel";
			case 2004:
				return "Imperial Tomb";
			case 2005:
				return "Fafurion";
			case 5000:
				return "NewbieLair";
			case 5001:
				return "DVC";
			case 5002:
				return "Zaken";
			case 5003:
				return "Frintezza";
		}
		return null;
	}
}
