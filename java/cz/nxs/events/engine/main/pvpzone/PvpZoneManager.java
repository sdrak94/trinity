/**
 * 
 */
package cz.nxs.events.engine.main.pvpzone;

import java.util.List;

import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;

/**
 * @author hNoke
 *
 */
public class PvpZoneManager
{
	private PvpZone _activeZone;
	private String selectedMap;
	
	private long timeStarted;
	private int lastMapIndex;
	
	private static PvpZone _coliseum;
	
	public static List<PvpZone> _zones = new FastList<PvpZone>();
	
	public PvpZoneManager()
	{
		load();
	}
	
	private void load()
	{
		_activeZone = null;
		lastMapIndex = -1;
		
		_coliseum = new ColiseumZone();
		
		
		_zones.add(_coliseum);
	}
	
	public void switchMap()
	{
		if(isActive())
			return;
		
		lastMapIndex ++;
		if(lastMapIndex >= _zones.size() || _zones.get(lastMapIndex) == null)
		{
			lastMapIndex = 0;
			selectedMap = _zones.get(0).getName();
		}
		else
		{
			selectedMap = _zones.get(lastMapIndex).getName();
		}
	}
	
	public void start(String zone)
	{
		if(zone == null)
			zone = selectedMap;
		
		if(zone == null)
		{
			switchMap();
			zone = selectedMap;
		}
		
		if(zone.equals("Coliseum"))
		{
			_activeZone = _coliseum;
			_activeZone.start();
		}
		
		timeStarted = System.currentTimeMillis();
	}
	
	public void stopByGm()
	{
		if(_activeZone != null)
		{
			_activeZone.end();
			_activeZone = null;
			
			timeStarted = 0;
		}
	}
	
	public void stop()
	{
		if(_activeZone != null)
		{
			_activeZone = null;
			
			timeStarted = 0;
		}
	}
	
	public void registerPlayer(PlayerEventInfo player)
	{
		if(_activeZone != null)
		{
			if (player.isRegistered())
			{
				player.sendMessage(LanguageEngine.getMsg("registering_alreadyRegistered"));
				return;
			}
			
			int i = EventWarnings.getInstance().getPoints(player);
			if(i >= EventWarnings.MAX_WARNINGS && !player.isGM())
			{
				player.sendMessage(LanguageEngine.getMsg("registering_warningPoints", EventWarnings.MAX_WARNINGS, i));
				return;
			}
			
			if (EventManager.getInstance().canRegister(player) && _activeZone.canRegister(player))
			{
				PlayerEventInfo pi = CallBack.getInstance().getPlayerBase().addInfo(player);
				pi.setIsRegisteredToPvpZone(true);
				
				player.sendMessage("You have been registered to the pvp zone.");
				
				_activeZone.addPlayer(player);
			}
			else
			{
				player.sendMessage("You cannot register in this state.");
			}
		}
	}
	
	public void unregisterPlayer(PlayerEventInfo player)
	{
		if(_activeZone != null)
		{
			if (!player.isRegistered())
			{
				player.sendMessage(LanguageEngine.getMsg("unregistering_notRegistered"));
				return;
			}
			
			if(_activeZone.canUnregister(player))
			{
				player.setIsRegisteredToPvpZone(false);
				CallBack.getInstance().getPlayerBase().eventEnd(player);
				
				player.sendMessage("You have been unregistered from the pvp zone.");
				
				_activeZone.removePlayer(player);
			}
			else
			{
				player.sendMessage("You cannot unregister when the gates are open.");
			}
		}
	}
	
	public boolean isActive()
	{
		return _activeZone != null;
	}
	
	public PvpZone getActiveZone()
	{
		return _activeZone;
	}
	
	public String timeActive()
	{
		if(timeStarted == 0)
			return "N/A";
		
		long currTime = System.currentTimeMillis();
		return Math.max(0, (currTime - timeStarted) / 60000) + " min";
	}
	
	public int getPlayersCount()
	{
		if(_activeZone != null)
			return _activeZone.getPlayersCountForHtml();
		return 0;
	}
	
	public String getMapName()
	{
		if(isActive())
		{
			return _activeZone.getName();
		}
		else
			return selectedMap == null ? "N/A" : selectedMap;
	}
	
	public String getState()
	{
		if(_activeZone != null) return _activeZone.getStateNameForHtml();
		return "N/A";
	}
}
