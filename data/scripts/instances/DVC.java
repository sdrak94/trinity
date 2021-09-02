package instances;

import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Rnd;

public class DVC extends Quest
{
	// NPCs
	private static int			MALKION				= 96012;
	private static int			EXIT_TELEPORTER		= 90013;
	private static int			MOVE_TELEPORTER1	= 90015;
	private static int			MOVE_TELEPORTER2	= 90016;
	private static int			MOVE_TELEPORTER3	= 90017;
	private static int			MOVE_TELEPORTER4	= 90018;
	private static int			MOVE_TELEPORTER5	= 90019;
	private static int			MOVE_TELEPORTER6	= 90020;
	private static int			MOVE_TELEPORTER7	= 90021;
	private static int			MOVE_TELEPORTER8	= 90022;
	private static int			MOVE_TELEPORTER9	= 90023;
	private static int			MOVE_TELEPORTER10	= 90024;
	private static int			MOVE_TELEPORTER11	= 90025;
	// BOSSES
	private static final int[]	BOSSES				=
	{
		95622, 95623, 95624, 95634
	};
	// private static final int[] calc = {1, 2, 3};
	// final bosses
	private static final int[]	GRAND_BOSSES		=
	{};
	// private static int room1=0;
	private static int			room2				= 0;
	private static int			room3				= 0;
	private static int			room4				= 0;
	private static int			room5				= 0;
	private static int			room6				= 0;
	private static int			room7				= 0;
	private static int			room8				= 0;
	private static int			room9				= 0;
	private static int			room10				= 0;
	private static int			room11				= 0;
	// private static int room12=0;
	private static int			stage				= 0;
	// MOBS
	private static final int[]	MOBS				=
	{
		95132, 95131, 95129
	};
	private static String		qn					= "DVC";
	private static final int	INSTANCEID			= 2001;
	// REQUIREMENTS
	private static boolean		debug				= false;
	private static int			levelReq			= 90;
	private static int			pvpReq				= 300;
	private static int			fameReq				= 0;
	private static int			pkReq				= 0;
	
	private class teleCoord
	{
		int	instanceId;
		int	x;
		int	y;
		int	z;
	}
	
	private static teleCoord teleCurent;
	
	public class dvcWorld extends InstanceWorld
	{
		public dvcWorld()
		{
			InstanceManager.getInstance().super();
		}
	}
	
	public DVC(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(MALKION);
		addTalkId(MALKION);
		addTalkId(EXIT_TELEPORTER);
		for (int boss : BOSSES)
			addKillId(boss);
		for (int mob : MOBS)
			addKillId(mob);
		for (int mob : GRAND_BOSSES)
			addKillId(mob);
	}
	
	public static void main(String[] args)
	{
		new DVC(-1, qn, "instances");
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
				if (party.getMemberCount() < 2 || party.getMemberCount() > 10)
				{
					player.sendMessage("This is a 7-10 player party instance, so you must have a party of 7-10 people");
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
					if (ptm.getLevel() < levelReq)
					{
						ptm.sendMessage("You must be level " + levelReq + " to enter this instance");
						canEnter = false;
					}
					else if (ptm.getPvpKills() < pvpReq)
					{
						ptm.sendMessage("You must have " + pvpReq + " PvPs to enter this instance");
						canEnter = false;
					}
					else if (ptm.getPvpKills() < pkReq)
					{
						ptm.sendMessage("You must have " + pkReq + " PKs to enter this instance");
						canEnter = false;
					}
					else if (ptm.getPvpKills() < fameReq)
					{
						ptm.sendMessage("You must have " + fameReq + " fame to enter this instance");
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
					else if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
					{
						ptm.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
						canEnter = false;
					}
					else if (!ptm.isInsideRadius(player, 500, true, false))
					{
						ptm.sendMessage("You're too far away from your party leader");
						player.sendMessage("One of your party members is too far away");
						canEnter = false;
					} // else {
						// final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					// if (world != null) {
					// ptm.sendMessage("You can't enter because you have entered into another instance that hasn't expired yet, try waiting 5 min");
					// canEnter = false;
					// }
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
					player.sendMessage("This is a 7-10 player party instance, so you must have a party of 7-10 people");
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
			world = new dvcWorld();
			world.instanceId = instanceId;
			world.templateId = INSTANCEID;
			InstanceManager.getInstance().addWorld(world);
			_log.info("DVC: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
			// teleport players
			teleto.instanceId = instanceId;
			L2Party party = player.getParty();
			if (party == null)
			{
				if (!player.isGM())
					return 0;
				stage = 0;
				// Telecurent
				teleCurent = new teleCoord();
				teleCurent.x = -46196;
				teleCurent.y = 246111;
				teleCurent.z = -9129;
				// this can happen only if debug is true
				// random generate rooms
				// room1 = Rnd.get(3);
				room2 = Rnd.get(3);
				room3 = Rnd.get(4);
				room4 = Rnd.get(4);
				room5 = Rnd.get(4);
				room6 = Rnd.get(3);
				room7 = Rnd.get(3);
				room8 = Rnd.get(3);
				room9 = Rnd.get(3);
				room10 = Rnd.get(3);
				room11 = Rnd.get(3);
				// room12 = Rnd.get(3);
				InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(HALFWEEK));
				world.allowed.add(player.getObjectId());
				auditInstances(player, template, instanceId);
				teleportplayer(player, teleto);
				// 32500 -> 180 degree
				// 16250 -> 90
				// 48750 -> 270
				// ------------------------------------------------MOBS------------------------------------------------//
				spawnMobs((dvcWorld) world, player);
				// ------------------------1st Stage------------------------//
				// 1st Room
				// spawnExitGK((dvcWorld) world, player);
			}
			else
			{
				stage = 0;
				// Telecurent
				teleCurent = new teleCoord();
				teleCurent.x = -46196;
				teleCurent.y = 246111;
				teleCurent.z = -9129;
				// random generate rooms
				// room1 = Rnd.get(3);
				room2 = Rnd.get(3);
				room3 = Rnd.get(4);
				room4 = Rnd.get(4);
				room5 = Rnd.get(4);
				room6 = Rnd.get(3);
				room7 = Rnd.get(3);
				room8 = Rnd.get(3);
				room9 = Rnd.get(3);
				room10 = Rnd.get(3);
				room11 = Rnd.get(3);
				// room12 = Rnd.get(3);
				for (L2PcInstance partyMember : party.getPartyMembers())
				{
					partyMember.sendMessage("You have entered the Party Instance");
					InstanceManager.getInstance().setInstanceTime(partyMember.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));
					world.allowed.add(partyMember.getObjectId());
					auditInstances(partyMember, template, instanceId);
					teleportplayer(partyMember, teleto);
				}
				// teleporters
				spawnMobs((dvcWorld) world, player);
				/*
				 * addSpawn(MOVE_TELEPORTER1, -46199, 245517, -9129, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER2, -51630, 245484, -9994, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER2, -109213, -181505, -6759, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER2, -87656, -81804, -8356, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER3, -48120, 243416, -9995, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER3, 147102, 152854, -12174, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER3, 29480, 11031, -4238, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER3, 16331, 208701, -9361, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER4, 53325, 245575, -6572, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER4, -12528, 272922, -9041, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER4, 17500, 244931, 9667, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER4, 143892, 144632, -8951, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER5, 57428, 78497, -3545, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER5, 90425, -7199, -3045, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER5, 183414, -118806, -3082, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER5, 149432, 152608, -12174, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER6, -5252, 55770, -3491, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER6, 12547, -137910, -1884, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER6, -21191, -174902, -9999, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER7, -149359, 256620, -60, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER7, 154169, 143121, -12742, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER7, 143047, -25676, -2046, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER8, -74230, 86507, -5129, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER8, -19022, 280109, -15050, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER8, 15836, 238380, 9770, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER9, 113512, -157408, -1540, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER9, 153977, 121938, -3813, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER9, -90530, 150146, -3631, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER10, 44990, 17921, -4390, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER10, -251434, 214787, -12092, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER10, 95472, -120451, -4435, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER11, -114796, -178095, -6759, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER11, 36294, -48237, -1100, 0, false, 0, false, world.instanceId);
				 * addSpawn(MOVE_TELEPORTER11, -10834, 273930, -11939, 0, false, 0, false, world.instanceId);
				 * addSpawn(EXIT_TELEPORTER, 46801, -106806, -1569, 0, false, 0, false, world.instanceId);
				 * //spawnExitGK((dvcWorld) world, player);
				 */
			}
			return instanceId;
		}
	}
	
