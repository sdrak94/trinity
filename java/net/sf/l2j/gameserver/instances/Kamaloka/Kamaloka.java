package net.sf.l2j.gameserver.instances.Kamaloka;

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
import net.sf.l2j.util.Rnd;

public class Kamaloka extends Quest
{
//NPCs
private static int SABRIEL = 90006;
private static int TELEPORTER = 90007;
private static int TELEPORTER2 = 90012;
private static int SQUASH = 90008;

//BOSSES
private static final int[] BOSSES = {22503,18554,22493,18564,25597};
private static final int FARIS = 95616;

//final bosses
private static final int[] GRAND_BOSSES = {95657,95658};

//MOBS
private static final int[] MOBS   = {22485,22490,22491,22497,22488,18558,18559,22494,22499,22500,22502};

//stronger MOBS
private static final int[] MOBS_STRONGER   = {18562,18555,22487,25617,25621,22505,25616};

private static String qn = "Kamaloka";
private static final int INSTANCEID = 2000;

private static boolean debug = false;
private static int levelReq = Config.KAMALOKA_LEVELS;
private static int pvpReq = Config.KAMALOKA_PVPS; //Previously 50
private static int healerPvpReq = Config.KAMALOKA_SUPPORT_PVPS; //Custom
/*
	//coords
	private static int[] INITIAL_SPAWN_POINT = {-76435, -185543, -11003};
	private static int[] BOSS_ROOM_SPAWN_POINT = {-55580, -219857, -8117};*/

private class teleCoord {int instanceId; int x; int y; int z;}

public class KamalokaWorld extends InstanceWorld
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
		_log.warning("WTF KAMALOKA declivemobs went into negatives ");
	}
}

public int getLiveMobs()
{
	return liveMobs;
}

public KamalokaWorld()
{
	InstanceManager.getInstance().super();
}
}

public Kamaloka(int questId, String name, String descr)
{
	super(questId, name, descr);
	
	addStartNpc(SABRIEL);
	addTalkId(SABRIEL);
	addTalkId(TELEPORTER);
	addTalkId(TELEPORTER2);
	
	for (int boss : BOSSES)
		addKillId(boss);
	
	for (int mob : MOBS)
		addKillId(mob);
	
	for (int mob : MOBS_STRONGER)
		addKillId(mob);
	
	addKillId(FARIS);
	
	for (int mob : GRAND_BOSSES)
		addKillId(mob);
}

public static void main(String[] args)
{
	new Kamaloka(-1, qn, "instances");
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
				if (ptm == null) return false;
				
				if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
				{
					ptm.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
					canEnter = false;
				}
				else if (ptm.getLevel() < levelReq)
				{
					ptm.sendMessage("You must be level "+levelReq+" to enter this instance");
					canEnter = false;
				}			
			    else if (!ptm.isHealerClass() && ptm.getPvpKills() < pvpReq)
				{
					ptm.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
					canEnter = false;
				}
				else if (ptm.isHealerClass() && ptm.getPvpKills() < healerPvpReq)
				{
					ptm.sendMessage("Support classes must have "+healerPvpReq+" PvPs to enter this instance");
					canEnter = false;
				}/*
				else if (ptm.getGearLevel() < gearLvlReq)
				{
					ptm.sendMessage("Your gear level should be equal or more than "+gearLvlReq+" in order to enter this instance");
					canEnter = false;
				}*/
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
						player.sendMessage(ptm.getName()+" is preventing you from entering the instance");
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
			
			/*if (!single && party == null && System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(player.getAccountName(), INSTANCEID))
			{
				player.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
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
			}*/
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
		world = new KamalokaWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("Kamaloka: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		
		final L2Party party = player.getParty();
		
		if (party != null)
		{
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				QuestState st = player.getQuestState(qn);
				
				if (st == null)
					st = newQuestState(player);
				
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
		
		spawn1stMobs((KamalokaWorld) world, player);
		
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

	
	
	if (npcId == SABRIEL)
	{
		teleCoord teleto = new teleCoord();
		teleto.x = -76435;
		teleto.y = -185543;
		teleto.z = -11008;
		enterInstance(player, "Kamaloka.xml", teleto);
	}
	else if (npcId == TELEPORTER)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof KamalokaWorld))
			return null;
		
		final L2Party party = player.getParty();
		
		final KamalokaWorld kamWorld = (KamalokaWorld)world;
		
		if (kamWorld.getStage() == 4)
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
			spawn1stMobs(kamWorld, player);
		}
		else if (kamWorld.getStage() == 9)
		{
			if (party != null)
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					if (ptm == null) continue;
					ptm.teleToLocation(-55580, -219857, -8117, false);
					ptm.sendPacket(new ExShowScreenMessage("The Boss of Kamaloka has Appeared!", 6000));
				}
			}
			else
			{
				player.teleToLocation(-55580, -219857, -8117, false);
				player.sendPacket(new ExShowScreenMessage("The Boss of Kamaloka has Appeared!", 6000));
			}
		}
		else if (kamWorld.getStage() == 10)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			
			if (player.getParty() == null)
			{
				QuestState st = player.getQuestState(qn);
				if(st != null)
				{
					st.exitQuest(true);
				}
				else 
				{
					_log.warning("KAMALOKA: Player's: "+ player.getName() +"'s quest state is null");
				}
				exitInstance(player, teleto);
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				if (instance.getPlayers().isEmpty())
				{
					InstanceManager.getInstance().destroyInstance(instanceId);
				}
				player.sendPacket(new ExShowScreenMessage("You have completed the Kamaloka instance!", 6000));
			}
			else
			{
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					QuestState st = player.getQuestState(qn);
					if(st != null)
					{
						st.exitQuest(true);
					}
					else 
					{
						_log.warning("KAMALOKA: Player's: "+ ptm.getName() +"'s quest state is null");
					}
					exitInstance(ptm, teleto);
					ptm.sendPacket(new ExShowScreenMessage("You have completed the Kamaloka instance!", 6000));
				}
				
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				if (instance.getPlayers().isEmpty())
				{
					InstanceManager.getInstance().destroyInstance(instanceId);
				}
			}
			
		}
		else
		{
			_log.warning("LOL wtf kamworld stage is fucked up!");
		}
	}
	else if (npcId == TELEPORTER2)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof KamalokaWorld))
			return null;
		
		final KamalokaWorld kamWorld = (KamalokaWorld)world;

		if (kamWorld.getStage() == 10)
		{
			teleCoord teleto = new teleCoord();
			teleto.x = -82993;
			teleto.y = 150860;
			teleto.z = -3129;
			
			if (player.getParty() == null)
			{
				exitInstance(player, teleto);
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				if (instance.getPlayers().isEmpty())
				{
					InstanceManager.getInstance().destroyInstance(instanceId);
				}
				player.sendPacket(new ExShowScreenMessage("You have completed the Kamaloka instance!", 6000));
			}
			else
			{
				final L2Party party = player.getParty();
				for (L2PcInstance ptm : party.getPartyMembers())
				{
					exitInstance(ptm, teleto);
					ptm.sendPacket(new ExShowScreenMessage("You have completed the Kamaloka instance!", 6000));
				}
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				//instance.getPlayers().clear();
				if (instance.getPlayers().isEmpty())
				{
					InstanceManager.getInstance().destroyInstance(instanceId);
				}
			}
		}
		else
		{
			_log.warning("LOL wtf kamworld stage is fucked up!");
		}
	}
	
	return null;
}

