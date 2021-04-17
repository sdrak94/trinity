package net.sf.l2j.gameserver.model;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class TradeList
{
public class TradeItem
{
private int _objectId;
private final L2Item _item;
private int _enchant;
private final int _type2;
private long _count;
private long _storeCount;
private long _price;
private final byte _elemAtkType;
private final int _elemAtkPower;
private final int[] _elemDefAttr = {0, 0, 0, 0, 0, 0};

public TradeItem(L2ItemInstance item, long count, long price)
{
	_objectId = item.getObjectId();
	_item = item.getItem();
	_enchant = item.getEnchantLevel();
	_type2 = item.getCustomType2();
	_count = count;
	_price = price;
	_elemAtkType = item.getAttackElementType();
	_elemAtkPower = item.getAttackElementPower();
	for (byte i = 0; i < 6; i++)
		_elemDefAttr[i] = item.getElementDefAttr(i);
}

public TradeItem(L2Item item, long count, long price)
{
	_objectId = 0;
	_item = item;
	_enchant = 0;
	_type2 = 0;
	_count = count;
	_storeCount = count;
	_price = price;
	_elemAtkType = Elementals.NONE;
	_elemAtkPower = 0;
}

public TradeItem(TradeItem item, long count, long price)
{
	_objectId = item.getObjectId();
	_item = item.getItem();
	_enchant = item.getEnchant();
	_type2 = 0;
	_count = count;
	_storeCount = count;
	_price = price;
	_elemAtkType = item.getAttackElementType();
	_elemAtkPower = item.getAttackElementPower();
	for (byte i = 0; i < 6; i++)
		_elemDefAttr[i] = item.getElementDefAttr(i);
}

public void setObjectId(int objectId)
{
	_objectId = objectId;
}

public int getObjectId()
{
	return _objectId;
}

public L2Item getItem()
{
	return _item;
}

public void setEnchant(int enchant)
{
	_enchant = enchant;
}

public int getEnchant()
{
	return _enchant;
}

public int getCustomType2()
{
	return _type2;
}

public void setCount(long count)
{
	_count = count;
}

public long getCount()
{
	return _count;
}

public long getStoreCount()
{
	return _storeCount;
}

public void setPrice(long price)
{
	_price = price;
}

public long getPrice()
{
	return _price;
}

public byte getAttackElementType()
{
	return _elemAtkType;
}

public int getAttackElementPower()
{
	return _elemAtkPower;
}

public int getElementDefAttr(byte i)
{
	return _elemDefAttr[i];
}
}

private static final Logger _log = Logger.getLogger(TradeList.class.getName());

private final L2PcInstance _owner;
private L2PcInstance _partner;
private final List<TradeItem> _items;
private String _title;
private boolean _packaged;

private boolean _confirmed = false;
private boolean _locked = false;

public TradeList(L2PcInstance owner)
{
	_items = new FastList<TradeItem>();
	_owner = owner;
}

public L2PcInstance getOwner()
{
	return _owner;
}

public void setPartner(L2PcInstance partner)
{
	_partner = partner;
}

public L2PcInstance getPartner()
{
	return _partner;
}

public void setTitle(String title)
{
	if (title.length()>26)
	{
		if (title.length()>35)
		{
			
			_log.warning("[VENDOR BUG] "+_owner.getName() +" tried to use a store msg with length: " +title.length()+ " Characters and message: "+title);
		}
		title = title.substring(0, 26);
	}
	_title = title;
}

public String getTitle()
{
	return _title;
}

public boolean isLocked()
{
	return _locked;
}

public boolean isConfirmed()
{
	return _confirmed;
}

public boolean isPackaged()
{
	return _packaged;
}

public void setPackaged(boolean value)
{
	_packaged = value;
}

/**
 * Retrieves items from TradeList
 */
public TradeItem[] getItems()
{
	return _items.toArray(new TradeItem[_items.size()]);
}

/**
 * Returns the list of items in inventory available for transaction
 * @return L2ItemInstance : items in inventory
 */
public TradeList.TradeItem[] getAvailableItems(PcInventory inventory)
{
	final List<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();
	for (TradeList.TradeItem item : _items)
	{
		item = new TradeItem(item, item.getCount(), item.getPrice());
		inventory.adjustAvailableItem(item);
		list.add(item);
	}
	
	return list.toArray(new TradeList.TradeItem[list.size()]);
}

/**
 * Returns Item List size
 */
public int getItemCount()
{
	return _items.size();
}

/**
 * Adjust available item from Inventory by the one in this list
 * @param item : L2ItemInstance to be adjusted
 * @return TradeItem representing adjusted item
 */
public TradeItem adjustAvailableItem(L2ItemInstance item)
{
	if (item.isStackable())
	{
		for (TradeItem exclItem : _items)
		{
			if (exclItem.getItem().getItemId() == item.getItemId())
			{
				if (item.getCount() <= exclItem.getCount())
					return null;
				else return new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
			}
		}
	}
	return new TradeItem(item, item.getCount(), item.getReferencePrice());
}

/**
 * Adjust ItemRequest by corresponding item in this list using its <b>ObjectId</b>
 * @param item : ItemRequest to be adjusted
 */
public void adjustItemRequest(ItemRequest item)
{
	for (TradeItem filtItem : _items)
	{
		if (filtItem.getObjectId() == item.getObjectId())
		{
			if (filtItem.getCount() < item.getCount())
				item.setCount(filtItem.getCount());
			return;
		}
	}
	item.setCount(0);
}

/**
 * Add simplified item to TradeList
 * @param objectId : int
 * @param count : int
 * @return
 */
public synchronized TradeItem addItem(int objectId, long count)
{
	return addItem(objectId, count, 0);
}

/**
 * Add item to TradeList
 * @param objectId : int
 * @param count : long
 * @param price : long
 * @return
 */
public synchronized TradeItem addItem(int objectId, long count, long price)
{
	if (isLocked())
	{
		_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
		return null;
	}
	L2Object o = L2World.getInstance().findObject(objectId);
	if (!(o instanceof L2ItemInstance))
	{
		_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
		return null;
	}
	L2ItemInstance item = (L2ItemInstance)o;
	
	boolean allow = getOwner().isGM() && Config.GM_TRADE_RESTRICTED_ITEMS;
	
	if (!allow && item.getUntradeableTime() <= 9999999900003L)
	{
		try
		{
			if (getOwner().isThisCharacterMarried())
			{
				final long marriageDate = CoupleManager.getInstance().getCouple(getOwner().getCoupleId()).getWeddingDate().getTimeInMillis();
				final long difference = System.currentTimeMillis() - marriageDate;
				
				if (difference > 5529600000L) // 128 days
				{
					if (getOwner().getActiveTradeList().getPartner().getObjectId() == getOwner().getPartnerId())
						allow = true;
				}
			}
		}
		catch (Exception e)
		{
			allow = false;
		}
	}
	
	if (!(item.isTradeable() || allow) || item.getItemType() == L2EtcItemType.QUEST || item.isHeroItem())
		return null;
	
	if (count <= 0 || count > item.getCount())
		return null;
	
	if (!item.isStackable() && count > 1)
	{
		_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
		return null;
	}
	
	if ((MAX_ADENA / count) < price)
	{
		_log.warning(_owner.getName() + ": Attempt to overflow adena !");
		return null;
	}
	
	if (item.getItemId() == 4355)
	{
		if (_owner.getActiveTradeList() == null)
		{
			_owner.sendMessage("You cannot add Blue Evas to private stores");
			return null;
		}
	}
	
	for (TradeItem checkitem : _items)
	{
		if (checkitem.getObjectId() == objectId)
			return null;
	}
	TradeItem titem = new TradeItem(item, count, price);
	_items.add(titem);
	
	// If Player has already confirmed this trade, invalidate the confirmation
	invalidateConfirmation();
	return titem;
}

/**
 * Add item to TradeList
 * @param objectId : int
 * @param count : long
 * @param price : long
 * @return
 */
public synchronized TradeItem addItemByItemId(int itemId, long count, long price)
{
	if (isLocked())
	{
		_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
		return null;
	}
	
	L2Item item = ItemTable.getInstance().getTemplate(itemId);
	if (item == null)
	{
		_log.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
		return null;
	}
	
	if (!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
		return null;
	
	if (!item.isStackable() && count > 1)
	{
		_log.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
		return null;
	}
	
	if ((MAX_ADENA / count) < price)
	{
		_log.warning(_owner.getName() + ": Attempt to overflow adena !");
		return null;
	}
	
	TradeItem titem = new TradeItem(item, count, price);
	_items.add(titem);
	
	// If Player has already confirmed this trade, invalidate the confirmation
	invalidateConfirmation();
	return titem;
}

/**
 * Remove item from TradeList
 * @param objectId : int
 * @param count : int
 * @return
 */
public synchronized TradeItem removeItem(int objectId, int itemId, long count)
{
	if (isLocked())
	{
		_log.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
		return null;
	}
	
	for (TradeItem titem : _items)
	{
		if (titem.getObjectId() == objectId
				|| titem.getItem().getItemId() == itemId)
		{
			// If Partner has already confirmed this trade, invalidate the confirmation
			if (_partner != null)
			{
				TradeList partnerList = _partner.getActiveTradeList();
				if (partnerList == null)
				{
					_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
					return null;
				}
				partnerList.invalidateConfirmation();
			}
			
			// Reduce item count or complete item
			if (count != -1 && titem.getCount() > count)
				titem.setCount(titem.getCount() - count);
			else _items.remove(titem);
			
			return titem;
		}
	}
	return null;
}

/**
 * Update items in TradeList according their quantity in owner inventory
 */
public synchronized void updateItems()
{
	for (TradeItem titem : _items)
	{
		L2ItemInstance item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
		if (item == null || titem.getCount() < 1)
			removeItem(titem.getObjectId(), -1, -1);
		else if (item.getCount() < titem.getCount())
			titem.setCount(item.getCount());
	}
}

/**
 * Lockes TradeList, no further changes are allowed
 */
public void lock()
{
	_locked = true;
}

/**
 * Clears item list
 */
public synchronized void clear()
{
	_items.clear();
	_locked = false;
}

/**
 * Confirms TradeList
 * @return : boolean
 */
public boolean confirm()
{
	if (_confirmed)
		return true; // Already confirmed
	
	// If Partner has already confirmed this trade, proceed exchange
	if (_partner != null)
	{
		TradeList partnerList = _partner.getActiveTradeList();
		if (partnerList == null)
		{
			_log.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
			return false;
		}
		
		// Synchronization order to avoid deadlock
		TradeList sync1, sync2;
		if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
		{
			sync1 = partnerList; sync2 = this;
		}
		else
		{
			sync1 = this; sync2 = partnerList;
		}
		
		synchronized (sync1)
		{
			synchronized (sync2)
			{
				_confirmed = true;
				if (partnerList.isConfirmed())
				{
					partnerList.lock();
					lock();
					if (!partnerList.validate())
						return false;
					if (!validate())
						return false;
					
					doExchange(partnerList);
				}
				else _partner.onTradeConfirm(_owner);
			}
		}
	}
	else _confirmed = true;
	
	return _confirmed;
}

/**
 * Cancels TradeList confirmation
 */
public void invalidateConfirmation()
{
	_confirmed = false;
}

/**
 * Validates TradeList with owner inventory
 */
private boolean validate()
{
	// Check for Owner validity
	if (_owner == null || L2World.getInstance().findObject(_owner.getObjectId()) == null)
	{
		_log.warning("Invalid owner of TradeList");
		return false;
	}
	
	// Check for Item validity
	for (TradeItem titem : _items)
	{
		L2ItemInstance item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
		if (item == null || item.getCount() < 1)
		{
			_log.warning(_owner.getName() + ": Invalid Item in TradeList");
			return false;
		}
	}
	
	return true;
}

/**
 * Transfers all TradeItems from inventory to partner
 */
private boolean TransferItems(L2PcInstance partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
{
	boolean ownerGM = false;
	
	if (getOwner() != null && getOwner().isGM() && !partner.isGM())
		ownerGM = true;
	
	for (TradeItem titem : _items)
	{
		L2ItemInstance oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
		if (oldItem == null)
			return false;
		L2ItemInstance newItem = _owner.getInventory().transferItem("Trade", titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
		if (newItem == null)
			return false;
		
		if (ownerGM && !newItem.isStackable())
		{
			final long untTime = newItem.getUntradeableTime();
			
			if (untTime < 9999999900000L)
			{
				final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_GM_TRADE*60*60*1000);
				
				if (untTime + 3600000 < newTime)
				{
					newItem.setUntradeableTimer(newTime);
				}
			}
		}
		
		// Add changes to inventory update packets
		if (ownerIU != null)
		{
			if (oldItem.getCount() > 0 && oldItem != newItem)
				ownerIU.addModifiedItem(oldItem);
			else ownerIU.addRemovedItem(oldItem);
		}
		
		if (partnerIU != null)
		{
			if (newItem.getCount() > titem.getCount())
				partnerIU.addModifiedItem(newItem);
			else partnerIU.addNewItem(newItem);
		}
	}
	
	return true;
}

/**
 * Count items slots
 */
public int countItemsSlots(L2PcInstance partner)
{
	int slots = 0;
	
	for (TradeItem item : _items)
	{
		if (item == null)
			continue;
		L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
		if (template == null)
			continue;
		if (!template.isStackable())
			slots += item.getCount();
		else if (partner.getInventory().getItemByItemId(item.getItem().getItemId()) == null)
			slots++;
	}
	
	return slots;
}

/**
 * Calc weight of items in tradeList
 */

public int calcItemsWeight()
{
	int weight = 0;
	
	for (TradeItem item : _items)
	{
		if (item == null) continue;
		L2Item template = ItemTable.getInstance().getTemplate(item.getItem().getItemId());
		if (template == null) continue;
		weight += item.getCount() * template.getWeight();
	}
	
	return weight;
}

/**
 * Proceeds with trade
 */
private void doExchange(TradeList partnerList)
{
	boolean success = false;
	
	// check weight and slots
	/*		if ((!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight()))
				|| !(partnerList.getOwner().getInventory().validateWeight(calcItemsWeight())))
		{
			partnerList.getOwner().sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			getOwner().sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
		}
		else*/ if ((!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner())))
				|| (!partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner()))))
		{
			partnerList.getOwner().sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			getOwner().sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
		}
		else
		{
			// Prepare inventory update packet
			InventoryUpdate ownerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
			InventoryUpdate partnerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
			
			// Transfer items
			partnerList.TransferItems(getOwner(), partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
			
			// Send inventory update packet
			if (ownerIU != null)
				_owner.sendPacket(ownerIU);
			else _owner.sendPacket(new ItemList(_owner, false));
			
			if (partnerIU != null)
				_partner.sendPacket(partnerIU);
			else _partner.sendPacket(new ItemList(_partner, false));
			
			success = true;
		}
		// Finish the trade
		partnerList.getOwner().onTradeFinish(success);
		getOwner().onTradeFinish(success);
}

