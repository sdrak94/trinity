package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;

public final class Logout extends L2GameClientPacket
{
private static final String _C__09_LOGOUT = "[C] 09 Logout";

@Override
protected void readImpl()
{
}

@Override
protected void runImpl()
{
	getClient().setLoggingOut(true);
	
	// Dont allow leaving if player is fighting
	final L2PcInstance player = getClient().getActiveChar();
	
	if (player == null)
		return;
	
	if(player.getActiveEnchantItem() != null || player.getActiveEnchantAttrItem() != null)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
//	if (player.isLocked())
//	{
//		_log.warning("Player " + player.getName() + " tried to logout during class change.");
//		player.sendPacket(ActionFailed.STATIC_PACKET);
//		return;
//	}

	if (player.isSubbing())
	{
		_log.warning("Player " + player.getName() + " tried to logout during class change.");
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	if (!player.isGM())
	{
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) || player.getPvpFlag() != 0)
		{
			if (Config.DEBUG)
				_log.fine("Player " + player.getName() + " tried to logout while fighting");
			
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInZombieEvent())
		{
			player.sendMessage("You are not allowed to log out during Zombie Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInRaidEvent())
		{
			player.sendMessage("You are not allowed to log out during Raid Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInKoreanEvent())
		{
			player.sendMessage("You are not allowed to log out during Korean Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInDominationEvent())
		{
			player.sendMessage("You are not allowed to log out during Domination Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInLastManStandingEvent())
		{
			player.sendMessage("You are not allowed to log out during Last Man Standing Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isInLastTeamStandingEvent())
		{
			player.sendMessage("You are not allowed to log out during Last Team Standing Event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	
	if (player.isInFunEvent())
	{
		player.sendMessage("A superior power doesn't allow you to leave the event");
		return;
	}
	
	/*		if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("You cant logout in olympiad mode");
			return;
		}*/
	
	// Prevent player from logging out if they are a festival participant
	// and it is in progress, otherwise notify party members that the player
	// is not longer a participant.
	if (player.isFestivalParticipant())
	{
		if (SevenSignsFestival.getInstance().isFestivalInitialized())
		{
			player.sendMessage("You cannot log out while you are a participant in a festival.");
			return;
		}
		final L2Party playerParty = player.getParty();
		
		if (playerParty != null)
			player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName()
					+ " has been removed from the upcoming festival."));
	}
	
	if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE))
	{
		player.closeNetConnection();
		return;
	}
	
	getClient().setLoggingOut(true);
	
	sendPacket(new LeaveWorld());
	getClient().setActiveChar(null);
	player.deleteMe();
}
/*
 * (non-Javadoc)
 * 
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__09_LOGOUT;
}
}