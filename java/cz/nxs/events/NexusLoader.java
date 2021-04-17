package cz.nxs.events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;

import cz.nxs.debug.DebugConsole;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.EventRewardSystem;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.OldStats;
import cz.nxs.events.engine.main.base.MainEventInstanceTypeManager;
import cz.nxs.events.engine.stats.EventStatsManager;
import cz.nxs.interf.NexusEvents;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import cz.nxs.playervalue.PlayerValueEngine;
import javolution.text.TextBuilder;
import javolution.util.FastSet;

/**
 * @author hNoke
 * loads the engine.
 */
public class NexusLoader
{
	public static final String version = "3.0";
	
	private static FileWriter fileWriter;
	private static final SimpleDateFormat _toFileFormat = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
	
	public enum NexusBranch
	{
		Freya(2.1),
		Hi5(2.1),
		Hi5Priv(2.1),
		Final(2.1);
		//InterludeL2jFrozen(1.5, "L2J-Interlude L2jFrozen", false, "./lib/", "config/nexus_serial.txt", true);
		
		public double _newestVersion;
		NexusBranch(double interfaceVersion)
		{
			_newestVersion = interfaceVersion;
		}
	}
	
	public static boolean debugConsole = false;
	public static boolean detailedDebug = false;
	public static boolean detailedDebugToConsole = false;
	public static boolean logToFile = false;
	
	//private static final SimpleDateFormat _formatter = new SimpleDateFormat("H:mm:ss");
	
	public static DebugConsole debug;
	private static NexusBranch _branch;
	private static String _desc;
	private static String _serialPath;
	private static double _interfaceVersion;
	private static String _key;
	
	private static boolean loaded = false;
	private static boolean loading = false;
	private static boolean tryReconnect = false;
	private static boolean _instances;
	private static String _libsFolder;
	private static boolean _limitedHtml;
	
