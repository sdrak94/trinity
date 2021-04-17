package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2GoldenPigAI;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2GoldenPigInstance extends L2Attackable
{
	public L2GoldenPigInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean hasAI()
	{
		return true;
	}
	
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai;
		
		if (ai == null)
		{
			synchronized(this)
			{
				if (_ai == null) _ai = new L2GoldenPigAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return true;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public boolean isAggressive()
	{
		return false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		deleteMe();
		return true;
	}
	
	@Override
	public void deleteMe()
	{
		super.deleteMe();
	}
}