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
package ai.individual;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;
import scripts.ai.groupTemplates.L2AttackableAIScript;

public class FleeNpc extends L2AttackableAIScript 
{
	private int[] _npcId = { 20432, 22228 ,18150,18151,18152,18153,18154,18155,18156,18157};

	public FleeNpc(int questId, String name, String descr) 
	{
		super(questId, name, descr);

		for( int i = 0; i < _npcId.length; i++ )
		{
			this.addEventId(_npcId[i], QuestEventType.ON_ATTACK);
		}
	}

	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet) 
	{
		if (npc.getNpcId() >= 18150 && npc.getNpcId() <= 18157)
		{
			npc.getAI().setIntention( CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition((npc.getX() + Rnd.get(-40, 40)), (npc.getY()+ Rnd.get(-40, 40)), npc.getZ(), npc.getHeading()));
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return null;
		}
		else if (npc.getNpcId() == 20432 || npc.getNpcId() == 22228)
		{
			if (Rnd.get(3) == 2)
				npc.getAI().setIntention( CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition((npc.getX() + Rnd.get(-200, 200)), (npc.getY()+ Rnd.get(-200, 200)), npc.getZ(), npc.getHeading()));
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return null;
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	// Register the new Script at the Script System
	public static void main(String[] args) 
	{
		new FleeNpc(-1, "FleeNpc", "Ai for Flee Npcs");
	}
}
