package instances.TOI;

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

public class TOI extends Quest
{
	
	
//Instance Configuration
private static String qn = "TOI";
private static final int INSTANCEID = 2003;
private static boolean debug = false;
private static int levelReq = 90;
private static int pvpReq = 1250;
private static int fameReq = 350;	

//NPCs
private static int MALEFICENT = 80000;
private static int TELEPORTER = 80001;
private static int TELEPORTER2 = 80002;
private static int BAAL = 80003;

//TOI 1 MOBS
private static final int[] MOBS   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 2 MOBS
private static final int[] MOBS2   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 3 MOBS
private static final int[] MOBS3   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 4 MOBS
private static final int[] MOBS4   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 5 MOBS
private static final int[] MOBS5   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 6 MOBS
private static final int[] MOBS6   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 7 MOBS
private static final int[] MOBS7   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 8 MOBS
private static final int[] MOBS8   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 9 MOBS
private static final int[] MOBS9   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 10 MOBS
private static final int[] MOBS10   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 11 MOBS
private static final int[] MOBS11   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//TOI 12 MOBS
private static final int[] MOBS12   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//Raid Bosses
private static final int[] BOSSES = {22503,18554,22493,18564,25597};
private static final int AZZAZEL = 80004;

//Baium
private static final int[] GRAND_BOSSES = {95657,95658};

private class teleCoord {int instanceId; int x; int y; int z;}

public class TOIWorld extends InstanceWorld
{
private int stage = 0;
private int liveMobs = 0;

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
		_log.warning("Tower of Insolence declive mobs went into negatives ");
	}
}

public int getLiveMobs()
{
	return liveMobs;
}

public TOIWorld()
{
	InstanceManager.getInstance().super();
}
}

public TOI(int questId, String name, String descr)
{
	super(questId, name, descr);
	
	addStartNpc(MALEFICENT);
	addTalkId(MALEFICENT);
	addTalkId(TELEPORTER);
	addTalkId(TELEPORTER2);
	
	for (int boss : BOSSES)
		addKillId(boss);
	
	for (int mob : MOBS)
		addKillId(mob);
	
	for (int mob : MOBS2)
		addKillId(mob);
	
	for (int mob : MOBS3)
		addKillId(mob);
	
	for (int mob : MOBS4)
		addKillId(mob);
	
	for (int mob : MOBS5)
		addKillId(mob);
	
	for (int mob : MOBS6)
		addKillId(mob);
	
	for (int mob : MOBS7)
		addKillId(mob);
	
	for (int mob : MOBS8)
		addKillId(mob);
	
	for (int mob : MOBS9)
		addKillId(mob);
	
	for (int mob : MOBS10)
		addKillId(mob);
	
	for (int mob : MOBS11)
		addKillId(mob);
	
	for (int mob : MOBS12)
		addKillId(mob);
	
	addKillId(AZZAZEL);
	
	for (int mob : GRAND_BOSSES)
		addKillId(mob);
}

