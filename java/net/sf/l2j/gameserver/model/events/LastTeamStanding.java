package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;
import java.util.Collections;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;


public class LastTeamStanding
{
	
	private static ArrayList<L2PcInstance> players = new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance> blueTeam = new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance> redTeam = new ArrayList<L2PcInstance>();
	private Location redTeamLocation = new Location(148695,46725,-3414);
	private Location blueTeamLocation = new Location(148695,46725,-3414);
	
	public static enum State
	{
		ACTIVE,
		INACTIVE,
		REGISTER
	}
	public static enum LastTeamStandingTeam
	{
		BLUE,
		RED,
		NONE
	}
	
	private State state = State.INACTIVE;
	
	public class Start implements Runnable
	{
		@Override
		public void run()
		{
			if (state == State.INACTIVE)
			{
				startEvent();
			}
		}
	}
	
	public void startEvent()
	{
		
		state = State.REGISTER;
		
		Announcements.getInstance().announceToAll("Last Team Standing event participation started");
		Announcements.getInstance().announceToAll("5 minutes till Last Team Standing event registration close");
		Announcements.getInstance().announceToAll("Register command: .joinlts || Leave command: .leavelts");
		wait(1);
		//Announcements.getInstance().announceToAll("4 minutes till Last Team Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("3 minutes till Last Team Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("2 minutes till Last Team Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("1 minute till Last Team Standing event registration close");
		//wait(1);
		
		if (players.size() >= 2)
		{
			state = State.ACTIVE;
			closeDoors();
			shufflePlayers();
			InstanceManager.getInstance().createInstance(50);
			handleTeleports();
			wait(2);
			if (state == State.ACTIVE)
				end();
		}
		else
		{
			Announcements.getInstance().announceToAll("Last Team Standing event has been cancelled due to lack of participants");
			state = State.INACTIVE;
			cleanEvent();
		}
	}
	public State getState()
	{
		return state;
	}
	private void shufflePlayers()
	{
		Collections.shuffle(players);
		for (int i=0;i< (players.size());i++)
		{
			if (players.get(i)==null)
				continue;
			if (i%2 ==0)
			{
				redTeam.add(players.get(i));
				players.get(i).setLastTeamStandingTeam(LastTeamStandingTeam.RED);
				players.get(i).broadcastUserInfo();
			}
			else
			{
				blueTeam.add(players.get(i));
				players.get(i).setLastTeamStandingTeam(LastTeamStandingTeam.BLUE);
				players.get(i).broadcastUserInfo();
			}
		}
	}
	private void handleTeleports()
	{
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Last Team Standing Arena in 5 seconds", 5000);
			p.sendPacket(message);
			p.setInstanceId(50);
			p.broadcastUserInfo();
		}
		waitSecs(5);
		for (L2PcInstance p: redTeam)
		{
			if (p==null)
				continue;
			p.teleToLocation(redTeamLocation, true);
		}
		for (L2PcInstance p: blueTeam)
		{
			if (p==null)
				continue;
			p.teleToLocation(blueTeamLocation, true);
		}
	}
	public void onDeath(L2PcInstance p)
	{
		if (p==null)
			return;
		if (redTeam.contains(p))
		{
			if(redTeam.stream().filter(L2PcInstance::isDead).count() == redTeam.size())
				end();
		}
		else
		{
			if(blueTeam.stream().filter(L2PcInstance::isDead).count() == blueTeam.size())
				end();
		}
	}
	public void removePlayerOnDisconnect(L2PcInstance p)
	{
		if (getState() == State.REGISTER)
		{
			if (players.contains(p))
			{
				players.remove(p);
			}
		}
		else //active state
		{
			if (blueTeam.contains(p))
			{
				blueTeam.remove(p);
				players.remove(p);
				if (blueTeam.size()==0)
					end();
			}
			else if(redTeam.contains(p))
			{
				redTeam.remove(p);
				players.remove(p);
				if (redTeam.size()==0)
					end();
			}
		}
	}
	public void end()
	{
		if(state == State.INACTIVE)
			return;
		openDoors();
		rewardWinner();
		teleportPlayersBack();
		cleanEvent();
	}
	
	private void cleanEvent()
	{
		state = State.INACTIVE;
		redTeam.clear();
		blueTeam.clear();
		players.clear();
	}
	public void register(L2PcInstance p)
	{
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join Last Team Standing Event in Olympiad mode");
			return;
		}
		if(p.isInSiege())
		{
			p.sendMessage("You can't join Last Team Standing Event during sieges");
			return;
		}
		if(!players.contains(p))
		{
			players.add(p);
			p.setIsInLastTeamStandingEvent(true);
			p.sendMessage("You have succesfully registered");
		}
		else
		{
			p.sendMessage("You have already registered");
		}
	}
	
	public void unregister(L2PcInstance p)
	{
		if(players.contains(p))
		{
			players.remove(p);
			p.setIsInLastTeamStandingEvent(false);
			p.sendMessage("You have succesfully unregistered");
		}
		else
			p.sendMessage("You 're not in the registration list");
	}
	private void teleportPlayersBack()
	{
		for (L2PcInstance p : players)
		{
			if (p==null)
				continue;
			if (p.isDead())
			{
				p.doRevive();
				p.broadcastStatusUpdate();
				p.broadcastUserInfo();
			}
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
			p.setIsInLastTeamStandingEvent(false);
			p.broadcastUserInfo();
		}
	}
	public void rewardWinner()
	{
		if(redTeam.stream().filter(L2PcInstance::isDead).count() == redTeam.size())
		{
			Announcements.getInstance().announceToAll("Last Team Standing Event is over, Blue Team is Victorious");
			for(L2PcInstance p : blueTeam)
			{
				if (p==null)
					continue;
				p.addItem("LastTeamStanding", 57, 5000, p, true);
			}
		}
		else if(blueTeam.stream().filter(L2PcInstance::isDead).count() == blueTeam.size())
		{
			Announcements.getInstance().announceToAll("Last Team Standing Event is over, Red Team is Victorious");
			for(L2PcInstance p : redTeam)
			{
				if (p==null)
					continue;
				p.addItem("LastTeamStanding", 57, 5000, p, true);
			}
		}
		else //both teams have players alive
		{
			if(blueTeam.size()-blueTeam.stream().filter(L2PcInstance::isDead).count() > redTeam.size()-redTeam.stream().filter(L2PcInstance::isDead).count()) // more blue players alive
			{
				Announcements.getInstance().announceToAll("Last Team Standing Event is over, the game has ended in a draw");
				for(L2PcInstance p : blueTeam)
				{
					if (p==null)
						continue;
					p.addItem("LastTeamStanding", 57, 5000, p, true);
				}
			}
			else if(blueTeam.size()-blueTeam.stream().filter(L2PcInstance::isDead).count() < redTeam.size()-redTeam.stream().filter(L2PcInstance::isDead).count()) // more red players alive
			{
				for(L2PcInstance p : redTeam)
				{
					if (p==null)
						continue;
					p.addItem("LastTeamStanding", 57, 5000, p, true);
				}
			}
			else //tie reward
			{
				for(L2PcInstance p : players)
				{
					if (p==null)
						continue;
					p.addItem("LastTeamStanding", 57, 2500, p, true);
				}
			}
		}
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
	private static void closeDoors()
	{
		closeDoor(24190002);
		closeDoor(24190003);
		closeDoor(24190001);
		closeDoor(24190004);
	}
	private static void closeDoor(int i)
	{
		
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		
		if (doorInstance != null)
		{
			doorInstance.closeMe();
		}
	}
	private static void openDoors()
	{
		openDoor(24190002);
		openDoor(24190003);
		openDoor(24190001);
		openDoor(24190004);
	}
	private static void openDoor(int i)
	{
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		
		if (doorInstance != null)
		{
			doorInstance.openMe();
		}
	}
      
	public LastTeamStanding()
	{
		
	}
	
	public static LastTeamStanding getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LastTeamStanding _instance = new LastTeamStanding();
	}
}