package instances;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
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

public class FreyaInstance extends Quest
{
	// NPCs
	private static int			DEVICE			= 1010035;
	private static int			EXIT_DEVICE		= 1010036;
	private static int[]		MOBS			=
	{
		1010030, 1010031, 1010032, 1010034
	};
	private static String		qn				= "FreyaInstance";
	private static final int	INSTANCEID		= 5004;
	private static boolean		debug			= false;
	
	private static int			levelReq		= Config.FREYA_LEVELS;
	private static int			pvpReq			= Config.FREYA_PVPS;
	private static int			healerPvpReq	= Config.FREYA_SUPPORT_PVPS;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	public class FreyaWorld extends InstanceWorld
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
				_log.warning("WTF Freya declivemobs went into negatives ");
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
		
		public FreyaWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public FreyaInstance(int questId, String name, String descr)
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
		new FreyaInstance(-1, qn, "instances");
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
			world = new FreyaWorld();
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
			spawnMobs((FreyaWorld) world);
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
			teleto.x = 114019;
			teleto.y = -112297;
			teleto.z = -11205;
			enterInstance(player, "Freya.xml", teleto);
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
				player.sendPacket(new ExShowScreenMessage("You have completed the Freya instance", 6000));
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
					player.sendPacket(new ExShowScreenMessage("You have completed the Freya instance", 6000));
				}
			}
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{

		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof FreyaWorld))
			return null;
		final FreyaWorld FreyaWorld = (FreyaWorld) world;
		FreyaWorld.decLiveMobs();
		if (FreyaWorld.getLiveMobs() <= 0)
		{
			FreyaWorld.liveMobs = 0;
			FreyaWorld.incStage();
			spawnMobs(FreyaWorld);
		}
		return null;
	}
	
	public void spawnMobs(FreyaWorld world)
	{
        if (world.getStage() == 0)
        {
            addSpawn(1010030, 114719, -114795, -11205, 14661, false, 0, false, world.instanceId); // Knights
            addSpawn(1010030, 115173, -114781, -11205, 16383, false, 0, false, world.instanceId); // Knights
			addSpawn(1010030, 114274, -114789, -11205, 15438, false, 0, false, world.instanceId); // Knights
            addSpawn(1010031, 114873, -115171, -11208, 22259, false, 0, false, world.instanceId); // Sorc
            addSpawn(1010031, 114571, -115170, -11205, 16383, false, 0, false, world.instanceId); // Sorc
            world.liveMobs = 5;
        }
        if (world.getStage() == 1)
        {
            addSpawn(1010030, 114508, -115735, -11204, 16383, false, 0, false, world.instanceId); // KNIGHT
            addSpawn(1010030, 114930, -115726, -11204, 13730, false, 0, false, world.instanceId); // KNIGHT
            addSpawn(1010030, 115932, -115419, -11203, 27572, false, 0, false, world.instanceId); // KNIGHT
            addSpawn(1010030, 115855, -114133, -11202, 36302, false, 0, false, world.instanceId); // KNIGHT
			addSpawn(1010030, 113532, -114186, -11200, 63772, false, 0, false, world.instanceId); // KNIGHT
            addSpawn(1010030, 113570, -115450, -11200, 3438, false, 0, false, world.instanceId);  // KNIGHT
            world.liveMobs = 6;
        }
        if (world.getStage() == 2)
        {
            addSpawn(1010032, 114721, -115930, -11203, 16068, false, 0, false, world.instanceId); // CAPTAIN
            world.liveMobs = 1;
        }
        if (world.getStage() == 3)
        {
            addSpawn(1010031, 114508, -116931, -11081, 8935, false, 0, false, world.instanceId);  // SORC
            addSpawn(1010031, 114951, -116927, -11081, 25327, false, 0, false, world.instanceId); // SORC
            addSpawn(1010031, 114232, -116671, -11082, 55863, false, 0, false, world.instanceId); // SORC
            addSpawn(1010031, 115199, -116681, -11082, 41664, false, 0, false, world.instanceId); // SORC
            addSpawn(1010030, 114723, -117049, -11081, 16383, false, 0, false, world.instanceId); // KNIGHT
            world.liveMobs = 5;
        }
        if (world.getStage() == 4)
        {
            addSpawn(1010034, 114718, -117078, -11083, 16198, false, 0, false, world.instanceId); // freya
            world.liveMobs = 1;
        }
        if (world.getStage() == 5)
        {
            addSpawn(EXIT_DEVICE, 114718, -117078, -11083, 16198, false, 0, false, world.instanceId); // EXIT
        }
	}
}
