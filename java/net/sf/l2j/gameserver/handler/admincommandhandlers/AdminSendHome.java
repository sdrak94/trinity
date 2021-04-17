
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public class AdminSendHome implements IAdminCommandHandler
{
	private static final String[]	ADMIN_COMMANDS	= { "admin_sendhome" };
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_sendhome"))
		{
			if (command.split(" ").length > 1)
				handleSendhome(activeChar, command.split(" ")[1]);
			else
				handleSendhome(activeChar);
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleSendhome(L2PcInstance activeChar)
	{
		handleSendhome(activeChar, null);
	}


	private void handleSendhome(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();

		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);

			if (plyr != null)
			{
				obj = plyr;
			}
		}

		if (obj == null)
			obj = activeChar;

		if (obj instanceof L2Character)
			doSendhome((L2Character) obj);
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}

	private void doSendhome(L2Character targetChar)
	{
		targetChar.teleToLocation(TeleportWhereType.Town);
	}
}