package instances;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2CommandChannel;
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
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Rnd;

public class DemigodsLair extends Quest
{

	
	// NPCs
	private static int			DEVICE			= 90006;
	private static int			TELEPORTER2		= 60008;
	
	private static int[]		TELEPORTER		=
	{
		300001, 300002, 300003, 300004, 300005
	};
	
	private static final int[]	BOSSES			=
	{
	 	310001, 310002, 310003, 310004, 310005
	};
	

	private static String		qn				= "DemigodsLair";
	private static final int	INSTANCEID		= 4000;
	private static boolean		debug			= false;
	private static int			levelReq		= Config.SOLO_LEVELS;
	private static int			pvpReq			= Config.SOLO_PVPS;				// Previously 50
	private static int			healerPvpReq	= Config.SOLO_SUPPORT_PVPS;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	Location loc = null;
	
	public Location getLoc(int npcId)
	{
		switch (npcId)
		{
			case 300001:
				loc = new Location(16325 + Rnd.get(500), 213153 + Rnd.get(500), -9358); //Beleth
				break;
			case 300002:
				loc = new Location(16325 + Rnd.get(500), 213153 - Rnd.get(500), -9358);
				break;
			case 300003:
				loc = new Location(16325 + Rnd.get(500), 213153 - Rnd.get(500), -9358);
				break;
			case 300004:
				loc = new Location(16325 + Rnd.get(500), 213153 - Rnd.get(500), -9358);
				break;
			case 300005:
				loc = new Location(16325 + Rnd.get(500), 213153 - Rnd.get(500), -9358);
				break;
		}
		return loc;
	}
	public class DemigodsLairWorld extends InstanceWorld
	{
		private int	stage		= 0;
		
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
		
		public DemigodsLairWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public DemigodsLair(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(DEVICE);
		addTalkId(DEVICE);
		
		for (int gks : TELEPORTER)
			addTalkId(gks);
		
		addTalkId(TELEPORTER2);
		for (int boss : BOSSES)
			addKillId(boss);
	}
	
