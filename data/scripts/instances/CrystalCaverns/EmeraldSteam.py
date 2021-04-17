# Script by Psychokiller1888
# Still missing:
# - Doors behavior in Emerald. They should close themselves as soon as the one who opened them moves
# - Contaminated crystal should not be taken at entrance, but exchange by the oracle in case the "boss" dies.
# - Meeting with Baylor is custom and not complete. Please understand that it's hard to get real info about CC
# - Mob stats and droplists, AI for the final mobs + z-axis corrections on them
# - Traps in Steam Corridor
# - Retail check for last room in Steam Corridor just before Kechi
# - Surely much more, but at least it's completly working! Enjoy retail like S80 crafting possibilities!
# @devs: Please edit this list if bugs found or points solved

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

qn = "EmeraldSteam"

debug = False

#Items
CRYSTAL      = 9690
BLUE_CRYSTAL = 9695
RED_CRYSTAL  = 9696

#NPCs
ORACLE_GUIDE  = 32281
ORACLE_GUIDE2 = 32278
ORACLE_GUIDE3 = 32280

#Mobs
GK1          = 22275
GK2          = 22277
KECHICAPTAIN = 22307
TOURMALINE   = 22292

KECHI     = 25532
DOLPH     = 22299
DARNEL    = 25531
TEROD     = 22301
WEYLIN    = 22298
GUARDIAN  = 22303
GUARDIAN2 = 22304

MOBLIST = [22306,22307,22416,22418,22419,22420,22293,22297,22281,22282,22287,22288,22289]

#Doors/Walls
DOOR1 = 24220021
DOOR2 = 24220024

#Oracle order
ordreOracle1 = [
                [32275,32274,32274,32274],
                [32274,32275,32274,32274],
                [32274,32274,32275,32274],
                [32274,32274,32274,32275]
               ]

ordreOracle2 = [
                [32276,32274,32274,32274],
                [32274,32276,32274,32274],
                [32274,32274,32276,32274],
                [32274,32274,32274,32276]
               ]

ordreOracle3 = [
                [32277,32274,32274,32274],
                [32274,32277,32274,32274],
                [32274,32274,32277,32274],
                [32274,32274,32274,32277]
               ]

class PyObject:
	pass

def openDoor(doorId,instanceId):
	for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
		if door.getDoorId() == doorId:
			door.openMe()

def dropItem(npc,itemId,count):
	ditem = ItemTable.getInstance().createItem("Loot", itemId, count, None)
	ditem.dropMe(npc, npc.getX(), npc.getY(), npc.getZ())

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

def teleportplayer(self,player,teleto,entry):
	if entry:
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
	# Check for existing instances of party members
	for partyMember in party.getPartyMembers().toArray():
		if partyMember.getInstanceId()!=0:
			instanceId = partyMember.getInstanceId()
			if debug: print "Emerald/Steam: found party member in instance:"+str(instanceId)
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
		teleportplayer(self,player,teleto,True)
		return instanceId
	# New instance
	else:
		instanceId = InstanceManager.getInstance().createDynamicInstance(template)
		if not self.worlds.has_key(instanceId):
			world = PyObject()
			world.instanceId = instanceId
			world.bosses = 5
			self.worlds[instanceId]=world
			self.world_ids.append(instanceId)
			print "Emerald/Steam Instance: " +str(instanceId) + " created by player: " + str(player.getName())
			# Close all doors
			for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
				door.closeMe()
			# Start the first room
			runEmeraldAndSteamFirstRoom(self,world)
		# Teleports player
		teleto.instanceId = instanceId
		teleportplayer(self,player,teleto,True)
		return instanceId
	return instanceId

def exitInstance(player,tele):
	player.setInstanceId(0)
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(0)
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)

def checkKillProgress(npc,room):
	cont = True
	if room.npclist.has_key(npc):
		room.npclist[npc] = True
	for npc in room.npclist.keys():
		if room.npclist[npc] == False:
			cont = False
	return cont

