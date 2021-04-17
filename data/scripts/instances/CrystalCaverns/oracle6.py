# Script by Psychokiller1888

import sys
from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.instancemanager    import InstanceManager
from net.sf.l2j.gameserver.model.entity       import Instance

ORACLE_GUIDE = 32278
DOOR1        = 24220061
DOOR2        = 24220023

class PyObject:
	pass

def openDoor(doorId,instanceId):
	for door in InstanceManager.getInstance().getInstance(instanceId).getDoors():
		if door.getDoorId() == doorId:
			door.openMe()

class oracle6(JQuest):
	def __init__(self,id,name,descr):
		self.isSpawned = False
		JQuest.__init__(self,id,name,descr)

	def onFirstTalk (self,npc,player):
		npcId = npc.getNpcId()
		if npcId == ORACLE_GUIDE:
			instanceId = npc.getInstanceId()
			openDoor(DOOR1,instanceId)
			openDoor(DOOR2,instanceId)

QUEST = oracle6(-1, "oracle6", "ai")
QUEST.addStartNpc(ORACLE_GUIDE)
QUEST.addFirstTalkId(ORACLE_GUIDE)