package net.sf.l2j.gameserver.model.entity.events;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.Location;


public class PortalData
{
	public static Map<Integer, RaidEventData> _portaldata = new ConcurrentHashMap<>();
	private String _fileName = "data/xml/BossEvent.xml";
	private static PortalData _instance = null;
	public static PortalData getInstance()
	{
		if(_instance == null)
			_instance = new PortalData();
		return _instance;
	}
	public PortalData()
	{
		loadXml();
	}
	
	public File getXMLFile()
	{
		return new File(_fileName);
	}
	
	protected void loadXml()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = getXMLFile();
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("portal"))
						{
							int id = 0;
							int registrationTime = 0;
							int runTime = 1;
							Location bossLocation = null;
							Location teleportLocation = null;
							Location regNpcLocation = null;
							int dayOfMonth = -1;
							int dayOfWeek = -1;
							int hour = 0;
							int minute = 0;
							int regNpcId = 0;
							id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if (att.getNodeName().equalsIgnoreCase("bossLoc"))
									bossLocation = new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue()) );
								if (att.getNodeName().equalsIgnoreCase("teleportLoc"))
									teleportLocation = new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue()) );
								if (att.getNodeName().equalsIgnoreCase("runTime"))
									runTime = Integer.parseInt(att.getAttributes().getNamedItem("minutes").getNodeValue());
								if (att.getNodeName().equalsIgnoreCase("registration"))
								{
									registrationTime = Integer.parseInt(att.getAttributes().getNamedItem("time").getNodeValue());
									regNpcId = Integer.parseInt(att.getAttributes().getNamedItem("npcId").getNodeValue());
									regNpcLocation = new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()),Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue()) );
								}
								if (att.getNodeName().equalsIgnoreCase("spawnDate"))
								{
									if (att.getAttributes().getNamedItem("dayOfMonth") != null)
										dayOfMonth = Integer.parseInt(att.getAttributes().getNamedItem("dayOfMonth").getNodeValue());
									if (att.getAttributes().getNamedItem("dayOfWeek") != null)
										dayOfWeek = Integer.parseInt(att.getAttributes().getNamedItem("weekDay").getNodeValue());
									hour = Integer.parseInt(att.getAttributes().getNamedItem("hour").getNodeValue());
									minute = Integer.parseInt(att.getAttributes().getNamedItem("minute").getNodeValue());
								}
							}
							if (!_portaldata.containsKey(id))
								_portaldata.put(id, new RaidEventData(id, registrationTime,runTime,bossLocation,teleportLocation,regNpcLocation,regNpcId, dayOfMonth, dayOfWeek, hour, minute));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void ReloadPortals()
	{
		_portaldata.clear();
		loadXml();
		System.out.println("Portals Reloaded.");
	}
	public Map<Integer,RaidEventData> getPortalData()
	{
		return _portaldata;
	}
	public RaidEventData getPortal(int id)
	{
		return _portaldata.get(id);
	}
	
	public class RaidEventData
	{
		private final int _npcId;
		private final int _registrationTime;
		private final Location _bossLocation;
		private final Location _teleportLocation;
		private final Location _regNpcLocation;
		private final int _regNpcId;
		private final int _dayOfMonth;
		private final int _dayOfWeek;
		private final int _hour;
		private final int _minute;
		private final int _runTime;
		
		public RaidEventData(int npcId, int regTime, int runTime, Location bossLoc,Location teleportLoc,Location regNpc,int regNpcId, int dayOfMonth, int dayOfWeek, int hour, int minute)
		{
			_npcId = npcId;
			_registrationTime = regTime;
			_runTime = runTime;
			_bossLocation = bossLoc;
			_teleportLocation = teleportLoc;
			_regNpcLocation = regNpc;
			_regNpcId = regNpcId;
			_dayOfMonth = dayOfMonth;
			_dayOfWeek = dayOfWeek;
			_hour = hour;
			_minute = minute;
		}
		public int getRegistrationNpcId()
		{
			return _regNpcId;
		}
		public Location getRegistrationNpcLocation()
		{
			return _regNpcLocation;
		}
		public int getRunTime()
		{
			return _runTime;
		}
		
		public int getDayOfMonth()
		{
			return _dayOfMonth;
		}
		
		public int getDayOfWeek()
		{
			return _dayOfWeek;
		}
		
		public int getHour()
		{
			return _hour;
		}
		
		public int getMinute()
		{
			return _minute;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public int getRegistrationTime()
		{
			return _registrationTime;
		}
		
		public Location getBossLocation()
		{
			return _bossLocation;
		}
		
		public Location getTeleportLocation()
		{
			return _teleportLocation;
		}
	}
}