public static void main(String[] args)
{
	new TOI(-1, qn, "instances");
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
			if (party.getMemberCount() > 9)
			{
				player.sendMessage("This is a 9 player instance; you cannot enter with a party size > 9 people");
				return false;
			}
			
			if (party.getMemberCount() < 9)
			{
				player.sendMessage("This is a 9 player instance; you cannot enter with a party size < 9 people");
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
				if (ptm == null) return false;
				
				if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
				{
					ptm.sendMessage("You can only enter this instance once a week, wait until the next week");
					canEnter = false;
				}
				else if (ptm.getLevel() < levelReq)
				{
					ptm.sendMessage("You must be level "+levelReq+" to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < pvpReq)
				{
					ptm.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
					canEnter = false;
				}
				else if (ptm.getFame() < fameReq)
				{
					ptm.sendMessage("You must have "+fameReq+" fame to enter this instance");
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
					ptm.sendMessage("You're too far away from your party leader");
					player.sendMessage("One of your party members is too far away");
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
						player.sendMessage(ptm.getName()+" is preventing you from entering the instance");
					return false;
				}
			}
		}
		else
		{
			if (!single)
			{
				player.sendMessage("This is a 9 player instance; you cannot enter with a party size < 9 people");
				return false;
			}
			
			if (!single && party == null && System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(player.getAccountName(), INSTANCEID))
			{
				player.sendMessage("You can only enter this instance once a week, wait until the next week");
				return false;
			}
			else if (player.getLevel() < levelReq)
			{
				player.sendMessage("You must be level "+levelReq+" to enter this instance");
				return false;
			}
			else if (player.getPvpKills() < pvpReq)
			{
				player.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
				return false;
			}
			else if (player.getFame() < fameReq)
			{
				player.sendMessage("You must have "+fameReq+" fame to enter this instance");
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
	//check for existing instances for this player
	InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
	//existing instance
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
		teleportplayer(player,teleto);
		return instanceId;
	}
	else  //New instance
	{
		if (!checkConditions(player, false))
			return 0;
		
		instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		world = new TOIWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("Tower of Insolence: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		
		final L2Party party = player.getParty();
		
		if (party != null)
		{
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				if (ptm == null) continue;
				
				InstanceManager.getInstance().setInstanceTime(ptm.getAccountName(), INSTANCEID, getNextInstanceTime(ONEDAY));
				
				// teleport players
				teleto.instanceId = instanceId;
				
				world.allowed.add(ptm.getObjectId());
				auditInstances(ptm, template, instanceId);
				teleportplayer(ptm,teleto);
			}
		}
		else
		{
			InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(ONEDAY));
			
			// teleport players
			teleto.instanceId = instanceId;
			
			world.allowed.add(player.getObjectId());
			auditInstances(player, template, instanceId);
			teleportplayer(player,teleto);
		}
		
		spawn1stMobs((TOIWorld) world, player);
		
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
	
	if (npcId == MALEFICENT)
	{
		//Change Here Instance spawn location.
		teleCoord teleto = new teleCoord();
		teleto.x = -76435;
		teleto.y = -185543;
		teleto.z = -11008;
		enterInstance(player, "TOI.xml", teleto);
	}
	else if (npcId == TELEPORTER)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof TOIWorld))
			return null;
		
		final L2Party party = player.getParty();
		
		final TOIWorld toiworld = (TOIWorld)world;
		
		if (toiworld.getStage() == 1)
		{
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null) continue;
					ptm.teleToLocation(-76435, -185543, -11003, false);
				}
			}
			else
			{
				player.teleToLocation(-76435, -185543, -11003, false);
			}
			npc.deleteMe();
			spawn1stMobs(toiworld, player);
		}
		else if (toiworld.getStage() == 2)
		{
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null) continue;
					ptm.teleToLocation(-55580, -219857, -8117, false);
					ptm.sendPacket(new ExShowScreenMessage("The Boss of Tower of Insolence has Appeared!", 6000));
				}
			}
			else
			{
				player.teleToLocation(-55580, -219857, -8117, false);
				player.sendPacket(new ExShowScreenMessage("The Boss of Tower of Insolence has Appeared!", 6000));
			}
			/*npc.deleteMe();*/
		}
		else if (toiworld.getStage() == 3)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			
			if (player.getParty() == null)
			{
				exitInstance(player, teleto);
				player.sendPacket(new ExShowScreenMessage("You have completed the Tower of Insolence instance", 6000));
			}
			else
			{
				for (L2PcInstance ptm : player.getParty().getPartyMembers())
				{
					exitInstance(ptm, teleto);
					ptm.sendPacket(new ExShowScreenMessage("You have completed the Tower of Insolence instance", 6000));
				}
			}
			
			st.exitQuest(true);
		}
		else
		{
			_log.warning("Tower of Insolence stage is fucked up!");
		}
	}
	else if (npcId == TELEPORTER2)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof TOIWorld))
			return null;
		
		final TOIWorld toiworld = (TOIWorld)world;
		
		if (toiworld.getStage() == 3)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			
			if (player.getParty() == null)
			{
				exitInstance(player, teleto);
				player.sendPacket(new ExShowScreenMessage("You have completed the Tower of Insolence instance", 6000));
			}
			else
			{
				for (L2PcInstance ptm : player.getParty().getPartyMembers())
				{
					exitInstance(ptm, teleto);
					ptm.sendPacket(new ExShowScreenMessage("You have completed the Tower of Insolence instance", 6000));
				}
			}
		}
		else
		{
			_log.warning("Tower of Insolence stage is fucked up!");
		}
		
		st.exitQuest(true);
	}
	
	return null;
}

