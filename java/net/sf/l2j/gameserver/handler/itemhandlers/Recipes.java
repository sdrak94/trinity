package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Recipes implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2RecipeList rp = RecipeController.getInstance().getRecipeByItemId(item.getItemId());
		if (rp == null)
			return;
		if (activeChar.hasRecipeList(rp.getId()))
			activeChar.sendPacket(new SystemMessage(SystemMessageId.RECIPE_ALREADY_REGISTERED));
		else
		{
					activeChar.registerDwarvenRecipeList(rp, true);
					activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
		}
	}
}
