# By Evil33t
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.ai import CtrlIntention

Tears = 25534
Tears_Copy = 25535

class PyObject:
	pass

class Quest (JQuest) :
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.npcobject = {}

	def onAttack (self, npc, player, damage, isPet, skill):
		npcId = npc.getNpcId()
		if npcId == Tears:
			try:
				test = self.npcobject[npc.getObjectId()]
			except:
				self.npcobject[npc.getObjectId()] = PyObject()
			try:
				test = self.npcobject[npc.getObjectId()].copylist
			except:
				self.npcobject[npc.getObjectId()].copylist = [] 
			try:
				test = self.npcobject[npc.getObjectId()].isSpawned
			except:
				self.npcobject[npc.getObjectId()].isSpawned = False

			if self.npcobject[npc.getObjectId()].isSpawned:
				for onpc in self.npcobject[npc.getObjectId()].copylist:
					onpc.onDecay()
				self.npcobject[npc.getObjectId()].copylist = [] 
				self.npcobject[npc.getObjectId()].isSpawned = False
				return
			maxHp = npc.getMaxHp()
			nowHp = npc.getStatus().getCurrentHp()
			rand = Rnd.get(0,150)
			if (nowHp < maxHp*0.4 and not self.npcobject[npc.getObjectId()].isSpawned) and rand<5:
				party = player.getParty()
				if party :
					for partyMember in party.getPartyMembers().toArray() :
						partyMember.setTarget(None)
						partyMember.abortAttack()
						partyMember.abortCast()
						partyMember.breakAttack();
						partyMember.breakCast();
						partyMember.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE)
				else:
						player.setTarget(None)
						player.abortAttack()
						player.abortCast()
						player.breakAttack();
						player.breakCast();
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE)

				self.npcobject[npc.getObjectId()].isSpawned = True
				for i in range(0,10):
					self.npcobject[npc.getObjectId()].copylist.append(self.addSpawn(Tears_Copy,npc.getX(),npc.getY(),npc.getZ(),0,False,0,False,player.getInstanceId()))

	def onKill(self,npc,player,isPet):
		npcId = npc.getNpcId()
		if npcId == Tears:
			self.addSpawn(32279,144307,154419,-11857,0,False,0,False, player.getInstanceId())
		return 

QUEST = Quest(-1,"Tears","ai")
QUEST.addAttackId(Tears)
QUEST.addAttackId(Tears_Copy)
QUEST.addKillId(Tears)
