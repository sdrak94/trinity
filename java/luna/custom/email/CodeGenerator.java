package luna.custom.email;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class CodeGenerator
{
	
	public static CodeGenerator getInstance()
	{
		return SingletonHolder._instance;
	}
	private static class SingletonHolder
	{
		protected static final CodeGenerator _instance = new CodeGenerator();
	}

	
	public static String code = "";
	public static String donateId = "";
	public static String code_donation = "";
	
	public final String getRandomString1()
	{
		return code;
	}
	public final String getRandomString2()
	{
		return code_donation;
	}
	/**
	 * 
	 * @param candidateChars
	 *            the candidate chars
	 * @param length
	 *            the number of random chars to be generated
	 * 
	 * @return
	 */

	
	static String getCode(int n, boolean donation) 
    { 
  
        // chose a Character random from this String 
        String code1 = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"; 
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
  
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
            int index 
                = (int)(code1.length() 
                        * Math.random()); 
  
            // add Character one by one in end of sb 
            sb.append(code1 
                          .charAt(index)); 
        } 
        if (donation)
        {
        	code_donation = sb.toString();
        }
        else
            code = sb.toString();
        return sb.toString(); 
    } 
	public void start(L2PcInstance activeChar)
	{
		getCode(5,false);
		activeChar.setCode(code);
	}
	public void startDonate()
	{
		getCode(17,true);
		donateId = code_donation;
	}
	public String startDonateRefund()
	{
		String code;
		code = getCode(17,true);
		return code;
	}
}