package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ChatTell implements IChatHandler
{
private static final int[] COMMAND_IDS =
{
	2
};

/**
 * Handle chat type 'tell'
 * @see net.sf.l2j.gameserver.handler.IChatHandler#handleChat(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
 */
public void handleChat(int type, L2PcInstance activeChar, String target, String text)
{
	//Return if player is chat banned
	if (!activeChar.isGM() && (activeChar.isChatBanned() || activeChar.isCursedWeaponEquipped()))
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED));
		return;
	}
	
	//return if player is in jail
	if (Config.JAIL_DISABLE_CHAT && activeChar.isInJail())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CHATTING_PROHIBITED));
		return;
	}
	
	// Return if no target is set
	if (target == null)
		return;
	
	text = text.replace("[gm]", "");
	text = text.replace("[GM]", "");
	text = text.replace("[Gm]", "");
	text = text.replace("[gM]", "");
	
	text = text.replace("(gm)", "");
	text = text.replace("(GM)", "");
	text = text.replace("(Gm)", "");
	text = text.replace("(gM)", "");
	
	text = text.replace("{gm}", "");
	text = text.replace("{GM}", "");
	text = text.replace("{Gm}", "");
	text = text.replace("{gM}", "");
	
	/*		text = text.replaceAll("forgotten", "pride");
		text = text.replaceAll("Forgotten", "pride");
		text = text.replaceAll("FORGOTTEN", "pride");*/
	/*		text = text.replaceAll("wartag", "pride");
		text = text.replaceAll("Wartag", "pride");
		text = text.replaceAll("WARTAG", "pride");*/
	
	L2PcInstance receiver = L2World.getInstance().getPlayer(target);
	
	if (receiver != null && ((!BlockList.isBlocked(receiver, activeChar) && !receiver.getBlockList().isBlockAll()) || activeChar.isGM()))
	{
		if (Config.JAIL_DISABLE_CHAT && receiver.isInJail())
		{
			activeChar.sendMessage("Player is in jail.");
			return;
		}
		if (receiver.isChatBanned())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE));
			return;
		}
		if (receiver.getClient() != null && receiver.getClient().isDetached())
		{
			activeChar.sendMessage("Player is in offline mode.");
			return;
		}
		if (!receiver.getMessageRefusal() || (activeChar.isGM() && activeChar.getAccessLevel().getLevel() > receiver.getAccessLevel().getLevel()))
		{
			receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(), text));
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE));
		}
	}
	else
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
		sm.addString(target);
		activeChar.sendPacket(sm);
		sm = null;
	}
}

/**
 * Returns the chat types registered to this handler
 * @see net.sf.l2j.gameserver.handler.IChatHandler#getChatTypeList()
 */
public int[] getChatTypeList()
{
	return COMMAND_IDS;
}
}