def runEmeraldAndSteamFirstRoom(self,world):
	world.status = 0
	world.keyKeepers = PyObject()
	world.keyKeepers.npclist = {} 
	newNpc = self.addSpawn(22275, 148206, 149486, -12140, 32308, False, 0, False, world.instanceId)
	world.keyKeepers.npclist[newNpc] = False 
	newNpc = self.addSpawn(22277, 148203, 151093, -12140, 31100, False, 0, False, world.instanceId)
	world.keyKeepers.npclist[newNpc] = False 
	self.addSpawn(22276, 147193, 149487, -12140, 32301, False, 0, False, world.instanceId)
	self.addSpawn(22278, 147182, 151091, -12140, 32470, False, 0, False, world.instanceId)
	self.addSpawn(22282, 144289, 150685, -12140, 49394, False, 0, False, world.instanceId)
	self.addSpawn(22282, 144335, 149846, -12140, 38440, False, 0, False, world.instanceId)
	self.addSpawn(22282, 144188, 149230, -12140, 13649, False, 0, False, world.instanceId)
	self.addSpawn(22282, 144442, 149234, -12140, 19083, False, 0, False, world.instanceId)
	self.addSpawn(22282, 145949, 149477, -12140, 32941, False, 0, False, world.instanceId)
	self.addSpawn(22282, 146792, 149545, -12140, 40543, False, 0, False, world.instanceId)
	self.addSpawn(22282, 145441, 151178, -12140, 36154, False, 0, False, world.instanceId)
	self.addSpawn(22282, 146735, 150981, -12140, 25702, False, 0, False, world.instanceId)
	self.addSpawn(22284, 144115, 151086, -12140, 51316, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145009, 149475, -12140, 31393, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146952, 151228, -12140, 38140, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145499, 149614, -12140, 38775, False, 0, False, world.instanceId)
	self.addSpawn(22284, 144308, 151420, -12140, 48469, False, 0, False, world.instanceId)
	self.addSpawn(22284, 144214, 149514, -12140, 15265, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145358, 150956, -12140, 26056, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145780, 151225, -12140, 39635, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146644, 151325, -12140, 42053, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146459, 150968, -12140, 11232, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145699, 149508, -12140, 34774, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145397, 149262, -12140, 16218, False, 0, False, world.instanceId)
	self.addSpawn(22284, 145750, 150944, -12140, 30099, False, 0, False, world.instanceId)
	self.addSpawn(22284, 144421, 151087, -12140, 21857, False, 0, False, world.instanceId)
	self.addSpawn(22282, 144154, 150261, -12140, 39283, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146359, 149355, -12140, 23301, False, 0, False, world.instanceId)
	self.addSpawn(22284, 147819, 150915, -12140, 25958, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146507, 149650, -12140, 50727, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146542, 149262, -12140, 18038, False, 0, False, world.instanceId)
	self.addSpawn(22284, 147918, 149636, -12140, 36636, False, 0, False, world.instanceId)
	self.addSpawn(22284, 147643, 149334, -12140, 29038, False, 0, False, world.instanceId)
	self.addSpawn(22284, 146491, 151144, -12140, 28915, False, 0, False, world.instanceId)
	self.addSpawn(22284, 147783, 151257, -12140, 37421, False, 0, False, world.instanceId)
	self.addSpawn(ORACLE_GUIDE3, 154333, 145200, -12589, 16483, False, 0, False, world.instanceId)

