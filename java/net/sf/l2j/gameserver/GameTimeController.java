package net.sf.l2j.gameserver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.8 $ $Date: 2005/04/06 16:13:24 $
 */
public class GameTimeController
{
	static final Logger							_log				= Logger.getLogger(GameTimeController.class.getName());
	public static final int						TICKS_PER_SECOND	= 10;													// not able to change this without checking through code
	public static final int						MILLIS_IN_TICK		= 1000 / TICKS_PER_SECOND;
	protected static int						_gameTicks;
	protected static long						_gameStartTime;
	protected static boolean					_isNight			= false;
	private static Map<Integer, L2Character>	_movingObjects		= new FastMap<Integer, L2Character>().shared();
	protected static TimerThread				_timer;
	private ScheduledFuture<?>					_timerWatcher;
	
	/**
	 * one ingame day is 240 real minutes
	 */
	public static GameTimeController getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private GameTimeController()
	{
		_gameStartTime = System.currentTimeMillis() - 3600000; // offset so that the server starts a day begin
		_gameTicks = 3600000 / MILLIS_IN_TICK; // offset so that the server starts a day begin
		_timer = new TimerThread();
		_timer.start();
		_timerWatcher = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimerWatcher(), 0, 1000);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BroadcastSunState(), 0, 600000);
	}
	
	public boolean isNowNight()
	{
		return _isNight;
	}
	
	public int getGameTime()
	{
		return (_gameTicks / (TICKS_PER_SECOND * 10));
	}
	
	public static int getGameTicks()
	{
		return _gameTicks;
	}
	
	/**
	 * Add a L2Character to movingObjects of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR>
	 * <BR>
	 *
	 * @param cha
	 *            The L2Character to add to movingObjects of GameTimeController
	 */
	public void registerMovingObject(L2Character cha)
	{
		if (cha == null)
			return;
		if (!_movingObjects.containsKey(cha.getObjectId()))
			_movingObjects.put(cha.getObjectId(), cha);
	}
	
	/**
	 * Move all L2Characters contained in movingObjects of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update the position of each L2Character</li>
	 * <li>If movement is finished, the L2Character is removed from movingObjects</li>
	 * <li>Create a task to update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED</li><BR>
	 * <BR>
	 */
	protected void moveObjects()
	{
		// Create an FastList to contain all L2Character that are arrived to
		// destination
		List<L2Character> ended = null;
		// Go throw the table containing L2Character in movement
		Collection<L2Character> mObjs = _movingObjects.values();
		// synchronized (_movingObjects)
		{
			for (L2Character ch : mObjs)
			{
				// If movement is finished, the L2Character is removed from
				// movingObjects and added to the ArrayList ended
				if (ch.updatePosition(_gameTicks))
				{
					if (ended == null)
						ended = new ArrayList<L2Character>();
					ended.add(ch);
				}
			}
			if (ended != null)
			{
				_movingObjects.values().removeAll(ended);
				for (L2Character ch : ended)
					ThreadPoolManager.getInstance().executeTask(new MovingObjectArrived(ch));
				ended.clear();
			}
		}
	}
	
	public void stopTimer()
	{
		_timerWatcher.cancel(true);
		_timer.interrupt();
	}
	
	class TimerThread extends Thread
	{
		protected Exception _error;
		
		public TimerThread()
		{
			super("GameTimeController");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
			_error = null;
		}
		
		@Override
		public void run()
		{
			try
			{
				for (;;)
				{
					int _oldTicks = _gameTicks; // save old ticks value to avoid moving objects 2x in same tick
					long runtime = System.currentTimeMillis() - _gameStartTime; // from server boot to now
					_gameTicks = (int) (runtime / MILLIS_IN_TICK); // new ticks value (ticks now)
					if (_oldTicks != _gameTicks)
						moveObjects(); // Runs possibly too often
					runtime = (System.currentTimeMillis() - _gameStartTime) - runtime;
					// calculate sleep time... time needed to next tick minus time it takes to call moveObjects()
					int sleepTime = 1 + MILLIS_IN_TICK - ((int) runtime) % MILLIS_IN_TICK;
					// _log.finest("TICK: "+_gameTicks);
					sleep(sleepTime);
				}
			}
			catch (Exception e)
			{
				_error = e;
			}
		}
	}
	
	class TimerWatcher implements Runnable
	{
		public void run()
		{
			if (!_timer.isAlive())
			{
				String time = (new SimpleDateFormat("HH:mm:ss")).format(new Date());
				_log.warning(time + " TimerThread stop with following error. restart it.");
				if (_timer._error != null)
					_timer._error.printStackTrace();
				_timer = new TimerThread();
				_timer.start();
			}
		}
	}
	
	/**
	 * Update the _knownObject and _knowPlayers of each L2Character that finished its movement and of their already known L2Object then notify AI with EVT_ARRIVED.<BR>
	 * <BR>
	 */
	class MovingObjectArrived implements Runnable
	{
		private final L2Character _ended;
		
		MovingObjectArrived(L2Character ended)
		{
			_ended = ended;
		}
		
		public void run()
		{
			try
			{
				if (_ended.hasAI()) // AI could be just disabled due to region turn off
				{
					if (Config.MOVE_BASED_KNOWNLIST)
						_ended.getKnownList().findObjects();
					_ended.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				}
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param rise
	 */
	class BroadcastSunState implements Runnable
	{
		public void run()
		{
			int h = (getGameTime() / 60) % 24; // Time in hour
			boolean tempIsNight = (h > 21 || h < 5);
			if (tempIsNight != _isNight)
			{ // If diff day/night state
				_isNight = tempIsNight; // Set current day/night varible to value of temp varible
				DayNightSpawnManager.getInstance().notifyChangeMode();
//				if (_isNight)
//				{
//					Announcements.getInstance().announceToAll("Night begins");
//					Announcements.getInstance().announceToAll("Pagan's Temple is open");
//				}
//				else
//				{
//					DayNightSpawnManager.getInstance().kickAllPlayersFromPTWhenNightIsOver();
//					Announcements.getInstance().announceToAll("Day begins");
//					Announcements.getInstance().announceToAll("The sun raised and the undeads vanished.");
//				}
			}
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GameTimeController _instance = new GameTimeController();
	}
}