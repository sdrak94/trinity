#Kamaloka Script by Psychokiller1888

import sys
from java.lang                                     import System
from net.sf.l2j.gameserver.instancemanager        import InstanceManager
from net.sf.l2j.gameserver.model.entity           import Instance
from net.sf.l2j.gameserver.model.actor            import L2Character
from net.sf.l2j.gameserver.model.actor            import L2Summon
from net.sf.l2j.gameserver.model                  import L2World
from net.sf.l2j.gameserver.model.quest            import State
from net.sf.l2j.gameserver.model.quest            import QuestState
from net.sf.l2j.gameserver.model.quest.jython     import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets  import SystemMessage

qn = "Kamaloka"

#NPC
BATHIS    = 30332 #Gludio
LUCAS     = 30071 #Dion
GOSTA     = 30916 #Heine
MOUEN     = 30196 #Oren
VISHOTSKY = 31981 #Schuttgart
MATHIAS   = 31340 #Rune

#Teleporters
TPGLUDIO     = 50022
TPDION       = 50022
TPHEINE      = 50023
TPOREN       = 50024
TPSCHUTTGART = 50025
TPRUNE       = 50026

MAX_DISTANCE = 500

GUARDS = [BATHIS,LUCAS,GOSTA,MOUEN,VISHOTSKY,MATHIAS]
BOSSES = [18554,18555,18558,18559,18562,18564,18566,18568,18571,18573,18577]
#Kamaloka Levels   23          26          33          36          43          46          53          56          63          66          73

debug = True

class PyObject:
	pass

def saveEntry(member) :
	currentTime = System.currentTimeMillis()/1000
	st = member.getQuestState(qn)
	if not st :
		st = self.newQuestState(member)
	st.set("LastEntry",str(currentTime))
	return

def getBackTeleport(npcId) :
	if npcId == 18554 or npcId == 18555:
		tpBack = [-13870,123767,-3117]
	elif npcId == 18558 or npcId == 18559:
		tpBack = [18149,146024,-3100]
	elif npcId == 18562 or npcId == 18564:
		tpBack = [108449,221607,-3598]
	elif npcId == 18566 or npcId == 18568:
		tpBack = [80985,56373,-1560]
	elif npcId == 18571 or npcId == 18573:
		tpBack = [85945,-142176,-1341]
	elif npcId == 18577:
		tpBack = [42673,-47988,-797]
	return tpBack

def checkDistance(player) :
	isTooFar = False
	party = player.getParty()
	if party:
		for partyMember in party.getPartyMembers().toArray():
			if abs(partyMember.getX() - player.getX()) > MAX_DISTANCE :
				isTooFar = True
				break;
			if abs(partyMember.getY() - player.getY()) > MAX_DISTANCE :
				isTooFar = True
				break;
			if abs(partyMember.getZ() - player.getZ()) > MAX_DISTANCE :
				isTooFar = True
				break;
	return isTooFar

def checkCondition(player,reuse,minLevel,maxLevel):
	currentTime = System.currentTimeMillis()/1000
	party = player.getParty()
	if not party:
		player.sendPacket(SystemMessage.sendString("You must be in a party with at least one other person."))
		return False
	#check size of the party, max 6 for entering Kamaloka
	if party and party.getMemberCount() > 6:
		player.sendPacket(SystemMessage.sendString("Instance for max 6 players in party."))
		return False
	for partyMember in party.getPartyMembers().toArray():
		if partyMember.getLevel() < minLevel or partyMember.getLevel() > maxLevel:
			player.sendPacket(SystemMessage.sendString("You and your party mates must be between level " + str(minLevel) + " and level " + str(maxLevel) + " to enter this Kamaloka."))
			partyMember.sendPacket(SystemMessage.sendString("You must be between level " + str(minLevel) + " and level " + str(maxLevel) + " to enter this Kamaloka."))
			return False
		st = partyMember.getQuestState(qn)
		if st:
			LastEntry = st.getInt("LastEntry")
			if currentTime < LastEntry + reuse:
				player.sendPacket(SystemMessage.sendString("One of your party member still has to wait for re-access Kamaloka"))
				partyMember.sendPacket(SystemMessage.sendString("You have to wait at least 24 hours between each time you enter Kamaloka"))
				return False
	return True

def teleportplayer(self,player,teleto):
	player.setInstanceId(teleto.instanceId)
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(teleto.instanceId)
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)
	return

def enterInstance(self,player,teleto,KamaInfo):
	instanceId = 0
	template = KamaInfo[0]
	reuse = KamaInfo[1]
	minLevel = KamaInfo[3]
	maxLevel = KamaInfo[4]
	if checkDistance(player):
		player.sendPacket(SystemMessage.sendString("Please regroup your party before joining Kamaloka."))
		return 0
	if not checkCondition(player,reuse,minLevel,maxLevel):
		return 0
	party = player.getParty()
	# Check for exising instances of party members
	for partyMember in party.getPartyMembers().toArray():
		if partyMember.getInstanceId() != 0:
			instanceId = partyMember.getInstanceId()
	# New instance
	instanceId = InstanceManager.getInstance().createDynamicInstance(template)
	if not self.worlds.has_key(instanceId):
		world = PyObject()
		world.instanceId = instanceId
		self.worlds[instanceId]=world
		self.world_ids.append(instanceId)
		print "Kamaloka: started " + template + " Instance: " +str(instanceId) + " created by player: " + str(player.getName())
	# Teleport players
	teleto.instanceId = instanceId
	for partyMember in party.getPartyMembers().toArray():
		partyMember.stopAllEffects()
		partyMember.clearSouls()
		partyMember.clearCharges()
		teleportplayer(self,partyMember,teleto)
	return instanceId

