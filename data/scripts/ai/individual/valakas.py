# L2J_JP CREATE SANDMAN
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.instancemanager.grandbosses import ValakasManager

#NPC
HEART_OF_VOLCANO = 31385
KLEIN            = 31540
VALAKAS          = 29028

#ITEM
FLOATING_STONE  = 7267
VALAKAS_CIRCLET = 8567

# Main Quest Code
class valakas(JQuest):

  def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

  def onTalk (self,npc,player):
    st = player.getQuestState("valakas")
    if not st : return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    npcId = npc.getNpcId()
    if npcId == HEART_OF_VOLCANO :
      if st.getInt("ok"):
        if ValakasManager.getInstance().isEnableEnterToLair():
          ValakasManager.getInstance().setValakasSpawnTask()
          st.player.teleToLocation(203940,-111840,66)
          return
        else:
          st.exitQuest(1)
          return "<html><body>Heart of Volcano:<br>Valakas is already awake!<br>You may not enter the Lair of Valakas.</body></html>"
      else:
        st.exitQuest(1)
        return "Conditions are not right to enter to Lair of Valakas."
    elif npcId == KLEIN :
      if ValakasManager.getInstance().isEnableEnterToLair():
        if st.getQuestItemsCount(FLOATING_STONE) > 0 :
          st.takeItems(FLOATING_STONE,1)
          player.teleToLocation(183831,-115457,-3296)
          st.set("ok","1")
        else:
          st.exitQuest(1)
          return "<html><body>Klein:<br>You do not have the Floating Stone. Go get one and then come back to me.</body></html>"
      else:
        st.exitQuest(1)
        return "<html><body>Klein:<br>Valakas is already awake!<br>You may not enter the Lair of Valakas.</body></html>"
      return

  def onKill (self,npc,player,isPet):
    st = player.getQuestState("valakas")
    #give the valakas slayer circlet to ALL PARTY MEMBERS who help kill valakas,
    party = player.getParty()
    if party :
       for partyMember in party.getPartyMembers().toArray() :
           pst = partyMember.getQuestState("valakas")
           if pst :
               if pst.getQuestItemsCount(VALAKAS_CIRCLET) < 1 :
                   pst.giveItems(VALAKAS_CIRCLET,1)
                   pst.exitQuest(1)
    else :
       pst = player.getQuestState("valakas")
       if pst :
           if pst.getQuestItemsCount(VALAKAS_CIRCLET) < 1 :
               pst.giveItems(VALAKAS_CIRCLET,1)
               pst.exitQuest(1)
    ValakasManager.getInstance().setCubeSpawn()
    if not st: return
    st.exitQuest(1)

# Quest class and state definition
QUEST = valakas(-1,"valakas","ai")

# Quest NPC starter initialization
QUEST.addStartNpc(KLEIN)
QUEST.addStartNpc(HEART_OF_VOLCANO)

QUEST.addTalkId(KLEIN)
QUEST.addTalkId(HEART_OF_VOLCANO)

QUEST.addKillId(VALAKAS)
