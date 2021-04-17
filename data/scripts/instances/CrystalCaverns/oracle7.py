# Script by Psychokiller1888

import sys
from net.sf.l2j.gameserver.instancemanager             import InstanceManager
from net.sf.l2j.gameserver.model.entity                import Instance
from net.sf.l2j.gameserver.model.quest                 import State
from net.sf.l2j.gameserver.model.quest                 import QuestState
from net.sf.l2j.gameserver.model.quest.jython          import QuestJython as JQuest
from net.sf.l2j.gameserver.instancemanager             import InstanceManager
from net.sf.l2j.gameserver.model.entity                import Instance
from net.sf.l2j.util                                   import Rnd
from net.sf.l2j.gameserver.datatables                  import ItemTable

ORACLE_GUIDE  = 32280
BLUE_CRYSTAL  = 9695
RED_CRYSTAL   = 9696
CLEAR_CRYSTAL = 9697
PRISON_KEY    = 10015
BAYLOR        = 29099

CRY = [9695,9696,9697]

class PyObject:
	pass

def exitInstance(player,teleto):
	player.setInstanceId(0)
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.setInstanceId(0)
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)

def teleportplayer(player,teleto):
	player.teleToLocation(teleto.x, teleto.y, teleto.z)
	pet = player.getPet()
	if pet != None :
		pet.teleToLocation(teleto.x, teleto.y, teleto.z)
	return

def dropItem(npc,itemId,count):
	ditem = ItemTable.getInstance().createItem("Loot", itemId, count, None)
	ditem.dropMe(npc, npc.getX(), npc.getY() + 50, npc.getZ());

class oracle7(JQuest):
	def __init__(self,id,name,descr):
		JQuest.__init__(self,id,name,descr)

	def onAdvEvent(self,event,npc,player):
		htmltext = event
		instanceId = player.getInstanceId()
		if event == "32280-1":
			item1 = player.getInventory().getItemByItemId(BLUE_CRYSTAL)
			item2 = player.getInventory().getItemByItemId(RED_CRYSTAL)
			item3 = player.getInventory().getItemByItemId(CLEAR_CRYSTAL)
			if not item1 or not item2 or not item3:
				htmltext = "32280-3"
			else:
				rndCrystal = Rnd.get(len(CRY))
				player.destroyItemByItemId("Quest", CRY[rndCrystal], 1, player, True)
				dropItem(npc,PRISON_KEY,1)
				self.addSpawn(BAYLOR, 153569, 142075, -12732, 37421, False, 0, False, instanceId)
		elif event == "32280-2":
			tele = PyObject()
			tele.x = 149361
			tele.y = 172327
			tele.z = -945
			exitInstance(player,tele)
			return
		return htmltext+".htm"

	def onTalk (self,npc,player):
		npcId = npc.getNpcId()
		if npcId == ORACLE_GUIDE:
			htmltext = "32280.htm"
		return htmltext

QUEST = oracle7(-1, "oracle7", "ai")
QUEST.addStartNpc(ORACLE_GUIDE)
QUEST.addTalkId(ORACLE_GUIDE)