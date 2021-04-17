package net.sf.l2j.gameserver.model.entity.events;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.events.PortalData.RaidEventData;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class RaidEvent
{
	private static final int minplayers = 2;
	private static RaidEvent _instance = null;
	private static List<L2PcInstance> players = new ArrayList<>();
	private static L2Spawn boss;
	private static L2Spawn registrationNpc;
	private State state = State.INACTIVE;
	private RaidEventData eventData;
	
	public void setRaidEventData(RaidEventData red)
	{
		if (red!=null)
			eventData = red;
	}
	
	public RaidEventData getCurrentRaidEventData()
	{
		return eventData;
	}
	
	public RaidEvent()
	{
		
	}
	
	public enum State
	{
		ACTIVE,
		INACTIVE,
		REGISTER
	}
	
	public static RaidEvent getInstance()
	{
		if(_instance == null)
			_instance = new RaidEvent();
		return _instance;
	}
	
	public State getState()
	{
		return state;
	}
	
	public void startEvent()
	{
		if(!state.equals(State.INACTIVE) || eventData == null)
			return;
		InstanceManager.getInstance().createInstance(50);
		handleRegistration();
		
		if(players.size()<=minplayers)
		{
			Announcements.getInstance().announceToAll("Raid event has been cancelled due to lack of participants");
			cleanEvent();
			return;
		}
		state = State.ACTIVE;
		spawnBoss();
		notifyAndTeleportPlayers();
		wait(eventData.getRunTime());
		end();
	}
	
	private void handleRegistration()
	{
		state = State.REGISTER;
		Announcements.getInstance().announceToAll("Raid event participation started");
		//more announces
		spawnRegNpc();
		wait(eventData.getRegistrationTime());
		unspawnRegNpc();
	}
	private void spawnRegNpc()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(eventData.getRegistrationNpcId());
		if (tmpl == null)
			return;
		try
		{
			registrationNpc = new L2Spawn(tmpl);
			
			registrationNpc.setLocx(eventData.getRegistrationNpcLocation().getX());
			registrationNpc.setLocy(eventData.getRegistrationNpcLocation().getY());
			registrationNpc.setLocz(eventData.getRegistrationNpcLocation().getZ());
			registrationNpc.setAmount(1);
			registrationNpc.setHeading(0);
			registrationNpc.setRespawnDelay(1);
			registrationNpc.setInstanceId(50);
			SpawnTable.getInstance().addNewSpawn(registrationNpc, false);
			
			registrationNpc.init();
			registrationNpc.getLastSpawn().decayMe();
			registrationNpc.getLastSpawn().spawnMe(registrationNpc.getLastSpawn().getX(), registrationNpc.getLastSpawn().getY(), registrationNpc.getLastSpawn().getZ());
		
		}
		catch (Exception e)
		{
			System.out.println("Couldn't spawn registration npc for Raid Event");
		}
	}
	private void unspawnRegNpc()
	{
		if (registrationNpc == null || registrationNpc.getLastSpawn() == null)
			return;
		
		registrationNpc.getLastSpawn().deleteMe();
		registrationNpc.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(registrationNpc, true);
	}
	private void notifyAndTeleportPlayers()
	{
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Raid Event Location in 5 seconds", 5000);
			p.sendPacket(message);
			p.setInstanceId(50);
		}
		waitSecs(5);
		for (L2PcInstance p : players)
			if (p!=null)
				p.teleToLocation(eventData.getTeleportLocation(),true);
	}
	private void notifyAndTeleportPlayersBack()
	{
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Giran Town in 5 seconds", 5000);
			p.sendPacket(message);
		}
		waitSecs(5);
		for(L2PcInstance p : players)
		{
			if (p == null)
				continue;
			p.teleToLocation(83450, 148608, -3405);
			p.setInstanceId(0);
			p.setIsInRaidEvent(false);
			p.broadcastUserInfo();
		}
	}
	public void onDeath(L2PcInstance p)
	{
		
	}
	public void onReconnect(L2PcInstance p)
	{
		if (!players.contains(p))
			return;
		p.setIsInRaidEvent(true);
		p.setInstanceId(50);
		p.broadcastUserInfo();
	}
    public void onDisconnect(L2PcInstance p)
    {
    	if (p == null)
    		return;
    	if (players.contains(p) && boss.getLastSpawn().getCurrentHp() < boss.getLastSpawn().getMaxHp()*20/100)
    		players.remove(p);
    }
    
	private void spawnBoss()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(eventData.getNpcId());
		if (tmpl == null)
			return;
		try
		{
			boss = new L2Spawn(tmpl);
			boss.setLocx(eventData.getBossLocation().getX());
			boss.setLocy(eventData.getBossLocation().getY());
			boss.setLocz(eventData.getBossLocation().getZ());
			boss.setAmount(1);
			boss.setHeading(0);
			boss.setRespawnDelay(1);
			boss.setInstanceId(50);
			SpawnTable.getInstance().addNewSpawn(boss, false);
			
			boss.init();
			boss.getLastSpawn().decayMe();
			boss.getLastSpawn().spawnMe(boss.getLastSpawn().getX(), boss.getLastSpawn().getY(), boss.getLastSpawn().getZ());
		
		}
		catch (Exception e)
		{
			System.out.println("Couldn't spawn Boss for Raid Event");
		}
	}
	
	private void unspawnBoss()
	{
		if (boss == null || boss.getLastSpawn() == null)
			return;
		
		boss.getLastSpawn().deleteMe();
		boss.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(boss, true);
	}
	
	public void end()
	{
		if(state == State.INACTIVE)
			return;
		unspawnBoss();
		notifyAndTeleportPlayersBack();
		cleanEvent();
	}
	
	private void cleanEvent()
	{
		players.clear();
		boss = null;
		registrationNpc = null;
		state = State.INACTIVE;
	}
	
	public void register(L2PcInstance p)
	{
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join Raid Event in Olympiad mode");
			return;
		}
		if(p.isInSiege())
		{
			p.sendMessage("You can't join Raid Event during sieges");
			return;
		}
		if(!players.contains(p))
		{
			players.add(p);
			p.setIsInRaidEvent(true);
			p.sendMessage("You have succesfully registered");
		}
		else
			p.sendMessage("You have already registered");
	}
	
	public void unregister(L2PcInstance p)
	{
		if(!players.contains(p))
		{
			p.sendMessage("You 're not in the registration list");
			return;
		}
		players.remove(p);
		p.setIsInRaidEvent(false);
		p.sendMessage("You have succesfully unregistered");
	}
	
	public void waitSecs(int i)
	{
		try
		{
			Thread.sleep(i * 1000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	public void wait(int i)
	{
		try
		{
			Thread.sleep(i * 60000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}

}