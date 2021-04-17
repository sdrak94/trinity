/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package custom.IOPRace;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class IOPRace extends Quest
{
	final private static int RIGNOS = 32349;
	final private static int STAMP = 10013;
	final private static int KEY = 9694;
	final private static String qn = "IOPRace";
	private int _player = -1;

	public IOPRace(int id,String name,String descr)
	{
		super(id,name,descr);
		addStartNpc(RIGNOS);
		addTalkId(RIGNOS);
		addFirstTalkId(RIGNOS);
	}
	
	public String onFirstTalk(L2Npc npc,L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		int npcId = npc.getNpcId();
		if (npcId == RIGNOS)
		{
			if (_player != -1 && _player == player.getObjectId())
				return "32349-return.htm"; // retail text missing
			else if (_player != -1)
				return "32349-notavailable.htm"; // retail text missing
		}
		npc.showChatWindow(player);
		return null;
	}
	
	public String onTalk(L2Npc npc,L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		int npcId = npc.getNpcId();
		if (npcId == RIGNOS)
		{
			if (player.getLevel() < 78)
				return "32349-lolevel.htm"; // retail text missing
			else if (_player == -1)
			{
				// clean old data
				player.stopSkillEffects(5239);
				if (player.getPet() != null)
					player.getPet().stopSkillEffects(5239);
				st.takeItems(STAMP,-1);
				st.set("1st", "0");
				st.set("2nd", "0");
				st.set("3rd", "0");
				st.set("4th", "0");

				npc.setTarget(player);
				npc.doCast(SkillTable.getInstance().getInfo(5239, 5));
				if (player.getPet() != null)
				{
					npc.setTarget(player.getPet());
					npc.doCast(SkillTable.getInstance().getInfo(5239, 5));
				}
				startQuestTimer("timer", 1800000, null, null); // 30 min
				_player = player.getObjectId();
				return "32349-started.htm"; // retail text missing
			}
		}
		return super.onTalk(npc,player);
	}
	
	public String onAdvEvent (String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("timer"))
		{
			_player = -1;
			return null;
		}
		else if (event.equalsIgnoreCase("finish")) // this is probably custom
		{
			if (_player == player.getObjectId())
			{
				_player = -1;
				QuestState st = player.getQuestState(qn);
				if (st.getQuestItemsCount(STAMP) < 4)
				{
					// not done message missing
					return "32349-nofinish.htm";
				}
				st.takeItems(STAMP, -1);
				st.giveItems(KEY, 3);
				player.stopSkillEffects(5239);
				if (player.getPet() != null)
					player.getPet().stopSkillEffects(5239);
				cancelQuestTimers("timer");
				return "32349-finish.htm"; // need better message
			}
		}
		else if (event.equalsIgnoreCase("break")) // this is probably custom
		{
			if (_player == player.getObjectId())
			{
				_player = -1;
				QuestState st = player.getQuestState(qn);
				st.takeItems(STAMP, -1);
				player.stopSkillEffects(5239);
				if (player.getPet() != null)
					player.getPet().stopSkillEffects(5239);
				cancelQuestTimers("timer");
				return "32349-cancel.htm";
			}
		}
		return super.onAdvEvent(event,npc,player);
	}

	public static void main(String[] args)
	{
		new IOPRace(-1,qn,"custom");
	}
}