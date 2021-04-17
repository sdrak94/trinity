package luna.custom.DressMeEngine;

public class DressMeShieldData
{
	private final int _id;
	private final int _shield;
	private final String _name;
	private final int _priceId;
	private final long _priceCount;
	private final int _requiredShieldId;
	private final float		_requiredTier;
	
	public DressMeShieldData(int id, int shield, String name, int priceId, long priceCount, int requiredShieldId, float requiredTier)
	{
		_id = id;
		_shield = shield;
		_name = name;
		_priceId = priceId;
		_priceCount = priceCount;
		_requiredShieldId = requiredShieldId;
		_requiredTier = requiredTier;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getShieldId()
	{
		return _shield;
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

	public int get_requiredShieldId()
	{
		return _requiredShieldId;
	}
	public float getRequiredTier()
	{
		return _requiredTier;
	}
}