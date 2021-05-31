package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectionInfo;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.RestartResponse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestRestart extends L2GameClientPacket
{
	private static final String _C__46_REQUESTRESTART = "[C] 46 RequestRestart";
	private static final Logger _log = Logger.getLogger(RequestRestart.class.getName());


	@Override
	protected void readImpl()
	{
		// trigger
	}

    @Override
	protected void runImpl()
    {
        final L2PcInstance player = getClient().getActiveChar();
        
        if (player == null)
        {
/*            _log.warning("[RequestRestart] activeChar null!?");*/
            return;
        }
        
        if(player.getActiveEnchantItem() != null || player.getActiveEnchantAttrItem() != null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
        if (player.isLocked())
		{
			_log.warning("Player " + player.getName() + " tried to restart during class change.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

        if (player.isInsideZone(L2Character.ZONE_EVENT))
		{
			_log.warning("Player " + player.getName() + " tried to restart during event.");
			String msgContent = player.getName() + " tried to restart during event.";
			GmListTable.broadcastToGMs(new CreatureSay(player.getObjectId(), 9, "Event Protection",  msgContent));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
    	if (player.IsCaptchaValidating())
        {
            player.sendMessage("Cannot restart while answering captcha");
            return;
        }
    	if (player.isInRaidEvent())
		{
			player.sendMessage("You are not allowed to restart during Raid Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
    	if (player.isInZombieEvent())
		{
			player.sendMessage("You are not allowed to restart during Zombie Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInKoreanEvent())
		{
			player.sendMessage("You are not allowed to restart during Korean Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInDominationEvent())
		{
			player.sendMessage("You are not allowed to restart during Domination Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInLastManStandingEvent())
		{
			player.sendMessage("You are not allowed to restart during Last Man Standing Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInLastTeamStandingEvent())
		{
			player.sendMessage("You are not allowed to restart during Last Man Standing Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
      if (player.isSubbing())
		{
			_log.warning("Player " + player.getName() + " tried to restart during class change.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
        if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
        {
            player.sendMessage("You cant logout in olympiad mode");
            return;
        }

        if(player.isInFunEvent())
        {
        	player.sendMessage("You cant logout while in this event");
        	return;
        }

        if (player.isTeleporting())
        {
        	player.abortCast();
        	player.setIsTeleporting(false);
        }

        if (player.getPrivateStoreType() != 0)
        {
            player.sendMessage("Cannot restart while trading");
            return;
        }

        if (player.getActiveRequester() != null)
        {
            player.getActiveRequester().onTradeCancel(player);
            player.onTradeCancel(player.getActiveRequester());
        }

        if (!player.isGM())
        {
	        if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) || player.getPvpFlag() != 0)
	        {	
	            player.sendPacket(new SystemMessage(SystemMessageId.CANT_RESTART_WHILE_FIGHTING));
	            player.sendPacket(ActionFailed.STATIC_PACKET);
	            return;
	        }
	        
        }

		/*
		 * if (player.getCpPoints() > 0) { player.clearPath(); }
		 */
        // Prevent player from restarting if they are a festival participant
        // and it is in progress, otherwise notify party members that the player
        // is not longer a participant.
        if (player.isFestivalParticipant())
        {
            if (SevenSignsFestival.getInstance().isFestivalInitialized())
            {
                player.sendMessage("You cannot restart while you are a participant in a festival.");
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            L2Party playerParty = player.getParty();

            if (playerParty != null)
                player.getParty().broadcastToPartyMembers(
                                                          SystemMessage.sendString(player.getName()
                                                              + " has been removed from the upcoming festival."));
        }

        final L2GameClient client = getClient();

        // detach the client from the char so that the connection isnt closed in the deleteMe
        player.setClient(null);

        // removing player from the world
        getClient().setActiveChar(null);
        player.deleteMe();
        
        // return the client to the authed status
        client.setState(GameClientState.AUTHED);

        final RestartResponse response = new RestartResponse();
        sendPacket(response);

        // send char list
        final CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
        sendPacket(cl);
        client.setCharSelection(cl.getCharInfo());
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
	@Override
	public String getType()
	{
		return _C__46_REQUESTRESTART;
	}
}