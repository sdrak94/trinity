package net.sf.l2j.gameserver;

import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CharSchemesTable;
import net.sf.l2j.gameserver.datatables.OfflineTradersTable;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.CursedWeapon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.loginserver.L2LoginServer;

/**
 * 
 * This class provides the functions for shutting down and restarting the server
 * It closes all open clientconnections and saves all data.
 * 
 * @version $Revision: 1.2.4.5 $ $Date: 2005/03/27 15:29:09 $
 */
@SuppressWarnings("unused")
public class Shutdown extends Thread
{
	private static Logger _log = Logger.getLogger(Shutdown.class.getName());
	private static Shutdown _counterInstance = null;

	private int _secondsShut;
	private int _shutdownMode;
	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static final String[] MODE_TEXT = { "SIGTERM", "shutting down", "restarting", "aborting" };

	/**
	 * This function starts a shutdown countdown from Telnet (Copied from
	 * Function startShutdown())
	 * 
	 * @param ip
	 *            IP Which Issued shutdown command
	 * @param seconds
	 *            seconds untill shutdown
	 * @param restart
	 *            true if the server will restart after shutdown
	 */
	public static final HashSet<Savable> savables = new HashSet<>();
	
	private void SendServerQuit(int seconds)
	{
		SystemMessage sysm = new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS);
		sysm.addNumber(seconds);
		Broadcast.toAllOnlinePlayers(sysm);
	}

	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		_log.warning("IP: " + IP + " issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");
		// _an.announceToAll("Server is " + _modeText[shutdownMode] +
		// " in "+seconds+ " seconds!");

		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}

		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}

		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 * 
	 * @param IP
	 *            IP Which Issued shutdown command
	 */
	public void telnetAbort(String IP)
	{
		_log.warning("IP: " + IP + " issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");

		if (_counterInstance != null)
		{
			_counterInstance._abort();
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		}
	}

	/**
	 * Default constucter is only used internal to create the shutdown-hook
	 * instance
	 * 
	 */
	private Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}

	/**
	 * This creates a countdown instance of Shutdown.
	 * 
	 * @param seconds
	 *            how many seconds until shutdown
	 * @param restart
	 *            true is the server shall restart after shutdown
	 * 
	 */
	public Shutdown(int seconds, boolean restart)
	{
		if (seconds < 0)
		{
			seconds = 0;
		}
		_secondsShut = seconds;
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}
	}

	/**
	 * get the shutdown-hook instance the shutdown-hook instance is created by
	 * the first call of this function, but it has to be registrered externaly.
	 * 
	 * @return instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		return SingletonHolder._instance;
	}

	/**
	 * this function is called, when a new thread starts
	 * 
	 * if this thread is the thread of getInstance, then this is the shutdown
	 * hook and we save all data and disconnect all clients.
	 * 
	 * after this thread ends, the server will completely exit
	 * 
	 * if this is not the thread of getInstance, then this is a countdown
	 * thread. we start the countdown, and when we finished it, and it was not
	 * aborted, we tell the shutdown-hook why we call exit, and then call exit
	 * 
	 * when the exit status of the server is 1, startServer.sh / startServer.bat
	 * will restart the server.
	 * 
	 */
	@Override
	public void run()
	{
		if (this == SingletonHolder._instance)
		{
			// ensure all services are stopped
			try
			{
				GameTimeController.getInstance().stopTimer();
			}
			catch (Throwable t)
			{
				// ignore
			}

			// stop all threadpolls
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch (Throwable t)
			{
				// ignore
			}

			try
			{
				LoginServerThread.getInstance().interrupt();
			}
			catch (Throwable t)
			{
				// ignore
			}

			// last byebye, save all data and quit this server
			// logging doesnt work here :(
			saveData();

			// saveData sends messages to exit players, so sgutdown selector
			// after it
			try
			{
				GameServer.gameServer.getSelectorThread().shutdown();
				GameServer.gameServer.getSelectorThread().setDaemon(true);
			}
			catch (Throwable t)
			{
				// ignore
			}

			// commit data, last chance
			try
			{
				L2DatabaseFactory.getInstance().shutdown();
			}
			catch (Throwable t)
			{

			}

			// server will quit, when this function ends.
			if (SingletonHolder._instance._shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			// gm shutdown: send warnings and then call exit to start shutdown
			// sequence
			countdown();
			// last point where logging is operational :(
			_log.warning("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
			switch (_shutdownMode)
			{
				case GM_SHUTDOWN:
					SingletonHolder._instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART:
					SingletonHolder._instance.setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}

	/**
	 * This functions starts a shutdown countdown
	 * 
	 * @param activeChar
	 *            GM who issued the shutdown command
	 * @param seconds
	 *            seconds until shutdown
	 * @param restart
	 *            true if the server will restart after shutdown
	 */
	public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		if (restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}

		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");

		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}

		if (_counterInstance != null)
		{
			_counterInstance._abort();
		}

		// the main instance should only run for shutdown hook, so we start a
		// new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 * 
	 * @param activeChar
	 *            GM who issued the abort command
	 */
	public void abort(L2PcInstance activeChar)
	{
		_log.warning("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		if (_counterInstance != null)
		{
			_counterInstance._abort();
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		}
	}

	/**
	 * set the shutdown mode
	 * 
	 * @param mode
	 *            what mode shall be set
	 */
	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}

	/**
	 * set shutdown mode to ABORT
	 * 
	 */
	private void _abort()
	{
		_shutdownMode = ABORT;
	}

	/**
	 * this counts the countdown and reports it to all players countdown is
	 * aborted if mode changes to ABORT
	 */
	private void countdown()
	{

		try
		{
			while (_secondsShut > 0)
			{

				switch (_secondsShut)
				{
					case 540:
						SendServerQuit(540);
						break;
					case 480:
						SendServerQuit(480);
						break;
					case 420:
						SendServerQuit(420);
						break;
					case 360:
						SendServerQuit(360);
						break;
					case 300:
						SendServerQuit(300);
						break;
					case 240:
						SendServerQuit(240);
						break;
					case 180:
						SendServerQuit(180);
						break;
					case 120:
						SendServerQuit(120);
						break;
					case 60:
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN); // avoids
																									// new
																									// players
																									// from
																									// logging
																									// in
						SendServerQuit(60);
						break;
					case 30:
						SendServerQuit(30);
						break;
					case 10:
						SendServerQuit(10);
						break;
					case 5:
						SendServerQuit(5);
						break;
					case 4:
						SendServerQuit(4);
						break;
					case 3:
						SendServerQuit(3);
						break;
					case 2:
						SendServerQuit(2);
						break;
					case 1:
						SendServerQuit(1);
						break;
				}

				_secondsShut--;

				int delay = 1000; // milliseconds
				Thread.sleep(delay);

				if (_shutdownMode == ABORT)
					break;
			}
		}
		catch (InterruptedException e)
		{
			// this will never happen
		}
	}

	/**
	 * this sends a last byebye, disconnects all players and saves data
	 * 
	 */
	private void saveData()
	{
		switch (_shutdownMode)
		{
			case SIGTERM:
				_log.info("SIGTERM received. Shutting down NOW!");
				break;
			case GM_SHUTDOWN:
				_log.info("GM shutdown received. Shutting down NOW!");
				break;
			case GM_RESTART:
				_log.info("GM restart received. Restarting NOW!");
				break;

		}
		if (Config.ACTIVATE_POSITION_RECORDER)
			Universe.getInstance().implode(true);

		// we cannt abort shutdown anymore, so i removed the "if"
		disconnectAllCharacters();

		// Seven Signs data is now saved along with Festival data.
		if (!SevenSigns.getInstance().isSealValidationPeriod())
			SevenSignsFestival.getInstance().saveFestivalData(false);

		// Save Seven Signs data before closing. :)
		SevenSigns.getInstance().saveSevenSignsData(null, true);

		// Save all raidboss and GrandBoss status ^_^
		RaidBossSpawnManager.getInstance().cleanUp();
		_log.info("RaidBossSpawnManager: All raidboss info saved!!");
		GrandBossManager.getInstance().cleanUp();
		_log.info("GrandBossManager: All Grand Boss info saved!!");
		_log.info("TradeController saving data.. This action may take some minutes! Please wait until completed!");
		TradeController.getInstance().dataCountStore();
		_log.info("TradeController: All count Item Saved");
		Olympiad.getInstance().saveOlympiadStatus();
		_log.info("Olympiad System: Data saved!!");
		for (Savable sv : savables)
			sv.store();
		//GlobalVariablesManager.getInstance();
		// Save all manor data
		CastleManorManager.getInstance().save();

		// Save all global (non-player specific) Quest data that needs to
		// persist after reboot
		QuestManager.getInstance().save();


		// Save items on ground before closing
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			ItemsOnGroundManager.getInstance().cleanUp();
			_log.info("ItemsOnGroundManager: All items on ground saved!!");
		}

		CharSchemesTable.getInstance().onServerShutdown();

		_log.info("Data saved. All players disconnected, shutting down.");

		try
		{
			int delay = 5000;
			Thread.sleep(delay);
		}
		catch (InterruptedException e)
		{
			// never happens :p
		}
	}

	/**
	 * this disconnects all clients from the server
	 * 
	 */
	private void disconnectAllCharacters()
	{
		for (CursedWeapon weap : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if (weap != null)
				weap.endOfLife();
		}
		OfflineTradersTable.storeOffliners();
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (final L2PcInstance player : pls)
		{
			if (player == null)
				continue;
			// Logout Character
			try
			{
				final L2GameClient client = player.getClient();
				if (client != null && !client.isDetached())
				{
					client.close(ServerClose.STATIC_PACKET);
					client.setActiveChar(null);
					player.setClient(null);
				}
				player.deleteMe();
			}
			catch (final Throwable t)
			{
				_log.log(Level.WARNING, "Failed to logout chararacter: " + player, t);
			}
		}
	}
	
	public void restartLogin(L2PcInstance gm)
	{
		try
		{
			L2LoginServer.getInstance().shutdown(false);
			
			L2LoginServer.main(null);
			
			if(gm != null)
			{
				gm.sendMessage("LS restarted successfully.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void addShutdownHook(Savable sv)
	{	System.out.println("Added shutdown hook for " + sv.getClass().getCanonicalName());
		savables.add(sv);
	}

	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Shutdown _instance = new Shutdown();
	}

	public interface Savable
	{
		void store();
	}
}