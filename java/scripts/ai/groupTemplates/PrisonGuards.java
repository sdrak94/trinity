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

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.util.Rnd;

public class PrisonGuards extends Quest
{
	final private static int GUARD1 = 18367;
	final private static int GUARD2 = 18368;
	final private static int STAMP = 10013;
	final private static String[] GUARDVARS = {"1st","2nd","3rd","4th"};
	final private static String qn = "IOPRace";
	
	private Map<L2Npc,Integer> _guards = new FastMap<L2Npc, Integer>();

	public PrisonGuards(int questId, String name, String descr)
	{
		super(questId, name, descr);
		int[] mob = {GUARD1,GUARD2};
		addAggroRangeEnterId(GUARD2);
		for (int npc : mob)
		{
			addKillId(npc);
			addAttackId(npc);
			addSpellFinishedId(npc);
		}
		// place 1
		_guards.put(this.addSpawn(GUARD2,160704,184704,-3704,49152,false,0),0);
		_guards.put(this.addSpawn(GUARD2,160384,184704,-3704,49152,false,0),0);
		_guards.put(this.addSpawn(GUARD1,160528,185216,-3704,49152,false,0),0);
		// place 2
		_guards.put(this.addSpawn(GUARD2,135120,171856,-3704,49152,false,0),1);
		_guards.put(this.addSpawn(GUARD2,134768,171856,-3704,49152,false,0),1);
		_guards.put(this.addSpawn(GUARD1,134928,172432,-3704,49152,false,0),1);
		// place 3
		_guards.put(this.addSpawn(GUARD2,146880,151504,-2872,49152,false,0),2);
		_guards.put(this.addSpawn(GUARD2,146366,151506,-2872,49152,false,0),2);
		_guards.put(this.addSpawn(GUARD1,146592,151888,-2872,49152,false,0),2);
		// place 4
		_guards.put(this.addSpawn(GUARD2,155840,160448,-3352,0,false,0),3);
		_guards.put(this.addSpawn(GUARD2,155840,159936,-3352,0,false,0),3);
		_guards.put(this.addSpawn(GUARD1,155578,160177,-3352,0,false,0),3);
		for (L2Npc npc : _guards.keySet())
		{
			npc.setIsNoRndWalk(true);
			if (npc.getNpcId() == GUARD1)
			{
				npc.setIsInvul(true);
				npc.disableCoreAI(true);
			}
		}
	}
	
	public void saveGlobalData()
	{
		for (L2Npc npc : _guards.keySet())
			npc.decayMe();
	}
	
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (npc.getNpcId() == GUARD2 && !npc.isInCombat() && npc.getTarget() == null)
		{
			L2Character attacker = (isPet ? player.getPet():player);
			if (attacker.getFirstEffect(5239) == null)
				return null;
			npc.setTarget(attacker);
			npc.setIsRunning(true);
			((L2Attackable) npc).addDamageHate(player, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		return super.onAggroRangeEnter(npc,player,isPet);
	}
	
	public String onKill (L2Npc npc, L2PcInstance killer, boolean isPet) 
	{
		if (_guards.containsKey(npc))
			this.startQuestTimer("Respawn", 60000, npc, null);
		return super.onKill(npc,killer,isPet);
	}
	
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if (npc.getNpcId() == GUARD2)
		{
			List<L2Character> removingChars = new FastList<L2Character>();
			for (L2Character character : ((L2MonsterInstance)npc).getAggroList().keySet())
				if (character.getFirstEffect(5239) == null)
					removingChars.add(character);
			for (L2Character character : removingChars)
				((L2MonsterInstance)npc).getAggroList().remove(character);
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	public String onAttack (L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		int npcId = npc.getNpcId();
		L2Character acting = (isPet ? attacker.getPet():attacker);
		boolean found = (acting.getFirstEffect(5239) != null);
		if (npcId == GUARD1 || npcId == GUARD2)
		{
			if (!found)
			{
				getPara(npc,attacker,isPet);
			}
			else if (npcId == GUARD2)
			{
				return super.onAttack(npc,attacker,damage,isPet,skill);
			}
			else if (!_guards.containsKey(npc))
			{
				return super.onAttack(npc,attacker,damage,isPet,skill);
			}
			else if (found && Rnd.get(100) < 5)
			{
				if (attacker.getQuestState(qn) != null && attacker.getQuestState(qn).getInt(GUARDVARS[_guards.get(npc)]) != 1)
				{
					attacker.getQuestState(qn).set(GUARDVARS[_guards.get(npc)], "1");
					attacker.getQuestState(qn).giveItems(STAMP,1);
				}
			}
			else
				return super.onAttack(npc,attacker,damage,isPet,skill);
		}
		return super.onAttack(npc,attacker,damage,isPet,skill);
	}
	
	public String onAdvEvent (String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("Respawn"))
		{
			L2Npc newGuard = this.addSpawn(npc.getNpcId(), npc.getSpawn().getLocx(), npc.getSpawn().getLocy(), npc.getSpawn().getLocz(), npc.getSpawn().getHeading(), false, 0);
			newGuard.setIsNoRndWalk(true);
			if (npc.getNpcId() == GUARD1)
			{
				newGuard.setIsInvul(true);
				newGuard.disableCoreAI(true);
			}
			int place = _guards.get(npc);
			_guards.remove(npc);
			_guards.put(newGuard, place);
		}
		return "";
	}
	
	public void getPara(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		if (npc.isCastingNow())
			return;
		else if (isSummon)
		{
			player.getPet().stopSkillEffects(4578);
			npc.setTarget(player.getPet());
			npc.doCast(SkillTable.getInstance().getInfo(4578, 1));
		}
		else
		{
			player.stopSkillEffects(4578);
			npc.setTarget(player);
			npc.doCast(SkillTable.getInstance().getInfo(4578, 1));
		}
	}
	
	public static void main(String[] args)
	{
		new PrisonGuards(-1,"PrisonGuards","ai");
	}
}
