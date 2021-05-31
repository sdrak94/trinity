/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * @author godson
 */
package net.sf.l2j.gameserver.model.olympiad;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

public class Olympiad
{
	protected static final Logger						_log							= Logger.getLogger(Olympiad.class.getName());
	protected static final Logger						_logResults						= Logger.getLogger("olympiad");
	public static Map<Integer, StatsSet>				_nobles;
	public static FastList<StatsSet>					_heroesToBe;
	public static FastList<L2PcInstance>				_nonClassBasedRegisters;
	public static Map<Integer, FastList<L2PcInstance>>	_classBasedRegisters;
	public static Map<Integer, Integer>					_noblesRank;
	private static final String							OLYMPIAD_DATA_FILE				= "config/olympiad.properties";
	public static final String							OLYMPIAD_HTML_PATH				= "data/html/olympiad/";
	private static final String							OLYMPIAD_LOAD_DATA				= "SELECT current_cycle, period, olympiad_end, validation_end, " + "next_weekly_change FROM olympiad_data WHERE id = 0";
	private static final String							OLYMPIAD_SAVE_DATA				= "INSERT INTO olympiad_data (id, current_cycle, " + "period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) " + "ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, " + "validation_end=?, next_weekly_change=?";
	private static final String							OLYMPIAD_LOAD_NOBLES			= "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, " + "characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, " + "olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn " + "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
	private static final String							OLYMPIAD_SAVE_NOBLES			= "INSERT INTO olympiad_nobles " + "(`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`," + "`competitions_drawn`) VALUES (?,?,?,?,?,?,?)";
	private static final String							OLYMPIAD_UPDATE_NOBLES			= "UPDATE olympiad_nobles SET " + "olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?";
	private static final String							OLYMPIAD_GET_HEROS				= "SELECT olympiad_nobles.charId, characters.char_name " + "FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId " + "AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 20 " + "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC";
	private static final String							GET_ALL_CLASSIFIED_NOBLESS		= "SELECT charId from olympiad_nobles_eom " + "WHERE competitions_done >= 20 ORDER BY olympiad_points DESC, competitions_done DESC";
	private static final String							GET_EACH_CLASS_LEADER			= "SELECT characters.char_name from olympiad_nobles_eom, characters " + "WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? " + "AND olympiad_nobles_eom.competitions_done >= 20 " + "ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC LIMIT 10";
	private static final String							GET_EACH_CLASS_LEADER_CURRENT	= "SELECT characters.char_name from olympiad_nobles, characters " + "WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? " + "AND olympiad_nobles.competitions_done >= 20 " + "ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC LIMIT 10";
	private static final String							OLYMPIAD_DELETE_ALL				= "TRUNCATE olympiad_nobles";
	private static final String							OLYMPIAD_MONTH_CLEAR			= "TRUNCATE olympiad_nobles_eom";
	private static final String							OLYMPIAD_MONTH_CREATE			= "INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles";
	private static final int[]							HERO_IDS						=
	{
		88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103,
		104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 131, 132, 133, 134, 136
	};
	private static final int							COMP_START						= Config.ALT_OLY_START_TIME;																																																																																	// 6PM
	private static final int							COMP_MIN						= Config.ALT_OLY_MIN;																																																																																			// 00
																																																																																																														// mins
	private static final long							COMP_PERIOD						= Config.ALT_OLY_CPERIOD;																																																																																		// 6
																																																																																																														// hours
	protected static final long							WEEKLY_PERIOD					= Config.ALT_OLY_WPERIOD;																																																																																		// 1
																																																																																																														// week
	protected static final long							VALIDATION_PERIOD				= Config.ALT_OLY_VPERIOD;																																																																																		// 24
																																																																																																														// hours
	private static final int							DEFAULT_POINTS					= 35;
	protected static final int							WEEKLY_POINTS					= 5;
	public static final String							CHAR_ID							= "charId";
	public static final String							CLASS_ID						= "class_id";
	public static final String							CHAR_NAME						= "char_name";
	public static final String							POINTS							= "olympiad_points";
	public static final String							COMP_DONE						= "competitions_done";
	public static final String							COMP_WON						= "competitions_won";
	public static final String							COMP_LOST						= "competitions_lost";
	public static final String							COMP_DRAWN						= "competitions_drawn";
	protected long										_olympiadEnd;
	protected long										_validationEnd;
	/**
	 * The current period of the olympiad.<br>
	 * <b>0 -</b> Competition period<br>
	 * <b>1 -</b> Validation Period
	 */
	protected int										_period;
	protected long										_nextWeeklyChange;
	protected int										_currentCycle;
	private long										_compEnd;
	private Calendar									_compStart;
	protected static boolean							_inCompPeriod;
	protected static boolean							_compStarted					= false;
	protected ScheduledFuture<?>						_scheduledCompStart;
	protected ScheduledFuture<?>						_scheduledCompEnd;
	protected ScheduledFuture<?>						_scheduledOlympiadEnd;
	protected ScheduledFuture<?>						_scheduledWeeklyTask;
	protected ScheduledFuture<?>						_scheduledValdationTask;
	
