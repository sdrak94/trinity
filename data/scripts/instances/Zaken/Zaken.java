package instances.Zaken;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ItemInstance;
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
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;




public class Zaken extends Quest
{
	private class Pirate
	{
		public         L2ItemInstance foodItem = null;
		public boolean isAtDestination         = false;
		public         L2CharPosition oldpos   = null;
	}
	
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
	private static String		qn				= "Zaken";
	private static final int	INSTANCEID		= 2006;
	private static boolean		debug			= false;
	private static int			levelReq		= 90;
	private static int			pvpReq			= 800;																								// Previously 50
	private static int			healerPvpReq	= 400;																								// Custom
	
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
		
		public ZakenWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public Zaken(int questId, String name, String descr)
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
		new Zaken(-1, qn, "instances");
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
			spawn1stMobs((ZakenWorld) world, player);
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
				final ZakenWorld fafWorld = (ZakenWorld) world;
				teleCoord teleto = new teleCoord();
				if (fafWorld == null)
				{
					teleto.x = 52219;
					teleto.y = 218495;
					teleto.z = -3235;
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
				if (world == null || !(world instanceof ZakenWorld))
					return null;
				final ZakenWorld fafWorld = (ZakenWorld) world;
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
				if (world == null || !(world instanceof ZakenWorld))
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
		if (world == null || !(world instanceof ZakenWorld))
			return null;
		final ZakenWorld fafWorld = (ZakenWorld) world;
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
	
	public void spawnGK1(ZakenWorld world, L2PcInstance player)
	{
		teleporterFst = addSpawn(TELEPORTER, 191241, 256938, -3324, 32767, false, 0, false, world.instanceId); // Fafurion Teleporter
	}
	
	public void spawnGK2(ZakenWorld world, L2PcInstance player)
	{
		teleporterScnd = addSpawn(TELEPORTER, 180508, 217375, -14313, 29133, false, 0, false, world.instanceId); // Fafurion Teleporter
	}
	
	public void spawnGK3(ZakenWorld world, L2PcInstance player)
	{
		addSpawn(TELEPORTER2, 180538, 209168, -14816, 14279, false, 0, false, world.instanceId); // Floor Teleporter
	}
	
	public void spawn1stMobs(ZakenWorld world, L2PcInstance player) // Temple OutSide STAGE 1
	{
		if (world.getStage() == 0)
		{
			L2CharPosition locPirate1 = new L2CharPosition(51793, 217203, -3336, 11934);
			
			L2Npc pirate = addSpawn(31391, 51793, 216229, -3232, 14433, false, 0, false, world.instanceId); // Dummy - Boy B

			pirate.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, locPirate1);
			//pirate.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, locPirate1);
			
			
			// world.incStage();
		}
	}


	public boolean isAtDestination(L2Npc npc, Location targetLoc)
	{
		boolean reached = false;
		// System.out.println(""+ npc.getLoc().getX() + ", " + npc.getLoc().getY() + ", " + npc.getLoc().getZ());
		// System.out.println(""+ targetLoc.getX() + ", " + targetLoc.getY() + ", " + targetLoc.getZ());
		if (npc.getLoc().getX() == targetLoc.getX() && npc.getLoc().getY() == targetLoc.getY())
			reached = true;
		return reached;
	}
}