@Override
public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
	
	if (world == null || !(world instanceof TOIWorld))
		return null;
	
	final TOIWorld toiworld = (TOIWorld)world;
	
	toiworld.decLiveMobs();
	
	if (toiworld.getLiveMobs() <= 0)
	{
		toiworld.liveMobs = 0;
		
		for (int id : GRAND_BOSSES)
		{
			if (id == npc.getNpcId())
			{
				toiworld.incStage();
				addSpawn(TELEPORTER2, -55580, -219857, -8117, 0, false, 0, false, world.instanceId);
				return null;
			}
		}
		
		final int stage = toiworld.getStage();
		
		switch (stage)
		{
		case 0:
			spawn1stMobs(toiworld, killer);
			break;
		case 1:
			spawn2ndMobs(toiworld, killer);
			break;
		case 2:
			spawn3rdMobs(toiworld, killer);
			break;
		case 3:
			spawn4rdMobs(toiworld, killer);
			break;
		case 4:
			spawn5thMobs(toiworld, killer);
			break;
		case 5:
			spawn6thMobs(toiworld, killer);
			break;
		case 6:
			spawn7thMobs(toiworld, killer);
			break;
		case 7:
			spawn8thMobs(toiworld, killer);
			break;
		case 8:
			spawn9thMobs(toiworld, killer);
			break;
		case 9:
			spawn10thMobs(toiworld, killer);
			break;
		case 10:
			spawn11thMobs(toiworld, killer);
			break;
		case 11:
			spawn12thMobs(toiworld, killer);
			break;
		case 12:
			spawnSubBoss(toiworld, killer);
			break;
		case 13:
			spawnGrandBoss(toiworld, killer);
			spawnGK(toiworld, killer);
			spawnBAAL(toiworld, killer);
			break;
		}
	}
	
	return null;
}

public void spawnGK(TOIWorld world, L2PcInstance player)
{
	addSpawn(TELEPORTER, -86602, -185545, -10059, 0, false, 0, false, world.instanceId);
}

public void spawnBAAL(TOIWorld world, L2PcInstance player)
{
	addSpawn(BAAL, -76435, -185543, -11003, 0, false, 0, false, world.instanceId);
}

public void spawn1stMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		
		world.incStage();
	}
}

public void spawn2ndMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS2[Rnd.get(MOBS2.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn3rdMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS3[Rnd.get(MOBS3.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn4rdMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS4[Rnd.get(MOBS4.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn5thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS5[Rnd.get(MOBS5.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn6thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS6[Rnd.get(MOBS6.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn7thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS7[Rnd.get(MOBS7.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}
public void spawn8thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS8[Rnd.get(MOBS8.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn9thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS9[Rnd.get(MOBS9.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		
		world.incStage();
	}
}

public void spawn10thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS10[Rnd.get(MOBS10.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn11thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS11[Rnd.get(MOBS11.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn12thMobs(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS12[Rnd.get(MOBS12.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawnSubBoss(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() == 1)
	{
		addSpawn(BOSSES[Rnd.get(BOSSES.length)], -86217, -185543, -10042, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		if (Rnd.get(100) < 98)
		{
			addSpawn(BOSSES[Rnd.get(BOSSES.length)], -86217, -185543, -10042, 0, true, 0, false, world.instanceId);
			world.incLiveMobs();
			
			if (Rnd.get(100)> 85)
				addSpawn(BOSSES[Rnd.get(BOSSES.length)], -86217, -185543, -10042, 0, true, 0, false, world.instanceId);		
			world.incLiveMobs();
		}
		else
		{
			addSpawn(AZZAZEL, -86217, -185543, -10042, 0, true, 0, false, world.instanceId);
			world.incLiveMobs();
		}
		
		world.incStage();
	}
}

public void spawnGrandBoss(TOIWorld world, L2PcInstance player)
{
	if (world.getStage() >= 2)
	{
		addSpawn(GRAND_BOSSES[Rnd.get(GRAND_BOSSES.length)], -56284, -219858, -8120, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		_log.warning("Tower of Insolence spawning grand boss w/o stage being >= 2");
	}
}
}