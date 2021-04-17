import sys
from net.sf.l2j.gameserver.model.quest.jython import QuestJython

class Quest (QuestJython) :

 def __init__(self,id,name,descr): QuestJython.__init__(self,id,name,descr)

 def onTalk (self,npc,player):
   st = player.getQuestState("1107_CrumaTower")
   htmltext = ""
   if player.getLevel() > 55 :
      htmltext = "30483.htm"
   else :
      player.teleToLocation(17724,114004,-11672)
   st.exitQuest(1)
   return htmltext

QUEST       = Quest(-1,"1107_CrumaTower","Teleports")

QUEST.addStartNpc(30483)
QUEST.addTalkId(30483)
