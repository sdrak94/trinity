package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.List;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.CharactersTable;
import net.sf.l2j.gameserver.datatables.CharactersTable.CharacterLoginData;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class LogindataBBSManager extends BaseBBSManager
{
	public static final String PATH = "data/html/CommunityBoard";
	private static final int LIMIT = 25;
	
	public String[] getBypassCommands()
	{
		return new String[]
		{
			"_bbslink"
		};
	}
	
	public static LogindataBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LogindataBBSManager _instance = new LogindataBBSManager();
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if ("_bbslink".equalsIgnoreCase(command) || command.contains("_maillist_0_1_0_"))
		{
			List<CharacterLoginData> list = CharactersTable.getAllLoginData(activeChar.getObjectId(), LIMIT);
			String html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_logindata.htm");
			
			for (int i = 1; i <= LIMIT; i++)
			{
				CharacterLoginData data = list.size() >= i ? list.get(i - 1) : null;
				if (data != null)
				{
					html = html.replaceAll("%date" + i + "%", data.getLoginDate());
					html = html.replaceAll("%ip" + i + "%", data.getIP());
					html = html.replaceAll("%hwid" + i + "%", data.getHWID());
					html = html.replaceAll("%onlinetime" + i + "%", data.getOnlineTime());
					html = html.replaceAll("%log" + i + "%", data.getLog(1));
				}
			}
			
			// Clean the other %var% shits
			html = html.replaceAll("%date(.*)%", "&nbsp;");
			html = html.replaceAll("%ip(.*)%", "&nbsp;");
			html = html.replaceAll("%hwid(.*)%", "&nbsp;");
			html = html.replaceAll("%onlinetime(.*)%", "&nbsp;");
			html = html.replaceAll("%log(.*)%", "&nbsp;");
			
			separateAndSend(html, activeChar);
		}
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
	
	}
}
