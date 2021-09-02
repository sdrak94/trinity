package net.sf.l2j.util;

/*
 * Copyright (C) 2004-2020 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileFilter;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import net.sf.l2j.Config;


/**
 * Interface for XML parsers.
 * @author Zoey76
 */
public interface IXmlReader
{
	static final Logger LOG = LoggerFactory.getLogger(IXmlReader.class);
	
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	/** The default file filter, ".xml" files only. */
	static final XMLFilter XML_FILTER = new XMLFilter();
	
	/**
	 * This method can be used to load/reload the data.<br>
	 * It's highly recommended to clear the data storage, either the list or map.
	 */
	public void load();
	
	/**
	 * Wrapper for {@link #parseFile(File)} method.
	 * @param path the relative path to the datapack root of the XML file to parse.
	 */
	default void parseDatapackFile(String path)
	{
		parseFile(new File(Config.DATAPACK_ROOT, path));
	}
	/**
	 * Parses a single XML file.<br>
	 * If the file was successfully parsed, call {@link #parseDocument(Document, File)} for the parsed document.<br>
	 * <b>Validation is enforced.</b>
	 * @param f the XML file to parse.
	 */
	default void parseFile(File f)
	{
		if (!getCurrentFileFilter().accept(f))
		{
			LOG.warn("{}: Could not parse {} is not a file or it doesn't exist!", getClass().getSimpleName(), f.getName());
			return;
		}
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setIgnoringComments(true);
		try
		{
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new XMLErrorHandler());
			parseDocument(db.parse(f), f);
		}
		catch (SAXParseException e)
		{
			LOG.warn("{}: Could not parse file {} at line {}, column {}"+ getClass().getSimpleName()+ f.getName()+ e.getLineNumber(), e.getColumnNumber(), e);
			return;
		}
		catch (Exception e)
		{
			LOG.warn("{}: Could not parse file {}"+ getClass().getSimpleName(), f.getName(), e);
			return;
		}
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param file the path to the directory where the XML files are.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	default boolean parseDirectory(File file)
	{
		return parseDirectory(file, false);
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	default boolean parseDirectory(String path)
	{
		return parseDirectory(new File(path), false);
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are.
	 * @param recursive parses all sub folders if there is.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	default boolean parseDirectory(String path, boolean recursive)
	{
		return parseDirectory(new File(path), recursive);
	}
	
	/**
	 * Loads all XML files from {@code path} and calls {@link #parseFile(File)} for each one of them.
	 * @param dir the directory object to scan.
	 * @param recursive parses all sub folders if there is.
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise.
	 */
	default boolean parseDirectory(File dir, boolean recursive)
	{
		if (!dir.exists())
		{
			LOG.warn("{}: Folder {} doesn't exist!", getClass().getSimpleName(), dir.getAbsolutePath());
			return false;
		}
		
		final File[] files = dir.listFiles();
		if (files != null)
		{
			for (File f : files)
			{
				if (recursive && f.isDirectory())
				{
					parseDirectory(f, recursive);
				}
				else if (getCurrentFileFilter().accept(f))
				{
					parseFile(f);
				}
			}
		}
		return true;
	}
	default void forEach(Node node, Consumer<Node> action)
	{
		forEach(node, a -> true, action);
	}
	
	/**
	 * Executes action for each child that matches nodeName
	 * @param node
	 * @param nodeName
	 * @param action
	 */
	default void forEach(Node node, String nodeName, Consumer<Node> action)
	{
		forEach(node, innerNode -> nodeName.equalsIgnoreCase(innerNode.getNodeName()), action);
	}
	
	/**
	 * Executes action for each child of node if matches the filter specified
	 * @param node
	 * @param filter
	 * @param action
	 */
	default void forEach(Node node, Predicate<Node> filter, Consumer<Node> action)
	{
		final NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++)
		{
			final Node targetNode = list.item(i);
			if (filter.test(targetNode))
			{
				action.accept(targetNode);
			}
		}
	}
	
	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are
	 * @param recursive parses all sub folders if there is
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise
	 */
	default boolean parseDatapackDirectory(String path, boolean recursive)
	{
		return parseDirectory(new File(Config.DATAPACK_ROOT, path), recursive);
	}
	
	/**
	 * Abstract method that when implemented will parse the current document.<br>
	 * Is expected to be call from {@link #parseFile(File)}.
	 * @param doc the current document to parse
	 * @param f the current file
	 */
	default void parseDocument(Document doc, File f)
	{
		parseDocument(doc);
	}
	
	/**
	 * Abstract method that when implemented will parse the current document.<br>
	 * Is expected to be call from {@link #parseFile(File)}.
	 * @param doc the current document to parse
	 */
	default void parseDocument(Document doc)
	{
		LOG.error("{}: Parser not implemented!", getClass().getSimpleName());
	}
	
	/**
	 * Parses a boolean value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Boolean parseBoolean(Node node, Boolean defaultValue)
	{
		return node != null ? Boolean.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a boolean value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Boolean parseBoolean(Node node)
	{
		return parseBoolean(node, null);
	}
	
	/**
	 * Parses a boolean value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Boolean parseBoolean(NamedNodeMap attrs, String name)
	{
		return parseBoolean(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a boolean value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Boolean parseBoolean(NamedNodeMap attrs, String name, Boolean defaultValue)
	{
		return parseBoolean(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a byte value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Byte parseByte(Node node, Byte defaultValue)
	{
		return node != null ? Byte.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a byte value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Byte parseByte(Node node)
	{
		return parseByte(node, null);
	}
	
	/**
	 * Parses a byte value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Byte parseByte(NamedNodeMap attrs, String name)
	{
		return parseByte(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a byte value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Byte parseByte(NamedNodeMap attrs, String name, Byte defaultValue)
	{
		return parseByte(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a short value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Short parseShort(Node node, Short defaultValue)
	{
		return node != null ? Short.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a short value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Short parseShort(Node node)
	{
		return parseShort(node, null);
	}
	
	/**
	 * Parses a short value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Short parseShort(NamedNodeMap attrs, String name)
	{
		return parseShort(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a short value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Short parseShort(NamedNodeMap attrs, String name, Short defaultValue)
	{
		return parseShort(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses an int value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default int parseInt(Node node, Integer defaultValue)
	{
		return node != null ? Integer.parseInt(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses an int value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default int parseInt(Node node)
	{
		return parseInt(node, -1);
	}
	
	/**
	 * Parses an integer value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Integer parseInteger(Node node, Integer defaultValue)
	{
		return node != null ? Integer.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses an integer value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Integer parseInteger(Node node)
	{
		return parseInteger(node, null);
	}
	
	/**
	 * Parses an integer value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Integer parseInteger(NamedNodeMap attrs, String name)
	{
		return parseInteger(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses an integer value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Integer parseInteger(NamedNodeMap attrs, String name, Integer defaultValue)
	{
		return parseInteger(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a long value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Long parseLong(Node node, Long defaultValue)
	{
		return node != null ? Long.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a long value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Long parseLong(Node node)
	{
		return parseLong(node, null);
	}
	
	/**
	 * Parses a long value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Long parseLong(NamedNodeMap attrs, String name)
	{
		return parseLong(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a long value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Long parseLong(NamedNodeMap attrs, String name, Long defaultValue)
	{
		return parseLong(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a float value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Float parseFloat(Node node, Float defaultValue)
	{
		return node != null ? Float.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a float value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Float parseFloat(Node node)
	{
		return parseFloat(node, null);
	}
	
	/**
	 * Parses a float value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Float parseFloat(NamedNodeMap attrs, String name)
	{
		return parseFloat(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a float value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Float parseFloat(NamedNodeMap attrs, String name, Float defaultValue)
	{
		return parseFloat(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a double value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Double parseDouble(Node node, Double defaultValue)
	{
		return node != null ? Double.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a double value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Double parseDouble(Node node)
	{
		return parseDouble(node, null);
	}
	
	/**
	 * Parses a double value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default Double parseDouble(NamedNodeMap attrs, String name)
	{
		return parseDouble(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a double value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default Double parseDouble(NamedNodeMap attrs, String name, Double defaultValue)
	{
		return parseDouble(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a string value.
	 * @param node the node to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default String parseString(Node node, String defaultValue)
	{
		return node != null ? node.getNodeValue() : defaultValue;
	}
	
	/**
	 * Parses a string value.
	 * @param node the node to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default String parseString(Node node)
	{
		return parseString(node, null);
	}
	
	/**
	 * Parses a string value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @return if the node is not null, the value of the parsed node, otherwise null
	 */
	default String parseString(NamedNodeMap attrs, String name)
	{
		return parseString(attrs.getNamedItem(name));
	}
	
	/**
	 * Parses a string value.
	 * @param attrs the attributes
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null, the value of the parsed node, otherwise the default value
	 */
	default String parseString(NamedNodeMap attrs, String name, String defaultValue)
	{
		return parseString(attrs.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses an enumerated value.
	 * @param <T> the enumerated type
	 * @param node the node to parse
	 * @param clazz the class of the enumerated
	 * @param defaultValue the default value
	 * @return if the node is not null and the node value is valid the parsed value, otherwise the default value
	 */
	default <T extends Enum<T>> T parseEnum(Node node, Class<T> clazz, T defaultValue)
	{
		if (node == null)
		{
			return defaultValue;
		}
		
		try
		{
			return Enum.valueOf(clazz, node.getNodeValue());
		}
		catch (IllegalArgumentException e)
		{
			LOG.warn("Invalid value specified for node: {} specified value: {} should be enum value of \"{}\" using default value: {}"+ node.getNodeName()+ node.getNodeValue(), clazz.getSimpleName(), defaultValue);
			return defaultValue;
		}
	}
	
	/**
	 * Parses an enumerated value.
	 * @param <T> the enumerated type
	 * @param node the node to parse
	 * @param clazz the class of the enumerated
	 * @return if the node is not null and the node value is valid the parsed value, otherwise null
	 */
	default <T extends Enum<T>> T parseEnum(Node node, Class<T> clazz)
	{
		return parseEnum(node, clazz, null);
	}
	
	/**
	 * Parses an enumerated value.
	 * @param <T> the enumerated type
	 * @param attrs the attributes
	 * @param clazz the class of the enumerated
	 * @param name the name of the attribute to parse
	 * @return if the node is not null and the node value is valid the parsed value, otherwise null
	 */
	default <T extends Enum<T>> T parseEnum(NamedNodeMap attrs, Class<T> clazz, String name)
	{
		return parseEnum(attrs.getNamedItem(name), clazz);
	}
	
	/**
	 * Parses an enumerated value.
	 * @param <T> the enumerated type
	 * @param attrs the attributes
	 * @param clazz the class of the enumerated
	 * @param name the name of the attribute to parse
	 * @param defaultValue the default value
	 * @return if the node is not null and the node value is valid the parsed value, otherwise the default value
	 */
	default <T extends Enum<T>> T parseEnum(NamedNodeMap attrs, Class<T> clazz, String name, T defaultValue)
	{
		return parseEnum(attrs.getNamedItem(name), clazz, defaultValue);
	}
	
	/**
	 * Gets the current file filter.
	 * @return the current file filter
	 */
	default FileFilter getCurrentFileFilter()
	{
		return XML_FILTER;
	}
	
	/**
	 * Simple XML error handler.
	 * @author Zoey76
	 */
	static class XMLErrorHandler implements ErrorHandler
	{
		@Override
		public void warning(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
		
		@Override
		public void error(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
		
		@Override
		public void fatalError(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
	}
}
