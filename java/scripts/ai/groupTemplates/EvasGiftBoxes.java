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
package scripts.ai.groupTemplates;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.util.Rnd;

public class EvasGiftBoxes extends Quest
{
	final private static int GIFTBOX = 32342;

	final private static int KISSOFEVA = 1073;

	// index 0: without kiss of eva
	// index 1: with kiss of eva
	// chance,itemId,...
	final private static int[][] CHANCES = {{2,9692,1,9693},{100,9692,50,9693}};

	final private static String qn = "EvasGiftBoxes";

	public EvasGiftBoxes(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addKillId(GIFTBOX);
		addSpawnId(GIFTBOX);
	}
	
	public String onSpawn(L2Npc npc)
	{
		npc.setIsNoRndWalk(true);
		return super.onSpawn(npc);
	}

	public String onKill (L2Npc npc, L2PcInstance killer, boolean isPet) 
	{
		if (npc.getNpcId() == GIFTBOX)
		{
			QuestState st = killer.getQuestState(qn);
			if (st == null)
				st = newQuestState(killer);
			int isKissOfEvaBuffed = 0;
			if (killer.getFirstEffect(KISSOFEVA) != null)
				isKissOfEvaBuffed = 1;
			for (int i = 0; i < CHANCES[isKissOfEvaBuffed].length; i += 2)
				if (Rnd.get(100) < CHANCES[isKissOfEvaBuffed][i])
					st.giveItems(CHANCES[isKissOfEvaBuffed][i+1],1);
		}
		return super.onKill(npc,killer,isPet);
	}

	public static void main(String[] args)
	{
		new EvasGiftBoxes(-1,qn,"ai");
	}
}