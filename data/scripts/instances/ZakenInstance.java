package instances;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
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

public class ZakenInstance extends Quest
{
	// NPCs
	private static int			DEVICE		= 1010015;
	private static int			EXIT_DEVICE	= 1010016;
	private static int[]		MOBS		=
	{
	 	1010010,1010011,1010012,1010013
	};
	private static String		qn			= "ZakenInstance";
	private static final int	INSTANCEID	= 5002;
	private static boolean		debug		= false;

	private static int			levelReq		= Config.ZAKEN_LEVELS;
	private static int			pvpReq			= Config.ZAKEN_PVPS;
	private static int			healerPvpReq	= Config.ZAKEN_SUPPORT_PVPS;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	public class ZakenWorld extends InstanceWorld
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
				_log.warning("WTF Zaken declivemobs went into negatives ");
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
		
		public ZakenWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public ZakenInstance(int questId, String name, String descr)
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
		new ZakenInstance(-1, qn, "instances");
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
			world = new ZakenWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info(qn + ": new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
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
			spawnMobs((ZakenWorld) world);
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
			teleto.x = 53168;
			teleto.y = 217143;
			teleto.z = -3751;
			enterInstance(player, "Zaken.xml", teleto);
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
					_log.warning("SOLO INSTANCE: Player's: " + player.getName() + "'s quest state is null");
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
				player.sendPacket(new ExShowScreenMessage("You have completed the Zaken instance", 6000));
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
						_log.warning("SOLO INSTANCE: Player's: " + ptm.getName() + "'s quest state is null");
					}
					exitInstance(ptm, teleto);
					int instanceId = npc.getInstanceId();
					Instance instance = InstanceManager.getInstance().getInstance(instanceId);
					if (instance.getPlayers().isEmpty() && instance != null)
					{
						InstanceManager.getInstance().destroyInstance(instanceId);
					}
					player.sendPacket(new ExShowScreenMessage("You have completed the Zaken instance", 6000));
				}
			}
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof ZakenWorld))
			return null;
		final ZakenWorld ZakenWorld = (ZakenWorld) world;
		ZakenWorld.decLiveMobs();
		if (ZakenWorld.getLiveMobs() <= 0)
		{
			ZakenWorld.liveMobs = 0;
			ZakenWorld.incStage();
			spawnMobs(ZakenWorld);
		}
		return null;
	}
	
	
	public void spawnMobs(ZakenWorld world)
    {
        if (world.getStage() == 0)
        {
            addSpawn(1010011, 54225, 216829, -3742, 32361, false, 0, false, world.instanceId); // CAPTAIN
            addSpawn(1010010, 54336, 216796, -3747, 31654, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 54316, 216871, -3733, 34377, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 54279, 216916, -3728, 35111, false, 0, false, world.instanceId); // ZOMBIE
            world.liveMobs = 4;
        }
        if (world.getStage() == 1)
        {
            addSpawn(1010011, 55628, 216884, -3605, 29577, false, 0, false, world.instanceId); // CAPTAIN
            addSpawn(1010010, 55746, 216841, -3600, 33168, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 55733, 216955, -3600, 35772, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 55618, 216950, -3600, 33221, false, 0, false, world.instanceId); // ZOMBIE
            world.liveMobs = 4;
        }
        if (world.getStage() == 2)
        {
            addSpawn(1010012, 56340, 215861, -3487, 19083, false, 0, false, world.instanceId); // CAPTAIN
            world.liveMobs = 1;
        }
        if (world.getStage() == 3)
        {
            addSpawn(1010011, 52200, 217308, -3341, 51274, false, 0, false, world.instanceId); // CAPTAIN
            addSpawn(1010010, 52081, 217458, -3341, 49568, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 52279, 217464, -3341, 48792, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 52286, 217186, -3335, 49414, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 52066, 217178, -3338, 50948, false, 0, false, world.instanceId); // ZOMBIE
            world.liveMobs = 5;
        }
        if (world.getStage() == 4)
        {
            addSpawn(1010011, 52221, 217248, -3342, 49409, false, 0, false, world.instanceId); // CAPTAIN
            addSpawn(1010010, 52764, 216906, -3333, 31552, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 52754, 217278, -3333, 34302, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 52745, 217580, -3344, 37783, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 51716, 217597, -3342, 359, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 51709, 217258, -3342, 3243, false, 0, false, world.instanceId); // ZOMBIE
            addSpawn(1010010, 51707, 216927, -3344, 61312, false, 0, false, world.instanceId); // ZOMBIE
            world.liveMobs = 7;
        }
        if (world.getStage() == 5)
        {
            addSpawn(1010013, 52207, 217319, -3341, 49644, false, 0, false, world.instanceId); // ZAKEN
            world.liveMobs = 1;
        }
        if (world.getStage() == 6)
        {
            addSpawn(EXIT_DEVICE, 52207, 217319, -3341, 49644, false, 0, false, world.instanceId); // EXIT
        }
    }
}