/**
 * Buy items from this PrivateStore list
 * @return : boolean true if success
 */
public synchronized boolean privateStoreBuy(L2PcInstance player, ItemRequest[] items)
{
	if (_locked)
		return false;
	
	if (!validate())
	{
		lock();
		return false;
	}
	
	int slots = 0;
	long totalPrice = 0;
	
	final PcInventory ownerInventory = _owner.getInventory();
	final PcInventory playerInventory = player.getInventory();
	
	for (ItemRequest item : items)
	{
		boolean found = false;
		
		for (TradeItem ti : _items)
		{
			if (ti.getObjectId() == item.getObjectId())
			{
				if (ti.getPrice() == item.getPrice())
				{
					if (ti.getCount() < item.getCount())
						item.setCount(ti.getCount());
					found = true;
				}
				break;
			}
		}
		// item with this objectId and price not found in tradelist
		if (!found)
		{
			item.setCount(0);
			continue;
		}
		
		// check for overflow in the single item
		if ((MAX_ADENA / item.getCount()) < item.getPrice())
		{
			// private store attempting to overflow - disable it
			lock();
			return false;
		}
		
		totalPrice += item.getCount() * item.getPrice();
		// check for overflow of the total price
		if (MAX_ADENA < totalPrice	|| totalPrice < 0)
		{
			// private store attempting to overflow - disable it
			lock();
			return false;
		}
		
		// Check if requested item is available for manipulation
		L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
		
		if (oldItem == null || !oldItem.isTradeable())
		{
			// private store sell invalid item - disable it
			lock();
			return false;
		}
		
		L2Item template = ItemTable.getInstance().getTemplate(item.getItemId());
		if (template == null)
			continue;
		if (!template.isStackable())
			slots += item.getCount();
		else if (playerInventory.getItemByItemId(item.getItemId()) == null)
			slots++;
	}
	
	if (totalPrice > playerInventory.getBlueEva())
	{
		player.sendMessage("You do not have enough Blue Evas");
		return false;
	}
	
	if (!playerInventory.validateCapacity(slots))
	{
		player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
		return false;
	}
	
	// Prepare inventory update packets
	final InventoryUpdate ownerIU = new InventoryUpdate();
	final InventoryUpdate playerIU = new InventoryUpdate();
	
	final L2ItemInstance blueEva = playerInventory.destroyItemByItemId("PrivateStore Buy", 4355, totalPrice, player, _owner);
	
	if (blueEva == null)
	{
		_log.warning("LOL WTF: private store buy of "+player.getName()+" buying from "+_owner.getName()+" is null!");
		return false;
	}
	
	playerIU.addItem(blueEva);
	
	ownerIU.addItem(ownerInventory.addItem("PrivateStore Buy", 4355, totalPrice, _owner, player));
	
	boolean ok = true;
	
	// Transfer items
	for (ItemRequest item : items)
	{
		if (item.getCount() == 0)
			continue;
		
		// Check if requested item is available for manipulation
		L2ItemInstance oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
		if (oldItem == null)
		{
			// should not happens - validation already done
			lock();
			ok = false;
			break;
		}
		
		// Proceed with item transfer
		L2ItemInstance newItem = ownerInventory.transferItem("PrivateStore Buy", item.getObjectId(), item.getCount(), playerInventory, _owner, player);
		if (newItem == null)
		{
			ok = false;
			break;
		}
		removeItem(item.getObjectId(), -1, item.getCount());
		
		// Add changes to inventory update packets
		if (oldItem.getCount() > 0 && oldItem != newItem)
			ownerIU.addModifiedItem(oldItem);
		else ownerIU.addRemovedItem(oldItem);
		if (newItem.getCount() > item.getCount())
			playerIU.addModifiedItem(newItem);
		else playerIU.addNewItem(newItem);
		
		// Send messages about the transaction to both players
		if (newItem.isStackable())
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
			msg.addString(player.getName());
			msg.addItemName(newItem);
			msg.addItemNumber(item.getCount());
			_owner.sendPacket(msg);
			
			msg = new SystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_C1);
			msg.addString(_owner.getName());
			msg.addItemName(newItem);
			msg.addItemNumber(item.getCount());
			player.sendPacket(msg);
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S2);
			msg.addString(player.getName());
			msg.addItemName(newItem);
			_owner.sendPacket(msg);
			
			msg = new SystemMessage(SystemMessageId.PURCHASED_S2_FROM_C1);
			msg.addString(_owner.getName());
			msg.addItemName(newItem);
			player.sendPacket(msg);
		}
	}
	
	// Send inventory update packet
	_owner.sendPacket(ownerIU);
	player.sendPacket(playerIU);
	return ok;
}

