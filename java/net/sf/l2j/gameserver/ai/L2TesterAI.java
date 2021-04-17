package net.sf.l2j.gameserver.ai;

import net.sf.l2j.gameserver.model.actor.L2Character;

public class L2TesterAI extends L2AttackableAI implements Runnable
{
	public L2TesterAI(L2Character.AIAccessor accessor)
	{
		super(accessor);
	}
	
	@Override
	public void run()
	{
	}
	
	@Override
	public void startAITask()
	{
		
	}
	
	@Override
	public void stopAITask()
	{
		
	}
	
	@Override
	protected void onEvtDead()
	{
	}
	
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
	}
	
	@Override
	protected void onIntentionAttack(L2Character target)
	{
	}
	
	@Override
	protected void onEvtThink()
	{
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
	}
	
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
	}
	
	@Override
	protected void onIntentionActive()
	{
	}
	
	@Override
	public void setGlobalAggro(int value)
	{
	}
}