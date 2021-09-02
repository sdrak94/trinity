package inertia.model.extensions;

import java.util.ArrayList;

import inertia.model.behave.AbstractBehave;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

public class MoveArround extends AbstractBehave
{

	@Override
	public void onThinkStart()
	{
		L2PcInstance player = _autoChill.getActivePlayer();
		
		//Move Arround
		if (Rnd.get(100) <= 70)
		{
			ArrayList<L2Character> close = new ArrayList<>();
			close.addAll(player.getKnownList().getKnownType(L2Character.class));
			
			for (L2Character nearby : close)
			{
				int actorCollision = (int) player.getCollisionRadius();
				int targetCollison = (int) nearby.getTemplate().getCollisionRadius();
				
				int combinedCollision = actorCollision + targetCollison;
				
				L2Character attackTarget = (L2Character) player.getTarget();
				
				if (attackTarget == null)
					return;
				
				if (player.isInsideRadius(nearby, actorCollision, false, false) && nearby != attackTarget)
				{
					int newX = combinedCollision + Rnd.get(-300, 300);
					if (Rnd.nextBoolean())
						newX = attackTarget.getX() + newX;
					else
						newX = attackTarget.getX() - newX;
					
					int newY = combinedCollision + Rnd.get(-300, 300);
					
					if (Rnd.nextBoolean())
						newY = attackTarget.getY() + newY;
					else
						newY = attackTarget.getY() - newY;
					
					if (!player.isInsideRadius(newX, newY, actorCollision, false))
					{
						int newZ = player.getZ() + 30;
						final Location moveLoc = GeoData.getInstance().getValidLocation(player.getX(), player.getY(), player.getZ(), newX, newY, newZ, player.getInstanceId());
						
						L2CharPosition finalloc = new L2CharPosition(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ(), Rnd.get(1, 2000));
						
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, finalloc);

					}
					return;
				}
			}
		}
	}

	@Override
	public void onThinkEnd()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeath(L2Character killer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onKill(L2Character victim)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAttack(L2Character target)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSkillCast(L2Skill skill)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNewTarget(L2Character oldTarget, L2Character newTarget)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean filterSkill(L2Skill skill)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean filterTarget(L2Character target)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void whileDead()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void whileTargetDead()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreditsEnd()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public float lagMultiplier()
	{
		// TODO Auto-generated method stub
		return 1f;
	}

	@Override
	public void onUntarget()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFollowClose(L2PcInstance assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFollowFar(L2PcInstance assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAssistNoTarget(L2PcInstance assistPlayer)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStartAutoAttack(L2Character actualTarget)
	{
		// TODO Auto-generated method stub
		
	}}
