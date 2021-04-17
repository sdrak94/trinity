package luna.custom.ranking.xml.data;

import java.util.logging.Logger;




public class RankLoader
{
	
	private static final Logger _log = Logger.getLogger(RankLoader.class.getName());

	
	public static void load()
	{
		RanksParser.getInstance().load();
	}
}
