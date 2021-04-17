package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.AugmentationData.AugStat;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * @author Trance
 */
public class AdminAugment implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_augment"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_augment"))
		{
			final L2PcInstance player = activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance ? (L2PcInstance) activeChar.getTarget() : activeChar;
			final L2ItemInstance item = player.getActiveWeaponInstance();
			if (item != null && item.isAugmented())
			{
				activeChar.sendMessage(player.getName() + " has " + item.getItemName() + " with the following stats:");
				for (AugStat param : AugmentationData.getInstance().getAugStatsById(item.getAugmentation().getAugmentationId()))
					activeChar.sendMessage(param.getStat().getName() + " +" + param.getValue());
			}
			else
				activeChar.sendMessage(player != activeChar ? player.getName() +"'s weapon is not augmented." : "You don't have an augmented weapon.");
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}