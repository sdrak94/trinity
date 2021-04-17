package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.Env;

public final class ConditionSlotItemId extends ConditionInventory {

	public final int _itemId;
	public final int _enchantLevel;

	public ConditionSlotItemId(int slot, int itemId, int enchantLevel)
	{
		super(slot);
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player instanceof L2PcInstance))
			return false;
		
		boolean good = false;
		boolean switched = false;
		
		final Inventory inv = ((L2PcInstance)env.player).getInventory();
		
		L2ItemInstance item = inv.getPaperdollItem(_slot);
		
		if (item == null)
		{
			if (_slot == 1)
			{
				_slot = 2;
				switched = true;
			}
			else if (_slot == 2)
			{
				_slot = 1;
				switched = true;
			}
			else if (_slot == 5)
			{
				_slot = 6;
				switched = true;
			}
			else if (_slot == 6)
			{
				_slot = 5;
				switched = true;
			}
			
			if (switched)
				item = inv.getPaperdollItem(_slot);
			
			if (item == null)
				return _itemId == 0;
		}
		
		good = item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
		
		if (!good && !switched)
		{
			if (_slot == 1)
			{
				_slot = 2;
				switched = true;
			}
			else if (_slot == 2)
			{
				_slot = 1;
				switched = true;
			}
			else if (_slot == 5)
			{
				_slot = 6;
				switched = true;
			}
			else if (_slot == 6)
			{
				_slot = 5;
				switched = true;
			}
			
			if (switched)
			{
				item = inv.getPaperdollItem(_slot);
				
				if (item == null)
					return _itemId == 0;
				
				good = item.getItemId() == _itemId && item.getEnchantLevel() >= _enchantLevel;
			}
		}
		
		return good;
	}
}
