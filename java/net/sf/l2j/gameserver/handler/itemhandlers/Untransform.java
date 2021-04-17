package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class Untransform implements Runnable
{

		private final L2PcInstance _player;

		public Untransform(L2PcInstance player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.untransform();
		}
}
