# By Psychokiller1888

from net.sf.l2j.gameserver.instancemanager        import InstanceManager
from net.sf.l2j.gameserver.model.actor            import L2Summon
from net.sf.l2j.gameserver.model.entity           import Instance
from net.sf.l2j.gameserver.model.quest            import State
from net.sf.l2j.gameserver.model.quest            import QuestState
from net.sf.l2j.gameserver.model.quest.jython     import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets  import CreatureSay
from net.sf.l2j.gameserver.network.serverpackets  import MagicSkillUse
from net.sf.l2j.gameserver.network.serverpackets  import SystemMessage
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.datatables             import ItemTable

qn = "CoralGarden"

#Items
CRYSTAL       = 9690
CLEAR_CRYSTAL = 9697

#NPCs
ORACLE_GUIDE  = 32281
TEARS         = 25534

#Doors/Walls
CORALGARDENGATEWAY    = 24220025 #Starting Room
CORALGARDENSECRETGATE = 24220026 #Tears Door

debug = False

class PyObject:
	pass

def openDoor(doorId,instanceId):
	for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
		if door.getDoorId() == doorId:
			door.openMe()

def dropItem(npc,itemId,count):
	ditem = ItemTable.getInstance().createItem("Loot", itemId, count, None)
	ditem.dropMe(npc, npc.getX(), npc.getY(), npc.getZ());

def checkKillProgress(npc,room):
	cont = True
	if room.npclist.has_key(npc):
		room.npclist[npc] = True
	for npc in room.npclist.keys():
		if room.npclist[npc] == False:
			cont = False
	if debug: print str(room.npclist)
	return cont

def checkCondition(player):
	if not player.getLevel() >= 78:
		player.sendPacket(SystemMessage.sendString("You must be level 78 to enter Crystal Caverns."))
		return False
	party = player.getParty()
	if not party:
		player.sendPacket(SystemMessage.sendString("You must be in a party with at least one other person."))
		return False
	item = player.getInventory().getItemByItemId(CRYSTAL)
	if not item:
		player.sendPacket(SystemMessage.sendString("You must have a Contaminated Crystal in your Inventory."))
		return False
	return True

def teleportplayer(self,player,teleto):
	player.destroyItemByItemId("Quest", CRYSTAL, 1, player, True)
	player.setInstanceId(teleto.instanceId)
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(teleto.instanceId)
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)
	return

def enterInstance(self,player,template,teleto):
	instanceId = 0
	if not checkCondition(player):
		return 0
	party = player.getParty()
	# Check for exising instances of party members
	for partyMember in party.getPartyMembers().toArray():
		if partyMember.getInstanceId()!=0:
			instanceId = partyMember.getInstanceId()
			if debug: print "Coral Garden: found party member in instance:"+str(instanceId)
	# Existing instance
	if instanceId != 0:
		foundworld = False
		for worldid in self.world_ids:
			if worldid == instanceId:
				foundworld = True
		if not foundworld:
			player.sendPacket(SystemMessage.sendString("Your Party Members are in another Instance."))
			return 0
		teleto.instanceId = instanceId
		teleportplayer(self,player,teleto)
		return instanceId
	# New instance
	else:
		instanceId = InstanceManager.getInstance().createDynamicInstance(template)
		if not self.worlds.has_key(instanceId):
			world = PyObject()
			world.instanceId = instanceId
			self.worlds[instanceId]=world
			self.world_ids.append(instanceId)
			print template + "Coral Garden Instance: " +str(instanceId) + " created by player: " + str(player.getName())
			# Close all doors
			for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
				door.closeMe()
			# Start the first room
			runHall(self,world)
		# Teleport player
		teleto.instanceId = instanceId
		teleportplayer(self,player,teleto)
		return instanceId
	return instanceId

def exitInstance(player,tele):
	player.setInstanceId(0)
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(0)
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)

