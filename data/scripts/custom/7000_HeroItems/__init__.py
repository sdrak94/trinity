# Made by DrLecter
import sys
from net.sf.l2j.gameserver.model.quest import State
from net.sf.l2j.gameserver.model.quest import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest
from net.sf.l2j.gameserver.datatables import ItemTable
qn = "7000_HeroItems"
MONUMENTS=[31690]+range(31769,31773)

HERO_ITEMS={
6611:["weapon_the_sword_of_hero_i00","Infinity Blade","During a critical attack, decreases one's P. Def and increases de-buff casting ability, damage shield effect, Max HP, Max MP, Max CP, and shield defense power. Also enhances damage to target during PvP.","379/169","Sword"],
6612:["weapon_the_two_handed_sword_of_hero_i00","Infinity Cleaver","Increases Max HP, Max CP, critical power and critical chance. Inflicts extra damage when a critical attack occurs and has possibility of reflecting the skill back on the player. Also enhances damage to target during PvP.","461/169","Double Handed Sword"],
6613:["weapon_the_axe_of_hero_i00","Infinity Axe","During a critical attack, it bestows one the ability to cause internal conflict to one's opponent. Damage shield function, Max HP, Max MP, Max CP as well as one's shield defense rate are increased. It also enhances damage to one's opponent during PvP.","379/169","Blunt"],
6614:["weapon_the_mace_of_hero_i00","Infinity Rod","When good magic is casted upon a target, increases MaxMP, MaxCP, Casting Spd, and MP regeneration rate. Also recovers HP 100% and enhances damage to target during PvP.","303/226","Blunt"],
6615:["weapon_the_hammer_of_hero_i00","Infinity Crusher","Increases MaxHP, MaxCP, and Atk. Spd. Stuns a target when a critical attack occurs and has possibility of reflecting the skill back on the player. Also enhances damage to target during PvP.","461/169","Blunt"],
6616:["weapon_the_staff_of_hero_i00","Infinity Scepter","When casting good magic, it can recover HP by 100% at a certain rate, increases MAX MP, MaxCP, M. Atk., lower MP Consumption, increases the Magic Critical rate, and reduce the Magic Cancel. Enhances damage to target during PvP.","369/226","Blunt"],
6617:["weapon_the_dagger_of_hero_i00","Infinity Stinger","Increases MaxMP, MaxCP, Atk. Spd., MP regen rate, and the success rate of Mortal and Deadly Blow from the back of the target. Silences the target when a critical attack occurs and has Vampiric Rage effect. Also enhances damage to target during PvP.","332/169","Dagger"],
6618:["weapon_the_fist_of_hero_i00","Infinity Fang","Increases MaxHP, MaxMP, MaxCP and evasion. Stuns a target when a critical attack occurs and has possibility of reflecting the skill back on the player at a certain probability rate. Also enhances damage to target during PvP.","461/169","Dual Fist"],
6619:["weapon_the_bow_of_hero_i00","Infinity Bow","Increases MaxMP/MaxCP and decreases re-use delay of a bow. Slows target when a critical attack occurs and has Cheap Shot effect. Also enhances damage to target during PvP.","707/169","Bow"],
6620:["weapon_the_dualsword_of_hero_i00","Infinity Wing","When a critical attack occurs, increases MaxHP, MaxMP, MaxCP and critical chance. Silences the target and has possibility of reflecting the skill back on the target. Also enhances damage to target during PvP.","461/169","Dual Sword"],
6621:["weapon_the_pole_of_hero_i00","Infinity Spear","During a critical attack, increases MaxHP, Max CP, Atk. Spd. and Accuracy. Casts dispel on a target and has possibility of reflecting the skill back on the target. Also enhances damage to target during PvP.","379/169","Pole"],
9388:["weapon_infinity_rapier_i00","Infinity Rapier","Decreases the target's P. Def and increases the de-buff casting ability, the damage shield ability, and the Max HP/Max MP/Max CP on a critical attack. Increases damage inflicted during PvP. A critical attack will have a chance to increase P. Atk., M. Atk., and healing power, and decrease MP consumption during skill use, for you and your party members.","344/169","Rapier"],
9389:["weapon_infinity_sword_i00","Infinity Sword","Increases critical attack success rate/power, MaxHP, MaxCP, and damage inflicted during PvP. Also inflicts extra damage on critical attacks, and reflects debuff attacks back on enemies.","410/169","Ancient Sword"],
9390:["weapon_infinity_shooter_i00","Infinity Shooter","Produces the following effects when a critical attack occurs: the target is slowed, decrease MP consumption for skill use, and increase Max MP/Max CP. Enhances damage done to the target during PvP.","405/169","Crossbow"],
6842:["accessory_hero_cap_i00","Wings of Destiny Circlet","Hair accessory exclusively used by heroes.","0","Hair Accessory"]
}

