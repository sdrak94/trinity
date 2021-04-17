package net.sf.l2j.gameserver.model.entity.events;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.entity.events.PortalData.RaidEventData;

public class RaidEventScheduler
{
	private static RaidEventScheduler	_instance	= null;
	protected static final Logger		_log		= Logger.getLogger(RaidEventScheduler.class.getName());
	private Calendar					NextRestart;
	private SimpleDateFormat			format		= new SimpleDateFormat("HH:mm");
	private RaidEventData				nextActiveData;
	
	private void setActiveEventData(RaidEventData red)
	{
		if (red != null)
			nextActiveData = red;
	}
	
	public static RaidEventScheduler getInstance()
	{
		if (_instance == null)
			_instance = new RaidEventScheduler();
		return _instance;
	}
	
	public String getRestartNextTime()
	{
		if (NextRestart.getTime() != null)
			return format.format(NextRestart.getTime());
		return "Error";
	}
	
	private RaidEventScheduler()
	{}
	
	public void StartCalculationOfNextRestartTime()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			List<Calendar> dates = new ArrayList<>();
			for (Map.Entry<Integer, RaidEventData> red : PortalData.getInstance().getPortalData().entrySet())
			{
				Calendar temp = Calendar.getInstance();
				temp.setLenient(true);
				if (red.getValue().getDayOfMonth() != -1)
					temp.set(Calendar.DAY_OF_MONTH, red.getValue().getDayOfMonth());
				if (red.getValue().getDayOfWeek() != -1)
					temp.set(Calendar.DAY_OF_WEEK, red.getValue().getDayOfWeek());
				temp.set(Calendar.HOUR_OF_DAY, red.getValue().getHour());
				temp.set(Calendar.MINUTE, red.getValue().getMinute());
				temp.set(Calendar.SECOND, 00);
				temp.set(Calendar.MILLISECOND, 0);
				while (temp.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					if (red.getValue().getDayOfMonth() != -1)
						while (red.getValue().getDayOfMonth() != temp.get(Calendar.DAY_OF_MONTH))
							temp.add(Calendar.DAY_OF_MONTH, 1);
					else if (red.getValue().getDayOfWeek() != -1)
						temp.add(Calendar.DAY_OF_MONTH, 7);
					else
						temp.add(Calendar.DAY_OF_MONTH, 1);
				}
				Calendar closest = dates.stream().min((d1, d2) -> Long.compare(d1.getTimeInMillis(), d2.getTimeInMillis())).get();
				_log.info("Event Scheduler: Closest Event Time: " + closest.getTime().toString());
				ThreadPoolManager.getInstance().scheduleGeneral(() ->
				{
					RaidEvent.getInstance().setRaidEventData(nextActiveData);
					RaidEvent.getInstance().startEvent();
					RaidEventScheduler.getInstance().StartCalculationOfNextRestartTime();
				}, closest.getTimeInMillis() - currentTime.getTimeInMillis());
			}
		}
		catch (Exception e)
		{
			_log.warning("Event Scheduler: failed to execute, properties missing");
		}
	}
}