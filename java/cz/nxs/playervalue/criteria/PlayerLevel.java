package cz.nxs.playervalue.criteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class PlayerLevel implements ICriteria
{
	private Map<Integer, Integer> _levels;
	
	public PlayerLevel()
	{
		loadData();
	}
	
	private void loadData()
	{
		_levels = new FastMap<Integer, Integer>();
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT level, score FROM nexus_playervalue_levels");
			ResultSet rset = statement.executeQuery();
			
			int level, score;
			while (rset.next())
			{
				level = rset.getInt("level");
				score = rset.getInt("score");
				
				_levels.put(level, score);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			// turn off this system
			EventConfig.getInstance().getGlobalConfig("GearScore", "enableGearScore").setValue("false");
		}
		finally
		{
			try
			{ con.close(); }
			catch (Exception e) {}
		}
		
		// database is empty - server started up for the first time?
		if(_levels.isEmpty())
		{
			recalculate1(10, 10);
			saveToDb(_levels);
			return;
		}
		
		Map<Integer, Integer> missing = new FastMap<Integer, Integer>();
		for(int i = 1; i <= 85; i++)
		{
			if(!_levels.containsKey(i)) 
			{
				missing.put(i, 0);
				NexusLoader.debug("PlayerValue engine - PlayerLevel criteria - in table 'nexus_playervalue_levels' was missing record for level " + i + ". The engine will try to add it back with value 0, but you might need to correct it." , Level.SEVERE);
			}
		}
		
		if(!missing.isEmpty())
			saveToDb(missing);
	}
	
	public void saveToDb(Map<Integer, Integer> levels)
	{
		if(levels.isEmpty())
			return;
			
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			TextBuilder tb = new TextBuilder();
			for(Entry<Integer, Integer> i : levels.entrySet())
			{
				tb.append("(" + i.getKey() + "," + i.getValue() + "),");
			}
			
			String values = tb.toString();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_playervalue_levels VALUES " + values.substring(0, values.length() - 1) + ";");
			statement.execute();
			
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{ con.close(); }
			catch (Exception e) {}
		}
	}
	
	/** 'score = firstValue + (levelCoefficient * playersLevel - 1)' formula 
	 * firstValue = value for level 1
	 */
	private void recalculate1(int firstValue, int levelCoefficient)
	{
		int value = firstValue;
		
		for(int level = 1; level <= 85; level ++)
		{
			value += levelCoefficient;
			_levels.put(level, value);
		}
	}
	
	/** 'score = (playersLevel - startLevel) * levelCoefficient; if finalValue is lower than 0, than finalValue = 0' formula 
	 * (may be useful for high rate servers, where start level is higher) 
	 */
	@SuppressWarnings("unused")
	private void recalculate2(int startLevel, int levelCoefficient)
	{
		int value;
		
		for(int level = 1; level <= 85; level ++)
		{
			value = level - startLevel;
			value *= levelCoefficient;
			
			if(value < 0)
				value = 0;
			
			_levels.put(level, value);
		}
	}
	
	@Override
	public int getPoints(PlayerEventInfo player)
	{
		if(_levels.containsKey(player.getLevel()))
			return _levels.get(player.getLevel());
		else
			return 0;
	}

	public static PlayerLevel getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PlayerLevel _instance = new PlayerLevel();
	}
}
