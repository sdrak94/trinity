package luna.custom.globalScheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.events.Communicator;

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
