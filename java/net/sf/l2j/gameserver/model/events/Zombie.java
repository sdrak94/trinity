package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.util.Rnd;


public class Zombie
{
       public L2PcInstance zombie = null;
       public static List<L2PcInstance> _infected = new ArrayList<L2PcInstance>();
       public static List<L2PcInstance> _notinfected = new ArrayList<L2PcInstance>();
       private String eventMap = "School of Dark Arts";
       public int rounds = 3;
       private static final Location[] SODA_REGULAR_SPAWNS =
    	   {
    			   new Location(-50469, 47782, -5405),
    			   new Location(-44297, 47841, -5405),
    			   new Location(-49405, 51336, -5917),
    			   new Location(-44069, 44648, -5409),
    			   new Location(-50621, 44571, -5409),
    			   new Location(-49738, 44933, -5405),
    			   new Location(-47730, 50556, -5661),
    			   new Location(-44312, 48227, -5921),
    			   new Location(-50464, 48247, -5921),
    			   new Location(-49944, 50057, -5921),
    			   new Location(-47109, 51812, -5917),
    			   new Location(-47198, 47115, -5917),
    			   new Location(-47978, 48119, -5660),
    			   new Location(-49869, 48981, -5921),
    			   new Location(-44869, 49881, -5921),
    			   new Location(-45287, 46174, -5405),
    		};
       private static final Location[] SODA_ZOMBIE_SPAWNS =
    	   {
    			   new Location(-46917, 49801, -5725),
    			   new Location(-45138, 49749, -5917),
    			   new Location(-45558, 49105, -5917),
    			   new Location(-46969, 47184, -5917),
    			   new Location(-47836, 47157, -5917),
    			   new Location(-49326, 47837, -5917),
    			   new Location(-49400, 51318, -5917),
    		};
       private static final Location[] NORNILS_REGULAR_SPAWNS =
    	   {
    			   new Location(-88132, 50987, -4480),
    			   new Location(-87737, 55347, -4576),
    			   new Location(-79295, 55201, -4960),
    			   new Location(-82599, 51033, -4736),
    			   new Location(-83688, 51019, -4736),
    		};
       private static final Location[] NORNILS_ZOMBIE_SPAWNS =
    	   {
    			   new Location(-83104, 54838, -4896),
    			   new Location(-85427, 47509, -3840),
    			   new Location(-78738, 49569, -4320),
    			   new Location(-78757, 51583, -4704),
    			   new Location(-87926, 52803, -4416),
    			   new Location(-87900, 52402, -4416),
    			   new Location(-83589, 54804, -4896),
    			   new Location(-82638, 54824, -4896),
    			   new Location(-83530, 47109, -3840),
    		};
       List<Integer[]> spawnLocations = new ArrayList<Integer[]>();
       
       public static enum State
       {
    	   ACTIVE,
    	   INACTIVE,
    	   REGISTER
       }
      
       public State state = State.INACTIVE;
       
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
    	   if (Rnd.get(100)>50)
    		   eventMap = "Nornil's Garden";
    	   Announcements.getInstance().announceToAll("Zombie event participation started");
    	   Announcements.getInstance().announceToAll("5 minutes till Zombie event registration close");
    	   Announcements.getInstance().announceToAll("Register command: .joinzm || Leave command: .leavezm");
    	   wait(1);
    	   Announcements.getInstance().announceToAll("4 minutes till Zombie event registration close");
    	   wait(1);
    	   Announcements.getInstance().announceToAll("3 minutes till Zombie event registration close");
    	   wait(1);
    	   Announcements.getInstance().announceToAll("2 minutes till Zombie event registration close");
    	   wait(1);
    	   Announcements.getInstance().announceToAll("1 minute till Zombie event registration close");
    	   wait(1);
    	   
