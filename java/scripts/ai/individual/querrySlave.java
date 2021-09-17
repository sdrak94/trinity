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
package scripts.ai.individual;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;

public class querrySlave extends QuestJython
{
	private final int[]	_npcId	=
							{ 32299 };

	public querrySlave(int questId, String name, String descr)
	{
		super(questId, name, descr);

		for (int finalElement : _npcId)
		{
			addEventId(finalElement, QuestEventType.ON_FIRST_TALK);
			addEventId(finalElement, QuestEventType.ON_TALK);
		}
	}

	private void follow(L2Npc npc, L2PcInstance player)
	{
		npc.setTarget(player);
		npc.getAI().setIntention(AI_INTENTION_FOLLOW);
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		follow(npc, player);
		return "";
	}

	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		follow(npc, player);
		return "";
	}

	// Register the new Script at the Script System
	public static void main(String[] args)
	{
		new querrySlave(-1, "querrySlave", "Ai for Querry Slaves");
	}
}
