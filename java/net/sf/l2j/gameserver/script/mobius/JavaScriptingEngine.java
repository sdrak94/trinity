package net.sf.l2j.gameserver.script.mobius;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * @author Mobius
 */
public class JavaScriptingEngine
{
	private static final Logger LOGGER = Logger.getLogger(JavaScriptingEngine.class.getName());
	
	private static final Map<String, String> _properties = new HashMap<>();
	private static final JavaCompiler _compiler = ToolProvider.getSystemJavaCompiler();
	
	public JavaScriptingEngine()
	{
		// Load config.
		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream("config/ScriptEngine.ini"))
		{
			props.load(fis);
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not load ScriptEngine.ini: " + e.getMessage());
		}
		
		// Set properties.
		for (Entry<Object, Object> prop : props.entrySet())
		{
			_properties.put((String) prop.getKey(), (String) prop.getValue());
		}
	}
	
	public JavaExecutionContext createExecutionContext()
	{
		return new JavaExecutionContext();
	}
	
	public String getProperty(String key)
	{
		return _properties.get(key);
	}
	
	public JavaCompiler getCompiler()
	{
		return _compiler;
	}
}