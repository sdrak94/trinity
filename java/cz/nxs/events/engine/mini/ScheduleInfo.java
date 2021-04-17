package cz.nxs.events.engine.mini;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cz.nxs.events.engine.base.EventType;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - stores information about times when certain EventMode is aviable to run
 */
public class ScheduleInfo
{
	//private EventType _event;
	//private String _modeName;
	private Map<Integer, RunTime> _times;
	private int currentRunTimeUsed;
	private boolean defaultTimeUsed;
	
	public ScheduleInfo(EventType type, String modeName)
	{
		//_event = type;
		//_modeName = modeName;
		
		_times = new FastMap<Integer, RunTime>();
		currentRunTimeUsed = 0;
		
		RunTime defaultTime = addTime();
		
		for(Day d : Day.values())
			defaultTime.addDay(d.prefix);
		
		defaultTimeUsed = true;
	}
	
	public String decrypt()
	{
		TextBuilder tb = new TextBuilder();
		
		for(RunTime time : _times.values())
		{
			tb.append(time.from + "-" + time.to + "_" + time.getDaysString(false) + ";");
		}
		
		String result = tb.toString();
		
		if(result.length() > 0)
			return result.substring(0, result.length()-1);
		else return result;
	}
	
	public void encrypt(String data)
	{
		if(data.length() == 0)
			return;
		
		try
		{
			String[] runtimes = data.split(";");
			
			String daysString;
			String hours;
			
			String from;
			String to;
			
			String[] days;
			
			for(String runtime : runtimes)
			{
				hours = runtime.split("_")[0];
				daysString = runtime.split("_")[1];
				
				from = hours.split("-")[0];
				to = hours.split("-")[1];
				
				days = daysString.split(",");
				
				RunTime time = addTime();
				time.from = from;
				time.to = to;
				
				if(days.length == 1 && days[0].equals("AllDays"))
				{
					for(Day d : Day.values())
						time.addDay(d.prefix);
				}
				else
				{
					for(String s : days)
					{
						time.addDay(s);
					}
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public long getNextStart(boolean test)
	{
		if(_times.size() == 0)
			return -1;
		
		long lowestValue = Long.MAX_VALUE;
		long temp = 0;
		
		for(Entry<Integer, RunTime> time : _times.entrySet())
		{
			temp = time.getValue().getNext(true, test);
			
			if(temp != -1 && temp < lowestValue)
			{
				lowestValue = temp;
				currentRunTimeUsed = time.getKey();
			}
		}
		
		return lowestValue;
	}
	
	public long getEnd(boolean test)
	{
		if(_times.size() == 0)
			return -1;
		
		return _times.get(currentRunTimeUsed).getNext(false, test);
	}
	
	public boolean isNonstopRun()
	{
		if(_times.size() == 1)
		{
			for(RunTime time : _times.values())
			{
				// non stop run
				if(time.days.size() == Day.values().length && (time.from.equals("00:00") || time.from.equals("0:00")) && time.to.equals("23:59"))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public RunTime addTime()
	{
		if(defaultTimeUsed)
		{
			_times.clear();
			
			currentRunTimeUsed = 0;
			defaultTimeUsed = false;
		}
		
		RunTime time;
		int lastId = 0;
		
		for(int id : _times.keySet())
		{
			if(id > lastId)
				lastId = id;
		}
		
		lastId ++;
		
		time = new RunTime(lastId);
		_times.put(lastId, time);
		
		return time;
	}
	
	public Map<Integer, RunTime> getTimes()
	{
		return _times;
	}
	
	public class RunTime
	{
		public int id;
		public List<Day> days = new ArrayList<Day>();
		public String from;
		public String to;
		
		public RunTime(int id)
		{
			this.id = id;
			this.from = "00:00";
			this.to = "23:59";
		}
		
		private Calendar getNextRun(boolean start)
		{
			if(days.isEmpty())
				return null;
			
			Calendar current = Calendar.getInstance();
			
			List<Calendar> times = new FastList<Calendar>();
			
			// init values
			for(Day day : days)
			{
				Calendar time = Calendar.getInstance();
				time.set(Calendar.DAY_OF_WEEK, day.dayId);
				
				if(start)
				{
					time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(from.split(":")[0]));
					time.set(Calendar.MINUTE, Integer.parseInt(from.split(":")[1]));
				}
				else
				{
					time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(to.split(":")[0]));
					time.set(Calendar.MINUTE, Integer.parseInt(to.split(":")[1]));
				}
				
				times.add(time);
			}
			
			//calculate next time
			
			Calendar runTime = null;
			Calendar temp = null;
			
			// same week
			for(Calendar time : times)
			{
				// can run in the same week
				if(time.getTimeInMillis() > current.getTimeInMillis())
				{
					if(temp == null) 
						temp = time;
					else
					{
						if(time.getTimeInMillis() - current.getTimeInMillis() < temp.getTimeInMillis() - current.getTimeInMillis())
						{
							temp = time;
						}
					}
				}
			}
			
			if(temp != null)
				runTime = temp;
			else // no time aviable in this week - switch to next week
			{
				for(Calendar time : times)
				{
					time.add(Calendar.HOUR, 168);
					
					if(time.getTimeInMillis() > current.getTimeInMillis())
					{
						if(temp == null) 
							temp = time;
						else
						{
							if(time.getTimeInMillis() - current.getTimeInMillis() < temp.getTimeInMillis() - current.getTimeInMillis())
							{
								temp = time;
							}
						}
					}
				}
			}
			
			if(temp == null)
			{
				System.out.println("No time found!! RunTime ID = " + id + ", from - " + from + ", to " + to);
				return null;
			}
			else
			{
				runTime = temp;
			}
			
			runTime.set(Calendar.SECOND, 1);
			return runTime;
		}
		
		public long getNext(boolean start, boolean test)
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar runTime = getNextRun(start);
			
			// days are not specified
			if(runTime == null)
				return -1;
			
			if(test)
				return runTime.getTimeInMillis();
			
			long delay = runTime.getTimeInMillis() - currentTime.getTimeInMillis();
			if(delay < 0) // the event should have already started
				delay = 0;
			
			return delay;
		}
		
		public String getDaysString(boolean html)
		{
			TextBuilder tb = new TextBuilder();
			int i = 1;
			
			if(days.size() == Day.values().length)
			{
				if(html)
					return "All days";
				else
					return "AllDays";
			}
			
			for(Day day : days)
			{
				tb.append(html ? day.prefix.toUpperCase() : day.prefix);
				if(i < days.size())
					tb.append(",");
				
				i++;
			}
			return tb.toString();
		}
		
		public boolean isActual()
		{
			Calendar current = Calendar.getInstance();
			
			Calendar start =  Calendar.getInstance();
			Calendar end =  Calendar.getInstance();
			
			start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(from.split(":")[0]));
			start.set(Calendar.MINUTE, Integer.parseInt(from.split(":")[1]));
			
			end.set(Calendar.HOUR_OF_DAY, Integer.parseInt(to.split(":")[0]));
			end.set(Calendar.MINUTE, Integer.parseInt(to.split(":")[1]));
			
			if(start.getTimeInMillis() > current.getTimeInMillis() || end.getTimeInMillis() < current.getTimeInMillis())
				return false;
			else // same hour
			{
				for(Day day : days)
				{
					if(day.dayId == current.get(Calendar.DAY_OF_WEEK)) // same day
						return true;
				}
			}
			
			return false;
		}
		
		public void addDay(String prefix)
		{
			for(Day day : Day.values())
			{
				if(prefix.equalsIgnoreCase(day.prefix))
				{
					days.add(day);
					break;
				}
			}
		}
	}
	
	public static enum Day
	{
		Monday ("m", Calendar.MONDAY, "Monday"),
		Tuesday ("tu", Calendar.TUESDAY, "Tuesday"),
		Wednesday("w", Calendar.WEDNESDAY, "Wednesday"),
		Thursday("th", Calendar.THURSDAY, "Thursday"),
		Friday("f", Calendar.FRIDAY, "Friday"),
		Saturday("sa", Calendar.SATURDAY, "Saturday"),
		Sunday("su", Calendar.SUNDAY, "Sunday");
		
		public String prefix;
		int dayId;
		public String fullName;
		private Day(String s, int id, String fullName)
		{
			this.prefix = s;
			this.dayId = id;
			this.fullName = fullName;
		}
		
		public static Day getDayByName(String name)
		{
			for(Day d : Day.values())
			{
				if(d.fullName.equalsIgnoreCase(name))
					return d;
			}
			return null;
		}
		
		public static Day getDay(String prefix)
		{
			for(Day d : Day.values())
			{
				if(d.prefix.equals(prefix))
					return d;
			}
			return null;
		}
	}
	
	public static void main(String[] args)
	{
		ScheduleInfo info = new ScheduleInfo(EventType.Classic_1v1, "HeyTest");
		
		RunTime time1 = info.addTime();
		time1.from = "14:00";
		time1.to = "20:00";
		time1.addDay("m");
		time1.addDay("tu");
		time1.addDay("su");
		
		RunTime time2 = info.addTime();
		time2.from = "20:30";
		time2.to = "21:00";
		time2.addDay("m");
		//time2.addDay("sa");
		
		RunTime time3 = info.addTime();
		time3.from = "14:00";
		time3.to = "14:30";
		time3.addDay("su");
		
		long l = info.getNextStart(false);
		System.out.println("Starting in " + l);
		
		//Date date = new Date(l);
		//System.out.println("Date: " + date.toString());
		
		System.out.println("Days: " + (l / 86400000));
		System.out.println("Hours: " + (l / 3600000));
		System.out.println("Minutes: " + (l / 60000));
	}
}
