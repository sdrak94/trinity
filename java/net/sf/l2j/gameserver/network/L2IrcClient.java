/*package net.sf.l2j.gameserver.network;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;
import org.schwering.irc.lib.ssl.SSLIRCConnection;
import org.schwering.irc.lib.ssl.SSLTrustManager;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.handler.IIrcCommandHandler;
import net.sf.l2j.gameserver.handler.IrcCommandHandler;
import net.sf.l2j.gameserver.instancemanager.IrcManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.tools.codec.Base64;
import net.sf.l2j.tools.random.Rnd;


public class L2IrcClient extends Thread
{
	private IRCConnection				conn;
	private final String						channel;
	private final String						nickname;
	protected boolean					forcedDisconnect	= false;
	protected FastMap<String, String>	_authedGms			= new FastMap<String, String>();

	public L2IrcClient(String host, int port, String pass, String nick, String user, String name, boolean ssl, String Chan)
	{
		if (!ssl)
		{
			conn = new IRCConnection(host, new int[] { port }, pass, nick, user, name);
		}
		else
		{
			conn = new SSLIRCConnection(host, new int[] { port }, pass, nick, user, name);
			((SSLIRCConnection) conn).addTrustManager(new TrustManager());
		}
		channel = Chan;
		nickname = nick;
		conn.addIRCEventListener(new Listener());
		conn.setEncoding("UTF-8");
		conn.setPong(true);
		conn.setDaemon(false);
		conn.setColors(false);
		start();
	}

	public void connect() throws IOException
	{
		if (!conn.isConnected())
		{
			conn.connect();
			conn.send("JOIN " + channel);
		}
	}

	public void disconnect()
	{
		if (conn.isConnected())
		{
			conn.doQuit();
			conn.close();
		}
	}

	public void forcedConnect()
	{
		IrcManager.getInstance().reload();
	}

	public void forcedDisconnect()
	{
		if (conn.isConnected())
		{
			conn.doQuit("Evil Admin killed me.");
			forcedDisconnect = true;
			conn.close();
		}
		IrcManager.getInstance().removeConnection();
	}

	public void forcedReconnect()
	{
		forcedDisconnect = false;
		IrcManager.getInstance().reload();
	}

	public void send(String Text)
	{
		if (checkConnection())
			conn.send(Text);
	}

	public void send(String target, String Text)
	{
		if (checkConnection())
			conn.doPrivmsg(target, Text);
	}

	public void sendChan(String Text)
	{
		if (checkConnection())
		{
			conn.doPrivmsg(channel, Text);
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + channel + "> " + Text);
		}
	}

	public boolean checkConnection()
	{
		if (!conn.isConnected())
		{
			try
			{
				if (!forcedDisconnect)
				{
					conn.doQuit();
					conn.connect();
				}
			}
			catch (Exception exc)
			{
				exc.printStackTrace();
			}
		}
		return conn.isConnected();
	}

	public class TrustManager implements SSLTrustManager
	{
		private X509Certificate[]	_chain;

		public X509Certificate[] getAcceptedIssuers()
		{
			return _chain != null ? _chain : new X509Certificate[0];
		}

		public boolean isTrusted(X509Certificate[] chain)
		{
			_chain = chain;
			return true;
		}

	}

	*//**
	 * Treats IRC events.
	 *//*
	public class Listener implements IRCEventListener
	{

		private boolean	isconnected;

		public void onRegistered()
		{
			_log.info("IRC: Connected");

			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: Connected");

			if (!Config.IRC_LOGIN_COMMAND.trim().isEmpty())
				send(Config.IRC_LOGIN_COMMAND.trim());

			if (Config.IRC_NICKSERV)
				send(Config.IRC_NICKSERV_NAME, Config.IRC_NICKSERV_COMMAND);

			isconnected = true;
		}

		public void onDisconnected()
		{
			_log.info("IRC: Disconnected");
			isconnected = false;
			if (conn.isConnected())
				conn.close();
			if (!forcedDisconnect)
			{
				IrcManager.getInstance().removeConnection();
				IrcManager.getInstance().reload();
			}

			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: Disconnected");
		}

		public void onError(String msg)
		{
			_log.info("IRC: Error: " + msg);

			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: Error: " + msg);
		}

		public void onError(int num, String msg)
		{
			_log.info("IRC: Error #" + num + ": " + msg);

			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: Error #" + num + ": " + msg);

			// nickname already in use
			if (num == 433)
			{
				Integer random = Rnd.get(999);
				send("NICK " + nickname + random);
				send("JOIN " + channel);
			}
		}

		public void onInvite(String chan, IRCUser u, String nickPass)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " invites " + nickPass);
		}

		public void onJoin(String chan, IRCUser u)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " joins");
		}

		public void onKick(String chan, IRCUser u, String nickPass, String msg)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " kicks " + nickPass);
		}

		public void onMode(IRCUser u, String nickPass, String mode)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC Mode: " + u.getNick() + " sets modes " + mode + " " + nickPass);
		}

		public void onMode(String chan, IRCUser u, IRCModeParser mp)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " sets mode: " + mp.getLine());
		}

		public void onNick(IRCUser u, String nickNew)
		{
			removeAccount(u.getUsername(), u.getHost());
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC Nick: " + u.getNick() + " is now known as " + nickNew);
		}

		public void onNotice(String target, IRCUser u, String msg)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("Irc " + target + "> " + u.getNick() + " (notice): " + msg);
		}

		public void onPart(String chan, IRCUser u, String msg)
		{
			removeAccount(u.getUsername(), u.getHost());
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " parts");
		}

		public void onPrivmsg(String chan, IRCUser u, String msg)
		{

			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + ": " + msg);

			if (msg.startsWith("ACTION") && Config.IRC_ME_SUPPORT)
			{
				SystemChatChannelId channelId = SystemChatChannelId.Chat_Shout;
				if (Config.IRC_TO_GAME_ME_DISPLAY.equals("trade"))
					channelId = SystemChatChannelId.Chat_Market;
				if (Config.IRC_TO_GAME_ME_DISPLAY.equals("hero"))
					channelId = SystemChatChannelId.Chat_Hero;

				String me;
				me = " * " + u.getNick() + msg.substring(6);
				CreatureSay cs = new CreatureSay(0, channelId, "[IRC]", me);

				for (L2PcInstance player : L2World.getInstance().getAllPlayers())
				{
					player.sendPacket(cs);
				}
			}
			else if (chan.equalsIgnoreCase(channel))
			{
				if (Config.IRC_TO_GAME_TYPE.equals("global") || Config.IRC_TO_GAME_TYPE.equals("special"))
				{
					if (Config.IRC_TO_GAME_TYPE.equals("global") || (Config.IRC_TO_GAME_TYPE.equals("special") && msg.substring(0, 1).equals(Config.IRC_TO_GAME_SPECIAL_CHAR) && msg.length() >= 2))
					{

						String sendmsg;
						if (Config.IRC_TO_GAME_TYPE.equals("special"))
							sendmsg = msg.substring(1, msg.length());
						else
							sendmsg = msg;

						SystemChatChannelId channelId = SystemChatChannelId.Chat_Shout;

						if (Config.IRC_TO_GAME_DISPLAY.equals("trade"))
							channelId = SystemChatChannelId.Chat_Market;
						if (Config.IRC_TO_GAME_DISPLAY.equals("hero"))
							channelId = SystemChatChannelId.Chat_Hero;

						CreatureSay cs = new CreatureSay(0, channelId, "[IRC] " + u.getNick(), sendmsg);

						for (L2PcInstance player : L2World.getInstance().getAllPlayers())
						{
							player.sendPacket(cs);
						}
					}
				}
				if (msg.equals("!online"))
				{
					sendChan("Online Players: " + L2World.getInstance().getAllPlayersCount() + " / " + LoginServerThread.getInstance().getMaxPlayer());
				}
				else if (msg.equals("!gmlist"))
				{
					if (GmListTable.getAllGms(false).size() == 0)
						sendChan("There are not any GMs that are providing customer service currently");
					else
						for (L2PcInstance gm : GmListTable.getAllGms(false))
							sendChan(gm.getName());
				}
				else if (msg.equals("!rates"))
				{
					sendChan("XP Rate: " + Config.RATE_XP + " | " + "SP Rate: " + Config.RATE_SP + " | " + "Spoil Rate: " + Config.RATE_DROP_SPOIL + " | " + "Adena Rate: " + Config.RATE_DROP_ADENA + " | " + "Drop Rate: "
							+ Config.RATE_DROP_ITEMS + " | " + "Party XP Rate: " + Config.RATE_PARTY_XP + " | " + "Party SP Rate: " + Config.RATE_PARTY_SP);
				}
				else if (msg.equals("!showon"))
				{
					if (L2World.getInstance().getAllPlayers().size() == 0)
						sendChan("No Players currently online");
					else
					{
						String _onlineNames = "Players currently online:";
						boolean _isFirst = true;
						for (L2PcInstance player : L2World.getInstance().getAllPlayers())
						{
							_onlineNames = _onlineNames + (_isFirst ? " " : ", ") + player.getName();
							_isFirst = false;
						}
						sendChan(_onlineNames);
					}
				}
			}

			if (msg.startsWith("!ident"))
			{
				StringTokenizer st = new StringTokenizer(msg);
				st.nextToken();
				try
				{
					String username = st.nextToken();
					String password = st.nextToken();
					isAccountValid(username, password, u.getUsername(), u.getNick(), u.getHost());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				IIrcCommandHandler ircch = IrcCommandHandler.getInstance().getIrcCommandHandler(msg);

				if (ircch != null)
					ircch.useIrcCommand(msg, u.getNick(), null, _authedGms.containsKey(u.getUsername().toLowerCase()));
			}
		}

		public void onQuit(IRCUser u, String msg)
		{
			removeAccount(u.getUsername(), u.getHost());
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC Quit: " + u.getNick());
		}

		public void onReply(int num, String value, String msg)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC REPLY #" + num + ": " + value + " " + msg);
		}

		public void onTopic(String chan, IRCUser u, String topic)
		{
			if (Config.IRC_LOG_CHAT)
				_logChat.info("IRC: " + chan + "> " + u.getNick() + " changes topic into: " + topic);
		}

		public void onPing(String p)
		{
			if (_log.isDebugEnabled())
				_log.info("IRC: Ping Pong");

			// keep connection alive
			conn.doPong(p);
		}

		public void unknown(String a, String b, String c, String d)
		{
			_log.warn("IRC UNKNOWN: " + a + " " + b + " " + c + " " + d);
		}

		public boolean isConnected()
		{
			return isconnected;
		}
	}

	protected void isAccountValid(String account, String password, String ident, String nick, String ip)
	{
		boolean ok = false;
		java.sql.Connection con = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			byte[] expected = null;
			int access = 0;
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT password, access_level FROM irc WHERE login=?");
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				expected = Base64.decode(rset.getString("password"));
				access = rset.getInt("access_level");
			}
			rset.close();
			statement.close();

			if (expected == null)
				return;
			else
			{
				if (access <= 0)
					return;

				ok = true;
				for (int i = 0; i < expected.length; i++)
				{
					if (hash[i] != expected[i])
					{
						ok = false;
						break;
					}
				}
			}
			if (ok)
			{
				if (!_authedGms.containsKey(ident.toLowerCase()))
				{
					_authedGms.put(ident.toLowerCase(), ip);
					send(nick, "Successfully Authed!");
				}
				else
				{
					send(nick, "Already Authed!");
				}

			}
			else
			{
				send(nick, "Authentication Failed!");
			}
		}
		catch (Exception e)
		{
			_log.warn("", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	protected void removeAccount(String nick, String ip)
	{
		if (_authedGms.containsKey(nick.toLowerCase()))
			_authedGms.remove(nick.toLowerCase());
		else if (_authedGms.containsValue(ip))
			_authedGms.remove(ip);
	}

	public FastMap<String, String> getAuthedGms()
	{
		return _authedGms;
	}
}
*/