	protected void moveLevel(L2PcInstance player, teleCoord tele)
	{
		if (player.getInstanceId() == 0)
			return;
		player.teleToLocation(tele.x, tele.y, tele.z);
		L2Summon pet = player.getPet();
		if (pet != null)
		{
			// pet.setInstanceId(0);
			pet.teleToLocation(tele.x, tele.y, tele.z);
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
		if (st == null)
			st = newQuestState(player);
		if (npcId == MALKION)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -46196;
			teleto.y = 246111;
			teleto.z = -9129;
			enterInstance(player, "DVC.xml", teleto);
		}
		else if (npcId == EXIT_TELEPORTER)
		{
			final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
			if (world == null || !(world instanceof dvcWorld))
				return null;
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			if (player.getParty() == null)
			{
				exitInstance(player, teleto);
				player.sendPacket(new ExShowScreenMessage("You have completed the Party instance.", 6000));
			}
			else
			{
				for (L2PcInstance ptm : player.getParty().getPartyMembers())
				{
					exitInstance(ptm, teleto);
					ptm.sendPacket(new ExShowScreenMessage("You have completed the Party instance.", 6000));
				}
			}
			st.exitQuest(true);
		}
		else if (npcId == MOVE_TELEPORTER1)
		{
			teleCoord teleto = new teleCoord();
			if (stage == 0)
			{
				if (room2 == 1)
				{
					teleto.x = -51627;
					teleto.y = 246119;
					teleto.z = -9994;
				}
				else if (room2 == 2)
				{
					teleto.x = -109812;
					teleto.y = -181509;
					teleto.z = -6759;
				}
				else
				{
					teleto.x = -86998;
					teleto.y = -81823;
					teleto.z = -8361;
				}
			}
			else if (stage != 0 && teleCurent.x == -46196 && teleCurent.y == 246111 && teleCurent.z == -9129)
			{
				if (room2 == 1)
				{
					teleto.x = -51627;
					teleto.y = 246119;
					teleto.z = -9994;
				}
				else if (room2 == 2)
				{
					teleto.x = -109812;
					teleto.y = -181509;
					teleto.z = -6759;
				}
				else
				{
					teleto.x = -86998;
					teleto.y = -81823;
					teleto.z = -8361;
				}
			}
			else
			{
				teleto.x = teleCurent.x;
				teleto.y = teleCurent.y;
				teleto.z = teleCurent.z;
			}
			stage++;
			// teleCurent.x = teleto.x;
			// teleCurent.y = teleto.y;
			// teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("2nd Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("2nd Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("2nd Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("2nd Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER2)
		{
			teleCoord teleto = new teleCoord();
			if (room3 == 1)
			{
				teleto.x = -49496;
				teleto.y = 243412;
				teleto.z = -9995;
			}
			else if (room3 == 2)
			{
				teleto.x = 144885;
				teleto.y = 152612;
				teleto.z = -12174;
			}
			else if (room3 == 3)
			{
				teleto.x = 27229;
				teleto.y = 11037;
				teleto.z = -3984;
			}
			else
			{
				teleto.x = 16333;
				teleto.y = 209599;
				teleto.z = -9361;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("3rd Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("3rd Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("3rd Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("3rd Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER3)
		{
			teleCoord teleto = new teleCoord();
			if (room10 == 1)
			{
				teleto.x = 53340;
				teleto.y = 246353;
				teleto.z = -6584;
			}
			else if (room10 == 2)
			{
				teleto.x = -12527;
				teleto.y = 274719;
				teleto.z = -9041;
			}
			else
			{
				teleto.x = 15480;
				teleto.y = 244006;
				teleto.z = 9667;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("Boss Level reached. 4th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("Boss Level reached. 4th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("Boss Level reached. 4th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("Boss Level reached. 4th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER4)
		{
			teleCoord teleto = new teleCoord();
			if (room4 == 1)
			{
				teleto.x = 56065;
				teleto.y = 79111;
				teleto.z = -3539;
			}
			else if (room4 == 2)
			{
				teleto.x = 89988;
				teleto.y = -7194;
				teleto.z = -3072;
			}
			else if (room4 == 3)
			{
				teleto.x = 186025;
				teleto.y = -120737;
				teleto.z = -3092;
			}
			else
			{
				teleto.x = 147438;
				teleto.y = 144642;
				teleto.z = -8951;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("5th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("5th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("5th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("5th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER5)
		{
			teleCoord teleto = new teleCoord();
			if (room5 == 1)
			{
				teleto.x = -5636;
				teleto.y = 56360;
				teleto.z = -3491;
			}
			else if (room5 == 2)
			{
				teleto.x = 12018;
				teleto.y = -142131;
				teleto.z = -1884;
			}
			else if (room5 == 3)
			{
				teleto.x = -10863;
				teleto.y = -174902;
				teleto.z = -10948;
			}
			else
			{
				teleto.x = 147641;
				teleto.y = 152604;
				teleto.z = -12174;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("6th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("6th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("6th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("6th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER6)
		{
			teleCoord teleto = new teleCoord();
			if (room6 == 1)
			{
				teleto.x = -149362;
				teleto.y = 253295;
				teleto.z = -126;
			}
			else if (room6 == 2)
			{
				teleto.x = 153042;
				teleto.y = 141194;
				teleto.z = -12742;
			}
			else
			{
				teleto.x = 145149;
				teleto.y = -23785;
				teleto.z = -2019;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("7th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("7th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("7th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("7th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER7)
		{
			teleCoord teleto = new teleCoord();
			if (room11 == 1)
			{
				teleto.x = -75276;
				teleto.y = 87753;
				teleto.z = -5161;
			}
			else if (room11 == 2)
			{
				teleto.x = -19026;
				teleto.y = 277436;
				teleto.z = -15050;
			}
			else
			{
				teleto.x = 17817;
				teleto.y = 239460;
				teleto.z = 9770;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("Boss Level reached. 8th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("Boss Level reached. 8th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("Boss Level reached. 8th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("Boss Level reached. 8th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER8)
		{
			teleCoord teleto = new teleCoord();
			if (room7 == 1)
			{
				teleto.x = 113522;
				teleto.y = -153160;
				teleto.z = -1537;
			}
			else if (room7 == 2)
			{
				teleto.x = 152361;
				teleto.y = 119446;
				teleto.z = -3795;
			}
			else
			{
				teleto.x = -90461;
				teleto.y = 149984;
				teleto.z = -3631;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("9th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("9th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("9th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("9th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER9)
		{
			teleCoord teleto = new teleCoord();
			if (room8 == 1)
			{
				teleto.x = 43747;
				teleto.y = 17223;
				teleto.z = -4398;
			}
			else if (room8 == 2)
			{
				teleto.x = -250291;
				teleto.y = 220059;
				teleto.z = -12457;
			}
			else
			{
				teleto.x = 93133;
				teleto.y = -122625;
				teleto.z = -4553;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("10th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("10th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("10th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("10th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER10)
		{
			teleCoord teleto = new teleCoord();
			if (room9 == 1)
			{
				teleto.x = -114782;
				teleto.y = -181929;
				teleto.z = -6759;
			}
			else if (room9 == 2)
			{
				teleto.x = 38306;
				teleto.y = -48240;
				teleto.z = -1157;
			}
			else
			{
				teleto.x = -14167;
				teleto.y = 273932;
				teleto.z = -11939;
			}
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("11th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("11th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("11th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("11th Level", 6000));
			 * }
			 * }
			 */
		}
		else if (npcId == MOVE_TELEPORTER11)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = 47119;
			teleto.y = -108916;
			teleto.z = -1832;
			teleCurent.x = teleto.x;
			teleCurent.y = teleto.y;
			teleCurent.z = teleto.z;
			final L2Party party = player.getParty();
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null)
						continue;
					if (ptm.getInstanceId() == 0)
						continue;
					ptm.teleToLocation(teleto.x, teleto.y, teleto.z, false);
					ptm.sendPacket(new ExShowScreenMessage("Final stage. 12th Level", 6000));
				}
			}
			else
			{
				player.teleToLocation(teleto.x, teleto.y, teleto.z, false);
				player.sendPacket(new ExShowScreenMessage("Final stage. 12th Level", 6000));
			}
			/*
			 * if (player.getParty() == null) {
			 * moveLevel(player, teleto);
			 * player.sendPacket(new ExShowScreenMessage("Final stage. 12th Level", 6000));
			 * } else {
			 * for (L2PcInstance ptm : player.getParty().getPartyMembers()) {
			 * moveLevel(ptm, teleto);
			 * ptm.sendPacket(new ExShowScreenMessage("Final stage. 12th Level", 6000));
			 * }
			 * }
			 */
		}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		return null;
	}
	
	public void spawnExitGK(dvcWorld world, L2PcInstance player)
	{
		addSpawn(EXIT_TELEPORTER, -46197, 245516, -9129, 0, false, 0, false, world.instanceId);
	}
	
	public void spawnMobs(dvcWorld world, L2PcInstance player)
	{
		addSpawn(110024, -46674, 246694, -9124, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46393, 246562, -9125, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46679, 246391, -9124, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46396, 246178, -9125, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46676, 245987, -9124, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46352, 245992, -9125, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46395, 245797, -9125, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46677, 245583, -9124, 0, false, 0, false, world.instanceId);
		addSpawn(110024, -46199, 245662, -9124, 16250, false, 0, false, world.instanceId);
		addSpawn(110024, -45718, 245600, -9124, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45981, 245794, -9125, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45721, 245987, -9124, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -46060, 245987, -9124, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45981, 246180, -9125, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45716, 246383, -9124, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45979, 246565, -9125, 32500, false, 0, false, world.instanceId);
		addSpawn(110024, -45718, 246690, -9124, 32500, false, 0, false, world.instanceId);
		// ------------------------2nd Stage------------------------//
		// 1st Room(Same as first level)HARD ROOM
		addSpawn(110022, -51156, 245538, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51150, 245666, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51153, 245794, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51147, 245925, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51151, 246059, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51152, 246182, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51151, 246310, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51151, 246435, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51151, 246562, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51151, 246690, -9989, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51375, 245669, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51375, 245794, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51373, 245925, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51377, 246053, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51375, 246180, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51375, 246308, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51373, 246435, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51374, 246561, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51379, 246694, -9990, 32500, false, 0, false, world.instanceId);
		addSpawn(110022, -51766, 245540, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(95124, -51488, 245538, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(110022, -51768, 246731, -9989, 48750, false, 0, false, world.instanceId);
		addSpawn(95124, -51490, 246732, -9989, 48750, false, 0, false, world.instanceId);
		addSpawn(95124, -52105, 246688, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52105, 246558, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52107, 246431, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52105, 246309, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52108, 246179, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52103, 246045, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52110, 245928, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52106, 245786, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52110, 245667, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -52107, 245536, -9989, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51888, 246691, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51886, 246559, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51886, 246433, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51885, 246306, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51889, 246178, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51889, 246050, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51888, 245920, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51889, 245795, -9990, 0, false, 0, false, world.instanceId);
		addSpawn(95124, -51889, 245660, -9985, 0, false, 0, false, world.instanceId);
		// 2nd Room(Catacomb)
		addSpawn(110053, -110541, -181259, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -110289, -181258, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -110285, -181382, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -110034, -181269, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -109776, -181257, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -109765, -181390, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -109521, -181261, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -109266, -181260, -6756, 48750, false, 0, false, world.instanceId);
		addSpawn(110053, -109296, -181388, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110054, -110548, -181762, -6756, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -110283, -181759, -6756, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -110028, -181622, -6754, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -110034, -181759, -6756, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -109781, -181761, -6756, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -109525, -181757, -6756, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -109508, -181613, -6754, 16250, false, 0, false, world.instanceId);
		addSpawn(110054, -109265, -181763, -6756, 16250, false, 0, false, world.instanceId);
		// 3rd Room
		addSpawn(110025, -87033, -81280, -8351, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87516, -81807, -8356, 0, false, 0, false, world.instanceId);
		addSpawn(110025, -87035, -82348, -8359, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -86649, -81799, -8356, 32500, false, 0, false, world.instanceId);
		addSpawn(110025, -87178, -81434, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87074, -81436, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -86973, -81537, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -86870, -81537, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -86768, -81639, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -86665, -81636, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87382, -81639, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87279, -81637, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87178, -81740, -8356, 48750, false, 0, false, world.instanceId);
		addSpawn(110025, -87176, -81143, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -87381, -81947, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -87280, -81947, -8359, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -87177, -82049, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -87075, -82050, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -86972, -82149, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -86869, -82151, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -86767, -82253, -8356, 16250, false, 0, false, world.instanceId);
		addSpawn(110025, -86663, -82254, -8356, 16250, false, 0, false, world.instanceId);
		// ------------------------3rd Stage------------------------//
		// 1st Room(Less mobs harded ones)
		addSpawn(110015, -49753, 243016, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -49502, 243292, -9990, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -49263, 243118, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -49023, 243299, -9990, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -48746, 242970, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -48459, 243300, -9990, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -48187, 243119, -9989, 16250, false, 0, false, world.instanceId);
		addSpawn(110015, -49754, 243779, -9989, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -49501, 243534, -9990, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -49266, 243682, -9989, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -49029, 243527, -9990, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -48747, 243819, -9989, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -48465, 243540, -9990, 48750, false, 0, false, world.instanceId);
		addSpawn(110015, -48187, 243672, -9989, 48750, false, 0, false, world.instanceId);
		// 2nd Room(Secret Mob Room)
		addSpawn(110021, 144286, 151964, -12136, 16250, false, 0, false, world.instanceId);// secret
		addSpawn(110020, 145076, 152644, -12164, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 145408, 152436, -12164, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 145547, 152852, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 145817, 152643, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 145951, 152781, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 145953, 152503, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146088, 152851, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146150, 152365, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146286, 152574, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146627, 152574, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146896, 152434, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146896, 152610, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 146890, 152781, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110020, 147094, 152607, -12164, 32500, false, 0, false, world.instanceId);
		addSpawn(110021, 146489, 152308, -12169, (48750 + 32500), false, 0, false, world.instanceId);
		addSpawn(110021, 146489, 152932, -12131, 48750, false, 0, false, world.instanceId);
		addSpawn(110021, 145755, 152942, -12131, 48750, false, 0, false, world.instanceId);
		addSpawn(110021, 145081, 152934, -12131, 48750, false, 0, false, world.instanceId);
		addSpawn(110021, 145082, 152291, -12131, 16250, false, 0, false, world.instanceId);
		addSpawn(110021, 144765, 152304, -12131, 8125, false, 0, false, world.instanceId);
		// 3rd Room(Delven Newbie HARD ROOM)
		addSpawn(110061, 29467, 11225, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 29208, 11222, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 29025, 11215, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28769, 11214, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28637, 11214, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28512, 11214, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28640, 11342, -4229, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28253, 11472, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28888, 11560, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 28076, 11642, -4233, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 27873, 11721, -4229, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 27615, 11725, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 27358, 11725, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 27100, 11726, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 26772, 11562, -4234, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110061, 27231, 11472, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110061, 27497, 11474, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110061, 27871, 11472, -4234, 48750, false, 0, false, world.instanceId);
		addSpawn(110061, 27102, 11344, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110061, 27367, 11343, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110061, 27622, 11340, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110063, 28081, 11030, -4233, 32500, false, 0, false, world.instanceId);
		addSpawn(110062, 29508, 10840, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 29249, 10839, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 29023, 10848, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28884, 10500, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28633, 10848, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28771, 10719, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28641, 10719, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28512, 10718, -4234, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 28085, 10407, -4233, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 27873, 10590, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 27872, 10345, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 27616, 10718, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 27617, 10591, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 27613, 10347, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 27369, 10591, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 27358, 10346, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 27228, 10719, -4234, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 27104, 10593, -4234, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 27103, 10341, -4229, 16250, false, 0, false, world.instanceId);
		addSpawn(110062, 26987, 10722, -4229, (48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110062, 26778, 10501, -4234, (48750 + 16250), false, 0, false, world.instanceId);
		// 4th Room(Closed room)
		addSpawn(110079, 16082, 210270, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15974, 210083, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15822, 209951, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15677, 209794, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15660, 209568, -9355, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15917, 209483, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15663, 209392, -9352, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15736, 209206, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 15907, 209078, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 16080, 208953, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 16208, 208824, -9356, 0, false, 0, false, world.instanceId);
		addSpawn(110079, 16308, 210049, -9356, 48750, false, 0, false, world.instanceId);
		addSpawn(110078, 16331, 209046, -9356, 16250, false, 0, false, world.instanceId);
		addSpawn(110078, 16555, 210273, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16639, 210082, -9355, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16793, 209944, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16941, 209801, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16947, 209569, -9353, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16729, 209479, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16948, 209395, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16886, 209189, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16738, 209077, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16613, 208951, -9356, 32500, false, 0, false, world.instanceId);
		addSpawn(110078, 16461, 208827, -9356, 32500, false, 0, false, world.instanceId);
		// ------------------------4th Stage------------------------//
		// Orc in the pit
		addSpawn(110040, 53327, 245981, -6572, 16250, false, 0, false, world.instanceId);
		// Glabrezu
		addSpawn(110094, -12512, 273916, -9009, 16250, false, 0, false, world.instanceId);
		// Drake in the strange tower
		addSpawn(110091, 16415, 244436, 9676, 35500, false, 0, false, world.instanceId);
		// ------------------------5th Stage------------------------//
		// 1st Room(Elven Lake Ruin)
		addSpawn(110042, 56575, 78490, -3569, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 56945, 78700, -3562, 32500, false, 0, false, world.instanceId);
		addSpawn(110043, 57226, 78690, -3552, 32500, false, 0, false, world.instanceId);
		addSpawn(110042, 57177, 78261, -3594, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 57073, 77704, -3563, 32500, false, 0, false, world.instanceId);
		addSpawn(110042, 56654, 77872, -3553, 32500, false, 0, false, world.instanceId);
		addSpawn(110047, 57480, 77963, -3585, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 57700, 78101, -3585, 32500, false, 0, false, world.instanceId);
		addSpawn(110043, 57960, 78258, -3581, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 58033, 77736, -3563, 32500, false, 0, false, world.instanceId);
		addSpawn(110043, 59366, 77789, -3580, 32500, false, 0, false, world.instanceId);
		addSpawn(110042, 60019, 78148, -3637, 32500, false, 0, false, world.instanceId);
		addSpawn(110047, 60332, 78842, -3640, 32500, false, 0, false, world.instanceId);
		addSpawn(110042, 57527, 79050, -3562, 32500, false, 0, false, world.instanceId);
		addSpawn(110047, 56808, 79090, -3549, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 57230, 79086, -3548, 32500, false, 0, false, world.instanceId);
		addSpawn(110047, 57720, 78954, -3564, 32500, false, 0, false, world.instanceId);
		addSpawn(110043, 58031, 78705, -3576, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 58319, 78311, -3569, 32500, false, 0, false, world.instanceId);
		addSpawn(110047, 57122, 79744, -3586, 0, false, 0, false, world.instanceId);
		addSpawn(110042, 58144, 79331, -3575, 32500, false, 0, false, world.instanceId);
		addSpawn(110043, 58759, 78865, -3577, 32500, false, 0, false, world.instanceId);
		addSpawn(110041, 59685, 79853, -3612, 32500, false, 0, false, world.instanceId);
		// 2nd Room(Ruin castle HARD ROOM)
		addSpawn(110033, 89260, -8844, -2798, 16250, false, 0, false, world.instanceId);
		addSpawn(110033, 89842, -8950, -2778, 16250, false, 0, false, world.instanceId);
		addSpawn(110033, 90616, -8827, -2782, 16250, false, 0, false, world.instanceId);
		addSpawn(110033, 91902, -6253, -3038, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 91700, -5806, -3087, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 91321, -4947, -3252, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 90397, -3666, -3358, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 89238, -3027, -3365, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 90138, -4916, -3025, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 89165, -4234, -3072, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 88756, -3811, -3140, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 87257, -3836, -3233, 48750, false, 0, false, world.instanceId);
		addSpawn(110033, 88370, -4945, -3131, 0, false, 0, false, world.instanceId);
		addSpawn(110032, 90046, -7738, -3018, 16250, false, 0, false, world.instanceId);
		addSpawn(110032, 90234, -7179, -3052, 32500, false, 0, false, world.instanceId);
		addSpawn(110032, 90105, -6708, -3074, 48750, false, 0, false, world.instanceId);
		addSpawn(110032, 89677, -7346, -3072, 0, false, 0, false, world.instanceId);
		addSpawn(110032, 89298, -7356, -3097, 0, false, 0, false, world.instanceId);
		addSpawn(110032, 89174, -6944, -3142, 0, false, 0, false, world.instanceId);
		addSpawn(110032, 89380, -6714, -3140, 48750, false, 0, false, world.instanceId);
		// 3rd Room(Forge of the gods)
		addSpawn(110008, 185671, -120938, -3075, 0, false, 0, false, world.instanceId);
		addSpawn(110001, 185710, -120309, -3082, 48750, false, 0, false, world.instanceId);
		addSpawn(110001, 185336, -120117, -3087, 48750, false, 0, false, world.instanceId);
		addSpawn(110008, 184949, -120211, -3087, 0, false, 0, false, world.instanceId);
		addSpawn(110004, 184444, -120583, -3074, 0, false, 0, false, world.instanceId);
		addSpawn(110004, 184630, -120897, -3077, 0, false, 0, false, world.instanceId);
		addSpawn(110000, 185020, -121366, -3073, 0, false, 0, false, world.instanceId);
		addSpawn(110008, 184928, -121053, -3088, 0, false, 0, false, world.instanceId);
		addSpawn(110002, 185423, -120596, -3083, 0, false, 0, false, world.instanceId);
		addSpawn(110004, 184890, -120579, -3074, 0, false, 0, false, world.instanceId);
		addSpawn(110001, 184303, -120148, -3084, 0, false, 0, false, world.instanceId);
		addSpawn(110008, 184235, -119718, -3077, 48750, false, 0, false, world.instanceId);
		addSpawn(110008, 184417, -119180, -3077, 48750, false, 0, false, world.instanceId);
		addSpawn(110000, 184194, -118744, -3079, 48750, false, 0, false, world.instanceId);
		addSpawn(110001, 183609, -118659, -3080, 48750, false, 0, false, world.instanceId);
		addSpawn(110000, 183374, -119165, -3087, 0, false, 0, false, world.instanceId);
		addSpawn(110002, 183540, -119502, -3083, 0, false, 0, false, world.instanceId);
		addSpawn(110001, 183964, -119180, -3088, 0, false, 0, false, world.instanceId);
		addSpawn(110008, 183581, -120061, -3075, 0, false, 0, false, world.instanceId);
		// 4th Room(Top of Parnasus)
		addSpawn(110017, 148200, 144384, -8946, 32500, false, 0, false, world.instanceId);
		addSpawn(110017, 147851, 144021, -8946, 26250, false, 0, false, world.instanceId);
		addSpawn(110017, 147131, 143780, -8946, 16250, false, 0, false, world.instanceId);
		addSpawn(110017, 146109, 143766, -8946, 16250, false, 0, false, world.instanceId);
		addSpawn(110017, 145281, 143794, -8946, 16250, false, 0, false, world.instanceId);
		addSpawn(110017, 144195, 143726, -8946, 16250, false, 0, false, world.instanceId);
		addSpawn(110017, 143472, 144044, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110017, 143224, 144416, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110017, 143203, 144867, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110017, 143480, 145208, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110017, 144305, 145503, -8946, 48750, false, 0, false, world.instanceId);
		addSpawn(110017, 145249, 145501, -8946, 48750, false, 0, false, world.instanceId);
		addSpawn(110017, 146055, 145543, -8946, 48750, false, 0, false, world.instanceId);
		addSpawn(110017, 147089, 145466, -8946, 48750, false, 0, false, world.instanceId);
		addSpawn(110017, 147894, 145289, -8946, 48750, false, 0, false, world.instanceId);
		addSpawn(110017, 148142, 144834, -8946, 32500, false, 0, false, world.instanceId);
		addSpawn(110016, 146085, 144662, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110016, 145593, 144378, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110016, 145585, 144892, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110016, 144790, 144678, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110016, 144259, 144159, -8946, 0, false, 0, false, world.instanceId);
		addSpawn(110016, 144247, 145071, -8946, 0, false, 0, false, world.instanceId);
		// ------------------------6th Stage------------------------//
		// 1st Room (Delven waterfall)
		addSpawn(110068, -5176, 55806, -3486, 20000, false, 0, false, world.instanceId);
		addSpawn(110034, -5234, 55360, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110034, -5204, 54713, -3486, 32500, false, 0, false, world.instanceId);
		addSpawn(110077, -5761, 54525, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -5492, 53870, -3486, 32500, false, 0, false, world.instanceId);
		addSpawn(110068, -6055, 53473, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -6025, 52845, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -5626, 52909, -3486, 48750, false, 0, false, world.instanceId);
		addSpawn(110077, -5308, 52229, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110034, -4140, 52191, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110068, -4222, 52671, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -3392, 53153, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110068, -2978, 52805, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -1835, 53517, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110034, -1486, 53123, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110077, -1112, 53709, -3486, 32500, false, 0, false, world.instanceId);
		addSpawn(110034, -1695, 54493, -3486, 32500, false, 0, false, world.instanceId);
		addSpawn(110077, -2207, 54443, -3486, 0, false, 0, false, world.instanceId);
		addSpawn(110034, -2408, 55209, -3486, 48750, false, 0, false, world.instanceId);
		addSpawn(110068, -3223, 55217, -3486, 32500, false, 0, false, world.instanceId);
		addSpawn(110034, -3405, 54740, -3486, 16250, false, 0, false, world.instanceId);
		addSpawn(110077, -4664, 55447, -3486, 32500, false, 0, false, world.instanceId);
		// 2nd Room (Frozen)
		addSpawn(110010, 9322, -141193, -1495, 0, false, 0, false, world.instanceId);
		addSpawn(110011, 10344, -141848, -1879, 0, false, 0, false, world.instanceId);
		addSpawn(110010, 10874, -142248, -1879, 0, false, 0, false, world.instanceId);
		addSpawn(110012, 12722, -142889, -1721, 20000, false, 0, false, world.instanceId);
		addSpawn(110011, 11984, -142768, -1843, 16250, false, 0, false, world.instanceId);
		addSpawn(110012, 13240, -142371, -1706, 16250, false, 0, false, world.instanceId);
		addSpawn(110012, 13441, -143154, -1607, 23000, false, 0, false, world.instanceId);
		addSpawn(110010, 14359, -141403, -1764, 32500, false, 0, false, world.instanceId);
		addSpawn(110012, 15194, -138081, -1781, 48750, false, 0, false, world.instanceId);
		addSpawn(110010, 14877, -137059, -1516, 48750, false, 0, false, world.instanceId);
		addSpawn(110011, 14293, -138045, -1753, 48750, false, 0, false, world.instanceId);
		addSpawn(110010, 11097, -138048, -1772, 0, false, 0, false, world.instanceId);
		addSpawn(110012, 13370, -138474, -1776, 48750, false, 0, false, world.instanceId);
		addSpawn(110010, 10682, -131578, -1746, 0, false, 0, false, world.instanceId);
		addSpawn(110013, 11103, -140453, -1879, 0, false, 0, false, world.instanceId);
		addSpawn(110014, 10545, -139586, -1879, 0, false, 0, false, world.instanceId);
		addSpawn(110014, 12053, -139823, -1879, 48750, false, 0, false, world.instanceId);
		addSpawn(110013, 12708, -140540, -1879, 48750, false, 0, false, world.instanceId);
		addSpawn(110012, 13592, -141189, -1879, 32500, false, 0, false, world.instanceId);
		addSpawn(110013, 14103, -140533, -1879, 32500, false, 0, false, world.instanceId);
		addSpawn(110012, 13300, -139893, -1879, 48750, false, 0, false, world.instanceId);
		addSpawn(110014, 12328, -138907, -1879, 48750, false, 0, false, world.instanceId);
		addSpawn(110012, 13096, -139173, -1879, 48750, false, 0, false, world.instanceId);
		addSpawn(110013, 12667, -138250, -1879, 48750, false, 0, false, world.instanceId);
		// 3rd Room (Kamaloka Room HARD)
		addSpawn(110069, -11846, -174742, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12056, -174540, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12375, -174537, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12582, -174736, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12581, -175069, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12381, -175271, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12038, -175263, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -11840, -175073, -10931, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12032, -174899, -10947, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12207, -175071, -10947, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12392, -174905, -10947, 0, false, 0, false, world.instanceId);
		addSpawn(110069, -12208, -174728, -10947, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14077, -174904, -10668, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14117, -174531, -10665, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14543, -174345, -10663, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14968, -174529, -10665, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -15003, -174902, -10668, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14968, -175204, -10665, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14541, -175492, -10663, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14112, -175202, -10665, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -14541, -174904, -10684, 0, false, 0, false, world.instanceId);
		addSpawn(110075, -15224, -174903, -10668, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16516, -174897, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16618, -174654, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16879, -174545, -10401, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -17139, -174656, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -17234, -174905, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -17132, -175143, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16881, -175261, -10400, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16605, -175145, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16657, -174990, -10421, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16647, -174813, -10421, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16773, -174688, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16978, -174689, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -17098, -174812, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -17099, -174991, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16973, -175115, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16783, -175115, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16751, -174903, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16878, -174778, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16998, -174904, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110055, -16874, -175025, -10405, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20396, -174795, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20549, -174645, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20759, -174645, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20912, -174798, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20913, -175008, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20761, -175161, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20547, -175163, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20395, -175009, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(110056, -20656, -174904, -9977, 0, false, 0, false, world.instanceId);
		// 4rth room (Closed Room)
		addSpawn(110030, 147968, 152363, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 147968, 152502, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 147968, 152642, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 147968, 152781, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148173, 152434, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148173, 152573, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148173, 152712, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148173, 152850, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 148507, 152364, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 148507, 152503, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 148507, 152643, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 148507, 152780, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148711, 152432, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148711, 152571, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148711, 152711, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 148711, 152150, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 149046, 152365, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 149046, 152503, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 149046, 152643, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110030, 149046, 152780, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 149247, 152434, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 149247, 152572, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 149247, 152712, -12169, 32500, false, 0, false, world.instanceId);
		addSpawn(110031, 149247, 152852, -12169, 32500, false, 0, false, world.instanceId);
		// ------------------------7th Stage------------------------//
		// 1st Room (Gludio airship) HARD ROOM
		addSpawn(110027, -149303, 253883, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149415, 253884, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149185, 254111, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149530, 254110, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149417, 254338, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149301, 254340, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149156, 254608, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149562, 254606, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149963, 254659, -186, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -148752, 254661, -186, 48750, false, 0, false, world.instanceId);
		addSpawn(110080, -149509, 255159, -164, 48750, false, 0, false, world.instanceId);
		addSpawn(110080, -149209, 255163, -164, 48750, false, 0, false, world.instanceId);
		addSpawn(110080, -149212, 255460, -164, 48750, false, 0, false, world.instanceId);
		addSpawn(110080, -149505, 255462, -164, 48750, false, 0, false, world.instanceId);
		addSpawn(110080, -149360, 255311, -164, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149193, 256214, -180, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149523, 256217, -180, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -149968, 255960, -185, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -150308, 256214, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -148757, 255968, -185, 48750, false, 0, false, world.instanceId);
		addSpawn(110027, -148408, 256205, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -147730, 255339, -182, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -147827, 255300, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -147923, 255343, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148019, 255300, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148113, 255338, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148210, 255302, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148308, 255340, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148400, 255301, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -148497, 255340, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150989, 255341, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150893, 255301, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150797, 255340, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150701, 255301, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150605, 255340, -183, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150509, 255301, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150413, 255339, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150319, 255286, -184, 48750, false, 0, false, world.instanceId);
		addSpawn(110026, -150219, 255338, -183, 48750, false, 0, false, world.instanceId);
		// 2nd Room (Closed Room like boss)
		addSpawn(110072, 153572, 142072, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110072, 153748, 141891, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110072, 153390, 141887, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110072, 153391, 142253, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110072, 153747, 142257, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110074, 154362, 142076, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110074, 153575, 142860, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110074, 152785, 142075, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110074, 153568, 141278, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110073, 153563, 140802, -12737, 16250, false, 0, false, world.instanceId);
		addSpawn(110073, 154209, 140989, -12737, 26000, false, 0, false, world.instanceId);
		addSpawn(110073, 154696, 141434, -12737, 29000, false, 0, false, world.instanceId);
		addSpawn(110073, 154863, 142087, -12737, 32500, false, 0, false, world.instanceId);
		addSpawn(110073, 154668, 142729, -12737, 35000, false, 0, false, world.instanceId);
		addSpawn(110073, 154159, 143192, -12737, 46750, false, 0, false, world.instanceId);
		addSpawn(110073, 153569, 143351, -12737, 48750, false, 0, false, world.instanceId);
		addSpawn(110073, 152941, 143168, -12737, 51000, false, 0, false, world.instanceId);
		addSpawn(110073, 152461, 142715, -12737, 0, false, 0, false, world.instanceId);
		addSpawn(110073, 152310, 142068, -12737, 0, false, 0, false, world.instanceId);
		addSpawn(110073, 152471, 141435, -12737, 0, false, 0, false, world.instanceId);
		// 3rd Room (Blazing Swamp)
		addSpawn(110023, 145661, -24501, -2076, 16250, false, 0, false, world.instanceId);
		addSpawn(110006, 144843, -24589, -2087, 16250, false, 0, false, world.instanceId);
		addSpawn(110006, 144317, -24800, -2111, 16250, false, 0, false, world.instanceId);
		addSpawn(110005, 144048, -24365, -2044, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110007, 143588, -24763, -2062, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110009, 143657, -25418, -2100, 16250, false, 0, false, world.instanceId);
		addSpawn(110023, 143133, -25095, -2043, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110009, 142739, -25627, -2019, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110006, 142617, -26609, -2072, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110009, 143034, -27016, -2024, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110009, 145220, -25123, -2209, 16250, false, 0, false, world.instanceId);
		addSpawn(110005, 144736, -25643, -2154, 16250, false, 0, false, world.instanceId);
		addSpawn(110023, 144901, -26726, -2154, 16250, false, 0, false, world.instanceId);
		addSpawn(110007, 144407, -24081, -2118, (16250 + 48750 + 6000), false, 0, false, world.instanceId);
		addSpawn(110023, 143778, -27349, -2115, 16250, false, 0, false, world.instanceId);
		addSpawn(110005, 143990, -26533, -2115, 16250, false, 0, false, world.instanceId);
		addSpawn(110023, 143562, -26751, -2115, 16250, false, 0, false, world.instanceId);
		addSpawn(110007, 144315, -27769, -1948, 16250, false, 0, false, world.instanceId);
		addSpawn(110005, 144907, -27904, -1944, 16250, false, 0, false, world.instanceId);
		addSpawn(110006, 145099, -27404, -2026, 16250, false, 0, false, world.instanceId);
		// ------------------------8th Stage------------------------//
		// 1st Room
		// RB horse catacomb
		addSpawn(110095, -74854, 87138, -5120, 16250, false, 0, false, world.instanceId);
		addSpawn(1000030, -75321, 87132, -5124, 0, false, 0, false, world.instanceId);
		addSpawn(1000031, -74859, 87600, -5124, 16250, false, 0, false, world.instanceId);
		addSpawn(1000032, -74384, 87136, -5124, 16250, false, 0, false, world.instanceId);
		addSpawn(1000033, -74857, 86666, -5124, 16250, false, 0, false, world.instanceId);
		// 2nd room
		// Fat in big room
		addSpawn(110096, -19008, 278660, -15041, 48750, false, 0, false, world.instanceId);
		// 3rd room Darion's like room
		addSpawn(110065, 16729, 238884, 9779, 0, false, 0, false, world.instanceId);
		// ------------------------9th Stage------------------------//
		// 1st Room (Schutgart Area)
		addSpawn(110058, 113118, -153955, -1488, 16250, false, 0, false, world.instanceId);
		addSpawn(110059, 111861, -154885, -1488, 32500, false, 0, false, world.instanceId);
		addSpawn(110058, 112270, -155274, -1532, 0, false, 0, false, world.instanceId);
		addSpawn(110059, 112340, -155663, -1488, 48750, false, 0, false, world.instanceId);
		addSpawn(110059, 113132, -156596, -1488, 48750, false, 0, false, world.instanceId);
		addSpawn(110060, 113875, -156854, -1532, 48750, false, 0, false, world.instanceId);
		addSpawn(110060, 114098, -155863, -1488, 0, false, 0, false, world.instanceId);
		addSpawn(110058, 115111, -155666, -1488, 0, false, 0, false, world.instanceId);
		addSpawn(110059, 114733, -154895, -1488, 0, false, 0, false, world.instanceId);
		addSpawn(110060, 114085, -155264, -1532, 32500, false, 0, false, world.instanceId);
		addSpawn(110060, 113925, -154522, -1488, 16250, false, 0, false, world.instanceId);
		// 2nd Room (DVC HARD)
		addSpawn(110029, 154444, 118461, -3808, 30000, false, 0, false, world.instanceId);
		addSpawn(110070, 153658, 118577, -3808, 16250, false, 0, false, world.instanceId);
		addSpawn(110070, 153808, 119379, -3808, 32500, false, 0, false, world.instanceId);
		addSpawn(110029, 153296, 119137, -3790, 30000, false, 0, false, world.instanceId);
		addSpawn(110070, 152791, 119502, -3808, 35000, false, 0, false, world.instanceId);
		addSpawn(110029, 152702, 120183, -3808, 48750, false, 0, false, world.instanceId);
		addSpawn(110070, 152153, 120769, -3808, 48750, false, 0, false, world.instanceId);
		addSpawn(110070, 151892, 121664, -3808, 50000, false, 0, false, world.instanceId);
		addSpawn(110029, 152855, 121720, -3808, 32500, false, 0, false, world.instanceId);
		addSpawn(110070, 153731, 121802, -3808, 32500, false, 0, false, world.instanceId);
		addSpawn(110029, 153694, 121283, -3808, 32500, false, 0, false, world.instanceId);
		addSpawn(110070, 154426, 121215, -3808, 32500, false, 0, false, world.instanceId);
		// 3rd Room (Gludin Harbor)
		addSpawn(110045, -90957, 148684, -3626, 16250, false, 0, false, world.instanceId);
		addSpawn(110046, -93229, 149110, -3626, 0, false, 0, false, world.instanceId);
		addSpawn(110045, -94453, 148398, -3626, 16250, false, 0, false, world.instanceId);
		addSpawn(110046, -94459, 149492, -3626, 48750, false, 0, false, world.instanceId);
		addSpawn(110046, -91728, 149934, -3626, 0, false, 0, false, world.instanceId);
		addSpawn(110046, -92413, 149465, -3626, 0, false, 0, false, world.instanceId);
		addSpawn(110045, -92666, 150511, -3626, 0, false, 0, false, world.instanceId);
		addSpawn(110046, -95288, 150033, -3626, 16250, false, 0, false, world.instanceId);
		addSpawn(110046, -95298, 151327, -3626, 48750, false, 0, false, world.instanceId);
		addSpawn(110046, -91020, 151444, -3626, 48750, false, 0, false, world.instanceId);
		addSpawn(110045, -92675, 151595, -3626, 0, false, 0, false, world.instanceId);
		addSpawn(110046, -93701, 152427, -3626, 48750, false, 0, false, world.instanceId);
		// ------------------------10th Stage------------------------//
		// 1st Room
		addSpawn(110050, 44595, 16608, -4393, 32500, false, 0, false, world.instanceId);
		addSpawn(110071, 43985, 15931, -4387, 16250, false, 0, false, world.instanceId);
		addSpawn(110049, 43408, 16511, -4393, 16250, false, 0, false, world.instanceId);
		addSpawn(110071, 42632, 16532, -4398, 0, false, 0, false, world.instanceId);
		addSpawn(110050, 43028, 17314, -4393, 0, false, 0, false, world.instanceId);
		addSpawn(110049, 43317, 18061, -4393, 48750, false, 0, false, world.instanceId);
		addSpawn(110050, 42663, 18352, -4372, (48750 + 48750 + 16250), false, 0, false, world.instanceId);
		addSpawn(110071, 43619, 18704, -4392, 48750, false, 0, false, world.instanceId);
		addSpawn(110050, 44594, 18437, -4398, 48750, false, 0, false, world.instanceId);
		addSpawn(110049, 44263, 18098, -4393, 48750, false, 0, false, world.instanceId);
		addSpawn(110071, 44752, 17635, -4398, 32500, false, 0, false, world.instanceId);
		addSpawn(110050, 45131, 17217, -4395, 32500, false, 0, false, world.instanceId);
		// 2nd Room (HARD ROOM)
		addSpawn(110064, -250698, 218203, -12335, 32500, false, 0, false, world.instanceId);
		addSpawn(110064, -252191, 218191, -12335, 0, false, 0, false, world.instanceId);
		addSpawn(110064, -251423, 217618, -12294, 16250, false, 0, false, world.instanceId);
		addSpawn(110064, -250126, 217520, -12294, 32500, false, 0, false, world.instanceId);
		addSpawn(110064, -252797, 217532, -12293, 0, false, 0, false, world.instanceId);
		addSpawn(110064, -251186, 217060, -12293, 16250, false, 0, false, world.instanceId);
		addSpawn(110064, -250694, 216901, -12273, 16250, false, 0, false, world.instanceId);
		addSpawn(110064, -252181, 216918, -12277, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -252098, 216186, -12251, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -252935, 216455, -12251, 0, false, 0, false, world.instanceId);
		addSpawn(110048, -252976, 216011, -12230, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -253090, 214992, -12095, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -252704, 215006, -12095, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -251429, 216714, -12252, 48750, false, 0, false, world.instanceId);
		addSpawn(110048, -251432, 216017, -12232, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -251431, 215296, -12176, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -249925, 216447, -12251, 32500, false, 0, false, world.instanceId);
		addSpawn(110048, -249895, 216016, -12232, 16250, false, 0, false, world.instanceId);
		addSpawn(110048, -250036, 215059, -12095, 16250, false, 0, false, world.instanceId);
		// 3rd Room
		addSpawn(110035, 92145, -121193, -4484, 0, false, 0, false, world.instanceId);
		addSpawn(110036, 93216, -120542, -4554, 48750, false, 0, false, world.instanceId);
		addSpawn(110035, 92980, -119630, -4542, 0, false, 0, false, world.instanceId);
		addSpawn(110036, 93715, -119264, -4473, 48750, false, 0, false, world.instanceId);
		addSpawn(110036, 95075, -119620, -4539, 48750, false, 0, false, world.instanceId);
		addSpawn(110035, 94071, -120281, -4537, 48750, false, 0, false, world.instanceId);
		addSpawn(110035, 93562, -121710, -4530, 48750, false, 0, false, world.instanceId);
		addSpawn(110036, 93763, -122782, -4544, 32500, false, 0, false, world.instanceId);
		addSpawn(110035, 94524, -122354, -4531, 32500, false, 0, false, world.instanceId);
		addSpawn(110036, 95514, -122565, -4557, 32500, false, 0, false, world.instanceId);
		addSpawn(110035, 96022, -121658, -4547, 32500, false, 0, false, world.instanceId);
		addSpawn(110036, 94995, -121129, -4540, 32500, false, 0, false, world.instanceId);
		// ------------------------11th Stage------------------------//
		// 1st Room (Big catacomb)
		addSpawn(110018, -114323, -181758, -6756, 32500, false, 0, false, world.instanceId);
		addSpawn(110018, -114307, -181233, -6756, 32500, false, 0, false, world.instanceId);
		addSpawn(110018, -114329, -180727, -6756, 32500, false, 0, false, world.instanceId);
		addSpawn(110019, -114779, -180630, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110019, -114390, -180087, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110018, -114316, -179439, -6757, 32500, false, 0, false, world.instanceId);
		addSpawn(110018, -114310, -178682, -6754, 32500, false, 0, false, world.instanceId);
		addSpawn(110018, -114318, -178168, -6757, 32500, false, 0, false, world.instanceId);
		addSpawn(110018, -115289, -178689, -6754, 0, false, 0, false, world.instanceId);
		addSpawn(110018, -119895, -178689, -6754, 0, false, 0, false, world.instanceId);
		addSpawn(110018, -115268, -179201, -6757, 0, false, 0, false, world.instanceId);
		addSpawn(110019, -114792, -179389, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110019, -115194, -180089, -6754, 48750, false, 0, false, world.instanceId);
		addSpawn(110018, -115278, -180750, -6756, 0, false, 0, false, world.instanceId);
		addSpawn(110018, -115274, -181254, -6756, 0, false, 0, false, world.instanceId);
		addSpawn(110018, -115268, -181763, -6756, 0, false, 0, false, world.instanceId);
		// 2nd Room (Rune temple)
		addSpawn(110076, 37052, -48023, -1153, 48750, false, 0, false, world.instanceId);
		addSpawn(110076, 37052, -48446, -1153, 48750, false, 0, false, world.instanceId);
		addSpawn(110051, 38368, -46912, -1126, 48750, false, 0, false, world.instanceId);
		addSpawn(110051, 37648, -46920, -1130, 48750, false, 0, false, world.instanceId);
		addSpawn(110051, 36832, -46916, -1143, 48750, false, 0, false, world.instanceId);
		addSpawn(110052, 37468, -47712, -1122, 48750, false, 0, false, world.instanceId);
		addSpawn(110052, 37240, -47712, -1122, 48750, false, 0, false, world.instanceId);
		addSpawn(110076, 37362, -48235, -1153, 0, false, 0, false, world.instanceId);
		addSpawn(110052, 37248, -48752, -1122, 16250, false, 0, false, world.instanceId);
		addSpawn(110052, 37456, -48752, -1122, 16250, false, 0, false, world.instanceId);
		addSpawn(110076, 35785, -48454, -1088, 0, false, 0, false, world.instanceId);
		addSpawn(110076, 35597, -47846, -1096, 0, false, 0, false, world.instanceId);
		addSpawn(110051, 36869, -49529, -1128, 16250, false, 0, false, world.instanceId);
		addSpawn(110051, 36256, -49536, -1099, 16250, false, 0, false, world.instanceId);
		addSpawn(110051, 37831, -49530, -1128, 16250, false, 0, false, world.instanceId);
		addSpawn(110051, 38398, -49548, -1129, 32500, false, 0, false, world.instanceId);
		// 3rd Room (3rd floor tower of infinitum) HARD ROOM
		addSpawn(1000025, -11868, 273287, -12074, 0, false, 0, false, world.instanceId);
		addSpawn(1000026, -11886, 274579, -12074, 15500, false, 0, false, world.instanceId);
		addSpawn(1000027, -13175, 274591, -12074, 30000, false, 0, false, world.instanceId);
		addSpawn(1000028, -13142, 273306, -12074, 48750, false, 0, false, world.instanceId);
		addSpawn(1000029, -12516, 273942, -11623, 32500, false, 0, false, world.instanceId);
		// ------------------------12th Stage------------------------//
		// final boss
		addSpawn(110092, 48443, -106874, -1585, 48750, false, 0, false, world.instanceId);
		// teleporters
		addSpawn(MOVE_TELEPORTER1, -46199, 245517, -9129, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER2, -51630, 245484, -9994, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER2, -109213, -181505, -6759, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER2, -87656, -81804, -8356, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER3, -48120, 243416, -9995, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER3, 147102, 152854, -12174, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER3, 29480, 11031, -4238, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER3, 16331, 208701, -9361, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER4, 53325, 245575, -6572, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER4, -12528, 272922, -9041, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER4, 17500, 244931, 9667, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER5, 143892, 144632, -8951, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER5, 57428, 78497, -3545, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER5, 90425, -7199, -3045, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER5, 183414, -118806, -3082, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER6, 149432, 152608, -12174, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER6, -5252, 55770, -3491, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER6, 12547, -137910, -1884, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER6, -21191, -174902, -9999, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER7, -149359, 256620, -60, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER7, 154169, 143121, -12742, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER7, 143047, -25676, -2046, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER8, -74230, 86507, -5129, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER8, -19022, 280109, -15050, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER8, 15836, 238380, 9770, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER9, 113512, -157408, -1540, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER9, 153977, 121938, -3813, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER9, -90530, 150146, -3631, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER10, 44990, 17921, -4390, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER10, -251434, 214787, -12092, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER10, 95472, -120451, -4435, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER11, -114796, -178095, -6759, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER11, 36294, -48237, -1100, 0, false, 0, false, world.instanceId);
		addSpawn(MOVE_TELEPORTER11, -10834, 273930, -11939, 0, false, 0, false, world.instanceId);
		addSpawn(EXIT_TELEPORTER, 46801, -106806, -1569, 0, false, 0, false, world.instanceId);
	}
}