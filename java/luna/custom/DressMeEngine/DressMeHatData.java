package luna.custom.DressMeEngine;

public class DressMeHatData
{
	private final int _id;
	private final int _hat;
	private final String _name;
	private final int _slot;
	private final int _priceId;
	private final long _priceCount;
	private final int _limitedTimePriceId;
	private final int _limitedTimePriceCount;
	private final int _limitedTimeHours;
	private final int _limitedTimePriceId2;
	private final int _limitedTimePriceCount2;
	private final int _limitedTimeHours2;
	private final int _limitedTimePriceId3;
	private final int _limitedTimePriceCount3;
	private final int _limitedTimeHours3;
	
	public DressMeHatData(int id, int hat, String name, int slot, int priceId, long priceCount, int limitedTimePriceId,int limitedTimePriceCount, int limitedTimeHours, int limitedTimePriceId2,int limitedTimePriceCount2, int limitedTimeHours2, int limitedTimePriceId3,int limitedTimePriceCount3, int limitedTimeHours3)
	{
		_id = id;
		_hat = hat;
		_name = name;
		_slot = slot;
		_priceId = priceId;
		_priceCount = priceCount;
		_limitedTimePriceId =limitedTimePriceId;
		_limitedTimePriceCount = limitedTimePriceCount;
		_limitedTimeHours = limitedTimeHours;
		_limitedTimePriceId2 =limitedTimePriceId2;
		_limitedTimePriceCount2 = limitedTimePriceCount2;
		_limitedTimeHours2 = limitedTimeHours2;
		_limitedTimePriceId3 =limitedTimePriceId3;
		_limitedTimePriceCount3 = limitedTimePriceCount3;
		_limitedTimeHours3 = limitedTimeHours3;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getHatId()
	{
		return _hat;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getSlot()
	{
		return _slot;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public long getPriceCount()
	{
		return _priceCount;
	}
	public int getLimitedTimePriceId()
	{
		return _limitedTimePriceId;
	}

	public int getLimitedTimePriceCount()
	{
		return _limitedTimePriceCount;
	}

	public int getLimitedTimeHours()
	{
		return _limitedTimeHours;
	}
	
	

	public int getLimitedTimePriceId2()
	{
		return _limitedTimePriceId2;
	}

	public int getLimitedTimePriceCount2()
	{
		return _limitedTimePriceCount2;
	}

	public int getLimitedTimeHours2()
	{
		return _limitedTimeHours2;
	}
	
	

	public int getLimitedTimePriceId3()
	{
		return _limitedTimePriceId3;
	}

	public int getLimitedTimePriceCount3()
	{
		return _limitedTimePriceCount3;
	}

	public int getLimitedTimeHours3()
	{
		return _limitedTimeHours3;
	}
}