    	   if (_notinfected.size() >= 2)
    	   {
    		   state = State.ACTIVE;
    		   closeDoor(24190002);
    		   closeDoor(24190003);
    		   closeDoor(24190001);
    		   closeDoor(24190004);
    		   pickZombie();
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("You will be teleported in Zombie Arena in 5 seconds", 5000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("You will be teleported in Zombie Arena in 5 seconds", 5000);
                       pl.sendPacket(message1);
               }
    		   waitSecs(5);
    		   for (L2PcInstance player : _notinfected)
    		   {
    			   player.sendMessage("Zombie is " + zombie.getName());
    			   if(eventMap.equals("Nornil's Garden"))
    				   player.teleToLocation(NORNILS_REGULAR_SPAWNS[Rnd.get(NORNILS_REGULAR_SPAWNS.length-1)], true);
    			   else
    				   player.teleToLocation(SODA_REGULAR_SPAWNS[Rnd.get(SODA_REGULAR_SPAWNS.length-1)], true);
    			   player.setIsInvul(true);
    			   //player.setInstanceId(100);
    		   }
    		   zombie.sendMessage("Infect others by hitting them");
    		   if(eventMap.equals("Nornil's Garden"))
				   zombie.teleToLocation(NORNILS_ZOMBIE_SPAWNS[Rnd.get(NORNILS_ZOMBIE_SPAWNS.length-1)], false);
			   else
				   zombie.teleToLocation(SODA_ZOMBIE_SPAWNS[Rnd.get(SODA_ZOMBIE_SPAWNS.length-1)], false);
    		   zombie.setIsInvul(true);
    		   //zombie.setInstanceId(100);
    		   //15 minutes
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("15 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("15 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
               wait(5);
               if(state == State.INACTIVE)
        		   return;
               //10 minutes
               for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("10 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("10 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
               wait(5);
               if(state == State.INACTIVE)
        		   return;
               //5 minutes
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("5 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("5 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
    		   wait(1);
    		   if(state == State.INACTIVE)
        		   return;
    		   //4 minutes
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("4 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("4 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
    		   wait(1);
    		   if(state == State.INACTIVE)
        		   return;
    		   //3 minutes
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("3 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("3 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
    		   wait(1);
    		   if(state == State.INACTIVE)
        		   return;
    		   //2 minutes
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("2 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("2 minutes till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
               wait(1);
               if(state == State.INACTIVE)
        		   return;
               //1 minute
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("1 minute till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("1 minute till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
    		   waitSecs(30);
    		   if(state == State.INACTIVE)
        		   return;
    		   //30 seconds
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("30 Seconds till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("30 Seconds till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
               waitSecs(20);
               if(state == State.INACTIVE)
        		   return;
               //10 seconds
    		   for (L2PcInstance pl : _notinfected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("10 seconds till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               for (L2PcInstance pl : _infected)
               {
                       ExShowScreenMessage message1 = new ExShowScreenMessage("10 seconds till Zombie Event is over", 3000);
                       pl.sendPacket(message1);
               }
               if(state == State.INACTIVE)
        		   return;
               waitSecs(10);
               if(state == State.INACTIVE)
        		   return;
    		   end();
    	   }
    	   else
    	   {
    		   Announcements.getInstance().announceToAll("Zombie event has been cancelled due to lack of participants");
    		   state = State.INACTIVE;
    	   }
    	   
       }
      
       public void pickZombie()
       {
               zombie = _notinfected.get(Rnd.get(0, _notinfected.size() - 1));
               infect(zombie);
       }
      
       public void infect(L2PcInstance z)
       {
               _infected.add(z);
               _notinfected.remove(z);
               z.getAppearance().setTitleColor(255, 0, 0);
               z.setZombie(true);
               TransformationManager.getInstance().transformPlayer(303, z);
               if (_notinfected.size() == 0)
				{
					end();
				}
       }
      
       public void uninfectall()
       {
               for (L2PcInstance uninfect : _infected)
               {
            	   uninfect.setZombie(false);
            	   uninfect.untransform();
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
    	   uninfectall();
    	   for (L2PcInstance pl : _notinfected)
           {
                   ExShowScreenMessage message1 = new ExShowScreenMessage("You will be teleported in Giran in 5 seconds", 5000);
                   pl.sendPacket(message1);
           }
           for (L2PcInstance pl : _infected)
           {
                   ExShowScreenMessage message1 = new ExShowScreenMessage("You will be teleported in Giran Town in 5 seconds", 5000);
                   pl.sendPacket(message1);
           }
           waitSecs(5);
    	   for (L2PcInstance p : _infected)
    	   {
    		   p.setInstanceId(0);
    		   p.setIsInZombieEvent(false);
    		   p.setIsInvul(false);
    		   p.teleToLocation(83450, 148608, -3405);
    	   }
    	   for (L2PcInstance p: _notinfected)
    	   {
    		   p.setInstanceId(0);
    		   p.setIsInZombieEvent(false);
    		   p.setIsInvul(false);
    		   p.teleToLocation(83450, 148608, -3405);	
    	   }
    	   rewardWinner();
    	   
    	   if (_notinfected.size() > 0)
    	   {
    		   Announcements.getInstance().announceToAll("Zombie event has end");
    		   Announcements.getInstance().announceToAll("Players won");
    		   _notinfected.removeAll(_notinfected);
    	   }
    	   else
    	   {
    		   
    		   Announcements.getInstance().announceToAll("Zombie event has end");
    		   Announcements.getInstance().announceToAll("Zombies won");
    		   _infected.removeAll(_infected);
    	   }
    	   state = State.INACTIVE;
       }
      
       public void register(L2PcInstance p)
       {
    	   if (p.isInOlympiadMode())
    	   {
    		   p.sendMessage("You can't join Zombie Event in Olympiad mode");
    		   return;
    	   }
    	   if(p.isInSiege())
    	   {
    		   p.sendMessage("You can't join Zombie Event during sieges");
    		   return;
    	   }
    	   if(!_notinfected.contains(p))
    	   {
               _notinfected.add(p);
               p.setIsInZombieEvent(true);
               p.sendMessage("You have succesfully registered");
    	   }
    	   else
    	   {
    		   p.sendMessage("You have already registered");
    	   }
       }
      
       public void unregister(L2PcInstance p)
       {
    	   if(_notinfected.contains(p))
    	   {
               _notinfected.remove(p);
               p.setIsInZombieEvent(false);
               p.sendMessage("You have succesfully unregistered");
    	   }
    	   else
    		   p.sendMessage("You 're not in the registration list");
       }
      
       public void rewardWinner()
       {
               if (_notinfected.size() == 0)
               {
                       for (L2PcInstance n : _infected)
                       {
                               n.addItem("zombie", 57, 10000, n, true);
                       }
               }
               else
               {
                       for (L2PcInstance n : _notinfected)
                       {
                               n.addItem("zombie", 57, 10000, n, true);
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
      
       public Zombie()
       {
    	   
       }
      
       public static Zombie getInstance()
       {
               return SingletonHolder._instance;
       }
      
       private static class SingletonHolder
       {
               protected static final Zombie _instance = new Zombie();
       }
}