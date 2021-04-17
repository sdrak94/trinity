# version 0.2
# by Fulminus
# L2J_JP EDIT SANDMAN

import sys
from net.sf.l2j.gameserver.datatables import SkillTable
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.instancemanager.grandbosses import BaiumManager
from net.sf.l2j.util                              import Rnd

#NPC
BAIUM     = 29020
ARCHANGEL = 29021
STATUE    = 29025
VORTEX    = 31862

#ITEM
FABRIC = 4295

#SKILL
SPEAR_POUND = 4132
ANGEL_HEAL  = 4133

# Boss: Baium
class baium (JQuest):

  def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

  def onTalk (self,npc,player):
    st = player.getQuestState("baium")
    if not st : return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    npcId = npc.getNpcId()
    if npcId == STATUE :
      if st.getInt("ok"):
        if not npc.isBusy():
           npc.onBypassFeedback(player,"wake_baium")
           npc.setBusy(True)
           npc.setBusyMessage("Attending another player's request")
      else:
        st.exitQuest(1)
        return "Conditions are not right to wake up Baium"
    elif npcId == VORTEX :
      if BaiumManager.getInstance().isEnableEnterToLair() :
        if player.isFlying() :
          return "<html><body>Angelic Vortex:<br>You may not enter while flying a wyvern.</body></html>"
        if st.getQuestItemsCount(FABRIC) :
          st.takeItems(FABRIC,1)
          player.teleToLocation(113100,14500,10077)
          st.set("ok","1")
        else :
          return "<html><body>Angelic Vortex:<br>You do not have enough items.</body></html>"
      else :
        return "<html><body>Angelic Vortex:<br>You may not enter at this time.</body></html>"
    return

  def onAttack(self, npc, player, damage, isPet, skill) :
    if npc.getNpcId() == ARCHANGEL:
      if Rnd.get(100) < 10 :
        skill = SkillTable.getInstance().getInfo(SPEAR_POUND,1)
        if skill != None :
          npc.setTarget(player)
          npc.doCast(skill)
      if Rnd.get(100) < 5 and ((npc.getStatus().getCurrentHp() / npc.getStat().getMaxHp())*100) < 50:
        skill = SkillTable.getInstance().getInfo(ANGEL_HEAL,1)
        if skill != None :
          npc.setTarget(npc)
          npc.doCast(skill)
    else:
      BaiumManager.getInstance().setLastAttackTime()
    return

  def onKill (self,npc,player,isPet):
    npcId = npc.getNpcId()
    if npcId == BAIUM :
      BaiumManager.getInstance().setCubeSpawn()
      st = player.getQuestState("baium")
      if st :
        st.exitQuest(1)
    return


# Quest class and state definition
QUEST = baium(-1, "baium", "ai")
# Quest NPC starter initialization
QUEST.addStartNpc(STATUE)
QUEST.addStartNpc(VORTEX)

QUEST.addTalkId(STATUE)
QUEST.addTalkId(VORTEX)

QUEST.addKillId(BAIUM)

QUEST.addAttackId(BAIUM)
QUEST.addAttackId(ARCHANGEL)