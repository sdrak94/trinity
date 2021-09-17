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
package net.sf.l2j.gameserver.model.zone;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Abstract base class for any zone type
 * Handles basic operations
 *
 * @author  durgus
 */
public abstract class L2ZoneType
{
protected static final Logger _log = Logger.getLogger(L2ZoneType.class.getName());

protected final int _id;
protected List<L2ZoneForm> _zone;
protected FastMap<Integer, L2Character> _characterList;
protected FastMap<Integer, Integer> _zones;

/** Parameters to affect specific characters */
private boolean _checkAffected;

private int _minLvl;
private int _maxLvl;
private int[] _race;
private int[] _class;
private char _classType;
private Map<QuestEventType, FastList<Quest>> _questEvents;
public boolean _allowShops = true;

protected L2ZoneType(int id)
{
	_id = id;
	_characterList = new FastMap<Integer, L2Character>().shared();
	_zones = new FastMap<Integer, Integer>().shared();
	
	_checkAffected = false;
	
	_minLvl = 0;
	_maxLvl = 0xFF;
	
	_classType = 0;
	
	_race = null;
	_class = null;
}

/**
 * @return Returns the id.
 */
public int getId()
{
	return _id;
}

/**
 * Setup new parameters for this zone
 * @param type
 * @param value
 */
public void setParameter(String name, String value)
{
	_checkAffected = true;
	
	// Minimum level
	if (name.equals("affectedLvlMin"))
	{
		_minLvl = Integer.parseInt(value);
	}
	// Maximum level
	else if (name.equals("affectedLvlMax"))
	{
		_maxLvl = Integer.parseInt(value);
	}
	// Affected Races
	else if (name.equals("affectedRace"))
	{
		// Create a new array holding the affected race
		if (_race == null)
		{
			_race = new int[1];
			_race[0] = Integer.parseInt(value);
		}
		else
		{
			int[] temp = new int[_race.length + 1];
			
			int i = 0;
			for (; i < _race.length; i++)
				temp[i] = _race[i];
			
			temp[i] = Integer.parseInt(value);
			
			_race = temp;
		}
	}
	// Affected classes
	else if (name.equals("affectedClassId"))
	{
		// Create a new array holding the affected classIds
		if (_class == null)
		{
			_class = new int[1];
			_class[0] = Integer.parseInt(value);
		}
		else
		{
			int[] temp = new int[_class.length + 1];
			
			int i = 0;
			for (; i < _class.length; i++)
				temp[i] = _class[i];
			
			temp[i] = Integer.parseInt(value);
			
			_class = temp;
		}
	}
	// Affected class type
	else if (name.equals("affectedClassType"))
	{
		if (value.equals("Fighter"))
		{
			_classType = 1;
		}
		else
		{
			_classType = 2;
		}
	}
	// allow shops
	else if (name.equals("allowShops"))
	{
		_allowShops = Boolean.parseBoolean(value);
	}
}

/**
 * Checks if the given character is affected by this zone
 * @param character
 * @return
 */
private boolean isAffected(L2Character character)
{
	// Check lvl
	if (character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
		return false;
	
	if (character instanceof L2PcInstance)
	{
		// Check class type
		if (_classType != 0)
		{
			if (((L2PcInstance) character).isMageClass())
			{
				if (_classType == 1)
					return false;
			}
			else if (_classType == 2)
				return false;
		}
		
		// Check race
		if (_race != null)
		{
			boolean ok = false;
			
			for (int element : _race)
			{
				if (((L2PcInstance) character).getRace().getRealOrdinal() == element)
				{
					ok = true;
					break;
				}
			}
			
			if (!ok)
				return false;
		}
		
		// Check class
		if (_class != null)
		{
			boolean ok = false;
			
			for (int clas : _class)
			{
				if (((L2PcInstance) character).getClassId().ordinal() == clas)
				{
					ok = true;
					break;
				}
			}
			
			if (!ok)
				return false;
		}
	}
	return true;
}

/**
 * Set the zone for this L2ZoneType Instance
 * @param zone
 */
public void setZone(L2ZoneForm zone)
{
	getZones().add(zone);
}

/**
 * Returns this zones zone form
 * @param zone
 * @return
 */
public L2ZoneForm getZone()
{
	for (L2ZoneForm zone : getZones())
	{
		return zone;
	}
	return null;
}

public final List<L2ZoneForm> getZones()
{
	if (_zone == null)
		_zone = new FastList<L2ZoneForm>();
	return _zone;
}

/**
 * Checks if the given coordinates are within zone's plane
 * @param x
 * @param y
 */
public boolean isInsideZone(int x, int y)
{
	for (L2ZoneForm zone : getZones())
	{
		if (zone.isInsideZone(x, y, zone.getHighZ()))
			return true;
	}
	return false;
}

/**
 * Checks if the given coordinates are within the zone
 * @param x
 * @param y
 * @param z
 */
public boolean isInsideZone(int x, int y, int z)
{
	for (L2ZoneForm zone : getZones())
	{
		if (zone.isInsideZone(x, y, z))
			return true;
	}
	return false;
}

/**
 * Checks if the given object is inside the zone.
 *
 * @param object
 */
public boolean isInsideZone(L2Object object)
{
	return isInsideZone(object.getX(), object.getY(), object.getZ());
}

public double getDistanceToZone(int x, int y)
{
	return getZone().getDistanceToZone(x, y);
}

public double getDistanceToZone(L2Object object)
{
	return getZone().getDistanceToZone(object.getX(), object.getY());
}

public void revalidateInZone(L2Character character)
{
	// If the character can't be affected by this zone return
	if (_checkAffected)
	{
		if (!isAffected(character))
			return;
	}
	
	// If the object is inside the zone...
	if (isInsideZone(character.getX(), character.getY(), character.getZ()))
	{
		// Was the character not yet inside this zone?
		if (!_characterList.containsKey(character.getObjectId()))
		{
			FastList<Quest> quests = getQuestByEvent(QuestEventType.ON_ENTER_ZONE);
			if (quests != null)
			{
				for (Quest quest : quests)
				{
					quest.notifyEnterZone(character, this);
				}
			}
			_characterList.put(character.getObjectId(), character);
			onEnter(character);
		}
	}
	else
	{
		removeCharacter(character);
	}
}

/**
 * Force fully removes a character from the zone
 * Should use during teleport / logoff
 * @param character
 */
public void removeCharacter(L2Character character)
{
	if (_characterList.containsKey(character.getObjectId()))
	{
		FastList<Quest> quests = getQuestByEvent(QuestEventType.ON_EXIT_ZONE);
		if (quests != null)
		{
			for (Quest quest : quests)
			{
				quest.notifyExitZone(character, this);
			}
		}
		_characterList.remove(character.getObjectId());
		onExit(character);
	}
}

/**
 * Will scan the zones char list for the character
 * @param character
 * @return
 */
public boolean isCharacterInZone(L2Character character)
{
	return _characterList.containsKey(character.getObjectId());
}

protected abstract void onEnter(L2Character character);

protected abstract void onExit(L2Character character);

public abstract void onDieInside(L2Character character);

public abstract void onReviveInside(L2Character character);

public FastMap<Integer, L2Character> getCharactersInside()
{
	return _characterList;
}

public void addQuestEvent(QuestEventType EventType, Quest q)
{
	if (_questEvents == null)
		_questEvents = new FastMap<QuestEventType, FastList<Quest>>();
	FastList<Quest> questByEvents = _questEvents.get(EventType);
	if (questByEvents == null)
		questByEvents = new FastList<Quest>();
	if (!questByEvents.contains(q))
		questByEvents.add(q);
	_questEvents.put(EventType, questByEvents);
}

public FastList<Quest> getQuestByEvent(QuestEventType EventType)
{
	if (_questEvents == null)
		return null;
	return _questEvents.get(EventType);
}

/**
 * Broadcasts packet to all players inside the zone
 */
public void broadcastPacket(L2GameServerPacket packet)
{
	if (_characterList.isEmpty())
		return;
	
	for (L2Character character : _characterList.values())
	{
		if (character instanceof L2PcInstance)
			character.sendPacket(packet);
	}
}

	public void visualizeZone(final int z)
	{
		getZone().visualizeZone(z);
	}
}
