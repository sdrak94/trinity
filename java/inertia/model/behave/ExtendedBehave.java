package inertia.model.behave;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ExtendedBehave extends AbstractBehave
{

	@Override
	public void onThinkStart()
	{
	}

	@Override
	public void onThinkEnd()
	{
	}

	@Override
	public void onDeath(L2Character killer)
	{
	}

	@Override
	public void onKill(L2Character victim)
	{
	}

	@Override
	public void onAttack(L2Character target)
	{
	}

	@Override
	public void onSkillCast(L2Skill skill)
	{
	}

	@Override
	public void onNewTarget(L2Character oldTarget, L2Character newTarget)
	{
	}

	@Override
	public boolean filterSkill(L2Skill skill)
	{
		return false;
	}

	@Override
	public boolean filterTarget(L2Character target)
	{
		return false;
	}

	@Override
	public void whileDead()
	{
	}

	@Override
	public void whileTargetDead()
	{
	}

	@Override
	public void onCreditsEnd()
	{
	}

	@Override
	public float lagMultiplier()
	{
		return 0;
	}

	@Override
	public void onUntarget()
	{
	}

	@Override
	public void onFollowClose(L2PcInstance assistPlayer)
	{
	}

	@Override
	public void onFollowFar(L2PcInstance assistPlayer)
	{
	}

	@Override
	public void onAssistNoTarget(L2PcInstance assistPlayer)
	{
	}

	@Override
	public void onStartAutoAttack(L2Character actualTarget)
	{
	}
	

}
