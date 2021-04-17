package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import gnu.trove.map.hash.TIntObjectHashMap;
import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Skill;

/**
 * Warning: must be loaded after loading SkillTable
 *
 * @author  DrHouse
 */
public class ResidentialSkillTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());

	private static ResidentialSkillTable _instance = null;
	private static TIntObjectHashMap<FastList<L2Skill>> _list;

	ResidentialSkillTable()
	{
		load();
	}

	private void load()
	{
		_list = new TIntObjectHashMap<FastList<L2Skill>>();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM skill_residential ORDER BY entityId");
			ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				int entityId = rs.getInt("entityId");
				int skillId = rs.getInt("skillId");
				int skillLvl = rs.getInt("skillLevel");

				L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLvl);

				if (sk == null)
				{
					_log.warning("ResidentialSkillTable: SkillTable has returned null for ID/level: " +skillId+"/"+skillLvl);
					continue;
				}
				if (!_list.containsKey(entityId))
				{
					FastList<L2Skill> aux = new FastList<L2Skill>();
					aux.add(sk);
					_list.put(entityId, aux);
				}
				else
					_list.get(entityId).add(sk);
			}
			statement.close();
			rs.close();
		}
		catch (Exception e)
		{
			_log.warning("ResidentialSkillTable: a problem occured while loading skills!");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{

			}
			_log.info("ResidentialSkillTable: Loaded " + _list.size() + " entities with associated skills.");
		}
	}

	public FastList<L2Skill> getSkills(int entityId)
	{
		if (_list.containsKey(entityId))
			return _list.get(entityId);

		return null;
	}

	public static ResidentialSkillTable getInstance()
	{
		if (_instance == null) 
			_instance = new ResidentialSkillTable();

		return _instance;
	}
}