package net.sf.l2j.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.gameserver.GameServer;


public class IconsTable
{
	private final Map<Integer, String> itemIcons = new HashMap<>();
	private final Map<Integer, String> skillIcons = new HashMap<>();
	private static final Logger _log = Logger.getLogger(GameServer.class.getName());
	
	protected IconsTable()
	{
		parseData();
		parseSkillData();
	}
	
	public void reload()
	{
		itemIcons.clear();
		skillIcons.clear();
		parseData();
		parseSkillData();
	}
	public void parseSkillData()
	{
		final long t0 = System.currentTimeMillis();
		try
		{
//			loadItemIcons();
//			loadSkillIcons();
			
			final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
			final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			
			
			final File iconsXml = new File("data/xml/icons.xml");
			
			final Document doc = docBuilder.parse(iconsXml); 
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("icons".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
					{
						final NamedNodeMap nnm = n1.getAttributes();
						
						if ("skill".equalsIgnoreCase(n1.getNodeName()))
						{
							final int id = Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
							final String icon = nnm.getNamedItem("icon").getNodeValue();
							
							skillIcons.put(id, icon);
						}
//						else if ("item".equalsIgnoreCase(n1.getNodeName()))
//						{
//							final int id = Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
//							final String icon = nnm.getNamedItem("icon").getNodeValue();
//							
//							itemIcons.put(id, icon);
//						}
					}
				}
			}
			
			final long t = System.currentTimeMillis() - t0;
			_log.config("IconsTable: Succesfully loaded " + (itemIcons.size() + skillIcons.size()) + " icons, in " + t + " Millisecondss.");
		}
		catch (final Exception e)
		{
			_log.config("IconsTable: Failed loading IconsTable. Possible error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	public void parseData()
	{
		final long t0 = System.currentTimeMillis();
		try
		{
			loadItemIcons();
			// not needed loadSkillIcons();
			final long t = System.currentTimeMillis() - t0;
			_log.config("IconsTable: Succesfully loaded " + (itemIcons.size() + skillIcons.size()) + " icons, in " + t + " Milliseconds.");
		}
		catch (final Exception e)
		{
			_log.config("IconsTable: Failed loading IconsTable. Possible error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void loadItemIcons() throws Exception
	{
		final File f = new File("./data/icons.xml");
		try (final BufferedReader br = new BufferedReader(new FileReader(f)))
		{
			String line = null;
			while ((line=br.readLine()) != null)
			{
				String[] explode = line.split("\t");
				if (explode.length > 1)
				{
					final int id = Integer.parseInt(explode[0]);
					itemIcons.put(id, explode[1]);
				}
			}
		}
	}
	
	/**
	 * @param id the requested itemId
	 * @return the String value of the Icon of the given itemId.
	 */
	public String getItemIcon(final int id)
	{
		final String ico = itemIcons.get(id);
		if (ico == null)
		{
			//_log.config("IconsTable: Invalid Item-Icon request: " + id + ", or it doesn't exist, Ignoring ...");
			return "icon.skill1050";
		}
		return ico;
	}
	
	/**
	 * @param id the requested skillId
	 * @return the String value of the Icon of the given itemId.
	 */
	public String getSkillIcon(final int id)
	{
		final String ico = skillIcons.get(id);
		if (ico == null)
		{
			//_log.config("IconsTable: Invalid Skill-Icon request: " + id + ", or it doesn't exist, Ignoring ...");
			return "icon.skill1050";
		}
		return ico;
	}
	
	public static final IconsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static final IconsTable _instance = new IconsTable();
	}
}