def runHall(self,world):
	world.status=0
	world.startRoom = PyObject()
	world.startRoom.npclist = {}
	newNpc = self.addSpawn(22314, 141740, 150330, -11817, 6633, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 141233, 149960, -11817, 49187, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 141866, 150723, -11817, 13147, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 142276, 151105, -11817, 7823, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 142102, 151640, -11817, 20226, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 142093, 152269, -11817, 3445, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 141569, 152994, -11817, 22617, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 141083, 153210, -11817, 28405, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 140469, 152415, -11817, 41700, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 140180, 151635, -11817, 45729, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 140490, 151126, -11817, 54857, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 140930, 150269, -11817, 17591, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 141203, 150210, -11817, 64400, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 141360, 150357, -11817, 9093, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 142255, 151694, -11817, 14655, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 141920, 151124, -11817, 8191, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 141911, 152734, -11817, 21600, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 141032, 152929, -11817, 32791, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 140317, 151837, -11817, 43864, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 140183, 151939, -11817, 25981, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22316, 140944, 152724, -11817, 12529, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22316, 141301, 154428, -11817, 17207, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22316, 142499, 154437, -11817, 65478, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 142664, 154612, -11817, 8498, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 142711, 154137, -11817, 28756, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 142705, 154378, -11817, 26017, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 141605, 154490, -11817, 31128, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 141115, 154674, -11817, 28781, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 141053, 154431, -11817, 46546, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 141423, 154130, -11817, 60888, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 142249, 154395, -11817, 64346, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 141530, 152803, -11817, 53953, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 142020, 152272, -11817, 55995, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 142134, 151667, -11817, 52687, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 141958, 151021, -11817, 42965, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 140979, 150233, -11817, 38924, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 140509, 150983, -11817, 23466, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 140151, 151410, -11817, 23661, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22317, 140446, 152370, -11817, 13192, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 140249, 152133, -11817, 41391, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 140664, 152655, -11817, 8720, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 141610, 152988, -11817, 57460, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22314, 141189, 154197, -11817, 16792, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 142315, 154368, -11817, 30260, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22315, 142577, 154774, -11817, 45981, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22313, 141338, 153089, -11817, 26387, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22316, 140800, 150707, -11817, 55884, False,0,False, world.instanceId)
	world.startRoom.npclist[newNpc]=False
	if debug: print "Coral: hall spawned in instance " + str(world.instanceId)

def runGolems(self,world):
	world.status = 1
	newNpc = self.addSpawn(TEARS,144298,154420,-11854,63371,False,0,False, world.instanceId) # Tears
	newNpc = self.addSpawn(32328,140547,151670,-11813,32767,False,0,False, world.instanceId)
	newNpc = self.addSpawn(32328,141941,151684,-11813,63371,False,0,False, world.instanceId)

class CoralGarden(JQuest):
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.worlds = {}
		self.world_ids = []

	def onTalk (self,npc,player):
		npcId = npc.getNpcId()
		instanceId=0
		if npcId == ORACLE_GUIDE:
			tele = PyObject()
			tele.x = 140486
			tele.y = 148895
			tele.z = -11817
			instanceId = enterInstance(self, player, "coral.xml", tele)
		if instanceId == 0:
			return
		try:
			for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
				if door.getDoorId() == CORALGARDENGATEWAY:
					door.openMe()
		except:
			pass
		return

	def onKill(self,npc,player,isPet):
		if self.worlds.has_key(npc.getInstanceId()):
			world = self.worlds[npc.getInstanceId()]
			if world.status == 0:
				if checkKillProgress(npc,world.startRoom):
					runGolems(self,world)
			elif world.status == 1:
				npcId = npc.getNpcId()
				if npcId == TEARS:
					dropItem(npc,CLEAR_CRYSTAL,1)
		return

QUEST = CoralGarden(-1, qn, "CoralGarden")
QUEST.addStartNpc(ORACLE_GUIDE)
QUEST.addTalkId(ORACLE_GUIDE)
QUEST.addKillId(TEARS)
for mob in [22313,22317,22314,22315,22316]:
  QUEST.addKillId(mob)