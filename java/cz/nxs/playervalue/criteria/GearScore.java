package cz.nxs.playervalue.criteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map.Entry;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.base.GlobalConfigModel;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class GearScore implements ICriteria
{
	private FastMap<Integer, Integer> scores;
	private FastList<Integer> changed;
	
	public GearScore()
	{
		changed = new FastList<Integer>();
		loadData();
	}
	
	private void loadData()
	{
		FastMap<Integer, Integer> data;
		
		Connection con = null;
		int size = 0;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT itemId, score FROM nexus_playervalue_items");
			ResultSet rset = statement.executeQuery();
			
			data = new FastMap<Integer, Integer>();
			
			int itemId, score;
			while (rset.next())
			{
				itemId = rset.getInt("itemId");
				score = rset.getInt("score");
				data.put(itemId, score);
			}
			
			rset.close();
			statement.close();
			
			// process data
			
			scores = new FastMap<Integer, Integer>();
			FastMap<Integer, Integer> missing = new FastMap<Integer, Integer>();
			
			for(int id : CallBack.getInstance().getOut().getAllArmorsId())
			{
				size ++;
				if(data.containsKey(id))
				{
					scores.put(id, data.get(id));
				}
				else
				{
					int def = getDefaultValue(id);
					scores.put(id, def);
					missing.put(id, def);
				}
			}
			
			for(int id : CallBack.getInstance().getOut().getAllWeaponsId())
			{
				size ++;
				if(data.containsKey(id))
				{
					scores.put(id, data.get(id));
				}
				else
				{
					int def = getDefaultValue(id);
					scores.put(id, def);
					missing.put(id, def);
				}
			}
			
			// add missing data to db
			
			if(!missing.isEmpty())
			{
				TextBuilder tb = new TextBuilder();
				for(Entry<Integer, Integer> e : missing.entrySet())
				{
					tb.append("(" + e.getKey() + "," + e.getValue() + "),");
				}
				
				String values = tb.toString();
				
				statement = con.prepareStatement("INSERT INTO nexus_playervalue_items (itemId,score) VALUES " + values.substring(0, values.length() - 1) + ";");
				statement.execute();
				missing = null;
				
				statement.close();
			}
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
		
		data = null;
		NexusLoader.debug("Nexus Engine: Gear score engine - loaded " + size + " items.");
	}
	
	public void saveAll()
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			TextBuilder tb = new TextBuilder();
			for(int i : changed)
			{
				tb.append("(" + i + "," + getScore(i) + "),");
			}
			
			String values = tb.toString();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_playervalue_items (itemId,score) VALUES " + values.substring(0, values.length() - 1) + ";");
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
	
	public int getScore(int itemId)
	{
		return scores.get(itemId);
	}
	
	public void setScore(int itemId, int value)
	{
		scores.put(itemId, value);
		changed.add(itemId);
	}
	
	public int getDefaultValue(int itemId)
	{
		ItemData item = new ItemData(itemId);
		
		int score = 0;
		
		String configName = "defVal_";
		
		// grade
		if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_NONE())
			configName += "N-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_D())
			configName += "D-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_C())
			configName += "C-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_B())
			configName += "B-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_A())
			configName += "A-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_S())
			configName += "S-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_S80())
			configName += "S80-Grade_";
		else if(item.getCrystalType() == CallBack.getInstance().getValues().CRYSTAL_S84())
			configName += "S84-Grade_";
		
		// body part
		if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_UNDERWEAR())
			configName += "Underwear";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_EAR() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_LR_EAR() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_R_EAR())
			configName += "Earring";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_NECK())
			configName += "Necklace";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_R_FINGER() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_FINGER() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_LR_FINGER())
			configName += "Ring";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_HEAD())
			configName += "Helmet";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_R_HAND() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_LR_HAND())
		{
			if(item.isWeapon())
			{
				if(item.getWeaponType() == null)
					return 0;
				
				String first = item.getWeaponType().toString();
				
				if(first.length() > 1)
				{
					first = first.substring(0, 1);
					String name = item.getWeaponType().toString();
					name = name.substring(1, name.length()).toLowerCase();
					
					configName += (first + name);
				}
				else
				{
					configName += item.getWeaponType().toString();
				}
			}
			else 
				return 0;
		}
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_HAND())
			configName += "Shield";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_GLOVES())
			configName += "Gloves";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_CHEST())
			configName += "Chest";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_LEGS())
			configName += "Gaiters";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_FEET())
			configName += "Boots";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_BACK())
			configName += "Cloak";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_FULL_ARMOR())
			configName += "FullArmor";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_HAIR() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_HAIR2() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_HAIRALL())
			configName += "Hair";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_R_BRACELET() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_BRACELET())
			configName += "Bracelet";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_DECO())
			configName += "Talisman";
		else if(item.getBodyPart() == CallBack.getInstance().getValues().SLOT_BELT())
			configName += "Belt";
		else // formal wear mostly
			return 0;
		
		if(!EventConfig.getInstance().globalConfigExists(configName))
		{
			GlobalConfigModel gc = EventConfig.getInstance().addGlobalConfig("GearScore", configName, "Gear score default value for " + configName + " equippable item type.", "0", 1);
			EventConfig.getInstance().saveGlobalConfig(gc); // this will save it to DB
			
			score = 0;
		}
		else
			score = EventConfig.getInstance().getGlobalConfigInt(configName);
		
		return score;
	}
	
	@Override
	public int getPoints(PlayerEventInfo player)
	{
		int points = 0;
		for(ItemData item : player.getItems())
		{
			//TODO: method param boolean - CHECK ONLY FOR THE BEST ITEMS IN PLAYER's INVENTORY - used when registering to event
			if(!item.isEquipped())
				continue;
			
			if(item.isArmor() || item.isJewellery() || item.isWeapon())
			{
				points += getScore(item.getItemId());
			}
		}
		return points;
	}
	
	public static GearScore getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GearScore _instance = new GearScore();
	}
}
