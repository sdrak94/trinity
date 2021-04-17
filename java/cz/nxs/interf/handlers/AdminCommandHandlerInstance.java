package cz.nxs.interf.handlers;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.handler.NexusAdminCommand;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author hNoke
 *
 */
public abstract class AdminCommandHandlerInstance implements IAdminCommandHandler, NexusAdminCommand
{
	@Override
	public abstract boolean useAdminCommand(String command, PlayerEventInfo pi);

	@Override
	public final boolean useAdminCommand(String command, L2PcInstance player)
	{
		return useAdminCommand(command, player.getEventInfo());
	}
}
