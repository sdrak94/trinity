package net.sf.l2j.gameserver.model.events.dataTables;

public class RewardsTemplate
{
	private final int	_rewardType;
	private final int	_itemId;
	private final int	_ammount;
	private final int	_chance;
	private final boolean _static;
	
	public RewardsTemplate(int rewardType, int itemId, int ammount, int chance, boolean isStatic)
	{
		_rewardType = rewardType;
		_itemId = itemId;
		_ammount = ammount;
		_chance = chance;
		_static = isStatic;
	}
	public int getRewardType()
	{
		return _rewardType;
	}
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getAmmount()
	{
		return _ammount;
	}
	
	public int getChance()
	{
		return _chance;
	}
	public boolean isStatic()
	{
		return _static;
	}
}