	public static void main(String[] args)
	{
		new DemigodsLair(-1, qn, "instances");
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
				if (party.getMemberCount() > 3)
				{
					player.sendMessage("This is a 3 player instance; you cannot enter with a party size > 3 people");
					return false;
				}
				if (party.getMemberCount() < 3)
				{
					player.sendMessage("This is a 3 player instance; you cannot enter with a party size < 3 people");
					return false;
				}
				if (player.getObjectId() != party.getPartyLeaderOID())
				{
					player.sendPacket(new SystemMessage(2185));
					return false;
				}
				if (!checkIPs(party))
					return false;
				boolean canEnter = true;
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						return false;
					if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
					{
						ptm.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
						canEnter = false;
					}
					else if (ptm.getLevel() < levelReq)
					{
						ptm.sendMessage("You must be level " + levelReq + " to enter this instance");
						canEnter = false;
					}
					else if (!ptm.isHealerClass() && ptm.getPvpKills() < pvpReq)
					{
						ptm.sendMessage("You must have " + pvpReq + " PvPs to enter this instance");
						canEnter = false;
					}
					else if (ptm.isHealerClass() && ptm.getPvpKills() < healerPvpReq)
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
					player.sendMessage("This is a 3 player instance; you cannot enter with a party size < 3 people");
					return false;
				}
			}
			return true;
		}
	}
	
	private void teleportplayer(L2PcInstance player, teleCoord teleto)
	{
		player.setInstanceId(teleto.instanceId);
		player.teleToLocation(teleto.x +Rnd.get(400), teleto.y +Rnd.get(400), teleto.z);
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
			world = new DemigodsLairWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info("Demigod's Lair: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
			final L2Party party = player.getParty();
			L2CommandChannel CC = null;
			if (party != null && party.getCommandChannel() != null)
			{
				CC = party.getCommandChannel();
			}
			if (party != null && CC != null)
			{
				for (L2PcInstance ptm : CC.getMembers())
				{
					if (ptm == null)
						continue;
					InstanceManager.getInstance().setInstanceTime(ptm.getAccountName(), INSTANCEID, getNextInstanceTime(HALFDAY));
					// teleport players
					teleto.instanceId = instanceId;
					world.allowed.add(ptm.getObjectId());
					auditInstances(ptm, template, instanceId);
					teleportplayer(ptm, teleto);
					 
				}
			}
			else
			{
				InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(HALFDAY));
				// teleport players
				teleto.instanceId = instanceId;
				world.allowed.add(player.getObjectId());
				auditInstances(player, template, instanceId);
				teleportplayer(player, teleto);
			}
			spawnDemigodsTeleporters((DemigodsLairWorld) world, player);
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
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		if (npcId == DEVICE)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -185624 - (Rnd.get(250));
			teleto.y = 242626 + (Rnd.get(250));
			teleto.z = 1682;
			enterInstance(player, "DemigodsLair.xml", teleto);
		}
		else if (npcId != DEVICE)
		{
			int gkId2 = 0;
			int tempStage = 0;
			final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
			Instance instance = InstanceManager.getInstance().getPlayerInstance(player.getObjectId());
			if (world == null || !(world instanceof DemigodsLairWorld))
				return null;
			final L2Party party = player.getParty();
			L2CommandChannel CC = null;
			if (party != null && party.getCommandChannel() != null)
			{
				CC = party.getCommandChannel();
			}
			final DemigodsLairWorld demigodsWorld = (DemigodsLairWorld) world;
			if (demigodsWorld.getStage() == 0)
			{
				for (int gkId : TELEPORTER)
				{
					if (gkId == 300001 && npcId == gkId)
					{
						gkId2 = gkId;
						tempStage = 1;
						break;
					}
					else if (gkId == 300002 && npcId == gkId)
					{
						gkId2 = gkId;
						tempStage = 2;
						break;
					}
					else if (gkId == 300003 && npcId == gkId)
					{
						gkId2 = gkId;
						tempStage = 3;
						break;
					}
					else if (gkId == 300004 && npcId == gkId)
					{
						gkId2 = gkId;
						tempStage = 4;
						break;
					}
					else if (gkId == 300005 && npcId == gkId)
					{
						gkId2 = gkId;
						tempStage = 5;
						break;
					}
					else
					{
						_log.warning("Demigod Instance Error #100");
					}
				}
				if (party != null && CC != null)
				{
					for (L2PcInstance ptm : CC.getMembers())
					{
						if (ptm == null)
							continue;
						ptm.teleToLocation(getLoc(gkId2), false);
					}
				}
				else if (party != null && CC == null)
				{
					for (L2PcInstance ptm : party.getPartyMembers())
					{
						if (ptm == null)
							continue;
						ptm.teleToLocation(getLoc(gkId2), false);
					}
				}
				else
				{
					player.teleToLocation(getLoc(gkId2), false);
				}
				instance.removeNpcs();
				demigodsWorld.setStage(tempStage);
				spawnDemigods(demigodsWorld, player);
			}
			else
			{
				switch (demigodsWorld.getStage())
				{
					case 0:
						player.teleToLocation(-185624, 242626, 1682, false);//Couldn't happen
						break;
					case 1:
						player.teleToLocation(getLoc(300001), false);
						break;
					case 2:
						player.teleToLocation(getLoc(300002), false);
						break;
					case 3:
						player.teleToLocation(getLoc(300003), false);
						break;
					case 4:
						player.teleToLocation(getLoc(300004), false);
						break;
					case 5:
						player.teleToLocation(getLoc(300005), false);
						break;
				}
			}
		}
		else
		{
			_log.warning("LOL wtf demigodsWorld stage is fucked up!");
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
		if (world == null || !(world instanceof DemigodsLairWorld))
			return null;
		
		switch (npc.getNpcId())
		{
			case 310001:
				break;
				//TODO Add Raids IDs
		}
		
		return null;
	}
	
	
	public void spawnDemigodsTeleporters(DemigodsLairWorld world, L2PcInstance player)
	{
		addSpawn(300001, -185781, 240693, 1571, 2983, false, 0, false, world.instanceId); // Beleth
		addSpawn(300002, -186845, 244136, 1576, 2425, false, 0, false, world.instanceId); // Queen Ant
		addSpawn(300003, -183865, 245089, 1588, 35908, false, 0, false, world.instanceId); // Zaken
		addSpawn(300004, -184017, 243140, 1579, 35216, false, 0, false, world.instanceId); // Baium
		addSpawn(300005, -182880, 241582, 1557, 35699, false, 0, false, world.instanceId); // Frintezza
		// Vortex Gates
		addSpawn(300000, -186890, 244128, 1578, 2390, false, 0, false, world.instanceId); //
		addSpawn(300000, -183810, 245105, 1588, 35598, false, 0, false, world.instanceId); //
		addSpawn(300000, -183863, 243180, 1546, 35259, false, 0, false, world.instanceId); //
		addSpawn(300000, -182755, 241612, 1588, 36025, false, 0, false, world.instanceId); //
		addSpawn(300000, -185863, 240677, 1581, 2390, false, 0, false, world.instanceId); //
	}

	public void spawnDemigods(DemigodsLairWorld world, L2PcInstance player)
	{
		switch (world.getStage())
		{
			case 1:
				addSpawn(300001, -185781, 240693, 1571, 2983, false, 0, false, world.instanceId); // Beleth Teleporter
				addSpawn(310001, 16325, 213144, -9353, 45686, false, 0, false, world.instanceId); // Beleth Raid
				break;
			case 2:
				addSpawn(300002, -186845, 244136, 1576, 2425, false, 0, false, world.instanceId); // Queen Ant Teleporter
				addSpawn(310002, -186845, 244136, 1576, 2425, false, 0, false, world.instanceId); // Queen Ant Raid
				break;
			case 3:
				addSpawn(300003, -183865, 245089, 1588, 35908, false, 0, false, world.instanceId); // Zaken Teleporter
				addSpawn(310003, -183865, 245089, 1588, 35908, false, 0, false, world.instanceId); // Zaken Raid
				break;
			case 4:
				addSpawn(300004, -184017, 243140, 1579, 35216, false, 0, false, world.instanceId); // Baium Teleporter
				addSpawn(310004, -184017, 243140, 1579, 35216, false, 0, false, world.instanceId); // Baium Raid
				break;
			case 5:
				addSpawn(300005, -182880, 241582, 1557, 35699, false, 0, false, world.instanceId); // Frintezza Teleporter
				addSpawn(310005, -182880, 241582, 1557, 35699, false, 0, false, world.instanceId); // Frintezza Raid
				break;
		}
	}
}
