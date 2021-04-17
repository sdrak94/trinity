package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ChooseInventoryItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class EnchantScrolls implements IItemHandler
{
public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
{
	if (!(playable instanceof L2PcInstance))
		return;
	final L2PcInstance activeChar = (L2PcInstance) playable;
	
	if (activeChar.isCastingNow())
		return;
	
	if (activeChar.isProcessingTransaction())
		return;
	
	if (activeChar.isEnchanting() || activeChar.getActiveEnchantItem() != null)
		return;
	
	activeChar.setActiveEnchantItem(item);
	activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_ITEM_TO_ENCHANT));
	activeChar.sendPacket(new ChooseInventoryItem(item.getItemId()));
	
	//activeChar.setActiveEnchantItem(item);
	//activeChar.sendPacket(new ExChooseInventoryAttributeItem(item.getItemId()));
}
}
