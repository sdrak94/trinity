package luna.custom.season;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;

public class SeasonManager
{
	public int _season;
	public int _pastSeason;
	public int season_days_left; //TODO
	
	EnumSet<Month> Season_1 = EnumSet.of( Month.SEPTEMBER	, Month.OCTOBER );
	EnumSet<Month> Season_2 = EnumSet.of( Month.NOVEMBER	, Month.DECEMBER );
	EnumSet<Month> Season_3 = EnumSet.of( Month.JANUARY		, Month.FEBRUARY );
	EnumSet<Month> Season_4 = EnumSet.of( Month.MARCH		, Month.APRIL );
	EnumSet<Month> Season_5 = EnumSet.of( Month.MAY			, Month.JUNE );
	EnumSet<Month> Season_6 = EnumSet.of( Month.JULY		, Month.AUGUST );

	ZoneId zoneId = ZoneId.of( "Europe/Berlin" );
	ZonedDateTime zdt = ZonedDateTime.now( zoneId );
	Month month = Month.from( zdt );
	
	{
		if ( Season_1.contains(month))
		{
		    _season = 1;
		}
		else if (Season_2.contains(month))
		{
		   _season = 2;
		}
		else if (Season_3.contains(month))
		{
		    _season = 3;
		}
		else if (Season_4.contains(month))
		{
		    _season = 4;
		}
		else if (Season_5.contains(month))
		{
		    _season = 5;
		}
		else if (Season_6.contains(month))
		{
		    _season = 6;
		}
	}
	{
		
	}
	public int getSeason()
	{
		return _season;
	}
	public int getPastSeason()
	{
		if (_season == 1)
		{
			_pastSeason = 6;
		}
		else 
		{
			_pastSeason = _season - 1;
		}
		return _pastSeason;
	}
	
	
	
	public static SeasonManager getInstance()
	{
		return SingletonHolder._instance;
	}
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SeasonManager _instance =  new SeasonManager();
	}
	
}