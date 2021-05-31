package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class Action extends L2GameClientPacket
{
private static final String ACTION__C__04 = "[C] 04 Action";

// cddddc
private int _objectId;
@SuppressWarnings("unused")
private int _originX;
@SuppressWarnings("unused")
private int _originY;
@SuppressWarnings("unused")
private int _originZ;
private int _actionId;

// player can select object as a target but can't interact,
// or spawn protection will be removed
private boolean _removeSpawnProtection = false;

@Override
protected void readImpl()
{
	_objectId = readD(); // Target object Identifier
	_originX = readD();
	_originY = readD();
	_originZ = readD();
	_actionId = readC(); // Action identifier : 0-Simple click, 1-Shift click
}

@Override
protected void runImpl()
{
	//_log.fine("Action:" + _actionId);
	if (Config.DEBUG)
		_log.fine("Action:" + _actionId);
	if (Config.DEBUG)
		_log.fine("oid:" + _objectId);
	
	if (_objectId < 1)
		return;
	
	// Get the current L2PcInstance of the player
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	if (activeChar.getActionObjIdNoTarget() != 0)
	{
		if (activeChar.getActionObjIdNoTarget() == _objectId)
		{
			if (activeChar.getActionObjIdNoTargetTicks() + 6 > GameTimeController.getGameTicks() && activeChar.getTarget() == null)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				activeChar.setActionObjIdNoTarget(0);
				activeChar.setActionObjIdNoTargetTicks(0);
				return;
			}
		}
		
		activeChar.setActionObjIdNoTarget(0);
		activeChar.setActionObjIdNoTargetTicks(0);
	}
	
	if (activeChar.isOutOfControl())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (activeChar.inObserverMode())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
		getClient().sendPacket(sm);
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	final L2Object obj;
	
	if (activeChar.getTargetId() == _objectId)
	{
		obj = activeChar.getTarget();
		_removeSpawnProtection = true;
	}
	else
		obj = L2World.getInstance().findObject(_objectId);
	
	// If object requested does not exist, add warn msg into logs
	if (obj == null)
	{
		// pressing e.g. pickup many times quickly would get you here
		// _log.warning("Character: " + activeChar.getName() + " request action with non existent ObjectID:" + _objectId);
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Players can't interact with objects in the other instances
	// except from multiverse
	if (activeChar.getInstanceId() != -1 && obj.getInstanceId() != activeChar.getInstanceId())
	{
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Only GMs can directly interact with invisible characters
	if (obj instanceof L2PcInstance && ((L2PcInstance)obj).isInvisible() && !activeChar.isGMReally() && (((L2PcInstance)obj).isGMReally() || !activeChar.canSeeInvisiblePeople()))
	{
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Check if the target is valid, if the player haven't a shop or isn't the requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...)
	if (!activeChar.isProcessingTransaction())
	{
		switch (_actionId)
		{
		case 0:
			obj.onAction(activeChar);
			break;
		case 1:
			if (obj instanceof L2Character && ((L2Character)obj).isDead())
				obj.onAction(activeChar);
			else
				obj.onActionShift(getClient());
			break;
		default:
			// Ivalid action detected (probably client cheating), log this
			_log.warning("Character: " + activeChar.getName() + " requested invalid action: " + _actionId);
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			break;
		}
	}
	else
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
}

@Override
protected boolean triggersOnActionRequest()
{
	return _removeSpawnProtection;
}

@Override
public String getType()
{
	return ACTION__C__04;
}
}