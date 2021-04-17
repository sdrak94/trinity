package net.sf.l2j.gameserver.model.events.manager.controler;

import java.util.HashMap;
import java.util.HashSet;

import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.Shutdown.Savable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class DelaysController implements ITimeTrigger, Savable
{
	private final HashMap<String, HashSet<Integer>>	delays	= new HashMap<>();
	private int										loadedDelays;
	
	private DelaysController()
	{
		loadDelays();
		System.out.println("DelaysController: Loaded " + loadedDelays + " delays from the DB.");
		RealTimeController.registerHook(this);
		Shutdown.addShutdownHook(this);
	}
	
	public void addDelay(String worldId, L2PcInstance player)
	{
		addDelay(worldId, Integer.valueOf(player.getObjectId()));
	}
	
	public void addDelay(String worldId, int playerId)
	{
		HashSet<Integer> worldDelays = delays.get(worldId);
		if (worldDelays == null)
		{
			worldDelays = new HashSet<>();
			delays.put(worldId, worldDelays);
		}
		worldDelays.add(playerId);
		loadedDelays++;
	}
	
	public synchronized boolean hasDelay(String worldId, L2PcInstance player)
	{
		final HashSet<Integer> worldDelays = delays.get(worldId);
		if (worldDelays == null)
			return false;
		return worldDelays.contains(Integer.valueOf(player.getObjectId()));
	}
	
	public synchronized boolean rmPlayerDelay(String worldId, L2PcInstance player)
	{
		final HashSet<Integer> worldDelays = delays.get(worldId);
		if (worldDelays == null)
			return false;
		return worldDelays.remove(Integer.valueOf(player.getObjectId()));
	}
	
	public synchronized void rmInstanceDelays(String worldId)
	{
		final HashSet<Integer> worldDelays = delays.get(worldId);
		if (worldDelays != null)
			worldDelays.clear();
	}
	
	
	private void loadDelays()
	{

	}
	
	public static class InstanceHolder
	{
		private static final DelaysController _instance = new DelaysController();
	}
	
	public static DelaysController getInstance()
	{
		return InstanceHolder._instance;
	}
	
	@Override
	public void notify(int day, String time)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void store()
	{
		// TODO Auto-generated method stub
	}
}