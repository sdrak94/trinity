package net.sf.l2j.gameserver.model.events.manager;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;

public class EventsCollector
{
	protected Map<Integer, Event>	Dominations		= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	SiegeEvents		= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	TeamVsTeams		= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	CTFs			= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	DMs				= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	Koreans			= new HashMap<Integer, Event>();
	protected Map<Integer, Event>	HuntingGrounds	= new HashMap<Integer, Event>();
	
	protected void collectEvents(L2PcInstance p)
	{
		clean();
		for (int i = 0; i < EventsParser.getInstance().getEvents().size(); i++)
		{
			Event e = EventsParser.getInstance().getEvents().get(i);
			switch (e.getType())
			{
				case "Domination":
					Dominations.put(getSize(e), e);
					break;
				case "SiegeEvent":
					SiegeEvents.put(getSize(e), e);
					break;
				case "TeamVsTeam":
					TeamVsTeams.put(getSize(e), e);
					break;
				case "CTF":
					CTFs.put(getSize(e), e);
					break;
				case "DM":
					DMs.put(getSize(e), e);
					break;
				case "Korean":
					Koreans.put(getSize(e), e);
					break;
				case "HuntingGrounds":
					HuntingGrounds.put(getSize(e), e);
					break;
			}
		}
		logger(p);
	}
	
	private void clean()
	{
		Dominations.clear();
		SiegeEvents.clear();
		TeamVsTeams.clear();
		CTFs.clear();
		DMs.clear();
		Koreans.clear();
		HuntingGrounds.clear();
	}
	
	public Map<Integer, Event> getEventCollection(String event)
	{
		Map<Integer, Event> desiredEventCollection = null;
		switch (event)
		{
			case "Domination":
				desiredEventCollection = Dominations;
				break;
			case "SiegeEvent":
				desiredEventCollection = SiegeEvents;
				break;
			case "TeamVsTeam":
				desiredEventCollection = TeamVsTeams;
				break;
			case "CTF":
				desiredEventCollection = CTFs;
				break;
			case "DM":
				desiredEventCollection = DMs;
				break;
			case "Korean":
				desiredEventCollection = Koreans;
				break;
			case "HuntingGrounds":
				desiredEventCollection = HuntingGrounds;
				break;
		}
		return desiredEventCollection;
	}
	
	public int getSize(Event e)
	{
		int s = 0;
		switch (e.getType())
		{
			case "Domination":
				s = Dominations.size();
				break;
			case "SiegeEvent":
				s = SiegeEvents.size();
				break;
			case "TeamVsTeam":
				s = TeamVsTeams.size();
				break;
			case "CTF":
				s = CTFs.size();
				break;
			case "DM":
				s = DMs.size();
				break;
			case "Korean":
				s = Koreans.size();
				break;
			case "HuntingGrounds":
				s = HuntingGrounds.size();
				break;
		}
		return s;
	}
	
