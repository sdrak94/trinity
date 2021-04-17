package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public final class AttackRequest extends L2GameClientPacket
{
// cddddc
private int _objectId;
@SuppressWarnings("unused")
private int _originX;
@SuppressWarnings("unused")
private int _originY;
@SuppressWarnings("unused")
private int _originZ;
@SuppressWarnings("unused")
private int _attackId;

private static final String _C__0A_ATTACKREQUEST = "[C] 0A AttackRequest";

@Override
protected void readImpl()
{
	_objectId  = readD();
	_originX  = readD();
	_originY  = readD();
	_originZ  = readD();
	_attackId  = readC(); 	 // 0 for simple click   1 for shift-click
}

@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null) return;
	
	L2Object target;
	if (activeChar.getTargetId() == _objectId)
		target = activeChar.getTarget();
	else
		target = L2World.getInstance().findObject(_objectId);
	
	if (target == null) return;
	
	// Players can't attack objects in the other instances		// except from multiverse
	if (target.getInstanceId() != activeChar.getInstanceId() && activeChar.getInstanceId() != -1)
		return;
	if (activeChar.isOutOfControl())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Only GMs can directly interact with invisible characters
	if (target instanceof L2PcInstance && ((L2PcInstance)target).isInvisible() && !activeChar.isGMReally() && (((L2PcInstance)target).isGMReally() || !activeChar.canSeeInvisiblePeople()))
	{
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (activeChar.getTarget() != target)
	{
		target.onAction(activeChar);
	}
	else
	{
		if ((target.getObjectId() != activeChar.getObjectId()) && activeChar.getPrivateStoreType() == 0 && !activeChar.isProcessingTransaction())
		{
			if (target instanceof L2Playable || target instanceof L2Decoy)
				target.onForcedAttack(activeChar);
			else
				target.onAction(activeChar);
		}
		else
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
}


@Override
public String getType()
{
	return _C__0A_ATTACKREQUEST;
}

@Override
protected boolean triggersOnActionRequest()
{
	return true;
}
}
