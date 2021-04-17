package cz.nxs.interf.delegate;

import cz.nxs.interf.Values;
import cz.nxs.l2j.WeaponType;
import cz.nxs.l2j.delegate.IItemData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

/**
 * @author hNoke
 *
 */
public class ItemData implements IItemData
{
	private L2ItemInstance _item;
	private L2Item _itemTemplate;
	
	/** creates item template */
	public ItemData(int id)
	{
		_item = null;
		_itemTemplate = ItemTable.getInstance().getTemplate(id);
	}
	
	/** creates a delegate for given L2ItemInstance */
	public ItemData(L2ItemInstance cha)
	{
		_item = cha;
		
		if(_item != null)
			_itemTemplate = _item.getItem();
	}
	
	/** creates NEW L2ItemInstance */
	public ItemData(int itemId, int count)
	{
		_item = ItemTable.getInstance().createItem("Event Engine ItemData", itemId, count, null);
		
		if(_item != null)
			_itemTemplate = _item.getItem();
	}
	
	public L2ItemInstance getOwner()
	{
		return _item;
	}
	
	@Override
	public int getObjectId()
	{
		if(exists())
			return getOwner().getObjectId();
		return -1;
	}
	
	public L2Item getTemplate()
	{
		return _itemTemplate;
	}
	
	@Override
	public boolean exists()
	{
		return _item != null;
	}
	
	@Override
	public boolean isEquipped()
	{
		if(exists() && _item.isEquipped())
			return true;
		return false;
	}
	
	@Override
	public int getItemId()
	{
		return _itemTemplate.getItemId();
	}
	
	@Override
	public String getItemName()
	{
		return _itemTemplate.getName();
	}
	
	@Override
	public int getEnchantLevel()
	{
		return _item != null ? _item.getEnchantLevel() : 0;
	}
	
	@Override
	public int getCrystalType()
	{
		return _itemTemplate.getCrystalType();
	}
	
	@Override
	public int getBodyPart()
	{
		return _itemTemplate.getBodyPart();
	}
	
	@Override
	public boolean isArmor()
	{
		return _itemTemplate instanceof L2Armor;
	}
	
	@Override
	public boolean isWeapon()
	{
		return _itemTemplate instanceof L2Weapon;
	}
	
	@Override
	public WeaponType getWeaponType()
	{
		if(isWeapon())
			return Values.getInstance().getWeaponType(this);
		else 
			return null;
	}
	
	@Override
	public boolean isType2Armor()
	{
		return _itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR;
	}
	
	@Override
	public boolean isType2Weapon()
	{
		return _itemTemplate.getType2() == L2Item.TYPE2_WEAPON;
	}
	
	@Override
	public boolean isType2Accessory()
	{
		return _itemTemplate.getType2() == L2Item.TYPE2_ACCESSORY;
	}
	
	@Override
	public boolean isJewellery()
	{
		return _itemTemplate.getType2() == L2Item.TYPE2_ACCESSORY;
	}
	
	@Override
	public boolean isPotion()
	{
		return _itemTemplate.getItemType() == L2EtcItemType.POTION;
	}
	
	@Override
	public boolean isScroll()
	{
		return _itemTemplate.getItemType() == L2EtcItemType.SCROLL;
	}
	
	@Override
	public boolean isPetCollar()
	{
		if(_item != null && _item.isEtcItem() && _item.getEtcItem().getItemType() == L2EtcItemType.PET_COLLAR)
			return true;
		return false;
	}
	
	@Override
	public String getTier()
	{
		String tier = null;
		//tier = _item.getTier();
		return tier;
	}
	
	@Override
	// temporary changes item's enchant level
	public void setEnchantLevel(int level)
	{
		if(_item.isArmor() || _item.isWeapon())
		{
			
		}
	}
	
	@Override
	// restores enchant level back to original value
	public void restoreEnchantLevel()
	{
		
	}
}
