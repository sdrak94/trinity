package luna.custom.achievements;


import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AchievementBBSManager extends BaseBBSManager
{
	public static final String PATH = "data/html/CommunityBoard";
	
	public String[] getBypassCommands()
	{
		return new String[]
		{
			"_bbsAch"
		};
	}
	
	public static AchievementBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementBBSManager _instance = new AchievementBBSManager();
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
			String html = command;
			separateAndSend(html, activeChar);
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
	
	}
}
