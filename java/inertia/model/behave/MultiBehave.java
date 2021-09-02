package inertia.model.behave;

import java.util.ArrayList;

import inertia.model.IInertiaBehave;
import inertia.model.Inertia;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class MultiBehave extends AbstractBehave
{
	private final ArrayList<IInertiaBehave> _extensions = new ArrayList<>();
	
	@Override
	public void expand(IInertiaBehave behave)
	{
		_extensions.add(behave);
	}
	
	public void onThinkStart()
	{
		_extensions.forEach((ext) ->
		{
			ext.onThinkStart();
		});
	}

	public void onThinkEnd()
	{
		_extensions.forEach((ext) ->
		{
			ext.onThinkEnd();
		});
	}
	
	public void onDeath(final L2Character killer)
	{
		_extensions.forEach((ext) ->
		{
			ext.onDeath(killer);
		});
	}
	
	public void onKill(final L2Character victim)
	{
		_extensions.forEach((ext) ->
		{
			ext.onKill(victim);
		});
	}
	
	public void onAttack(final L2Character target)
	{
		_extensions.forEach((ext) ->
		{
			ext.onAttack(target);
		});
	}
	
	public void onSkillCast(final L2Skill skill)
	{
		_extensions.forEach((ext) ->
		{
			ext.onSkillCast(skill);
		});
	}
	
	public void onNewTarget(final L2Character oldTarget, final L2Character newTarget)
	{
		_extensions.forEach((ext) ->
		{
			ext.onNewTarget(oldTarget, newTarget);
		});
	}
	
	public boolean filterSkill(final L2Skill skill)
	{
		for (final var ext : _extensions)
			if (!ext.filterSkill(skill))
				return false;
		return true;
	}
	
	public boolean filterTarget(final L2Character target)
	{
		for (final var ext : _extensions)
			if (!ext.filterTarget(target))
				return false;
		return true;
	}

	@Override
	public void whileDead()
	{
		_extensions.forEach((ext) ->
		{
			ext.whileDead();
		});
	}

	@Override
	public void onCreditsEnd()
	{
		_extensions.forEach((ext) ->
		{
			ext.onCreditsEnd();
		});
	}

	@Override
	public float lagMultiplier()
	{
		return 1f;
	}

	@Override
	public void onUntarget()
	{
		_extensions.forEach((ext) ->
		{
			ext.onUntarget();
		});
	}

	@Override
	public void whileTargetDead()
	{
		_extensions.forEach((ext) ->
		{
			ext.whileTargetDead();
		});
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
		_extensions.forEach((ext) ->
		{
			ext.onStartAutoAttack(actualTarget);
		});
	}
	
	@Override
	public void setAutoChill(Inertia autoChill)
	{
		super.setAutoChill(autoChill);
		for (final var ext : _extensions)
			ext.setAutoChill(autoChill);
	}

}
