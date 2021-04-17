# By Psychokiller1888

import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.util                              import Rnd
from net.sf.l2j.gameserver.datatables import ItemTable

BOX = 29116

def dropItem(npc,itemId,count):
	ditem = ItemTable.getInstance().createItem("Loot", itemId, count, None)
	ditem.dropMe(npc, npc.getX(), npc.getY(), npc.getZ()); 

class baylorChest(JQuest):
	def __init__(self,id,name,descr):
		self.isSpawned = False
		JQuest.__init__(self,id,name,descr)

	def onKill (self,npc,player,isPet):
		chance = Rnd.get(100)
		if chance <= 1:
			dropItem(npc,9470,1)
		elif chance >= 2 and chance <= 32:
			dropItem(npc,6578,2)
		else:
			dropItem(npc,6704,10)
		return

QUEST = baylorChest(-1, "baylorChest", "ai")
QUEST.addKillId(BOX)
