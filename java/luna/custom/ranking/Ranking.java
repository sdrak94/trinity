package luna.custom.ranking;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import luna.custom.ranking.comperators.FameComparator;
import luna.custom.ranking.comperators.PkComparator;
import luna.custom.ranking.comperators.PvPComparator;
import luna.custom.ranking.comperators.RaidComparator;
import luna.custom.ranking.xml.data.RankData;
import luna.custom.ranking.xml.data.RanksHolder;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class Ranking 
{
	private static final String top_query = "SELECT c.charId, c.char_name, c.title, l.class_name, c.accesslevel, c.pvpkills, c.pkkills, c.fame, d.clan_name, d.crest_id, d.ally_crest_id, ( SELECT SUM(points) FROM character_raid_points WHERE charId = c.charId) FROM characters c INNER JOIN class_list l ON c.classid = l.id LEFT JOIN clan_data d ON c.clanid = d.clan_id";

	//private static final String top_query = "SELECT c.charId, c.char_name, c.title, l.class_name, c.pvpkills, c.pkkills, c.fame, d.clan_name, d.crest_id, d.ally_crest_id, ( SELECT SUM(points) FROM character_raid_points WHERE charId = c.charId) FROM characters c INNER JOIN class_list l ON c.classid = l.id LEFT JOIN clan_data d ON c.clanid = d.clan_id WHERE c.accesslevel =0";
	
	public static final ArrayList<TopData> topPvp = new ArrayList<>();
	public static final ArrayList<TopData> topPk = new ArrayList<>();
	public static final ArrayList<TopData> topFame = new ArrayList<>();
	public static final ArrayList<TopData> topRaid = new ArrayList<>();

	private static void buildQuery()
	{
		
		topPvp.clear();
		topPk.clear();
		topFame.clear();
		topRaid.clear();
		
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
		     final Statement  st  = con.createStatement();
		     final ResultSet  rs  = st.executeQuery(top_query))
		{
			while (rs.next())
			{
				final TopData topData = new TopData(rs);

				if (rs.getString("char_name").contains("[GM]") || rs.getInt("accesslevel") !=0)
				{
					continue;
				}
				topPvp.add(topData);
			}
			
			topPk.addAll(topPvp);
			topFame.addAll(topPvp);
			topRaid.addAll(topPvp);
			
			Collections.sort(topPvp, new PvPComparator());
			Collections.sort(topPk, new PkComparator());
			Collections.sort(topFame, new FameComparator());
			Collections.sort(topRaid, new RaidComparator());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void start()
	{
		System.out.println("Starting Ranking...");
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
				{
					@Override
					public void run() 
					{
						buildQuery();
					}
			
				}, 0, 10000);
	}
	
	public static int getMyPosition(ArrayList<TopData> list, L2PcInstance activeChar)
	{
		int mypos = 0;
		for (;mypos < list.size(); mypos++)
		{
			final TopData topData = list.get(mypos);
			if (topData.getCharId() == activeChar.getObjectId())
				return mypos;
		}
		return 0;
	}
	
	public static RankData getRank(final int pos)
	{
		final RanksHolder rankTables = RanksHolder.getInstance();
		
		int givenSlots = 0;
		for (RankData rank : rankTables.getRanks())
		{
			if (pos < (givenSlots += rank.getPool()))
				return rank;
		}
		
		return rankTables.getDefault();
	}
	
	public static RankData getRank(ArrayList<TopData> list, L2PcInstance player)
	{
		return getRank(getMyPosition(list,player));
	}
	
	
}
