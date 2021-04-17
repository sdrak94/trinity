#
# Created by DraX on 2005.07.27. updated by DrLecter.
#
import sys

from net.sf.l2j.gameserver.model.actor.instance import L2PcInstance
from net.sf.l2j.gameserver.model.quest          import State
from net.sf.l2j.gameserver.model.quest          import QuestState
from net.sf.l2j.gameserver.model.quest.jython   import QuestJython as JQuest
qn = "1101_teleport_to_race_track"

RACE_MANAGER = 30995

TELEPORTERS = {
    30059:3,    # TRISHA
    30080:4,    # CLARISSA
    30177:6,    # VALENTIA
    30233:8,    # ESMERALDA
    30256:2,    # BELLA
    30320:1,    # RICHLIN
    30848:7,    # ELISA
    30899:5,    # FLAUEN
    31320:9,    # ILYANA
    31275:10,   # TATIANA
    31964:11,   # BILIA
    31210:12    # RACE TRACK GK
}

RETURN_LOCS = [[-80826,149775,-3043],[-12672,122776,-3116],[15670,142983,-2705],[83400,147943,-3404], \
              [111409,219364,-3545],[82956,53162,-1495],[146331,25762,-2018],[116819,76994,-2714], \
              [43835,-47749,-792],[147930,-55281,-2728],[87386,-143246,-1293],[12882,181053,-3560]]
              

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   if not st: return
   ###################
   # Start Locations #
   ###################
   if TELEPORTERS.has_key(npcId) :
     st.getPlayer().teleToLocation(12661,181687,-3560)
     st.setState(State.STARTED)
     st.set("id",str(TELEPORTERS[npcId]))     
   ############################
   # Monster Derby Race Track #
   ############################
   elif npcId == RACE_MANAGER:
     if st.getState() == State.STARTED and st.getInt("id") :
        # back to start location
        return_id = st.getInt("id") - 1
        if return_id < 13:
           st.getPlayer().teleToLocation(RETURN_LOCS[return_id][0],RETURN_LOCS[return_id][1],RETURN_LOCS[return_id][2])
           st.exitQuest(1)
           return
     # no base location founded, teleport him to nearest race GK
     st.getPlayer().teleToLocation(12882,181053,-3560)
     st.exitQuest(1)
   return

QUEST       = Quest(-1,qn,"Teleports")

for npcId in TELEPORTERS.keys() :
    QUEST.addStartNpc(npcId)
    QUEST.addTalkId(npcId)

QUEST.addTalkId(RACE_MANAGER)