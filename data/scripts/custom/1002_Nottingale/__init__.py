#made by Kerberos
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets import RadarControl

qn = "1002_Nottingale"

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player):
    htmltext = event
    st = player.getQuestState(qn)
    qs = player.getQuestState("10273_GoodDayToFly")
    if not qs or qs and qs.getState() != State.COMPLETED:
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-184545,243120,1581))
       htmltext = "32627.htm"
    elif event == "32627-3.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-192361,254528,3598))
    elif event == "32627-4.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-174600,219711,4424))
    elif event == "32627-5.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-181989,208968,4424))
    elif event == "32627-6.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-252898,235845,5343))
    elif event == "32627-8.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-212819,209813,4288))
    elif event == "32627-9.htm" :
       player.sendPacket(RadarControl(2,2,0,0,0))
       player.sendPacket(RadarControl(0,2,-246899,251918,4352))
    return htmltext

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   if not st :
      st = self.newQuestState(player)
   player.setLastQuestNpcObject(npc.getObjectId())
   npc.showChatWindow(player)
   return None

QUEST       = Quest(-1,qn,"custom")
QUEST.addStartNpc(32627)
QUEST.addFirstTalkId(32627)
QUEST.addTalkId(32627)