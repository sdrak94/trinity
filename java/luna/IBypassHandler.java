package luna;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public interface IBypassHandler
{
	public boolean handleBypass(final L2PcInstance player, final String cmd);
	
	public default void exception(final Exception e)
	{
		
	}
}