@Override
public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
	
	if (world == null || !(world instanceof KamalokaWorld))
		return null;
	
	final KamalokaWorld kamWorld = (KamalokaWorld)world;
	
	kamWorld.decLiveMobs();
	
	if (kamWorld.getLiveMobs() <= 0)
	{
		kamWorld.liveMobs = 0;
		
		for (int id : GRAND_BOSSES)
		{
			if (id == npc.getNpcId())
			{
				kamWorld.incStage();
				addSpawn(TELEPORTER2, -55580, -219857, -8117, 0, false, 0, false, world.instanceId);
				spawnSquash(kamWorld, killer);
				return null;
			}
		}				
		
		final int stage = kamWorld.getStage();
		
		switch (stage)
		{
		case 0: //shouldn't happen
			spawn1stMobs(kamWorld, killer);
			break;
		case 4:
			spawnGK(kamWorld, killer);
			break;
		case 1:
		case 5:
			spawn2ndMobs(kamWorld, killer);
			break;
		case 2:
		case 6:
			spawn3rdMobs(kamWorld, killer);
			break;
		case 3:
		case 7:
			spawnSubBoss(kamWorld, killer);
			break;
		case 8:
			spawnGrandBoss(kamWorld, killer);
			spawnGK(kamWorld, killer);
			break;
		}
	}
	
	return null;
}

public void spawnGK(KamalokaWorld world, L2PcInstance player)
{
	addSpawn(TELEPORTER, -86602, -185545, -10059, 0, false, 0, false, world.instanceId);
}

public void spawnSquash(KamalokaWorld world, L2PcInstance player)
{
	addSpawn(SQUASH, -56345, -219854, -8116, 0, false, 0, false, world.instanceId);
}

public void spawn1stMobs(KamalokaWorld world, L2PcInstance player)
{
	if (world.getStage() == 0)
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -77776, -185543, -11014, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn2ndMobs(KamalokaWorld world, L2PcInstance player)
{
	if (world.getStage() == 1)
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -80108, -185543, -10749, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawn3rdMobs(KamalokaWorld world, L2PcInstance player)
{
	if (world.getStage() == 2)
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS[Rnd.get(MOBS.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -82438, -185543, -10486, 0, true, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
}

public void spawnSubBoss(KamalokaWorld world, L2PcInstance player)
{
	if (world.getStage() == 3)
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
			else
				addSpawn(MOBS_STRONGER[Rnd.get(MOBS_STRONGER.length)], -86217, -185543, -10042, 0, true, 0, false, world.instanceId);
			
			world.incLiveMobs();
		}
		else
		{
			addSpawn(FARIS, -86217, -185543, -10042, 0, true, 0, false, world.instanceId);
			world.incLiveMobs();
		}
		
		world.incStage();
	}
}

public void spawnGrandBoss(KamalokaWorld world, L2PcInstance player)
{
	if (world.getStage() >= 8)
	{
		addSpawn(GRAND_BOSSES[Rnd.get(GRAND_BOSSES.length)], -56284, -219858, -8120, 0, false, 0, false, world.instanceId);
		world.incLiveMobs();
		world.incStage();
	}
	else
	{
		_log.warning("lol wtf kamaloka spawning grand boss w/o stage being >= 8");
	}
}
}