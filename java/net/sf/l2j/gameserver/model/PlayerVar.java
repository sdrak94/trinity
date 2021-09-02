package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class PlayerVar
{
	private L2PcInstance owner;
	private String name;
	private String value;
	private long expire_time;

	@SuppressWarnings("rawtypes")
	private ScheduledFuture task;

	public PlayerVar(L2PcInstance owner, String name, String value, long expire_time)
	{
		this.owner = owner;
		this.name = name;
		this.value = value;
		this.expire_time = expire_time;

		if (expire_time > 0) // if expires schedule expiration
		{
			task = ThreadPoolManager.getInstance().schedule(new PlayerVarExpireTask(this), expire_time - System.currentTimeMillis());
		}
	}

	public String getName()
	{
		return name;
	}

	public L2PcInstance getOwner()
	{
		return owner;
	}

	public boolean hasExpired()
	{
		return task == null || task.isDone();
	}

	public long getTimeToExpire()
	{
		return expire_time - System.currentTimeMillis();
	}

	/**
	 *
	 * @return ?????????? ???????? ??????????
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 *
	 * @return ?????????? ???????? ? ???? ?????????? ??????????
	 */
	public boolean getValueBoolean()
	{
		return value.equals("1") || value.equalsIgnoreCase("true");
	}

	public void setValue(String val)
	{
		value = val;
	}

	public void stopExpireTask()
	{
		if (task != null && !task.isDone())
		{
			task.cancel(true);
		}
	}

	private static class PlayerVarExpireTask implements Runnable
	{
		private PlayerVar _pv;

		private PlayerVarExpireTask(PlayerVar pv)
		{
			_pv = pv;
		}

		@Override
		public void run()
		{
			L2PcInstance pc = _pv.getOwner();
			if (pc == null)
			{
				return;
			}

			pc.unsetVar(_pv.getName());
			
			onUnsetVar(_pv);
		}
		
		private static void onUnsetVar(PlayerVar var)
		{
			switch (var.getName())
			{
				case "Para":
					if (var.getOwner() instanceof L2Playable)
						var.getOwner().getActingPlayer().unsetVar("Para");
					break;
			}
		}
	}
}