package cz.nxs.events.engine.mini;

import java.util.List;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;

/**
 * @author hNoke
 * - created when a player registers to a mini event
 * - stores data used when the engine creates mini event games
 */
public class RegistrationData
{
	private int _id;

	private FastList<PlayerEventInfo> _players;
	
	private boolean _choosen = false;
	
	public RegistrationData(FastList<PlayerEventInfo> players)
	{
		_players = players;

		try
		{
			_id = getKeyPlayer().getPlayersId();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public PlayerEventInfo getKeyPlayer()
	{
		return _players.getFirst();
	}
	
	public List<PlayerEventInfo> getPlayers()
	{
		return _players;
	}
	
	public PartyData getParty()
	{
		if(getKeyPlayer().isOnline())
			return getKeyPlayer().getParty();
		return null;
	}
	
	public void register(boolean isRegistered, MiniEventManager registeredEvent)
	{
		for(PlayerEventInfo pi : _players)
		{
			pi.setIsRegisteredToMiniEvent(isRegistered, registeredEvent);
			
			if(!isRegistered)
				CallBack.getInstance().getPlayerBase().eventEnd(pi);
		}
	}
	
	public void message(String msg, boolean screen)
	{
		for(PlayerEventInfo pi : _players)
		{
			if(screen)
				pi.screenMessage(msg, "", true);
			else 
				pi.sendMessage(msg);
		}
	}
	
	public int getAverageLevel()
	{
		int i = 0;
		for(PlayerEventInfo player : _players)
		{
			i += player.getLevel();
		}
		i = Math.round(i/_players.size());
		return i;
	}
	
	public boolean isChosen()
	{
		return _choosen;
	}
	
	public void setIsChosen(boolean b)
	{
		_choosen = b;
	}

	public int getId()
	{
		return _id;
	}
}
