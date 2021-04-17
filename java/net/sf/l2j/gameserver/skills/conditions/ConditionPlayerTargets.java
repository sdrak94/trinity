package net.sf.l2j.gameserver.skills.conditions;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerTargets extends Condition
{

	public final int _count;
	
	public ConditionPlayerTargets(int count)
	{
		_count = count;
	}
	 
    @Override
    public boolean testImpl(Env env)
    {	
    	if (env.player.getTarget() instanceof L2MonsterInstance)
    	{
        	List<L2MonsterInstance> targetListMb = new FastList<L2MonsterInstance>();
        	for (L2MonsterInstance objM: env.player.getActingPlayer().getKnownList().getKnownMonsterssInRadius(env.player.getActingPlayer().getPhysicalAttackRange()))
    		{
        		targetListMb.add(objM);
    		}
        	if (targetListMb.size() <= _count)
        	{
        		//Announcements.getInstance().announceToAll("Targets Mobs: "+ String.valueOf(targetListMb.size()));
        		return true;
        	}
    	}
    	else if (env.player.getTarget() instanceof L2PcInstance)
    	{
        	List<L2PcInstance> targetListPl = new FastList<L2PcInstance>();
        	for (L2PcInstance obj : env.player.getActingPlayer().getKnownList().getKnownPlayersInRadius(env.player.getActingPlayer().getPhysicalAttackRange()))
    		{
        		targetListPl.add(obj);
    		}
        	if (targetListPl.size() <= _count)
        	{
        		//Announcements.getInstance().announceToAll("Targets Players: "+ String.valueOf(targetListPl.size()));
        		return true;
        	}
    	}
        return false;
    }

}
