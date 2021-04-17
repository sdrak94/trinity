package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinAlly;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinAlly extends L2GameClientPacket
{

	private static final String _C__82_REQUESTJOINALLY = "[C] 82 RequestJoinAlly";
	//private static Logger _log = Logger.getLogger(RequestJoinAlly.class.getName());
	
	private int _id;

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
		    return;
		}

		L2Object ob = L2World.getInstance().findObject(_id);

		if (!(ob instanceof L2PcInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return;
		}

		if(activeChar.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
			return;
		}

		L2PcInstance target = (L2PcInstance)ob;
		L2Clan clan = activeChar.getClan();
		if (!clan.checkAllyJoinCondition(activeChar, target))
		{
			return;
		}
		if (!activeChar.getRequest().setRequest(target, this))
		{
			return;
		}
        if (BlockList.isBlocked(target, activeChar))
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;        	
        }
        if (target.getAccessLevel().getLevel() > activeChar.getAccessLevel().getLevel())
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;        	
        }
        if (target.getTradeRefusal() && !activeChar.isGM())
        {
        	activeChar.sendMessage("Target is in interaction refusal mode");
            return;
        }
        if (target.getPvpFlag() != 0 || activeChar.getPvpFlag() != 0)
        {   
        	activeChar.sendMessage("You cannot start requests in PvP mode");
			return;        	
        }

		SystemMessage sm = new SystemMessage(SystemMessageId.S2_ALLIANCE_LEADER_OF_S1_REQUESTED_ALLIANCE);
		sm.addString(activeChar.getClan().getAllyName());
		sm.addString(activeChar.getName());
		target.sendPacket(sm);
		sm = null;
		AskJoinAlly aja = new AskJoinAlly(activeChar.getObjectId(), activeChar.getClan().getAllyName());
		target.sendPacket(aja);
	}


	@Override
	public String getType()
	{
		return _C__82_REQUESTJOINALLY;
	}
}

