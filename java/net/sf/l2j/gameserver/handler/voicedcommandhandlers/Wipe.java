package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;


public class Wipe implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"wipe",
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
			PcInventory inventory = activeChar.getInventory();
			for (L2ItemInstance item : inventory.getItems())
			{
				if (!(item.getItemId() == 4357) || !(item.getItemId() == 4355) ||!(item.getItemId() == 4356) ||!(item.getItemId() == 6320) ||!(item.getItemId() == 6577) ||!(item.getItemId() == 1571)
						||!(item.getItemId() == 6578) ||!(item.getItemId() == 6392) ||!(item.getItemId() == 6393))
					continue;

				inventory.destroyItem("Destroy Item", item, activeChar, null);
			}

			activeChar.sendPacket(new ItemList(activeChar, false));
			activeChar.sendMessage("Inventory was cleaned up.");
			return true;
		
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}