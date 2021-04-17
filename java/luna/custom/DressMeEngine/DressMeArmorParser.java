package luna.custom.DressMeEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javolution.util.FastMap;
import luna.custom.data.xml.AbstractFileParser;

public final class DressMeArmorParser extends AbstractFileParser<DressMeArmorHolder>
{
	private final String ARMOR_FILE_PATH = "data/xml/sunrise/dressme/armor.xml";
	
	private static final DressMeArmorParser _instance = new DressMeArmorParser();
	private final Map<String, Object> _set = new FastMap<String, Object>();
	
	public static DressMeArmorParser getInstance()
	{
		return _instance;
	}
	
	private DressMeArmorParser()
	{
		super(DressMeArmorHolder.getInstance());
	}
	
	public File getXMLFile()
	{
		return new File(ARMOR_FILE_PATH);
	}
	public float getFloat(String name, float deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number)val).floatValue();
		try {
			return (float)Double.parseDouble((String)val);
		} catch (Exception e) {
			throw new IllegalArgumentException("Float value required, but found: "+val);
		}
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
						if (d.getNodeName().equalsIgnoreCase("dress"))
						{
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String name = d.getAttributes().getNamedItem("name").getNodeValue();
							String type = d.getAttributes().getNamedItem("type").getNodeValue();
							
							int itemId = 0;
							int requiredArmorSet = 0;
							long itemCount = 0;
							float requiredTier = 0;
							int chest = 0;
							int legs = 0;
							int gloves = 0;
							int feet = 0;
							
							int limitedTimePriceId = 0;
							int limitedTimePriceCount = 0;
							int limitedTimeHours = 0;
							int limitedTimePriceId2 = 0;
							int limitedTimePriceCount2 = 0;
							int limitedTimeHours2 = 0;
							int limitedTimePriceId3 = 0;
							int limitedTimePriceCount3 = 0;
							int limitedTimeHours3 = 0;
							
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if ("set".equalsIgnoreCase(att.getNodeName()))
								{
									chest = Integer.parseInt(att.getAttributes().getNamedItem("chest").getNodeValue());
									legs = Integer.parseInt(att.getAttributes().getNamedItem("legs").getNodeValue());
									gloves = Integer.parseInt(att.getAttributes().getNamedItem("gloves").getNodeValue());
									feet = Integer.parseInt(att.getAttributes().getNamedItem("feet").getNodeValue());
								}
								
								if ("price".equalsIgnoreCase(att.getNodeName()))
								{
									itemId = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									itemCount = Long.parseLong(att.getAttributes().getNamedItem("count").getNodeValue());
								}

								if ("requiredArmorSetId".equalsIgnoreCase(att.getNodeName()))
								{
									requiredArmorSet = Integer.parseInt(att.getAttributes().getNamedItem("setId").getNodeValue());
								}
								if ("requiredTier".equalsIgnoreCase(att.getNodeName()))
								{
									requiredTier = Integer.parseInt(att.getAttributes().getNamedItem("minTier").getNodeValue());
								}
								if ("limitedTimePrice".equalsIgnoreCase(att.getNodeName()))
								{
									limitedTimePriceId = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount = Integer.parseInt(att.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours = Integer.parseInt(att.getAttributes().getNamedItem("hours").getNodeValue());
								}
								if ("limitedTimePrice2".equalsIgnoreCase(att.getNodeName()))
								{
									limitedTimePriceId2 = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount2 = Integer.parseInt(att.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours2 = Integer.parseInt(att.getAttributes().getNamedItem("hours").getNodeValue());
								}
								if ("limitedTimePrice3".equalsIgnoreCase(att.getNodeName()))
								{
									limitedTimePriceId3 = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									limitedTimePriceCount3 = Integer.parseInt(att.getAttributes().getNamedItem("count").getNodeValue());
									limitedTimeHours3 = Integer.parseInt(att.getAttributes().getNamedItem("hours").getNodeValue());
								}
							}
							
							getHolder().addDress(new DressMeArmorData(id, name, type, chest, legs, gloves, feet, itemId, itemCount, requiredTier, requiredArmorSet, limitedTimePriceId, limitedTimePriceCount, limitedTimeHours, limitedTimePriceId2, limitedTimePriceCount2, limitedTimeHours2, limitedTimePriceId3, limitedTimePriceCount3, limitedTimeHours3));
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