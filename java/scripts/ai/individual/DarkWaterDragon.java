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


import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.ai.groupTemplates.L2AttackableAIScript;

public class DarkWaterDragon extends L2AttackableAIScript
{
    private static final int DRAGON = 22267;
    private static final int SHADE1 = 22268;
    private static final int SHADE2 = 22269;
    private static final int FAFURION = 18482;
    private static final int DETRACTOR1 = 22270;
    private static final int DETRACTOR2 = 22271;
    private static int _HasSpawned1; //If true, first Shades were already spawned
    private static FastSet<Integer> secondSpawn = new FastSet<Integer>(); //Used to track if second Shades were already spawned
    private static FastSet<Integer> myTrackingSet = new FastSet<Integer>(); //Used to track instances of npcs
    private static FastMap<Integer, L2PcInstance> _idmap = new FastMap<Integer, L2PcInstance>(); //Used to track instances of npcs
    
    public DarkWaterDragon(int id, String name, String descr)
    {
        super(id,name,descr);
        int[] mobs = {DRAGON, SHADE1, SHADE2, FAFURION, DETRACTOR1, DETRACTOR2};
        this.registerMobs(mobs);
        _HasSpawned1 = 0;
        myTrackingSet.clear();
        secondSpawn.clear();
    }
    public String onAdvEvent (String event, L2Npc npc, L2PcInstance player)
    {
        if (npc != null)
        {
            if (event.equalsIgnoreCase("first_spawn")) //timer to start timer "1"
            {
                this.startQuestTimer("1",40000, npc, null, true); //spawns detractor every 40 seconds
            }
            if (event.equalsIgnoreCase("second_spawn")) //timer to start timer "2"
            {
                this.startQuestTimer("2",40000, npc, null, true); //spawns detractor every 40 seconds
            }
            if (event.equalsIgnoreCase("third_spawn")) //timer to start timer "3"
            {
                this.startQuestTimer("3",40000, npc, null, true); //spawns detractor every 40 seconds
            }
            if (event.equalsIgnoreCase("fourth_spawn")) //timer to start timer "4"
            {
                this.startQuestTimer("4",40000, npc, null, true); //spawns detractor every 40 seconds
            }
            if (event.equalsIgnoreCase("1")) //spawns a detractor
            {
                 this.addSpawn(DETRACTOR1,(npc.getX()+100),(npc.getY()+100),npc.getZ(),0,false,40000);
            }
            if (event.equalsIgnoreCase("2")) //spawns a detractor
            {
                 this.addSpawn(DETRACTOR2,(npc.getX()+100),(npc.getY()-100),npc.getZ(),0,false,40000);
            }
            if (event.equalsIgnoreCase("3")) //spawns a detractor
            {
                this.addSpawn(DETRACTOR1,(npc.getX()-100),(npc.getY()+100),npc.getZ(),0,false,40000);
            }
            if (event.equalsIgnoreCase("4")) //spawns a detractor
            {
                 this.addSpawn(DETRACTOR2,(npc.getX()-100),(npc.getY()-100),npc.getZ(),0,false,40000);
            }
            if (event.equalsIgnoreCase("fafurion_despawn"))    //Fafurion Kindred disappears and drops reward
            {
                this.cancelQuestTimer("fafurion_poison", npc, null);
                this.cancelQuestTimer("1", npc, null);
                this.cancelQuestTimer("2", npc, null);
                this.cancelQuestTimer("3", npc, null);
                this.cancelQuestTimer("4", npc, null);
                
                L2Attackable temp = (L2Attackable) npc;
                player = _idmap.get(temp.getObjectId());
                if(player!=null) //You never know ...
                {
                    temp.doItemDrop(NpcTable.getInstance().getTemplate(18485), player);
                    _idmap.remove(temp.getObjectId());
                    temp.deleteMe();
                    myTrackingSet.remove(temp.getObjectId());
                }
            }
            if (event.equalsIgnoreCase("fafurion_poison"))    //Reduces Fafurions hp like it is poisoned
            {
                if (npc.getCurrentHp() < 500)
                {
                    this.cancelQuestTimer("fafurion_despawn", npc, null);
                    this.cancelQuestTimer("1", npc, null);
                    this.cancelQuestTimer("2", npc, null);
                    this.cancelQuestTimer("3", npc, null);
                    this.cancelQuestTimer("4", npc, null);
                    this.cancelQuestTimer("first_spawn", npc, null);
                    this.cancelQuestTimer("second_spawn", npc, null);
                    this.cancelQuestTimer("third_spawn", npc, null);
                    this.cancelQuestTimer("fourth_spawn", npc, null);
                    myTrackingSet.remove(npc.getObjectId());
                }
            	npc.reduceCurrentHp(500, npc, null); //poison kills Fafurion if he is not healed
            }
        }
        return super.onAdvEvent(event,npc,player);
    }
    
