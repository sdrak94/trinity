package net.sf.l2j.gameserver.model;

public class L2ExtractableProductItem2
{
	private final int _id;
	private final int _ammount;
	private final int _chance;
	
	public L2ExtractableProductItem2(final int id, final int ammount, final int chance)
	{
		_id = id;
		_ammount = ammount;
		_chance = chance;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getAmmount()
	{
		return _ammount;
	}
	
	public int getChance()
	{
		return _chance;
	}
}
