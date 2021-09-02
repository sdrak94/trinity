package ghosts.controller;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ghosts.model.GhostTemplate;

public class GhostTemplateTable
{
	final HashMap<String, GhostTemplate> ghostTemplates = new HashMap<>();

	private GhostTemplateTable()
	{
		try
		{
			load();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void load() throws Exception
	{
		ghostTemplates.clear();
		
		final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
		final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		
		final File templatesFolder = new File("./data/xml/ghosts/templates/");
		
		for (final File templateFile : templatesFolder.listFiles())
		{
			final Document doc = docBuilder.parse(templateFile);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("templates".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
					{
						if ("template".equalsIgnoreCase(n1.getNodeName()))
						{
							final GhostTemplate ghostTemplate = new GhostTemplate(n1);
							
							final String templateId = ghostTemplate.getTemplateId();
							
							ghostTemplates.put(templateId, ghostTemplate);
						}
					}
				}
				
				
			}
			
		}
		System.out.println("GhostTemplateTable Loaded " + ghostTemplates.size() + " templates.");
	}
	
	public GhostTemplate getById(final String templateId)
	{
		return ghostTemplates.get(templateId);
	}

	public void reload()
	{
		try 
		{
			load();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	public static class InstanceHolder
	{
		private static final GhostTemplateTable _instance = new GhostTemplateTable();
	}

	public static GhostTemplateTable getInstance()
	{
		return InstanceHolder._instance;
	}

}
