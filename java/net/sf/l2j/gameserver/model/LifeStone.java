package net.sf.l2j.gameserver.model;


public class LifeStone
{

	private final int _grade;
	private final boolean _normalType;
	
	public LifeStone(final int grade, final boolean normalType)
	{
		_grade = grade;
		_normalType = normalType;
	}
	
	public int getGrade()
	{
		return _grade;
	}
	
	public boolean isValid(L2ItemInstance item)
	{
		if (_normalType)
			return item.isWeapon();
		return item.isJewelry();
	}
}