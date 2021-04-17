package luna.custom.captcha.instancemanager;

import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import luna.custom.antibot.DDSConverter;
import luna.custom.captcha.Captcha;
import luna.custom.captcha.ImageData;
import luna.custom.captcha.RandomString;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeCrest;
import net.sf.l2j.gameserver.util.StringUtil;

public class BotsPreventionManager
{
	private static Logger _log = Logger.getLogger(BotsPreventionManager.class.getName());
	public class PlayerData
	{
		public PlayerData()
		{
			firstWindow = true;
		}
		
		public boolean			firstWindow;
		public BufferedImage	image;
		public String			captchaText	= "";
		public int				captchaID	= 0;
	}
	
	protected Random							_randomize;
	protected static Map<Integer, ImageData>	_imageMap;
	protected static Map<Integer, Integer>		_monsterscounter;
	protected static Map<Integer, Future<?>>	_beginvalidation;
	protected static Map<Integer, PlayerData>	_validation;
	
	public static Map<Integer, PlayerData> get_validation()
	{
		return _validation;
	}
	
	public static void set_validation(Map<Integer, PlayerData> _validation)
	{
		BotsPreventionManager._validation = _validation;
	}
	
	protected int					WINDOW_DELAY	= 3;											// delay used to generate new window if previous have been closed.
	protected int					VALIDATION_TIME	= Config.VALIDATION_TIME * 1000;
	private int						USEDID			= 0;
	static String					AntibotCommand1	= RandomString.getInstance().getRandomString1();
	private static final String[]	COMMANDS		=
	{
		AntibotCommand1 + "_continue"
	};
	
	public static final BotsPreventionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	BotsPreventionManager()
	{
		_randomize = new Random();
		_monsterscounter = new HashMap<>();
		_beginvalidation = new HashMap<>();
		_validation = new HashMap<>();
		_beginvalidation = new HashMap<>();
		_imageMap = new ConcurrentHashMap<>();
		_imageMap = Captcha.getInstance().createImageList();
	}

	
	public void prevalidationwindow(L2PcInstance player)
	{
		String AntibotCommand1 = RandomString.getInstance().getRandomString1();
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		StringBuilder tb = new StringBuilder();
		StringUtil.append(tb, "<html>");
		StringUtil.append(tb, "<title>Bots prevention</title>");
		StringUtil.append(tb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		StringUtil.append(tb, "<br><br><font color=\"a2a0a2\">If such window appears it means server suspect,<br1>that you may using cheating software.</font>");
		StringUtil.append(tb, "<br><br><font color=\"b09979\">If given answer results are incorrect or no action is made<br1>server is going to punish character instantly.</font>");
		StringUtil.append(tb, "<br><br><font color=\"a2a0a2\"><font color=\"b09979\">If you accidentally close this dialog while you're validating<br1> you can call it again by typing </font> <font color=\"A55729\">.captcha or .c </font>");
		StringUtil.append(tb, "<br><br><button value=\"CONTINUE\" action=\"bypass " + AntibotCommand1 + "_continue\" width=\"75\" height=\"21\" back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\">");
		StringUtil.append(tb, "</center></body>");
		StringUtil.append(tb, "</html>");
		html.setHtml(tb.toString());
		player.sendPacket(html);
	}
	
	public void validationwindow(L2PcInstance player)
	{
		String _AntibotCommand2 = RandomString.getInstance().getRandomString2();
		PlayerData container = _validation.get(player.getObjectId());
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		StringBuilder tb = new StringBuilder();
		StringUtil.append(tb, "<html>");
		StringUtil.append(tb, "<title>Bots prevention</title>");
		StringUtil.append(tb, "<body><center>");
		StringUtil.append(tb, "<br><br><font color=\"a2a0a2\">in order to prove you are a human being<br1>you've to</font> <font color=\"b09979\">enter the code from picture:</font>");
		StringUtil.append(tb, "<br><br><font color=\"a2a0a2\"><font color=\"b09979\">If you accidentally close this dialog while you're validating<br1> you can call it again by typing </font> <font color=\"A55729\">.captcha or .c </font>");
		// generated main pattern.
		StringUtil.append(tb, "<br><br><br><br><img src=\"Crest.crest_" + Config.SERVER_ID + "_" + (container.captchaID) + "\" width=256 height=64></td></tr>");
		StringUtil.append(tb, "<br>");
		StringUtil.append(tb, "<br><br><edit var=\"answer\" width=220>");
		StringUtil.append(tb, "<br><br><font color=\"b09979\">You have " + String.valueOf(player.getTries()) + " " + (player.getTries() == 1 ? "try" : "tries") + " available.</font>");
		StringUtil.append(tb, "<br><button value=\"Confirm\" action=\"bypass -h " + _AntibotCommand2 + "_ $answer\" width=\"85\" height=\"25\" back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"><br/>");
		StringUtil.append(tb, "</center><br><br><br><br><br><br><br><br></body>");
		StringUtil.append(tb, "</html>");
		html.setHtml(tb.toString());
		_validation.get(player.getObjectId()).firstWindow = false;
		player.sendPacket(html);
	}
	
	public void punishmentnwindow(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		StringBuilder tb = new StringBuilder();
		StringUtil.append(tb, "<html>");
		StringUtil.append(tb, "<title>Bots prevention</title>");
		StringUtil.append(tb, "<body><center><br><br><img src=\"L2UI_CH3.herotower_deco\" width=\"256\" height=\"32\">");
		StringUtil.append(tb, "<br><br><font color=\"a2a0a2\">If such window appears, it means character haven't<br1>passed through prevention system.");
		StringUtil.append(tb, "<br><br><font color=\"b09979\">In such case character get moved to nearest town.</font>");
		StringUtil.append(tb, "</center></body>");
		StringUtil.append(tb, "</html>");
		html.setHtml(tb.toString());
		player.sendPacket(html);
	}
	
	public void validationtasks(L2PcInstance player)
	{
		PlayerData container = new PlayerData();
		Captcha.getInstance().generateCaptcha(container, player);
		container.image = _imageMap.get(getInstance().USEDID).image;
		container.captchaID = _imageMap.get(getInstance().USEDID).captchaID;
		container.captchaText = _imageMap.get(getInstance().USEDID).captchaText;
		PledgeCrest packet = new PledgeCrest(container.captchaID, DDSConverter.convertToDDS(container.image).array());
		player.sendPacket(packet);
		getInstance().USEDID++;
		if (getInstance().USEDID == 998)
		{
			getInstance().USEDID = 0;
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				_imageMap = Captcha.getInstance().createImageList();
			}, 100);
		}
		_validation.put(player.getObjectId(), container);
		Future<?> newTask = ThreadPoolManager.getInstance().scheduleGeneral(new ReportCheckTask(player), VALIDATION_TIME);
		ThreadPoolManager.getInstance().scheduleGeneral(new countdown(player, VALIDATION_TIME / 1000), 0);
		player.setIsCaptchaValidating(true);
		_beginvalidation.put(player.getObjectId(), newTask);
	}
	
