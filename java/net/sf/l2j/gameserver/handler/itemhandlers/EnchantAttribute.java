package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExChooseInventoryAttributeItem;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class EnchantAttribute implements IItemHandler
{
public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
{
	if (!(playable instanceof L2PcInstance))
		return;
	
	final L2PcInstance activeChar = (L2PcInstance) playable;
	
	if (activeChar.isCastingNow())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	if (activeChar.isProcessingTransaction())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	if (activeChar.getActiveEnchantItem() != null)
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	InventoryUpdate iu = new InventoryUpdate();
	boolean vesper = false;
	
	for (L2ItemInstance itam : activeChar.getInventory().getItems())
	{
//		if ((itam.isJewelry() && itam.getUniqueness() >= 3 && itam.getName().contains("Vesper")) || itam.getItemId() == 20325 || itam.getItemId() == 70002)
//		{
//			iu.addModifiedItem(itam);
//			vesper = true;
//		}
		iu.addModifiedItem(itam);
		vesper = true;
	}
	
	if (vesper)
	{
		iu.setIsForVesper(true);
		activeChar.sendPacket(iu);
	}
	
	activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_ITEM_TO_ADD_ELEMENTAL_POWER));
	activeChar.setActiveEnchantAttrItem(item);
	activeChar.sendPacket(new ExChooseInventoryAttributeItem(item.getItemId()));
}
}
