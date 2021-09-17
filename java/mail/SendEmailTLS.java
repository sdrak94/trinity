package mail;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;



public class SendEmailTLS
{
	public static void main(String[] args)
	{
		final String username = "lineage2trinityserver@gmail.com";
		final String password = "WyKuQWhuDFgCipK";
		
//		 Get a Properties object
		 Properties props = System.getProperties();
		 props.setProperty("mail.smtp.host", "smtp.gmail.com");
		 props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		 props.setProperty("mail.smtp.socketFactory.fallback", "false");
		 props.setProperty("mail.smtp.port", "465");
		 props.setProperty("mail.smtp.socketFactory.port", "465");
		 props.setProperty("mail.smtps.auth", "false");
		 props.put("mail.smtps.quitwait", "false");
		 //Session session = Session.getInstance(props, null);
			Session session = Session.getInstance(props, null);
			// -- Create a new message --
			final MimeMessage msg = new MimeMessage(session);
			// -- Set the FROM and TO fields --
			try
			{
				msg.setFrom(new InternetAddress(username));
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("napsterakos321@gmail.com", false));
				msg.setSubject("test");
				msg.setText("aaa", StandardCharsets.UTF_8.displayName(), "html");
				msg.setSentDate(new Date());
				SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
				t.connect("smtp.gmail.com", username, password);
				t.sendMessage(msg, msg.getAllRecipients());
				t.close();
			}
			catch (MessagingException e)
			{
				e.printStackTrace();
			}
	}
}