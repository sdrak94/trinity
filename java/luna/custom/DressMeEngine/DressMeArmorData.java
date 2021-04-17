package luna.custom.DressMeEngine;

public class DressMeArmorData
{
	private final int _id;
	private final String _name;
	private final String _type;
	private final int _chest;
	private final int _legs;
	private final int _gloves;
	private final int _feet;
	private final int _priceId;
	private final long _priceCount;
	private final float _requiredTier;
	private final int _requiredArmorSet;
	private final int _limitedTimePriceId;
	private final int _limitedTimePriceCount;
	private final int _limitedTimeHours;
	private final int _limitedTimePriceId2;
	private final int _limitedTimePriceCount2;
	private final int _limitedTimeHours2;
	private final int _limitedTimePriceId3;
	private final int _limitedTimePriceCount3;
	private final int _limitedTimeHours3;
	
	public DressMeArmorData(int id, String name, String type, int chest, int legs, int gloves, int feet, int priceId, long priceCount, float requiredTier, int requiredArmorSet, int limitedTimePriceId,int limitedTimePriceCount, int limitedTimeHours, int limitedTimePriceId2,int limitedTimePriceCount2, int limitedTimeHours2, int limitedTimePriceId3,int limitedTimePriceCount3, int limitedTimeHours3)
	{
		_id = id;
		_name = name;
		_type = type;
		_chest = chest;
		_legs = legs;
		_gloves = gloves;
		_feet = feet;
		_priceId = priceId;
		_priceCount = priceCount;
		_requiredTier = requiredTier;
		_requiredArmorSet = requiredArmorSet;
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
	
	public String getName()
	{
		return _name;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public int getChest()
	{
		return _chest;
	}
	
	public int getLegs()
	{
		return _legs;
	}
	
	public int getGloves()
	{
		return _gloves;
	}
	
	public int getFeet()
	{
		return _feet;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public long getPriceCount()
	{
		return _priceCount;
	}	
	public float getRequiredTier()
	{
		return _requiredTier;
	}

	public int getRequiredArmorSet()
	{
		return _requiredArmorSet;
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