package luna.custom.globalScheduler;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;

public class GlobalEventVariablesHolder
{
	boolean												_night		= false;
	boolean												_olympiad	= false;
	
	public void setNight(boolean val)
	{
		_night = val;
		nightMode();
	}
	
	public boolean getNight()
	{
		return _night;
	}
	
	public void setOlympiad(boolean val)
	{
		_olympiad = val;
	}
	
	public boolean getOlympiad()
	{
		return _olympiad;
	}
	
	public void nightMode()
	{
		if (_night)
		{
			//Announcements.getInstance().announceToAll("Night begins");
			Announcements.getInstance().announceToAll("Pagan's Temple is open");
			//DayNightSpawnManager.getInstance().spawnNightCreatures();
		}
		else
		{
			DayNightSpawnManager.getInstance().kickAllPlayersFromPT();
			//DayNightSpawnManager.getInstance().spawnDayCreatures();
			//Announcements.getInstance().announceToAll("Day begins");
			Announcements.getInstance().announceToAll("Pagan's Temple is forbidden atm.");
			Announcements.getInstance().announceToAll("The undeads vanished.");
		}
	}
	
	public static GlobalEventVariablesHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GlobalEventVariablesHolder _instance = new GlobalEventVariablesHolder();
		
		private SingletonHolder()
		{}
	}
}
