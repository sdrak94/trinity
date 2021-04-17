package luna.custom.handler.items.lootBox;

import java.util.logging.Logger;

import luna.custom.handler.items.lootBox.LootData.GainData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public class LootBox implements IItemHandler
{
	Logger _log = Logger.getLogger(ItemTable.class.getName());
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		LootData.getInstance();
		final L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		if (LootData.getInstance().getBox(itemId) == null)
		{
			System.out.println("Box Id " + itemId + " doesn't exist in LootBoxes.xml");
		}
		else
		{
			if (activeChar.destroyItemByItemId("LootBox", itemId, 1, activeChar, true))
			{
				GainData lootItem = LootData.getInstance().getRandomReward(itemId);
				int enchant = 0;
				if (lootItem.getProEnch())
				{
					enchant = lootItem.getProRndEnc();
				}
				else
				{
					enchant = lootItem.getRndEnc();
				}
				activeChar.addItem("LootReward", lootItem.getItemId(), lootItem.getAmount(), activeChar, true, enchant);
				activeChar.getInventory().updateDatabase();

				if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					activeChar.getCounters().lootBoxesOpened++;
				}
			}
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
