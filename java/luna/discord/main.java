package luna.discord;

import org.apache.log4j.PropertyConfigurator;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class main
{
	public static void main(String[] args) throws Exception
	{
		String log4jConfPath = "D:/DriveC/Tree_local/lib/log4j/log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);
		try
		{
			JDA api = new JDABuilder(AccountType.BOT).setToken("Njg5NzA0MzkwMzAyNjI5OTI5.XnHAJQ.pv_w99k2HBYibZe-6tJGMEvuNUg").build();
			api.addEventListener(new MyEventListener());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}