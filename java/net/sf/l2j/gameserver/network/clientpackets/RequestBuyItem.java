package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.L2TradeList.L2TradeItem;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2CastleChamberlainInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MercManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetManagerInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.12.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestBuyItem extends L2GameClientPacket
{
private static final String _C__1F_REQUESTBUYITEM = "[C] 1F RequestBuyItem";
private static Logger _log = Logger.getLogger(RequestBuyItem.class.getName());

private static final int BATCH_LENGTH = 12; // length of the one item

private int _listId;
private Item[] _items = null;

@Override
protected void readImpl()
{
	_listId = readD();
	int count = readD();
	if(count <= 0
			|| count > Config.MAX_ITEM_IN_PACKET
			|| count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	
	_items = new Item[count];
	for (int i = 0; i < count; i++)
	{
		int itemId = readD();
		long cnt = readQ();
		if (itemId < 1 || cnt < 1)
		{
			_items = null;
			return;
		}
		_items[i] = new Item(itemId, cnt);
	}
}

@Override
protected void runImpl()
{
	final L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if(_items == null)
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Alt game - Karma punishment
	if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
		return;
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	boolean gem = false;
	
	L2TradeList list = TradeController.getInstance().getBuyList(_listId);
	
	if (list == null) return;
	if (list.isGemShop()) gem = true;
	
	L2Object target = null;
	L2Character merchant = null;
	String htmlFolder = "";
	
	if (!gem)
	{
		target = player.getTarget();
		
		if (!player.isGM() && (target == null // No target (ie GM Shop)
				|| !(target instanceof L2MerchantInstance
						|| target instanceof L2MerchantSummonInstance
						|| target instanceof L2FishermanInstance
						|| target instanceof L2MercManagerInstance
						|| target instanceof L2ClanHallManagerInstance
						|| target instanceof L2CastleChamberlainInstance) // Target not a merchant, fisherman or mercmanager
						|| !player.isInsideRadius(target, INTERACTION_DISTANCE, true, false))) // Distance is too far
			return;
		
		boolean ok = true;
		
		if (target != null)
		{
			if (target instanceof L2FishermanInstance)
			{
				htmlFolder = "fisherman";
			}
			else if (target instanceof L2MerchantInstance
					|| target instanceof L2MerchantSummonInstance)
			{
				htmlFolder = "merchant";
			}
			else if (target instanceof L2PetManagerInstance)
			{
				htmlFolder = "petmanager";
			}
			else
			{
				ok = false;
			}
		}
		else
		{
			ok = false;
		}
		
		if (ok)
		{
			merchant = (L2Character)target;
		}
		else if (!player.isGM())
		{
			player.sendMessage("Invalid Target: Seller must be targetted");
			return;
		}
		
		if (merchant != null)
		{
			List<L2TradeList> lists;
			
			if (merchant instanceof L2MerchantSummonInstance)
				lists = TradeController.getInstance().getBuyListByNpcId(((L2MerchantSummonInstance)merchant).getNpcId());
			else
				lists = TradeController.getInstance().getBuyListByNpcId(((L2MerchantInstance)merchant).getNpcId());
			
			if(!player.isGM())
			{
				if (lists == null)
				{
					Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
					return;
				}
				for (L2TradeList tradeList : lists)
				{
					if (tradeList.getListId() == _listId)
						list = tradeList;
				}
			}
			else
				list = TradeController.getInstance().getBuyList(_listId);
		}
		else
		{
			if (player.isGM())
				list = TradeController.getInstance().getBuyList(_listId);
		}
	}
	
	if (list == null)
	{
		Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
		return;
	}
	
	if (list.getNpcId().startsWith("gm") && !list.isGemShop() && !player.isGM())
	{
		Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName()
				+ " sent a modified packet to buy from gmshop.", Config.DEFAULT_PUNISH);
		return;
	}
	
	_listId = list.getListId();
	
	double castleTaxRate = 0;
	double baseTaxRate = 0;
	if (merchant != null && merchant instanceof L2MerchantInstance)
	{
		castleTaxRate = ((L2MerchantInstance)merchant).getMpc().getCastleTaxRate();
		baseTaxRate = ((L2MerchantInstance)merchant).getMpc().getBaseTaxRate();
	}
	long subTotal = 0;
	long castleTax = 0;
	long baseTax = 0;
	
	// Check for buylist validity and calculates summary values
	long slots = 0;
	for (Item i : _items)
	{
		long price = -1;
		
		L2TradeItem tradeItem = list.getItemById(i.getItemId());
		if (tradeItem == null)
		{
			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
			return;
		}
		
		L2Item template = ItemTable.getInstance().getTemplate(i.getItemId());
		if (template == null)
			continue;
		
		if (!template.isStackable() && i.getCount() > 1)
		{
			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase invalid quantity of items at the same time.",Config.DEFAULT_PUNISH);
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
			sendPacket(sm);
			sm = null;
			return;
		}
		
		price = list.getPriceForItemId(i.getItemId());
		if (i.getItemId() >= 3960 && i.getItemId() <= 4026)
			price *= Config.RATE_SIEGE_GUARDS_PRICE;
		
		if (price < 0)
		{
			_log.warning("ERROR, no price found .. wrong buylist ??");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (price == 0 && !player.isGM() && Config.ONLY_GM_ITEMS_FREE)
		{
			player.sendMessage("Ohh Cheat dont work? You have a problem now!");
			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried buy item for 0 adena.", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (tradeItem.hasLimitedStock())
		{
			// trying to buy more then avaliable
			if (i.getCount() > tradeItem.getCurrentCount())
				return;
		}
		
		if ((MAX_ADENA / i.getCount()) < price)
		{
			/*			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+MAX_ADENA+" adena worth of goods.", Config.DEFAULT_PUNISH);
			 */			return;
		}
		subTotal += i.getCount() * price;	// Before tax
		castleTax = (long)(subTotal * castleTaxRate);
		baseTax = (long)(subTotal * baseTaxRate);
		if (subTotal + castleTax + baseTax > MAX_ADENA)
		{
			/*			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+MAX_ADENA+" adena worth of goods.", Config.DEFAULT_PUNISH);
			 */			return;
		}
		
		if (!template.isStackable())
			slots += i.getCount();
		else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
			slots++;
	}
	
	if (slots > Integer.MAX_VALUE || slots < 0 || !player.getInventory().validateCapacity((int)slots))
	{
		sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
		return;
	}
	
	// Charge buyer and add tax to castle treasury if not owned by npc clan
	if ((subTotal < 0) || !player.reduceAdena("Buy", (subTotal + baseTax + castleTax), player.getLastFolkNPC(), false))
	{
		sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
		return;
	}
	
	// Proceed the purchase
	for (Item i : _items)
	{
		L2TradeItem tradeItem = list.getItemById(i.getItemId());
		if (tradeItem == null)
		{
			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
			continue;
		}
		
		if (tradeItem.hasLimitedStock())
		{
			if (tradeItem.decreaseCount(i.getCount()))
				player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
		}
		else
			player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
	}
	
	if (merchant != null)
	{
		String html;
		// add to castle treasury
		if (merchant instanceof L2MerchantInstance)
		{
			((L2MerchantInstance)merchant).getCastle().addToTreasury(castleTax);
			html = HtmCache.getInstance().getHtm("data/html/"+ htmlFolder +"/" + ((L2MerchantInstance)merchant).getNpcId() + "-bought.htm");
		}
		else
			html = HtmCache.getInstance().getHtm("data/html/"+ htmlFolder +"/" + ((L2MerchantSummonInstance)merchant).getNpcId() + "-bought.htm");
		
		if (html != null)
		{
			NpcHtmlMessage boughtMsg = new NpcHtmlMessage(merchant.getObjectId());
			boughtMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())));
			player.sendPacket(boughtMsg);
		}
	}
	
	player.sendPacket(new ItemList(player, true));
}

private class Item
{
private final int _itemId;
private final long _count;

public Item(int id, long num)
{
	_itemId = id;
	_count = num;
}

public int getItemId()
{
	return _itemId;
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
	return _C__1F_REQUESTBUYITEM;
}
}
