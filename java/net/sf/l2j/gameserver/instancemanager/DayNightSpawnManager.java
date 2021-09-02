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
package net.sf.l2j.gameserver.instancemanager;

import java.util.Map;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;

/**
 * This class ...
 *
 * @version $Revision: $ $Date: $
 * @author  godson
 */

public class DayNightSpawnManager
{

private static Logger _log = Logger.getLogger(DayNightSpawnManager.class.getName());

private static Map<L2Spawn, L2Npc> _dayCreatures;
private static Map<L2Spawn, L2Npc> _nightCreatures;
private static Map<L2Spawn, L2RaidBossInstance> _bosses;
private static Map<L2Spawn, L2TeleporterInstance> _gk;

//private static int _currentState;  // 0 = Day, 1 = Night

public static DayNightSpawnManager getInstance()
{
	return SingletonHolder._instance;
}

private DayNightSpawnManager()
{
	_dayCreatures = new FastMap<L2Spawn, L2Npc>();
	_nightCreatures = new FastMap<L2Spawn, L2Npc>();
	_bosses = new FastMap<L2Spawn, L2RaidBossInstance>();
	_gk = new FastMap<L2Spawn, L2TeleporterInstance>();
	
	_log.info("DayNightSpawnManager: Day/Night handler initialized");
}

public Map<L2Spawn, L2Npc> getDayCreatures()
{
	return _dayCreatures;
}

public Map<L2Spawn, L2Npc> getNightCreatures()
{
	return _nightCreatures;
}

public void addDayCreature(L2Spawn spawnDat)
{
	if (_dayCreatures.containsKey(spawnDat))
	{
		_log.warning("DayNightSpawnManager: Spawn already added into day map");
		return;
	}
	else
		_dayCreatures.put(spawnDat, null);
}

public void addNightCreature(L2Spawn spawnDat)
{
	if (_nightCreatures.containsKey(spawnDat))
	{
		_log.warning("DayNightSpawnManager: Spawn already added into night map");
		return;
	}
	else
		_nightCreatures.put(spawnDat, null);
}

/*
 * Spawn Day Creatures, and Unspawn Night Creatures
 */
public void spawnDayCreatures()
{
	spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
}

public void kickAllPlayersFromPT()
{
	for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
	{

		if (!(player instanceof Ghost) && player.getClient().isDetached())
		{
			continue;
		}
		if (player.isGM())
		{
			continue;
		}
		if (player.isInFT())
		{
			player.setIsPendingRevive(true);
			player.teleToLocation(83380, 148107, -3404, true);
			player.setInsideZone(L2Character.ZONE_FARM, false);
			player.setInsideZone(L2Character.ZONE_CHAOTIC, false);
			player.setIsInFT(false);
			player.sendMessage("The sun raised, the undeads vanished, so did you.");
		}
	}
	_log.warning("exited from: kickAllPlayersFromPTWhenNightIsOver" );
}
/*
 * Spawn Night Creatures, and Unspawn Day Creatures
 */
public void spawnNightCreatures()
{
	spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
}

/*
 * Manage Spawn/Respawn
 * Arg 1 : Map with L2NpcInstance must be unspawned
 * Arg 2 : Map with L2NpcInstance must be spawned
 * Arg 3 : String for log info for unspawned L2NpcInstance
 * Arg 4 : String for log info for spawned L2NpcInstance
 */
private void spawnCreatures(Map<L2Spawn, L2Npc> UnSpawnCreatures, Map<L2Spawn, L2Npc> SpawnCreatures, String UnspawnLogInfo,
		String SpawnLogInfo)
{
	try
	{
		if (!UnSpawnCreatures.isEmpty())
		{
			int i = 0;
			for (L2Npc dayCreature : UnSpawnCreatures.values())
			{
				if (dayCreature == null)
					continue;

				if (dayCreature.isDecayed())
					dayCreature.setDecayed(false);
				if (dayCreature.isDead())
					dayCreature.doRevive();
				dayCreature.setXYZ(dayCreature.getSpawn().getLocx(), dayCreature.getSpawn().getLocy(), dayCreature.getSpawn().getLocz());
				dayCreature.deleteMe();
				dayCreature.spawnMeNoOnSpawn();
				dayCreature.getSpawn().stopRespawn();
				i++;
			}
			//if (Config.DEBUG)
				_log.info("DayNightSpawnManager: Deleted " + i + " " + UnspawnLogInfo + " creatures");
		}
		
		final int playersOnline = L2World.getInstance().getAllPlayersCount();
		
		int i = 0;
		L2Npc creature = null;
		for (L2Spawn spawnDat : SpawnCreatures.keySet())
		{
			if (spawnDat.getMinPopRequiredToSpawn() >= 10 && spawnDat.getMinPopRequiredToSpawn() > playersOnline)
				continue;
			
			if (SpawnCreatures.get(spawnDat) == null)
			{
				creature = spawnDat.doSpawn();
				if (creature == null)
					continue;
				
				SpawnCreatures.remove(spawnDat);
				SpawnCreatures.put(spawnDat, creature);
				creature.setCurrentHp(creature.getMaxHp());
				creature.setCurrentMp(creature.getMaxMp());
				creature.getSpawn().startRespawn();
				creature.setXYZ(creature.getSpawn().getLocx(), creature.getSpawn().getLocy(), creature.getSpawn().getLocz());
				if (creature.isDecayed())
					creature.setDecayed(false);
				if (creature.isDead())
					creature.doRevive();
			}
			else
			{
				creature = SpawnCreatures.get(spawnDat);
				if (creature == null)
					continue;
				
				creature.getSpawn().startRespawn();
				if (creature.isDecayed())
					creature.setDecayed(false);
				if (creature.isDead())
					creature.doRevive();
				creature.setCurrentHp(creature.getMaxHp());
				creature.setCurrentMp(creature.getMaxMp());
				creature.setXYZ(creature.getSpawn().getLocx(), creature.getSpawn().getLocy(), creature.getSpawn().getLocz());
				creature.spawnMe();
			}
			
			i++;
		}
		
		//if (Config.DEBUG)
			_log.info("DayNightSpawnManager: Spawning " + i + " " + SpawnLogInfo + " creatures");
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
}

private void changeMode(int mode)
{
	if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty())
		return;
	
	switch (mode)
	{
	case 0:
		spawnDayCreatures();
		specialNightBoss(0);
		break;
	case 1:
		spawnNightCreatures();
		specialNightBoss(1);
		break;
	default:
		_log.warning("DayNightSpawnManager: Wrong mode sent");
		break;
	}
}

public void notifyChangeMode()
{
	try
	{
		if (GameTimeController.getInstance().isNowNight())
			changeMode(1);
		else
			changeMode(0);
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
}

public void cleanUp()
{
	_nightCreatures.clear();
	_dayCreatures.clear();
	_bosses.clear();
}

private void specialNightBoss(int mode)
{
	try
	{
		for (L2Spawn spawn : _bosses.keySet())
		{
			L2RaidBossInstance boss = _bosses.get(spawn);
			
			if (boss == null && mode == 1)
			{
				boss = (L2RaidBossInstance) spawn.doSpawn();
				RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
				_bosses.remove(spawn);
				_bosses.put(spawn, boss);
				continue;
			}
			
			if (boss == null && mode == 0)
				continue;
			
			if (boss.getNpcId() == 25328 && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
				handleHellmans(boss, mode);
			return;
		}
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
}

private void handleHellmans(L2RaidBossInstance boss, int mode)
{
	switch (mode)
	{
	case 0:
		boss.deleteMe();
		_log.info("DayNightSpawnManager: Deleting Hellman raidboss");
		break;
	case 1:
		boss.spawnMe();
		_log.info("DayNightSpawnManager: Spawning Hellman raidboss");
		break;
	}
}

public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
{
	if (_bosses.containsKey(spawnDat))
		return _bosses.get(spawnDat);
	
	if (GameTimeController.getInstance().isNowNight())
	{
		L2RaidBossInstance raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
		_bosses.put(spawnDat, raidboss);
		
		return raidboss;
	}
	else
		_bosses.put(spawnDat, null);
	
	return null;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final DayNightSpawnManager _instance = new DayNightSpawnManager();
}
}
