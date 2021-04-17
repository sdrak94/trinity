/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetManagerInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestSellItem extends L2GameClientPacket
{
private static final String _C__1E_REQUESTSELLITEM = "[C] 1E RequestSellItem";
//private static Logger _log = Logger.getLogger(RequestSellItem.class.getName());

private static final int BATCH_LENGTH = 16; // length of the one item

private int _listId;
private Item[] _items = null;

/**
 * packet type id 0x1e
 *
 * sample
 *
 * 1e
 * 00 00 00 00		// list id
 * 02 00 00 00		// number of items
 *
 * 71 72 00 10		// object id
 * ea 05 00 00		// item id
 * 01 00 00 00		// item count
 *
 * 76 4b 00 10		// object id
 * 2e 0a 00 00		// item id
 * 01 00 00 00		// item count
 *
 * format:		cdd (ddd)
 * @param decrypt
 */

@Override
protected void readImpl()
{
	_listId = readD();
	int count = readD();
	if (count <= 0
			|| count > Config.MAX_ITEM_IN_PACKET
			|| count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	
	_items = new Item[count];
	for (int i = 0; i < count; i++)
	{
		int objectId = readD();
		int itemId = readD();
		long cnt = readQ();
		if (objectId < 1 || itemId < 1 || cnt < 1)
		{
			_items = null;
			return;
		}
		_items[i] = new Item(objectId, itemId, cnt);
	}
}

@Override
protected void runImpl()
{
	processSell();
}

protected void processSell()
{
	L2PcInstance player = getClient().getActiveChar();
	
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
	
	if (player.isAccountLockedDown() || player.isInJail())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	L2Object target = player.getTarget();
	if (!player.isGM() && (target == null								// No target (ie GM Shop)
			|| !(target instanceof L2MerchantInstance
					|| target instanceof L2MerchantSummonInstance)	// Target not a merchant
					|| target.getInstanceId() != player.getInstanceId()
					|| !player.isInsideRadius(target, INTERACTION_DISTANCE, true, false))) // Distance is too far
		return;
	
	boolean ok = true;
	String htmlFolder = "";
	
	if (target != null)
	{
		if (target instanceof L2MerchantInstance
				|| target instanceof L2MerchantSummonInstance)
			htmlFolder = "merchant";
		else if (target instanceof L2FishermanInstance)
			htmlFolder = "fisherman";
		else if (target instanceof L2PetManagerInstance)
			htmlFolder = "petmanager";
		else
			ok = false;
	}
	else
		ok = false;
	
	L2Character merchant = null;
	
	if (ok)
		merchant = (L2Character)target;
	
	if (merchant != null && _listId > 1000000) // lease
	{
		int npcId = 0;
		if (merchant instanceof L2MerchantInstance)
			npcId = ((L2MerchantInstance)merchant).getTemplate().npcId;
		else if (merchant instanceof L2MerchantSummonInstance)
			npcId = ((L2MerchantSummonInstance)merchant).getTemplate().npcId;
		if (npcId != _listId-1000000)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	
	long totalPrice = 0;
	// Proceed the sell
	for (Item i : _items)
	{
		L2ItemInstance item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
		if (item == null || (!item.isSellable()))
			continue;
		
		long price = item.getReferencePrice() / 2;
		totalPrice += price * i.getCount();
		if ((MAX_ADENA / i.getCount()) < price || totalPrice > MAX_ADENA)
		{
			/*			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+MAX_ADENA+" adena worth of goods.",  Config.DEFAULT_PUNISH);
			 */			return;
		}
		
		item = player.getInventory().destroyItem("Sell", i.getObjectId(), i.getCount(), player, null);
		
		/* TODO: Disabled until Leaseholders are rewritten ;-)
			int price = item.getReferencePrice()*(int)count/2;
			L2ItemInstance li = null;
			L2ItemInstance la = null;
			if (_listId > 1000000) {
				li = merchant.findLeaseItem(item.getItemId(),item.getEnchantLevel());
				la = merchant.getLeaseAdena();
				if (li == null || la == null) continue;
				price = li.getPriceToBuy()*(int)count; // player sells, thus merchant buys.
				if (price > la.getCount()) continue;
			}
		 */
		/* TODO: Disabled until Leaseholders are rewritten ;-)
				if (item != null && _listId > 1000000) {
					li.setCount(li.getCount()+(int)count);
					li.updateDatabase();
					la.setCount(la.getCount()-price);
					la.updateDatabase();
				}
		 */
	}
	player.addAdena("Sell", totalPrice, merchant, false);
	String html;
	if (merchant instanceof L2MerchantInstance)
		html = HtmCache.getInstance().getHtm("data/html/"+ htmlFolder +"/" + ((L2MerchantInstance)merchant).getNpcId() + "-sold.htm");
	else
		html = null/*HtmCache.getInstance().getHtm("data/html/"+ htmlFolder +"/" + ((L2MerchantSummonInstance)merchant).getNpcId() + "-sold.htm")*/;
	
	if (html != null)
	{
		NpcHtmlMessage soldMsg = new NpcHtmlMessage(merchant.getObjectId());
		soldMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())));
		player.sendPacket(soldMsg);
	}
	
	player.sendPacket(new ItemList(player, true));
}

private class Item
{
private final int _objectId;
private final long _count;

public Item(int objId, int id, long num)
{
	_objectId = objId;
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
	return _C__1E_REQUESTSELLITEM;
}
}