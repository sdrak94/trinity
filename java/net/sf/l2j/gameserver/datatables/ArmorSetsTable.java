package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ArmorSet;

public class ArmorSetsTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());
	
	private final TIntObjectHashMap<L2ArmorSet> _armorSets;
	
	public static ArmorSetsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ArmorSetsTable()
	{
		_armorSets = new TIntObjectHashMap<L2ArmorSet>();
		loadData();
	}
	
	private void loadData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest FROM armorsets");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int chest = rset.getInt("chest");
				int legs = rset.getInt("legs");
				int head = rset.getInt("head");
				int gloves = rset.getInt("gloves");
				int feet = rset.getInt("feet");
				String[] skills = rset.getString("skill").split(";");
				int shield = rset.getInt("shield");
				int shield_skill_id = rset.getInt("shield_skill_id");
				int enchant16skill = rset.getInt("enchant6skill");
				int mw_legs = rset.getInt("mw_legs");
				int mw_head = rset.getInt("mw_head");
				int mw_gloves = rset.getInt("mw_gloves");
				int mw_feet = rset.getInt("mw_feet");
				int mw_shield = rset.getInt("mw_shield");
				int mw_chest = rset.getInt("mw_chest");
				_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant16skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest));
				_armorSets.put(mw_chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant16skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest));
			}
			
			_log.config("ArmorSetsTable: Loaded " + _armorSets.size() + " armor sets.");
			
			rset.close();
		}
		catch (Exception e)
		{
			_log.severe("ArmorSetsTable: Error reading ArmorSets table: " + e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		if (Config.CUSTOM_ARMORSETS_TABLE)
		{
			try
			{
				int cSets = _armorSets.size();
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest FROM custom_armorsets");
				ResultSet rset = statement.executeQuery();
				while (rset.next())
				{
					int chest = rset.getInt("chest");
					int legs = rset.getInt("legs");
					int head = rset.getInt("head");
					int gloves = rset.getInt("gloves");
					int feet = rset.getInt("feet");
					String[] skills = rset.getString("skill").split(";");
					int shield = rset.getInt("shield");
					int shield_skill_id = rset.getInt("shield_skill_id");
					int enchant16skill = rset.getInt("enchant6skill");
					int mw_legs = rset.getInt("mw_legs");
					int mw_head = rset.getInt("mw_head");
					int mw_gloves = rset.getInt("mw_gloves");
					int mw_feet = rset.getInt("mw_feet");
					int mw_shield = rset.getInt("mw_shield");
					int mw_chest = rset.getInt("mw_chest");
					_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant16skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest));
					_armorSets.put(mw_chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant16skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield, mw_chest));
				}
				
				_log.config("ArmorSetsTable: Loaded " + (_armorSets.size() - cSets) + " Custom armor sets.");
				rset.close();
			}
			catch (Exception e)
			{
				_log.severe("ArmorSetsTable: Error reading Custom ArmorSets table: " + e);
			}
			finally
			{
				try
				{
					statement.close();
				}
				catch (Exception e)
				{
				}
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
	}
	
/*	public boolean setExists(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}*/
	
	public L2ArmorSet getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ArmorSetsTable _instance = new ArmorSetsTable();
	}

	public boolean isArmorSet(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}
}
