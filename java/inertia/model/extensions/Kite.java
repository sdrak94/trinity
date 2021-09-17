package inertia.model.extensions;

import inertia.model.behave.AbstractBehave;
import net.sf.l2j.gameserver.GeoEngine;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

public class Kite extends AbstractBehave
{
	@Override
	public void onThinkStart()
	{
	}
	
	@Override
	public void onThinkEnd()
	{

		L2PcInstance player = _autoChill.getActivePlayer();
		if (player == null)
			return;
		L2Character attackTarget = (L2Character) player.getTarget();
		if (attackTarget == null)
			return;
		
		int actorCollision = (int) player.getCollisionRadius();
		int targetCollison = (int) attackTarget.getTemplate().getCollisionRadius();
		int combinedCollision = actorCollision + targetCollison;
		final double dist = Math.sqrt(player.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));
		
		
		if (!player.isMovementDisabled() && dist <= 300)
		{
			int posX;
			int posY;
			int posZ;
			posX = player.getX();
			posY = player.getY();
			posZ = player.getZ() + 30;
			if (attackTarget.getX() < posX)
				posX += 300;
			else
				posX -= 300;
			if (attackTarget.getY() > posY)
				posY += Rnd.get(100,300);
			else
				posY -= Rnd.get(100,300);
			
			Location moveLoc = GeoEngine.getInstance().getValidLocation(player.getX(), player.getY(), player.getZ(), posX, posY, posZ, player.getInstanceId());
			
			
			if(!GeoEngine.getInstance().canMoveFromToTarget(player.getX(), player.getY(), player.getZ(), posX, posY, posZ, player.getInstanceId()))
			{
				posX -= 600;
				posY -= 600;
				moveLoc = GeoEngine.getInstance().getValidLocation(player.getX(), player.getY(), player.getZ(), posX, posY, posZ, player.getInstanceId());
			}
			L2CharPosition finalloc = new L2CharPosition(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ(), Rnd.get(1, 6000));
			
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, finalloc);
			//Broadcast.toAllOnlinePlayers(player.getName() + " Im Kiting");
		}
	}
	
	@Override
	public void onDeath(L2Character killer)
	{}
	
	@Override
	public void onKill(L2Character victim)
	{}
	
	@Override
	public void onAttack(L2Character target)
	{}
	
	@Override
	public void onSkillCast(L2Skill skill)
	{
		_autoChill.addLag(3000);
	}
	
	@Override
	public void onNewTarget(L2Character oldTarget, L2Character newTarget)
	{}
	
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
	{}
	
	@Override
	public void whileTargetDead()
	{}
	
	@Override
	public void onCreditsEnd()
	{}
	
	@Override
	public float lagMultiplier()
	{
		return 1f;
	}
	
	@Override
	public void onUntarget()
	{}
	
	@Override
	public void onFollowClose(L2PcInstance assistPlayer)
	{}
	
	@Override
	public void onFollowFar(L2PcInstance assistPlayer)
	{}
	
	@Override
	public void onAssistNoTarget(L2PcInstance assistPlayer)
	{}
	
	@Override
	public void onStartAutoAttack(L2Character actualTarget)
	{}
}
