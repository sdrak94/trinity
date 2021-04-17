package luna.discord;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class DiscordTextTunnel
{
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

	public void tun (int _type, String _text, L2PcInstance activeChar)
	{
		if (_type == ALL)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("all_chat", activeChar.getName() + ": " + _text);
		}
		else if ((_type == SHOUT || _type == TRADE || _type == HERO_VOICE) && (_text.startsWith("WTS") || _text.startsWith("WTB") || _text.startsWith("WTT")))
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("marketplace", activeChar.getName() + ": " + _text);
		}
		else if (_type == TRADE)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("trade_chat", activeChar.getName() + ": " + _text);
		}
		else if (_type == SHOUT)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("shout_chat", activeChar.getName() + ": " + _text);
		}
		else if (_type == CLAN)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("clan_chat", "[" + activeChar.getClan().getName() + "] - " + activeChar.getName() + ": " + _text);
		}
		else if (_type == PARTY)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("party_chat", activeChar.getName() + ": " + _text);
		}
		else if (_type == ANNOUNCEMENT)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("announce_chat", activeChar.getName() + ": " + _text);
		}
		else if (_type == HERO_VOICE)
		{
			_text = _text.replaceAll("Type=", "");
			_text = _text.replaceAll("ID=", "");
			_text = _text.replaceAll("Color=", "");
			_text = _text.replaceAll("Underline=", "");
			_text = _text.replaceAll("Title=", "");
			DiscordBot.getInstance().runCommander("voice_chat", activeChar.getName() + ": " + _text);
		}
	}
	public static DiscordTextTunnel getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DiscordTextTunnel INSTANCE = new DiscordTextTunnel();
	}
}
