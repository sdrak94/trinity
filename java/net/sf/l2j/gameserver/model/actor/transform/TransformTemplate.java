/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.ExBasicActionList;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.util.SkillHolder;

/**
 * @author UnAfraid
 */
public class TransformTemplate
{
	private final float _collisionRadius;
	private final float _collisionHeight;
	private final L2WeaponType _baseAttackType;
	private final int _baseAttackRange;
	private final double _baseRandomDamage;
	private List<SkillHolder> _skills;
	private List<AdditionalSkillHolder> _additionalSkills;
	private List<AdditionalItemHolder> _additionalItems;
	private Map<Integer, Integer> _baseDefense;
	private Map<Integer, Double> _baseStats;
	private Map<Integer, Double> _baseSpeed;
	
	private ExBasicActionList _list;
	private final Map<Integer, TransformLevelData> _data = new LinkedHashMap<>(100);
	
	public TransformTemplate(StatsSet set)
	{
		_collisionRadius = set.getFloat("radius", 0);
		_collisionHeight = set.getFloat("height", 0);
		_baseAttackType = set.getEnum("attackType", L2WeaponType.class, L2WeaponType.FIST);
		_baseAttackRange = set.getInt("range", 40);
		_baseRandomDamage = set.getDouble("randomDamage", 0);
		
		addSpeed(set.getDouble("run", 0));
		
		addStats(Stats.POWER_ATTACK, set.getDouble("pAtk", 0));
		addStats(Stats.MAGIC_ATTACK, set.getDouble("mAtk", 0));
		addStats(Stats.POWER_ATTACK_RANGE, set.getInt("range", 0));
		addStats(Stats.POWER_ATTACK_SPEED, set.getInt("attackSpeed", 0));
		addStats(Stats.CRITICAL_RATE, set.getInt("critRate", 0));
		addStats(Stats.STAT_STR, set.getInt("str", 0));
		addStats(Stats.STAT_INT, set.getInt("int", 0));
		addStats(Stats.STAT_CON, set.getInt("con", 0));
		addStats(Stats.STAT_DEX, set.getInt("dex", 0));
		addStats(Stats.STAT_WIT, set.getInt("wit", 0));
		addStats(Stats.STAT_MEN, set.getInt("men", 0));
		
		addDefense(Inventory.PAPERDOLL_CHEST, set.getInt("chest", 0));
		addDefense(Inventory.PAPERDOLL_LEGS, set.getInt("legs", 0));
		addDefense(Inventory.PAPERDOLL_HEAD, set.getInt("head", 0));
		addDefense(Inventory.PAPERDOLL_FEET, set.getInt("feet", 0));
		addDefense(Inventory.PAPERDOLL_GLOVES, set.getInt("gloves", 0));
		addDefense(Inventory.PAPERDOLL_UNDER, set.getInt("underwear", 0));
		addDefense(Inventory.PAPERDOLL_BACK, set.getInt("cloak", 0));
		addDefense(Inventory.PAPERDOLL_REAR, set.getInt("rear", 0));
		addDefense(Inventory.PAPERDOLL_LEAR, set.getInt("lear", 0));
		addDefense(Inventory.PAPERDOLL_RFINGER, set.getInt("rfinger", 0));
		addDefense(Inventory.PAPERDOLL_LFINGER, set.getInt("lfinger", 0));
		addDefense(Inventory.PAPERDOLL_NECK, set.getInt("neck", 0));
	}
	
	private void addSpeed(double value)
	{
		if (_baseSpeed == null)
		{
			_baseSpeed = new HashMap<>();
		}
		_baseSpeed.put(1, value);
	}
	
	private void addDefense(int type, int value)
	{
		if (_baseDefense == null)
		{
			_baseDefense = new HashMap<>();
		}
		_baseDefense.put(type, value);
	}
	
	public int getDefense(int type)
	{
		if ((_baseDefense == null) || !_baseDefense.containsKey(type))
		{
			return 0;
		}
		return _baseDefense.get(type);
	}
	
	private void addStats(Stats stat, double value)
	{
		if (_baseStats == null)
		{
			_baseStats = new HashMap<>();
		}
		_baseStats.put(stat.ordinal(), value);
	}
	
	public double getStats(Stats stat)
	{
		if ((_baseStats == null) || !_baseStats.containsKey(stat.ordinal()))
		{
			return 0;
		}
		return _baseStats.get(stat.ordinal());
	}
	
	public float getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	public float getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	public L2WeaponType getBaseAttackType()
	{
		return _baseAttackType;
	}
	
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	public double getBaseRandomDamage()
	{
		return _baseRandomDamage;
	}
	
	public void addSkill(SkillHolder holder)
	{
		if (_skills == null)
		{
			_skills = new ArrayList<>();
		}
		_skills.add(holder);
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills != null ? _skills : Collections.<SkillHolder> emptyList();
	}
	
	public void addAdditionalSkill(AdditionalSkillHolder holder)
	{
		if (_additionalSkills == null)
		{
			_additionalSkills = new ArrayList<>();
		}
		_additionalSkills.add(holder);
	}
	
	public List<AdditionalSkillHolder> getAdditionalSkills()
	{
		return _additionalSkills != null ? _additionalSkills : Collections.<AdditionalSkillHolder> emptyList();
	}
	
	public void addAdditionalItem(AdditionalItemHolder holder)
	{
		if (_additionalItems == null)
		{
			_additionalItems = new ArrayList<>();
		}
		_additionalItems.add(holder);
	}
	
	public List<AdditionalItemHolder> getAdditionalItems()
	{
		return _additionalItems != null ? _additionalItems : Collections.<AdditionalItemHolder> emptyList();
	}
	
	public void setBasicActionList(ExBasicActionList list)
	{
		_list = list;
	}
	
	public ExBasicActionList getBasicActionList()
	{
		return _list;
	}
	
	public boolean hasBasicActionList()
	{
		return _list != null;
	}
	
	public void addLevelData(TransformLevelData data)
	{
		_data.put(data.getLevel(), data);
	}
	
	public TransformLevelData getData(int level)
	{
		return _data.get(level);
	}
}
