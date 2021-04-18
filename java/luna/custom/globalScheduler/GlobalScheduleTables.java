package luna.custom.globalScheduler;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import luna.custom.globalScheduler.template.FuturedGlobalEvent;

public class GlobalScheduleTables
{
	private HashMap<Integer, ArrayList<FuturedGlobalEvent>>	FuturedGlobalEvents	= new HashMap<>();
	
	public void loadNode(final Node n) throws Exception
	{
		FuturedGlobalEvents.clear();
		for (Node n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
		{
			if (!"day".equalsIgnoreCase(n2.getNodeName()))
				continue;
			final Node dayNode = n2.getAttributes().getNamedItem("day");
			final Integer targetDay = dayNode == null ? new Integer(0) : Integer.valueOf(dayNode.getNodeValue());
			final int day = Integer.valueOf(dayNode.getNodeValue());
			for (Node n3 = n2.getFirstChild(); n3 != null; n3 = n3.getNextSibling())
			{
				if ("event".equalsIgnoreCase(n3.getNodeName()))
				{
					final NamedNodeMap nnm = n3.getAttributes();
					final String eventType = nnm.getNamedItem("eventType").getNodeValue();
					final String time = nnm.getNamedItem("time").getNodeValue();
					addEvent(targetDay, new FuturedGlobalEvent(eventType, time, day));
				}
			}
		}
		report();
	}
	
	public void report()
	{
		int size = 0;
		for (ArrayList<?> list : FuturedGlobalEvents.values())
			size += list.size();
		System.out.println("Global Scheduler: Loaded " + size + " global event timers!");
	}
	
	public HashMap<Integer, ArrayList<FuturedGlobalEvent>> getFuturedGlobalEvents()
	{
		return FuturedGlobalEvents;
	}
	
	private void addEvent(final int targetDay, final FuturedGlobalEvent schEvent)
	{
		ArrayList<FuturedGlobalEvent> events = FuturedGlobalEvents.get(targetDay);
		if (events == null)
		{
			events = new ArrayList<>();
			FuturedGlobalEvents.put(targetDay, events);
		}
		events.add(schEvent);
	}
	
	protected void reload()
	{
		FuturedGlobalEvents.clear();
	}
	
	private static final class InstanceHolder
	{
		private static final GlobalScheduleTables _instance = new GlobalScheduleTables();
	}
	
	public static GlobalScheduleTables getInstance()
	{
		return InstanceHolder._instance;
	}
}
