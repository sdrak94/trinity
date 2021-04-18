package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class SpawnTable
{
private static Logger _log = Logger.getLogger(SpawnTable.class.getName());

private final Map<Integer, L2Spawn> _spawntable = new FastMap<Integer, L2Spawn>().shared();
private int _npcSpawnCount;
private int _customSpawnCount;
@SuppressWarnings("unused")
private int _lastPop = 0;

private int _highestId;

public static ScheduledFuture<?> _populationCheckTask;

public static SpawnTable getInstance()
{
	return SingletonHolder._instance;
}

private SpawnTable()
{
	if (!Config.ALT_DEV_NO_SPAWNS)
		fillSpawnTable();
	
	/*_populationCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new populationSpawnTask(), 400000, 600000);*/
}

public Map<Integer, L2Spawn> getSpawnTable()
{
	return _spawntable;
}
/*
private class populationSpawnTask implements Runnable
{

protected populationSpawnTask()
{
}
public void run()
{
	if (!_spawntable.isEmpty())
	{
		int unspawned = 0;
		int spawned = 0;
		
		final int pop = L2World.getInstance().getAllPlayersCount();
		final boolean night = GameTimeController.getInstance().isNowNight();
		
		for (L2Spawn spawn : _spawntable.values())
		{
			if (spawn == null)
				continue;
			
			final int threshold = spawn.getMinPopRequiredToSpawn();
			
			if (threshold < 10)
				continue;
			
			if (pop < threshold) //should be unspawned
			{
				if (_lastPop < pop || _lastPop < threshold)
					continue;
				
				spawn.stopRespawn();
				
				if (spawn._respawnTask != null)
					spawn._respawnTask.cancel(true);
				
				final L2Npc lastSpawn = spawn.getLastSpawn();
				
				if (lastSpawn != null)
				{
					if (!lastSpawn.isInCombat())
					lastSpawn.deleteMe();
				}
				
				_log.info("PopulationSpawnManager: Unspawned spawn id: "+spawn.getId()+ " and the lastspawn is "+ (lastSpawn != null ? lastSpawn.getName() : "null"));
				
				unspawned++;
			}
			else //should be spawned
			{
				if (_lastPop >= pop || _lastPop >= threshold)
					continue;
				
				if ((spawn.getPeriodOfDay() == 1 && night) || (spawn.getPeriodOfDay() == 2 && !night))
					continue;
				
				L2Npc npc = spawn.getLastSpawn();
				
				_log.info("PopulationSpawnManager: Spawned spawn id: "+spawn.getId()+ " and the lastspawn is "+ (npc != null ? npc.getName() : "null"));
				
				if (npc != null) //has been spawned before
				{
					_log.info("PopulationSpawnManager: NPC is not null in spawnmanager: "+npc.getName()+" HP: "+npc.getCurrentHp());
					spawn.startRespawn();
					
					if (npc.isDecayed())
						npc.setDecayed(false);
					if (npc.isDead())
						npc.doRevive();
					npc.setCurrentHp(npc.getMaxHp());
					npc.setCurrentMp(npc.getMaxMp());
					
					if (spawn._respawnTask == null)
						npc.spawnMe();
				}
				else // has never been spawned before?
				{
					_log.info("PopulationSpawnManager: NPC is NULL in spawnmanager: "+spawn.getId());
					npc = spawn.doSpawn();
					if (npc == null)
						continue;
					
					npc.setCurrentHp(npc.getMaxHp());
					npc.setCurrentMp(npc.getMaxMp());
					spawn.startRespawn();
					if (npc.isDecayed())
						npc.setDecayed(false);
					if (npc.isDead())
						npc.doRevive();
				}
				
				spawned++;
			}
			
			_log.info("PopulationSpawnManager: Deleted " + unspawned + " and spawned " + spawned+" creatures at pop: " + pop);
		}
		
		_lastPop = pop;
	}
}
}*/

private void fillSpawnTable()
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		if (Config.DELETE_GMSPAWN_ON_CUSTOM)
		{
			statement = con.prepareStatement("SELECT id, location, count, npc_templateid, locx, locy, locz, randomx, randomy, heading, respawn_delay, loc_id, periodOfDay, population FROM spawnlist where id NOT in ( select id from custom_notspawned where isCustom = false ) ORDER BY id");
		}
		else
		{
			statement = con.prepareStatement("SELECT id, location, count, npc_templateid, locx, locy, locz, randomx, randomy, heading, respawn_delay, loc_id, periodOfDay, population FROM spawnlist ORDER BY id");
		}
		ResultSet rset = statement.executeQuery();
		
		L2Spawn spawnDat;
		L2NpcTemplate template1;
		
		while (rset.next())
		{
			template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
			if (template1 != null)
			{
				if (template1.type.equalsIgnoreCase("L2SiegeGuard"))
				{
					// Don't spawn
				}
				else if (template1.type.equalsIgnoreCase("L2RaidBoss"))
				{
					// Don't spawn raidboss
				}
				else if (!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
				{
					// Dont' spawn class masters
				}
				else
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setId(rset.getInt("id"));
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setRandomX(rset.getInt("randomx"));
					spawnDat.setRandomY(rset.getInt("randomy"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					int loc_id = rset.getInt("loc_id");
					spawnDat.setLocation(loc_id);
					
					if (rset.getString("location").equalsIgnoreCase("random"))
						spawnDat.setSpawnType(1);
					
					spawnDat.setMinPopRequiredToSpawn(rset.getInt("population"));
					
					switch (rset.getInt("periodOfDay"))
					{
					case 0: // default
						if (spawnDat.getMinPopRequiredToSpawn() < 10)
							_npcSpawnCount += spawnDat.init();
						spawnDat.setPeriodOfDay(0);
						break;
					case 1: // Day
						DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
						if (spawnDat.getMinPopRequiredToSpawn() < 10)
							_npcSpawnCount++;
						spawnDat.setPeriodOfDay(1);
						break;
					case 2: // Night
						DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
						if (spawnDat.getMinPopRequiredToSpawn() < 10)
							_npcSpawnCount++;
						spawnDat.setPeriodOfDay(2);
						break;
					}
					
					_spawntable.put(spawnDat.getId(), spawnDat);
					if (spawnDat.getId() > _highestId)
						_highestId = spawnDat.getId();
					
					if (spawnDat.getSpawnType() == 1 || (rset.getInt("respawn_delay") >= 1600) && template1.type.equalsIgnoreCase("L2Monster"))
					{
						if (spawnDat.getLastSpawn() != null)
						{
							spawnDat.getLastSpawn().doDie(null);
							//_log.warning("Killing "+spawnDat.getLastSpawn().getName()+" at the start of server!!!!");
						}
					}
				}
			}
			else
			{
				_log.warning("SpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
			}
		}
		rset.close();
		statement.close();
	}
	catch (Exception e)
	{
		// problem with initializing spawn, go to next one
		_log.warning("SpawnTable: Spawn could not be initialized: " + e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
	
	_log.config("SpawnTable: Loaded " + _spawntable.size() + " Npc Spawn Locations.");
	
	if (Config.CUSTOM_SPAWNLIST_TABLE)
	{
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (Config.DELETE_GMSPAWN_ON_CUSTOM)
			{
				statement = con.prepareStatement("SELECT id, location, count, npc_templateid, locx, locy, locz, randomx, randomy, heading, respawn_delay, loc_id, periodOfDay, population FROM custom_spawnlist where id NOT in ( select id from custom_notspawned where isCustom = true ) ORDER BY id");
			}
			else
			{
				statement = con.prepareStatement("SELECT id, location, count, npc_templateid, locx, locy, locz, randomx, randomy, heading, respawn_delay, loc_id, periodOfDay, population FROM custom_spawnlist ORDER BY id");
			}
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.type.equalsIgnoreCase("L2SiegeGuard"))
					{
						// Don't spawn
					}
					else if (template1.type.equalsIgnoreCase("L2RaidBoss"))
					{
						// Don't spawn raidboss
					}
					else if (!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
					{
						// Dont' spawn class masters
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setRandomX(rset.getInt("randomx"));
						spawnDat.setRandomY(rset.getInt("randomy"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						spawnDat.setCustom(true);
						int loc_id = rset.getInt("loc_id");
						spawnDat.setLocation(loc_id);
						
						if (rset.getString("location").equalsIgnoreCase("random"))
							spawnDat.setSpawnType(1);
						
						spawnDat.setMinPopRequiredToSpawn(rset.getInt("population"));
						
						switch (rset.getInt("periodOfDay"))
						{
						case 0: // default
							if (spawnDat.getMinPopRequiredToSpawn() < 10)
								_customSpawnCount += spawnDat.init();
							spawnDat.setPeriodOfDay(0);
							break;
						case 1: // Day
							DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
							if (spawnDat.getMinPopRequiredToSpawn() < 10)
								_customSpawnCount++;
							spawnDat.setPeriodOfDay(1);
							break;
						case 2: // Night
							DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
							if (spawnDat.getMinPopRequiredToSpawn() < 10)
								_customSpawnCount++;
							spawnDat.setPeriodOfDay(2);
							break;
						}
						
						_spawntable.put(spawnDat.getId(), spawnDat);
						if (spawnDat.getId() > _highestId)
							_highestId = spawnDat.getId();
						
						if (spawnDat.getSpawnType() == 1 || (rset.getInt("respawn_delay") >= 1600) && template1.type.equalsIgnoreCase("L2Monster"))
						{
							if (spawnDat.getLastSpawn() != null)
							{
								spawnDat.getLastSpawn().doDie(null);
								//_log.warning("Killing "+spawnDat.getLastSpawn().getName()+" at the start of server!!!!");
							}
						}
					}
				}
				else
				{
					_log.warning("CustomSpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("CustomSpawnTable: Spawn could not be initialized: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		_log.config("CustomSpawnTable: Loaded " + _customSpawnCount + " Npc Spawn Locations.");
		
	}
	
	if (Config.DEBUG)
		_log.fine("SpawnTable: Spawning completed, total number of NPCs in the world: " + (_npcSpawnCount + _customSpawnCount));
	
}

public L2Spawn getTemplate(int id)
{
	return _spawntable.get(id);
}

public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
{
	_highestId++;
	spawn.setId(_highestId);
	_spawntable.put(_highestId, spawn);
	
	if (storeInDb)
	{
		Connection con = null;
		String spawnTable;
		if (spawn.isCustom() && Config.CUSTOM_SPAWNLIST_TABLE)
			spawnTable = "custom_spawnlist";
		else
			spawnTable = "spawnlist";
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO " + spawnTable
					+ "(id,location,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id,periodOfDay,population) values(?,?,?,?,?,?,?,?,?,?,?,?)");
			
			statement.setInt(1, spawn.getId());
			
			String location = "";
			if (spawn.getSpawnType() == 1)
				location = "random";
			
			statement.setString(2, location);
			statement.setInt(3, spawn.getAmount());
			statement.setInt(4, spawn.getNpcid());
			statement.setInt(5, spawn.getLocx());
			statement.setInt(6, spawn.getLocy());
			statement.setInt(7, spawn.getLocz());
			statement.setInt(8, spawn.getHeading());
			statement.setInt(9, spawn.getRespawnDelay() / 1000);
			statement.setInt(10, spawn.getLocation());
			statement.setInt(11, spawn.getPeriodOfDay());
			statement.setInt(12, spawn.getMinPopRequiredToSpawn());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			// problem with storing spawn
			_log.warning("SpawnTable: Could not store spawn in the DB:" + e);
		}
		finally
		{
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

public void deleteSpawn(L2Spawn spawn, boolean updateDb)
{
	
	if (_spawntable.remove(spawn.getId()) == null)
		return;
	
	if (updateDb)
	{
		Connection con = null;
		if (Config.DELETE_GMSPAWN_ON_CUSTOM)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("Replace into custom_notspawned VALUES (?,?)");
				statement.setInt(1, spawn.getId());
				statement.setBoolean(2, spawn.isCustom());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				// problem with inserting nospawn
				_log.warning("SpawnTable: Spawn " + spawn.getId() + " could not be insert into DB: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		else
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("DELETE FROM "
						+ (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " WHERE id=?");
				statement.setInt(1, spawn.getId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				// problem with deleting spawn
				_log.warning("SpawnTable: Spawn " + spawn.getId() + " could not be removed from DB: " + e);
			}
			finally
			{
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
}

//just wrapper
public void reloadAll()
{
	fillSpawnTable();
}

/**
 * Get all the spawn of a NPC<BR><BR>
 *
 * @param npcId : ID of the NPC to find.
 * @return
 */
public void findNPCInstances(L2PcInstance activeChar, int npcId, int teleportIndex)
{
	int index = 0;
	
	if (activeChar.isGM())
	{
		L2NpcTemplate temp = NpcTable.getInstance().getTemplate(npcId);
		
		boolean canCheck = true;
		
		if (temp != null && temp.type.contains("Boss") && activeChar.getAccessLevel().getLevel() < 5)
			canCheck = false;
		
		if (canCheck)
		{
			for (L2Spawn spawn : _spawntable.values())
			{
				if (npcId == spawn.getNpcid())
				{
					index++;
					
					if (teleportIndex > -1)
					{
						if (teleportIndex == index)
							activeChar.teleToLocation(spawn.getCurX(), spawn.getCurY(), spawn.getCurZ(), true);
					}
					else
					{
						activeChar.sendMessage(index + " - " + spawn.getTemplate().name + " (" + spawn.getId() + "): " + spawn.getCurX() + " "
								+ spawn.getCurY() + " " + spawn.getCurZ());
					}
				}
			}
		}
	}
	
	if (index == 0)
		activeChar.sendMessage("No current spawns found.");
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final SpawnTable _instance = new SpawnTable();
}
}
