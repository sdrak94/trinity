package net.sf.l2j.gameserver.network;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NetPingPacket;

/**
 * @author vGodFather
 */
public class Pinger
{
	public static boolean getPing(L2PcInstance activeChar)
	{
		activeChar.sendMessage("Processing request...");
		activeChar.sendPacket(new NetPingPacket(activeChar));
		ThreadPoolManager.getInstance().scheduleGeneral(new AnswerTask(activeChar), 3000L);
		return true;
	}
	
	private static final class AnswerTask implements Runnable
	{
		private final L2PcInstance _player;
		
		public AnswerTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			int ping = _player.getPing();
			if (ping > -1)
			{
				_player.sendMessage("Current ping: " + ping + " ms.");
			}
			else
			{
				_player.sendMessage("The data from the client was not received.");
			}
			_player.setPing(-1);
		}
	}
}