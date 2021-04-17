# By Evil33t
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.ai import CtrlIntention

KECHI = 25532
GUARD1 = 22309
GUARD2 = 22310
GUARD3 = 22417

class PyObject:
	pass

class Quest (JQuest) :
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)
		self.npcobject = {}

	def SpawnMobs(self,npc):
		newNpc = self.addSpawn(GUARD1,154184,149230,-12151,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD1,153975,149823,-12152,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD1,154364,149665,-12151,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD1,153786,149367,-12151,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD2,154188,149825,-12152,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD2,153945,149224,-12151,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD3,154374,149399,-12152,0,False,0,False, npc.getInstanceId())
		newNpc = self.addSpawn(GUARD3,153796,149646,-12159,0,False,0,False, npc.getInstanceId())

	def onAttack(self, npc, player, damage, isPet, skill):
		npcId = npc.getNpcId()
		if npcId == KECHI:
			try:
				test = self.npcobject[npc.getObjectId()]
			except:
				self.npcobject[npc.getObjectId()] = PyObject()
				self.npcobject[npc.getObjectId()].started = False
				self.npcobject[npc.getObjectId()].killed = False
				self.npcobject[npc.getObjectId()].hp_to_spawn = {}
				self.npcobject[npc.getObjectId()].hp_to_spawn[80]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[60]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[40]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[30]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[20]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[10]=False
				self.npcobject[npc.getObjectId()].hp_to_spawn[5]=False
			maxHp = npc.getMaxHp()
			nowHp = npc.getStatus().getCurrentHp()
			if (nowHp < maxHp*0.8) and not self.npcobject[npc.getObjectId()].hp_to_spawn[80]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[80] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.6) and not self.npcobject[npc.getObjectId()].hp_to_spawn[60]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[60] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.4) and not self.npcobject[npc.getObjectId()].hp_to_spawn[40]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[40] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.3) and not self.npcobject[npc.getObjectId()].hp_to_spawn[30]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[30] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.2) and not self.npcobject[npc.getObjectId()].hp_to_spawn[20]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[20] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.1) and not self.npcobject[npc.getObjectId()].hp_to_spawn[10]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[10] = True
				self.SpawnMobs(npc)
			if (nowHp < maxHp*0.05) and not self.npcobject[npc.getObjectId()].hp_to_spawn[5]:
				self.npcobject[npc.getObjectId()].hp_to_spawn[5] = True
				self.SpawnMobs(npc)

	def onKill(self,npc,player,isPet):
		npcId = npc.getNpcId()
		if npcId == KECHI:
			self.addSpawn(32279,154077,149527,-12159,0,False,0,False, player.getInstanceId())
			self.npcobject[npc.getObjectId()].killed = True
		return 

QUEST = Quest(-1,"Kechi","ai")
QUEST.addKillId(KECHI)
QUEST.addAttackId(KECHI)
