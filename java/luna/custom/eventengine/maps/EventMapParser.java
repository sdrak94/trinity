package luna.custom.eventengine.maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.Location;

public class EventMapParser
{
	private List<EventMap> eventMaps = new CopyOnWriteArrayList<>();
	
	// public static Map<Integer, EventMap> eventMaps = new ConcurrentHashMap<>();
	private EventMapParser()
	{
		loadXml();
		System.out.println("Loaded " + eventMaps.size() + " event maps");
	}
	
	private void loadXml()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File("data/xml/events/EventMaps.xml");
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
						if (d.getNodeName().equalsIgnoreCase("eventMap"))
						{
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							Location blueTeamLoc = null, redTeamLoc = null;
							List<Location> randomLocs = new ArrayList<>();
							String name = "";
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if (att.getNodeName().equalsIgnoreCase("blueLocation"))
									blueTeamLoc = new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue()));
								if (att.getNodeName().equalsIgnoreCase("redLocation"))
									redTeamLoc = new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue()));
								if (att.getNodeName().equalsIgnoreCase("randomLocation"))
									randomLocs.add(new Location(Integer.parseInt(att.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(att.getAttributes().getNamedItem("z").getNodeValue())));
								if (att.getNodeName().equalsIgnoreCase("mapName"))
									name = att.getAttributes().getNamedItem("value").getNodeValue();
							}
							EventMap temp = new EventMap(blueTeamLoc, redTeamLoc, randomLocs, name, id);
							if (temp != null)
								eventMaps.add(temp);
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
	
	public List<EventMap> getEventMaps()
	{
		return eventMaps;
	}
	
	public void ReloadMaps()
	{
		eventMaps.clear();
		loadXml();
		System.out.println("Loaded " + eventMaps.size() + " event maps");
	}
	
	public static EventMapParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final EventMapParser INSTANCE = new EventMapParser();
	}
}
