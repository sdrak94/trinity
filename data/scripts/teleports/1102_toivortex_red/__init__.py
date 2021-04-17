import sys

from net.sf.l2j.gameserver.model.actor.instance import      L2PcInstance
from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
qn = "1102_toivortex_red"
RED_DIMENSION_STONE  = 4403
DIMENSION_VORTEX_1      = 30952
DIMENSION_VORTEX_2      = 30953

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)  
   npcId = npc.getNpcId()
   if npcId in [ DIMENSION_VORTEX_1, DIMENSION_VORTEX_2 ] : 
     if st.getQuestItemsCount(RED_DIMENSION_STONE) >= 1:
       st.takeItems(RED_DIMENSION_STONE,1)
       st.getPlayer().teleToLocation(118558,16659,5987)
       st.exitQuest(1)
       return
     else:
       st.exitQuest(1)
       return "1.htm"

QUEST       = Quest(-1,qn,"Teleports")

for i in [DIMENSION_VORTEX_1,DIMENSION_VORTEX_2] :
   QUEST.addStartNpc(i)
   QUEST.addTalkId(i)