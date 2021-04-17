package luna.custom.DressMeEngine;

public class DressMeWeaponData
{
	private final int		_id;
	private final String	_name;
	private final String	_type;
	private final boolean	_isBig;
	private final int		_priceId;
	private final long		_priceCount;
	private final int		_requiredWepId;
	private final float		_requiredTier;
	
	public DressMeWeaponData(int id, String name, String type, boolean isBig, int priceId, long priceCount, int requiredWepId, float requiredTier)
	{
		_id = id;
		_name = name;
		_type = type;
		_isBig = isBig;
		_priceId = priceId;
		_priceCount = priceCount;
		_requiredTier = requiredTier;
		_requiredWepId = requiredWepId;
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
	
	public boolean isBig()
	{
		return _isBig;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public long getPriceCount()
	{
		return _priceCount;
	}
	
	public int get_requiredWepId()
	{
		return _requiredWepId;
	}
	
	public float getRequiredTier()
	{
		return _requiredTier;
	}
}