	public void logger(L2PcInstance p)
	{
		if (p != null)
		{
			String active = "";
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("-----------L2Trinity Event Engine---------");
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + Dominations.size() + " Domination Events.");
			for (int i = 0; i < Dominations.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == Dominations.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Domination: [" + Dominations.get(i).getId() + "]" + Dominations.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + SiegeEvents.size() + " Siege Events.");
			for (int i = 0; i < SiegeEvents.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == SiegeEvents.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Siege Event: [" + SiegeEvents.get(i).getId() + "]" + SiegeEvents.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + TeamVsTeams.size() + " TeamVsTeam Events.");
			for (int i = 0; i < TeamVsTeams.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == TeamVsTeams.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -TVT: [" + TeamVsTeams.get(i).getId() + "]"+ TeamVsTeams.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + CTFs.size() + " CTF Events.");
			for (int i = 0; i < CTFs.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == CTFs.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -CTF: [" + CTFs.get(i).getId() + "]"+ CTFs.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + DMs.size() + " Deathmatch Events.");
			for (int i = 0; i < DMs.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == DMs.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -DM: [" + DMs.get(i).getId() + "]"+ DMs.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + Koreans.size() + " Korean Events.");
			for (int i = 0; i < Koreans.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == Koreans.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Korean: [" + Koreans.get(i).getId() + "]"+ Koreans.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + HuntingGrounds.size() + " HuntingGrounds Events.");
			for (int i = 0; i < HuntingGrounds.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == HuntingGrounds.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Hunting Grounds: [" + HuntingGrounds.get(i).getId() + "]"+ HuntingGrounds.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
		}
		System.out.println("Loaded: " + Dominations.size() + " Domination Events.");
		System.out.println("Loaded: " + SiegeEvents.size() + " Siege Events.");
		System.out.println("Loaded: " + TeamVsTeams.size() + " TeamVsTeam Events.");
		System.out.println("Loaded: " + CTFs.size() + " CTF Events.");
		System.out.println("Loaded: " + DMs.size() + " Deathmatch Events.");
		System.out.println("Loaded: " + Koreans.size() + " Korean Events.");
		System.out.println("Loaded: " + HuntingGrounds.size() + " HuntingGrounds Events.");
	}
	public void loggerCB(L2PcInstance p)
	{
		if (p != null)
		{
			
			String active = "";
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("-----------L2Trinity Event Engine---------");
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + Dominations.size() + " Domination Events.");
			for (int i = 0; i < Dominations.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == Dominations.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Domination: [" + Dominations.get(i).getId() + "]" + Dominations.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + SiegeEvents.size() + " Siege Events.");
			for (int i = 0; i < SiegeEvents.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == SiegeEvents.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Siege Event: [" + SiegeEvents.get(i).getId() + "]" + SiegeEvents.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + TeamVsTeams.size() + " TeamVsTeam Events.");
			for (int i = 0; i < TeamVsTeams.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == TeamVsTeams.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -TVT: [" + TeamVsTeams.get(i).getId() + "]"+ TeamVsTeams.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + CTFs.size() + " CTF Events.");
			for (int i = 0; i < CTFs.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == CTFs.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -CTF: [" + CTFs.get(i).getId() + "]"+ CTFs.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + DMs.size() + " Deathmatch Events.");
			for (int i = 0; i < DMs.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == DMs.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -DM: [" + DMs.get(i).getId() + "]"+ DMs.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + Koreans.size() + " Korean Events.");
			for (int i = 0; i < Koreans.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == Koreans.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Korean: [" + Koreans.get(i).getId() + "]"+ Koreans.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
			p.sendMessage("Loaded: " + HuntingGrounds.size() + " HuntingGrounds Events.");
			for (int i = 0; i < HuntingGrounds.size(); i++)
			{
				Event e = EventEngine.getInstance().getActiveEvent() != null ? EventEngine.getInstance().getActiveEvent() : null;
				if (e != null)
				{
					active = EventEngine.getInstance().getActiveEvent().getId() == HuntingGrounds.get(i).getId() ? "*Active - " : "";
				}
				p.sendMessage(active + " -Hunting Grounds: [" + HuntingGrounds.get(i).getId() + "]"+ HuntingGrounds.get(i).getName());
			}
			p.sendMessage("-------------------------------------------------");
		}
		System.out.println("Loaded: " + Dominations.size() + " Domination Events.");
		System.out.println("Loaded: " + SiegeEvents.size() + " Siege Events.");
		System.out.println("Loaded: " + TeamVsTeams.size() + " TeamVsTeam Events.");
		System.out.println("Loaded: " + CTFs.size() + " CTF Events.");
		System.out.println("Loaded: " + DMs.size() + " Deathmatch Events.");
		System.out.println("Loaded: " + Koreans.size() + " Korean Events.");
		System.out.println("Loaded: " + HuntingGrounds.size() + " HuntingGrounds Events.");
	}
	public static EventsCollector getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventsCollector _instance = new EventsCollector();
	}
}