def runEmerald(self,world):
	world.status = 1
	runSecretRoom1(self,world)
	runSecretRoom2(self,world)
	runSecretRoom3(self,world)
	runSecretRoom4(self,world)
	world.emeraldRoom = PyObject()
	world.emeraldRoom.npclist = {}
	newNpc = self.addSpawn(22293, 144158, 143424, -11957, 29058, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 144044, 143448, -11949, 27778, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142580, 143091, -11872, 7458, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 144013, 142556, -11890, 26562, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 144138, 143833, -12003, 35900, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 143759, 143251, -11916, 24854, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142588, 144861, -12011, 47303, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142094, 144289, -11940, 38219, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142076, 143774, -11883, 48980, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142653, 143778, -11915, 9493, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 143308, 144206, -11992, 37435, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 143367, 145048, -12034, 16679, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 143597, 145175, -12033, 15198, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 142998, 143444, -11901, 12969, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 144089, 143956, -12014, 38107, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22282, 144394, 147711, -12141, 453, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22282, 145165, 147331, -12128, 29058, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 145103, 146978, -12069, 23007, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 144732, 147205, -12089, 18082, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 143859, 146571, -12036, 9955, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 142857, 145851, -12038, 19739, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 144917, 146979, -12057, 26485, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 144240, 146965, -12070, 1552, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 144238, 146428, -12034, 16770, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 143937, 146699, -12039, 32559, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 144711, 146645, -12036, 29130, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 144407, 146617, -12035, 7391, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22281, 144502, 146926, -12050, 12678, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 143816, 146656, -12039, 10414, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 143753, 146466, -12037, 7091, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 143608, 145754, -12036, 48284, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 143240, 145454, -12037, 39901, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 142606, 144827, -12009, 41533, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 142996, 144395, -11994, 64068, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142732, 145762, -12038, 54764, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 143312, 145772, -12039, 45440, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 144369, 142957, -11890, 29784, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 144954, 143832, -11976, 37294, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 145367, 143588, -11845, 30279, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 145099, 143959, -11942, 29249, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 145241, 143436, -11883, 26892, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 147631, 145941, -12236, 55236, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 148004, 146336, -12283, 44613, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 149430, 145844, -12336, 43268, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 149467, 145353, -12303, 19506, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 147850, 144090, -12227, 9285, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 147723, 143307, -12227, 49819, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 149033, 143103, -12229, 31151, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22281, 148920, 143400, -12238, 34526, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22282, 148653, 142813, -12231, 28363, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22282, 147485, 143590, -12227, 62369, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22281, 148426, 145886, -12296, 42769, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 148658, 144958, -12282, 49451, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 148648, 144098, -12240, 44077, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 149156, 143936, -12238, 42632, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 148388, 143092, -12227, 11429, False, 0, False, world.instanceId)
	world.emeraldRoom.npclist[newNpc]=False

