#
# Created by Kerberos
#
import sys

from net.sf.l2j.gameserver.model.quest          	import State
from net.sf.l2j.gameserver.model.quest          	import QuestState
from net.sf.l2j.gameserver.model.quest.jython   	import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets 	import NpcSay
qn = "1106_teleport_to_fantasy_isle"
PADDIES = 32378

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
    31964:11    # BILIA
}

RETURN_LOCS = [[-80826,149775,-3043],[-12672,122776,-3116],[15670,142983,-2705],[83400,147943,-3404], \
              [111409,219364,-3545],[82956,53162,-1495],[146331,25762,-2018],[116819,76994,-2714], \
              [43835,-47749,-792],[147930,-55281,-2728],[87386,-143246,-1293]]              

ISLE_LOCS = [[-58752,-56898,-2032],[-59716,-57868,-2032],[-60691,-56893,-2032],[-59720,-55921,-2032]]

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
     random_id = st.getRandom(len(ISLE_LOCS))
     x,y,z = ISLE_LOCS[random_id][0],ISLE_LOCS[random_id][1],ISLE_LOCS[random_id][2]
     st.getPlayer().teleToLocation(x,y,z)
     st.setState(State.STARTED)
     st.set("id",str(TELEPORTERS[npcId]))     
   ################
   # Fantasy Isle #
   ################
   elif npcId == PADDIES:
     if st.getState() == State.STARTED and st.getInt("id") :
        # back to start location
        return_id = st.getInt("id") - 1
        st.getPlayer().teleToLocation(RETURN_LOCS[return_id][0],RETURN_LOCS[return_id][1],RETURN_LOCS[return_id][2])
        st.unset("id")
     else:
        # no base location founded (player swimmed)
        player.sendPacket(NpcSay(npc.getObjectId(),0,npc.getNpcId(),"You've arrived here from a different way. I'll send you to Rune Township which is the nearest town."))
        st.getPlayer().teleToLocation(43835,-47749,-792)
     st.exitQuest(1)
   return

QUEST       = Quest(-1,qn,"Teleports")

for npcId in TELEPORTERS.keys() :
    QUEST.addStartNpc(npcId)
    QUEST.addTalkId(npcId)

QUEST.addStartNpc(PADDIES)
QUEST.addTalkId(PADDIES)