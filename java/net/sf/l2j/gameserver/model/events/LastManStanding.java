package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;


public class LastManStanding
{
	
	private static ArrayList<L2PcInstance> players = new ArrayList<L2PcInstance>();
	private Location teleportLocation = new Location(148695,46725,-3414);
	private L2PcInstance lastkiller = null;
	
	public static enum State
	{
		ACTIVE,
		INACTIVE,
		REGISTER
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
		
		Announcements.getInstance().announceToAll("Last Man Standing event participation started");
		Announcements.getInstance().announceToAll("5 minutes till Last Man Standing event registration close");
		Announcements.getInstance().announceToAll("Register command: .joinlms || Leave command: .leavelms");
		wait(1);
		//Announcements.getInstance().announceToAll("4 minutes till Last Man Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("3 minutes till Last Man Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("2 minutes till Last Man Standing event registration close");
		//wait(1);
		//Announcements.getInstance().announceToAll("1 minute till Last Man Standing event registration close");
		//wait(1);
		
		if (players.size() >= 2)
		{
			state = State.ACTIVE;
			closeDoor(24190002);
			closeDoor(24190003);
			closeDoor(24190001);
			closeDoor(24190004);
			for (L2PcInstance p : players)
			{
				if (p==null)
					continue;
				ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Last Man Standing Arena in 5 seconds", 5000);
				p.sendPacket(message);
				InstanceManager.getInstance().createInstance(500);
				p.setInstanceId(500);
				p.broadcastUserInfo();
			}
			waitSecs(5);
			for (L2PcInstance p: players)
			{
				if (p==null)
					continue;
				p.teleToLocation(teleportLocation, true);
			}
			wait(2);
			if (state == State.ACTIVE)
				end();
		}
		else
		{
			Announcements.getInstance().announceToAll("Last Man Standing event has been cancelled due to lack of participants");
			state = State.INACTIVE;
			cleanEvent();
		}
	}
	public State getState()
	{
		return state;
	}
	public void setLastKiller(L2PcInstance killer)
	{
		lastkiller = killer;
	}
	public void removePlayerOnDeath(L2PcInstance p)
	{
		if (p!=null && players.contains(p))
		{
			players.remove(p);
			if (players.size()<=1)
			{
				end();
			}
			p.setInstanceId(0);
			p.setIsInLastManStandingEvent(false);
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					p.doRevive();
					p.teleToLocation(83450, 148608, -3405);
					p.broadcastStatusUpdate();
					p.broadcastUserInfo();
				}
			}, 5000);
		}
	}
	public void removePlayerOnDisconnect(L2PcInstance p)
	{
		if (players.contains(p))
		{
			players.remove(p);
			if (players.size()<=1)
			{
				end();
			}
		}
	}
	public void end()
	{
		if(state == State.INACTIVE)
			return;
		openDoor(24190002);
		openDoor(24190003);
		openDoor(24190001);
		openDoor(24190004);
		rewardWinner();
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
			p.setIsInLastManStandingEvent(false);;
			p.broadcastUserInfo();
		}
		
		cleanEvent();
		state = State.INACTIVE;
	}
	
	private void cleanEvent()
	{
		lastkiller = null;
		players.clear();
	}
	public void register(L2PcInstance p)
	{
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join Last Man Standing Event in Olympiad mode");
			return;
		}
		if(p.isInSiege())
		{
			p.sendMessage("You can't join Last Man Standing Event during sieges");
			return;
		}
		if(!players.contains(p))
		{
			players.add(p);
			p.setIsInLastManStandingEvent(true);
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
			p.setIsInLastManStandingEvent(false);
			p.sendMessage("You have succesfully unregistered");
		}
		else
			p.sendMessage("You 're not in the registration list");
	}
	
	public void rewardWinner()
	{
		if(players.size()==0 && lastkiller!=null) //if the winner disconnects (almost impossible)
		{
			Announcements.getInstance().announceToAll("Unfortunately, Last Man Standing Winner got disconnected");
			Announcements.getInstance().announceToAll("The new Last Man Standing winner is " + lastkiller);
			Announcements.getInstance().announceToAll("Last Man Standing event is over");
			lastkiller.addItem("LastManStanding", 57, 10000, players.get(0), true);
		}
		else if (players.size()==1 && players.get(0)!=null) //only 1 player left
		{
			Announcements.getInstance().announceToAll("Last Man Standing event is over, the winner is " + players.get(0).getName());
			players.get(0).addItem("LastManStanding", 57, 10000, players.get(0), true);
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
	
	private static void closeDoor(int i)
	{
		
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		
		if (doorInstance != null)
		{
			doorInstance.closeMe();
		}
	}
	
	private static void openDoor(int i)
	{
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		
		if (doorInstance != null)
		{
			doorInstance.openMe();
		}
	}
      
	public LastManStanding()
	{
		
	}
	
	public static LastManStanding getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LastManStanding _instance = new LastManStanding();
	}
}