package luna.custom.globalScheduler.template;

public class FuturedGlobalEvent
{
	private String _eventType;
	private final String _time;
	private final int _day;
	
	public FuturedGlobalEvent(String eventType, String time, int day)
	{
		_eventType = eventType;
		_time = time;
		_day = day;
	}
	
	public String getEventType()
	{
		return _eventType;
	}
	
	public void setEventId(String newEventType)
	{
		_eventType = newEventType;
	}
	public String getTime()
	{
		return _time;
	}
	public int getDay()
	{
		return _day;
	}
	@Override
	public String toString()
	{
		return _time + " " + _eventType;
	}
}