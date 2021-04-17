package luna.custom.globalScheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.dataTables.DoorTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.FenceTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.NpcSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamFlagTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamTemplate;
import net.sf.l2j.util.Util;

public class GlobalEventsParser
{
	private final GlobalScheduleTables calendarTable = GlobalScheduleTables.getInstance();
	
	private void loadXml()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File("data/xml/global_events/global_events.xml");
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
						if ("schedule".equalsIgnoreCase(d.getNodeName()))
							calendarTable.loadNode(d);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void reload()
	{
		loadXml();
		Communicator.getInstance().getTodayGlobalEvents();
	}
	
	public static GlobalEventsParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final GlobalEventsParser INSTANCE = new GlobalEventsParser();
	}
}