def runSecretRoom1(self,world):
	world.SecretRoom1 = PyObject()
	world.SecretRoom1.npclist = {}
	newNpc = self.addSpawn(22288, 143114, 140027, -11888, 15025, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 142173, 140973, -11888, 55698, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 143210, 140577, -11888, 17164, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 142638, 140107, -11888, 6571, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 142547, 140938, -11888, 48556, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	newNpc = self.addSpawn(22298, 142690, 140479, -11887, 7663, False, 0, False, world.instanceId)
	world.SecretRoom1.npclist[newNpc]=False
	# Blacksmith
	self.addSpawn(32359, 142110, 139896, -11888, 8033, False, 0, False, world.instanceId)

def runSecretRoom2(self,world):
	world.SecretRoom2 = PyObject()
	world.SecretRoom2.npclist = {}
	newNpc = self.addSpawn(22303, 146272, 141484, -11888, 15025, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 146870, 140906, -11888, 23832, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22289, 146833, 141741, -11888, 37869, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 146591, 142040, -11888, 34969, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22288, 145744, 141146, -11888, 12266, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 146044, 142006, -11888, 38094, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False
	newNpc = self.addSpawn(22287, 146276, 140847, -11888, 22210, False, 0, False, world.instanceId)
	world.SecretRoom2.npclist[newNpc]=False

def runSecretRoom3(self,world):
	world.SecretRoom3 = PyObject()
	world.SecretRoom3.npclist = {}
	newNpc = self.addSpawn(22293, 144868, 143439, -12816, 5588, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 145369, 144040, -12816, 42939, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False
	newNpc = self.addSpawn(22294, 145315, 143436, -12813, 27523, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False
	newNpc = self.addSpawn(22293, 145043, 143854, -12815, 56775, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 145355, 143729, -12815, 63378, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False
	newNpc = self.addSpawn(22297, 145126, 143697, -12815, 33214, False, 0, False, world.instanceId)
	world.SecretRoom3.npclist[newNpc]=False

def runSecretRoom4(self,world):
	world.SecretRoom4 = PyObject()
	world.SecretRoom4.npclist = {}
	newNpc = self.addSpawn(22282, 150930, 141920, -12116, 21592, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22282, 150212, 141905, -12116, 7201, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22286, 150661, 141859, -12116, 15452, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22286, 150411, 141935, -12116, 13445, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22280, 150280, 142241, -12116, 9672, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22280, 150738, 142110, -12115, 14903, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False
	newNpc = self.addSpawn(22304, 150564, 142231, -12115, 4836, False, 0, False, world.instanceId)
	world.SecretRoom4.npclist[newNpc]=False

def runDarnel(self,world):
	world.status = 7
	world.DarnelRoom = PyObject()
	world.DarnelRoom.npclist = {}
	newNpc = self.addSpawn(DARNEL, 152759, 145949, -12588, 21592, False, 0, False, world.instanceId)
	world.DarnelRoom.npclist[newNpc]=False
	openDoor(24220005,world.instanceId)
	openDoor(24220006,world.instanceId)

def runSteamRoom1(self,world):
	world.status = 20
	world.killedCaptains = 0
	world.steamRoom1 = PyObject()
	world.steamRoom1.npclist = {}
	newNpc = self.addSpawn(22306, 148755, 152573, -12170, 65497, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22416, 146862, 152734, -12169, 42584, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22416, 146014, 152607, -12172, 23694, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22416, 145346, 152585, -12172, 31490, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 146972, 152421, -12172, 28476, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 145714, 152821, -12172, 58705, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 145336, 152805, -12172, 39590, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 146530, 152762, -12172, 60307, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 145941, 152412, -12172, 14182, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 146243, 152807, -12172, 38832, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 145152, 152410, -12172, 21338, False, 0, False, world.instanceId)
	world.steamRoom1.npclist[newNpc] = False

def runSteamRoom1Oracle(self,world):
	world.OracleTriggered = False
	o1,o2,o3,o4 = ordreOracle1[Rnd.get(len(ordreOracle1))]
	self.addSpawn(o1, 147090, 152505, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o2, 147090, 152575, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o3, 147090, 152645, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o4, 147090, 152715, -12169, 31613, False, 0, False, world.instanceId)

def runSteamRoom2(self,world):
	world.status = 21
	world.killedCaptains = 0
	world.steamRoom2 = PyObject()
	world.steamRoom2.npclist = {}
	newNpc = self.addSpawn(22420, 148815, 152804, -12172, 44197, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 149414, 152478, -12172, 25651, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 148482, 152388, -12173, 32189, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 147908, 152861, -12172, 61173, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 147835, 152484, -12172, 7781, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 148176, 152627, -12173, 3336, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 148813, 152453, -12172, 50373, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 149233, 152773, -12172, 36765, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22306, 149550, 152718, -12172, 37301, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22306, 148881, 152601, -12172, 24054, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False
	newNpc = self.addSpawn(22306, 148183, 152486, -12172, 5289, False, 0, False, world.instanceId)
	world.steamRoom2.npclist[newNpc] = False

def runSteamRoom2Oracle(self,world):
	world.OracleTriggered = False
	o1,o2,o3,o4 = ordreOracle2[Rnd.get(len(ordreOracle2))]
	self.addSpawn(o1, 149783, 152505, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o2, 149783, 152575, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o3, 149783, 152645, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o4, 149783, 152715, -12169, 31613, False, 0, False, world.instanceId)

def runSteamRoom3(self,world):
	world.status = 22
	world.killedCaptains = 0
	world.steamRoom3 = PyObject()
	world.steamRoom3.npclist = {}
	newNpc = self.addSpawn(22419, 150751, 152430, -12172, 29190, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 150613, 152778, -12172, 19574, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 151242, 152832, -12172, 40116, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 151473, 152656, -12172, 28951, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 151090, 152401, -12172, 1909, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 151625, 152372, -12172, 31372, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 152283, 152577, -12172, 15323, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 151906, 152699, -12172, 49605, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 151134, 152626, -12172, 59956, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 152105, 152766, -12172, 59956, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 150416, 152567, -12173, 53744, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22307, 150689, 152618, -12172, 34932, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22307, 151329, 152558, -12172, 55102, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False
	newNpc = self.addSpawn(22307, 152054, 152557, -12172, 40959, False, 0, False, world.instanceId)
	world.steamRoom3.npclist[newNpc] = False

def runSteamRoom3Oracle(self,world):
	world.OracleTriggered = False
	o1,o2,o3,o4 = ordreOracle3[Rnd.get(len(ordreOracle3))]
	self.addSpawn(o1, 152461, 152505, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o2, 152461, 152575, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o3, 152461, 152645, -12169, 31613, False, 0, False, world.instanceId)
	self.addSpawn(o4, 152461, 152715, -12169, 31613, False, 0, False, world.instanceId)

def runSteamRoom4(self,world):
	world.status = 23
	world.killedCaptains = 0
	world.steamRoom4 = PyObject()
	world.steamRoom4.npclist = {}
	newNpc = self.addSpawn(22307, 150454, 149976, -12173, 28435, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22307, 151186, 150140, -12173, 37604, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22307, 151718, 149805, -12172, 26672, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 150755, 149852, -12173, 31074, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22418, 150457, 150173, -12172, 34736, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 151649, 150194, -12172, 35198, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 151254, 149876, -12172, 26433, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22420, 151819, 150010, -12172, 33680, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 150852, 150030, -12173, 32002, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False
	newNpc = self.addSpawn(22419, 150031, 149797, -12172, 16560, False, 0, False, world.instanceId)
	world.steamRoom4.npclist[newNpc] = False

def runKechi(self,world):
	world.status = 24
	world.kechiRoom = PyObject()
	world.kechiRoom.npclist = {}
	newNpc = self.addSpawn(22309, 154409, 149680, -12151, 8790, False, 0, False, world.instanceId)
	world.kechiRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(22310, 154165, 149734, -12159, 4087, False, 0, False, world.instanceId)
	world.kechiRoom.npclist[newNpc]=False
	newNpc = self.addSpawn(KECHI, 154069, 149525, -12158, 51165, False, 0, False, world.instanceId)
	world.kechiRoom.npclist[newNpc]=False

class EmeraldSteam(JQuest):
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.worlds = {}
		self.world_ids = []

	def onTalk (self,npc,player):
		instanceId = 0
		npcId = npc.getNpcId()
		if npcId == ORACLE_GUIDE:
			tele = PyObject()
			tele.x = 140486
			tele.y = 148895
			tele.z = -11817
			instanceId = enterInstance(self, player, "EmeraldSteam.xml", tele)
			openDoor(DOOR1,instanceId)
			openDoor(DOOR2,instanceId)
		if instanceId == 0:
			return
		return

	def onFirstTalk(self,npc,player):
		world = self.worlds[npc.getInstanceId()]
		npcId = npc.getNpcId()
		teleto = PyObject()
		doTeleport = False
		if npcId == 32274:
			self.addSpawn(22416, npc.getX() - 60, npc.getY(), npc.getZ(), 32116, False, 0, False, world.instanceId)
		elif npcId == 32275:
			if world.OracleTriggered:
				runSteamRoom2(self,world)
				teleto.x = 147529
				teleto.y = 152587
				teleto.z = -12169
				doTeleport = True
			else:
				self.addSpawn(22416, npc.getX() - 60, npc.getY(), npc.getZ(), 32116, False, 0, False, world.instanceId)
		elif npcId == 32276:
			if world.OracleTriggered:
				runSteamRoom3(self,world)
				teleto.x = 150194
				teleto.y = 152610
				teleto.z = -12169
				doTeleport = True
			else:
				self.addSpawn(22416, npc.getX() - 60, npc.getY(), npc.getZ(), 32116, False, 0, False, world.instanceId)
		elif npcId == 32277:
			if world.OracleTriggered:
				runSteamRoom4(self,world)
				teleto.x = 149743
				teleto.y = 149986
				teleto.z = -12141
				doTeleport = True
			else:
				self.addSpawn(22416, npc.getX() - 60, npc.getY(), npc.getZ(), 32116, False, 0, False, world.instanceId)
		if doTeleport:
			party = player.getParty()
			if party:
				for partyMember in party.getPartyMembers().toArray():
					teleportplayer(self,partyMember,teleto,False)
			else:
				teleportplayer(self,player,teleto,False)

	def onSkillSee(self,npc,caster,skill,targets,isPet):
		if not npc in targets: return
		world = self.worlds[npc.getInstanceId()]
		skillId = skill.getId()
		npcId = npc.getNpcId()
		if npcId in [32275,32276,32277] and skillId in [1217,1218,1011,1015,1401,5146]:
			world.OracleTriggered = True

	def onAttack(self,npc,player,damage,isPet,skill):
		return

	def onKill(self,npc,player,isPet):
		npcId = npc.getNpcId()
		if self.worlds.has_key(npc.getInstanceId()):
			world = self.worlds[npc.getInstanceId()]
			if world.status == 0:
				if npcId == GK1:
					for mob in world.keyKeepers.npclist: 
						mob.decayMe()
					dropItem(npc,9698,1)
					runEmerald(self,world)
				elif npcId == GK2:
					for mob in world.keyKeepers.npclist: 
						mob.decayMe()
					dropItem(npc,9699,1)
					runSteamRoom1(self,world)
			elif world.status == 1:
				if checkKillProgress(npc,world.emeraldRoom):
					world.status = 2
					self.addSpawn(TOURMALINE, 147937, 145886, -12256, 0, False, 0, False, world.instanceId)
			elif world.status == 2:
				if npcId == TOURMALINE:
					world.status = 3
					self.addSpawn(TEROD, 147191, 146855, -12266, 0, False, 0, False, world.instanceId)
			elif world.status == 3:
				if npcId == TEROD:
					world.status = 4
					self.addSpawn(TOURMALINE, 144840, 143792, -11991, 0, False, 0, False, world.instanceId)
			elif world.status == 4:
				if npcId == TOURMALINE:
					world.status = 5
					self.addSpawn(DOLPH, 142067,145364, -12036, 0, False, 0, False, world.instanceId)
			elif world.status == 5:
				if npcId == DOLPH:
					world.status = 6
			elif world.status == 20:
				if npcId == 22416:
					world.killedCaptains += 1
				if world.killedCaptains == 3:
					for mob in world.steamRoom1.npclist:
						mob.decayMe()
					runSteamRoom1Oracle(self,world)
				elif checkKillProgress(npc,world.steamRoom1):
					runSteamRoom1Oracle(self,world)
			elif world.status == 21:
				if npcId == 22306:
					world.killedCaptains += 1
				if world.killedCaptains == 3:
					for mob in world.steamRoom2.npclist:
						mob.decayMe()
					runSteamRoom2Oracle(self,world)
				elif checkKillProgress(npc,world.steamRoom2):
					runSteamRoom2Oracle(self,world)
			elif world.status == 22:
				if npcId == 22307:
					world.killedCaptains += 1
				if world.killedCaptains == 3:
					for mob in world.steamRoom3.npclist:
						mob.decayMe()
					runSteamRoom3Oracle(self,world)
				elif checkKillProgress(npc,world.steamRoom3):
					runSteamRoom3Oracle(self,world)
			elif world.status == 23:
				if npcId == 22307:
					world.killedCaptains += 1
				if world.killedCaptains == 3:
					for mob in world.steamRoom4.npclist:
						mob.decayMe()
					self.addSpawn(ORACLE_GUIDE2, 152243, 150152, -12141, 0, False, 0, False, world.instanceId)
					runKechi(self,world)
				elif checkKillProgress(npc,world.steamRoom4):
					self.addSpawn(ORACLE_GUIDE2, 152243, 150152, -12141, 0, False, 0, False, world.instanceId)
					runKechi(self,world)
			if world.status >= 1 and world.status <= 6:
				if npcId in [DOLPH,TEROD,WEYLIN,GUARDIAN,GUARDIAN2]:
					world.bosses = world.bosses - 1
					if world.bosses == 0:
						runDarnel(self,world)
			if world.status == 7 or world.status == 24:
				if npcId == DARNEL:
					dropItem(npc,BLUE_CRYSTAL,1)
				elif npcId == KECHI:
					dropItem(npc,RED_CRYSTAL,1)
		return

QUEST = EmeraldSteam(-1, qn, "EmeraldSteam")
QUEST.addStartNpc(ORACLE_GUIDE)
QUEST.addTalkId(ORACLE_GUIDE)
QUEST.addKillId(GK1)
QUEST.addKillId(GK2)
QUEST.addKillId(TEROD)
QUEST.addKillId(WEYLIN)
QUEST.addKillId(DOLPH)
QUEST.addKillId(DARNEL)
QUEST.addKillId(KECHI)
QUEST.addKillId(GUARDIAN)
QUEST.addKillId(GUARDIAN2)
QUEST.addKillId(TOURMALINE)
QUEST.addKillId(KECHICAPTAIN)
for npc in [32274,32275,32276,32277]:
	QUEST.addFirstTalkId(npc)
for npc in [32275,32276,32277]:
	QUEST.addSkillSeeId(npc)
for mob in MOBLIST:
	QUEST.addKillId(mob)