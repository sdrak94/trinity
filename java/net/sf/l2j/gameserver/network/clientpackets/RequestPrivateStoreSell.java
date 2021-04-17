package net.sf.l2j.gameserver.network.clientpackets;
import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.ItemRequest;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;


public final class RequestPrivateStoreSell extends L2GameClientPacket
{
private static final String _C__96_REQUESTPRIVATESTORESELL = "[C] 96 RequestPrivateStoreSell";
private static Logger _log = Logger.getLogger(RequestPrivateStoreSell.class.getName());

private static final int BATCH_LENGTH = 28; // length of the one item

private int _storePlayerId;
private ItemRequest[] _items = null;

@Override
protected void readImpl()
{
	_storePlayerId = readD();
	int count = readD();
	if (count <= 0
			|| count > Config.MAX_ITEM_IN_PACKET
			|| count * BATCH_LENGTH != _buf.remaining())
	{
		return;
	}
	_items = new ItemRequest[count];
	
	for (int i = 0; i < count; i++)
	{
		int objectId = readD();
		int itemId = readD();
		if (itemId == 4355)
		{
			getClient().getActiveChar().sendMessage("You cannot buy Blue Evas from player stores");
			return;
		}
		readH(); //TODO analyse this
		readH(); //TODO analyse this
		long cnt = readQ();
		long price = readQ();
		
		if (objectId < 1 || itemId < 1 || cnt < 1 || price < 0)
		{
			_items = null;
			return;
		}
		_items[i] = new ItemRequest(objectId, itemId, cnt, price);
	}
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if(_items == null)
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	
	if (!player.getFloodProtectors().getTransaction().tryPerformAction("privatestoresell"))
	{
		player.sendMessage("You are selling items too fast");
		return;
	}
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	if (player.isInFunEvent())
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		player.sendMessage("Private stores are prohibited during events");
		return;
	}
	
	if (player.isInJail())
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		player.sendMessage("Private stores are prohibited in jail");
		return;
	}
	
/*	if (!player.isGM() && player.getCharCreatedTime() + 24 * 60 * 60 * 1000 > System.currentTimeMillis())
	{
		player.sendMessage("You need to wait 24 hours after making a character to use trade");
		return;
	}*/
	L2Object object = L2World.getInstance().findObject(_storePlayerId);
	if (!(object instanceof L2PcInstance))
		return;
	
	L2PcInstance storePlayer = (L2PcInstance)object;
	if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
		return;
	
	if (player.getInstanceId() != storePlayer.getInstanceId()
			&& player.getInstanceId() != -1)
		return;
	
	if (storePlayer.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_BUY)
		return;
	
	if(player.isCursedWeaponEquipped())
		return;
	
	TradeList storeList = storePlayer.getBuyList();
	if (storeList == null)
		return;
	
	if (!player.getAccessLevel().allowTransaction())
	{
		player.sendMessage("Transactions are disable for your Access Level");
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (!storeList.privateStoreSell(player, _items))
	{
		sendPacket(ActionFailed.STATIC_PACKET);
		_log.warning("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
		return;
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
	return _C__96_REQUESTPRIVATESTORESELL;
}
}
