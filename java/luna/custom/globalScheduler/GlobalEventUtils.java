package luna.custom.globalScheduler;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

public class GlobalEventUtils
{
	static String[] nummeric =
	{
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
	};
	
	public static String prepareString(String msg, L2PcInstance playerDriver)
	{
		return msg;
	}
	
	public static boolean makeEvent(final L2PcInstance caller, final String eventType)
	{
		try
		{
			switch (eventType)
			{
				case "olympiad_start":
					if (!Config.ENABLE_OLD_OLY)
						Olympiad.getInstance().openOly();
					break;
				case "olympiad_end":
					if (!Config.ENABLE_OLD_OLY)
						Olympiad.getInstance().closeOly();
					break;
				case "pagan_open":
					GlobalEventVariablesHolder.getInstance().setPagansMode(true);
					break;
				case "pagan_close":
					GlobalEventVariablesHolder.getInstance().setPagansMode(false);
					break;
			}
			// System.out.println("Schedule Event here.");
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
}
