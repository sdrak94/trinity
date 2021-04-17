package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author BiggBoss
 */
public final class BotReportTable
{
	private static final Logger _log = LoggerFactory.getLogger(BotReportTable.class);
	
	private static final int COLUMN_BOT_ID = 1;
	private static final int COLUMN_REPORTER_ID = 2;
	private static final int COLUMN_REPORT_TIME = 3;
	
	public static final int ATTACK_ACTION_BLOCK_ID = -1;
	public static final int TRADE_ACTION_BLOCK_ID = -2;
	public static final int PARTY_ACTION_BLOCK_ID = -3;
	public static final int ACTION_BLOCK_ID = -4;
	public static final int CHAT_BLOCK_ID = -5;
	
	private static final String SQL_LOAD_REPORTED_CHAR_DATA = "SELECT * FROM bot_reported_char_data";
	private static final String SQL_INSERT_REPORTED_CHAR_DATA = "INSERT INTO bot_reported_char_data VALUES (?,?,?)";
	private static final String SQL_CLEAR_REPORTED_CHAR_DATA = "DELETE FROM bot_reported_char_data";
	
	private Map<Integer, Long> _ipRegistry;
	private Map<Integer, ReporterCharData> _charRegistry;
	private Map<Integer, ReportedCharData> _reports;
	private Map<Integer, PunishHolder> _punishments;
	
	BotReportTable()
	{
		if (Config.ACCEPT_GEOEDITOR_CONN)
		{
			_ipRegistry = new HashMap<>();
			_charRegistry = new ConcurrentHashMap<>();
			_reports = new ConcurrentHashMap<>();
			_punishments = new ConcurrentHashMap<>();
			
			try
			{
				File punishments = new File("./config/botreport_punishments.xml");
				if (!punishments.exists())
				{
					throw new FileNotFoundException(punishments.getName());
				}
				
				SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(punishments, new PunishmentsLoader());
			}
			catch (Exception e)
			{
				_log.warn("BotReportTable: Could not load punishments from /config/botreport_punishments.xml", e);
			}
			
			loadReportedCharData();
			scheduleResetPointTask();
		}
	}
	