    public String onAttack (L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
    {    
        int npcId = npc.getNpcId();
        int npcObjId = npc.getObjectId();
        if (npcId == DRAGON)
        {
            if (!myTrackingSet.contains(npcObjId)) //this allows to handle multiple instances of npc
                {
                    myTrackingSet.add(npcObjId);
                    _HasSpawned1 = npcObjId;
                }
                if ((_HasSpawned1==npcObjId))
                {
                    //Spawn first 5 shades on first attack on Dark Water Dragon
                    int x = npc.getX();
                    int y = npc.getY();
                    FastList<L2Attackable> _Shades = new FastList<L2Attackable>();
                    _Shades.add((L2Attackable) addSpawn(SHADE1,x+100,y+100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) addSpawn(SHADE2,x+100,y-100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) addSpawn(SHADE1,x-100,y+100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) addSpawn(SHADE2,x-100,y-100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) addSpawn(SHADE1,x-150,y+150,npc.getZ(),0,false,0));
                    _HasSpawned1 = 0;
                    L2Character originalAttacker = isPet? attacker.getPet(): attacker;
                    for (int i=0;i<_Shades.size();i++) //Shades attack the attacker
                    {
                        L2Attackable Shade = _Shades.get(i);
                        Shade.setRunning();
                        Shade.addDamageHate(originalAttacker,999);
                        Shade.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
                    }
                    _Shades.clear();
                }
                if (npc.getCurrentHp() < (npc.getMaxHp() / 2) && !(secondSpawn.contains(npcObjId)))
                {
                    //Spawn second 5 shades on half hp of on Dark Water Dragon
                    int x = npc.getX();
                    int y = npc.getY();
                    FastList<L2Attackable> _Shades = new FastList<L2Attackable>();
                    _Shades.add((L2Attackable) this.addSpawn(SHADE2,x+100,y+100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) this.addSpawn(SHADE1,x+100,y-100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) this.addSpawn(SHADE2,x-100,y+100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) this.addSpawn(SHADE1,x-100,y-100,npc.getZ(),0,false,0));
                    _Shades.add((L2Attackable) this.addSpawn(SHADE2,x-150,y+150,npc.getZ(),0,false,0));
                    secondSpawn.add(npcObjId);
                    L2Character originalAttacker = isPet? attacker.getPet(): attacker;
                    for (int i=0;i<_Shades.size();i++) //Shades attack the attacker
                    {
                        L2Attackable Shade = _Shades.get(i);
                        Shade.setRunning();
                        Shade.addDamageHate(originalAttacker,999);
                        Shade.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
                    }
                    _Shades.clear();
                }
            }
        return super.onAttack(npc, attacker, damage, isPet);
    }
    public String onKill (L2Npc npc, L2PcInstance killer, boolean isPet) 
    { 
        int npcId = npc.getNpcId();
        int npcObjId = npc.getObjectId();
        if (npcId == DRAGON)
        {
            myTrackingSet.remove(npcObjId);
            secondSpawn.remove(npcObjId);
            L2Attackable faf = (L2Attackable) this.addSpawn(FAFURION,npc.getX(),npc.getY(),npc.getZ(),0,false,0); //spawns Fafurion Kindred when Dard Water Dragon is dead
            _idmap.put(faf.getObjectId(),killer);
        }
        if (npcId == FAFURION)
        {
            this.cancelQuestTimer("fafurion_poison", npc, null);
            this.cancelQuestTimer("fafurion_despawn", npc, null);
            this.cancelQuestTimer("1", npc, null);
            this.cancelQuestTimer("2", npc, null);
            this.cancelQuestTimer("3", npc, null);
            this.cancelQuestTimer("4", npc, null);
            this.cancelQuestTimer("first_spawn", npc, null);
            this.cancelQuestTimer("second_spawn", npc, null);
            this.cancelQuestTimer("third_spawn", npc, null);
            this.cancelQuestTimer("fourth_spawn", npc, null);
            myTrackingSet.remove(npcObjId);
            _idmap.remove(npcObjId);
        }
        return super.onKill(npc,killer,isPet);
    }
    
    public String onSpawn (L2Npc npc) 
    {
        int npcId = npc.getNpcId();
        int npcObjId = npc.getObjectId();
        if (npcId == FAFURION)
        {
            if (!myTrackingSet.contains(npcObjId))
            {
                //Spawn 4 Detractors on spawn of Fafurion
                int x = npc.getX();
                int y = npc.getY();            
                this.addSpawn(DETRACTOR2,x+100,y+100,npc.getZ(),0,false,40000);
                this.addSpawn(DETRACTOR1,x+100,y-100,npc.getZ(),0,false,40000);
                this.addSpawn(DETRACTOR2,x-100,y+100,npc.getZ(),0,false,40000);
                this.addSpawn(DETRACTOR1,x-100,y-100,npc.getZ(),0,false,40000);
                myTrackingSet.add(npcObjId);
                this.startQuestTimer("first_spawn",2000, npc, null); //timer to delay timer "1" 
                this.startQuestTimer("second_spawn",4000, npc, null); //timer to delay timer "2" 
                this.startQuestTimer("third_spawn",8000, npc, null); //timer to delay timer "3" 
                this.startQuestTimer("fourth_spawn",10000, npc, null); //timer to delay timer "4" 
                this.startQuestTimer("fafurion_poison",3000, npc, null, true); //Every three seconds reduces Fafurions hp like it is poisoned
                this.startQuestTimer("fafurion_despawn",300000, npc, null); //Fafurion Kindred disappears after two minutes
            }
        }
        return super.onSpawn(npc);
    }
    public static void main(String[] args)
    {
        // Quest class and state definition
        new DarkWaterDragon(-1,"DarkWaterDragon","ai");
    }
}