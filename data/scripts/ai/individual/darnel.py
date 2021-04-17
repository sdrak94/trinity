# By Psychokiller1888
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.ai import CtrlIntention

DARNEL = 25531

class PyObject:
	pass

class Quest (JQuest) :
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.npcobject = {}

	def onKill(self,npc,player,isPet):
		npcId = npc.getNpcId()
		if npcId == DARNEL:
			self.addSpawn(32279,152761,145950,-12588,0,False,0,False,player.getInstanceId())
		return 

QUEST = Quest(-1,"Darnel","ai")
QUEST.addKillId(DARNEL)
