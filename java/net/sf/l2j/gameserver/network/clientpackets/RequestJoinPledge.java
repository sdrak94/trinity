package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinPledge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinPledge extends L2GameClientPacket
{
private static final String _C__24_REQUESTJOINPLEDGE = "[C] 24 RequestJoinPledge";

private int _target;
private int _pledgeType;

@Override
protected void readImpl()
{
	_target  = readD();
	_pledgeType = readD();
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
	{
		return;
	}
	
	L2Object ob = L2World.getInstance().findObject(_target);
	if (!(ob instanceof L2PcInstance))
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
		return;
	}
	
	L2PcInstance target = (L2PcInstance) ob;
	
	if (target.getAccessLevel().getLevel() > activeChar.getAccessLevel().getLevel())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
		return;
	}
	if (activeChar.getPvpFlag() != 0 || target.getPvpFlag() != 0)
	{   //cannot invite clan when flagged
		activeChar.sendMessage("You cannot clan invite when in PvP mode");
		return;
	}
	if (BlockList.isBlocked(target, activeChar))
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		return;
	}
	if (target.getTradeRefusal() && !activeChar.isGM())
	{
		activeChar.sendMessage("Target is in interaction refusal mode");
		return;
	}
	if (target.isProcessingRequest() || target.isProcessingTransaction())
	{
		activeChar.sendMessage("Target is busy");
		return;
	}
	
	L2Clan clan = activeChar.getClan();
	if (!clan.checkClanJoinCondition(activeChar, target, _pledgeType))
		return;
	if (!activeChar.getRequest().setRequest(target, this))
		return;
	
	String _pledgeName = activeChar.getClan().getName();
	String _subPledgeName = (activeChar.getClan().getSubPledge(_pledgeType) != null ? activeChar.getClan().getSubPledge(_pledgeType).getName() : null);
	target.sendPacket(new AskJoinPledge(activeChar.getObjectId(), _subPledgeName, _pledgeType, _pledgeName));
}

public int getPledgeType()
{
	return _pledgeType;
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__24_REQUESTJOINPLEDGE;
}
}
