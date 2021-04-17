package luna.custom.handler.items.capsuledItems;

import java.util.List;
import java.util.logging.Logger;

import luna.custom.handler.items.capsuledItems.CapsuleData.CapsuledData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.util.Rnd;

public class Capsule implements IItemHandler
{
	Logger _log = Logger.getLogger(ItemTable.class.getName());
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		CapsuleData.getInstance();
		final L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		if (CapsuleData.getInstance().getCapsule(itemId) == null)
		{
			System.out.println("Capsule Id " + itemId + " doesn't exist in Capsules.xml");
		}
		else
		{
			if (activeChar.destroyItemByItemId("Capsule", itemId, 1, activeChar, true))
			{
				List<CapsuledData> dat = CapsuleData.getInstance().getCapsule(itemId);
				int CapsuledItemsAmmount = dat.size();
				for (int i = 0; i < CapsuledItemsAmmount; i++)
				{
					int enchant = 0;
					int chance = dat.get(i).getChance();
					if (chance != 100)
					{
						int result = Rnd.get(100);
						if (result > chance)
						{
							// activeChar.sendMessage("[FAILURE] You rolled "+ result + "/" + chance);
							continue;
						}
					}
					if (dat.get(i).getProEnch())
					{
						enchant = dat.get(i).getProRndEnc();
					}
					else
					{
						enchant = dat.get(i).getRndEnc();
					}
					activeChar.addItem("CapsuleReward", dat.get(i).getItemId(), dat.get(i).getAmount(), activeChar, true, enchant);
					InventoryUpdate iu = new InventoryUpdate();
					activeChar.sendPacket(iu);
					activeChar.getInventory().updateDatabase();

					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						activeChar.getCounters().capsulesOpened++;
					}
				}
			}
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
