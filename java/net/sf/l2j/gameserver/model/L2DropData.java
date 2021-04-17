package net.sf.l2j.gameserver.model;

import java.util.Arrays;

public class L2DropData
{
public static final int MAX_CHANCE = 1000000;

private int _itemId;
private int _minDrop;
private int _maxDrop;
private int _chance;
private int _partyDropCount = 0;

public final int getPartyDropCount()
{
	return Math.min(_partyDropCount, 10);
}

public final void setPartyDropCount(int partyDropCount)
{
	_partyDropCount = partyDropCount;
	if (_partyDropCount > 10)
		_partyDropCount = 10;
}

private int _enchantLevel;
private int _minEnch;
private int _maxEnch;
private String _questID = null;
private String[] _stateID = null;
private double _fakeChance = -1d;

/**
 * Returns the ID of the item dropped
 * @return int
 */
public int getItemId()
{
	return _itemId;
}

/**
 * Sets the ID of the item dropped
 * @param itemId : int designating the ID of the item
 */
public void setItemId(int itemId)
{
	_itemId = itemId;
}

/**
 * Returns the minimum quantity of items dropped
 * @return int
 */
public int getMinDrop()
{
	return _minDrop;
}

/**
 * Returns the maximum quantity of items dropped
 * @return int
 */
public int getMaxDrop()
{
	return _maxDrop;
}

/**
 * Returns the chance of having a drop
 * @return int
 */
public int getChance()
{
	return _chance;
}

public double getFakeChance()
{
		return _fakeChance;
}
/**
 * Sets the value for minimal quantity of dropped items
 * @param mindrop : int designating the quantity
 */
public void setMinDrop(int mindrop)
{
	_minDrop = mindrop;
}

/**
 * Sets the value for maximal quantity of dopped items
 * @param maxdrop : int designating the quantity of dropped items
 */
public void setMaxDrop(int maxdrop)
{
	_maxDrop = maxdrop;
}

/**
 * Sets the chance of having the item for a drop
 * @param chance : int designating the chance
 */
public void setChance(int chance)
{
	_chance = chance;
}

public int getEnchantLevel()
{
	return _enchantLevel;
}

public void setEnchantLevel(int enchant)
{
	_enchantLevel = enchant;
}
/**
 * Returns the stateID.
 * @return String[]
 */
public String[] getStateIDs()
{
	return _stateID;
}

/**
 * Adds states of the dropped item
 * @param list : String[]
 */
public void addStates(String[] list)
{
	_stateID = list;
}

/**
 * Returns the questID.
 * @return String designating the ID of the quest
 */
public String getQuestID()
{
	return _questID;
}

/**
 * Sets the questID
 * @param String designating the questID to set.
 */
public void setQuestID(String questID)
{
	_questID = questID;
}

/**
 * Returns if the dropped item is requested for a quest
 * @return boolean
 */
public boolean isQuestDrop()
{
	return _questID != null && _stateID != null;
}

/**
 * Returns a report of the object
 * @return String
 */
@Override
public String toString()
{
	String out = "ItemID: " + getItemId() + " Min: " + getMinDrop() +
	" Max: " + getMaxDrop() + " Chance: " + (getChance() / 10000.0) + "%";
	if (isQuestDrop())
	{
		out += " QuestID: " + getQuestID() + " StateID's: " + Arrays.toString(getStateIDs());
	}
	
	return out;
}

/**
 * @see java.lang.Object#hashCode()
 */
@Override
public int hashCode()
{
	final int prime = 31;
	int result = 1;
	result = prime * result + _itemId;
	return result;
}

/**
 * @see java.lang.Object#equals(java.lang.Object)
 */
@Override
public boolean equals(Object obj)
{
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (!(obj instanceof L2DropData))
		return false;
	final L2DropData other = (L2DropData) obj;
	if (_itemId != other._itemId)
		return false;
	return true;
}

public void setFakeChance(final double d)
{
	_fakeChance = d;
}

}
