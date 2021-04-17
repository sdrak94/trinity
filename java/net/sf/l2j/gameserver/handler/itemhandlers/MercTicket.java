package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class MercTicket implements IItemHandler
{
	/**
	 * handler for using mercenary tickets.  Things to do:
	 * 1) Check constraints:
	 * 1.a) Tickets may only be used in a castle
	 * 1.b) Only specific tickets may be used in each castle (different tickets for each castle)
	 * 1.c) only the owner of that castle may use them
	 * 1.d) tickets cannot be used during siege
	 * 1.e) Check if max number of tickets has been reached
	 * 1.f) Check if max number of tickets from this ticket's TYPE has been reached
	 * 2) If allowed, call the MercTicketManager to add the item and spawn in the world
	 * 3) Remove the item from the person's inventory
	 */
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		int itemId = item.getItemId();
		L2PcInstance activeChar = (L2PcInstance) playable;
		Castle castle = CastleManager.getInstance().getCastle(activeChar);
		int castleId = -1;
		if (castle != null)
			castleId = castle.getCastleId();

		//add check that certain tickets can only be placed in certain castles
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE));
			return;
		}
		
		if (!activeChar.isCastleLord(castleId))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES));
			return;
		}
		
		if (castle.getSiege().getIsInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
			return;
         }
 
        //Checking Seven Signs Quest Period
        if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION) 
        {
        	//_log.warning("Someone has tried to spawn a guardian during Quest Event Period of The Seven Signs.");       	
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
        	return;
        }
        //Checking the Seal of Strife status
        switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
        {
        	case SevenSigns.CABAL_NULL:
        		if (SevenSigns.getInstance().CheckIsDawnPostingTicket(itemId))
        		{
                	//_log.warning("Someone has tried to spawn a Dawn Mercenary though the Seal of Strife is not controlled by anyone.");       	
        			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));        		
        			return;
        		}        			
        		break;
        	case SevenSigns.CABAL_DUSK:
        		if (!SevenSigns.getInstance().CheckIsRookiePostingTicket(itemId))
        		{
                	//_log.warning("Someone has tried to spawn a non-Rookie Mercenary though the Seal of Strife is controlled by Revolutionaries of Dusk.");       	
        			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
        			return;
        		}          		
        		break;
        	case SevenSigns.CABAL_DAWN:        	
        		break;
        }                
        
        if(MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()))
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
			return;
		}
		
		if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
			return;
		}
		if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT));
			return;
		}
		
		MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, null);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
		activeChar.sendPacket(new SystemMessage(SystemMessageId.PLACE_CURRENT_LOCATION_DIRECTION).addItemName(item));
	}
}
