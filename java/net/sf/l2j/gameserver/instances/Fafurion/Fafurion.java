package net.sf.l2j.gameserver.instances.Fafurion;

import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class Fafurion extends Quest
{
	// NPCs
	private static int			DEVICE			= 70000;
	private static int			TELEPORTER		= 70001;
	private static int			TELEPORTER2		= 70002;
	L2Npc						vortexGate		= null;
	L2Npc						teleporterFst	= null;
	L2Npc						teleporterScnd	= null;
	// stronger MOBS
	private static final int[]	MOBS			=
	{
		55550, 55517, 55516, 55515, 55514, 55513, 55512, 55510, 55509, 55508, 55507, 55506, 55505, 55504, 55503, 55502, 55501, 55500, 55328, 55327
	};
	private static String		qn				= "Fafurion";
	private static final int	INSTANCEID		= 2005;
	private static boolean		debug			= false;
	private static int			levelReq		= Config.FAFURION_LEVELS;
	private static int			pvpReq			= Config.FAFURION_PVPS;																								// Previously 50
	private static int			healerPvpReq	= Config.FAFURION_SUPPORT_PVPS;																								// Custom

	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	public class FafurionWorld extends InstanceWorld
	{
		private int	stage		= 0;
		private int	liveMobs	= 0;
		
		public void incStage()
		{
			stage++;
		}
		
		public int getStage()
		{
			return stage;
		}
		
		public void incLiveMobs()
		{
			liveMobs++;
		}
		
		public void decLiveMobs()
		{
			liveMobs--;
			if (liveMobs < 0)
			{
				_log.warning("Error: Fafurion mobs went into negative. ");
			}
		}
		
		public int getLiveMobs()
		{
			return liveMobs;
		}
		
		public FafurionWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public Fafurion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(DEVICE);
		addTalkId(DEVICE);
		addTalkId(TELEPORTER);
		addTalkId(TELEPORTER2);
		for (int mob : MOBS)
			addKillId(mob);
	}
	
	public static void main(String[] args)
	{
		new Fafurion(-1, qn, "instances");
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
			world = new FafurionWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info("Fafurion: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
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
				InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(HALFWEEK));
				// teleport players
				teleto.instanceId = instanceId;
				world.allowed.add(player.getObjectId());
				auditInstances(player, template, instanceId);
				teleportplayer(player, teleto);
			}
			spawn1stMobs((FafurionWorld) world, player);
			return instanceId;
		}
	}
	
	protected void exitInstance(L2PcInstance player, teleCoord tele)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		InstanceManager.getInstance().destroyInstance(world.instanceId);
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
		QuestState st = player.getQuestState(qn);
		if (st != null)
		{
			if (npcId == DEVICE)
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				final FafurionWorld fafWorld = (FafurionWorld) world;
				teleCoord teleto = new teleCoord();
				if (fafWorld == null)
				{
					teleto.x = 183355;
					teleto.y = 249484;
					teleto.z = -3393;
					enterInstance(player, "Fafurion.xml", teleto);
				}
				else
				{
					switch (fafWorld.getStage())
					{
						case 0:
						case 1:
						case 2:
						case 3:
						case 4:
							teleto.x = 183355;
							teleto.y = 249484;
							teleto.z = -3393;
							break;
						case 5:
						case 6:
							teleto.x = 177985;
							teleto.y = 217722;
							teleto.z = -14320;
							break;
						case 7:
						case 8:
							teleto.x = 181222;
							teleto.y = 217137;
							teleto.z = -14309;
							break;
					}
					enterInstance(player, "Fafurion.xml", teleto);
				}
			}
			else if (npcId == TELEPORTER)
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				if (world == null || !(world instanceof FafurionWorld))
					return null;
				final FafurionWorld fafWorld = (FafurionWorld) world;
				Location loc = null;
				switch (fafWorld.getStage())
				{
					case 5:
						loc = new Location(177985, 217722, -14320);
						spawn1stMobs(fafWorld, player);
						break;
					case 7:
						loc = new Location(181222, 217137, -14309);
						spawn1stMobs(fafWorld, player);
						break;
				}
				if (player.getParty() == null)
				{
					// exitInstance(player, teleto);
					player.teleToLocation(loc, false);
					player.sendPacket(new ExShowScreenMessage("Stage: " + String.valueOf(fafWorld.stage), 6000));
				}
				else
				{
					for (L2PcInstance ptm : player.getParty().getPartyMembers())
					{
						ptm.teleToLocation(loc, false);
						ptm.sendPacket(new ExShowScreenMessage("Stage: " + String.valueOf(fafWorld.stage), 6000));
					}
				}
			}
			else if (npcId == TELEPORTER2)
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				if (world == null || !(world instanceof FafurionWorld))
					return null;
				teleCoord teleto = new teleCoord();
				teleto.x = -82993;
				teleto.y = 150860;
				teleto.z = -3129;
				if (player.getParty() == null)
				{
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
					player.sendPacket(new ExShowScreenMessage("You have completed the Fafurion instance", 6000));
				}
				else
				{
					for (L2PcInstance ptm : player.getParty().getPartyMembers())
					{
						exitInstance(ptm, teleto);
						int instanceId = npc.getInstanceId();
						Instance instance = InstanceManager.getInstance().getInstance(instanceId);
						if (instance.getPlayers().isEmpty() && instance != null)
						{
							InstanceManager.getInstance().destroyInstance(instanceId);
						}
						player.sendPacket(new ExShowScreenMessage("You have completed the Fafurion instance", 6000));
					}
				}
				st.exitQuest(true);
			}
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof FafurionWorld))
			return null;
		final FafurionWorld fafWorld = (FafurionWorld) world;
		fafWorld.decLiveMobs();
		final L2Party party = killer.getParty();
		if (party != null)
		{
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				if (ptm == null)
					continue;
			}
		}
		if (fafWorld.getLiveMobs() <= 0)
		{
			fafWorld.liveMobs = 0;
			final int stage = fafWorld.getStage();
			switch (stage)
			{
				case 0: // shouldn't happen
					spawn1stMobs(fafWorld, killer); // Temple OutSide STAGE 1
					break;
				case 1:
					spawn2ndMobs(fafWorld, killer); // Temple OutSide STAGE 2 XONTROS FIGHT
					break;
				case 2:
					spawn3rdMobs(fafWorld, killer); // Temple Inside STAGE 3 mobs fight
					break;
				case 3:
					spawn4thMobs(fafWorld, killer); // Nest Cave Inside STAGE 4 Boss Fight
					break;
				case 4:
					spawn5thMobs(fafWorld, killer); // Fafurion Cave Inside STAGE 5 Minion Fight
					spawnGK1(fafWorld, killer);
					break;
				case 5:
					spawn6thMobs(fafWorld, killer); // Fafurion Cave Inside STAGE 6 Boss Fight
					break;
				case 6:
					spawn7thMobs(fafWorld, killer); // Fafurion BOSS
					spawnGK2(fafWorld, killer);
					break;
				case 7:
					spawnGK3(fafWorld, killer);
					break;
			}
		}
		return null;
	}
	
	public void vortexOut()
	{
		vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "...Huh..?"));
		vortexGate.broadcastPacket(new MagicSkillUse(vortexGate, vortexGate, 892, 1, 4000, 1000));
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "...NOOO!!"));
				vortexGate.broadcastPacket(new MagicSkillUse(vortexGate, vortexGate, 892, 1, 4000, 1000));
			}
		}, 2000);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "..That can't be real..."));
				vortexGate.broadcastPacket(new MagicSkillUse(vortexGate, vortexGate, 773, 1, 6000, 1000));
			}
		}, 4000);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "....HOW COULD YOU????"));
				vortexGate.broadcastPacket(new MagicSkillUse(vortexGate, vortexGate, 773, 1, 6000, 1000));
			}
		}, 6000);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "That must be the End...."));
				vortexGate.broadcastPacket(new MagicSkillUse(vortexGate, vortexGate, 892, 1, 6000, 1000));
			}
		}, 8000);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.broadcastPacket(new CreatureSay(vortexGate.getObjectId(), Say2.BATTLEFIELD, "Entrance Gate", "Time for me to go....\n\r...you may pass now...."));
			}
		}, 10000);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				vortexGate.deleteMe();
			}
		}, 12000);
	}
	
	protected int collectDamage(L2PcInstance player, String template)
	{
		final L2Party party = player.getParty();
		party.getMemberCount();
		return 0;
	}
	
	public void spawnGK1(FafurionWorld world, L2PcInstance player)
	{
		teleporterFst = addSpawn(TELEPORTER, 191241, 256938, -3324, 32767, false, 0, false, world.instanceId); // Fafurion Teleporter
	}
	
	public void spawnGK2(FafurionWorld world, L2PcInstance player)
	{
		teleporterScnd = addSpawn(TELEPORTER, 180508, 217375, -14313, 29133, false, 0, false, world.instanceId); // Fafurion Teleporter
	}
	
	public void spawnGK3(FafurionWorld world, L2PcInstance player)
	{
		addSpawn(TELEPORTER2, 180538, 209168, -14816, 14279, false, 0, false, world.instanceId); // Floor Teleporter
	}
	
	public void spawn1stMobs(FafurionWorld world, L2PcInstance player) // Temple OutSide STAGE 1
	{
		if (world.getStage() == 0)
		{
			vortexGate = spawnGate(55555, 187471, 255685, -3324, 42960, false, 0, false, world.instanceId); // Vortex Gate
			addSpawn(55513, 184425, 251102, -3378, 43568, false, 0, false, world.instanceId); // Fafurion's Sword Master
			world.incLiveMobs();
			addSpawn(55512, 185011, 250889, -3379, 42409, false, 0, false, world.instanceId); // Fafurion's Brawler
			world.incLiveMobs();
			addSpawn(55512, 183977, 251539, -3379, 43235, false, 0, false, world.instanceId); // Fafurion's Brawler
			world.incLiveMobs();
			addSpawn(55510, 184245, 252108, -3378, 43161, false, 0, false, world.instanceId); // Fafurion's Archer
			world.incLiveMobs();
			addSpawn(55505, 184825, 251708, -3378, 41376, false, 0, false, world.instanceId); // Fafurion's Duelist
			world.incLiveMobs();
			addSpawn(55510, 185436, 251325, -3378, 43111, false, 0, false, world.instanceId); // Fafurion's Archer
			world.incLiveMobs();
			addSpawn(55517, 185679, 251882, -3379, 43037, false, 0, false, world.instanceId); // Fafurion's Shaman
			world.incLiveMobs();
			addSpawn(55504, 185218, 252287, -3378, 43408, false, 0, false, world.instanceId); // Fafurion's Pole Master
			world.incLiveMobs();
			addSpawn(55517, 184673, 252562, -3379, 42537, false, 0, false, world.instanceId); // Fafurion's Shaman
			world.incLiveMobs();
			addSpawn(55509, 185464, 253560, -3378, 42723, false, 0, false, world.instanceId); // Fafurion's Serpent
			world.incLiveMobs();
			addSpawn(55508, 186315, 252967, -3378, 42719, false, 0, false, world.instanceId); // Fafurion's Winged Serpent
			world.incLiveMobs();
			addSpawn(55507, 186672, 253519, -3378, 43370, false, 0, false, world.instanceId); // Fafurion's Cleric
			world.incLiveMobs();
			addSpawn(55506, 186237, 253814, -3377, 43240, false, 0, false, world.instanceId); // Fafurion's Wizard
			world.incLiveMobs();
			addSpawn(55507, 185807, 254086, -3378, 42933, false, 0, false, world.instanceId); // Fafurion's Cleric
			world.incLiveMobs();
			addSpawn(55508, 186994, 253966, -3378, 43018, false, 0, false, world.instanceId); // Fafurion's Winged Serpent
			world.incLiveMobs();
			addSpawn(55509, 186096, 254562, -3378, 42824, false, 0, false, world.instanceId); // Fafurion's Serpent
			world.incLiveMobs();
			addSpawn(55512, 186966, 254700, -3347, 42851, false, 0, false, world.instanceId); // Fafurion's Brawler
			world.incLiveMobs();
			addSpawn(55512, 186742, 254816, -3349, 43862, false, 0, false, world.instanceId); // Fafurion's Brawler
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn2ndMobs(FafurionWorld world, L2PcInstance player) // Temple OutSide STAGE 2 XONTROS FIGHT
	{
		if (world.getStage() == 1)
		{
			addSpawn(55503, 187215, 255287, -3322, 43110, false, 0, false, world.instanceId); // Fafurion's Shield Master
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn3rdMobs(FafurionWorld world, L2PcInstance player) // Temple Inside STAGE 3 mobs fight
	{
		if (world.getStage() == 2)
		{
			vortexOut();
			addSpawn(55514, 190285, 256915, -3318, 26986, false, 0, false, world.instanceId); // Gryphon
			world.incLiveMobs();
			addSpawn(55515, 190103, 257524, -3318, 27033, false, 0, false, world.instanceId); // Half Dragon
			world.incLiveMobs();
			addSpawn(55514, 190666, 257698, -3318, 29105, false, 0, false, world.instanceId); // Gryphon
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn4thMobs(FafurionWorld world, L2PcInstance player) // Nest Cave Inside STAGE 4 Boss Fight
	{
		if (world.getStage() == 3)
		{
			addSpawn(55327, 191017, 257058, -3318, 28544, false, 0, false, world.instanceId); // Water Spirit
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn5thMobs(FafurionWorld world, L2PcInstance player) // Fafurion Cave Inside STAGE 5 Minion Fight
	{
		if (world.getStage() == 4)
		{
			addSpawn(55516, 178657, 219153, -14301, 51933, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 179399, 219008, -14301, 42878, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 179795, 218955, -14300, 39805, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 179813, 218541, -14300, 32994, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 178442, 216681, -14303, 14091, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 178807, 216485, -14301, 18306, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55516, 179239, 216839, -14312, 25753, false, 0, false, world.instanceId); // Fafurion's Minion
			world.incLiveMobs();
			addSpawn(55501, 178904, 218026, -14300, 32132, false, 0, false, world.instanceId); // Fafurion's Dragon Mage
			world.incLiveMobs();
			addSpawn(55501, 178790, 217229, -14309, 29373, false, 0, false, world.instanceId); // Fafurion's Dragon Mage
			world.incLiveMobs();
			addSpawn(55502, 179047, 217540, -14295, 31569, false, 0, false, world.instanceId); // Fafurion's Dragon Warrior
			world.incLiveMobs();
			addSpawn(55508, 179544, 217223, -14300, 32426, false, 0, false, world.instanceId); // Fafurion's Winged Serpent
			world.incLiveMobs();
			addSpawn(55509, 179540, 217834, -14300, 34347, false, 0, false, world.instanceId); // Fafurion's Serpent
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn6thMobs(FafurionWorld world, L2PcInstance player) // Fafurion Cave Inside STAGE 6 Boss Fight
	{
		if (world.getStage() == 5)
		{
			addSpawn(55328, 180023, 217435, -14304, 31481, false, 0, false, world.instanceId); // Spirit King
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public void spawn7thMobs(FafurionWorld world, L2PcInstance player) // Fafurion BOSS
	{
		if (world.getStage() == 6)
		{
			addSpawn(55500, 180767, 210605, -14812, 14948, false, 0, false, world.instanceId); // Fafurion
			world.incLiveMobs();
			world.incStage();
		}
	}
	
	public L2Npc spawnGate(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		L2Npc result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template != null)
			{
				if ((x == 0) && (y == 0))
				{
					_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					int offset;
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(70, 120);
					x += offset;
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(70, 120);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setInstanceId(instanceId);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.stopRespawn();
				result = spawn.spawnOne(isSummonSpawn);
				if (despawnDelay > 0)
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
				return result;
			}
			else
			{
				_log.severe("Quest.java addSpawn() called a null NPC to be spawned w/ ID: " + npcId);
			}
		}
		catch (Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}
		return null;
	}
}