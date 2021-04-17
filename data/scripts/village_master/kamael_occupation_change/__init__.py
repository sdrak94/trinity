# Created by DrLecter for the L2J Official Datapack Project
# Visit us at http://www.l2jdp.com/
# See readme-dp.txt and gpl.txt for license and distribution details
# Let us know if you did not receive a copy of such files.
import sys

from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest

qn = "kamael_occupation_change"

GWAINS_RECOMMENTADION = 9753
ORKURUS_RECOMMENDATION = 9760
STEELRAZOR_EVALUATION = 9772
KAMAEL_INQUISITOR_MARK = 9782
SOUL_BREAKER_CERTIFICATE = 9806
#MAYNARD,KHADAVA,GERSHWIN,VALPOR,HOLST,CASCA,BROME,ZENYA,AETONIC,BARTA,
#MIYA,VITUS,LIANE,EDDY,FERDINAND,TAINE,RUPAUL,MELDINA,HAGEL,CECI,
#PIECHE,ZOLDART,NIZER,YENICHE
NPCS_MALE1=[32139,32196,32199]
NPCS_MALE2=[32146,32205,32209,32213,32217,32221,32225,32229,32233]
NPCS_FEMALE1=[32140,32193,32202]
NPCS_FEMALE2=[32145,32206,32210,32214,32218,32222,32226,32230,32234]
SHADOW_WEAPON_COUPON_DGRADE = 8869
SHADOW_WEAPON_COUPON_CGRADE = 8870

#Filenames are made with the lowest npcId from the NPCs list. Some scripts
#contain generic dialogs for every npc to use, some others keep separate
#dialogs for different npcs.
preffix="32139"
#event:[newclass,req_class,req_race,low_ni,low_i,ok_ni,ok_i,[req_items]]
#low_ni : level too low, and you dont have quest item
#low_i: level too low, despite you have the item
#ok_ni: level ok, but you don't have quest item
#ok_i: level ok, you got quest item, class change takes place
CLASSES = {
    "DR":[125,123,5,20,"16","17","18","19",[GWAINS_RECOMMENTADION],SHADOW_WEAPON_COUPON_DGRADE],#m_kamael -> m_trooper
    "WA":[126,124,5,20,"20","21","22","23",[STEELRAZOR_EVALUATION],SHADOW_WEAPON_COUPON_DGRADE], #f_kamael -> f_warder
    "BE":[127,125,5,40,"24","25","26","27",[ORKURUS_RECOMMENDATION],SHADOW_WEAPON_COUPON_CGRADE], #m_trooper -> m_berserker
    "AR":[130,126,5,40,"28","29","30","31",[KAMAEL_INQUISITOR_MARK],SHADOW_WEAPON_COUPON_CGRADE], #f_warder -> f_arbalester
    "SBF":[129,126,5,40,"40","41","42","43",[SOUL_BREAKER_CERTIFICATE],SHADOW_WEAPON_COUPON_CGRADE], #f_warder -> f_soulbreaker
    "SBM":[128,125,5,40,"40","41","42","43",[SOUL_BREAKER_CERTIFICATE],SHADOW_WEAPON_COUPON_CGRADE]  #m_trooper -> m_soulbreaker
    }
#Messages
default = "No Quest"

def change(st,player,newclass,items) :
   for item in items :
      st.takeItems(item,1)
   st.playSound("ItemSound.quest_fanfare_2")
   player.setClassId(newclass)
   player.setBaseClass(newclass)
   player.broadcastUserInfo()
   return

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
   npcId    = npc.getNpcId()
   htmltext = default
   suffix = ''
   st = player.getQuestState(qn)
   if not st : return
   race     = player.getRace().ordinal()
   classid  = player.getClassId().getId()
   level    = player.getLevel()
   if npcId not in NPCS_MALE1 + NPCS_MALE2 + NPCS_FEMALE1 + NPCS_FEMALE2 : return
   if not event in CLASSES.keys() :
     return event
   else :
     newclass,req_class,req_race,req_level,low_ni,low_i,ok_ni,ok_i,req_item,reward=CLASSES[event]
     if race == req_race and classid == req_class :
        item = True
        for i in req_item :
            if not st.getQuestItemsCount(i):
               item = False
        if level < req_level :
           suffix = low_i
           if not item :
              suffix = low_ni
        else :
           if not item :
              suffix = ok_ni
           else :
              suffix = ok_i
              change(st,player,newclass,req_item)
              st.giveItems(reward,15)
     st.exitQuest(1)
     htmltext = preffix+"-"+suffix+".htm"
   return htmltext

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   race = player.getRace().ordinal()
   classId = player.getClassId()
   id = classId.getId()
   htmltext = default
   if player.isSubClassActive() :
      st.exitQuest(1)
      return htmltext
   # Kamaels only
   htmltext = preffix
   if race in [5] :
      if classId.level() >= 2 : # second/third occupation change already made
         htmltext += "-32.htm"
      elif npcId in NPCS_MALE1 :
         if id == 123 :      # m_fighter
            return htmltext+"-01.htm"
         else :
            return htmltext+"-34.htm"
      elif npcId in NPCS_FEMALE1 :
         if id == 124 :    # f_fighter
            return htmltext+"-05.htm"
         else :
            return htmltext+"-34.htm"
      elif npcId in NPCS_MALE2 :
         if id == 125 :      # m_trooper
            return htmltext+"-09.htm"
         else :
            return htmltext+"-34.htm"
      elif npcId in NPCS_FEMALE2 :
         if id == 126 :    # f_warder
            return htmltext+"-35.htm"
         else :
            return htmltext+"-34.htm"
   else :
      htmltext += "-33.htm"  # other races
   st.exitQuest(1)
   return htmltext

QUEST   = Quest(99990,qn,"village_master")

for npc in NPCS_MALE1 + NPCS_MALE2 + NPCS_FEMALE1 + NPCS_FEMALE2:
    QUEST.addStartNpc(npc)
    QUEST.addTalkId(npc)
