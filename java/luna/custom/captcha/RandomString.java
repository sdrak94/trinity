package luna.custom.captcha;

import java.util.Random;

import net.sf.l2j.gameserver.ThreadPoolManager;

public class RandomString
{
	public static RandomString getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RandomString _instance = new RandomString();
	}
	
	public static String	randomString1	= "";
	public static String	randomString2	= "";
	public static int		randomInt;
	
	public final String getRandomString1()
	{
		return randomString1;
	}
	
	public final String getRandomString2()
	{
		return randomString2;
	}
	
	private static int getRandomNumberInRange()
	{
		int min = 10;
		int max = 25;
		Random randomNum = new Random();
		int showMe = min + randomNum.nextInt(max);
		randomInt = showMe;
		return showMe;
	}
	
	static String getAlphaNumericString1(int n)
	{
		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++)
		{
			int index = (int) (AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}
		randomString1 = sb.toString();
		return sb.toString();
	}
	
	static String getAlphaNumericString2(int n)
	{
		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++)
		{
			int index = (int) (AlphaNumericString.length() * Math.random());
			sb.append(AlphaNumericString.charAt(index));
		}
		randomString2 = sb.toString();
		return sb.toString();
	}
	
	public static void start()
	{
		System.out.println("Starting Generating...");
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				getRandomNumberInRange();
				getAlphaNumericString1(randomInt);
				getAlphaNumericString2(randomInt);
			}
		}, 0, 3600000);
	}
}