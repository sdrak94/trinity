package net.sf.l2j.gameserver.instances.DarkCloudMansion;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class DarkCloudMansion extends Quest
{
private class DMCNpc
{
public L2Npc npc;
public boolean isDead = false;
public L2Npc golem = null;
public int status = 0;
public int order = 0;
public int count = 0;
}

private class DMCRoom
{
public FastList<DMCNpc> npcList = new FastList<DMCNpc>();
public int counter = 0;
public int reset = 0;
public int founded = 0;
public int[] Order;
}

private class DMCWorld extends InstanceWorld
{
public FastMap<String,DMCRoom> rooms = new FastMap<String,DMCRoom>();

public DMCWorld()
{
	InstanceManager.getInstance().super();
}
}

private static boolean debug = false;
private static boolean noRndWalk = true;

private static String qn = "DarkCloudMansion";
private static final int INSTANCEID = 9;

private class teleCoord {int instanceId; int x; int y; int z;}

//Items
private static int CC = 9690; //Contaminated Crystal

//NPCs
private static int YIYEN       = 32282;
private static int SOFaith     = 32288; //Symbol of Faith
private static int SOAdversity = 32289; //Symbol of Adversity
private static int SOAdventure = 32290; //Symbol of Adventure
private static int SOTruth     = 32291; //Symbol of Truth
private static int BSM         = 32324; //Black Stone Monolith
private static int SC          = 22402; //Shadow Column

//Mobs
private static int[] CCG = {18369,18370}; //Chromatic Crystal Golem
private static int[] BM  = {22272,22273,22274}; //Beleth's Minions
private static int[] HG  = {22264,22264}; //[22318,22319] #Hall Guards
private static int[] BS  = {18371,18372,18373,18374,18375,18376,18377}; //Beleth's Samples
private static int[] TOKILL = {18371,18372,18373,18374,18375,18376,18377,22318,22319,22272,22273,22274,18369,18370,22402,22264};

//Doors/Walls
private static int D1 = 24230001; //Starting Room
private static int D2 = 24230002; //First Room
private static int D3 = 24230005; //Second Room
private static int D4 = 24230003; //Third Room
private static int D5 = 24230004; //Forth Room
private static int D6 = 24230006; //Fifth Room
private static int W1 = 24230007; //Wall 1
/*private static int W2 = 24230008; //Wall 2
	private static int W3 = 24230009; //Wall 3
	private static int W4 = 24230010; //Wall 4
	private static int W5 = 24230011; //Wall 5
	private static int W6 = 24230012; //Wall 6
	private static int W7 = 24230013; //Wall 7*/

//Second room - random monolith order
private static int[][] MonolithOrder = new int[][]{
	{1,2,3,4,5,6},
	{6,5,4,3,2,1},
	{4,5,6,3,2,1},
	{2,6,3,5,1,4},
	{4,1,5,6,2,3},
	{3,5,1,6,2,4},
	{6,1,3,4,5,2},
	{5,6,1,2,4,3},
	{5,2,6,3,4,1},
	{1,5,2,6,3,4},
	{1,2,3,6,5,4},
	{6,4,3,1,5,2},
	{3,5,2,4,1,6},
	{3,2,4,5,1,6},
	{5,4,3,1,6,2},
};

//Second room - golem spawn locatons - random
private static int[][] GolemSpawn = new int[][]{
	{CCG[0],148060,181389},
	{CCG[1],147910,181173},
	{CCG[0],147810,181334},
	{CCG[1],147713,181179},
	{CCG[0],147569,181410},
	{CCG[1],147810,181517},
	{CCG[0],147805,181281}
};

//forth room - random shadow column
private static int[][] ColumnRows = new int[][]{
	{1,1,0,1,0},
	{0,1,1,0,1},
	{1,0,1,1,0},
	{0,1,0,1,1},
	{1,0,1,0,1}
};

//Fifth room - beleth order
private static int[][] Beleths = new int[][]{
	{1,0,1,0,1,0,0},
	{0,0,1,0,1,1,0},
	{0,0,0,1,0,1,1},
	{1,0,1,1,0,0,0},
	{1,1,0,0,0,1,0},
	{0,1,0,1,0,1,0},
	{0,0,0,1,1,1,0},
	{1,0,1,0,0,1,0},
	{0,1,1,0,0,0,1}
};

protected void openDoor(int doorId,int instanceId)
{
	for (L2DoorInstance door : InstanceManager.getInstance().getInstance(instanceId).getDoors())
		if (door.getDoorId() == doorId)
			door.openMe();
}

private boolean checkConditions(L2PcInstance player)
{
	if (debug)
		return true;
	else
	{
		L2Party party = player.getParty();
		if (party == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_IN_PARTY_CANT_ENTER));
			return false;
		}
		if (party.getLeader() != player)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_PARTY_LEADER_CAN_ENTER));
			return false;
		}
		if (party.getMemberCount() > 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.PARTY_EXCEEDED_THE_LIMIT_CANT_ENTER));
			return false;
		}
		for (L2PcInstance partyMember : party.getPartyMembers())
		{
			if (partyMember.getLevel() < 78)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_LEVEL_REQUIREMENT_NOT_SUFFICIENT);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
				return false;
			}
			if (!Util.checkIfInRange(1000, player, partyMember, true))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_LOCATION_THAT_CANNOT_BE_ENTERED);
				sm.addPcName(partyMember);
				player.sendPacket(sm);
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
		if (!(world instanceof DMCWorld))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_ENTERED_ANOTHER_INSTANCE_CANT_ENTER));
			return 0;
		}
		teleto.instanceId = world.instanceId;
		teleportplayer(player,teleto);
		return instanceId;
	}
	//New instance
	else
	{
		if (!checkConditions(player))
			return 0;
		L2Party party = player.getParty();
		instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		world = new DMCWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("DarkCloudMansion: started " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		runStartRoom((DMCWorld)world);
		// teleport players
		teleto.instanceId = instanceId;
		if (debug && party == null)
		{
			world.allowed.add(player.getObjectId());
			teleportplayer(player,teleto);
		}
		else
		{
			for (L2PcInstance partyMember : party.getPartyMembers())
			{
				if (partyMember.getQuestState(qn) == null)
					newQuestState(partyMember);
				world.allowed.add(partyMember.getObjectId());
				auditInstances(partyMember, template, instanceId);
				teleportplayer(partyMember,teleto);
			}
		}
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

protected void runStartRoom(DMCWorld world)
{
	world.status = 0;
	DMCRoom StartRoom = new DMCRoom();
	DMCNpc thisnpc;
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[0],146817,180335,-6117,0,false,0,false, world.instanceId);
	StartRoom.npcList.add(thisnpc);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[0],146741,180589,-6117,0,false,0,false, world.instanceId);
	StartRoom.npcList.add(thisnpc);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	world.rooms.put("StartRoom", StartRoom);
	if (debug)
		_log.info("DarkCloudMansion: first room spawned in instance " + world.instanceId);
}

