package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestTargetCanceld extends L2GameClientPacket
{
	private static final String _C__37_REQUESTTARGETCANCELD = "[C] 37 RequestTargetCanceld";
	//private static Logger _log = Logger.getLogger(RequestTargetCanceld.class.getName());

    private int _unselect;

	@Override
	protected void readImpl()
	{
        _unselect = readH();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
        if (activeChar != null)
        {
        	L2Summon pet = activeChar.getPet();
			
			if (pet != null)
			{
				if (activeChar.getTarget() == null)
				{
					if (!pet.isOutOfControl())
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
			
        	if (activeChar.isLockedTarget())
        	{
        		activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_DISABLE_TARGET));
        		return;
        	}
        	
            if (_unselect == 0)
            {
            	if (activeChar.isCastingNow() && activeChar.canAbortCast())
            		activeChar.abortCast();
            	else if (activeChar.getTarget() != null)
            	{
            		activeChar.setIsSelectingTarget(3);
            		activeChar.setTarget(null);
            	}
            }
            else if (activeChar.getTarget() != null)
            {
				activeChar.setIsSelectingTarget(3);
				activeChar.setTarget(null);
			}
            if (activeChar.isAttackingNow() && activeChar.getKnockedbackTimer() < GameTimeController.getGameTicks())
			{
            	activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				activeChar.abortAttack();				
			}
            else
            	sendPacket(ActionFailed.STATIC_PACKET);      
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__37_REQUESTTARGETCANCELD;
	}
}