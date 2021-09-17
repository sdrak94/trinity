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
package ai.groupTemplates;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.util.Rnd;

public class SummonMinions extends L2AttackableAIScript
{
	private static int HasSpawned;
	private static FastSet<Integer> myTrackingSet = new FastSet<Integer>(); //Used to track instances of npcs
	private FastMap<Integer, FastList<L2PcInstance>> _attackersList = new FastMap<Integer, FastList<L2PcInstance>>().setShared(true);
	private static final FastMap<Integer, Integer[]> MINIONS = new FastMap<Integer, Integer[]>();
	
	static
    {
            MINIONS.put(20767,new Integer[]{20768,20769,20770}); //Timak Orc Troop
            //MINIONS.put(22030,new Integer[]{22045,22047,22048}); //Ragna Orc Shaman
            //MINIONS.put(22032,new Integer[]{22036}); //Ragna Orc Warrior - summons shaman but not 22030 ><
            //MINIONS.put(22038,new Integer[]{22037}); //Ragna Orc Hero
            MINIONS.put(21524,new Integer[]{21525}); //Blade of Splendor
            MINIONS.put(21531,new Integer[]{21658}); //Punishment of Splendor
            MINIONS.put(21539,new Integer[]{21540}); //Wailing of Splendor
            //MINIONS.put(22257,new Integer[]{18364,18364}); //Island Guardian
           // MINIONS.put(22258,new Integer[]{18364,18364}); //White Sand Mirage
            //MINIONS.put(22259,new Integer[]{18364,18364}); //Muddy Coral
           // MINIONS.put(22260,new Integer[]{18364,18364}); //Kleopora
           // MINIONS.put(22261,new Integer[]{18365,18365}); //Seychelles
           // MINIONS.put(22262,new Integer[]{18365,18365}); //Naiad
           // MINIONS.put(22263,new Integer[]{18365,18365}); //Sonneratia
           // MINIONS.put(22264,new Integer[]{18366,18366}); //Castalia
           // MINIONS.put(22265,new Integer[]{18366,18366}); //Chrysocolla
           // MINIONS.put(22266,new Integer[]{18366,18366}); //Pythia
    }
	
	public SummonMinions(int questId, String name, String descr)
	{
		super(questId, name, descr);
		int[] temp =
		{
			20767,  21524, 21531, 21539, 22257, 22258, 22259, 22260, 22261, 22262, 22263, 22264, 22265, 22266
		};
		this.registerMobs(temp);
	}
	
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		int npcObjId = npc.getObjectId();
		if (MINIONS.containsKey(npcId))
		{
			if (!myTrackingSet.contains(npcObjId)) //this allows to handle multiple instances of npc
			{
				myTrackingSet.add(npcObjId);
				HasSpawned = npcObjId;
			}
			if (HasSpawned == npcObjId)
			{
				switch (npcId)
				{
					case 22030: //mobs that summon minions only on certain hp
					case 22032:
					case 22038:
					{
						if (npc.getCurrentHp() < (npc.getMaxHp() / 2))
						{
							HasSpawned = 0;
							if (Rnd.get(100) < 33) //mobs that summon minions only on certain chance
							{
								Integer[] minions = MINIONS.get(npcId);
								for (int i = 0; i < minions.length; i++)
								{
									L2Attackable newNpc = (L2Attackable) this.addSpawn(minions[i], (npc.getX() + Rnd.get(-150, 150)), (npc.getY() + Rnd.get(-150, 150)), npc.getZ(), 0, false, 0);
									newNpc.setRunning();
									newNpc.addDamageHate(attacker, 999);
									newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
								}
								minions = null;
							}
						}
						break;
					}
					case 22257:
					case 22258:
					case 22259:
					case 22260:
					case 22261:
					case 22262:
					case 22263:
					case 22264:
					case 22265:
					case 22266:
					{
						if (isPet)
							attacker = ((L2PcInstance)attacker).getPet().getOwner();
						if (attacker.getParty() != null)
						{
							for (L2PcInstance member : attacker.getParty().getPartyMembers())
							{
								if (_attackersList.get(npcObjId) == null)
								{
									FastList<L2PcInstance> player = new FastList<L2PcInstance>();
									player.add(member);
									_attackersList.put(npcObjId,player);
								}
								else if (!_attackersList.get(npcObjId).contains(member))
									_attackersList.get(npcObjId).add(member);
							}
						}
						else
						{
							if (_attackersList.get(npcObjId) == null)
							{
								FastList<L2PcInstance> player = new FastList<L2PcInstance>();
								player.add(attacker);
								_attackersList.put(npcObjId,player);
							}
							else if (!_attackersList.get(npcObjId).contains(attacker))
								_attackersList.get(npcObjId).add(attacker);
						}
						if (attacker != null && ((attacker.getParty() != null && attacker.getParty().getMemberCount() > 2)||_attackersList.get(npcObjId).size() > 2)) //Just to make sure..
						{
							HasSpawned = 0;
							Integer[] minions = MINIONS.get(npcId);
							for (int i = 0; i < minions.length; i++)
							{
								L2Attackable newNpc = (L2Attackable) this.addSpawn(minions[i], npc.getX() + Rnd.get(-150, 150), npc.getY() + Rnd.get(-150, 150), npc.getZ(), 0, false, 0);
								newNpc.setRunning();
								newNpc.addDamageHate(attacker, 999);
								newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
							}
							minions = null;
						}
						break;
					}
					default: //mobs without special conditions
					{
						HasSpawned = 0;
						Integer[] minions = MINIONS.get(npcId);
						if (npcId != 20767)
						{
							for (int i = 0; i < minions.length; i++)
							{
								L2Attackable newNpc = (L2Attackable) this.addSpawn(minions[i], npc.getX() + Rnd.get(-150, 150), npc.getY() + Rnd.get(-150, 150), npc.getZ(), 0, false, 0);
								newNpc.setRunning();
								newNpc.addDamageHate(attacker, 999);
								newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
							}
						}
						else
						{
							for (int i = 0; i < minions.length; i++)
							{
								this.addSpawn(minions[i], (npc.getX() + Rnd.get(-100, 100)), (npc.getY() + Rnd.get(-100, 100)), npc.getZ(), 0, false, 0);
							}
						}
						minions = null;
						if (npcId == 20767)
						{
							npc.broadcastPacket(new NpcSay(npcObjId, 0, npcId, "Come out, you children of darkness!"));
						}
						break;
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		int npcObjId = npc.getObjectId();
		if (MINIONS.containsKey(npcId))
		{
			myTrackingSet.remove(npcObjId);
		}
		if (_attackersList.get(npcObjId) != null)
		{
			_attackersList.get(npcObjId).clear();
		}
		return super.onKill(npc, killer, isPet);
	}
	
	public static void main(String[] args)
	{
		// now call the constructor (starts up the ai)
		new SummonMinions(-1, "SummonMinions", "ai");
	}
}