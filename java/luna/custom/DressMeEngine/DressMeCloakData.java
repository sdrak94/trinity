package luna.custom.DressMeEngine;

public class DressMeCloakData 
{
	private final int _id;
	private final int _cloak;
	private final String _name;
	private final int _priceId;
	private final long _priceCount;
	private final int _requiredId;
	
	public DressMeCloakData(int id, int cloak, String name, int priceId, long priceCount, int requiredId)
	{
		_id = id;
		_cloak = cloak;
		_name = name;
		_priceId = priceId;
		_priceCount = priceCount;
		_requiredId = requiredId;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getCloakId()
	{
		return _cloak;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public long getPriceCount()
	{
		return _priceCount;
	}

	public int get_requiredId()
	{
		return _requiredId;
	}
}
