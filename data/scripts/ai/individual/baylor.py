# By Umbrella HanWik
# Chests by Psychokill1888 and yes I removed that statue thing, the statue is the one for Sailren, so if i'm wrong just correct
import sys
from net.sf.l2j.gameserver.instancemanager             import InstanceManager
from net.sf.l2j.gameserver.model.entity                import Instance
from net.sf.l2j.gameserver.instancemanager.grandbosses import BaylorManager
from net.sf.l2j.gameserver.model.quest                 import State
from net.sf.l2j.gameserver.model.quest                 import QuestState
from net.sf.l2j.gameserver.model.quest.jython          import QuestJython as JQuest
from net.sf.l2j.gameserver.network.serverpackets       import SocialAction
from net.sf.l2j.tools.random                           import Rnd

#ENTRY_SATAT 0 = Baylor is not spawned
#ENTRY_SATAT 1 = Baylor is already dead
#ENTRY_SATAT 2 = Baylor is already entered by a other party
#ENTRY_SATAT 3 = Baylor is in interval
#ENTRY_SATAT 4 = You have no Party

#NPC
STATUE          =   32109
CRYSTALINE      =   29100
BAYLOR          =   29099
CHEST           =   29116

# Boss: baylor
class baylor (JQuest):

    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

#    def onTalk (self,npc,player):
#       st = player.getQuestState("baylor")
#        if not st : return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
#        npcId = npc.getNpcId()
#        if npcId == STATUE :
#            ENTRY_SATAT = BaylorManager.getInstance().canIntoBaylorLair(player)
#            if ENTRY_SATAT == 1 or ENTRY_SATAT == 2 :
#                st.exitQuest(1)
#            return "<html><body>Shilen's Stone Statue:<br>Another adventurers have already fought against the baylor. Do not obstruct them.</body></html>"
#        elif ENTRY_SATAT == 3 :
#            st.exitQuest(1)
#            return "<html><body>Shilen's Stone Statue:<br>The baylor is very powerful now. It is not possible to enter the inside.</body></html>"
#        elif ENTRY_SATAT == 4 :
#            st.exitQuest(1)
#            return "<html><body>Shilen's Stone Statue:<br>You seal the baylor alone? You should not do so! Bring the companion.</body></html>"
#        elif ENTRY_SATAT == 0 :
#            BaylorManager.getInstance().setBaylorSpawnTask(CRYSTALINE)
#            BaylorManager.getInstance().setBaylorSpawnTask(BAYLOR)
#            BaylorManager.getInstance().entryToBaylorLair(player)
#            return "<html><body>Shilen's Stone Statue:<br>Please seal the baylor by your ability.</body></html>"

    def onKill (self,npc,player,isPet):
        st = player.getQuestState("baylor")
        npcId = npc.getNpcId()
        if npcId == BAYLOR :
            instanceId = npc.getInstanceId()
            self.addSpawn(29116, 153763, 142075, -12741, 64792, False, 0, False, instanceId)
            self.addSpawn(29116, 153701, 141942, -12741, 57739, False, 0, False, instanceId)
            self.addSpawn(29116, 153573, 141894, -12741, 49471, False, 0, False, instanceId)
            self.addSpawn(29116, 153445, 141945, -12741, 41113, False, 0, False, instanceId)
            self.addSpawn(29116, 153381, 142076, -12741, 32767, False, 0, False, instanceId)
            self.addSpawn(29116, 153441, 142211, -12741, 25730, False, 0, False, instanceId)
            self.addSpawn(29116, 153573, 142260, -12741, 16185, False, 0, False, instanceId)
            self.addSpawn(29116, 153706, 142212, -12741, 7579, False, 0, False, instanceId)
            self.addSpawn(29116, 153571, 142860, -12741, 16716, False, 0, False, instanceId)
            self.addSpawn(29116, 152783, 142077, -12741, 32176, False, 0, False, instanceId)
            self.addSpawn(29116, 153571, 141274, -12741, 49072, False, 0, False, instanceId)
            self.addSpawn(29116, 154365, 142073, -12741, 64149, False, 0, False, instanceId)
            self.addSpawn(29116, 154192, 142697, -12741, 7894, False, 0, False, instanceId)
            self.addSpawn(29116, 152924, 142677, -12741, 25072, False, 0, False, instanceId)
            self.addSpawn(29116, 152907, 141428, -12741, 39590, False, 0, False, instanceId)
            self.addSpawn(29116, 154243, 141411, -12741, 55500, False, 0, False, instanceId)
            self.addSpawn(32273, 154243, 141411, -12741, 55500, False, 0, False, instanceId)
            if not st: return
            st.exitQuest(1)
        return

# Quest class and state definition
QUEST = baylor(-1, "baylor", "ai")

# Quest NPC starter initialization
QUEST.addStartNpc(STATUE)

#QUEST.addTalkId(STATUE)

#QUEST.addKillId(CRYSTALINE)
QUEST.addKillId(BAYLOR)