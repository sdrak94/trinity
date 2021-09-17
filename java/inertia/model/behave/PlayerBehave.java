package inertia.model.behave;

import ghosts.model.Ghost;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;

public class PlayerBehave extends AbstractBehave
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
	public void onDeath(final L2Character killer)
	{
		
	}
	
	@Override
	public void onKill(final L2Character victim)
	{
		
	}
	
	@Override
	public void onAttack(final L2Character target)
	{
		
	}
	
	@Override
	public void onSkillCast(final L2Skill skill)
	{
		
	}
	
	@Override
	public void onNewTarget(final L2Character oldTarget, final L2Character newTarget)
	{
		final var player = _autoChill.getActivePlayer();
		
		player.setTarget(newTarget);
		player.sendPacket(new MyTargetSelected(player, newTarget));

		final StatusUpdate su = new StatusUpdate(newTarget.getObjectId());
		su.addAttribute(StatusUpdate.MAX_HP, newTarget.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) newTarget.getCurrentHp());
		player.sendPacket(su);

	}
	
	@Override
	public boolean filterSkill(final L2Skill skill)
	{
		return true;
	}
	
	@Override
	public boolean filterTarget(final L2Character target)
	{
		return true;
	}
	
	
	@Override
	public void whileDead()
	{
		_autoChill.setRunning(false);
	}
	
	@Override
	public void onCreditsEnd()
	{
		_autoChill.setRunning(false);
		_autoChill.render();
	}

	@Override
	public float lagMultiplier()
	{
		return 0.2f;
	}
	
	@Override
	public void onUntarget()
	{
		final var player = _autoChill.getActivePlayer();
		
		if (player.isCastingNow())
			player.breakCast();
		if (player.isAttackingNow())
			player.breakAttack();
		player.setTarget(null);
	}

	@Override
	public void whileTargetDead()
	{
		final var player = _autoChill.getActivePlayer();
		
		player.setTarget(null);
	}

	@Override
	public void onFollowClose(final L2PcInstance assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();
	
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, assistPlayer);		
	}

	@Override
	public void onFollowFar(final L2PcInstance assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();

		player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, assistPlayer);
	}
	

	@Override
	public void onAssistNoTarget(final L2PcInstance assistPlayer)
	{
		final var player = _autoChill.getActivePlayer();

		player.setTarget(assistPlayer);
		player.sendPacket(new MyTargetSelected(player, assistPlayer));
	}
	
	@Override
	public void onStartAutoAttack(L2Character actualTarget)
	{
		final var player = _autoChill.getActivePlayer();
		if (!(player instanceof Ghost))
		{
			if (actualTarget instanceof L2Attackable)
			{
				if (player != null && player != actualTarget)
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
			}
		}
		else
		{
			if (player != null && player != actualTarget)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
		}
	}
	
	
}
