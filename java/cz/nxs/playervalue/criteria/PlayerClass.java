package cz.nxs.playervalue.criteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class PlayerClass implements ICriteria
{
	private FastMap<Integer, Integer> scores;
	private FastList<Integer> changed;
	
	private final Integer[] classes;
	
	public PlayerClass()
	{
		classes = CallBack.getInstance().getOut().getAllClassIds();
		loadData();
	}
	
	private void loadData()
	{
		scores = new FastMap<Integer, Integer>();
		changed = new FastList<Integer>();
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT classId, score FROM nexus_playervalue_classes");
			ResultSet rset = statement.executeQuery();
			
			int classId, score;
			while (rset.next())
			{
				classId = rset.getInt("classId");
				score = rset.getInt("score");
				scores.put(classId, score);
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
		
		for(Integer i : classes)
		{
			if(!scores.containsKey(i.intValue()))
			{
				changed.add(i.intValue());
				scores.put(i.intValue(), 0);
			}
		}
		
		save();
	}
	
	private void save()
	{
		if(changed.isEmpty())
			return;
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			TextBuilder tb = new TextBuilder();
			for(int i : changed)
			{
				tb.append("(" + i + "," + scores.get(i) + "),");
			}
			
			String values = tb.toString();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_playervalue_classes VALUES " + values.substring(0, values.length() - 1) + ";");
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
		
		changed.clear();
	}
	
	public int getScore(int classId)
	{
		if(scores.containsKey(classId))
			return scores.get(classId);
		else
		{
			NexusLoader.debug("PlayerValue engine: Class ID " + classId + " has no value setted up.", Level.WARNING);
			return 0;
		}
	}
	
	public void setValue(int classId, int value)
	{
		scores.put(classId, value);
		changed.add(classId);
	}
	
	@Override
	public int getPoints(PlayerEventInfo player)
	{
		int playerClass = player.getActiveClass();
		return getScore(playerClass);
	}
	
	public static PlayerClass getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PlayerClass _instance = new PlayerClass();
	}
}
