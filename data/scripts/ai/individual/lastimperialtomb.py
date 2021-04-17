# L2J_JP CREATE SANDMAN
import sys
from net.sf.l2j import Config
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.instancemanager.lastimperialtomb import LastImperialTombManager

qn = "lastimperialtomb"

#NPC
GUIDE         = 32011
ALARM_DEVICE  = 18328
CHOIR_PRAYER  = 18339
CHOIR_CAPTAIN = 18334
FRINTEZZA     = 29045

class lastimperialtomb (JQuest):
  def __init__(self,id,name,descr) :
    JQuest.__init__(self,id,name,descr)

  def onTalk (self,npc,player) :
    st = player.getQuestState(qn)
    if not st : return "<html><body>1</body></html>"
    npcId = npc.getNpcId()
    if npcId == GUIDE : # Frintezza Teleporter
      if player.isFlying() :
        return "<html><body>Imperial Tomb Guide:<br>To enter, get off the wyvern.</body></html>"
      if Config.LIT_REGISTRATION_MODE == 0 :
        if LastImperialTombManager.getInstance().tryRegistrationCc(player) :
          LastImperialTombManager.getInstance().registration(player,npc)
      elif Config.LIT_REGISTRATION_MODE == 1 :
        if LastImperialTombManager.getInstance().tryRegistrationPt(player) :
          LastImperialTombManager.getInstance().registration(player,npc)
      elif Config.LIT_REGISTRATION_MODE == 2 :
        if LastImperialTombManager.getInstance().tryRegistrationPc(player) :
          LastImperialTombManager.getInstance().registration(player,npc)
    return

  def onKill (self,npc,player,isPet):
    st = player.getQuestState(qn)
    npcId = npc.getNpcId()
    if npcId == ALARM_DEVICE :
      LastImperialTombManager.getInstance().onKillHallAlarmDevice()
    elif npcId == CHOIR_PRAYER :
      LastImperialTombManager.getInstance().onKillDarkChoirPlayer()
    elif npcId == CHOIR_CAPTAIN :
      LastImperialTombManager.getInstance().onKillDarkChoirCaptain()
    return

  def onFirstTalk (self,npc,player):
    return None

# Quest class and state definition
QUEST = lastimperialtomb(-1, qn, "ai")
# Quest NPC starter initialization
QUEST.addStartNpc(GUIDE)

QUEST.addTalkId(GUIDE)

QUEST.addKillId(ALARM_DEVICE)
QUEST.addKillId(CHOIR_PRAYER)
QUEST.addKillId(CHOIR_CAPTAIN)

QUEST.addFirstTalkId(FRINTEZZA)
