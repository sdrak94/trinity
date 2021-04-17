# Script is used for preventing displaying html for npcs that dont have html on retail
# Visit http://www.l2jdp.com/forum/ for more details.
import sys
from net.sf.l2j.gameserver.model.quest import Quest as JQuest
from net.sf.l2j.gameserver.network.serverpackets      import ActionFailed

NPCs = [18684,18685,18686,18687,18688,18689,18690,19691,18692,31557,31606, \
        31671,31672,31673,31674,32026,32030,32031,32032,32619,32620,32621]

class Quest (JQuest) :
    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)

    def onFirstTalk (self,npc,player):
        player.sendPacket(ActionFailed.STATIC_PACKET)
        return None

QUEST      = Quest(-1,".","custom")
for i in NPCs :
  QUEST.addFirstTalkId(i)