protected void spawnHall(DMCWorld world)
{
	DMCRoom Hall = new DMCRoom();
	DMCNpc thisnpc;
	world.rooms.remove("Hall");		//remove room instance to avoid adding mob every time
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[1],147217,180112,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[2],147217,180209,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[1],148521,180112,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[0],148521,180209,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[1],148525,180910,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[2],148435,180910,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[1],147242,180910,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BM[2],147242,180819,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	Hall.npcList.add(thisnpc);
	
	world.rooms.put("Hall", Hall);
	if (debug)
		_log.info("DarkCloudMansion: hall spawned");
}

protected void runHall(DMCWorld world)
{
	spawnHall(world);
	world.status = 1;
	openDoor(D1, world.instanceId);
}

protected void runFirstRoom(DMCWorld world)
{
	DMCRoom FirstRoom = new DMCRoom();
	DMCNpc thisnpc;
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(HG[1],147842,179837,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	FirstRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(HG[0],147711,179708,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	FirstRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(HG[1],147842,179552,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	FirstRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(HG[0],147964,179708,-6117,0,false,0,false, world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	FirstRoom.npcList.add(thisnpc);
	
	world.rooms.put("FirstRoom", FirstRoom);
	world.status = 2;
	openDoor(D2, world.instanceId);
	if (debug)
		_log.info("DarkCloudMansion: spawned first room");
}

protected void runHall2(DMCWorld world)
{
	addSpawn(SOFaith,147818,179643,-6117,0,false,0,false,world.instanceId);
	spawnHall(world);
	world.status = 3;
}

protected void runSecondRoom(DMCWorld world)
{
	DMCRoom SecondRoom = new DMCRoom();
	DMCNpc thisnpc;
	
	// TODO: find a better way to initialize to [1,0,0,0,0,0,0]
	SecondRoom.Order = new int[7];
	SecondRoom.Order[0] = 1;
	for (int i=1;i<7;i++)
		SecondRoom.Order[i] = 0;
	
	int i = Rnd.get(MonolithOrder.length);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147800,181150,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][0];
	SecondRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147900,181215,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][1];
	SecondRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147900,181345,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][2];
	SecondRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147800,181410,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][3];
	SecondRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147700,181345,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][4];
	SecondRoom.npcList.add(thisnpc);
	
	thisnpc = new DMCNpc();
	thisnpc.npc = addSpawn(BSM,147700,181215,-6117,0,false,0,false, world.instanceId);
	thisnpc.order = MonolithOrder[i][5];
	SecondRoom.npcList.add(thisnpc);
	
	world.rooms.put("SecondRoom", SecondRoom);
	world.status = 4;
	openDoor(D3, world.instanceId);
	if (debug)
		_log.info("DarkCloudMansion: spawned second room");
}

