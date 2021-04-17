package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TradeDone;


public final class AnswerTradeRequest extends L2GameClientPacket
{
private static final String _C__40_ANSWERTRADEREQUEST = "[C] 40 AnswerTradeRequest";

private int _response;

@Override
protected void readImpl()
{
	_response = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance player = getClient().getActiveChar();
	if (player == null) return;
	
	if (!player.getAccessLevel().allowTransaction())
	{
		player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
		sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	L2PcInstance partner = player.getActiveRequester();
	if (partner == null)
	{
		// Trade partner not found, cancel trade
		player.sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
		player.sendPacket(msg);
		player.setActiveRequester(null);
		msg = null;
		return;
	}
	else if (L2World.getInstance().findObject(partner.getObjectId()) == null)
	{
		// Trade partner not found, cancel trade
		player.sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
		player.sendPacket(msg);
		player.setActiveRequester(null);
		msg = null;
		return;
	}
	
	if (_response == 1)
	{
		if (partner.isProcessingTransaction())
		{
			try
			{
				final L2PcInstance player2 = partner.getActiveTradeList().getPartner();
				GmListTable.broadcastMessageToGMs(partner.getName()+ " just tried using that trade exploit on "+player2.getName()+" with his alt: "+player.getName());
				partner.cancelActiveTrade();
				// Clears requesting status
				player.setActiveRequester(null);
				player2.setActiveRequester(null);
				partner.onTransactionResponse();
				if (!partner.isGM())
					partner.logout();
				//partner.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 30);
				if (!player.isGM())
					player.logout();
				//player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 30);
				_log.severe(partner.getName()+ " just tried using that trade exploit on "+player2.getName()+" with his alt: "+player.getName()+"!!!!");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return;
		}
		if (player.isProcessingTransaction())
		{
			try
			{
				final L2PcInstance player2 = player.getActiveTradeList().getPartner();
				GmListTable.broadcastMessageToGMs(player.getName()+ " just tried using that trade exploit on "+player2.getName()+" with his alt: "+partner.getName());
				player.cancelActiveTrade();
				// Clears requesting status
				partner.setActiveRequester(null);
				player2.setActiveRequester(null);
				partner.onTransactionResponse();
				if (!player.isGM())
					player.logout();
				//player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 30);
				if (!partner.isGM())
					partner.logout();
				//partner.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 30);
				_log.severe(player.getName()+ " just tried using that trade exploit on "+player2.getName()+" with his alt: "+partner.getName()+"!!!!");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return;
		}
		
		if (!partner.isRequestExpired())
		{
			player.startTrade(partner);
			
			if (player.getLevel() <= 85)
			{
				CreatureSay cs = new CreatureSay(player.getObjectId(), Say2.PARTYROOM_COMMANDER, player.getName(), "I am under level 85, are you sure you read my name correctly? >>>"+player.getName()+"<<< if you get scammed by me the GMs won't help!");
				partner.sendPacket(cs);
				partner.sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, player.getName()+" is low level! BEWARE OF SCAMMERS-READ NAME CORRECTLY"));
			}
			if (partner.getLevel() <= 85)
			{
				CreatureSay cs = new CreatureSay(partner.getObjectId(), Say2.PARTYROOM_COMMANDER, partner.getName(), "I am under level 85, are you sure you read my name correctly? >>>"+partner.getName()+"<<< if you get scammed by me the GMs won't help!");
				player.sendPacket(cs);
				player.sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, partner.getName()+" is low level! BEWARE OF SCAMMERS-READ NAME CORRECTLY"));
			}
		}
		else
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.C1_DENIED_TRADE_REQUEST);
			msg.addString(player.getName());
			partner.sendPacket(msg);
			msg = null;
		}
	}
	else
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_DENIED_TRADE_REQUEST);
		msg.addString(player.getName());
		partner.sendPacket(msg);
		msg = null;
	}
	
	// Clears requesting status
	partner.onTransactionResponse();
	player.setActiveRequester(null);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__40_ANSWERTRADEREQUEST;
}
}