	public void banpunishment(L2PcInstance player)
	{
		_validation.remove(player.getObjectId());
		_beginvalidation.get(player.getObjectId()).cancel(true);
		_beginvalidation.remove(player.getObjectId());
		checkDb(player);
		int reportsInTotal = getReportCount(player);
		int bonusTime = 0;
		
		if (reportsInTotal > Config.PUNISHMENT_REPORTS1)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_1;
		if (reportsInTotal > Config.PUNISHMENT_REPORTS2)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_2;
		if (reportsInTotal > Config.PUNISHMENT_REPORTS3)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_3;
		if (reportsInTotal > Config.PUNISHMENT_REPORTS4)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_4;
		if (reportsInTotal > Config.PUNISHMENT_REPORTS5)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_5;
		if (reportsInTotal > Config.PUNISHMENT_REPORTS6)
			bonusTime = Config.PUNISHMENT_TIME_BONUS_6;

		int punishmenttime = (Config.PUNISHMENT_TIME + bonusTime);
		
		if (reportsInTotal <= Config.ESCAPE_PUNISHMENT_REPORTS_COUNT)
		{
			player.stopMove(null);
			player.teleToLocation(-186003, 242524, 1678);
			punishmentnwindow(player);
		}
		else if (reportsInTotal > Config.ESCAPE_PUNISHMENT_REPORTS_COUNT && reportsInTotal <= Config.KICK_PUNISHMENT_REPORTS_COUNT)
		{
			if (player.isOnline() == 1)
			{
				player.logout();
			}
		}
		else if (reportsInTotal > Config.KICK_PUNISHMENT_REPORTS_COUNT)
		{
			player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, punishmenttime);
		}
		player.setIsCaptchaValidating(false);
		player.setTries(5);
		player.sendMessage("Unfortunately, code doesn't match.");
	}
	
	public void checkDb(L2PcInstance bot)
	{
		int botReportCount = getReportCount(bot);
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT * FROM bot_report WHERE botId = ?");
				statement.setInt(1, bot.getObjectId());
				
				ResultSet rset = statement.executeQuery();
				
				if(!rset.next())
				{
					
					statement = con.prepareStatement("INSERT INTO bot_report(botId,botName,totalReportCount) VALUES (?, ?, ?)");
					statement.setInt(1, bot.getObjectId());
					statement.setString(2, bot.getName());
					statement.setInt(3, 1);
					statement.execute();

					rset.close();
					statement.close();
				}
				else
				{
					PreparedStatement statement2 = con.prepareStatement("UPDATE bot_report set totalReportCount=? where botId=?");
					statement2.setInt(1, botReportCount +1);
					statement2.setInt(2, bot.getObjectId());
					statement2.execute();
					statement2.close();
				}
				rset.close();
				statement.close();
		}
		}
		catch (SQLException e)
		{
			_log.warning("Error on parsing bot report.");
			if (Config.DEBUG)
				e.printStackTrace();
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
	
	private int getReportCount(L2PcInstance player)
	{
		int count = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT totalReportCount FROM bot_report where botId=?");
			statement.setInt(1, player.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				count = rset.getInt(1);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException se)
		{
			if (Config.DEBUG)
				se.printStackTrace();
		}
		return count;
	}

	public void AnalyseBypass(String command, L2PcInstance player)
	{
		if (!_validation.containsKey(player.getObjectId()))
			return;
		String params = command.substring(command.indexOf("_") + 1);
		if (params.startsWith("continue"))
		{
			validationwindow(player);
			_validation.get(player.getObjectId()).firstWindow = false;
			return;
		}
		PlayerData playerData = _validation.get(player.getObjectId());
		if (!params.trim().equalsIgnoreCase(playerData.captchaText))
		{
			boolean decreaseTries = true;
			player.setTries(player.getTries() - 1);
			if (decreaseTries)
			{
				player.sendMessage("Wrong captcha code or bot answer, try again!");
				player.sendMessage("You have " + String.valueOf(player.getTries()) + " " + (player.getTries() == 1 ? "try" : "tries") + " available.");
			}
			if (player.getTries() > 0)
			{
				validationwindow(player);
				_validation.get(player.getObjectId()).firstWindow = false;
				return;
			}
			if ((player.getTries() == 0))
			{
				banpunishment(player);
			}
		}
		if (params.trim().equalsIgnoreCase(playerData.captchaText))
		{
			player.sendMessage("Congratulations, code match!");
			player.sendMessage("You are under captcha protection for 15 minutes!");
			player.setTries(5);
			player.setIsCaptchaValidating(false);
			CaptchaSuccessfull(player);
			_validation.remove(player.getObjectId());
			_beginvalidation.get(player.getObjectId()).cancel(true);
			_beginvalidation.remove(player.getObjectId());
		}
	}
	
	protected class countdown implements Runnable
	{
		private final L2PcInstance	_player;
		private int					_time;
		
		public countdown(L2PcInstance player, int time)
		{
			_time = time;
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player.isOnline() == 1)
			{
				if (_validation.containsKey(_player.getObjectId()) && _validation.get(_player.getObjectId()).firstWindow)
				{
					prevalidationwindow(_player);
					switch (_time)
					{
						case 300:
						case 240:
						case 180:
						case 120:
						case 60:
							_player.sendMessage(_time / 60 + " minute(s) to enter the code.");
							_player.sendPacket(new ExShowScreenMessage(_time / 60 + " minute(s) to enter the code.", 6000));
							break;
						case 30:
						case 10:
						case 5:
						case 4:
						case 3:
						case 2:
						case 1:
							_player.sendMessage(_time + " second(s) to enter the code!");
							_player.sendPacket(new ExShowScreenMessage(_time + " second(s) to enter the code!", 1000));
							break;
					}
					if (_time > 1 && _validation.containsKey(_player.getObjectId()))
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new countdown(_player, _time - 1), 1000);
					}
				}
			}
		}
	}
	
	protected boolean tryParseInt(String value)
	{
		try
		{
			Integer.parseInt(value);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	public void CaptchaSuccessfull(L2PcInstance player)
	{
		if (_validation.get(player.getObjectId()) != null)
		{
			_validation.remove(player.getObjectId());
			player.setLastCaptchaTimeStamp(GameTimeController.getGameTicks());
		}
	}
	
	public Boolean IsAlredyInReportMode(L2PcInstance player)
	{
		if (_validation.get(player.getObjectId()) != null)
		{
			return true;
		}
		return false;
	}
	
	private class ReportCheckTask implements Runnable
	{
		private final L2PcInstance _player;
		
		public ReportCheckTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_validation.get(_player.getObjectId()) != null)
			{
				banpunishment(_player);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final BotsPreventionManager _instance = new BotsPreventionManager();
	}
}
