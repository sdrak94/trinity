package net.sf.l2j.gameserver.model.itemcontainer;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;

public class PetInventory extends Inventory
{
	private final L2PetInstance _owner;

	public PetInventory(L2PetInstance owner)
    {
		_owner = owner;
	}

	@Override
	public L2PetInstance getOwner()
    {
        return _owner;
    }
	
	@Override
	public int getOwnerId()
	{
		// gets the L2PcInstance-owner's ID
		int id;
		try
		{
			id = _owner.getOwner().getObjectId();
		}
		catch (NullPointerException e) 
		{
			return 0;
		}
		return id;
	}

	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{

	}
	
    public boolean validateCapacity(L2ItemInstance item)
    {
        int slots = 0;

        if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
        	slots++;

        return validateCapacity(slots);
    }
    
	@Override
	public boolean validateCapacity(int slots)
	{
		return (_items.size() + slots <= _owner.getInventoryLimit());
	}
	
	public boolean validateWeight(L2ItemInstance item, long count)
	{
		return true;
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		return true;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
    {
        return ItemLocation.PET;
    }

	@Override
	protected ItemLocation getEquipLocation()
    {
        return ItemLocation.PET_EQUIP;
    }
}
