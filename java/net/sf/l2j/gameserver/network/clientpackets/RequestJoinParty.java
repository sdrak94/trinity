package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinParty extends L2GameClientPacket
{
private static final String _C__29_REQUESTJOINPARTY = "[C] 29 RequestJoinParty";
private static Logger _log = Logger.getLogger(RequestJoinParty.class.getName());

private String _name;
private int _itemDistribution;

@Override
protected void readImpl()
{
	_name = readS();
	_itemDistribution = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance requestor = getClient().getActiveChar();
	L2PcInstance target = L2World.getInstance().getPlayer(_name);
	
	if (requestor == null)
		return;
	
	if (target == null)
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}

	if (requestor.cannotMakePartyActions())
	{
		requestor.sendMessage("You have running instance where parties are not allowed");
		return;
	}
	if (target.cannotMakePartyActions())
	{
		requestor.sendMessage("Target has a running instance where parties are not allowed");
		return;
	}
	
	if (target == requestor)
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
		return;
	}
	if (target.isInvisible() && !requestor.isGM())
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}
	
	if (target.getAccessLevel().getLevel() > requestor.getAccessLevel().getLevel() || requestor.isInJail())
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}
	
	if (target.isInParty())
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ALREADY_IN_PARTY);
		msg.addString(target.getName());
		requestor.sendPacket(msg);
		return;
	}
	
	if (!requestor.isGM())
	{
		if (BlockList.isInBlockList(target, requestor))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
			sm.addCharName(target);
			requestor.sendPacket(sm);
			return;
		}
		if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped())
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		if (target.isInJail() || requestor.isInJail())
		{
			requestor.sendMessage("Player is in Jail");
			return;
		}
		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
			return;
		
		if (target.isInDuel() || requestor.isInDuel())
			return;
		
		if (requestor.isInOrcVillage() || target.isInOrcVillage())
		{
			requestor.sendMessage("Target is in Orc Village");
			return;
		}

		if (requestor.cannotMakePartyActions() || target.cannotMakePartyActions())
		{
			return;
		}
		if (requestor.isInHuntersVillage() || target.isInHuntersVillage())
		{
			requestor.sendMessage("Target is in Hunter's Village");
			return;
		}
		if (DM._started && (requestor._inEventDM || target._inEventDM))
		{
			requestor.sendMessage("No parties in DM");
			return;
		}
		if (NewDM._started && requestor._inEventDM)
		{
			requestor.sendMessage("No parties in DM");
			return;
		}
		if (NewDM._started && target._inEventDM)
		{
			requestor.sendMessage("No parties in DM");
			return;
		}
	}
	
	if (target.getClient().isDetached())
	{
		requestor.sendMessage("Player is in offline mode.");
		return;
	}
	
	if (target.getTradeRefusal() && !requestor.isGM())
	{
		if (target.getClanId() != 0 && requestor.getClanId() != 0) // both have clans
		{
			if (target.getAllyId() != 0 && target.getAllyId() == requestor.getAllyId()) // both in the same ally
			{  // do nothing
			}
			else //not in same ally
			{
				if (target.getClanId() == requestor.getClanId()) // but in same clan
				{ // do nothing
				}
				else
				{
					requestor.sendMessage("Target is in interaction refusal mode");
					return;
				}
			}
		}
		else
		{
			requestor.sendMessage("Target is in interaction refusal mode");
			return;
		}
	}
	
	if (!requestor.isInParty())     //Asker has no party
	{
		createNewParty(target, requestor);
	}
	else                            //Asker is in party
	{
		if(requestor.getParty().isInDimensionalRift())
		{
			requestor.sendMessage("You can't invite a player when in Dimensional Rift.");
		}
		else
		{
			addTargetToParty(target, requestor);
		}
	}
}

/**
 * @param client
 * @param itemDistribution
 * @param target
 * @param requestor
 */
public static void addTargetToParty(L2PcInstance target, L2PcInstance requestor)
{
	if (requestor.getParty() == null)
	{
		return;
	}
	
	SystemMessage msg;
	
	// summary of ppl already in party and ppl that get invitation
	if (!requestor.getParty().isLeader(requestor))
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEADER_CAN_INVITE));
		return;
	}
	if (requestor.getParty() != null && requestor.getParty().getMemberCount() >= 10 )
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
		return;
	}
	
	if(requestor._inEventDM && NewDM._started)
		return;
	
	if (requestor.getParty().isInvitingAnother())
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.WAITING_FOR_ANOTHER_REPLY));
		return;
	}
	if (requestor.getParty() == null)
		return;

	if (requestor.cannotMakePartyActions() || target.cannotMakePartyActions())
	{
		return;
	}
	if (!target.isProcessingRequest())
	{
		requestor.onTransactionRequest(target);
		// in case a leader change has happened, use party's mode
		target.sendPacket(new AskJoinParty(requestor.getName(), requestor.getParty().getLootDistribution()));
		requestor.getParty().onInviteRequest();
		
		msg = new SystemMessage(SystemMessageId.C1_INVITED_TO_PARTY);
		msg.addString(target.getName());
		requestor.sendPacket(msg);
		
		if (Config.DEBUG)
			_log.fine("sent out a party invitation to:"+target.getName());
	}
	else
	{
		msg = new SystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
		msg.addString(target.getName());
		requestor.sendPacket(msg);
		
		if (Config.DEBUG)
			_log.warning(requestor.getName() + " already received a party invitation");
	}
	msg = null;
}


/**
 * @param client
 * @param itemDistribution
 * @param target
 * @param requestor
 */
private void createNewParty(L2PcInstance target, L2PcInstance requestor)
{
	if (NewDM._started && (requestor._inEventDM || target._inEventDM))
	{
		requestor.sendMessage("No parties in DM");
		return;
	}
	if (!target.isProcessingRequest())
	{
		requestor.setParty(new L2Party(requestor, _itemDistribution));
		
		requestor.onTransactionRequest(target);
		target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
		requestor.getParty().onInviteRequest();
		
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_INVITED_TO_PARTY);
		msg.addString(target.getName());
		requestor.sendPacket(msg);
		
		if (Config.DEBUG)
			_log.fine("sent out a party invitation to:"+target.getName());
	}
	else
	{
		requestor.sendPacket(new SystemMessage(SystemMessageId.WAITING_FOR_ANOTHER_REPLY));
		
		if (Config.DEBUG)
			_log.warning(requestor.getName() + " already received a party invitation");
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__29_REQUESTJOINPARTY;
}
}
