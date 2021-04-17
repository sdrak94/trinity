package luna.custom.eventengine.managers;

import net.sf.l2j.gameserver.instancemanager.InstanceManager;

public class UniqueIdManager
{
	private int nextAvailableId = 0;
	private UniqueIdManager()
	{
		
	}
	public int getNextAvailableId()
	{
		nextAvailableId++;
		InstanceManager.getInstance().createInstance(nextAvailableId);
		return nextAvailableId;
	}
	public static UniqueIdManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	private static class SingletonHolder
	{
		private static final UniqueIdManager INSTANCE = new UniqueIdManager();
	}
}