package luna.custom.ranking.xml.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import luna.custom.data.xml.AbstractFileParser;

	public final class RanksParser extends AbstractFileParser<RanksHolder>
	{
		private final String RANKS_FILE = "data/xml/ranking/ranks.xml";
		
		private static final RanksParser _instance = new RanksParser();
		
		public static RanksParser getInstance()
		{
			return _instance;
		}
		
		private RanksParser()
		{
			super(RanksHolder.getInstance());
		}
		
		public File getXMLFile()
		{
			return new File(RANKS_FILE);
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
					if (n.getNodeName().equalsIgnoreCase("ranks"))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("rank"))
							{
								String name = d.getAttributes().getNamedItem("name").getNodeValue();
								int pool = Integer.parseInt(d.getAttributes().getNamedItem("pool").getNodeValue());
								String color = d.getAttributes().getNamedItem("color").getNodeValue();
								String icon = d.getAttributes().getNamedItem("icon").getNodeValue();
										
								getHolder().addRank((new RankData(name, pool, color, icon)));
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.warn(getClass().getSimpleName() + ": Error: " + e);
			}
		}
	}