package luna.custom.antibot;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.Rnd;

/**
 * @author L2jSunrise Team
 * @Website www.l2jsunrise.com
 */
public class GenerateHtmls
{
	private static String _extraImgId;
	private static String _refreshImgId;
	
	public GenerateHtmls(L2PcInstance player)
	{
		// Dummy default
	}
	
	public static void captchaHtml(L2PcInstance activeChar, String botType)
	{
		StringBuilder tb = new StringBuilder();
		NpcHtmlMessage antibotReply = new NpcHtmlMessage(0);
		
		generateAntibotImages(activeChar);
		// Random image file name
		int imgId = IdFactory.getInstance().getNextId();
		// Conversion from .png to .dds, and crest packed send
		CaptchaImageGenerator.getInstance().captchaLogo(activeChar, imgId);
		
		tb.append("<html><title>Captcha Antibot System</title><body><center>Enter the 5-digits code below and click Confirm.<br>");
		tb.append("<img src=\"Crest.crest_" + Config.SERVER_ID + "_" + imgId + "\" width=256 height=64><br>");
		tb.append("<table width=254><tr>");
		tb.append("<td width=200 align=left><font color=\"888888\">To refresh the image click this button:</font></td><td width=20 align=left");
		
		switch (botType)
		{
			case "FARM":
				tb.append("<button action=\"bypass -h voice .farmcaptcha\" width=16 height=16 back=\"" + _refreshImgId + "\" fore=\"" + _refreshImgId + "\">");
				tb.append("</td></tr></table><br1>");
				tb.append("<font color=\"888888\">(There are only english uppercase letters.)</font><br1>");
				if (Config.ACCEPT_GEOEDITOR_CONN)
				{
					tb.append("<table width=254><tr>");
					tb.append("<td width=160 align=left><font color=\"849D68\">Select the correct answer:</font></td>");
					tb.append("<td align=center><img src=\"" + _extraImgId + "\" width=32 height=16></td>");
					tb.append("<td align=right><combobox width=62 var=type list=Maybe;Yes;No></td>");
					tb.append("</tr></table><br1>");
				}
				tb.append("<font color=\"FF0000\">Tries Left: " + activeChar.getTries() + "</font><br>");
				tb.append("<edit var=\"code\" width=110><br>");
				tb.append("<button value=\"Confirm\" action=\"bypass -h voice .antibot $code $type\" width=80 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"><br>");
				tb.append("<table bgcolor=2E2E2E>");
				tb.append("<tr>");
				tb.append("<td width=270><font color=898989>If you close by mistake this window, you can open it again by typing </font>");
				tb.append("<font color=849D68>\".captcha\"</font><font color=898989> in all chat.<br1>");
				tb.append("You have " + Config.ACCEPT_GEOEDITOR_CONN + " second(s) to answer and 3 tries,<br1>");
				tb.append("if time passes or your answer is wrong to all tries, punishment will be jail.</font></td>");
				activeChar.setFarmBotCode(CaptchaImageGenerator.getInstance().getFinalString());
				break;
			case "ENCHANT":
				tb.append("<button action=\"bypass -h voice .enchantcaptcha\" width=16 height=16 back=\"" + _refreshImgId + "\" fore=\"" + _refreshImgId + "\">");
				tb.append("</td></tr></table><br1>");
				tb.append("<font color=\"888888\">(There are only english uppercase letters.)<br1>");
				if (Config.ACCEPT_GEOEDITOR_CONN)
				{
					tb.append("<table width=254><tr>");
					tb.append("<td width=160 align=left><font color=\"849D68\">Select the correct answer:</font></td>");
					tb.append("<td align=center><img src=\"" + _extraImgId + "\" width=32 height=16></td>");
					tb.append("<td align=right><combobox width=62 var=type list=Maybe;Yes;No></td>");
					tb.append("</tr></table><br>");
				}
				tb.append("<edit var=\"code\" width=110><br>");
				tb.append("<button value=\"Confirm\" action=\"bypass -h voice .enchantbot $code $type\" width=80 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"><br>");
				tb.append("<table bgcolor=2E2E2E>");
				tb.append("<tr>");
				tb.append("<td width=270><font color=898989>If you close by mistake this window, it will open again in few seconds.</font></td>");
				activeChar.setEnchantBotCode(CaptchaImageGenerator.getInstance().getFinalString());
				break;
			default:
				break;
		}
		tb.append("</tr></table>");
		tb.append("</center></body></html>");
		
		antibotReply.setHtml(tb.toString());
		activeChar.sendPacket(antibotReply);
		CaptchaImageGenerator.getInstance().getFinalString().replace(0, 5, "");
	}
	
	private static void generateAntibotImages(L2PcInstance activeChar)
	{
		int r = Rnd.get(1, 3);
		
		switch (r)
		{
			case 1:
				activeChar.setBotAnswer("Maybe");
				_extraImgId = "%image:maybe.png%";
				break;
			case 2:
				activeChar.setBotAnswer("Yes");
				_extraImgId = "%image:yes.png%";
				break;
			case 3:
				activeChar.setBotAnswer("No");
				_extraImgId = "%image:no.png%";
				break;
			default:
				break;
		}
		
		_refreshImgId = "%image:refresh.png%";
	}
}