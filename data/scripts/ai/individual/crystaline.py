# By Evil33t
import sys
from net.sf.l2j.gameserver.instancemanager        import InstanceManager
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets import CreatureSay
from net.sf.l2j.gameserver.datatables import ItemTable
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.model import L2ItemInstance
from net.sf.l2j.gameserver.ai import CtrlIntention
from net.sf.l2j.gameserver.model import L2CharPosition
from net.sf.l2j.gameserver.model import L2World
from net.sf.l2j.gameserver.network.serverpackets  import MagicSkillUse

npcid = 32328
crystalid = 9693

def autochat(npc,text):
	if npc: npc.broadcastPacket(CreatureSay(npc.getObjectId(),0,npc.getName(),text))

class PyObject:
	pass

class crys (JQuest):

	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.knownobjects=[]
		self.npcobject = {}
		self.worlds = {}

	def onAdvEvent (self,event,npc,player):
		if event == "timer" or event =="timer2":
			if self.npcobject[npc.getObjectId()].correct == False:
				dx = npc.getX() - 142999
				dy = npc.getY() - 151671
				d = dx * dx + dy * dy
				if d < 10000:
					npc.broadcastPacket(MagicSkillUse(npc, npc, 5441, 1, 1, 0))
					self.npcobject[npc.getObjectId()].correct = True
					self.worlds[npc.getInstanceId()].correct = self.worlds[npc.getInstanceId()].correct +1

			if self.npcobject[npc.getObjectId()].correct == False:
				dx = npc.getX() - 139494
				dy = npc.getY() - 151668
				d = dx * dx + dy * dy
				if d < 10000:
					npc.broadcastPacket(MagicSkillUse(npc, npc, 5441, 1, 1, 0))
					self.npcobject[npc.getObjectId()].correct = True
					self.worlds[npc.getInstanceId()].correct = self.worlds[npc.getInstanceId()].correct +1

			if self.worlds[npc.getInstanceId()].correct>=2:
				for door in InstanceManager.getInstance().getInstance(npc.getInstanceId()).getDoors():
					if door.getDoorId() == 24220026:
						door.openMe()
				return

			if self.npcobject[npc.getObjectId()].lastitem:
				L2World.getInstance().removeVisibleObject(self.npcobject[npc.getObjectId()].lastitem, self.npcobject[npc.getObjectId()].lastitem.getWorldRegion())
				L2World.getInstance().removeObject(self.npcobject[npc.getObjectId()].lastitem)
				if len(self.npcobject[npc.getObjectId()].walklist_order)==0:
					return

			for item in self.npcobject[npc.getObjectId()].walklist_order:
				crystal = self.npcobject[npc.getObjectId()].walklist[item]
				newpos = L2CharPosition(crystal.getX(), crystal.getY(), crystal.getZ(), 0)
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, newpos)
				self.npcobject[npc.getObjectId()].lastitem = crystal
				self.npcobject[npc.getObjectId()].walklist_order.remove(item)
				break;

			if(len(self.npcobject[npc.getObjectId()].walklist_order))>0:
				if event == "timer":
					self.startQuestTimer("timer2",2000,npc,None)
				else:
					self.startQuestTimer("timer",2000,npc,None)
			else:
				if self.npcobject[npc.getObjectId()].last == False:
					self.npcobject[npc.getObjectId()].last = True
					if event == "timer":
						self.startQuestTimer("timer2",2000,npc,None)
					else:
						self.startQuestTimer("timer",2000,npc,None)
			return

	def onTalk (self,npc,player):
		self.lastitem = None
		self.timer = 0
		crystals = []
		for object in player.getKnownList().getKnownObjects().values():
			if isinstance(object,L2ItemInstance):
				if object.getItemId()==crystalid:
					crystals.append(object)

		walklist_order = []		
		walklist = {}
		for crystal in crystals:
			dx = npc.getX() - crystal.getX()
			dy = npc.getY() - crystal.getY()
			d = dx * dx + dy * dy
			if d < 300000:
				walklist_order.append(d)
				walklist[d] = crystal

		walklist_order.sort()
		try:
			test = self.npcobject[npc.getObjectId()]
		except:
			self.npcobject[npc.getObjectId()] = PyObject()

		self.npcobject[npc.getObjectId()].last = False
		self.npcobject[npc.getObjectId()].walklist = walklist
		self.npcobject[npc.getObjectId()].walklist_order = walklist_order

		try:
			test = self.npcobject[npc.getObjectId()].lastitem
		except:
			self.npcobject[npc.getObjectId()].lastitem = None
		try:
			test2 = self.npcobject[npc.getObjectId()].correct
		except:
			self.npcobject[npc.getObjectId()].correct = False
		try:
			test3 = self.worlds[player.getInstanceId()]
		except:
			self.worlds[player.getInstanceId()] = PyObject()
		try:
			test4 = self.worlds[player.getInstanceId()].correct
		except:
			self.worlds[player.getInstanceId()].correct = 0
		self.startQuestTimer("timer",1000,npc,None)
		return

# Quest class and state definition
QUEST = crys(-1, "crys", "ai")
QUEST.addStartNpc(npcid)
QUEST.addTalkId(npcid)
