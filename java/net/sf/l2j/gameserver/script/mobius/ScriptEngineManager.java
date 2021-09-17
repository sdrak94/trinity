package net.sf.l2j.gameserver.script.mobius;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import net.sf.l2j.Config;
import net.sf.l2j.util.IXmlReader;

public class ScriptEngineManager implements IXmlReader
{
	private static final Logger					LOGGER						= Logger.getLogger(ScriptEngineManager.class.getName());
	public static final Path					SCRIPT_FOLDER				= Config.SCRIPT_ROOT.toPath();
	public static final Path					MASTER_HANDLER_FILE			= Paths.get(SCRIPT_FOLDER.toString(), "handlers", "MasterHandler.java");
	public static final Path					EFFECT_MASTER_HANDLER_FILE	= Paths.get(SCRIPT_FOLDER.toString(), "handlers", "EffectMasterHandler.java");
	private static final JavaExecutionContext	_javaExecutionContext		= new JavaScriptingEngine().createExecutionContext();
	protected static final List<String>			_exclusions					= new ArrayList<>();
	
	protected ScriptEngineManager()
	{
		// Load Scripts.xml
		load();
	}
	
	@Override
	public void load()
	{
		_exclusions.clear();
		parseDatapackFile("config/Scripts.xml");
		LOGGER.info("Loaded " + _exclusions.size() + " files to exclude.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		try
		{
			final Map<String, List<String>> excludePaths = new HashMap<>();
			forEach(doc, "list", listNode -> forEach(listNode, "exclude", excludeNode ->
			{
				final String excludeFile = parseString(excludeNode.getAttributes(), "file");
				excludePaths.putIfAbsent(excludeFile, new ArrayList<>());
				forEach(excludeNode, "include", includeNode -> excludePaths.get(excludeFile).add(parseString(includeNode.getAttributes(), "file")));
			}));
			final int nameCount = SCRIPT_FOLDER.getNameCount();
			Files.walkFileTree(SCRIPT_FOLDER, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					final String fileName = file.getFileName().toString();
					if (fileName.endsWith(".java"))
					{
						final Iterator<Path> relativePath = file.subpath(nameCount, file.getNameCount()).iterator();
						while (relativePath.hasNext())
						{
							final String nextPart = relativePath.next().toString();
							if (excludePaths.containsKey(nextPart))
							{
								boolean excludeScript = true;
								final List<String> includePath = excludePaths.get(nextPart);
								if (includePath != null)
								{
									while (relativePath.hasNext())
									{
										if (includePath.contains(relativePath.next().toString()))
										{
											excludeScript = false;
											break;
										}
									}
								}
								if (excludeScript)
								{
									_exclusions.add(file.toUri().getPath());
									break;
								}
							}
						}
					}
					return super.visitFile(file, attrs);
				}
			});
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Couldn't load script exclusions.", e);
		}
	}
	
	private void processDirectory(File dir, List<Path> files)
	{
		for (File file : dir.listFiles())
		{
			if (file.isFile())
			{
				final String filePath = file.toURI().getPath();
				if (filePath.endsWith(".java") && !_exclusions.contains(filePath))
				{
					files.add(file.toPath().toAbsolutePath());
				}
			}
			else if (file.isDirectory())
			{
				processDirectory(file, files);
			}
		}
	}
	
	public void executeScript(Path sourceFiles) throws Exception
	{
		Path path = sourceFiles;
		if (!path.isAbsolute())
		{
			path = SCRIPT_FOLDER.resolve(path);
		}
		path = path.toAbsolutePath();
		final Entry<Path, Throwable> error = _javaExecutionContext.executeScript(path);
		if (error != null)
		{
			throw new Exception("ScriptEngine: " + error.getKey() + " failed execution!", error.getValue());
		}
	}
	
	public void executeScriptList() throws Exception
	{
		if (Config.ALT_DEV_NO_QUESTS)
		{
			return;
		}
		final List<Path> files = new ArrayList<>();
		processDirectory(SCRIPT_FOLDER.toFile(), files);
		final Map<Path, Throwable> invokationErrors = _javaExecutionContext.executeScripts(files);
		for (Entry<Path, Throwable> entry : invokationErrors.entrySet())
		{
			LOGGER.log(Level.WARNING, "ScriptEngine: " + entry.getKey() + " failed execution!", entry.getValue());
		}
	}
	
	public Path getCurrentLoadingScript()
	{
		return _javaExecutionContext.getCurrentExecutingScript();
	}
	
	public static ScriptEngineManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptEngineManager INSTANCE = new ScriptEngineManager();
	}
}