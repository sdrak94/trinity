package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.ChatHandler;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class Say2 extends L2GameClientPacket
{
	private static final String		_C__38_SAY2			= "[C] 38 Say2";
	private static Logger			_logChat			= Logger.getLogger("chat");
	public final static int			ALL					= 0;
	public final static int			SHOUT				= 1;						// !
	public final static int			TELL				= 2;
	public final static int			PARTY				= 3;						// #
	public final static int			CLAN				= 4;						// @
	public final static int			GM					= 5;
	public final static int			PETITION_PLAYER		= 6;						// used for petition
	public final static int			PETITION_GM			= 7;						// * used for petition
	public final static int			TRADE				= 8;						// +
	public final static int			ALLIANCE			= 9;						// $
	public final static int			ANNOUNCEMENT		= 10;
	public static final int			BOAT				= 11;
	public static final int			L2FRIEND			= 12;
	public static final int			MSNCHAT				= 13;
	public static final int			PARTYMATCH_ROOM		= 14;
	public final static int			PARTYROOM_COMMANDER	= 15;						// (Yellow)
	public final static int			PARTYROOM_ALL		= 16;						// (Red)
	public final static int			HERO_VOICE			= 17;
	public static final int			CRITICAL_ANNOUNCE	= 18;
	public static final int			SCREEN_ANNOUNCE		= 19;
	public final static int			BATTLEFIELD			= 20;						// Yellow for VIPS
	public static final int			MPCC_ROOM			= 21;
	public static final int			NPC_ALL				= 22;
	public static final int			NPC_SHOUT			= 23;
	private final static String[]	CHAT_NAMES			=
	{
		"ALL",
		"SHOUT",
		"TELL",
		"PARTY",
		"CLAN",
		"GM",
		"PETITION_PLAYER",
		"PETITION_GM",
		"TRADE",
		"ALLIANCE",
		"ANNOUNCEMENT",																// 10
		"BOAT",
		"L2FRIEND",
		"MSNCHAT",
		"PARTYMATCH_ROOM",
		"PARTYROOM_COMMANDER",
		"PARTYROOM_ALL",
		"HERO_VOICE",
		"CRITICAL_ANNOUNCE",
		"SCREEN_ANNOUNCE",
		"BATTLEFIELD",
		"MPCC_ROOM"
	};
	private final static String[]	BANNED_WORDS		=
	{
		"\\\\\\\\n",
		"\\\\\\fly",
		"yolasite",
		"yola-site",
		"yola site",
		"GM",
		"YOLASITE"
	};
	private static final String[]	LINKED_ITEM			=
	{
		"Type=", "ID=", "Color=", "Underline=", "Title="
	};
	private String					_text;
	private int						_type;
	private String					_target;
	
	@Override
	protected void readImpl()
	{
		_text = readS();
		try
		{
			_type = readD();
		}
		catch (BufferUnderflowException e)
		{
			_type = CHAT_NAMES.length;
		}
		_target = (_type == TELL) ? readS() : null;
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
			_log.info("Say2: Msg Type = '" + _type + "' Text = '" + _text + "'.");
		if (_type < 0 || _type >= CHAT_NAMES.length)
		{
			_log.warning("Say2: Invalid type: " + _type);
			return;
		}
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			_log.warning("[Say2.java] Active Character is null.");
			return;
		}
		if (_text.isEmpty())
		{
			_log.warning(activeChar.getName() + ": sending empty text. Possible packet hack!");
			return;
		}
		if (activeChar.isChatBanned() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED));
			return;
		}

		
		// Under no circumstances the official client will send a 400 character message
		// If there are no linked items in the message, you can only input 105 characters
		/*
		 * if (_text.length() > 400 || (_text.length() > 105 && !containsLinkedItems()))
		 * {
		 * activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		 * return;
		 * }
		 */
		// Even though the client can handle more characters than it's current limit allows, an overflow (critical error) happens if you pass a huge (1000+) message.
		// July 11, 2011 - Verified on High Five 4 official client as 105. // Allow higher limit if player shift some item (text is longer then).
		if (!activeChar.isGM() && (((_text.indexOf(8) >= 0) && (_text.length() > 600)) || ((_text.indexOf(8) < 0) && (_text.length() > 105))))
		{
			activeChar.sendPacket(SystemMessageId.DONT_SPAM);
			return;
		}
		/*
		 * if (_text.startsWith(">"))
		 * {
		 * Broadcast.toAllOnlinePlayers(new CreatureSay(0, 20, "[VIP]" + activeChar.getName(), _text.substring(1)));
		 * return;
		 * }
		 * if (_text.startsWith("<"))
		 * {
		 * Broadcast.toAllOnlinePlayers(new CreatureSay(0, 6, "[VIP]" + activeChar.getName(), _text.substring(1)));
		 * return;
		 * }
		 */
		if (_text.contains("Title=Donation Token"))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		String[] matches = new String[]
		{
			"\\\\\\\\n",
			"///fly",
			"yolasite",
			"yola-site",
			"yola site",
			"adrenaline",
			"faken",
			"La2bot",
			"AncientForEveR",
			"GM",
			"YOLASITE"
		};
		for (String s : matches)
		{
			if (_text.contains(s))
			{
				if (!activeChar.isGM())
				{
					activeChar.sendMessage("Prohibeted word found");
					return;
				}
			}
		}
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		_text = _text.replaceAll("\\\\n", "\n");
		if (_text.contains("\n"))
		{
			String[] lines = _text.split("\n");
			_text = "";
			for (int i = 0; i < lines.length; i++)
			{
				lines[i] = lines[i].trim();
				if (lines[i].length() == 0)
					continue;
				if (_text.length() > 0)
					_text += "\n  >";
				_text += lines[i];
			}
		}
		if (!containsLinkedItems())
		{
			if (_text.contains("type="))
			{
				_text = null;
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			_text = _text.trim();
		}
		if (_text.length() < 1)
			return;
		if (activeChar.isCursedWeaponEquipped() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.SHOUT_AND_TRADE_CHAT_CANNOT_BE_USED_WHILE_POSSESSING_CURSED_WEAPON));
			return;
		}
		if (activeChar.isInJail() && Config.JAIL_DISABLE_CHAT)
		{
			if (_type == TELL || _type == SHOUT || _type == TRADE || _type == HERO_VOICE)
			{
				activeChar.sendMessage("You cannot chat with players outside of the jail.");
				return;
			}
		}
		_text = _text.replaceAll("DvC", "Embryo Instance");
		_text = _text.replaceAll("DVC", "Embryo Instance");
		_text = _text.replaceAll("dvc", "Embryo Instance");
		
		

		
		
		if (_type == PETITION_PLAYER && activeChar.isGM())
			_type = PETITION_GM;
		if (Config.LOG_CHAT)
		{
			LogRecord record = new LogRecord(Level.INFO, _text);
			record.setLoggerName("chat");
			if (_type == TELL)
				record.setParameters(new Object[]
				{
					CHAT_NAMES[_type], "[" + activeChar.getName() + " to " + _target + "]"
				});
			else
				record.setParameters(new Object[]
				{
					CHAT_NAMES[_type], "[" + activeChar.getName() + "]"
				});
			_logChat.log(record);
		}
		IChatHandler handler = ChatHandler.getInstance().getChatHandler(_type);
		if (handler != null)
			handler.handleChat(_type, activeChar, _target, _text);
		

		/// DISCORD	START			
		//DiscordTextTunnel.getInstance().tun(_type, _text, activeChar);

	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__38_SAY2;
	}
	
	private boolean containsLinkedItems()
	{
		for (int i = 0; i < LINKED_ITEM.length; i++)
			if (!_text.contains(LINKED_ITEM[i]))
				return false;
		return true;
	}
}