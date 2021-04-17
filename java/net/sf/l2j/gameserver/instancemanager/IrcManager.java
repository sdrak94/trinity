/*package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.Config;


public class IrcManager
{
	private static IrcManager	_instance;
	private static L2IrcClient	_IrcConnection;
	private static boolean		initilized	= false;

	public static IrcManager getInstance()
	{
		if (_instance == null)
			_instance = new IrcManager();
		
		return _instance;
	}
	
	private IrcManager()
	{
		_log.info("Initializing IrcManager");
		load();
	}

	public static boolean isInitialized()
	{
		return initilized;
	}

	public void reload()
	{
		if (_IrcConnection != null)
		{
			_IrcConnection.disconnect();
			_IrcConnection = null;
			initilized = false;
		}

		try
		{
			_IrcConnection = new L2IrcClient(Config.IRC_SERVER, Config.IRC_PORT, Config.IRC_PASS, Config.IRC_NICK, Config.IRC_USER, Config.IRC_NAME, Config.IRC_SSL, Config.IRC_CHANNEL);
			_IrcConnection.connect();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		initilized = true;
	}

	public L2IrcClient getConnection()
	{
		return _IrcConnection;
	}

	public void removeConnection()
	{
		_IrcConnection.disconnect();
		_IrcConnection = null;
	}

	private final void load()
	{
		_IrcConnection = new L2IrcClient(Config.IRC_SERVER, Config.IRC_PORT, Config.IRC_PASS, Config.IRC_NICK, Config.IRC_USER, Config.IRC_NAME, Config.IRC_SSL, Config.IRC_CHANNEL);

		try
		{
			_IrcConnection.connect();
		}
		catch (Exception e)
		{
			_log.warn("", e);
		}

		initilized = true;
	}
}*/