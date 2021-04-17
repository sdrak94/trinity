package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.util.Rnd;

public class ExtractIt implements IItemHandler
{
	private static final int	_gemIds[]		=
												{
		50200,
		50201,
		50204,
		50207,
		50208,
		50209
												};
	private static final int	_enchantChance	= 66;
	
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();
		final long itemCount = item.getCount();
		if (itemCount > 0)
		{
			if (itemId == 99900)
			{
				final L2ItemInstance weap1 = ItemTable.getInstance().createItem("weap", Rnd.get(80000, 80017), 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(84, 3, weap1.getItem().getBodyPart(), false);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem();
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random rare weapon!");
			}
			else if (itemId == 99901)
			{
				final L2ItemInstance weap2 = ItemTable.getInstance().createItem("weap", Rnd.get(81001, 81017), 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(84, 3, weap2.getItem().getBodyPart(), false);
				weap2.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = enchantItem();
				weap2.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap2);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap2, activeChar, null);
				activeChar.sendMessage("Congratulations! You just earned a random rare weapon");
			}
			else if (itemId == 99914)
			{
				final L2ItemInstance gems = ItemTable.getInstance().createItem("gems", _gemIds[Rnd.get(0, 5)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", gems, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(gems);
				activeChar.sendPacket(iu);
				activeChar.sendMessage("Congratulations! You just earned a random rare gemstone!");
			}
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private int enchantItem()
	{
		int i = 3;
		for (; i <= 17; i++)
		{
			if (Rnd.get(100) >= _enchantChance)
			{
				break;
			}
		}
		return i;
	}
}
