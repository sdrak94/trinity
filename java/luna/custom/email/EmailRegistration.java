package luna.custom.email;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class EmailRegistration
{
	public static final Logger	_log		= Logger.getLogger(EmailRegistration.class.getName());
	private static final char[]	VALID_CHARS	=
	{
		'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'z', 'x', 'c', 'v', 'b', 'n', 'm', '@', '-', '_', '`', '`', '.', ' ', '!', '?', '(', ')', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'
	};
	
	public static EmailRegistration getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EmailRegistration _instance = new EmailRegistration();
	}
	
	public static void onBypass(L2PcInstance activeChar, String command)
	{
		String action = command.substring(0);
		if (action.startsWith("_register"))
		{
			String choiceEmail = action.substring(10);
			if (!choiceEmail.contains("@") || !choiceEmail.contains(".") || choiceEmail.endsWith("."))
			{
				activeChar.sendMessage("Email is Invalid!");
				showWindow(activeChar, 1);
				return;
			}
			activeChar.setEmailTemp(choiceEmail);
			showWindow(activeChar, 2);
			storeCode(activeChar);
			sendEmail(activeChar.getEmailTemp(), "Hello, " + activeChar.getName() + "<br> There's your one time code in order to verify your email in order to proceed the registration<br>Code: " + activeChar.getCode() + "<br>Thank You!");
			return;
		}
		if (action.startsWith("_code_register"))
		{
			String code = action.substring(15);
			if (!code.equals(activeChar.getCode()))
			{
				activeChar.sendMessage("Code is Invalid!");
				activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 16));
				showWindow(activeChar, 2);
				return;
			}
			else if (code.equals(activeChar.getCode()))
			{
				activeChar.sendMessage("Code Valid!");
				saveEmail(activeChar, activeChar.getEmailTemp());
				bindIp(activeChar);
				showWindow(activeChar, 3);
				activeChar.setLockdownTime(0);
				activeChar.doUnLockdown();
				return;
			}
			return;
		}
		if (action.startsWith("_keep_only_once_ok")) // 18
		{
			String code = action.substring(19);
			if (!code.equals(activeChar.getCode()))
			{
				showWindow(activeChar, 4);
				return;
			}
			else if (code.equals(activeChar.getCode()))
			{
				activeChar.sendMessage("Code Valid!");
				activeChar.setLockdownTime(0);
				activeChar.doUnLockdown();
				activeChar.showClanNotice();
				return;
			}
			return;
		}
		if (action.startsWith("_save_new_hwid"))
		{
			String code = action.substring(15);
			if (!code.equals(activeChar.getCode()))
			{
				showWindow(activeChar, 4);
				return;
			}
			else if (code.equals(activeChar.getCode()))
			{
				activeChar.sendMessage("Code Valid!");
				bindIp(activeChar);
				activeChar.sendMessage("You have successfully bound this HWID to your account!");
				activeChar.setLockdownTime(0);
				activeChar.doUnLockdown();
				activeChar.showClanNotice();
				
				return;
			}
			return;
		}
		if (action.startsWith("_keep_me_locked"))
		{
			activeChar.showClanNotice();
			return;
		}
	}
	
	private static void finalSendMail(final String username, final String password, String recipientEmail, String ccEmail, String title, String message) throws AddressException, MessagingException
	{
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
		// Get a Properties object
		Properties props = System.getProperties();
		props.setProperty("mail.smtp.host", "smtp.gmail.com");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", "465");
		props.setProperty("mail.smtp.socketFactory.port", "465");
		props.setProperty("mail.smtps.auth", "false");
		props.put("mail.smtps.quitwait", "false");
		Session session = Session.getInstance(props, null);
		final MimeMessage msg = new MimeMessage(session);
		try
		{
			msg.setFrom(new InternetAddress(Config.MAIL_USER));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));
			if (ccEmail.length() > 0)
			{
				msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
			}
			msg.setSubject(title);
			msg.setText(message, StandardCharsets.UTF_8.displayName(), "html");
			msg.setSentDate(new Date());
			SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
			t.connect("smtp.gmail.com", Config.MAIL_USER, Config.MAIL_PASSWORD);
			t.sendMessage(msg, msg.getAllRecipients());
			t.close();
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void sendEmail(final String email, final String message)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					finalSendMail(Config.MAIL_USER, Config.MAIL_PASSWORD, email, "", "L2Trinity Code!", message);
				}
				catch (MessagingException e)
				{
					_log.log(Level.SEVERE, "Error while sending Email, email:" + email + " message:" + message + " ", e);
				}
			}
		}, 0);
	}
	
	public static void showWindow(L2PcInstance activeChar, int Page)
	{
		if (Page == 1)
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/custom/Email/EmailReg.htm");
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/custom/Email/EmailReg.htm");
				html.replace("%name%", activeChar.getName());
				activeChar.sendPacket(html);
			}
		}
		else if (Page == 2)
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/custom/Email/EmailRegCodeVerify.htm");
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/custom/Email/EmailRegCodeVerify.htm");
				html.replace("%name%", activeChar.getName());
				html.replace("%mail%", activeChar.getEmailTemp());
				activeChar.sendPacket(html);
			}
		}
		else if (Page == 3)
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/custom/Email/EmailRegOk.htm");
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/custom/Email/EmailRegOk.htm");
				html.replace("%name%", activeChar.getName());
				html.replace("%mail%", activeChar.getEmailTemp());
				html.replace("%account%", activeChar.getAccountName());
				activeChar.sendPacket(html);
			}
		}
		else if (Page == 4)
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/custom/Email/NewHWID.htm");
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/custom/Email/NewHWID.htm");
				html.replace("%name%", activeChar.getName());
				html.replace("%mail%", "Not disabled on foreign HWIDs for your own protection.");
				html.replace("%hwid%", activeChar.getHWID());
				html.replace("%account%", activeChar.getAccountName());
				storeCode(activeChar);
				sendEmail(activeChar.getEmail(), "Hello, " + activeChar.getName() + "<br> You have logged in from a new HWID in order to protect your account you need to fill the code bellow ingame.<br>Code: " + activeChar.getCode() + "<br>Thank You!");
				activeChar.sendPacket(html);
				activeChar.doLockdown(504);
			}
		}
	}
	
	public static void storeCode(L2PcInstance activeChar)
	{
		CodeGenerator.getInstance().start(activeChar);
		String code = CodeGenerator.getInstance().getRandomString1();
		activeChar.setCode(code);
	}
	
	protected static boolean checkInvalidChars(String s, boolean sizeCheck)
	{
		if (sizeCheck && (s.length() < 3 || s.length() > 45))
		{
			return false;
		}
		char[] chars = s.toLowerCase().toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			boolean contains = false;
			for (char c : VALID_CHARS)
			{
				if (chars[i] == c)
				{
					contains = true;
					break;
				}
			}
			if (!contains)
			{
				return false;
			}
		}
		return true;
	}
	
	public static void saveEmail(L2PcInstance activeChar, String choiceEmail)
	{
		Connection con = null;
		// activeChar.getAccountName();
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET email=? WHERE login=?");
			statement.setString(1, choiceEmail);
			statement.setString(2, activeChar.getAccountName());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed saving email", e);
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
	
	public static void bindIp(L2PcInstance activeChar)
	{
		L2GameClient client = activeChar.getClient();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO account_security (account,ip,hwid) VALUES (?,?,?)");
			statement.setString(1, activeChar.getAccountName());
			statement.setString(2, activeChar.getIP());
			statement.setString(3, client.getFullHwid());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed storing ip", e);
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
	
	public static boolean checkBinds(L2PcInstance activeChar)
	{
		ArrayList<String> resultList = new ArrayList<String>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT hwid FROM account_security WHERE account=?");
			statement.setString(1, activeChar.getAccountName());
			statement.execute();
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				String hwid = rset.getString(1);
				resultList.add(hwid);
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed storing hwid", e);
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
		String hwid = activeChar.getHWID();
		if (resultList.contains(hwid))
		{
			return true;
		}
		else
			return false;
	}
}