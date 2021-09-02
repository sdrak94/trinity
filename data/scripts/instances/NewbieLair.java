package instances;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
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
import net.sf.l2j.util.Rnd;

public class NewbieLair extends Quest
{
	// NPCs
	private static int			DEVICE		= 60009;
	private static int			EXIT_DEVICE	= 60010;
	private static int[]		MOBS		=
	{
		1000016, 1000017, 1000018, 1000022, 1000009, 1000023, 1000033, 1000032, 1000024, 1000025, 1000028, 1000026, 1000030, 1000029
	};
	private static String		qn			= "NewbieLair";
	private static final int	INSTANCEID	= 5000;
	private static boolean		debug		= false;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	public class NewbieLairWorld extends InstanceWorld
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
				_log.warning("WTF KAMALOKA declivemobs went into negatives ");
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
		
		public NewbieLairWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public NewbieLair(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(DEVICE);
		addTalkId(DEVICE);
		for (int mob : MOBS)
			addKillId(mob);
	}
	
	public static void main(String[] args)
	{
		new NewbieLair(-1, qn, "instances");
	}
	
	private boolean checkConditions(L2PcInstance player, boolean New)
	{
		if (debug || player.isGM())
			return true;
		else
		{
			if (player.isInParty())
			{
				player.sendMessage("This is a solo player instance");
				return false;
			}
			else if (New && System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(player.getAccountName(), INSTANCEID))
			{
				System.out.println(String.valueOf(System.currentTimeMillis() + " < " + String.valueOf(InstanceManager.getInstance().getInstanceTime(player.getAccountName(), INSTANCEID))));
				player.sendMessage("You can only enter this instance once a day.");
				return false;
			}
			if (player.getLevel() < Config.SOLO_LEVELS)
			{
				player.sendMessage("You must be level 86 to enter this instance");
				return false;
			}
			if (!player.isSupportClassForSoloInstance() && player.getPvpKills() < Config.SOLO_PVPS)
			{
				player.sendMessage("You have at least "+Config.SOLO_PVPS+" pvps to enter this instance");
				return false;
			}
			if (player.isSupportClassForSoloInstance() && player.getPvpKills() < Config.SOLO_SUPPORT_PVPS)
			{
				player.sendMessage("You have at least "+Config.SOLO_SUPPORT_PVPS+" pvps to enter this instance");
				return false;
			}
			else if (player.getPvpFlag() != 0 || player.getKarma() > 0)
			{
				player.sendMessage("You can't enter the instance while in PVP mode or have karma");
				return false;
			}
			else if (player.isInFunEvent())
			{
				player.sendMessage("You can't enter the instance while in an event");
				return false;
			}
			else if (player.isInDuel() || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
			{
				player.sendMessage("You can't enter the instance while in duel/oly");
				return false;
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
			if (!checkConditions(player, false))
				return 0;
			teleto.instanceId = world.instanceId;
			teleportplayer(player, teleto);
			return instanceId;
		}
		else // New instance
		{
			if (!checkConditions(player, true))
				return 0;
			instanceId = InstanceManager.getInstance().createDynamicInstance(template);
			world = new NewbieLairWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info("NewbieLair: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
			
			QuestState st = player.getQuestState(qn);
			if (st == null)
				st = newQuestState(player);
			InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(ONEDAY));
			// teleport players
			teleto.instanceId = instanceId;
			world.allowed.add(player.getObjectId());
			auditInstances(player, template, instanceId);
			teleportplayer(player, teleto);
			preperation(player);
			spawnMobs((NewbieLairWorld) world, player);
			return instanceId;
		}
	}
	protected void preperation(L2PcInstance player)
	{

		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		int tempStage = 0;
		storeGates(player);
		final NewbieLairWorld NewbieLairWorld = (NewbieLairWorld) world;
		if (NewbieLairWorld.getStage() == 0)
		{
			if (player.getPvpKills() < 1000 || player.isSupportClassForSoloInstance())
			{
				tempStage = 1;
			}
			else if (player.getPvpKills() >= 1000)
			{
				tempStage = 2;
			}
			else
			{
				System.out.println("###NewbieLair: new  tempStage: " + tempStage + " created by player: " + player.getName());
				tempStage = 1;
			}
			NewbieLairWorld.setStage(tempStage);
			NewbieLairWorld.setWave(1);
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
	
	protected void openGates(L2PcInstance player, int gateLevel)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		switch (gateLevel)
		{
			case 1:
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(16200015).openMe();
				;
				break;
			case 2:
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(16200016).openMe();
				;
				break;
			case 3:
				InstanceManager.getInstance().getInstance(world.instanceId).getDoor(16200017).openMe();
				;
				break;
		}
	}
	
	protected void storeGates(L2PcInstance player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(16200014, true);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(16200015, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(16200016, false);
		InstanceManager.getInstance().getInstance(world.instanceId).addDoor(16200017, false);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		final int npcId = npc.getNpcId();
		if (npcId == DEVICE)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -111831;
			teleto.y = 87181;
			teleto.z = -12973;
			enterInstance(player, "NewbieLair.xml", teleto);
		}
		if (npcId == EXIT_DEVICE)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
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
			if (instance.getPlayers().isEmpty())
			{
				InstanceManager.getInstance().destroyInstance(instanceId);
			}
			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				player.getCounters().soloDone++;
			}
			player.sendPacket(new ExShowScreenMessage("You have completed the Solo instance!", 6000));
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof NewbieLairWorld))
			return null;
		final NewbieLairWorld NewbieLairWorld = (NewbieLairWorld) world;
		NewbieLairWorld.decLiveMobs();
		if (NewbieLairWorld.getLiveMobs() <= 0)
		{
			NewbieLairWorld.liveMobs = 0;
			incWave(NewbieLairWorld, killer);
		}
		return null;
	}
	
