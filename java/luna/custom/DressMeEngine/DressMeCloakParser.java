package luna.custom.DressMeEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import luna.custom.data.xml.AbstractFileParser;

public final class DressMeCloakParser extends AbstractFileParser<DressMeCloakHolder>
{
	private final String CLOAK_FILE_PATH = "data/xml/sunrise/dressme/cloak.xml";
	
	private static final DressMeCloakParser _instance = new DressMeCloakParser();
	
	public static DressMeCloakParser getInstance()
	{
		return _instance;
	}
	
	private DressMeCloakParser()
	{
		super(DressMeCloakHolder.getInstance());
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
						if (d.getNodeName().equalsIgnoreCase("cloak"))
						{
							int number = Integer.parseInt(d.getAttributes().getNamedItem("number").getNodeValue());
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String name = d.getAttributes().getNamedItem("name").getNodeValue();
							int itemId = 0;
							long itemCount = 0;
							int requiredCloakId = 0;
							
							for (Node att = d.getFirstChild(); att != null; att = att.getNextSibling())
							{
								if ("price".equalsIgnoreCase(att.getNodeName()))
								{
									itemId = Integer.parseInt(att.getAttributes().getNamedItem("id").getNodeValue());
									itemCount = Long.parseLong(att.getAttributes().getNamedItem("count").getNodeValue());
								}
								
								if ("requiredCloakId".equalsIgnoreCase(att.getNodeName()))
								{
									requiredCloakId = Integer.parseInt(att.getAttributes().getNamedItem("setId").getNodeValue());
								}
							}
							
							getHolder().addCloak(new DressMeCloakData(number, id, name, itemId, itemCount, requiredCloakId));
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