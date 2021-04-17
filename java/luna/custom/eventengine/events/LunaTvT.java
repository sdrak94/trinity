package luna.custom.eventengine.events;

import java.util.Collections;

import luna.custom.eventengine.LunaEvent;
import luna.custom.eventengine.enums.EventState;
import luna.custom.eventengine.enums.TeamType;
import luna.custom.eventengine.managers.EventManager;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public class LunaTvT extends LunaEvent
{
	private int redKills = 0;
	private int blueKills = 0;
	
	public static LunaTvT getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	private static class SingletonHolder
	{
		private static final LunaTvT INSTANCE = new LunaTvT();
	}
	private LunaTvT()
	{
		super("TvT");
	}
	public void startEvent()
	{
		if (!state.equals(EventState.INACTIVE))
			return;
		if (EventManager.getInstance().getActiveEvent()!=null)
			return;
		pickMap();
		if (eventMap == null)
			return;
		EventManager.getInstance().setActiveEvent(this);
		setState(EventState.REGISTER);
		announceRegistration();
		if (players.size()<1)
		{
			Announcements.getInstance().announceToAll("TvT event has been cancelled due to lack of participants");
			for (L2PcInstance p : players)
				if (p!=null)
					p.setIsInEvent(false);
			cleanEvent();
			return;
		}
		setState(EventState.ACTIVE);
		shuffleTeams();
		notifyAndTeleportPlayers();
		wait(1);
		end();
	}

	public void increaseRedKills()
	{
		redKills++;
	}
	public void increaseBlueKills()
	{
		blueKills++;
	}
	@Override
	public void onDeath(L2PcInstance p)
	{
		if(p==null || !players.contains(p))
			return;
		p.sendMessage("You will be revived in 5 seconds!");
		ThreadPoolManager.getInstance().scheduleGeneral(() -> {
			p.doRevive();
			if (p.getTeamType().equals(TeamType.BLUE))
				p.teleToLocation(eventMap.getBlueTeamLocation(), true);
			else
				p.teleToLocation(eventMap.getRedTeamLocation(), true);
			p.broadcastStatusUpdate();
			p.broadcastUserInfo();
		}, 5000L);
	}
	@Override
    public void onDisconnect(L2PcInstance p)
    {
    	if (!players.contains(p))
    		return;
    	players.remove(p);
    	if (getState().equals(EventState.REGISTER))
    		return;
    	if (players.stream().filter( pl -> pl.getTeamType().equals(TeamType.BLUE)).count() == 0)
    		end();
    	else if (players.stream().filter( pl -> pl.getTeamType().equals(TeamType.RED)).count() == 0)
    		end();
    }
	private void shuffleTeams()
	{
		Collections.shuffle(players);
		for (int i=0;i< (players.size());i++)
		{
			if (players.get(i)==null)
				continue;
			if (i%2 ==0)
				players.get(i).setTeamType(TeamType.RED);
			else
				players.get(i).setTeamType(TeamType.BLUE);
			players.get(i).broadcastUserInfo();
		}
	}
	private void notifyAndTeleportPlayers()
	{
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in TvT Arena in 5 seconds", 5000);
			p.sendPacket(message);
		}
		waitSecs(5);
		for(L2PcInstance p: players)
		{
			if (p==null)
				continue;
			p.setInstanceId(instanceId);
			if (p.getTeamType().equals(TeamType.RED))
				p.teleToLocation(eventMap.getRedTeamLocation(), true);
			else
				p.teleToLocation(eventMap.getBlueTeamLocation(), true);
		}
	}
	private void teleportPlayersBack()
	{
		for(L2PcInstance p : players)
		{
			if (p == null)
				continue;
			p.setInstanceId(instanceId);
			Location loc = new Location(83450, 148608, -3405);
			if (loc !=null)
				p.teleToLocation(loc, true);
			p.setIsInEvent(false);
			p.setTeamType(TeamType.NONE);
			p.broadcastUserInfo();
		}
	}
	
	public void end()
	{
		if(state.equals(EventState.INACTIVE))
			return;
		rewardWinner();
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Giran Town in 5 seconds", 5000);
			p.sendPacket(message);
		}
		waitSecs(5);
		teleportPlayersBack();
		cleanEvent();
	}
	
	protected void cleanEvent()
	{
		super.cleanEvent();
		players.clear();
		redKills = 0;
		blueKills = 0;
		setState(EventState.INACTIVE);
		EventManager.getInstance().setActiveEvent(null);
		InstanceManager.getInstance().destroyInstance(instanceId);
	}
	public void rewardWinner()
	{
		if (players.stream().filter( pl -> pl.getTeamType().equals(TeamType.BLUE)).count() == 0)//every trash blue disconnected
		{
			for(L2PcInstance p : players)
			{
				if (p==null )
					continue;
				p.addItem("TvT", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("TvT event is over, red team is victorious!");
		}
		else if (players.stream().filter( pl -> pl.getTeamType().equals(TeamType.RED)).count() == 0)//every trash red disconnected
		{
			for(L2PcInstance p : players)
			{
				if (p==null)
					continue;
				p.addItem("TvT", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("TvT event is over, blue team is victorious!");
		}
		else if (redKills>blueKills) //reward red team
		{
			for(L2PcInstance p : players)
			{
				if (p==null || !p.getTeamType().equals(TeamType.RED))
					continue;
				p.addItem("TvT", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("TvT event is over, red team is victorious!");
		}
		else if (redKills<blueKills)//reward blue team
		{
			for(L2PcInstance p : players)
			{
				if (p==null || !p.getTeamType().equals(TeamType.BLUE))
					continue;
				p.addItem("TvT", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("TvT event is over, blue team is victorious!");
		}
		else //reward in case of draw
		{
			for(L2PcInstance p : players)
				if (p!=null)
					p.addItem("TvT", 57, 10000, p, true);
			Announcements.getInstance().announceToAll("TvT event is over, the game ended in a draw!");
		}
	}
	
	@Override
	public void onRestartPoint(L2PcInstance player) {		
	}
}