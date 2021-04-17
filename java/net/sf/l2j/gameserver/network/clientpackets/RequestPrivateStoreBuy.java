package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ItemRequest;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.TradeList.TradeItem;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.util.Util;


public final class RequestPrivateStoreBuy extends L2GameClientPacket
{
private static final String _C__79_REQUESTPRIVATESTOREBUY = "[C] 79 RequestPrivateStoreBuy";
@SuppressWarnings("unused")
private static Logger _log = Logger.getLogger(RequestPrivateStoreBuy.class.getName());

private static final int BATCH_LENGTH = 20; // length of the one item

private int _storePlayerId;
private ItemRequest[] _items = null;

@Override
protected void readImpl()
{
	_storePlayerId = readD();
	int count = readD();
	if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	_items = new ItemRequest[count];
	
	for (int i = 0; i < count; i++)
	{
		int objectId = readD();
		long cnt = readQ();
		long price = readQ();
		
		if (objectId < 1 || cnt < 1 || price < 0)
		{
			_items = null;
			return;
		}
		_items[i] = new ItemRequest(objectId, cnt, price);
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
	
	if (!player.getFloodProtectors().getTransaction().tryPerformAction("privatestorebuy"))
	{
		player.sendMessage("You buying items too fast.");
		return;
	}
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	if (player.isInJail())
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		player.sendMessage("Private stores are prohibited in jail");
		return;
	}
	
	/*if (!player.isGM() && player.getCharCreatedTime() + 24 * 60 * 60 * 1000 > System.currentTimeMillis())
	{
		player.sendMessage("You need to wait 24 hours after making a character to use trade");
		return;
	}*/
	
	L2Object object = L2World.getInstance().findObject(_storePlayerId);
	if (!(object instanceof L2PcInstance))
		return;
	
	if(player.isCursedWeaponEquipped())
		return;
	
	L2PcInstance storePlayer = (L2PcInstance)object;
	if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
		return;
	
	if (player.getInstanceId() != storePlayer.getInstanceId() && player.getInstanceId() != -1)
		return;
	
	if (!(storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
		return;
	
	TradeList storeList = storePlayer.getSellList();
	if (storeList == null)
		return;
	
	if (!player.getAccessLevel().allowTransaction())
	{
		player.sendMessage("Transactions are disable for your Access Level");
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
	{
		if (storeList.getItemCount() > _items.length)
		{
			String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried to buy less items then sold by package-sell, ban this player for bot-usage!";
			Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
			return;
		}
	}
	
	for (TradeItem itam : storeList.getItems())
	{
		try
		{
			if (itam == null || !itam.getItem().isTradeable() || !((L2ItemInstance)L2World.getInstance().findObject(itam.getObjectId())).isTradeable() || itam.getItem().getItemId() == 4355)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	
	if (!storeList.privateStoreBuy(player, _items))
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		/*		_log.warning("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
		 */		return;
	}
	
	if (storeList.getItemCount() == 0)
	{
		storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
		storePlayer.broadcastUserInfo();
	}
}

@Override
public String getType()
{
	return _C__79_REQUESTPRIVATESTOREBUY;
}
}
