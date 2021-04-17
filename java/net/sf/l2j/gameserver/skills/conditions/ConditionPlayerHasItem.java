package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionPlayerHasItem extends Condition
{
	private final int _itemId;

	public ConditionPlayerHasItem(int itemId)
	{
		_itemId = itemId;
	}

	@Override
	public boolean testImpl(Env env)
	{		
		return env.player != null && env.player instanceof L2PcInstance && (env.player.getInventory().getInventoryItemCount(_itemId, 0) >= 1);
	}
}