	/**
	 * Loads all reports of each reported bot into this cache class.<br>
	 * Warning: Heavy method, used only on server start up
	 */
	private void loadReportedCharData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery(SQL_LOAD_REPORTED_CHAR_DATA))
		{
			long lastResetTime = 0;
			try
			{
				String[] hour = Config.TVT_EVENT_INTERVAL;
				Calendar c = Calendar.getInstance();
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour[0]));
				c.set(Calendar.MINUTE, Integer.parseInt(hour[1]));
				
				if (System.currentTimeMillis() < c.getTimeInMillis())
				{
					c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 1);
				}
				
				lastResetTime = c.getTimeInMillis();
			}
			catch (Exception e)
			{
			
			}
			
			while (rset.next())
			{
				int botId = rset.getInt(COLUMN_BOT_ID);
				int reporter = rset.getInt(COLUMN_REPORTER_ID);
				long date = rset.getLong(COLUMN_REPORT_TIME);
				if (_reports.containsKey(botId))
				{
					_reports.get(botId).addReporter(reporter, date);
				}
				else
				{
					ReportedCharData rcd = new ReportedCharData();
					rcd.addReporter(reporter, date);
					_reports.put(rset.getInt(COLUMN_BOT_ID), rcd);
				}
				
				if (date > lastResetTime)
				{
					ReporterCharData rcd = _charRegistry.get(reporter);
					if (rcd != null)
					{
						rcd.setPoints(rcd.getPointsLeft() - 1);
					}
					else
					{
						rcd = new ReporterCharData();
						rcd.setPoints(6);
						_charRegistry.put(reporter, rcd);
					}
				}
			}
			
			_log.info("BotReportTable: Loaded " + _reports.size() + " bot reports");
		}
		catch (Exception e)
		{
			_log.warn("BotReportTable: Could not load reported char data!", e);
		}
	}
	
	/**
	 * Save all reports for each reported bot down to database.<br>
	 * Warning: Heavy method, used only at server shutdown
	 */
	public void saveReportedCharData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement st = con.createStatement();
			PreparedStatement ps = con.prepareStatement(SQL_INSERT_REPORTED_CHAR_DATA))
		{
			st.execute(SQL_CLEAR_REPORTED_CHAR_DATA);
			
			for (Map.Entry<Integer, ReportedCharData> entrySet : _reports.entrySet())
			{
				Map<Integer, Long> reportTable = entrySet.getValue()._reporters;
				for (int reporterId : reportTable.keySet())
				{
					ps.setInt(1, entrySet.getKey());
					ps.setInt(2, reporterId);
					ps.setLong(3, reportTable.get(reporterId));
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.error("BotReportTable: Could not update reported char data in database!", e);
		}
	}
	
	/**
	 * Attempts to perform a bot report. R/W to ip and char id registry is synchronized. Triggers bot punish management<br>
	 * @param reporter (L2PcInstance who issued the report)
	 * @return True, if the report was registered, False otherwise
	 */
	public boolean reportBot(L2PcInstance reporter)
	{
		L2Object target = reporter.getTarget();
		L2PcInstance bot = null;
		if ((target == null) || ((bot = target.getActingPlayer()) == null) || (target.getObjectId() == reporter.getObjectId()))
		{
			return false;
		}
		
		if (bot.isInsidePeaceZone(bot) || bot.checkIfPvP(bot))
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_CHARACTER_IN_PEACE_OR_BATTLE_ZONE);
			return false;
		}
		
		if (bot.isInOlympiadMode())
		{
			reporter.sendPacket(SystemMessageId.TARGET_NOT_REPORT_CANNOT_REPORT_PEACE_PVP_ZONE_OR_OLYMPIAD_OR_CLAN_WAR_ENEMY);
			return false;
		}
		
		if ((bot.getClan() != null) && bot.getClan().isAtWarWith(reporter.getClan().getClanId()))
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_CLAN_WAR_ENEMY);
			return false;
		}
		
		/*if (bot.getExp() == bot.getStat().getStartingExp())
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_CHAR_WHO_ACQUIRED_XP);
			return false;
		}*/
		
		ReportedCharData rcd = _reports.get(bot.getObjectId());
		ReporterCharData rcdRep = _charRegistry.get(reporter.getObjectId());
		final int reporterId = reporter.getObjectId();
		
		synchronized (this)
		{
			if (_reports.containsKey(reporterId))
			{
				reporter.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AND_CANNOT_REPORT);
				return false;
			}
			
			final int ip = hashIp(reporter);
			if (!timeHasPassed(_ipRegistry, ip))
			{
				reporter.sendPacket(SystemMessageId.CANNOT_REPORT_TARGET_ALREDY_REPORTED_BY_CLAN_ALLY_MEMBER_OR_SAME_IP);
				return true;
			}
			
			if (rcd != null)
			{
				if (rcd.alredyReportedBy(reporterId))
				{
					reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_CHAR_AT_THIS_TIME_1);
					return true;
				}
				
				if (!Config.ACCEPT_GEOEDITOR_CONN && rcd.reportedBySameClan(reporter.getClan()))
				{
					reporter.sendPacket(SystemMessageId.CANNOT_REPORT_TARGET_ALREDY_REPORTED_BY_CLAN_ALLY_MEMBER_OR_SAME_IP);
					return true;
				}
			}
			
			if (rcdRep != null)
			{
				if (rcdRep.getPointsLeft() == 0)
				{
					reporter.sendPacket(SystemMessageId.YOU_HAVE_USED_ALL_POINTS_POINTS_ARE_RESET_AT_NOON);
					return false;
				}
				
				long reuse = (System.currentTimeMillis() - rcdRep.getLastReporTime());
				if (reuse < Config.ALT_CLAN_JOIN_DAYS)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CAN_REPORT_IN_S1_MINS_YOU_HAVE_S2_POINTS_LEFT);
					sm.addNumber((int) (reuse / 60000));
					sm.addNumber(rcdRep.getPointsLeft());
					reporter.sendPacket(sm);
					return false;
				}
			}
			
			final long curTime = System.currentTimeMillis();
			
			if (rcd == null)
			{
				rcd = new ReportedCharData();
				_reports.put(bot.getObjectId(), rcd);
			}
			rcd.addReporter(reporterId, curTime);
			
			if (rcdRep == null)
			{
				rcdRep = new ReporterCharData();
			}
			rcdRep.registerReport(curTime);
			
			_ipRegistry.put(ip, curTime);
			_charRegistry.put(reporterId, rcdRep);
		}
	
		
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_REPORTED_AS_BOT);
		sm.addCharName(bot);
		reporter.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_USED_REPORT_POINT_ON_C1_YOU_HAVE_C2_POINTS_LEFT);
		sm.addCharName(bot);
		sm.addInt(rcdRep.getPointsLeft());
		reporter.sendPacket(sm);
		
		handleReport(bot, rcd);
		
		return true;
	}
	
	/**
	 * Find the punishs to apply to the given bot and triggers the punish method.
	 * @param bot (L2PcInstance to be punished)
	 * @param rcd (RepotedCharData linked to this bot)
	 */
	private void handleReport(L2PcInstance bot, final ReportedCharData rcd)
	{
		// Report count punishment
		punishBot(bot, _punishments.get(rcd.getReportCount()));
		
		// Range punishments
		for (int key : _punishments.keySet())
		{
			if ((key < 0) && (Math.abs(key) <= rcd.getReportCount()))
			{
				punishBot(bot, _punishments.get(key));
			}
		}
	}
	
	/**
	 * Applies the given punish to the bot if the action is secure
	 * @param bot (L2PcInstance to punish)
	 * @param ph (PunishHolder containing the debuff and a possible system message to send)
	 */
	private void punishBot(L2PcInstance bot, PunishHolder ph)
	{
		if (ph != null)
		{
			ph._punish.getEffects(bot, bot);
			if (ph._systemMessageId > -1)
			{
				SystemMessageId id = SystemMessageId.getSystemMessageId(ph._systemMessageId);
				if (id != null)
				{
					bot.sendPacket(id);
				}
			}
		}
	}
	
	/**
	 * Adds a debuff punishment into the punishments record. If skill does not exist, will log it and return
	 * @param neededReports (report count to trigger this debuff)
	 * @param skillId
	 * @param skillLevel
	 * @param sysMsg (id of a system message to send when applying the punish)
	 */
	void addPunishment(int neededReports, int skillId, int skillLevel, int sysMsg)
	{
		L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (sk != null)
		{
			_punishments.put(neededReports, new PunishHolder(sk, sysMsg));
		}
		else
		{
			_log.warn("BotReportTable: Could not add punishment for " + neededReports + " report(s): Skill " + skillId + "-" + skillLevel + " does not exist!");
		}
	}
	
	void resetPointsAndSchedule()
	{
		synchronized (_charRegistry)
		{
			for (ReporterCharData rcd : _charRegistry.values())
			{
				rcd.setPoints(7);
			}
		}
		
		scheduleResetPointTask();
	}
	
	private void scheduleResetPointTask()
	{
		try
		{
			String[] hour = Config.TVT_EVENT_INTERVAL;
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour[0]));
			c.set(Calendar.MINUTE, Integer.parseInt(hour[1]));
			
			if (System.currentTimeMillis() > c.getTimeInMillis())
			{
				c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) + 1);
			}
			
			ThreadPoolManager.getInstance().scheduleGeneral(new ResetPointTask(), c.getTimeInMillis() - System.currentTimeMillis());
		}
		catch (Exception e)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new ResetPointTask(), 24 * 3600 * 1000);
			_log.warn("BotReportTable: Could not properly schedule bot report points reset task. Scheduled in 24 hours.", e);
		}
	}
	
	public static BotReportTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Returns a integer representative number from a connection
	 * @param player (The L2PcInstance owner of the connection)
	 * @return int (hashed ip)
	 */
	private static int hashIp(L2PcInstance player)
	{
		String con = player.getClient().getConnection().getInetAddress().getHostAddress();
		String[] rawByte = con.split("\\.");
		int[] rawIp = new int[4];
		for (int i = 0; i < 4; i++)
		{
			rawIp[i] = Integer.parseInt(rawByte[i]);
		}
		
		return rawIp[0] | (rawIp[1] << 8) | (rawIp[2] << 16) | (rawIp[3] << 24);
	}
	
	/**
	 * Checks and return if the abstrat barrier specified by an integer (map key) has accomplished the waiting time
	 * @param map (a Map to study (Int = barrier, Long = fully qualified unix time)
	 * @param objectId (an existent map key)
	 * @return true if the time has passed.
	 */
	private static boolean timeHasPassed(Map<Integer, Long> map, int objectId)
	{
		if (map.containsKey(objectId))
		{
			return (System.currentTimeMillis() - map.get(objectId)) > PARTY_ACTION_BLOCK_ID;
		}
		return true;
	}
	
	/**
	 * Represents the info about a reporter
	 */
	private final class ReporterCharData
	{
		private long _lastReport;
		private byte _reportPoints;
		
		ReporterCharData()
		{
			_reportPoints = 7;
			_lastReport = 0;
		}
		
		void registerReport(long time)
		{
			_reportPoints -= 1;
			_lastReport = time;
		}
		
		long getLastReporTime()
		{
			return _lastReport;
		}
		
		byte getPointsLeft()
		{
			return _reportPoints;
		}
		
		void setPoints(int points)
		{
			_reportPoints = (byte) points;
		}
	}
	
	/**
	 * Represents the info about a reported character
	 */
	private final class ReportedCharData
	{
		Map<Integer, Long> _reporters;
		
		ReportedCharData()
		{
			_reporters = new HashMap<>();
		}
		
		int getReportCount()
		{
			return _reporters.size();
		}
		
		boolean alredyReportedBy(int objectId)
		{
			return _reporters.containsKey(objectId);
		}
		
		void addReporter(int objectId, long reportTime)
		{
			_reporters.put(objectId, reportTime);
		}
		
		boolean reportedBySameClan(L2Clan clan)
		{
			if (clan == null)
			{
				return false;
			}
			
			for (int reporterId : _reporters.keySet())
			{
				if (clan.isMember(reporterId))
				{
					return true;
				}
			}
			
			return false;
		}
	}
	
	/**
	 * SAX loader to parse /config/botreport_punishments.xml file
	 */
	private final class PunishmentsLoader extends DefaultHandler
	{
		PunishmentsLoader()
		{
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attr)
		{
			if (qName.equals("punishment"))
			{
				int reportCount = -1, skillId = -1, skillLevel = 1, sysMessage = -1;
				try
				{
					reportCount = Integer.parseInt(attr.getValue("neededReportCount"));
					skillId = Integer.parseInt(attr.getValue("skillId"));
					String level = attr.getValue("skillLevel");
					String systemMessageId = attr.getValue("sysMessageId");
					if (level != null)
					{
						skillLevel = Integer.parseInt(level);
					}
					
					if (systemMessageId != null)
					{
						sysMessage = Integer.parseInt(systemMessageId);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				addPunishment(reportCount, skillId, skillLevel, sysMessage);
			}
		}
	}
	
	class PunishHolder
	{
		final L2Skill _punish;
		final int _systemMessageId;
		
		PunishHolder(final L2Skill sk, final int sysMsg)
		{
			_punish = sk;
			_systemMessageId = sysMsg;
		}
	}
	
	class ResetPointTask implements Runnable
	{
		@Override
		public void run()
		{
			resetPointsAndSchedule();
			
		}
	}
	
	private static final class SingletonHolder
	{
		static final BotReportTable INSTANCE = new BotReportTable();
	}
}
