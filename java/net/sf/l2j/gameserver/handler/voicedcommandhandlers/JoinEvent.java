
package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.AutomatedTvT;

/**
 * @author savormix
 *
 */
public class JoinEvent implements IVoicedCommandHandler
{
	private static final String[] CMDS = {
		"jointvt", "joinTvT", "joinTVT", "JOINTVT", "leavetvt", "leaveTvT", "leaveTVT", "LEAVETVT"
	};

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (command.equals(CMDS[0]) || command.equals(CMDS[1]) || command.equals(CMDS[2]) || command.equals(CMDS[3]))
		{
			AutomatedTvT.getInstance().registerPlayer(activeChar);
			return true;
		}
		else if (Config.AUTO_TVT_REGISTER_CANCEL)
		{
			AutomatedTvT.getInstance().cancelRegistration(activeChar);
			return true;
		}
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	@Override
	public String[] getVoicedCommandList()
	{
		return CMDS;
	}
}
