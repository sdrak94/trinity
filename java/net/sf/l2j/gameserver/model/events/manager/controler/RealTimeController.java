package net.sf.l2j.gameserver.model.events.manager.controler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import net.sf.l2j.gameserver.ThreadPoolManager;

public class RealTimeController
{
	private static final HashSet<ITimeTrigger>	hooks				= new HashSet<>();
	private static final SimpleDateFormat		sdf					= new SimpleDateFormat("HH:mm");
	private static int							timeDiffFromMachine	= 0; //1 hour = 1 * 60 * 60 * 1000
	
	public static void load()
	{
		final int schedAfter = 60000 - Calendar.getInstance().get(Calendar.SECOND) * 1000;
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
		{
			final Date date = new Date(System.currentTimeMillis() + timeDiffFromMachine);
			final String timeStr = sdf.format(date);
			for (ITimeTrigger hook : hooks)
				ThreadPoolManager.getInstance().scheduleGeneral(() -> hook.notify(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), timeStr), 0);
		}, schedAfter, 60000);
	}
	
	public static void registerHook(ITimeTrigger hook)
	{
		hooks.add(hook);
	}
	
	public static void rmHook(ITimeTrigger hook)
	{
		hooks.remove(hook);
	}
}
