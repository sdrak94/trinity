package net.sf.l2j.gameserver.model.events.dataTables;

public class FuturedEvent
{
	private String _eventId;
	private final String _time;
	private final int _day;
	
	public FuturedEvent(String eventId, String time, int day)
	{
		_eventId = eventId;
		_time = time;
		_day = day;
	}
	
	public String getEventId()
	{
		return _eventId;
	}
	
	public void setEventId(String newEventId)
	{
		_eventId = newEventId;
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
		return _time + " " + _eventId;
	}
}