	protected static enum COMP_TYPE
	{
		CLASSED,
		NON_CLASSED
	}
	
	public static Olympiad getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private Olympiad()
	{
		load();
		if (_period == 0)
			init();
	}
	
	public static Integer getStadiumCount()
	{
		return OlympiadManager.STADIUMS.length;
	}
	
	private void load()
	{
		_nobles = new FastMap<Integer, StatsSet>();
		Connection con = null;
		boolean loaded = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_DATA);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_period = rset.getInt("period");
				_olympiadEnd = rset.getLong("olympiad_end");
				_validationEnd = rset.getLong("validation_end");
				_nextWeeklyChange = rset.getLong("next_weekly_change");
				loaded = true;
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Olympiad System: Error loading olympiad data from database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		if (!loaded)
		{
			_log.log(Level.INFO, "Olympiad System: failed to load data from database, trying to load from file.");
			Properties OlympiadProperties = new Properties();
			InputStream is = null;
			try
			{
				is = new FileInputStream(new File("./" + OLYMPIAD_DATA_FILE));
				OlympiadProperties.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Olympiad System: Error loading olympiad properties: ", e);
				return;
			}
			finally
			{
				try
				{
					is.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			_currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
			_period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
			_olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
			_validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValidationEnd", "0"));
			_nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));
		}
		switch (_period)
		{
			case 0:
				if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
					setNewOlympiadEnd();
				else
					scheduleWeeklyChange();
				break;
			case 1:
				if (_validationEnd > Calendar.getInstance().getTimeInMillis())
				{
					loadNoblesRank();
					_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
				}
				else
				{
					_currentCycle++;
					_period = 0;
					deleteNobles();
					setNewOlympiadEnd();
				}
				break;
			default:
				_log.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				return;
		}
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				StatsSet statData = new StatsSet();
				int charId = rset.getInt(CHAR_ID);
				statData.set(CLASS_ID, rset.getInt(CLASS_ID));
				statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
				statData.set(POINTS, rset.getInt(POINTS));
				statData.set(COMP_DONE, rset.getInt(COMP_DONE));
				statData.set(COMP_WON, rset.getInt(COMP_WON));
				statData.set(COMP_LOST, rset.getInt(COMP_LOST));
				statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				statData.set("to_save", false);
				_nobles.put(charId, statData);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		synchronized (this)
		{
			_log.info("Olympiad System: Loading Olympiad System....");
			if (_period == 0)
				_log.info("Olympiad System: Currently in Olympiad Period");
			else
				_log.info("Olympiad System: Currently in Validation Period");
			long milliToEnd;
			if (_period == 0)
				milliToEnd = getMillisToOlympiadEnd();
			else
				milliToEnd = getMillisToValidationEnd();
			_log.info("Olympiad System: " + Math.round(milliToEnd / 60000) + " minutes until period ends");
			if (_period == 0)
			{
				milliToEnd = getMillisToWeekChange();
				_log.info("Olympiad System: Next weekly change is in " + Math.round(milliToEnd / 60000) + " minutes");
			}
		}
		_log.info("Olympiad System: Loaded " + _nobles.size() + " Nobles");
	}
	
	public void loadNoblesRank()
	{
		_noblesRank = new FastMap<Integer, Integer>();
		Map<Integer, Integer> tmpPlace = new FastMap<Integer, Integer>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(GET_ALL_CLASSIFIED_NOBLESS);
			ResultSet rset = statement.executeQuery();
			int place = 1;
			while (rset.next())
			{
				tmpPlace.put(rset.getInt(CHAR_ID), place++);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database for Ranking: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		int rank1 = (int) Math.round(tmpPlace.size() * 0.01);
		int rank2 = (int) Math.round(tmpPlace.size() * 0.10);
		int rank3 = (int) Math.round(tmpPlace.size() * 0.25);
		int rank4 = (int) Math.round(tmpPlace.size() * 0.50);
		if (rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}
		for (int charId : tmpPlace.keySet())
		{
			if (tmpPlace.get(charId) <= rank1)
				_noblesRank.put(charId, 1);
			else if (tmpPlace.get(charId) <= rank2)
				_noblesRank.put(charId, 2);
			else if (tmpPlace.get(charId) <= rank3)
				_noblesRank.put(charId, 3);
			else if (tmpPlace.get(charId) <= rank4)
				_noblesRank.put(charId, 4);
			else
				_noblesRank.put(charId, 5);
		}
	}
	
	protected void init()
	{
		if (_period == 1)
			return;
		_nonClassBasedRegisters = new FastList<L2PcInstance>();
		_classBasedRegisters = new FastMap<Integer, FastList<L2PcInstance>>();
		_compStart = Calendar.getInstance();
		switch (_compStart.get(Calendar.DAY_OF_WEEK))
		{
			case Calendar.MONDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.TUESDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 3);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.WEDNESDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 6);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.THURSDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 9);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.FRIDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 12);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.SATURDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 15);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.SUNDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 18);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
		}
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;
		if (_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), getMillisToOlympiadEnd());
		updateCompStatus();
	}
	
	protected class OlympiadEndTask implements Runnable
	{
		public void run()
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED);
			sm.addNumber(_currentCycle);
			Announcements.getInstance().announceToAll(sm);
			Announcements.getInstance().announceToAll("Olympiad Validation Period has began");
			try
			{
				if (_scheduledWeeklyTask != null)
					_scheduledWeeklyTask.cancel(false);
				_period = 1;
				try
				{
					sortHerosToBe();
					Hero.getInstance().computeNewHeroes();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					saveOlympiadStatus();
					updateMonthlyData();
					Calendar validationEnd = Calendar.getInstance();
					_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;
					loadNoblesRank();
					_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValidationEndTask(), getMillisToValidationEnd());
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected class ValidationEndTask implements Runnable
	{
		public void run()
		{
			Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
			_period = 0;
			_currentCycle++;
			deleteNobles();
			setNewOlympiadEnd();
			init();
		}
	}
	
	public boolean registerNoble(L2PcInstance noble, boolean classBased)
	{
		SystemMessage sm;
		if (!_inCompPeriod)
		{
			sm = new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}
		if (!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(noble);
			noble.sendPacket(sm);
			return false;
		}
		/** Begin Olympiad Restrictions */
		if (noble.getBaseClassId() != noble.getClassId().getId())
		{
			sm = new SystemMessage(SystemMessageId.C1_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_CLASS_CHARACTER);
			sm.addPcName(noble);
			noble.sendPacket(sm);
			return false;
		}
		if (noble._inEventTvT || noble._inEventFOS || noble._inEventHG || noble._inEventCTF || noble._inEventDM || noble._inEventVIP || noble.getInstanceId() != 0)
		{
			sm = new SystemMessage(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			noble.sendPacket(sm);
			return false;
		}
		if (noble.isCursedWeaponEquipped())
		{
			sm = new SystemMessage(SystemMessageId.C1_CANNOT_JOIN_OLYMPIAD_POSSESSING_S2);
			sm.addPcName(noble);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			noble.sendPacket(sm);
			return false;
		}
		if (noble.getLevel() < 91)
		{
			noble.sendMessage("You must be level 91 or higher to participate in the Olympiad.");
			return false;
		}
		if (noble.getPvpKills() < 1000)
		{
			noble.sendMessage("You must have at least 100 PvPs to participate in the Olympiad.");
			return false;
		}
		if (getMillisToCompEnd() < 600000)
		{
			sm = new SystemMessage(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			noble.sendPacket(sm);
			return false;
		}
		if (TvTEvent.isPlayerParticipant(noble.getObjectId()))
		{
			noble.sendMessage("You can't join olympiad while participating on TvT Event.");
			return false;
		}
		/** End Olympiad Restrictions */
		if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			for (L2PcInstance participant : classed)
			{
				if (participant.getObjectId() == noble.getObjectId())
				{
					sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
					sm.addPcName(noble);
					noble.sendPacket(sm);
					return false;
				}
			}
		}
		if (isRegisteredInComp(noble))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
			sm.addPcName(noble);
			noble.sendPacket(sm);
			return false;
		}
		if (!_nobles.containsKey(noble.getObjectId()))
		{
			StatsSet statDat = new StatsSet();
			statDat.set(CLASS_ID, noble.getClassId().getId());
			statDat.set(CHAR_NAME, noble.getName());
			statDat.set(POINTS, DEFAULT_POINTS);
			statDat.set(COMP_DONE, 0);
			statDat.set(COMP_WON, 0);
			statDat.set(COMP_LOST, 0);
			statDat.set(COMP_DRAWN, 0);
			statDat.set("to_save", true);
			_nobles.put(noble.getObjectId(), statDat);
		}
		if (classBased && getNoblePoints(noble.getObjectId()) < 3)
		{
			noble.sendMessage("Cant register when you have less than 3 points");
			return false;
		}
		if (!classBased && getNoblePoints(noble.getObjectId()) < 5)
		{
			noble.sendMessage("Cant register when you have less than 5 points");
			return false;
		}
		if (classBased)
		{
			if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
			{
				FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
				classed.add(noble);
				_classBasedRegisters.remove(noble.getClassId().getId());
				_classBasedRegisters.put(noble.getClassId().getId(), classed);
			}
			else
			{
				FastList<L2PcInstance> classed = new FastList<L2PcInstance>();
				classed.add(noble);
				_classBasedRegisters.put(noble.getClassId().getId(), classed);
			}
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
			noble.sendPacket(sm);
		}
		else
		{
			_nonClassBasedRegisters.add(noble);
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
			noble.clearPath();
			noble.sendPacket(sm);
		}
		return true;
	}
	
	protected static int getNobleCount()
	{
		return _nobles.size();
	}
	
	public static StatsSet getNobleStats(int playerId)
	{
		return _nobles.get(playerId);
	}
	
	protected static synchronized void updateNobleStats(int playerId, StatsSet stats)
	{
		_nobles.remove(playerId);
		_nobles.put(playerId, stats);
	}
	
	protected static FastList<L2PcInstance> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}
	
	protected static Map<Integer, FastList<L2PcInstance>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}
	
	protected static FastList<Integer> hasEnoughRegisteredClassed()
	{
		FastList<Integer> result = new FastList<Integer>();
		for (Integer classList : getRegisteredClassBased().keySet())
		{
			if (getRegisteredClassBased().get(classList).size() >= Config.ALT_OLY_CLASSED)
			{
				result.add(classList);
			}
		}
		if (!result.isEmpty())
		{
			return result;
		}
		return null;
	}
	
	protected static boolean hasEnoughRegisteredNonClassed()
	{
		return Olympiad.getRegisteredNonClassBased().size() >= Config.ALT_OLY_NONCLASSED;
	}
	
	protected static void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
	}
	
	public boolean isRegistered(L2PcInstance noble)
	{
		boolean result = false;
		if (_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(noble))
			result = true;
		else if (_classBasedRegisters != null && _classBasedRegisters.containsKey(noble.getClassId().getId()))
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			if (classed != null && classed.contains(noble))
				result = true;
		}
		return result;
	}
	
	public boolean unRegisterNoble(L2PcInstance noble)
	{
		SystemMessage sm;
		/*
		 * if (_compStarted) {
		 * noble.sendMessage("Cant Unregister whilst competition is under way");
		 * return false; }
		 */
		if (!_inCompPeriod)
		{
			sm = new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}
		if (!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		if (!isRegistered(noble))
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			noble.sendPacket(sm);
			return false;
		}
		for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
		{
			if (game == null)
				continue;
			if (game._playerOneID == noble.getObjectId() || game._playerTwoID == noble.getObjectId())
			{
				noble.sendMessage("Can't deregister whilst you are already selected for a game");
				return false;
			}
		}
		if (_nonClassBasedRegisters.contains(noble))
			_nonClassBasedRegisters.remove(noble);
		else
		{
			FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			classed.remove(noble);
			_classBasedRegisters.remove(noble.getClassId().getId());
			_classBasedRegisters.put(noble.getClassId().getId(), classed);
		}
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
		noble.sendPacket(sm);
		return true;
	}
	
	public void removeDisconnectedCompetitor(L2PcInstance player)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()) != null)
			OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()).handleDisconnect(player);
		FastList<L2PcInstance> classed = _classBasedRegisters.get(player.getClassId().getId());
		if (_nonClassBasedRegisters.contains(player))
			_nonClassBasedRegisters.remove(player);
		else if (classed != null && classed.contains(player))
		{
			classed.remove(player);
			_classBasedRegisters.remove(player.getClassId().getId());
			_classBasedRegisters.put(player.getClassId().getId(), classed);
		}
	}
	
	public void notifyCompetitorDamage(L2PcInstance player, int damage, int gameId)
	{
		if (OlympiadManager.getInstance().getOlympiadGames().get(gameId) != null)
			OlympiadManager.getInstance().getOlympiadGames().get(gameId).addDamage(player, damage);
	}
	
	private void updateCompStatus()
	{
		if(Config.ENABLE_OLD_OLY)
		{
			// _compStarted = false;
			synchronized (this)
			{
				long milliToStart = getMillisToCompBegin();
				double numSecs = (milliToStart / 1000) % 60;
				double countDown = ((milliToStart / 1000) - numSecs) / 60;
				int numMins = (int) Math.floor(countDown % 60);
				countDown = (countDown - numMins) / 60;
				int numHours = (int) Math.floor(countDown % 24);
				int numDays = (int) Math.floor((countDown - numHours) / 24);
				_log.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
				_log.info("Olympiad System: Event starts/started : " + _compStart.getTime());
			}
			_scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					if (isOlympiadEnd())
						return;
					_inCompPeriod = true;
					OlympiadManager om = OlympiadManager.getInstance();
					Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
					_log.info("Olympiad System: Olympiad Game Started");
					_logResults.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
					Thread olyCycle = new Thread(om);
					olyCycle.start();
					long regEnd = getMillisToCompEnd() - 600000;
					if (regEnd > 0)
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							public void run()
							{
								Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.OLYMPIAD_REGISTRATION_PERIOD_ENDED));
							}
						}, regEnd);
					}
					_scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						public void run()
						{
							if (isOlympiadEnd())
								return;
							_inCompPeriod = false;
							Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
							_log.info("Olympiad System: Olympiad Game Ended");
							while (OlympiadGame._battleStarted)
							{
								try
								{
									// wait 1 minutes for end of pendings games
									Thread.sleep(60000);
								}
								catch (InterruptedException e)
								{}
							}
							saveOlympiadStatus();
							init();
						}
					}, getMillisToCompEnd());
				}
			}, getMillisToCompBegin());
		}	
	}
	
	public void openOly()
	{
		synchronized (this)
		{
			long milliToStart = getMillisToCompBegin();
			double numSecs = (milliToStart / 1000) % 60;
			double countDown = ((milliToStart / 1000) - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);
			_log.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
			_log.info("Olympiad System: Event starts/started : " + _compStart.getTime());
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (isOlympiadEnd())
					return;
				_inCompPeriod = true;
				OlympiadManager om = OlympiadManager.getInstance();
				Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
				_log.info("Olympiad System: Olympiad Game Started");
				_logResults.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
				Thread olyCycle = new Thread(om);
				olyCycle.start();
			}
		}, 1000);
	}
	
	public void closeOly()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				if (isOlympiadEnd())
					return;
				_inCompPeriod = false;
				Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
				_log.info("Olympiad System: Olympiad Game Ended");
				while (OlympiadGame._battleStarted)
				{
					try
					{
						// wait 1 minutes for end of pendings games
						Thread.sleep(60000);
					}
					catch (InterruptedException e)
					{}
				}
				saveOlympiadStatus();
				init();
			}
		}, 1000);
	}
	
	private long getMillisToOlympiadEnd()
	{
		// if (_olympiadEnd > Calendar.getInstance().getTimeInMillis())
		return (_olympiadEnd - Calendar.getInstance().getTimeInMillis());
		// return 10L;
	}
	
	public void manualSelectHeroes()
	{
		if (_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);
		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new OlympiadEndTask(), 0);
	}
	
	protected long getMillisToValidationEnd()
	{
		if (_validationEnd > Calendar.getInstance().getTimeInMillis())
			return (_validationEnd - Calendar.getInstance().getTimeInMillis());
		return 10L;
	}
	
	public boolean isOlympiadEnd()
	{
		return (_period != 0);
	}
	
	protected void setNewOlympiadEnd()
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED);
		sm.addNumber(_currentCycle);
		Announcements.getInstance().announceToAll(sm);
		Calendar currentTime = Calendar.getInstance();
		currentTime.setFirstDayOfWeek(Calendar.SUNDAY);
		currentTime.add(Calendar.DAY_OF_YEAR, 8);
		currentTime.roll(Calendar.DAY_OF_WEEK, 7);
		if (currentTime.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
		{
			_log.warning("OMG!!!!!!!!!!!!!!!!!!!!! Olympiad end day is not a Saturday!!!");
			currentTime.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		}
		currentTime.set(Calendar.HOUR_OF_DAY, COMP_START + 17);
		currentTime.set(Calendar.MINUTE, 5);
		currentTime.set(Calendar.SECOND, 0);
		_log.warning("NEW OLY END TIME: " + currentTime.getTime().toString());
		_olympiadEnd = currentTime.getTimeInMillis();
		Calendar nextChange = Calendar.getInstance();
		_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
		scheduleWeeklyChange();
	}
	
	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}
	
	public long getMillisToCompBegin()
	{
		if (_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis())
			return 10L;
		if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
			return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
		return setNewCompBegin();
	}
	
	private long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		switch (_compStart.get(Calendar.DAY_OF_WEEK))
		{
			case Calendar.MONDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.TUESDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 3);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.WEDNESDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 6);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.THURSDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 9);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.FRIDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 12);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.SATURDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 15);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
			case Calendar.SUNDAY:
				_compStart.set(Calendar.HOUR_OF_DAY, COMP_START + 18);
				_compStart.set(Calendar.MINUTE, COMP_MIN);
				_compStart.set(Calendar.SECOND, 0);
				break;
		}
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;
		_log.info("Olympiad System: New Schedule @ " + _compStart.getTime());
		return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}
	
	protected long getMillisToCompEnd()
	{
		// if (_compEnd > Calendar.getInstance().getTimeInMillis())
		return (_compEnd - Calendar.getInstance().getTimeInMillis());
		// return 10L;
	}
	
	private long getMillisToWeekChange()
	{
		if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
			return (_nextWeeklyChange - Calendar.getInstance().getTimeInMillis());
		return 10L;
	}
	
	private void scheduleWeeklyChange()
	{
		_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			public void run()
			{
				addWeeklyPoints();
				_log.info("Olympiad System: Added weekly points to nobles");
				Calendar nextChange = Calendar.getInstance();
				_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
			}
		}, getMillisToWeekChange(), WEEKLY_PERIOD);
	}
	
	protected synchronized void addWeeklyPoints()
	{
		if (_period == 1)
			return;
		for (Integer nobleId : _nobles.keySet())
		{
			StatsSet nobleInfo = _nobles.get(nobleId);
			int currentPoints = nobleInfo.getInteger(POINTS);
			currentPoints += WEEKLY_POINTS;
			nobleInfo.set(POINTS, currentPoints);
			updateNobleStats(nobleId, nobleInfo);
		}
	}
	
	public FastMap<Integer, String> getMatchList()
	{
		return OlympiadManager.getInstance().getAllTitles();
	}
	
	// returns the players for the given olympiad game Id
	public L2PcInstance[] getPlayers(int Id)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(Id) == null)
			return null;
		else
			return OlympiadManager.getInstance().getOlympiadGame(Id).getPlayers();
	}
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public static void addSpectator(int id, L2PcInstance spectator, boolean storeCoords)
	{
		if (getInstance().isRegisteredInComp(spectator))
		{
			spectator.sendPacket(new SystemMessage(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME));
			return;
		}
		if (spectator._inEventCTF || spectator._inEventFOS || spectator._inEventTvT || spectator._inEventHG || spectator._inEventVIP || spectator._inEventDM)
		{
			spectator.sendMessage("You can not observe games while registered for an event.");
			return;
		}
		if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(spectator.getObjectId()))
		{
			spectator.sendMessage("You can not observe games while registered for TvT");
			return;
		}
		OlympiadManager.STADIUMS[id].addSpectator(id, spectator, storeCoords);
	}
	
	public static int getSpectatorArena(L2PcInstance player)
	{
		for (int i = 0; i < OlympiadManager.STADIUMS.length; i++)
		{
			if (OlympiadManager.STADIUMS[i].getSpectators().contains(player))
				return i;
		}
		return -1;
	}
	
	public static void removeSpectator(int id, L2PcInstance spectator)
	{
		try
		{
			OlympiadManager.STADIUMS[id].removeSpectator(spectator);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{}
	}
	
	public FastList<L2PcInstance> getSpectators(int id)
	{
		try
		{
			if (OlympiadManager.getInstance().getOlympiadGame(id) == null)
				return null;
			return OlympiadManager.STADIUMS[id].getSpectators();
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return null;
		}
	}
	
	public Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return OlympiadManager.getInstance().getOlympiadGames();
	}
	
	public boolean playerInStadia(L2PcInstance player)
	{
		return (ZoneManager.getInstance().getOlympiadStadium(player) != null);
	}
	
	public int[] getWaitingList()
	{
		int[] array = new int[2];
		if (!inCompPeriod())
			return null;
		int classCount = 0;
		if (_classBasedRegisters.size() != 0)
			for (FastList<L2PcInstance> classed : _classBasedRegisters.values())
			{
				classCount += classed.size();
			}
		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();
		return array;
	}
	
	/**
	 * Save noblesse data to database
	 */
	protected synchronized void saveNobleData()
	{
		if (_nobles == null || _nobles.isEmpty())
			return;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			for (Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);
				if (nobleInfo == null)
					continue;
				int charId = nobleId;
				int classId = nobleInfo.getInteger(CLASS_ID);
				int points = nobleInfo.getInteger(POINTS);
				int compDone = nobleInfo.getInteger(COMP_DONE);
				int compWon = nobleInfo.getInteger(COMP_WON);
				int compLost = nobleInfo.getInteger(COMP_LOST);
				int compDrawn = nobleInfo.getInteger(COMP_DRAWN);
				boolean toSave = nobleInfo.getBool("to_save");
				if (toSave)
				{
					statement = con.prepareStatement(OLYMPIAD_SAVE_NOBLES);
					statement.setInt(1, charId);
					statement.setInt(2, classId);
					statement.setInt(3, points);
					statement.setInt(4, compDone);
					statement.setInt(5, compWon);
					statement.setInt(6, compLost);
					statement.setInt(7, compDrawn);
					nobleInfo.set("to_save", false);
					updateNobleStats(nobleId, nobleInfo);
				}
				else
				{
					statement = con.prepareStatement(OLYMPIAD_UPDATE_NOBLES);
					statement.setInt(1, points);
					statement.setInt(2, compDone);
					statement.setInt(3, compWon);
					statement.setInt(4, compLost);
					statement.setInt(5, compDrawn);
					statement.setInt(6, charId);
				}
				statement.execute();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "Olympiad System: Failed to save noblesse data to database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void wipeNoble(int nobleId)
	{
		if (_nobles == null || _nobles.isEmpty())
			return;
		_nobles.remove(nobleId);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE from olympiad_nobles WHERE charId=?");
			statement.setInt(1, nobleId);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "Olympiad System: Failed to wipe noblesse data: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Save olympiad.properties file with current olympiad status and update noblesse table in database
	 */
	public void saveOlympiadStatus()
	{
		/* saveNobleData(); */
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_SAVE_DATA);
			statement.setInt(1, _currentCycle);
			statement.setInt(2, _period);
			statement.setLong(3, _olympiadEnd);
			statement.setLong(4, _validationEnd);
			statement.setLong(5, _nextWeeklyChange);
			statement.setInt(6, _currentCycle);
			statement.setInt(7, _period);
			statement.setLong(8, _olympiadEnd);
			statement.setLong(9, _validationEnd);
			statement.setLong(10, _nextWeeklyChange);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "Olympiad System: Failed to save olympiad data to database: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		/*
		 * Properties OlympiadProperties = new Properties();
		 * FileOutputStream fos = null;
		 * try
		 * {
		 * fos = new FileOutputStream(new File("./" + OLYMPIAD_DATA_FILE));
		 * OlympiadProperties.setProperty("CurrentCycle", String.valueOf(_currentCycle));
		 * OlympiadProperties.setProperty("Period", String.valueOf(_period));
		 * OlympiadProperties.setProperty("OlympiadEnd", String.valueOf(_olympiadEnd));
		 * OlympiadProperties.setProperty("ValdationEnd", String.valueOf(_validationEnd));
		 * OlympiadProperties.setProperty("NextWeeklyChange", String.valueOf(_nextWeeklyChange));
		 * OlympiadProperties.store(fos, "Olympiad Properties");
		 * }
		 * catch (Exception e)
		 * {
		 * _log.log(Level.WARNING, "Olympiad System: Unable to save olympiad properties to file: ", e);
		 * }
		 * finally
		 * {
		 * try
		 * {
		 * fos.close();
		 * }
		 * catch (Exception e)
		 * {
		 * }
		 * }
		 */
	}
	
	protected void updateMonthlyData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
			statement.execute();
			statement.close();
			statement = con.prepareStatement(OLYMPIAD_MONTH_CREATE);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Olympiad System: Failed to update monthly noblese data: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	protected void sortHerosToBe()
	{
		if (_period != 1)
			return;
		LogRecord record;
		if (_nobles != null)
		{
			_logResults.info("Noble,charid,classid,compDone,points");
			for (Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);
				if (nobleInfo == null)
					continue;
				int charId = nobleId;
				int classId = nobleInfo.getInteger(CLASS_ID);
				String charName = nobleInfo.getString(CHAR_NAME);
				int points = nobleInfo.getInteger(POINTS);
				int compDone = nobleInfo.getInteger(COMP_DONE);
				record = new LogRecord(Level.INFO, charName);
				record.setParameters(new Object[]
				{
					charId, classId, compDone, points
				});
				_logResults.log(record);
			}
		}
		_heroesToBe = new FastList<StatsSet>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			StatsSet hero;
			FastList<StatsSet> soulHounds = new FastList<StatsSet>();
			for (int element : HERO_IDS)
			{
				statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
				statement.setInt(1, element);
				rset = statement.executeQuery();
				if (rset.next())
				{
					hero = new StatsSet();
					hero.set(CLASS_ID, element);
					hero.set(CHAR_ID, rset.getInt(CHAR_ID));
					hero.set(CHAR_NAME, rset.getString(CHAR_NAME));
					if (element == 132 || element == 133) // Male & Female Soulhounds rank as one hero class
					{
						hero = _nobles.get(hero.getInteger(CHAR_ID));
						hero.set(CHAR_ID, rset.getInt(CHAR_ID));
						soulHounds.add(hero);
					}
					else
					{
						record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
						record.setParameters(new Object[]
						{
							hero.getInteger(CHAR_ID), hero.getInteger(CLASS_ID)
						});
						_logResults.log(record);
						_heroesToBe.add(hero);
					}
				}
				statement.close();
				rset.close();
			}
			switch (soulHounds.size())
			{
				case 0:
				{
					break;
				}
				case 1:
				{
					hero = new StatsSet();
					StatsSet winner = soulHounds.get(0);
					hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
					hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
					record.setParameters(new Object[]
					{
						hero.getInteger(CHAR_ID), hero.getInteger(CLASS_ID)
					});
					_logResults.log(record);
					_heroesToBe.add(hero);
					break;
				}
				case 2:
				{
					hero = new StatsSet();
					StatsSet winner;
					StatsSet hero1 = soulHounds.get(0);
					StatsSet hero2 = soulHounds.get(1);
					int hero1Points = hero1.getInteger(POINTS);
					int hero2Points = hero2.getInteger(POINTS);
					int hero1Comps = Olympiad.getCompetitionDone(hero1.getInteger(CHAR_ID));
					int hero2Comps = Olympiad.getCompetitionDone(hero2.getInteger(CHAR_ID));
					if (hero1Points > hero2Points)
						winner = hero1;
					else if (hero2Points > hero1Points)
						winner = hero2;
					else
					{
						if (hero1Comps > hero2Comps)
							winner = hero1;
						else
							winner = hero2;
					}
					hero.set(CLASS_ID, winner.getInteger(CLASS_ID));
					hero.set(CHAR_ID, winner.getInteger(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
					record.setParameters(new Object[]
					{
						hero.getInteger(CHAR_ID), hero.getInteger(CLASS_ID)
					});
					_logResults.log(record);
					_heroesToBe.add(hero);
					break;
				}
			}
		}
		catch (SQLException e)
		{
			_log.warning("Olympiad System: Couldnt load heros from DB");
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public FastList<String> getClassLeaderBoard(int classId)
	{
		// if (_period != 1) return;
		FastList<String> names = new FastList<String>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			if (Config.ALT_OLY_SHOW_MONTHLY_WINNERS)
				statement = con.prepareStatement(GET_EACH_CLASS_LEADER);
			else
				statement = con.prepareStatement(GET_EACH_CLASS_LEADER_CURRENT);
			statement.setInt(1, classId);
			rset = statement.executeQuery();
			while (rset.next())
			{
				names.add(rset.getString(CHAR_NAME));
			}
			if (classId == 132) // Male & Female SoulHounds are ranked together
			{
				statement.setInt(1, 133);
				rset = statement.executeQuery();
				while (rset.next())
				{
					names.add(rset.getString(CHAR_NAME));
				}
			}
			statement.close();
			rset.close();
			return names;
		}
		catch (SQLException e)
		{
			_log.warning("Olympiad System: Couldnt load olympiad leaders from DB");
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return names;
	}
	
	public int getNoblessePasses(L2PcInstance player, boolean clear)
	{
		if (_period != 1 || _noblesRank == null || _noblesRank.isEmpty())
			return 0;
		int objId = player.getObjectId();
		if (!_noblesRank.containsKey(objId))
			return 0;
		StatsSet noble = _nobles.get(objId);
		if (noble.getInteger(POINTS) == 0)
			return 0;
		int rank = _noblesRank.get(objId);
		int points = (player.isHero() ? Config.ALT_OLY_HERO_POINTS : 0);
		switch (rank)
		{
			case 1:
				points += Config.ALT_OLY_RANK1_POINTS;
				break;
			case 2:
				points += Config.ALT_OLY_RANK2_POINTS;
				break;
			case 3:
				points += Config.ALT_OLY_RANK3_POINTS;
				break;
			case 4:
				points += Config.ALT_OLY_RANK4_POINTS;
				break;
			default:
				points += Config.ALT_OLY_RANK5_POINTS;
		}
		if (clear)
		{
			noble.set(POINTS, 0);
			updateNobleStats(objId, noble);
		}
		points *= Config.ALT_OLY_GP_PER_POINT;
		return points;
	}
	
	public boolean isRegisteredInComp(L2PcInstance player)
	{
		boolean result = isRegistered(player);
		if (_inCompPeriod)
		{
			for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
			{
				if ((game._playerOneID == player.getObjectId()) || (game._playerTwoID == player.getObjectId()))
				{
					result = true;
					break;
				}
			}
		}
		return result;
	}
	
	public int getNoblePoints(int objId)
	{
		if (_nobles.isEmpty())
			return 0;
		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(POINTS);
		return points;
	}
	
	public int getLastNobleOlympiadPoints(int objId)
	{
		int result = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?");
			statement.setInt(1, objId);
			ResultSet rs = statement.executeQuery();
			if (rs.first())
				result = rs.getInt(1);
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not load last olympiad points:", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return result;
	}
	
	public int getLastNobleOlympiadGamesPlayed(int objId)
	{
		String states = "SELECT competitions_done FROM olympiad_nobles_eom WHERE charId = ?";
		if (_period == 1) // validation period
			states = "SELECT competitions_done FROM olympiad_nobles WHERE charId = ?";
		int result = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement(states);
			statement.setInt(1, objId);
			ResultSet rs = statement.executeQuery();
			if (rs.first())
				result = rs.getInt(1);
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not load last olympiad games count:", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return result;
	}
	
	public static int getCompetitionDone(int objId)
	{
		if (_nobles.isEmpty())
			return 0;
		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_DONE);
		return points;
	}
	
	public int getCompetitionWon(int objId)
	{
		if (_nobles.isEmpty())
			return 0;
		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_WON);
		return points;
	}
	
	public int getCompetitionLost(int objId)
	{
		if (_nobles.isEmpty())
			return 0;
		StatsSet noble = _nobles.get(objId);
		if (noble == null)
			return 0;
		int points = noble.getInteger(COMP_LOST);
		return points;
	}
	
	protected void deleteNobles()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warning("Olympiad System: Couldnt delete nobles from DB");
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		_nobles.clear();
	}
	
	public static void sendMatchList(L2PcInstance player)
	{
		NpcHtmlMessage message = new NpcHtmlMessage(0);
		message.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe2.htm");
		FastMap<Integer, String> matches = getInstance().getMatchList();
		for (int i = 0; i < Olympiad.getStadiumCount(); i++)
		{
			int arenaId = i + 1;
			String state = "Initial State";
			String players = "&nbsp;";
			if (matches.containsKey(i))
			{
				if (OlympiadGame._gameIsStarted)
					state = "Playing";
				else
					state = "Standby";
				players = matches.get(i);
			}
			message.replace("%state" + arenaId + "%", state);
			message.replace("%players" + arenaId + "%", players);
		}
		player.sendPacket(message);
	}
	
	public static void bypassChangeArena(String command, L2PcInstance player)
	{
		if (!player.inObserverMode())
			return;
		String[] commands = command.split(" ");
		int id = Integer.parseInt(commands[1]);
		int arena = getSpectatorArena(player);
		if (arena >= 0)
			Olympiad.removeSpectator(arena, player);
		else
			return;
		Olympiad.addSpectator(id, player, false);
	}
	
	public static void resetNobleStats(int objId)
	{
		StatsSet nobleStats = getNobleStats(objId);
		if (nobleStats != null)
		{
			nobleStats.set(COMP_DONE, 0);
			nobleStats.set(COMP_WON, 0);
			nobleStats.set(COMP_LOST, 0);
			nobleStats.set(COMP_DRAWN, 0);
			nobleStats.set(POINTS, 4);
			updateNobleStats(objId, nobleStats);
		}
	}
	
	public static void resetNobleStats(int objId, int points)
	{
		StatsSet nobleStats = getNobleStats(objId);
		if (nobleStats != null)
		{
			nobleStats.set(COMP_DONE, 0);
			nobleStats.set(COMP_WON, 0);
			nobleStats.set(COMP_LOST, 0);
			nobleStats.set(COMP_DRAWN, 0);
			nobleStats.set(POINTS, points);
			updateNobleStats(objId, nobleStats);
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Olympiad _instance = new Olympiad();
	}
}
