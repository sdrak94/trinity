/**
 * 
 */
package cz.nxs.interf.callback;

import java.util.Collection;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class EventAchievments implements ICallback
{
	@Override
	public void eventStarts(int instance, EventType event, Collection<? extends EventTeam> teams)
	{
		
	}

	@Override
	public void playerKills(EventType event, PlayerEventInfo player, PlayerEventInfo target)
	{
		
	}

	@Override
	public void playerScores(EventType event, PlayerEventInfo player, int count)
	{
		
	}

	@Override
	public void playerFlagScores(EventType event, PlayerEventInfo player)
	{
		
	}

	@Override
	public void playerKillsVip(EventType event, PlayerEventInfo player, PlayerEventInfo vip)
	{
		
	}

	@Override
	public void eventEnded(int instance, EventType event, Collection<? extends EventTeam> teams)
	{
		
	}
	
	public static EventAchievments getInstance()
	{
		if(SingletonHolder._instance == null) SingletonHolder.register();
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static EventAchievments  _instance;
		
		protected static void register()
		{
			_instance = new EventAchievments();
			CallbackManager.getInstance().registerCallback(_instance);
		}
	}
}
