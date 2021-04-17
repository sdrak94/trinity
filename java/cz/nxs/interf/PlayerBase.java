package cz.nxs.interf;

import cz.nxs.l2j.CallBack;
import cz.nxs.l2j.IPlayerBase;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author hNoke
 *
 */
public class PlayerBase implements IPlayerBase
{
	private FastMap<Integer, PlayerEventInfo> players;
	
	//TODO: offline players
	
	public PlayerBase()
	{
		players = new FastMap<Integer, PlayerEventInfo>().shared();
	}
	
	public void load()
	{
		CallBack.getInstance().setPlayerBase(this);
	}
	
	@Override
	public PlayerEventInfo getPlayer(int id)
	{
		return players.get(id);
	}
	
	@Override
	public FastMap<Integer, PlayerEventInfo> getPs()
	{
		return players;
	}
	
	protected PlayerEventInfo getPlayer(L2PcInstance player)
	{
		return player.getEventInfo();
	}
	
	@Override
	public PlayerEventInfo addInfo(PlayerEventInfo player)
	{
		players.put(player.getPlayersId(), player);
		return player;
	}
	
	@Override
	public void eventEnd(PlayerEventInfo player)
	{
		deleteInfo(player.getOwner());
	}
	
	@Override
	public void playerDisconnected(PlayerEventInfo player)
	{
		//TODO support for rejoining after disconnect
		eventEnd(player);
	}
	
	@Override
	public void deleteInfo(int player)
	{
		players.remove(player);
	}
	
	protected void deleteInfo(L2PcInstance player)
	{
		players.remove(player.getObjectId());
	}
	
	public static PlayerBase getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PlayerBase _instance = new PlayerBase();
	}
}
