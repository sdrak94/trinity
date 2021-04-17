package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 *  sample
 *  5F
 *  01 00 00 00
 *
 *  format  cdd
 *
 *
 * @version $Revision: 1.7.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestAnswerJoinAlly extends L2GameClientPacket
{
	private static final String _C__83_REQUESTANSWERJOINALLY = "[C] 83 RequestAnswerJoinAlly";
	//private static Logger _log = Logger.getLogger(RequestAnswerJoinAlly.class.getName());

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
		    return;
		}

		L2PcInstance requestor = activeChar.getRequest().getPartner();
        if (requestor == null)
        {
        	return;
        }

		if (_response == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_ALLY_INVITATION));
			requestor.sendPacket(new SystemMessage(SystemMessageId.NO_RESPONSE_TO_ALLY_INVITATION));
		}
		else
		{
	        if (!(requestor.getRequest().getRequestPacket() instanceof RequestJoinAlly))
	        {
	        	return; // hax
	        }

	        final L2Clan clan = requestor.getClan();

			if (clan.checkAllyJoinCondition(requestor, activeChar))
	        {
				final L2Clan clam = activeChar.getClan();
				
				if (clam == null)
				{
					activeChar.getRequest().onRequestResponse();
					return;
				}

				requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ACCEPTED_ALLIANCE));

				clam.setAllyId(clan.getAllyId());
				clam.setAllyName(clan.getAllyName());
				clam.setAllyPenaltyExpiryTime(0, 0);
				clam.setAllyCrestId(clan.getAllyCrestId());
				
				if (!clam.canHasCastleDueToTwoCastlePerAllyLimit())
				{
					activeChar.sendMessage("You have joined an alliance that already owns 2 castles, and thus your clan cannot attempt to take one");
					
					if (clam.getHasCastle() > 0)
					{
						CastleManager.getInstance().getCastleByOwner(clam).removeOwner2(clam);
					}
					else
					{
						for (Siege siege : SiegeManager.getInstance().getSieges())
						{
							siege.removeSiegeClan(clam);
						}
					}
				}
				
				clam.updateClanInDB();
				
                for (L2PcInstance member : clam.getOnlineMembers(0))
                    member.broadcastUserInfo();
	        }
		}

		activeChar.getRequest().onRequestResponse();
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__83_REQUESTANSWERJOINALLY;
	}
}
