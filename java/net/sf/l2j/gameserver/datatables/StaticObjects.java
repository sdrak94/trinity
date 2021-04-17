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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;

public class StaticObjects
{
private static Logger _log = Logger.getLogger(StaticObjects.class.getName());

private Map<Integer, L2StaticObjectInstance> _staticObjects;

public static StaticObjects getInstance()
{
	return SingletonHolder._instance;
}

private StaticObjects()
{
	_staticObjects = new FastMap<Integer, L2StaticObjectInstance>();
	parseData();
	_log.config("StaticObject: Loaded " + _staticObjects.size() + " StaticObject Templates.");
}

private void parseData()
{
	LineNumberReader lnr = null;
	try
	{
		File doorData = new File(Config.DATAPACK_ROOT, "data/staticobjects.csv");
		lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
		
		String line = null;
		while ((line = lnr.readLine()) != null)
		{
			if (line.trim().length() == 0 || line.startsWith("#"))
				continue;
			
			L2StaticObjectInstance obj = parse(line);
			_staticObjects.put(obj.getStaticObjectId(), obj);
		}
	}
	catch (FileNotFoundException e)
	{
		_log.warning("staticobjects.csv is missing in data folder");
	}
	catch (Exception e)
	{
		_log.warning("error while creating StaticObjects table " + e);
	}
	finally
	{
		try
		{
			lnr.close();
		}
		catch (Exception e)
		{
		}
	}
}

public static L2StaticObjectInstance parse(String line)
{
	StringTokenizer st = new StringTokenizer(line, ";");
	
	String name = st.nextToken(); //Pass over static object name (not used in server)
	
	int id = Integer.parseInt(st.nextToken());
	int x = Integer.parseInt(st.nextToken());
	int y = Integer.parseInt(st.nextToken());
	int z = Integer.parseInt(st.nextToken());
	int type = Integer.parseInt(st.nextToken());
	String texture = st.nextToken();
	int map_x = Integer.parseInt(st.nextToken());
	int map_y = Integer.parseInt(st.nextToken());
	
	StatsSet npcDat = new StatsSet();
	npcDat.set("name", name);
	npcDat.set("npcId", id);
	npcDat.set("level", 0);
	npcDat.set("jClass", "staticobject");
	
	npcDat.set("baseSTR", 0);
	npcDat.set("baseCON", 0);
	npcDat.set("baseDEX", 0);
	npcDat.set("baseINT", 0);
	npcDat.set("baseWIT", 0);
	npcDat.set("baseMEN", 0);
	
	npcDat.set("baseShldDef", 0);
	npcDat.set("baseShldRate", 0);
	npcDat.set("baseAccCombat", 38);
	npcDat.set("baseEvasRate", 38);
	npcDat.set("baseCritRate", 38);
	
	//npcDat.set("name", "");
	npcDat.set("collision_radius", 10);
	npcDat.set("collision_height", 10);
	npcDat.set("fCollision_radius", 10);
	npcDat.set("fCollision_height", 10);
	npcDat.set("sex", "male");
	npcDat.set("type", "");
	npcDat.set("baseAtkRange", 0);
	npcDat.set("baseMpMax", 0);
	npcDat.set("baseCpMax", 0);
	npcDat.set("rewardExp", 0);
	npcDat.set("rewardSp", 0);
	npcDat.set("basePAtk", 0);
	npcDat.set("baseMAtk", 0);
	npcDat.set("basePAtkSpd", 0);
	npcDat.set("aggroRange", 0);
	npcDat.set("baseMAtkSpd", 0);
	npcDat.set("rhand", 0);
	npcDat.set("lhand", 0);
	npcDat.set("armor", 0);
	npcDat.set("baseWalkSpd", 0);
	npcDat.set("baseRunSpd", 0);
	npcDat.set("name", "");
	npcDat.set("baseHpMax", 1);
	npcDat.set("baseHpReg", 3.e-3f);
	npcDat.set("baseMpReg", 3.e-3f);
	npcDat.set("basePDef", 1);
	npcDat.set("baseMDef", 1);
	
	L2CharTemplate template = new L2CharTemplate(npcDat);
	L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId(), template, id);
	obj.setType(type);
	obj.setXYZ(x, y, z);
	obj.setMap(texture, map_x, map_y);
	obj.spawnMe();
	
	return obj;
}

public void putObject(L2StaticObjectInstance obj)
{
	_staticObjects.put(obj.getStaticObjectId(), obj);
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final StaticObjects _instance = new StaticObjects();
}
}
