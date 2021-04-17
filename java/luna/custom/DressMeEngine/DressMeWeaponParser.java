package luna.custom.DressMeEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import luna.custom.data.xml.AbstractFileParser;

public final class DressMeWeaponParser extends AbstractFileParser<DressMeWeaponHolder>
{
	private final String WEAPON_FILE_PATH = "data/xml/sunrise/dressme/weapon.xml";
	
	private static final DressMeWeaponParser _instance = new DressMeWeaponParser();
	
	public static DressMeWeaponParser getInstance()
	{
		return _instance;
	}
	
	private DressMeWeaponParser()
	{
		super(DressMeWeaponHolder.getInstance());
	}
	
	@Override
	public File getXMLFile()
	{
		return new File(WEAPON_FILE_PATH);
	}
	
	@Override
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
						if (d.getNodeName().equalsIgnoreCase("weapon"))
						{
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String name = d.getAttributes().getNamedItem("name").getNodeValue();
							String type = d.getAttributes().getNamedItem("type").getNodeValue();
							
							Node isBigNode = d.getAttributes().getNamedItem("isBig");
							Boolean isBig = (isBigNode != null) && Boolean.parseBoolean(isBigNode.getNodeValue());
							
							int itemId = 0;
							long itemCount = 0;
							int requiredWepId = 0;
							float requiredTier = 0;
							
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if ("price".equalsIgnoreCase(att.getNodeName()))
								{
									itemId = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									itemCount = Long.parseLong(att.getAttributes().getNamedItem("count").getNodeValue());
								}
								if ("requiredTier".equalsIgnoreCase(att.getNodeName()))
								{
									requiredTier = Integer.parseInt(att.getAttributes().getNamedItem("minTier").getNodeValue());
								}
								if ("requiredWepId".equalsIgnoreCase(att.getNodeName()))
								{
									requiredWepId = Integer.parseInt(att.getAttributes().getNamedItem("setId").getNodeValue());
								}
							}
							
							getHolder().addWeapon(new DressMeWeaponData(id, name, type, isBig, itemId, itemCount, requiredWepId, requiredTier));
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