protected void runHall3(DMCWorld world)
{
	addSpawn(SOAdversity,147808,181281,-6117,16383,false,0,false,world.instanceId);
	spawnHall(world);
	world.status = 5;
}

protected void runThirdRoom(DMCWorld world)
{
	DMCRoom ThirdRoom = new DMCRoom();
	DMCNpc thisnpc = new DMCNpc();
	thisnpc.isDead = false;
	thisnpc.npc = addSpawn(BM[1],148765,180450,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[2],148865,180190,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[1],148995,180190,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[0],149090,180450,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[1],148995,180705,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[2],148865,180705,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	world.rooms.put("ThirdRoom", ThirdRoom);
	world.status = 6;
	openDoor(D4, world.instanceId);
	if (debug)
		_log.info("DarkCloudMansion: spawned third room");
}

protected void runThirdRoom2(DMCWorld world)
{
	addSpawn(SOAdventure,148910,178397,-6117,16383,false,0,false,world.instanceId);
	DMCRoom ThirdRoom = new DMCRoom();
	DMCNpc thisnpc = new DMCNpc();
	thisnpc.isDead = false;
	thisnpc.npc = addSpawn(BM[1],148765,180450,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[2],148865,180190,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[1],148995,180190,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[0],149090,180450,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[1],148995,180705,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	thisnpc.npc = addSpawn(BM[2],148865,180705,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		thisnpc.npc.setIsNoRndWalk(true);
	ThirdRoom.npcList.add(thisnpc);
	world.rooms.put("ThirdRoom2", ThirdRoom);
	world.status = 8;
	if (debug)
		_log.info("DarkCloudMansion: spawned third room second time");
}

protected void runForthRoom(DMCWorld world)
{
	DMCRoom ForthRoom = new DMCRoom();
	ForthRoom.counter = 0;
	DMCNpc thisnpc;
	int temp[] = new int[7];
	int templist[][] = new int[7][5];
	int xx = 0;
	
	for (int i = 0; i < 7; i++)
		temp[i] = Rnd.get(ColumnRows.length );
	
	for (int i=0; i < 7; i++)
		templist[i] = ColumnRows[temp[i]];
	for (int x = 148660; x<149285; x += 125)
	{
		int yy = 0;
		for (int y = 179280; y > 178405; y -= 125)
		{
			thisnpc = new DMCNpc();
			thisnpc.npc = addSpawn(SC,x,y,-6115,16215,false,0,false,world.instanceId);
			thisnpc.status = templist[yy][xx];
			thisnpc.order = yy;
			ForthRoom.npcList.add(thisnpc);
			yy++;
		}
		xx++;
	}
	//TODO: unify this into previous loop
	for (DMCNpc npc : ForthRoom.npcList)
	{
		if (npc.status == 0)
			npc.npc.setIsInvul(true);
	}
	
	world.rooms.put("ForthRoom", ForthRoom);
	world.status = 7;
	openDoor(D5, world.instanceId);
	if (debug)
		_log.info("DarkCloudMansion: spawned forth room");
}

protected void runFifthRoom(DMCWorld world)
{
	spawnFifthRoom(world);
	world.status = 9;
	openDoor(D6,world.instanceId);
	if (debug)
		_log.info("DarkCloudMansion: spawned fifth room");
}

private void spawnFifthRoom(DMCWorld world)
{
	int idx = 0;
	int temp[] = new int[6];
	DMCRoom FifthRoom = new DMCRoom();
	DMCNpc thisnpc;
	
	temp = Beleths[ Rnd.get(Beleths.length)];
	
	FifthRoom.reset = 0;
	FifthRoom.founded = 0;
	
	for (int x = 148720; x < 149175; x += 65)
	{
		thisnpc = new DMCNpc();
		thisnpc.npc = addSpawn(BS[idx],x,182145,-6117,48810,false,0,false,world.instanceId);
		thisnpc.npc.setIsNoRndWalk(true);
		thisnpc.order = idx;
		thisnpc.status = temp[idx];
		thisnpc.count = 0;
		FifthRoom.npcList.add(thisnpc);
		if (temp[idx] == 1 && Rnd.get(100) < 95)
			thisnpc.npc.broadcastPacket(new NpcSay(thisnpc.npc.getObjectId(), 0, thisnpc.npc.getNpcId(), "You will never pass this test!"));
		else if (temp[idx] != 1 && Rnd.get(100) < 67)
			thisnpc.npc.broadcastPacket(new NpcSay(thisnpc.npc.getObjectId(), 0, thisnpc.npc.getNpcId(), "You will never pass this test!"));
		idx++ ;
	}
	world.rooms.put("FifthRoom", FifthRoom);
}

protected boolean checkKillProgress(L2Npc npc, DMCRoom room)
{
	boolean cont = true;
	for (DMCNpc npcobj : room.npcList)
	{
		if (npcobj.npc == npc)
			npcobj.isDead = true;
		if (npcobj.isDead == false)
		{
			cont = false;
		}
	}
	return cont;
}

protected void spawnRndGolem(DMCWorld world, DMCNpc npc)
{
	if (npc.golem != null)
		return;
	int i = Rnd.get(GolemSpawn.length);
	int mobId = GolemSpawn[i][0];
	int x = GolemSpawn[i][1];
	int y = GolemSpawn[i][2];
	
	npc.golem = addSpawn(mobId,x,y,-6117,0,false,0,false,world.instanceId);
	if (noRndWalk)
		npc.golem.setIsNoRndWalk(true);
}

protected void checkStone(L2Npc npc, int order[], DMCNpc npcObj, DMCWorld world)
{
	for (int i=1; i<7; i++)
	{
		//if there is a non zero value in the precedent step, the sequence is ok
		if (order[i] == 0 && order[i-1] != 0)
		{
			if (npcObj.order == i && npcObj.status == 0)
			{
				order[i] = 1;
				npcObj.status = 1;
				npcObj.isDead = true;
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 5441, 1, 1, 0));
				return;
			}
		}
	}
	spawnRndGolem(world, npcObj);
}

protected void endInstance(DMCWorld world)
{
	world.status = 10;
	addSpawn(SOTruth,148911,181940,-6117,16383,false,0,false,world.instanceId);
	world.rooms.clear();
	if (debug)
		_log.info("DarkCloudMansion: finished");
}

protected void checkBelethSample(DMCWorld world, L2Npc npc, L2PcInstance player)
{
	DMCRoom FifthRoom = world.rooms.get("FifthRoom");
	
	for (DMCNpc mob : FifthRoom.npcList)
	{
		if (mob.npc == npc)
		{
			if (mob.count == 0)
			{
				mob.count = 1;
				if (mob.status == 1)
				{
					mob.npc.broadcastPacket(new NpcSay(mob.npc.getObjectId(), 0, mob.npc.getNpcId(), "You have done well!"));
					FifthRoom.founded += 1;
					startQuestTimer("decayMe",4500, npc, player);
				}
				else
				{
					FifthRoom.reset = 1;
					mob.npc.broadcastPacket(new NpcSay(mob.npc.getObjectId(), 0, mob.npc.getNpcId(), "This is fake!"));
					startQuestTimer("decayBelethSamples",4500, npc, player);
				}
			}
			else
				return;
		}
	}
}

protected void killedBelethSample(DMCWorld world, L2Npc npc)
{
	int decayedSamples = 0;
	DMCRoom FifthRoom = world.rooms.get("FifthRoom");
	
	for (DMCNpc mob : FifthRoom.npcList)
	{
		if (mob.npc == npc)
		{
			decayedSamples += 1;
			mob.count = 2;
		}
		else
		{
			if (mob.count == 2)
				decayedSamples += 1;
		}
	}
	if (FifthRoom.reset == 1)
	{
		for (DMCNpc mob : FifthRoom.npcList)
		{
			if (mob.count == 0 || (mob.status == 1 && mob.count != 2))
			{
				decayedSamples += 1;
				mob.npc.decayMe();
				mob.count = 2;
			}
		}
		if (decayedSamples == 7)
			spawnFifthRoom(world);
	}
	else
	{
		if (FifthRoom.reset == 0 && FifthRoom.founded == 3)
		{
			for (DMCNpc mob : FifthRoom.npcList)
				mob.npc.decayMe();
			endInstance(world);
		}
	}
}

protected boolean allStonesDone(DMCWorld world)
{
	DMCRoom SecondRoom = world.rooms.get("SecondRoom");
	
	for (DMCNpc mob : SecondRoom.npcList)
	{
		if (mob.isDead)
			continue;
		else
			return false;
	}
	return true;
}

protected void removeMonoliths(DMCWorld world)
{
	DMCRoom SecondRoom = world.rooms.get("SecondRoom");
	
	for (DMCNpc mob : SecondRoom.npcList)
		mob.npc.decayMe();
}

protected void chkShadowColumn(DMCWorld world, L2Npc npc)
{
	DMCRoom ForthRoom = world.rooms.get("ForthRoom");
	
	for (DMCNpc mob : ForthRoom.npcList)
	{
		if (mob.npc == npc)
		{
			for (int i = 0;i <7; i++)
			{
				if (mob.order == i && ForthRoom.counter == i)
				{
					openDoor(W1+i, world.instanceId);
					ForthRoom.counter += 1;
					if (ForthRoom.counter == 7)
						runThirdRoom2(world);
				}
			}
		}
	}
}

public DarkCloudMansion(int questId, String name, String descr) {
	super(questId, name, descr);
	addFirstTalkId(BSM);
	addStartNpc(YIYEN);
	addTalkId(YIYEN);
	addTalkId(SOTruth);
	addAttackId(SC);
	for (int mob : BS)
		addAttackId(mob);
	for (int mob : CCG)
		addAttackId(mob);
	for (int mob : TOKILL )
		addKillId(mob);
}

public static void main(String[] args)
{
	// now call the constructor (starts up the)
	new DarkCloudMansion(-1,"DarkCloudMansion",qn);
}

@Override
public String onAdvEvent (String event, L2Npc npc, L2PcInstance player)
{
	if (npc == null)
		return "";
	InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
	DMCWorld world;
	if (tmpworld instanceof DMCWorld)
		world = (DMCWorld)tmpworld;
	else
		return "";
	if (event == "decayMe")
	{
		if (world.rooms.containsKey("FifthRoom"))
		{
			DMCRoom FifthRoom = world.rooms.get("FifthRoom");
			for (DMCNpc mob : FifthRoom.npcList)
			{
				if (mob.npc == npc || (FifthRoom.reset == 0 && FifthRoom.founded == 3))
				{
					mob.npc.decayMe();
					mob.count = 2;
				}
			}
			if (FifthRoom.reset == 0 && FifthRoom.founded == 3)
				endInstance(world);
		}
	}
	if (event == "decayBelethSamples")
	{
		if (world.rooms.containsKey("FifthRoom"))
		{
			DMCRoom FifthRoom = world.rooms.get("FifthRoom");
			for (DMCNpc mob : FifthRoom.npcList)
			{
				if (mob.count == 0)
				{
					mob.npc.decayMe();
					mob.count = 2;
				}
			}
		}
	}
	return "";
}

@Override
public String onTalk (L2Npc npc, L2PcInstance player)
{
	int npcId = npc.getNpcId();
	if (npcId == YIYEN)
	{
		teleCoord tele = new teleCoord();
		tele.x = 146534;
		tele.y = 180464;
		tele.z = -6117;
		enterInstance(player, "DarkCloudMansion.xml", tele);
	}
	else
	{
		InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		DMCWorld world;
		if (tmpworld instanceof DMCWorld)
			world = (DMCWorld)tmpworld;
		else
			return "";
		if (npcId == SOTruth)
		{
			teleCoord tele = new teleCoord();
			tele.x = 139968;
			tele.y = 150367;
			tele.z = -3111;
			if (world.allowed.contains(player.getObjectId()))
			{
				QuestState st = player.getQuestState(qn);
				if (st == null)
					st = newQuestState(player);
				st.giveItems(CC,1);
				if (debug)
					_log.info("DarkCloudMansion - id " + player.getObjectId() + " removed from allowed player in this Instances.");
				world.allowed.remove(world.allowed.indexOf(player.getObjectId()));
			}
			exitInstance(player,tele);
			int instanceId = npc.getInstanceId();
			Instance instance = InstanceManager.getInstance().getInstance(instanceId);
			if (instance.getPlayers().isEmpty())
			{
				InstanceManager.getInstance().destroyInstance(instanceId);
			}
			return "";
		}
	}
	return "";
}

@Override
public String onKill( L2Npc npc, L2PcInstance player, boolean isPet)
{
	InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
	DMCWorld world;
	if (tmpworld instanceof DMCWorld)
	{
		world = (DMCWorld)tmpworld;
		if (world.status==0)
			if (checkKillProgress(npc, world.rooms.get("StartRoom")))
				runHall(world);
		if (world.status==1)
			if (checkKillProgress(npc,world.rooms.get("Hall")))
				runFirstRoom(world);
		if (world.status==2)
			if (checkKillProgress(npc,world.rooms.get("FirstRoom")))
				runHall2(world);
		if (world.status==3)
			if (checkKillProgress(npc,world.rooms.get("Hall")))
				runSecondRoom(world);
		if (world.status==4)
		{
			DMCRoom SecondRoom = world.rooms.get("SecondRoom");
			for (DMCNpc mob : SecondRoom.npcList)
				if (mob.golem == npc)
					mob.golem = null;
		}
		if (world.status==5)
			if (checkKillProgress(npc,world.rooms.get("Hall")))
				runThirdRoom(world);
		if (world.status==6)
			if (checkKillProgress(npc,world.rooms.get("ThirdRoom")))
				runForthRoom(world);
		if (world.status==7)
			chkShadowColumn(world,npc);
		if (world.status==8)
			if (checkKillProgress(npc,world.rooms.get("ThirdRoom2")))
				runFifthRoom(world);
		if (world.status==9)
			killedBelethSample(world,npc);
	}
	return "";
}

@Override
public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet, L2Skill skill)
{
	InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
	DMCWorld world;
	if (tmpworld instanceof DMCWorld)
	{
		world = (DMCWorld)tmpworld;
		if (world.status == 7)
		{
			DMCRoom ForthRoom = world.rooms.get("ForthRoom");
			for (DMCNpc mob : ForthRoom.npcList)
			{
				if (mob.npc == npc)
				{
					if (mob.npc.isInvul() && Rnd.get(100) < 12)
					{
						if (debug)
							_log.info("DarkCloudMansion: spawn room 4 guard");
						addSpawn(BM[Rnd.get(BM.length)],player.getX(),player.getY(),player.getZ(),0,false,0,false,world.instanceId);
					}
				}
			}
		}
		if (world.status == 9)
			checkBelethSample(world,npc,player);
	}
	return "";
}

@Override
public String onFirstTalk (L2Npc npc, L2PcInstance player)
{
	InstanceWorld tmpworld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
	DMCWorld world;
	if (tmpworld instanceof DMCWorld)
	{
		world = (DMCWorld)tmpworld;
		if (world.status==4)
		{
			DMCRoom SecondRoom = world.rooms.get("SecondRoom");
			for (DMCNpc mob : SecondRoom.npcList)
			{
				if (mob.npc == npc)
				{
					checkStone(npc,SecondRoom.Order,mob,world);
				}
			}
			
			if (allStonesDone(world))
			{
				removeMonoliths(world);
				runHall3(world);
			}
		}
	}
	return "";
}
}