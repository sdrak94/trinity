# Created by Kerberos
import sys

from net.sf.l2j.gameserver.model.quest        import State
from net.sf.l2j.gameserver.model.quest        import QuestState
from net.sf.l2j.gameserver.model.quest.jython import QuestJython as JQuest

qn = "1001_FortuneTelling"

BODY = "<html><body>Fortune-Teller Mine:<br>I see an image approaching before you... It is difficult to put what I saw into words.<br>How can I say this? Okay, listen closely:<br><br><center>"
END = "</center><br><br>Take these words to heart. You should seriously consider the meaning...</body></html>"
FORTUNE = [ \
	"What you\'ve endured will return as a benefit.", \
	"The dragon now acquires an eagle\'s wings.", \
	"Be warned as you may be overwhelmed by surroundings if you lack a clear opinion.", \
	"A new trial or start may be successful as luck shadows changes.", \
	"You may feel nervous and anxious because of unfavorable situations.", \
	"You may meet the person you\'ve longed to see.", \
	"You may meet many new people but it will be difficult to find a perfect person who wins your heart.", \
	"Good fortune and opportunity may lie ahead as if one's born in a golden spoon.", \
	"Be confident and act tenaciously at all times. You may be able to accomplish to perfection during somewhat unstable situations.", \
	"There may be an occasion where you are consoled by people.", \
	"Be independent at all times.", \
	"Do not loosen up with your precautions.", \
	"Closely observe people who pass by since you may meet a precious person who can help you.", \
	"Listen to the advice that's given to you with a humble attitude.", \
	"Focus on networking with like-minded people. They may join you for a big mission in the future.", \
	"Staying busy rather than being stationary will help.", \
	"You may lose your drive and feel lost.", \
	"People around you will encourage your every task in the future.", \
	"Be kind to and care for those close to you, they may help in the future.", \
	"Your ambition and dream will come true.", \
	"Your value will shine as your potential is finally realized.", \
	"If you keep smiling without despair, people will come to trust you and offer help.", \
	"There may be a little loss, but think of it as an investment for yourself.", \
	"The difficult situations will turn to hope with unforeseen help.", \
	"Impatience may lie ahead as the situation is unfavorable.", \
	"Be responsible with your tasks but do not hesitate to ask for colleagues\' help.", \
	"You may fall in danger each time when acting upon improvisation.", \
	"A determined act after prepared research will attract people.", \
	"A rest will promise a bigger development.", \
	"You will be rewarded for your efforts and accomplishments.", \
	"There are many things to consider after encountering hindrances.", \
	"Consider other\'s situations and treat them sincerely at all times.", \
	"A comparison to others may be helpful.", \
	"Be cautious to control emotions as temptations are nearby.", \
	"Momentarily delay an important decision.", \
	"Be confident and act tenaciously at all times. You may be able to accomplish to perfection during somewhat unstable situations.", \
	"Visiting a place you\'ve never been before may bring luck.", \
	"What used to be well managed may stumble one after another.", \
	"Your steady pursuit of new information and staying ahead of others will raise your value.", \
	"Being neutral is a good way to go, but clarity may be helpful contrary to your hesitance.", \
	"Skillful evasion is needed when dealing with people who pick fights as a disaster may arise from it.", \
	"Small things make up big things so even value trivial matters.", \
	"Bigger mistakes will be on the road if you fail to correct a small mistake.", \
	"Momentarily delay an important decision.", \
	"A remedy is on its way for a serious illness."]

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
    st = player.getQuestState(qn)
    if not st: return
    if st.getQuestItemsCount(57) < 1000 :
      htmltext="lowadena.htm"
    else :
      st.takeItems(57,1000)
      st.getRandom(45)
      htmltext=BODY+FORTUNE[st.getRandom(45)]+END
    st.exitQuest(1)
    return htmltext

QUEST       = Quest(-1,qn,"Custom")

QUEST.addStartNpc(32616)
QUEST.addTalkId(32616)