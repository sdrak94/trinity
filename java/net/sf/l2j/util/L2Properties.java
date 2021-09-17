package net.sf.l2j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class L2Properties extends Properties
{
	private static final long	serialVersionUID	= 1L;
	private static Logger		_log				= LoggerFactory.getLogger(L2Properties.class);
	
	public L2Properties()
	{}
	
	public L2Properties(String name) throws IOException
	{
		try (FileInputStream fis = new FileInputStream(name);)
		{
			this.load(fis);
		}
	}
	
	public L2Properties(File file) throws IOException
	{
		try (FileInputStream fis = new FileInputStream(file);)
		{
			this.load(fis);
		}
	}
	
	public L2Properties(InputStream inStream) throws IOException
	{
		this.load(inStream);
	}
	
	public L2Properties(Reader reader) throws IOException
	{
		this.load(reader);
	}
	
	public void load(String name) throws IOException
	{
		try (FileInputStream fis = new FileInputStream(name);)
		{
			this.load(fis);
		}
	}
	
	public void load(File file) throws IOException
	{
		try (FileInputStream fis = new FileInputStream(file);)
		{
			this.load(fis);
		}
	}
	
	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public void load(InputStream inStream) throws IOException
	{
		try
		{
			try (InputStreamReader isr = new InputStreamReader(inStream, Charset.defaultCharset());)
			{
				super.load(isr);
			}
		}
		finally
		{
			inStream.close();
		}
	}
	
	@Override
	public void load(Reader reader) throws IOException
	{
		try
		{
			super.load(reader);
		}
		finally
		{
			reader.close();
		}
	}
	
	@Override
	public String getProperty(String key)
	{
		String property = super.getProperty(key);
		if (property == null)
		{
			_log.info("L2Properties: Missing property for key - " + key);
			return null;
		}
		return property.trim();
	}
	
	@Override
	public String getProperty(String key, String defaultValue)
	{
		String property = super.getProperty(key, defaultValue);
		if (property == null)
		{
			_log.warn("L2Properties: Missing defaultValue for key - " + key);
			return null;
		}
		return property.trim();
	}
	
	private String getValue(String key)
	{
		String value = super.getProperty(key);
		return value != null ? value.trim() : null;
	}
	
	public String getString(String key, String defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			_log.warn(" [ ] missing property for key: " + key + " using default value: " + defaultValue);
			return defaultValue;
		}
		return value;
	}

	public boolean getBoolean(String key, boolean defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			_log.warn("[] missing property for key: " + key + " using default value: " + defaultValue);
			return defaultValue;
		}
		
		if (value.equalsIgnoreCase("true"))
		{
			return true;
		}
		else if (value.equalsIgnoreCase("false"))
		{
			return false;
		}
		else
		{
			_log.warn("[] Invalid value specified for key: " + key + " specified value: " + value + " should be \"boolean\" using default value: " + defaultValue);
			return defaultValue;
		}
	}
}
