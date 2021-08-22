package luna.custom.globalScheduler;
public interface ITimeTrigger
{
	public void notify(int day, String trigger);

	public void notify(final String dayName, final String timeString);
}