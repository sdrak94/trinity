package inertia.model.behave;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;

public class GhostBehave extends PlayerBehave
{
	@Override
	public void whileDead()
	{
		final var player = _autoChill.getActivePlayer();
		if (player.getInstanceId() == 0)
		{
			Location location = MapRegionTable.getInstance().getTeleToLocation(player, TeleportWhereType.Town);
			if (player.isDead())
				player.doRevive();
			player.teleToLocation(location.getX(), location.getY(), location.getZ());
		}
	}
	
	@Override
	public void onThinkEnd()
	{
		super.onThinkEnd();
		var player = getAutoChill().getActivePlayer();
		if (player == null)
			return;

		if (player.getActiveWeaponInstance() == null)
		{
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item.isWeapon())
				{
					if (item.isEquipable())
					{
						player.useEquippableItem(item, true, false);
					}
				}
			}
		}
		if (player.getTarget() == null || player.isMoving())
			return;
		if (_autoChill.getTargetByRange(5000) == null)
		{
			L2Character cr = _autoChill.getTargetByRange(3000);
			if (cr == null)
				return;
			// if (GeoEngine.getInstance().canSeeTarget(player, cr))
			// {
			// Location midLoc = Util.getMidPoint(player.getLocation(), cr.getLocation());
			// if (GeoEngine.getInstance().canSeeLocation(player, midLoc))
			// {
			// if (Rnd.get(100) < 60)
			// {
			// final Location movto = new Location(midLoc.getX() + Rnd.get(-200, 200), midLoc.getY() + Rnd.get(-200, 200), midLoc.getZ());
			// player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, movto);
			// }
			// }
			// }
		}
	}
	
	@Override
	public void onStartAutoAttack(L2Character actualTarget)
	{
		final var player = _autoChill.getActivePlayer();
		if (player != null && player != actualTarget)
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
	}
	
	@Override
	public float lagMultiplier()
	{
		return 1f;
	}
}
