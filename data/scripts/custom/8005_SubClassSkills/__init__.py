import sys

from net.sf.l2j                               import Config
from net.sf.l2j.gameserver.datatables         import SkillTable
from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.network            import SystemMessageId
from net.sf.l2j.gameserver.network.serverpackets import AcquireSkillInfo
from net.sf.l2j.gameserver.network.serverpackets import AcquireSkillList
from net.sf.l2j.gameserver.network.serverpackets import SystemMessage
from net.sf.l2j.gameserver.util               import Util

qn = "8005_SubClassSkills"
NPC=32323

SKILLITEMS=[10280,10281,10282,10283,10284,10285,10286,10287,10288,10289,10290,10291,10292,10293,10294,10612]
SUBSKILLS={
10280:[631,632,633,634], # Common
10612:[637,638,639,640,799,800], # Enhanced
10281:[801,650,651], # Warriors
10282:[804,641,652], # Knights
10283:[644,645,653], # Rogues
10284:[802,646,654], # Wizards
10285:[803,648,1490], # Healers
10286:[643,1489,1491], # Summoners
10287:[642,647,655], # Enchanters
10289:[656], # Warriors
10288:[657], # Knights
10290:[658], # Rogues
10292:[659], # Wizards
10291:[661], # Healers
10294:[660], # Summoners
10293:[662] # Enchanters
}
QUESTVARSITEMS={
"EmergentAbility90-":[10280],
"EmergentAbility91-":[10280],
"ClassAbility92-":[10612,10281,10282,10283,10284,10285,10286,10287],
"ClassAbility93-":[10288,10289,10290,10291,10292,10293,10294]
}

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAcquireSkillList (self, npc, player) :
   asl = AcquireSkillList(AcquireSkillList.SkillType.unk4)
   st = player.getQuestState(qn)
   oldSkills = player.getAllSkills()
   count = 0
   for i in SKILLITEMS:
     for j in SUBSKILLS[i]:
       minLevel = 0
       maxLevel = SkillTable.getInstance().getMaxLevel(j)
       for oldsk in oldSkills:
         if oldsk.getId() == j:
           minLevel = oldsk.getLevel()
       if minLevel < maxLevel:
         count+=1
         asl.addSkill(j, minLevel+1, maxLevel, 0, 0)
   player.sendPacket(asl)
   if count == 0:
     player.sendPacket(SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN))
   return ""
   
 def onAcquireSkill (self, npc, player, skill) :
   if player.isSubClassActive():
     player.sendMessage("You are trying to learn skill that u can't..");
     Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
     return "false"
   st = player.getQuestState(qn)
   for i in SKILLITEMS:
     if skill.getId() in SUBSKILLS[i]:
       for var in QUESTVARSITEMS:
         if i in QUESTVARSITEMS[var]:
           for j in range(12):
             qvar = st.getGlobalQuestVar(var+str(j+1))
             if qvar != "" and qvar != "0" and not qvar.endswith(";") :
               Item = player.getInventory().getItemByObjectId(int(qvar))
               if Item and Item.getItemId() == i:
                 player.destroyItem(qn,int(qvar), 1, player, 0)
                 st.saveGlobalQuestVar(var+str(j+1),str(skill.getId())+";")
                 return "true"
   player.sendPacket(SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL))
   return "false"

 def onAcquireSkillInfo (self, npc, player, skill) :
   asi = AcquireSkillInfo(skill.getId(), skill.getLevel(), 0, 4)
   for i in SKILLITEMS:
     if skill.getId() in SUBSKILLS[i]:
       asi.addRequirement(99, i, 1, 50)
   player.sendPacket(asi)
   return ""

 def onAdvEvent (self, event, npc, player) :
   htmltext = event
   st = player.getQuestState(qn)
   if event == "learn":
     htmltext = ""
     if player.isSubClassActive():
       htmltext = "8005-04.htm"
     else:
       j=0
       for i in SKILLITEMS:
         j+=st.getQuestItemsCount(i)
       if j > 0:
         self.onAcquireSkillList(npc,player)
       else:
         htmltext = "8005-04.htm"
   elif event == "cancel":
     if st.getQuestItemsCount(57) < 20000000000:
       htmltext = "8005-07.htm"
     elif player.getSubClasses().size() == 0:
       htmltext = "8005-03.htm"
     elif player.isSubClassActive():
       htmltext = "8005-04.htm"
     else:
       activeCertifications = 0
       for var in QUESTVARSITEMS:
         for i in range(4):
           qvar = st.getGlobalQuestVar(var+str(i+1))
           if qvar.endswith(";") :
             activeCertifications += 1
           elif qvar != "" and qvar != "0" :
             activeCertifications += 1
       if activeCertifications == 0:
         htmltext = "8005-08.htm"
       else:
         for var in QUESTVARSITEMS:
           for i in range(4):
             qvar = st.getGlobalQuestVar(var+str(i+1))
             if qvar.endswith(";"):
               skill = SkillTable.getInstance().getInfo(int(qvar.replace(";","")), 1)
               if skill:
                 qvar = st.getGlobalQuestVar(var+str(i+1))
                 skillId = int(qvar.replace(";",""))
                 skillLevel = player.getSkillLevel(skillId)
                 skill = SkillTable.getInstance().getInfo(skillId, skillLevel)
                 player.removeSkill(skill)
                 st.saveGlobalQuestVar(var+str(i+1), "0")
             elif qvar != "" and qvar != "0" :
               Item = player.getInventory().getItemByObjectId(int(qvar))
               if Item :
                 player.destroyItem(qn, int(qvar), 1, player, 0)
               else :
                 Item = player.getWarehouse().getItemByObjectId(int(qvar))
                 if Item :
                   print "Somehow " + player.getName() + " put certification book into warehouse!"
                   player.getWarehouse().destroyItem(qn, Item, 1, player, None)
                 else:
                   print "Somehow " + player.getName() + " his/her delete certification book!"
               st.saveGlobalQuestVar(var+str(i+1), "0")
         st.takeItems(57,20000000000)
         htmltext = "8005-09.htm"
         player.sendSkillList()
   return htmltext

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   if npcId == NPC:
     st.set("cond","0")
     st.setState(State.STARTED)
     return "8005-01.htm"

QUEST       = Quest(-1,qn,"custom")

QUEST.addStartNpc(NPC)

QUEST.addTalkId(NPC)

QUEST.addAcquireSkillId(NPC)