package luna.custom.globalScheduler;

import ghosts.model.Ghost;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class GlobalEventVariablesHolder
{
	boolean	_pagansMode	= false;
	boolean	_olympiad	= false;
	
	public void setPagansMode(boolean val)
	{
		_pagansMode = val;
		pagansMode();
	}
	
	public boolean getPagansStatus()
	{
		return _pagansMode;
	}
	
	public void setOlympiad(boolean val)
	{
		_olympiad = val;
	}
	
	public boolean getOlympiad()
	{
		return _olympiad;
	}
	
	public void pagansMode()
	{
		if (_pagansMode)
		{
			Announcements.getInstance().announceToAll("Pagan's Temple is open.");
		}
		else
		{
			kickAllPlayersFromPT();
			Announcements.getInstance().announceToAll("Pagan's Temple is now closed.");
			//Announcements.getInstance().announceToAll("The undeads vanished.");
		}
	}
	public void kickAllPlayersFromPT()
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{

			if (!(player instanceof Ghost) && player.getClient().isDetached())
			{
				continue;
			}
			if (player.isGM())
			{
				continue;
			}
			if (player.isInFT())
			{
				player.setIsPendingRevive(true);
				player.teleToLocation(83380, 148107, -3404, true);
				player.setInsideZone(L2Character.ZONE_FARM, false);
				player.setInsideZone(L2Character.ZONE_CHAOTIC, false);
				player.setIsInFT(false);
				//player.sendMessage("The sun raised, the undeads vanished, so did you.");
			}
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