	public void incWave(NewbieLairWorld world, L2PcInstance player)
	{
		int curwave = world.getWave();
		if (world.getWave() > 0 && world.getWave() < 5)
		{
			world.setWave(curwave + 1);
			openGates(player, curwave);
			spawnMobs(world, player);
		}
	}
	
	public void spawnMobs(NewbieLairWorld world, L2PcInstance player)
	{
		if (world.getStage() == 1)
		{
			switch (world.getWave())
			{
				case 1:
					addSpawn(1000016, -113348, 87378, -12884, 32767, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000017, -113552, 87418, -12882, 50667, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000018, -113747, 87378, -12885, 52192, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000022, -113747, 86972, -12882, 8721, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000009, -113550, 86924, -12879, 15910, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000023, -113350, 86978, -12882, 29865, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 2:
					addSpawn(1000016, -115247, 87383, -12782, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000017, -115454, 87426, -12775, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000018, -115665, 87388, -12780, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000022, -115645, 86977, -12782, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000009, -115448, 86902, -12775, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000023, -115241, 86970, -12780, 27319, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 3:
					addSpawn(1000016, -117122, 86975, -12694, 16383, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000017, -117343, 86918, -12685, 13828, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000018, -117547, 86968, -12694, 63477, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000022, -117559, 87386, -12694, 55863, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000009, -117344, 87439, -12685, 49995, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000023, -117147, 87387, -12692, 42114, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 4:
					if (Rnd.get(100) < 50)
					{
						addSpawn(1000029, -119534, 87177, -12591, 62676, false, 0, false, world.instanceId); // P00CHES
					}
					else
					{
						addSpawn(1000030, -119534, 87177, -12591, 62676, false, 0, false, world.instanceId); // P00CHES
					}
					world.liveMobs = 1;
					break;
				case 5:
					addSpawn(EXIT_DEVICE, -119542, 87179, -12590, 30042, false, 0, false, world.instanceId); // Mother Nornil
					break;
			}
		}
		if (world.getStage() == 2)
		{
			switch (world.getWave())
			{
				case 1:
					addSpawn(1000033, -113348, 87378, -12884, 32767, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000032, -113552, 87418, -12882, 50667, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000024, -113747, 87378, -12885, 52192, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000025, -113747, 86972, -12882, 8721, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000028, -113550, 86924, -12879, 15910, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000026, -113350, 86978, -12882, 29865, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 2:
					addSpawn(1000033, -115247, 87383, -12782, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000032, -115454, 87426, -12775, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000024, -115665, 87388, -12780, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000025, -115645, 86977, -12782, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000028, -115448, 86902, -12775, 27319, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000026, -115241, 86970, -12780, 27319, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 3:
					addSpawn(1000033, -117122, 86975, -12694, 16383, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000032, -117343, 86918, -12685, 13828, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000024, -117547, 86968, -12694, 63477, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000025, -117559, 87386, -12694, 55863, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000028, -117344, 87439, -12685, 49995, false, 0, false, world.instanceId); // P00CHES
					addSpawn(1000026, -117147, 87387, -12692, 42114, false, 0, false, world.instanceId); // P00CHES
					world.liveMobs = 6;
					break;
				case 4:
					if (Rnd.get(100) < 50)
					{
						addSpawn(1000029, -119534, 87177, -12591, 62676, false, 0, false, world.instanceId); // P00CHES
					}
					else
					{
						addSpawn(1000030, -119534, 87177, -12591, 62676, false, 0, false, world.instanceId); // P00CHES
					}
					world.liveMobs = 1;
					break;
				case 5:
					addSpawn(EXIT_DEVICE, -119542, 87179, -12590, 30042, false, 0, false, world.instanceId); // Mother Nornil
					break;
			}
		}
	}
}
