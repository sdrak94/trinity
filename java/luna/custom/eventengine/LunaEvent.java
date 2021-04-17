package luna.custom.eventengine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import luna.custom.eventengine.enums.EventState;
import luna.custom.eventengine.managers.UniqueIdManager;
import luna.custom.eventengine.maps.EventMap;
import luna.custom.eventengine.maps.EventMapParser;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;
public abstract class LunaEvent
{
	private String _name;
	protected static List<L2PcInstance> players = new CopyOnWriteArrayList<>();
	protected EventMap eventMap;
	protected static EventState state = EventState.INACTIVE;
	protected int instanceId;
	public LunaEvent(String name)
	{
		this._name = name;
		instanceId = UniqueIdManager.getInstance().getNextAvailableId();
	}
	public String getName()
	{
		return _name;
	}
	public void register(L2PcInstance p)
	{
		if (!state.equals(EventState.REGISTER))
		{
			p.sendMessage("Registration for " + getName() + " Event is over");
			return;
		}
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join " + getName() + " Event in Olympiad mode");
			return;
		}
		if(p.isInSiege())
		{
			p.sendMessage("You can't join " + getName() + " Event during sieges");
			return;
		}
		if(!players.contains(p))
		{
			players.add(p);
			p.setIsInEvent(true);
			p.sendMessage("You have succesfully registered for " + getName() + " Event");
		}
		else
			p.sendMessage("You have already registered for " + getName() + " Event");
	}
	
	public void unregister(L2PcInstance p)
	{
		if (!state.equals(EventState.REGISTER))
			return;	
		if(players.contains(p))
		{
			players.remove(p);
			p.setIsInEvent(false);
			p.sendMessage("You have succesfully unregistered from " + getName() + " Event");
		}
		else
			p.sendMessage("You 're not in the registration list");
	}
	protected void cleanEvent()
	{
		eventMap = null;
	}
	public EventState getState()
	{
		return state;
	}
	public void setState(EventState s)
	{
		state = s;
	}
	public void announceRegistration()
	{
		Announcements.getInstance().announceToAll(getName() + " event participation started");
		//Announcements.getInstance().announceToAll("3 minutes till " + getName() + " event registration close");
		//Announcements.getInstance().announceToAll("Register command: .join || Leave command: .leave");
		//wait(1);
		//Announcements.getInstance().announceToAll("2 minutes till " + getName() + " event registration close");
		//Announcements.getInstance().announceToAll("Register command: .join || Leave command: .leave");
		//wait(1);
		Announcements.getInstance().announceToAll("1 minute till " + getName() + " registration close");
		Announcements.getInstance().announceToAll("Register command: .join || Leave command: .leave");
		wait(1);
	}
	protected void pickMap()
	{
		eventMap = EventMapParser.getInstance().getEventMaps().get(Rnd.get(EventMapParser.getInstance().getEventMaps().size()));
	}
	protected void waitSecs(int val)
	{
		try
		{
			Thread.sleep(val * 1000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	protected void wait(int val)
	{
		try
		{
			Thread.sleep(val * 60000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	public abstract void onDeath(L2PcInstance player);
	public abstract void onDisconnect(L2PcInstance player);
	public abstract void onRestartPoint(L2PcInstance player);
}