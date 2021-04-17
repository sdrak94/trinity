/**
 * 
 */
package cz.nxs.events.engine.lang;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import cz.nxs.events.NexusLoader;
import javolution.util.FastMap;


/**
 * @author hNoke
 *
 */
public final class LanguageEngine
{
	private static final String DIRECTORY = "config/nexus language";
	
	private static Map<String, String> _msgMap = new FastMap<String, String>();
	private static Map<String, String> _languages = new FastMap<String, String>();
	
	private static String _currentLang = "en";
	
	public static void init()
	{
		try
		{
			prepare();
			load();
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error while loading language files", Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	public static void prepare() throws IOException
	{
		File folder = new File(DIRECTORY);
		if(!folder.exists() || folder.isDirectory())
			folder.mkdir();
	}
	
	public static void load() throws IOException
	{
		File dir = new File(DIRECTORY);

		for(File file : dir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				if(pathname.getName().endsWith(".xml"))
					return true;
				return false;
			}
		}))
		{
			if(file.getName().startsWith("nexus_lang_"))
				loadXml(file, file.getName().substring(11, file.getName().indexOf(".xml")));
		}
		
		NexusLoader.debug("Loaded " + _languages.size() + " languages.");
	}
	
	private static void loadXml(File file, final String lang)
	{
		int count = 0;
		String version = "";
		String langName = "";
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		Document doc = null;
        
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				NexusLoader.debug("Could not load language file for nexus engine - " + lang, Level.WARNING);
			}
			
			Node n = doc.getFirstChild();
			NamedNodeMap docAttr = n.getAttributes();
			
			if (docAttr.getNamedItem("version") != null)
				version = docAttr.getNamedItem("version").getNodeValue();
			
			if (docAttr.getNamedItem("lang") != null)
				langName = docAttr.getNamedItem("lang").getNodeValue();
			
			if(version != null)
				NexusLoader.debug("Processing language file for language - " + lang + "; version " + version, Level.INFO);
			
			if(!version.equals(NexusLoader.version))
				NexusLoader.debug("Language file for language " + lang + " is not up-to-date with latest version of the engine (" + NexusLoader.version + "). Some newly added messages might not be translated.", Level.WARNING);
			
			if(!_languages.containsKey(lang))
				_languages.put(lang, langName);
			
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("message"))
				{
					NamedNodeMap attrs = d.getAttributes();
					String id = attrs.getNamedItem("id").getNodeValue();
					String text = attrs.getNamedItem("text").getNodeValue();
					
					_msgMap.put(lang + "_" + id, text);
					count ++;
				}
			}
		}
		
		NexusLoader.debug("Loaded language file for language " + lang + " " + count + " messages.", Level.INFO);
	}
	
	public static String getMsgByLang(String lang, String id)
	{
		String msg = _msgMap.get(lang + "_" + id); 
		if(msg == null)
			msg = _msgMap.get("en" + "_" + id);
		
		if(msg == null)
			NexusLoader.debug("No Msg found: ID " + id + " lang = " + lang, Level.WARNING);
		
		return msg;
	}
	
	public static String getMsg(String id)
	{
		String lang = getLanguage();
		
		if(lang == null)
			lang = "en";
		
		return getMsgByLang(lang, id);
	}
	
	public static String getMsg(String id, Object... obs)
	{
		String msg = getMsg(id);
		return fillMsg(msg, obs);
	}
	
	public static String fillMsg(String msg, Object... obs)
	{
		String newMsg = msg;
		int first;
		
		for(Object o : obs)
		{
			if(o instanceof Integer || o instanceof Long)
			{
				first = newMsg.indexOf("%i");
				if(first == -1)
					continue;
				else
				{
					if(o instanceof Integer)
						newMsg = newMsg.replaceFirst("%i", ((Integer)o).toString());
					else
						newMsg = newMsg.replaceFirst("%i", ((Long)o).toString());
				}
			}
			else if(o instanceof Double)
			{
				first = newMsg.indexOf("%d");
				if(first == -1)
					continue;
				else
				{
					newMsg = newMsg.replaceFirst("%d", ((Double)o).toString());
				}
			}
			else if(o instanceof String)
			{
				first = newMsg.indexOf("%s");
				if(first == -1)
					continue;
				else
				{
					newMsg = newMsg.replaceFirst("%s", (String)o);
				}
			}
		}
		
		return newMsg;
	}
	
	public static void setLanguage(String lang)
	{
		_currentLang = lang;
	}
	
	public static String getLanguage()
	{
		return _currentLang;
	}
	
	public static Map<String, String> getLanguages()
	{
		return _languages;
	}
}