def exitInstance(player,tele):
	if player.getInstanceId > 0:
		player.setInstanceId(0)
	player.teleToLocation(tele.x, tele.y, tele.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(0)
		pet.teleToLocation(tele.x, tele.y, tele.z)

class Kamaloka(JQuest):
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.worlds = {}
		self.world_ids = []
		
	def onAdvEvent (self,event,npc,player):
		st = player.getQuestState(qn)
		if not st:
			st = self.newQuestState(player)
		htmltext = event
		if event == "finishKamaloka" :
			npcId = npc.getNpcId()
			tpBack = getBackTeleport(npcId)
			tele = PyObject()
			tele.x = tpBack[0]
			tele.y = tpBack[1]
			tele.z = tpBack[2]
			instanceId = player.getInstanceId()
			if instanceId > 0:
				playerList = InstanceManager.getInstance().getInstance(instanceId).getPlayers()
				for member in playerList.toArray():
					member = L2World.getInstance().findPlayer(member)
					saveEntry(member)
					exitInstance(member,tele)
			return
		elif event == "lvl23" :
			#KamaInfo = [FILE, Reuse Delay, Boss, LvlMin, LvlMax, X, Y, Z
			KamaInfo = ["Kamaloka-23.xml",86400,18554,18,28,-57109,-219871,-8117]
		elif event == "lvl26" :
			KamaInfo = ["Kamaloka-26.xml",86400,18555,21,31,-55556,-206144,-8117]
		elif event == "lvl33" :
			KamaInfo = ["Kamaloka-33.xml",86400,18558,28,38,-55492,-206143,-8117]
		elif event == "lvl36" :
			KamaInfo = ["Kamaloka-36.xml",86400,18559,31,41,-41257,-213143,-8117]
		elif event == "lvl43" :
			KamaInfo = ["Kamaloka-43.xml",86400,18562,38,48,-49802,-206141,-8117]
		elif event == "lvl46" :
			KamaInfo = ["Kamaloka-46.xml",86400,18564,41,51,-41184,-213144,-8117]
		elif event == "lvl53" :
			KamaInfo = ["Kamaloka-53.xml",86400,18566,48,58,-41201,-219859,-8117]
		elif event == "lvl56" :
			KamaInfo = ["Kamaloka-56.xml",86400,18568,51,61,-57102,-206143,-8117]
		elif event == "lvl63" :
			KamaInfo = ["Kamaloka-63.xml",86400,18571,58,68,-57116,-219857,-8117]
		elif event == "lvl66" :
			KamaInfo = ["Kamaloka-66.xml",86400,18573,61,71,-41228,-219860,-8117]
		elif event == "lvl73" :
			KamaInfo = ["Kamaloka-73.xml",86400,18577,68,78,-55823,-212935,-8071]
		tele = PyObject()
		tele.x = KamaInfo[5]
		tele.y = KamaInfo[6]
		tele.z = KamaInfo[7]
		instanceId = enterInstance(self,player,tele,KamaInfo)
		if not instanceId:
			return
		if instanceId == 0:
			return
		return
		
	def onTalk (self,npc,player):
		st = player.getQuestState(qn)
		npcId = npc.getNpcId()
		if not st:
			st.setState(State.STARTED)
		if npcId == BATHIS:
			htmltext = "start-bathis.htm"
		if npcId == LUCAS:
			htmltext = "start-lucas.htm"
		if npcId == GOSTA:
			htmltext = "start-gosta.htm"
		if npcId == MOUEN:
			htmltext = "start-mouen.htm"
		if npcId == VISHOTSKY:
			htmltext = "start-vishotsky.htm"
		if npcId == MATHIAS:
			htmltext = "start-mathias.htm"
		return htmltext

	def onKill(self,npc,player,isPet):
		npcId = npc.getNpcId()
		playerList = InstanceManager.getInstance().getInstance(player.getInstanceId()).getPlayers()
		for member in playerList.toArray():
			member = L2World.getInstance().findPlayer(member)
			saveEntry(member)
			member.sendPacket(SystemMessage.sendString("You will be moved out of Kamaloka in 5 minutes"))
		self.startQuestTimer("finishKamaloka",300000,npc,player)
		return

QUEST = Kamaloka(-1, qn, "instances")

for npc in GUARDS :
	QUEST.addStartNpc(npc)
	QUEST.addTalkId(npc)

for bosses in BOSSES :
	QUEST.addKillId(bosses)