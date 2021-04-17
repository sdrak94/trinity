package luna.custom.handler.commands;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class UserGem implements IUserCommandHandler
{

	private static final int[] COMMAND_IDS =
	{
	 	126
	};
	@SuppressWarnings("static-access")
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if (id != COMMAND_IDS[0])
			return false;
		
		Gem.onBypass(activeChar, "gemmain");
		return true;
	}
	
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}