	public static void init(NexusBranch l2branch, double interfaceVersion, String desc, boolean allowInstances, String libsFolder, String serialPath, boolean limitedHtml)
	{
		if(_branch == null)
		{
			_branch = l2branch;
			_interfaceVersion = interfaceVersion;
			_serialPath = serialPath;
		}
		
		if(_key == null)
		{
			String key = null;
			
			InputStream is;
			InputStreamReader reader = null;
			BufferedReader bReader = null;
			
			try
			{
				is = new FileInputStream(new File(_serialPath));
				try
				{
					reader = new InputStreamReader(is, Charset.defaultCharset());
					bReader = new BufferedReader(reader);
					
					String line;
					while((line = bReader.readLine()) != null)
					{
						key = line;
						break;
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						is.close();
						if (reader != null)
							reader.close();
						if(bReader != null)
							bReader.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			
			_key = key;
			
			debug("License key: " + (_key == null ? "-" : _key));
		}
		
		if(_key != null && isValid(_key))
		{
			loading = true;
			
			EventConfig.getInstance().loadGlobalConfigs();
			
			debugConsole = EventConfig.getInstance().getGlobalConfigBoolean("debug");
			
			if(!java.awt.GraphicsEnvironment.isHeadless())
				debugConsole = false;
			
			if(debugConsole)
				loadDebugConsole(true);
			
			String fileName = createDebugFile();
			if(fileName != null)
				debug("Nexus Engine: Debug messages are stored in '" + fileName + "'");
			
			debug("Nexus Engine: Thanks for using a legal version of the engine.");
			
			_desc = desc;
			_instances = allowInstances;
			_libsFolder = libsFolder;
			
			_limitedHtml = limitedHtml;
			
			debug("Nexus Engine: Loading engine version " + version + "...");
			debug("Nexus Engine: Using " + _desc + " interface (for engine of v" + interfaceVersion + ").");
			
			if(interfaceVersion != l2branch._newestVersion)
				debug("Nexus Engine: Your interface is outdated for this engine!!! Please update it.", Level.SEVERE);
			
			OldStats.getInstance();
			
			NexusEvents.loadHtmlManager();
			
			logToFile = EventConfig.getInstance().getGlobalConfigBoolean("logToFile");
			detailedDebug = EventConfig.getInstance().getGlobalConfigBoolean("detailedDebug");
			detailedDebugToConsole = EventConfig.getInstance().getGlobalConfigBoolean("detailedDebugToConsole");
			
			LanguageEngine.init();
			
			EventManager.getInstance();
			EventConfig.getInstance().loadEventConfigs();
			
			EventMapSystem.getInstance().loadMaps();
			EventRewardSystem.getInstance();
			
			EventManager.getInstance().getMainEventManager().loadScheduleData();
			
			MainEventInstanceTypeManager.getInstance();
			
			EventStatsManager.getInstance();
			
			EventWarnings.getInstance();
			
			PlayerValueEngine.getInstance();
			
			loaded = true;
			debug("Nexus Engine: Version " + version + " successfully loaded.");
		}
		else
		{
			loaded = false;
			scheduleTryReconnect();
		}
	}
	
	private static boolean isValid(String key)
	{
		return true;
	}
	
	private static void scheduleTryReconnect()
	{
		if(!loaded && tryReconnect)
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					tryReconnect();
				}
			}, 30000);
		}
	}
	
	protected static void tryReconnect()
	{
		if(loaded || !tryReconnect)
			return;
		
		debug("Reconnecting to the auth server...");
		init(_branch, _interfaceVersion, _desc, _instances, _libsFolder, _serialPath, _limitedHtml);
	}
	
	public static void loadDebugConsole(boolean onServerStart)
	{
		if(!java.awt.GraphicsEnvironment.isHeadless())
		{
            DebugConsole.initGui();
            DebugConsole.info("Nexus Engine: Debug console initialized.");
		}
		else  if(!onServerStart) System.out.println("Debug console can't be opened in this environment.");
	}
	
	private static boolean _gmsDebugging = false;
	private static Set<PlayerEventInfo> _gmsDebuggingSet = new FastSet<PlayerEventInfo>();
	
	public static boolean isDebugging(PlayerEventInfo gm)
	{
		if(!_gmsDebugging)
			return false;
		return _gmsDebuggingSet.contains(gm);
	}
	
	public static void addGmDebug(PlayerEventInfo gm)
	{
		if(!_gmsDebugging)
			_gmsDebugging = true;
		
		_gmsDebuggingSet.add(gm);
	}
	
	public static void removeGmDebug(PlayerEventInfo gm)
	{
		if(!_gmsDebugging)
			return;
		
		_gmsDebuggingSet.remove(gm);
		
		if(_gmsDebuggingSet.isEmpty())
			_gmsDebugging = false;
	}
	
	public static void debug(String msg, Level level)
	{
		if(!msg.startsWith("Nexus ") && !msg.startsWith("nexus"))
			msg = "Nexus Engine: " + msg;
		
		if(debugConsole)
			DebugConsole.log(level, msg);
		else
			System.out.println(msg);

		if(_gmsDebugging)
		{
			sendToGms(msg, level, false);
		}
		
		writeToFile(level, msg, false);
	}
	
	public static void debug(String msg)
	{
		if(!msg.startsWith("Nexus ") && !msg.startsWith("nexus"))
			msg = "Nexus Engine: " + msg;
			
		try
		{
			if(debugConsole)
				DebugConsole.info(msg);
			else
				System.out.println(msg);
		}
		catch (Exception e)
		{
		}
		
		try
		{
			if(_gmsDebugging)
			{
				sendToGms(msg, Level.INFO, false);
			}
		}
		catch (Exception e)
		{
		}
		
		writeToFile(Level.INFO, msg, false);
	}
	
	public static int DEBUG_CHAT_CHANNEL_CLASSIC = 7;
	public static int DEBUG_CHAT_CHANNEL = 6;
	
	public static void sendToGms(String msg, Level level, boolean detailed)
	{
		try
		{
			for(PlayerEventInfo gm : _gmsDebuggingSet)
			{
				gm.creatureSay("*" + (detailed ? msg : msg.substring(14)) + "  (" + level.toString() + ")", (detailed ? "DD" : "DEBUG"), detailed ? DEBUG_CHAT_CHANNEL : DEBUG_CHAT_CHANNEL_CLASSIC);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void detailedDebug(String msg)
	{
		if(!msg.startsWith("DD "))
			msg = "DD:  " + msg;
		
		try
		{
			if(_gmsDebugging)
			{
				sendToGms(msg, Level.INFO, true);
			}
		}
		catch (Exception e)
		{
		}
		
		try
		{
			if(detailedDebugToConsole && debugConsole)
				DebugConsole.log(Level.INFO, msg);
		}
		catch (Exception e)
		{
		}
		
		writeToFile(Level.INFO, msg, true);
	}
	
	public static boolean allowInstances()
	{
		return _instances;
	}
	
	public static String getLibsFolderName()
	{
		return _libsFolder;
	}
	
	public static boolean isLimitedHtml()
	{
		return _limitedHtml;
	}
	
	private static File debugFile;
	private static File detailedDebugFile;
	
	private static String createDebugFile()
	{
		String path = "log/nexus";
		
		File folder = new File(path);
		if(!folder.exists())
		{
			if(!folder.mkdir())
				path = "log";
		}
		
		debugFile = new File(path + "/NexusEvents.log");
		if(!debugFile.exists())
		{
			try
			{
				debugFile.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		int id = 0;
		for(File f : folder.listFiles())
		{
			if(f.getName().startsWith("NexusEvents_detailed"))
			{
				try
				{
					String name = f.getName().substring(0, f.getName().length() - 4);
					int id2 = Integer.getInteger(name.substring(21));
					if(id2 > id)
						id = id2;
				}
				catch (Exception e)
				{
				}
			}
		}
		
		id += 1;
		
		detailedDebugFile = new File(path + "/NexusEvents_detailed_" + (id) + ".log");

		if(detailedDebugFile.exists())
		{
			try
			{
				detailedDebugFile.delete();	
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
			
		if(!detailedDebugFile.exists())
		{
			try
			{
				detailedDebugFile.createNewFile();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return detailedDebugFile.getAbsolutePath();
		//return path + "/NexusEvents_detailed_" + (id) + ".log";
	}
	
	public static void writeToFile(Level level, String msg, boolean detailed)
	{
		if(detailed)
		{
			//if(!detailedDebug) // already handled
			//	return;
		}
		else
		{
			if(!logToFile)
				return;
		}
		
		try 
		{
			if(!detailed)
				fileWriter = new FileWriter(debugFile, true);
			else
				fileWriter = new FileWriter(detailedDebugFile, true);
			
			fileWriter.write(_toFileFormat.format(new Date()) + ":  " + msg + " (" + level.getLocalizedName() + ")\r\n");
		} 
		catch (Exception e) 
		{
			if(debugConsole)
				DebugConsole.log(Level.WARNING, "Error writing debug msgs to file: " + e.toString());
		} 
		finally 
		{
			try 
			{
				fileWriter.close();
			} 
			catch (Exception e) { }
		}
	}
	
	public static String getTraceString(StackTraceElement[] trace)
	{
		final TextBuilder sbString = TextBuilder.newInstance();
		for (final StackTraceElement element : trace)
		{
			sbString.append(element.toString()).append("\n");
		}
		
		String result = sbString.toString();
		TextBuilder.recycle(sbString);
		return result;
	}
	
	public static void shutdown()
	{
		EventWarnings.getInstance().saveData();
	}
	
	public static boolean loaded()
	{
		return loaded;
	}
	
	public static boolean loadedOrBeingLoaded()
	{
		return loading;
	}
}
