package instances;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class FrintezzaInstance extends Quest
{
	// NPCs
	private static int			DEVICE		= 1010025;
	private static int			EXIT_DEVICE	= 1010026;
	private static int[]		MOBS		=
	{
		1010027,1010020,1010021,1010022,1010023,1010024
	};
	private static String		qn			= "FrintezzaInstance";
	private static final int	INSTANCEID	= 5003;
	private static boolean		debug		= false;

	private static int			levelReq		= Config.FRINTEZZA_LEVELS;
	private static int			pvpReq			= Config.FRINTEZZA_PVPS;
	private static int			healerPvpReq	= Config.FRINTEZZA_SUPPORT_PVPS;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	public class FrintezzaWorld extends InstanceWorld
	{
		private int	stage		= 0;
		private int	wave		= 0;
		private int	liveMobs	= 0;
		
		public void incLiveMobs()
		{
			liveMobs++;
		}
		
		public void decLiveMobs()
		{
			liveMobs--;
			if (liveMobs < 0)
			{
				_log.warning("WTF Frintezza declivemobs went into negatives ");
			}
		}
		
		public int getLiveMobs()
		{
			return liveMobs;
		}
		
		public void incStage()
		{
			stage++;
		}
		
		public void setStage(int stageNum)
		{
			stage = stageNum;
		}
		
		public int getStage()
		{
			return stage;
		}
		
		public int getWave()
		{
			return wave;
		}
		
		public void setWave(int waveNum)
		{
			wave = waveNum;
		}
		
		public FrintezzaWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public FrintezzaInstance(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(DEVICE);
		addTalkId(DEVICE);
		addTalkId(EXIT_DEVICE);
		for (int mob : MOBS)
			addKillId(mob);
	}
	
	public static void main(String[] args)
	{
		new FrintezzaInstance(-1, qn, "instances");
	}
	
	private boolean checkConditions(L2PcInstance player, boolean single)
	{
		if (debug || player.isGM())
			return true;
		else
		{
			final L2Party party = player.getParty();
			if (!single && party != null)
			{
				if (party.getMemberCount() < 10)
				{
					player.sendMessage("This is a 10 player instance; you cannot enter with a party size < 10 people");
					return false;
				}
				if (player.getObjectId() != party.getPartyLeaderOID())
				{
					player.sendPacket(new SystemMessage(2185));
					return false;
				}
				boolean canEnter = true;
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						return false;
					if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
					{
                        ptm.sendMessage("You have cooldown for this instance.");
						canEnter = false;
					}
					else if (ptm.getLevel() < levelReq)
					{
						ptm.sendMessage("You must be level " + levelReq + " to enter this instance");
						canEnter = false;
					}
					else if (!ptm.isSurferLee() && ptm.getPvpKills() < pvpReq)
					{
						ptm.sendMessage("You must have " + pvpReq + " PvPs to enter this instance");
						canEnter = false;
					}
					else if (ptm.isSurferLee() && ptm.getPvpKills() < healerPvpReq)
					{
						ptm.sendMessage("Support classes must have " + healerPvpReq + " PvPs to enter this instance");
						canEnter = false;
					}
					else if (ptm.getPvpFlag() != 0 || ptm.getKarma() > 0)
					{
						ptm.sendMessage("You can't enter the instance while in PVP mode or have karma");
						canEnter = false;
					}
					else if (ptm.isInFunEvent())
					{
						ptm.sendMessage("You can't enter the instance while in an event");
						canEnter = false;
					}
					else if (ptm.isInDuel() || ptm.isInOlympiadMode() || Olympiad.getInstance().isRegistered(ptm))
					{
						ptm.sendMessage("You can't enter the instance while in duel/oly");
						canEnter = false;
					}
					else if (!ptm.isInsideRadius(player, 500, true, false))
					{
						ptm.sendMessage("One of your party members is too far away");
						canEnter = false;
					}
					else
					{
						final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
						if (world != null)
						{
							ptm.sendMessage("You can't enter because you have entered into another instance that hasn't expired yet, try waiting 5 min");
							canEnter = false;
						}
					}
					if (!canEnter)
					{
						ptm.sendMessage("You're preventing your party from entering an instance");
						if (ptm != player)
							player.sendMessage(ptm.getName() + " is preventing you from entering the instance");
						return false;
					}
				}
			}
			else
			{
				if (!single)
				{
					player.sendMessage("This is a 10 player instance; you cannot enter with a party size < 10 people");
					return false;
				}
			}
			return true;
		}
	}
	
	private void teleportplayer(L2PcInstance player, teleCoord teleto)
	{
		player.setInstanceId(teleto.instanceId);
		player.teleToLocation(teleto.x, teleto.y, teleto.z);
		L2Summon pet = player.getPet();
		if (pet != null)
		{
			pet.setInstanceId(teleto.instanceId);
			pet.teleToLocation(teleto.x, teleto.y, teleto.z);
		}
		return;
	}
	
	protected int enterInstance(L2PcInstance player, String template, teleCoord teleto)
	{
		int instanceId = 0;
		// check for existing instances for this player
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		// existing instance
		if (world != null)
		{
			if (world.templateId != INSTANCEID)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_ENTERED_ANOTHER_INSTANCE_CANT_ENTER));
				return 0;
			}
			if (!checkConditions(player, true))
				return 0;
			teleto.instanceId = world.instanceId;
			teleportplayer(player, teleto);
			return instanceId;
		}
		else // New instance
		{
			if (!checkConditions(player, false))
				return 0;
			instanceId = InstanceManager.getInstance().createDynamicInstance(template);
			world = new FrintezzaWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info("Frintezza_EASY: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					QuestState st = player.getQuestState(qn);
					if (st == null)
						st = newQuestState(player);
					if (ptm == null)
						continue;
					InstanceManager.getInstance().setInstanceTime(ptm.getAccountName(), INSTANCEID, getNextInstanceTime(HALFWEEK));
					// teleport players
					teleto.instanceId = instanceId;
					world.allowed.add(ptm.getObjectId());
					auditInstances(ptm, template, instanceId);
					teleportplayer(ptm, teleto);
				}
			}
			else
			{
				QuestState st = player.getQuestState(qn);
				if (st == null)
					st = newQuestState(player);
				InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(ONEDAY));
				// teleport players
				teleto.instanceId = instanceId;
				world.allowed.add(player.getObjectId());
				auditInstances(player, template, instanceId);
				teleportplayer(player, teleto);
			}

			storeGates(world);
			spawnMobs(player, (FrintezzaWorld) world, null);
			return instanceId;
		}
	}
	
	protected void exitInstance(L2PcInstance player, teleCoord tele)
	{
		player.setInstanceId(0);
		player.teleToLocation(tele.x, tele.y, tele.z);
		L2Summon pet = player.getPet();
		if (pet != null)
		{
			pet.setInstanceId(0);
			pet.teleToLocation(tele.x, tele.y, tele.z);
		}
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final int npcId = npc.getNpcId();
		if (npcId == DEVICE)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = 174076;
			teleto.y = -76041;
			teleto.z = -5108;
			enterInstance(player, "Frintezza.xml", teleto);
		}
		if (npcId == EXIT_DEVICE)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			if (player.getParty() == null)
			{
				QuestState st = player.getQuestState(qn);
				if (st != null)
				{
					st.exitQuest(true);
				}
				else
				{
					_log.warning(qn + ": Player's: " + player.getName() + "'s quest state is null");
				}
				exitInstance(player, teleto);
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				if (instance != null)
				{
					if (instance.getPlayers().isEmpty())
					{
						InstanceManager.getInstance().destroyInstance(instanceId);
					}
				}
				player.sendPacket(new ExShowScreenMessage("You have completed the Frintezza instance", 6000));
			}
			else
			{
				for (L2PcInstance ptm : player.getParty().getPartyMembers())
				{
					QuestState st = ptm.getQuestState(qn);
					if (st != null)
					{
						st.exitQuest(true);
					}
					else
					{
						_log.warning(qn + ": Player's: " + ptm.getName() + "'s quest state is null");
					}
					exitInstance(ptm, teleto);
					int instanceId = npc.getInstanceId();
					Instance instance = InstanceManager.getInstance().getInstance(instanceId);
					if (instance.getPlayers().isEmpty() && instance != null)
					{
						InstanceManager.getInstance().destroyInstance(instanceId);
					}
					player.sendPacket(new ExShowScreenMessage("You have completed the Frintezza instance", 6000));
				}
			}
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof FrintezzaWorld))
			return null;
		final FrintezzaWorld FrintezzaWorld = (FrintezzaWorld) world;
		FrintezzaWorld.decLiveMobs();
		if (FrintezzaWorld.getLiveMobs() <= 0)
		{
			Location loc = null;
			
			if(npc != null)
				loc = npc.getSpawn().getLastSpawn().getLoc();
			
			FrintezzaWorld.liveMobs = 0;
			FrintezzaWorld.incStage();
			spawnMobs(killer, FrintezzaWorld, loc);
		}
		return null;
	}
	
	public void spawnMobs(L2PcInstance player, FrintezzaWorld world, Location loc)
    {
        if (world.getStage() == 0)
        {
            addSpawn(1010027, 174076, -76041, -5108, 31233, false, 0, false, world.instanceId); // Alarm
            world.liveMobs = 1;
        }
        if (world.getStage() == 1)
        {
            openGates(player, 1);
            addSpawn(1010020, 173192, -76907, -5111, 39884, false, 0, false, world.instanceId);
            addSpawn(1010020, 174107, -77277, -5104, 30603, false, 0, false, world.instanceId);
            addSpawn(1010020, 174927, -76981, -5108, 5141, false, 0, false, world.instanceId);
            addSpawn(1010020, 175354, -76101, -5108, 45693, false, 0, false, world.instanceId);
            addSpawn(1010020, 175021, -75125, -5111, 57865, false, 0, false, world.instanceId);
            addSpawn(1010020, 174112, -74778, -5111, 46398, false, 0, false, world.instanceId);
            addSpawn(1010020, 173263, -75195, -5104, 3968, false, 0, false, world.instanceId);
            addSpawn(1010020, 172884, -76002, -5104, 16383, false, 0, false, world.instanceId);
            world.liveMobs = 8;
        }
        if (world.getStage() == 2) // 2nd Room BOSS STAGE
        {
            openGates(player, 2);
            addSpawn(1010022, 174032, -81872, -5124, 16383, false, 0, false, world.instanceId);
            world.liveMobs = 1;
        }
        if (world.getStage() == 3) // 2nd Room Mobs Stage 1
        {
            openGates(player, 3);
            addSpawn(1010021, 175320, -81762, -5108, 32864, false, 0, false, world.instanceId); 
            addSpawn(1010021, 175367, -82324, -5108, 28440, false, 0, false, world.instanceId);
            addSpawn(1010021, 175311, -81220, -5108, 40689, false, 0, false, world.instanceId);
            addSpawn(1010021, 172795, -81817, -5111, 935, false, 0, false, world.instanceId); 
            addSpawn(1010021, 172739, -81258, -5108, 59709, false, 0, false, world.instanceId);
            addSpawn(1010021, 172739, -82332, -5108, 5324, false, 0, false, world.instanceId);
            world.liveMobs = 6;
        }
        if (world.getStage() == 4) // Mini Boss Spawn
        {
            openGates(player, 4);
            addSpawn(1010023, 174233, -88004, -5115, 15949, false, 0, false, world.instanceId);
            world.liveMobs = 1;
        }
        if (world.getStage() == 5) // Grand Boss Spawn
        {
    		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
    		{
    			@Override
    			public void run()
				{
					if (loc != null)
						addSpawn(1010024, loc.getX(), loc.getY(), loc.getZ(), 15344, false, 0, false, world.instanceId);
					else
						addSpawn(1000024, 174235, -88024, -5115, 16383, false, 0, false, world.instanceId);
					
		            world.liveMobs = 1;
				}
    		}, 2000);
        }
        if (world.getStage() == 6)
        {
            addSpawn(EXIT_DEVICE, 174235, -88024, -5115, 16383, false, 0, false, world.instanceId); // EXIT
        }
    }
	
	protected void openGates(L2PcInstance player, int gateLevel)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		switch (gateLevel)
		{
			//Alarm Room Curtains
			case 1:
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150051).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150052).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150053).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150054).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150055).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150056).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150057).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150058).openMe();
				break;
			case 2:
				//Room 1 -> Room 2 Gate #2
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150042).openMe();
				//Room 1 -> Room 2 Gate #1
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150043).openMe();
				break;
			case 3:
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150061).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150062).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150063).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150064).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150065).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150066).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150067).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150068).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150069).openMe();
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150070).openMe();
				break;
			case 4:
				//Room 2 -> Room 3 Gate #1
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150045).openMe();
				//Room 2 -> Room 3 Gate #2
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(25150046).openMe();
				break;
		}
	}
	
	protected void storeGates(InstanceWorld world)
	{
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150051, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150052, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150053, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150054, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150055, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150056, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150057, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150058, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150042, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150043, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150061, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150062, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150063, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150064, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150065, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150066, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150067, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150068, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150069, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150070, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150045, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(25150046, false);
	}
}
