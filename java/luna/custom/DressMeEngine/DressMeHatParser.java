package luna.custom.DressMeEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import luna.custom.data.xml.AbstractFileParser;

public final class DressMeHatParser extends AbstractFileParser<DressMeHatHolder>
{
	private final String					CLOAK_FILE_PATH	= "data/xml/sunrise/dressme/hat.xml";
	private static final DressMeHatParser	_instance		= new DressMeHatParser();
	
	public static DressMeHatParser getInstance()
	{
		return _instance;
	}
	
	private DressMeHatParser()
	{
		super(DressMeHatHolder.getInstance());
	}
	
	public File getXMLFile()
	{
		return new File(CLOAK_FILE_PATH);
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
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("hat"))
						{
							int number = Integer.parseInt(d.getAttributes().getNamedItem("number").getNodeValue());
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String name = d.getAttributes().getNamedItem("name").getNodeValue();
							int slot = Integer.parseInt(d.getAttributes().getNamedItem("slot").getNodeValue());
							int itemId = 0;
							long itemCount = 0;
							int limitedTimePriceId = 0;
							int limitedTimePriceCount = 0;
							int limitedTimeHours = 0;
							int limitedTimePriceId2 = 0;
							int limitedTimePriceCount2 = 0;
							int limitedTimeHours2 = 0;
							int limitedTimePriceId3 = 0;
							int limitedTimePriceCount3 = 0;
							int limitedTimeHours3 = 0;
							for (Node price = d.getFirstChild(); price != null; price = price.getNextSibling())
							{
								if ("price".equalsIgnoreCase(price.getNodeName()))
								{
									itemId = Integer.parseInt(price.getAttributes().getNamedItem("id").getNodeValue());
									itemCount = Long.parseLong(price.getAttributes().getNamedItem("count").getNodeValue());
								}
								if ("limitedTimePrice".equalsIgnoreCase(price.getNodeName()))
								{
									limitedTimePriceId = Integer.parseInt(price.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount = Integer.parseInt(price.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours = Integer.parseInt(price.getAttributes().getNamedItem("hours").getNodeValue());
								}
								if ("limitedTimePrice2".equalsIgnoreCase(price.getNodeName()))
								{
									limitedTimePriceId2 = Integer.parseInt(price.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount2 = Integer.parseInt(price.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours2 = Integer.parseInt(price.getAttributes().getNamedItem("hours").getNodeValue());
								}
								if ("limitedTimePrice3".equalsIgnoreCase(price.getNodeName()))
								{
									limitedTimePriceId3 = Integer.parseInt(price.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount3 = Integer.parseInt(price.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours3 = Integer.parseInt(price.getAttributes().getNamedItem("hours").getNodeValue());
								}
							}
							getHolder().addHat(new DressMeHatData(number, id, name, slot, itemId, itemCount, limitedTimePriceId, limitedTimePriceCount, limitedTimeHours, limitedTimePriceId2, limitedTimePriceCount2, limitedTimeHours2, limitedTimePriceId3, limitedTimePriceCount3, limitedTimeHours3));
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