/**
 * Sell items to this PrivateStore list
 * @return : boolean true if success
 */
public synchronized boolean privateStoreSell(L2PcInstance player, ItemRequest[] items)
{
	if (_locked)
		return false;
	
	boolean ok = false;
	
	final PcInventory ownerInventory = _owner.getInventory();
	final PcInventory playerInventory = player.getInventory();
	
	// Prepare inventory update packet
	final InventoryUpdate ownerIU = new InventoryUpdate();
	final InventoryUpdate playerIU = new InventoryUpdate();
	
	long totalPrice = 0;
	
	for (ItemRequest item : items)
	{
		// searching item in tradelist using itemId
		boolean found = false;
		
		for (TradeItem ti : _items)
		{
			if (ti.getItem().getItemId() == item.getItemId())
			{
				// price should be the same
				if (ti.getPrice() == item.getPrice())
				{
					// if requesting more than available - decrease count
					if (ti.getCount() < item.getCount())
						item.setCount(ti.getCount());
					found = item.getCount() > 0;
				}
				break;
			}
		}
		// not found any item in the tradelist with same itemId and price
		// maybe another player already sold this item ?
		if (!found)
			continue;
		
		// check for overflow in the single item
		if ((MAX_ADENA / item.getCount()) < item.getPrice())
		{
			lock();
			break;
		}
		
		long _totalPrice = totalPrice + item.getCount() * item.getPrice();
		// check for overflow of the total price
		if (MAX_ADENA < _totalPrice	|| _totalPrice < 0)
		{
			lock();
			break;
		}
		
		if (ownerInventory.getBlueEva() < _totalPrice)
			continue;
		
		// Check if requested item is available for manipulation
		int objectId = item.getObjectId();
		L2ItemInstance oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
		// private store - buy use same objectId for buying several non-stackable items
		if (oldItem == null)
		{
			// searching other items using same itemId
			oldItem = playerInventory.getItemByItemId(item.getItemId());
			if (oldItem == null)
				continue;
			objectId = oldItem.getObjectId();
			oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
			if (oldItem == null)
				continue;
		}
		
		if (!oldItem.isTradeable())
			continue;
		
		// Proceed with item transfer
		L2ItemInstance newItem = playerInventory.transferItem("PrivateStore Sell", objectId, item.getCount(), ownerInventory, player, _owner);
		if (newItem == null)
			continue;
		
		removeItem(-1, item.getItemId(), item.getCount());
		ok = true;
		
		// increase total price only after successful transaction
		totalPrice = _totalPrice;
		
		// Add changes to inventory update packets
		if (oldItem.getCount() > 0 && oldItem != newItem)
			playerIU.addModifiedItem(oldItem);
		else playerIU.addRemovedItem(oldItem);
		if (newItem.getCount() > item.getCount())
			ownerIU.addModifiedItem(newItem);
		else ownerIU.addNewItem(newItem);
		
		// Send messages about the transaction to both players
		if (newItem.isStackable())
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.PURCHASED_S3_S2_S_FROM_C1);
			msg.addString(player.getName());
			msg.addItemName(newItem);
			msg.addItemNumber(item.getCount());
			_owner.sendPacket(msg);
			
			msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
			msg.addString(_owner.getName());
			msg.addItemName(newItem);
			msg.addItemNumber(item.getCount());
			player.sendPacket(msg);
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.PURCHASED_S2_FROM_C1);
			msg.addString(player.getName());
			msg.addItemName(newItem);
			_owner.sendPacket(msg);
			
			msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S2);
			msg.addString(_owner.getName());
			msg.addItemName(newItem);
			player.sendPacket(msg);
		}
	}
	
	if (totalPrice > 0)
	{
		if (totalPrice > ownerInventory.getBlueEva())
			// should not happens, just a precaution
			return false;
		
		final L2ItemInstance blueEva = ownerInventory.destroyItemByItemId("PrivateStore Sell", 4355, totalPrice, _owner, player);
		
		if (blueEva == null)
		{
			_log.warning("LOL WTF: private store buy of "+player.getName()+" buying from "+_owner.getName()+" is null!");
			return false;
		}
		
		ownerIU.addItem(blueEva);
		playerIU.addItem(playerInventory.addItem("PrivateStore Sell", 4355, totalPrice, player, _owner));
	}
	
	if (ok)
	{
		// Send inventory update packet
		_owner.sendPacket(ownerIU);
		player.sendPacket(playerIU);
	}
	return ok;
}
}
