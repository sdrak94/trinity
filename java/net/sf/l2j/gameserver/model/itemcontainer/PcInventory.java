/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.TradeList.TradeItem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class PcInventory extends Inventory
{
	public static final int		ADENA_ID			= 57;
	public static final int		ANCIENT_ADENA_ID	= 5575;
	public static final long	MAX_ADENA			= 20000000000L;
	public static final int		MAX_SCROLL			= 500;
	private final L2PcInstance	_owner;
	private L2ItemInstance		_adena;
	private L2ItemInstance		_ancientAdena;
	
	public PcInventory(L2PcInstance owner)
	{
		_owner = owner;
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public L2ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public long getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public L2ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public long getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public long getBlueEva()
	{
		return getItemByItemId(4355) != null ? getItemByItemId(4355).getCount() : 0;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}
	
	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if ((!allowAdena && item.getItemId() == 57))
				continue;
			if ((!allowAncientAdena && item.getItemId() == 5575))
				continue;
			boolean isDuplicate = false;
			for (L2ItemInstance litem : list)
			{
				if (litem.getItemId() == item.getItemId())
				{
					isDuplicate = true;
					break;
				}
			}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * Allows an item to appear twice if and only if there is a difference in enchantment level.
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}
	
	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if ((!allowAdena && item.getItemId() == 57))
				continue;
			if ((!allowAncientAdena && item.getItemId() == 5575))
				continue;
			boolean isDuplicate = false;
			for (L2ItemInstance litem : list)
				if ((litem.getItemId() == item.getItemId()) && (litem.getEnchantLevel() == item.getEnchantLevel()))
				{
					isDuplicate = true;
					break;
				}
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(getOwner(), false, false))))
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * 
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public L2ItemInstance[] getAllItemsByItemId(int itemId)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if (item.getItemId() == itemId)
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	public L2ItemInstance[] getAllCertsByItemId()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if (item.getItemId() >= 60078 && item.getItemId() <= 60110)
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * 
	 * @return L2ItemInstance[] : matching items from inventory
	 */
	public Collection<L2ItemInstance> getAllItemsByItemIdNew(int itemId)
	{
		return getAllItemsByItemIdNew(itemId, true);
	}
	
	public List<L2ItemInstance> getAllItemsByItemIdNew(int itemId, boolean includeEquipped)
	{
		final List<L2ItemInstance> list = new ArrayList<>();
		for (L2ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			if ((item.getItemId() == itemId) && (includeEquipped || !item.isEquipped()))
			{
				list.add(item);
			}
		}
		return list;
	}
	
	public ArrayList<L2ItemInstance> getEquipedItems()
	{
		return _items.stream().filter(L2ItemInstance::isEquipped).collect(Collectors.toCollection(ArrayList::new));
	}
	
	public List<L2ItemInstance> getAllItemsByItemIdNew(int itemId, int enchantment, boolean includeEquipped)
	{
		final List<L2ItemInstance> list = new ArrayList<>();
		for (L2ItemInstance item : _items)
		{
			if (item == null)
			{
				continue;
			}
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
			{
				list.add(item);
			}
		}
		return list;
	}
	
	public L2ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() == enchantment))
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	public L2ItemInstance[] getAllItemsByItemIdMultisell(int itemId, int enchantment)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() >= enchantment))
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
			if (item != null && item.isAvailable(getOwner(), allowAdena, allowNonTradeable))
				list.add(item);
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getAvailableItemsFreight()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
			if (item != null && item.isAvailableFreight(getOwner()))
				list.add(item);
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Get all augmented items
	 * 
	 * @return
	 */
	public L2ItemInstance[] getAugmentedItems()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
			if (item != null && item.isAugmented())
				list.add(item);
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Get all element items
	 * 
	 * @return
	 */
	public L2ItemInstance[] getElementItems()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();
		for (L2ItemInstance item : _items)
		{
			if (item != null && item.getElementals() != null)
				list.add(item);
		}
		return list.toArray(new L2ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusted by tradeList
	 * 
	 * @return L2ItemInstance : items in inventory
	 */
	public TradeList.TradeItem[] getAvailableItems(TradeList tradeList)
	{
		List<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();
		for (L2ItemInstance item : _items)
			if (item.isAvailable(getOwner(), false, false))
			{
				TradeList.TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
					list.add(adjItem);
			}
		return list.toArray(new TradeList.TradeItem[list.size()]);
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * 
	 * @param item
	 *            : L2ItemInstance to be adjusten
	 * @return TradeItem representing adjusted item
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		boolean notAllEquipped = false;
		for (L2ItemInstance adjItem : getItemsByItemId(item.getItem().getItemId()))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped())
					notAllEquipped |= true;
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		if (notAllEquipped)
		{
			L2ItemInstance adjItem = getItemByItemId(item.getItem().getItemId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			if (adjItem.getCount() < item.getCount())
				item.setCount(adjItem.getCount());
			return;
		}
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PCInventory
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of adena to be added
	 * @param actor
	 *            : L2PcInstance Player requesting the item add
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, long count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			addItem(process, ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes adena to PCInventory
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of adena to be removed
	 * @param actor
	 *            : L2PcInstance Player requesting the item add
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void reduceAdena(String process, long count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			destroyItemByItemId(process, ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of adena to be added
	 * @param actor
	 *            : L2PcInstance Player requesting the item add
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(String process, long count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of adena to be removed
	 * @param actor
	 *            : L2PcInstance Player requesting the item add
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void reduceAncientAdena(String process, long count, L2PcInstance actor, L2Object reference)
	{
		if (count > 0)
			destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be added
	 * @param actor
	 *            : L2PcInstance Player requesting the item add
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		if (item.getItemId() == L2Item.DONATION_TOKEN && !process.equalsIgnoreCase("donation_token"))
			return null;
		item = super.addItem(process, item, actor, reference);
		if (item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		if (item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		return item;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item Identifier of the item to be added
	 * @param count
	 *            : int Quantity of items to be added
	 * @param actor
	 *            : L2PcInstance Player requesting the item creation
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, int itemId, long count, L2PcInstance actor, L2Object reference)
	{
		if (itemId == L2Item.DONATION_TOKEN && !process.equalsIgnoreCase("donation_token"))
			return null;
		L2ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		if (item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		if (item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		if (item != null && actor != null)
		{
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(item);
				actor.sendPacket(playerIU);
			}
			else
				actor.sendPacket(new ItemList(actor, false));
		}
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item Identifier of the item to be transfered
	 * @param count
	 *            : int Quantity of items to be transfered
	 * @param actor
	 *            : L2PcInstance Player requesting the item transfer
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be destroyed
	 * @param actor
	 *            : L2PcInstance Player requesting the item destroy
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		return this.destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be destroyed
	 * @param actor
	 *            : L2PcInstance Player requesting the item destroy
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, long count, L2PcInstance actor, L2Object reference)
	{
		item = super.destroyItem(process, item, count, actor, reference);
		if (_adena != null && _adena.getCount() <= 0)
			_adena = null;
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
			_ancientAdena = null;
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param objectId
	 *            : int Item Instance identifier of the item to be destroyed
	 * @param count
	 *            : int Quantity of items to be destroyed
	 * @param actor
	 *            : L2PcInstance Player requesting the item destroy
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, int objectId, long count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item identifier of the item to be destroyed
	 * @param count
	 *            : int Quantity of items to be destroyed
	 * @param actor
	 *            : L2PcInstance Player requesting the item destroy
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItemByItemId(String process, int itemId, long count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = getItemByItemId(itemId);
		if (item == null)
		{
			return null;
		}
		return this.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be dropped
	 * @param actor
	 *            : L2PcInstance Player requesting the item drop
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.dropItem(process, item, actor, reference);
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param objectId
	 *            : int Item Instance identifier of the item to be dropped
	 * @param count
	 *            : int Quantity of items to be dropped
	 * @param actor
	 *            : L2PcInstance Player requesting the item drop
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, int objectId, long count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		return item;
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * 
	 * @param item
	 *            : L2ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(L2ItemInstance item)
	{
		// Removes any reference to the item from Shortcut bar
		getOwner().removeItemFromShortCut(item.getObjectId());
		// Removes active Enchant Scroll
		if (item.equals(getOwner().getActiveEnchantItem()))
			getOwner().setActiveEnchantItem(null);
		if (item.getItemId() == ADENA_ID)
			_adena = null;
		else if (item.getItemId() == ANCIENT_ADENA_ID)
			_ancientAdena = null;
		return super.removeItem(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[31][3];
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level,visual_item_id FROM items WHERE owner_id=? AND loc='PAPERDOLL'");
			statement2.setInt(1, objectId);
			ResultSet invdata = statement2.executeQuery();
			while (invdata.next())
			{
				int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				// paperdoll[slot][1] = invdata.getInt("item_id");
				// dressme armor char select
				if (invdata.getInt("visual_item_id") > 0)
				{
					paperdoll[slot][1] = invdata.getInt("visual_item_id");
				}
				else
				{
					paperdoll[slot][1] = invdata.getInt("item_id");
				}
				// dressme weapon char select
				paperdoll[slot][2] = invdata.getInt("enchant_level");
				if (slot == Inventory.PAPERDOLL_LRHAND)
				{
					paperdoll[Inventory.PAPERDOLL_RHAND][0] = invdata.getInt("object_id");
					if (invdata.getInt("visual_item_id") > 0)
					{
						paperdoll[Inventory.PAPERDOLL_RHAND][1] = invdata.getInt("visual_item_id");
					}
					else
					{
						paperdoll[Inventory.PAPERDOLL_RHAND][1] = invdata.getInt("item_id");
					}
					paperdoll[slot][1] = invdata.getInt("visual_item_id") > 0 ? invdata.getInt("visual_item_id") : invdata.getInt("item_id");
					paperdoll[Inventory.PAPERDOLL_RHAND][2] = invdata.getInt("enchant_level");
				}
			}
			invdata.close();
			statement2.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not restore inventory:", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				_log.warning("");
			}
		}
		return paperdoll;
	}
	
	public boolean validateCapacity(L2ItemInstance item)
	{
		int slots = 0;
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
			slots++;
		return validateCapacity(slots);
	}
	
	public boolean validateCapacity(List<L2ItemInstance> items)
	{
		int slots = 0;
		for (L2ItemInstance item : items)
			if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
				slots++;
		return validateCapacity(slots);
	}
	
	public boolean validateCapacityByItemId(int ItemId)
	{
		int slots = 0;
		L2ItemInstance invItem = getItemByItemId(ItemId);
		if (!(invItem != null && invItem.isStackable()))
			slots++;
		boolean ok = validateCapacity(slots);
		if (!ok)
		{
			getOwner().sendMessage("You couldn't receive an item because your inventory is full");
		}
		return ok;
	}
	
	@Override
	public boolean validateCapacity(int slots)
	{
		return (_items.size() + slots <= _owner.getInventoryLimit());
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		return true;
	}
	/**
	 * Returns the ID of the item in the paperdoll slot
	 * 
	 * @param slot
	 *            : int designating the slot
	 * @return int designating the ID of the item
	 */
}
