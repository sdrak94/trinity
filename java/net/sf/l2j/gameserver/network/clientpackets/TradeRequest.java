package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SendTradeRequest;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public final class TradeRequest extends L2GameClientPacket
{
private static final String TRADEREQUEST__C__15 = "[C] 15 TradeRequest";

private int _objectId;

@Override
protected void readImpl()
{
	_objectId = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null) return;
	
	if (!player.getAccessLevel().allowTransaction())
	{
		player.sendMessage("Transactions are disable for your Access Level");
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	/*if (!player.isGM() && player.getCharCreatedTime() + 24 * 60 * 60 * 1000 > System.currentTimeMillis())
	{
		player.sendMessage("You need to wait 24 hours after making a character to use trade");
		return;
	}*/
	
	if (player.isInvisible())
	{
		player.sendMessage("Go visible");
		return;
	}
	
	L2Object target = L2World.getInstance().findObject(_objectId);
	if (target == null || !player.getKnownList().knowsObject(target)
			|| !(target instanceof L2PcInstance) || (target.getObjectId() == player.getObjectId()))
	{
		player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}
	
	L2PcInstance partner = (L2PcInstance)target;
	
	if (partner.getAccessLevel().getLevel() > player.getAccessLevel().getLevel())
	{
		if (partner.isInvisible() || partner.getBlockList().isBlockAll())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}
		else
		{
			player.sendMessage("You cannot request to trade with those that are mightier than you.");
			return;
		}
	}
	
/*	if (!player.isGM() && partner.getCharCreatedTime() + 24 * 60 * 60 * 1000 > System.currentTimeMillis())
	{
		player.sendMessage("Target's character is too new to do trade (24 hrs)");
		return;
	}*/
	
	if (partner.isInvisible())
	{
		player.sendMessage("Cannot see target");
		return;
	}

	if (partner.getVarB("noTrade"))
	{
		player.sendMessage("That person is in trade refusal mode.");
		return;
	}
	if (player.isDisguised() || partner.isDisguised())
	{
		player.sendMessage("Can't trade when either you or your target is disguised");
		return;
	}
	
	// cant trade with players from other instance except from multiverse
	if (partner.getInstanceId() != player.getInstanceId() && player.getInstanceId() != -1)
		return;
	
	if (partner.getPvpFlag() != 0 || player.getPvpFlag() != 0)
	{
		player.sendMessage("You cannot start trade in PvP mode");
		return;
	}
	
	if (partner.isInOlympiadMode() || player.isInOlympiadMode())
	{
		player.sendMessage("You or your target cant request trade in Olympiad mode");
		return;
	}
	
	if (partner.isInDuel() || player.isInDuel() || player.isInvisible())
	{
		player.sendMessage("You or your target cant request trade in dueling mode");
		return;
	}
	
	// Alt game - Karma punishment
	if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || partner.getKarma() > 0))
	{
		player.sendMessage("Chaotic players can't use Trade.");
		return;
	}
	
	if (player.getPrivateStoreType() != 0 || partner.getPrivateStoreType() != 0)
	{
		player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
		return;
	}
	
	if (player.isInJail() || partner.isInJail())
	{
		player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}
	
	if (player.isProcessingTransaction() || player.isProcessingRequest())
	{
		if (Config.DEBUG) _log.fine("already trading with someone");
		player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_TRADING));
		return;
	}
	
	if (partner.isProcessingTransaction() || partner.isProcessingRequest())
	{
		if (Config.DEBUG) _log.info("transaction already in progress.");
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
		sm.addString(partner.getName());
		player.sendPacket(sm);
		return;
	}
	
	if (partner.getTradeRefusal() && !player.isGM())
	{
		player.sendMessage("Target is in trade refusal mode");
		return;
	}
	
	if (BlockList.isInBlockList(partner, player))
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
		sm.addCharName(partner);
		player.sendPacket(sm);
		return;
	}
	
	if (Util.calculateDistance(player, partner, true) > 150)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.TARGET_TOO_FAR);
		player.sendPacket(sm);
		return;
	}
	
	player.onTransactionRequest(partner);
	partner.sendPacket(new SendTradeRequest(player.getObjectId()));
	SystemMessage sm = new SystemMessage(SystemMessageId.REQUEST_C1_FOR_TRADE);
	sm.addString(partner.getName());
	player.sendPacket(sm);
}

@Override
public String getType()
{
	return TRADEREQUEST__C__15;
}
}
