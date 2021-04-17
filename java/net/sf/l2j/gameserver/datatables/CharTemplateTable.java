/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.1.2.10 $ $Date: 2005/03/29 14:00:54 $
 */
public class CharTemplateTable
{
private static final Logger LOG = Logger.getLogger(CharTemplateTable.class.getName());

private final Map<Integer, L2PcTemplate> _templates = new FastMap<Integer, L2PcTemplate>();

public static CharTemplateTable getInstance()
{
	return SingletonHolder._instance;
}

private CharTemplateTable()
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT * FROM class_list, char_templates, lvlupgain"
				+ " WHERE class_list.id = char_templates.classId" + " AND class_list.id = lvlupgain.classId"
				+ " ORDER BY class_list.id");
		ResultSet rset = statement.executeQuery();
		
		while (rset.next())
		{
			StatsSet set = new StatsSet();
			set.set("classId", rset.getInt("id"));
			set.set("className", rset.getString("className"));
			set.set("raceId", rset.getInt("raceId"));
			set.set("baseSTR", rset.getInt("STR"));
			set.set("baseCON", rset.getInt("CON"));
			set.set("baseDEX", rset.getInt("DEX"));
			set.set("baseINT", rset.getInt("_INT"));
			set.set("baseWIT", rset.getInt("WIT"));
			set.set("baseMEN", rset.getInt("MEN"));
			set.set("baseHpMax", rset.getFloat("defaultHpBase"));
			set.set("lvlHpAdd", rset.getFloat("defaultHpAdd"));
			set.set("lvlHpMod", rset.getFloat("defaultHpMod"));
			set.set("baseMpMax", rset.getFloat("defaultMpBase"));
			set.set("baseCpMax", rset.getFloat("defaultCpBase"));
			set.set("lvlCpAdd", rset.getFloat("defaultCpAdd"));
			set.set("lvlCpMod", rset.getFloat("defaultCpMod"));
			set.set("lvlMpAdd", rset.getFloat("defaultMpAdd"));
			set.set("lvlMpMod", rset.getFloat("defaultMpMod"));
			set.set("baseHpReg", 1.5);
			set.set("baseMpReg", 40);
			set.set("basePAtk", rset.getInt("p_atk"));
			set.set("basePDef", /*classId.isMage()? 77 : 129*/rset.getInt("p_def"));
			set.set("baseMAtk", rset.getInt("m_atk"));
			set.set("baseMDef", rset.getInt("char_templates.m_def"));
			set.set("classBaseLevel", rset.getInt("class_lvl"));
			set.set("basePAtkSpd", rset.getInt("p_spd"));
			set.set("baseMAtkSpd", /*classId.isMage()? 166 : 333*/rset.getInt("char_templates.m_spd"));
			set.set("baseCritRate", rset.getInt("char_templates.critical") / 10);
			set.set("baseRunSpd", rset.getInt("move_spd"));
			set.set("baseWalkSpd", 0);
			set.set("baseShldDef", 0);
			set.set("baseShldRate", 0);
			set.set("baseAtkRange", 40);
			
			set.set("spawnX", rset.getInt("x"));
			set.set("spawnY", rset.getInt("y"));
			set.set("spawnZ", rset.getInt("z"));
			
			L2PcTemplate ct;
			
			set.set("collision_radius", rset.getFloat("m_col_r"));
			set.set("collision_height", rset.getFloat("m_col_h"));
			// Add-on for females
			set.set("fcollision_radius", rset.getFloat("f_col_r"));
			set.set("fcollision_height", rset.getFloat("f_col_h"));
			ct = new L2PcTemplate(set);
			
			_templates.put(ct.classId.getId(), ct);
		}
		
		rset.close();
		statement.close();
		
		LOG.info("CharTemplateTable: Loaded " + _templates.size() + " Character Templates.");
	}
	catch (SQLException e)
	{
		LOG.log(Level.SEVERE, "Failed loading char templates", e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
			// nothing
		}
	}
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT classId, itemId, amount, equipped FROM char_creation_items");
		ResultSet rset = statement.executeQuery();
		
		int classId, itemId, amount;
		boolean equipped;
		while (rset.next())
		{
			classId = rset.getInt("classId");
			itemId = rset.getInt("itemId");
			amount = rset.getInt("amount");
			equipped = rset.getString("equipped").equals("true");
			
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				if (classId == -1)
				{
					for (L2PcTemplate pct : _templates.values())
					{
						pct.addItem(itemId, amount, equipped);
					}
				}
				else
				{
					L2PcTemplate pct = _templates.get(classId);
					if (pct != null)
					{
						pct.addItem(itemId, amount, equipped);
					}
					else
					{
						LOG.warning("char_creation_items: Entry for undefined class, classId: " + classId);
					}
				}
			}
			else
			{
				LOG.warning("char_creation_items: No data for itemId: " + itemId + " defined for classId " + classId);
			}
		}
	}
	catch (SQLException e)
	{
		LOG.log(Level.SEVERE, "Failed loading char creation items.", e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
			// nothing
		}
	}
}

public L2PcTemplate getTemplate(ClassId classId)
{
	return this.getTemplate(classId.getId());
}

public L2PcTemplate getTemplate(int classId)
{
	return _templates.get(classId);
}

public final String getClassNameById(int classId)
{
	L2PcTemplate pcTemplate = _templates.get(classId);
	if (pcTemplate == null)
	{
		throw new IllegalArgumentException("No template for classId: " + classId);
	}
	return pcTemplate.className;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final CharTemplateTable _instance = new CharTemplateTable();
}
}
