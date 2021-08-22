package luna.custom.globalScheduler;

import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.Shutdown.Savable;

public class DelaysController implements ITimeTrigger, Savable
{
	
	private DelaysController()
	{
		RealTimeController.registerHook(this);
		Shutdown.addShutdownHook(this);
	}

	public static class InstanceHolder
	{
		private static final DelaysController _instance = new DelaysController();
	}
	
	public static DelaysController getInstance()
	{
		return InstanceHolder._instance;
	}
	
	@Override
	public void notify(int day, String time)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void store()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void notify(String dayName, String timeString)
	{
		// TODO Auto-generated method stub
		
	}
}