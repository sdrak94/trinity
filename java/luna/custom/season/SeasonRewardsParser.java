package luna.custom.season;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import cz.nxs.events.engine.EventRewardSystem.RewardItem;
import javolution.util.FastMap;


	public final class SeasonRewardsParser
	{
		private final String REWARDS_FILE = "data/xml/ranking/rewards.xml";
		
		private static final SeasonRewardsParser _instance = new SeasonRewardsParser();
		
		public static SeasonRewardsParser getInstance()
		{
			return _instance;
		}
		
	
		
		public File getXMLFile()
		{
			return new File(REWARDS_FILE);
		}
		
		protected void readData()
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			File file = getXMLFile();
			
			try
			{
				InputSource in = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				in.setEncoding("UTF-8");
				Document doc = factory.newDocumentBuilder().parse(in);
				
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if (n.getNodeName().equalsIgnoreCase("rewards"))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							String rank = d.getAttributes().getNamedItem("rank").getNodeValue();
							{
								int itemId = Integer.parseInt(d.getAttributes().getNamedItem("itemId").getNodeValue());
								int ammount = Integer.parseInt(d.getAttributes().getNamedItem("ammount").getNodeValue());
							}
						}
					}
				}
			}
			catch (Exception e)
			{
			}
		}
		public class SeasonRewards
		{
			private Map<RankContainer, Map<Integer, RewardItem>> _rewards;
			
			public SeasonRewards()
			{
				_rewards = new FastMap<RankContainer, Map<Integer, RewardItem>>();
			}
		}
		public class RankContainer
		{
			public RankContainer rank;
			
			RankContainer(RankContainer rank)
			{
				this.rank = rank;
			}
			
		}
}
