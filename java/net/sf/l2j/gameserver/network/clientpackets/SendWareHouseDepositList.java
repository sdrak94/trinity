package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ClanWarehouse;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * 31  SendWareHouseDepositList  cd (dd)
 *
 * @version $Revision: 1.3.4.5 $ $Date: 2005/04/11 10:06:09 $
 */
public final class SendWareHouseDepositList extends L2GameClientPacket
{
private static final String _C__31_SENDWAREHOUSEDEPOSITLIST = "[C] 31 SendWareHouseDepositList";
private static Logger _log = Logger.getLogger(SendWareHouseDepositList.class.getName());

private static final int BATCH_LENGTH = 12; // length of the one item

private WarehouseItem _items[] = null;

@Override
protected void readImpl()
{
	final int count = readD();
	if (count <= 0
			|| count > Config.MAX_ITEM_IN_PACKET
			|| count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	
	_items = new WarehouseItem[count];
	for (int i=0; i < count; i++)
	{
		int objId = readD();
		long cnt = readQ();
		if (objId < 1 || cnt < 0)
		{
			_items = null;
			return;
		}
		_items[i] = new WarehouseItem(objId, cnt);
	}
}

@Override
protected void runImpl()
{
	if (_items == null)
		return;
	
	final L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if (!player.getFloodProtectors().getTransaction().tryPerformAction("deposit"))
	{
		player.sendMessage("You depositing items too fast.");
		return;
	}
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	final ItemContainer warehouse = player.getActiveWarehouse();
	if (warehouse == null)
		return;
	if (warehouse instanceof ClanWarehouse)
	{
		if (!Config.ALLOW_CLAN_WAREHOUSE)
		{
			player.sendMessage("Clan Warehouse is disabled.");
			return;
		}
	}
	final boolean isPrivate = warehouse instanceof PcWarehouse;
	
	final L2Npc manager = player.getLastFolkNPC();
	if ((manager == null
			|| !manager.isWarehouse()
			|| !manager.canInteract(player)) && !player.isGM())
		return;
	
	if (!isPrivate && !player.getAccessLevel().allowTransaction())
	{
		player.sendMessage("Transactions are disable for your Access Level");
		return;
	}
	
	if (player.getActiveEnchantItem() != null || player.isProcessingTransaction())
	{
		Util.handleIllegalPlayerAction(player,"Player "+player.getName()+" tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
		return;
	}
	
	// Alt game - Karma punishment
	if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
		return;
	
	// Freight price from config or normal price per item slot (30)
	final long fee = _items.length * 30;
	long currentAdena = player.getAdena();
	int slots = 0;
	
	for (WarehouseItem i : _items)
	{
		L2ItemInstance item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "deposit");
		if (item == null)
		{
			_log.warning("Error depositing a warehouse object for char "+player.getName()+" (validity check)");
			return;
		}
		
		// Calculate needed adena and slots
		if (item.getItemId() == ADENA_ID)
			currentAdena -= i.getCount();
		if (!item.isStackable())
			slots += i.getCount();
		else if (warehouse.getItemByItemId(item.getItemId()) == null)
			slots++;
	}
	
	// Item Max Limit Check
	if (!warehouse.validateCapacity(slots))
	{
		sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
		return;
	}
	
	// Check if enough adena and charge the fee
	if (currentAdena < fee || !player.reduceAdena(warehouse.getName(), fee, manager, false))
	{
		sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
		return;
	}
	
	// get current tradelist if any
	if (player.getActiveTradeList() != null)
		return;
	
	// Proceed to the transfer
	InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
	for (WarehouseItem i : _items)
	{
		// Check validity of requested item
		L2ItemInstance oldItem = player.checkItemManipulation(i.getObjectId(), i.getCount(), "deposit");
		if (oldItem == null)
		{
			_log.warning("Error depositing a warehouse object for char "+player.getName()+" (olditem == null)");
			return;
		}
		
		if (!oldItem.isDepositable(isPrivate) || !oldItem.isAvailable(player, true, isPrivate))
			continue;
		
		final L2ItemInstance newItem = player.getInventory().transferItem(warehouse.getName(), i.getObjectId(), i.getCount(), warehouse, player, manager);
		if (newItem == null)
		{
			_log.warning("Error depositing a warehouse object for char "+player.getName()+" (newitem == null)");
			continue;
		}
		
		if (playerIU != null)
		{
			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
		}
	}
	
	// Send updated item list to the player
	if (playerIU != null)
		player.sendPacket(playerIU);
	else
		player.sendPacket(new ItemList(player, false));
}

private class WarehouseItem
{
private final int _objectId;
private final long _count;

public WarehouseItem(int id, long num)
{
	_objectId = id;
	_count = num;
}

public int getObjectId()
{
	return _objectId;
}

public long getCount()
{
	return _count;
}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__31_SENDWAREHOUSEDEPOSITLIST;
}
}
