package net.sf.l2j.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.status.Status;

/**
 *
 * @author  KenM
 */
public class L2LoginServer
{
public static final int PROTOCOL_REV = 0x0102;

private static L2LoginServer _instance;
private Logger _log = Logger.getLogger(L2LoginServer.class.getName());
private GameServerListener _gameServerListener;
private SelectorThread<L2LoginClient> _selectorThread;
private Status _statusServer;

public static void main(String[] args)
{
	_instance = new L2LoginServer();
}

public static L2LoginServer getInstance()
{
	return _instance;
}

public L2LoginServer()
{
	Server.serverMode = Server.MODE_LOGINSERVER;
	// Local Constants
	final String LOG_FOLDER = "log"; // Name of folder for log file
	final String LOG_NAME = "./log.cfg"; // Name of log file
	
	/*** Main ***/
	// Create log folder
	File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
	logFolder.mkdir();
	
	// Create input stream for log file -- or store file data into memory
	InputStream is = null;
	try
	{
		is = new FileInputStream(new File(LOG_NAME));
		LogManager.getLogManager().readConfiguration(is);
		is.close();
	}
	catch (IOException e)
	{
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	finally
	{
		try
		{
			if (is != null)
			{
				is.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// Load Config
	Config.load();
	
	// Prepare Database
	try
	{
		L2DatabaseFactory.getInstance();
	}
	catch (SQLException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed initializing database. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	try
	{
		LoginController.load();
	}
	catch (GeneralSecurityException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed initializing LoginController. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	try
	{
		GameServerTable.load();
	}
	catch (GeneralSecurityException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed to load GameServerTable. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	catch (SQLException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed to load GameServerTable. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	loadBanFile();
	
	InetAddress bindAddress = null;
	if (!Config.LOGIN_BIND_ADDRESS.equals("*"))
	{
		try
		{
			bindAddress = InetAddress.getByName(Config.LOGIN_BIND_ADDRESS);
		}
		catch (UnknownHostException e)
		{
			_log.warning("WARNING: The LoginServer bind address is invalid, using all avaliable IPs. Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
		}
	}
	
	final SelectorConfig sc = new SelectorConfig();
	sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
	sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
	sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
	sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
	
	final L2LoginPacketHandler lph = new L2LoginPacketHandler();
	final SelectorHelper sh = new SelectorHelper();
	try
	{
		_selectorThread = new SelectorThread<L2LoginClient>(sc, sh, lph, sh, sh);
	}
	catch (IOException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed to open Selector. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	try
	{
		_gameServerListener = new GameServerListener();
		_gameServerListener.start();
		_log.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
	}
	catch (IOException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed to start the Game Server Listener. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	
	if (Config.IS_TELNET_ENABLED)
	{
		try
		{
			_statusServer = new Status(Server.serverMode);
			_statusServer.start();
		}
		catch (IOException e)
		{
			_log.warning("Failed to start the Telnet Server. Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
		}
	}
	else
	{
		_log.info("Telnet server is currently disabled.");
	}
	
	try
	{
		_selectorThread.openServerSocket(bindAddress, Config.PORT_LOGIN);
	}
	catch (IOException e)
	{
		_log.log(Level.SEVERE, "FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
		if (Config.DEVELOPER)
		{
			e.printStackTrace();
		}
		System.exit(1);
	}
	_selectorThread.start();
	_log.info("Login Server ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + Config.PORT_LOGIN);
}

public Status getStatusServer()
{
	return _statusServer;
}

public GameServerListener getGameServerListener()
{
	return _gameServerListener;
}

private void loadBanFile()
{
	File bannedFile = new File("./banned_ip.cfg");
	if (bannedFile.exists() && bannedFile.isFile())
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(bannedFile);
		}
		catch (FileNotFoundException e)
		{
			_log.warning("Failed to load banned IPs file (" + bannedFile.getName() + ") for reading. Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			return;
		}
		
		LineNumberReader reader = null;
		String line;
		String[] parts;
		try
		{
			reader = new LineNumberReader(new InputStreamReader(fis));
			
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				// check if this line isnt a comment line
				if (line.length() > 0 && line.charAt(0) != '#')
				{
					// split comments if any
					parts = line.split("#", 2);
					
					// discard comments in the line, if any
					line = parts[0];
					
					parts = line.split(" ");
					
					String address = parts[0];
					
					long duration = 0;
					
					if (parts.length > 1)
					{
						try
						{
							duration = Long.parseLong(parts[1]);
						}
						catch (NumberFormatException e)
						{
							_log.warning("Skipped: Incorrect ban duration (" + parts[1] + ") on (" + bannedFile.getName() + "). Line: " + reader.getLineNumber());
							continue;
						}
					}
					
					try
					{
						LoginController.getInstance().addBanForAddress(address, duration);
					}
					catch (UnknownHostException e)
					{
						_log.warning("Skipped: Invalid address (" + parts[0] + ") on (" + bannedFile.getName() + "). Line: " + reader.getLineNumber());
					}
				}
			}
		}
		catch (IOException e)
		{
			_log.warning("Error while reading the bans file (" + bannedFile.getName() + "). Details: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				reader.close();
			}
			catch (Exception e)
			{
			}
			
			try
			{
				fis.close();
			}
			catch (Exception e)
			{
			}
		}
		_log.config("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
	}
	else
	{
		_log.config("IP Bans file (" + bannedFile.getName() + ") is missing or is a directory, skipped.");
	}
}

public void shutdown(boolean restart)
{
	Runtime.getRuntime().exit(restart ? 2 : 0);
}
}