def render_list(mode,item) :
    html = "<html><body><font color=\"LEVEL\">List of Hero Items:</font><table border=0 width=300>"
    if mode == "list" :
       for i in HERO_ITEMS.keys() :
          html += "<tr><td width=35 height=45><img src=icon."+HERO_ITEMS[i][0]+" width=32 height=32 align=left></td><td valign=top><a action=\"bypass -h Quest 7000_HeroItems "+str(i)+"\"><font color=\"FFFFFF\">"+HERO_ITEMS[i][1]+"</font></a></td></tr>"
    else :
       html += "<tr><td align=left><font color=\"LEVEL\">Item Information</font></td><td align=right>\
<button value=Back action=\"bypass -h Quest 7000_HeroItems buy\" width=40 height=15 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>\
</td><td width=5><br></td></tr></table><table border=0 bgcolor=\"000000\" width=500 height=160><tr><td valign=top>\
<table border=0><tr><td valign=top width=35><img src=icon."+HERO_ITEMS[item][0]+" width=32 height=32 align=left></td>\
<td valign=top width=400><table border=0 width=100%><tr><td><font color=\"FFFFFF\">"+HERO_ITEMS[item][1]+"</font></td>\
</tr></table></td></tr></table><br><font color=\"LEVEL\">Item info:</font>\
<table border=0 bgcolor=\"000000\" width=290 height=220><tr><td valign=top><font color=\"B09878\">"+HERO_ITEMS[item][2]+"</font>\
</td></tr><tr><td><br>Type:"+HERO_ITEMS[item][4]+"<br><br>Patk/Matk: "+HERO_ITEMS[item][3]+"<br><br>\
<table border=0 width=300><tr><td align=center><button value=Obtain action=\"bypass -h Quest 7000_HeroItems _"+str(item)+"\" width=60 height=15 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df></td></tr></table></td></tr></table></td></tr>"
    html += "</table></body></html>"
    return html

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc, player) :
     st = player.getQuestState(qn)
     if not st : return
     if player.isHero():
       if event == "buy" :
          htmltext=render_list("list",0)
       elif event.isdigit() and int(event) in HERO_ITEMS.keys():
          htmltext=render_list("item",int(event))
       elif event.startswith("_") :
          item = int(event.split("_")[1])
          if item == 6842:
            if st.getQuestItemsCount(6842):
               htmltext = "You can't have more than one circlet and a weapon"
            else :
               st.giveItems(item,1)
               htmltext = "Enjoy your Wings of Destiny Circlet"
          else :
             for i in range(6611,6622)+range(9388,9391):
                if st.getQuestItemsCount(i):
                   st.exitQuest(1)
                   return "You already have an "+HERO_ITEMS[i][1]
             st.giveItems(item,1)
             htmltext = "Enjoy your "+HERO_ITEMS[item][1]
             st.playSound("ItemSound.quest_fanfare_2")
          st.exitQuest(1)
     return htmltext

 def onTalk (Self,npc,player):
     st = player.getQuestState(qn)
     htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
     if player.isHero():
        htmltext=render_list("list",0)
     else :
        st.exitQuest(1)
     return htmltext

QUEST       = Quest(-1,qn,"Hero Items")



for i in MONUMENTS:
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)
