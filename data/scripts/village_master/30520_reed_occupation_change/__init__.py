#
# Created by DraX on 2005.08.08 modified by Ariakas on 2005.09.19
#

import sys

from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
qn = "30520_reed_occupation_change"
WAREHOUSE_CHIEF_REED = 30520

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc, player) :
   htmltext = event
   st = player.getQuestState(qn)
   if not st : return

   htmltext = "No Quest"

   Race     = st.getPlayer().getRace()
   ClassId  = st.getPlayer().getClassId()
   Level    = st.getPlayer().getLevel()

   if event == "30520-01.htm":
     return "30520-01.htm"

   if event == "30520-02.htm":
     return "30520-02.htm"

   if event == "30520-03.htm":
     return "30520-03.htm"

   if event == "30520-04.htm":
     return "30520-04.htm"

   st.exitQuest(1)
   return htmltext

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()

   Race    = st.getPlayer().getRace()
   ClassId = st.getPlayer().getClassId()
   
   # Dwarfs got accepted
   if npcId == WAREHOUSE_CHIEF_REED and Race in [Race.Dwarf]:
     if ClassId in [ClassId.dwarvenFighter]:
       htmltext = "30520-01.htm"
       st.setState(State.STARTED)
       return htmltext
     if ClassId in [ClassId.scavenger, ClassId.artisan]:
       htmltext = "30520-05.htm"
       st.exitQuest(False)
       st.exitQuest(1)
       return htmltext
     if ClassId in [ClassId.bountyHunter, ClassId.warsmith]:
       htmltext = "30520-06.htm"
       st.exitQuest(False)
       st.exitQuest(1)
       return htmltext

   # All other Races must be out
   if npcId == WAREHOUSE_CHIEF_REED and Race in [Race.Orc, Race.DarkElf, Race.Elf, Race.Human, Race.Kamael]:
     st.exitQuest(False)
     st.exitQuest(1)
     return "30520-07.htm"

QUEST   = Quest(30520,qn,"village_master")



QUEST.addStartNpc(30520)

QUEST.addTalkId(30520)