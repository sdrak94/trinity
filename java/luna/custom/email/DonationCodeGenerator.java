package luna.custom.email;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class DonationCodeGenerator
{
	public static final Logger _log = Logger.getLogger(DonationCodeGenerator.class.getName());
	private static final char[] VALID_CHARS = {'q','w','e','r','t','y','u','i','o','p','a','s','d','f','g','h','j','k','l','z','x','c','v','b','n','m','@','-','_','`','`','.',' ','!','?','(',')','1','2','3','4','5','6','7','8','9','0'};

	
	public static DonationCodeGenerator getInstance()
	{
		return SingletonHolder._instance;
	}
	private static class SingletonHolder
	{
		protected static final DonationCodeGenerator _instance = new DonationCodeGenerator();
	}
	public static void onBypass(L2PcInstance activeChar, String command)
	{
		
		String action = command.substring(0);
		
		if(action.startsWith("_register"))
		{
			String choiceEmail = action.substring(10);
			if (!choiceEmail.contains("@") || !choiceEmail.contains(".") || choiceEmail.endsWith("."))
			{
				activeChar.sendMessage("Email is Invalid!");
				//showWindow(activeChar, 1);
				return;
			}
			activeChar.setEmailTemp(choiceEmail);
			//showWindow(activeChar, 2);
			
			sendEmail(activeChar.getEmailTemp(), "Hello, "+activeChar.getName()+"<br> There's your one time code in order to verify your email in order to proceed the registration<br>Code: "+activeChar.getCode()+"<br>Thank You!");
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
        props.put("mail.smtp.auth", "true");    
        props.put("mail.smtp.port", "465");    

		Session session = Session.getInstance(props, null);

		// -- Create a new message --
		final MimeMessage msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --
		msg.setFrom(new InternetAddress(username));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));

		if (ccEmail.length() > 0)
		{
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
		}

		msg.setSubject(title);
		msg.setText(message, StandardCharsets.UTF_8.displayName(), "html");
		msg.setSentDate(new Date());

		SMTPTransport t = (SMTPTransport)session.getTransport("smtp");
		t.connect("smtp.gmail.com", username, password);
		t.sendMessage(msg, msg.getAllRecipients());      
		t.close();
	}
	public static void sendEmail(final String email, final String message)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					finalSendMail(Config.DONATE_MAIL_USER, Config.DONATE_MAIL_PASSWORD, email, "", "L2Trinity Code!", message);
				}
				catch (MessagingException e)
				{

					_log.log(Level.SEVERE,"Error while sending Email, email:"+email+" message:"+message+" ", e);
				}
			}
		}, 0);
	}
	//donate_send email ammount
	public static void storeCode(String email, int ammount)
	{
		CodeGenerator.getInstance().startDonate();
		String donateId = CodeGenerator.getInstance().getRandomString2();
		saveTxnId(donateId, ammount, email);
		sendEmail(email, "Thanks for supporting L2Trinity<br>This is your donation transaction code:#"+ donateId +" for: "+ammount+" euro <br> If there's a bonus on the amount you donated it will be displayed in game.");
		
	}

	public static void saveTxnId(String donateId, int payment_amount, String email)
	{
		int bonus_amount = 0;
		if (payment_amount > 24 && payment_amount < 50)
		{
			bonus_amount = (int) (payment_amount * 1.1) - payment_amount;
		}
		else if (payment_amount > 49 && payment_amount < 100)
		{
			bonus_amount = (int) (payment_amount * 1.2) - payment_amount;
		}
		else if (payment_amount > 99 && payment_amount < 150)
		{
			bonus_amount = (int) (payment_amount * 1.25) - payment_amount;
		}
		else if (payment_amount > 149 && payment_amount < 250)
		{
			bonus_amount = (int) (payment_amount * 1.3) - payment_amount;
		}
		else if (payment_amount > 249 && payment_amount < 350)
		{
			bonus_amount = (int) (payment_amount * 1.35) - payment_amount;
		}
		int received_amount, id = 0;
		received_amount = payment_amount + bonus_amount;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("select count(*) from donations");
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					id = rset.getInt(1);
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed saving id", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("INSERT INTO donations (id, txn_id, payment_amount, bonus_amount, received_amount, payment_status, retrieved, retriever_ip, retriever_acct, retriever_char, retrieval_date, email, hwid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, id++);
			statement.setString(2, donateId);
			statement.setInt(3, payment_amount);
			statement.setInt(4, bonus_amount);
			statement.setInt(5, received_amount);
			statement.setString(6, "Completed");
			statement.setString(7, "");
			statement.setString(8, "");
			statement.setString(9, "");
			statement.setString(10, "");
			statement.setString(11, "");
			statement.setString(12, email);
			statement.setString(13, "");
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed saving email", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		
	}
}