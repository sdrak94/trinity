package luna.util;

public class LunaUtil
{
	public static String formatTime(int time)
	{
		if (time == 0)
			return "now";
		time = Math.abs(time);
		String ret = "";
		long numDays = time / 86400;
		time -= numDays * 86400;
		long numHours = time / 3600;
		time -= numHours * 3600;
		long numMins = time / 60;
		time -= numMins * 60;
		long numSeconds = time;
		if (numDays > 0)
			ret += numDays + "d ";
		if (numHours > 0)
			ret += numHours + "h ";
		if (numMins > 0)
			ret += numMins + "m ";
		if (numSeconds > 0)
			ret += numSeconds + "s";
		return ret.trim();
	}
}
