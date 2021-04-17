package net.sf.l2j.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.L2AttackableAI;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.knownlist.MonsterKnownList;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.MinionList;
import net.sf.l2j.util.Rnd;

/**
 * This class manages all Monsters.
 *
 * L2MonsterInstance :<BR><BR>
 * <li>L2MinionInstance</li>
 * <li>L2RaidBossInstance </li>
 * <li>L2GrandBossInstance </li>
 *
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public class L2MonsterInstance extends L2Attackable
{
//private static Logger _log = Logger.getLogger(L2MonsterInstance.class.getName());

public final MinionList _minionList;

protected ArrayList<ScheduledFuture<?>> _tasks = new ArrayList<>();
protected ScheduledFuture<?> _minionMaintainTask = null;
protected void startRespawnTask()
{
	if (getInstanceId() == 0)
		_tasks.add(ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() -> checkAndRespawn(), 60000, 5000 + Rnd.get(1000, 2500)));
}
private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;

/**
 * Constructor of L2MonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Call the L2Character constructor to set the _template of the L2MonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
 * <li>Set the name of the L2MonsterInstance</li>
 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
 *
 * @param objectId Identifier of the object to initialized
 * @param L2NpcTemplate Template to apply to the NPC
 */
public L2MonsterInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
	if (getTemplate().getMinionData() != null)
		_minionList  = new MinionList(this);
	else
		_minionList = null;
}
public void checkAndRespawn()
{
	if (isDead() || isDecayed() || getInstanceId() != 0)
		return;
	final L2Spawn spawn = getSpawn();
	if (spawn == null)
		return;
	final int spawnX = spawn.getLocx();
	final int spawnY = spawn.getLocy();
	final int spawnZ = spawn.getLocz();
	final int rad = isInCombat() ? 3000 : 600;
	if (!isMovementDisabled() && !isInsideRadius(spawnX, spawnY, spawnZ, rad, true, false))
	{
		teleToLocation(spawnX, spawnY, spawnZ, 20, false);
		setCurrentHp(Math.min(getMaxHp(), getCurrentHp() + getMaxHp() * 0.05));
		clearAggroList();
	}
}
@Override
public final MonsterKnownList getKnownList()
{
	return (MonsterKnownList)super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new MonsterKnownList(this));
}

/**
 * Return True if the attacker is not another L2MonsterInstance.<BR><BR>
 */
@Override
public boolean isAutoAttackable(L2Character attacker)
{
	if (attacker instanceof L2MonsterInstance)
		return false;
	
	return !isEventMob;
}

/**
 * Return True if the L2MonsterInstance is Agressive (aggroRange > 0).<BR><BR>
 */
@Override
public boolean isAggressive()
{
	return (getTemplate().aggroRange > 0) && !isEventMob;
}

@Override
public void onSpawn()
{
	super.onSpawn();
	startMaintenanceTask();
	if (_minionList != null && !(this instanceof L2MinionInstance))
	{
		try
		{
			for (L2MinionInstance minion : getSpawnedMinions())
			{
				if (minion == null) continue;
				getSpawnedMinions().remove(minion);
				minion.deleteMe();
			}
			_minionList.clearRespawnList();
			
			manageMinions();
		}
		catch ( NullPointerException e )
		{
		}
	}
	else if (isRaid())
	{
		manageMinions();
	}
}

protected int getMaintenanceInterval()
{
	return MONSTER_MAINTENANCE_INTERVAL;
}
protected void startMaintenanceTask()
{
	startRespawnTask();
}
/**
 * Spawn all minions at a regular interval
 *
 *//*
protected void startMaintenanceTask()
{
	// maintenance task now used only for minions spawn
	if (_minionList == null)
		return;
	
	_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
		public void run()
		{
			_minionList.spawnMinions();
		}
	}, getMaintenanceInterval() + Rnd.get(1000));
}*/

protected void manageMinions ()
{
	_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
		public void run()
		{
			_minionList.spawnMinions();
		}
	}, getMaintenanceInterval());
}

public void callMinions()
{
	if (hasMinions())
	{
		for (L2MinionInstance minion : _minionList.getSpawnedMinions())
		{
			if (minion == null || minion.isDead())
				continue;
			
			// Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance
			if (!isInsideRadius(minion, 200, false, false))
			{
				// Calculate a new random coord for the minion based on the master's coord
				// but with minimum distance from master = 30
				int minionX = Rnd.nextInt(340);
				int minionY = Rnd.nextInt(340);
				
				if (minionX < 171)
					minionX = getX() + minionX + 30;
				else
					minionX = getX() - minionX + 140;
				
				if (minionY < 171)
					minionY = getY() + minionY + 30;
				else
					minionY = getY() - minionY + 140;
				
				// Move the minion to the new coords
				if (!minion.isInCombat() && !minion.isDead() && !minion.isOutOfControl() && !minion.isMovementDisabled())
					minion.moveToLocation(minionX, minionY, getZ(), 0);
			}
		}
	}
}

public void callMinionsToAssist(L2Character attacker, int damage, L2Attackable exclude)
{
	if (hasMinions())
	{
		for (L2MinionInstance minion : _minionList.getSpawnedMinions())
		{
			if (minion == null || minion.isDead() || minion == exclude)
				continue;
			
			try
			{
				if (((L2AttackableAI)minion.getAI())._globalAggro < 0)
					((L2AttackableAI)minion.getAI())._globalAggro = 0;
				minion.addDamageHate(attacker, damage);
			}
			catch (Exception e)
			{
			}
		}
	}
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	
	if (!(this instanceof L2RaidBossInstance) && killer instanceof L2Playable && getLevel() > 79)
		killer.getActingPlayer().setKillL2AttackableTick(GameTimeController.getGameTicks());
	
	if (_minionMaintainTask != null)
		_minionMaintainTask.cancel(true); // doesn't do it?
	
	if (hasMinions() && isRaid())
		deleteSpawnedMinions();
	
	return true;
}

public List<L2MinionInstance> getSpawnedMinions()
{
	if (_minionList == null)
		return null;
	return _minionList.getSpawnedMinions();
}

public int getTotalSpawnedMinionsInstances()
{
	if (_minionList == null)
		return 0;
	return _minionList.countSpawnedMinions();
}

public int getTotalSpawnedMinionsGroups()
{
	if (_minionList == null)
		return 0;
	return _minionList.lazyCountSpawnedMinionsGroups();
}

public void notifyMinionDied(L2MinionInstance minion)
{
	_minionList.moveMinionToRespawnList(minion);
}

public void notifyMinionSpawned(L2MinionInstance minion)
{
	_minionList.addSpawnedMinion(minion);
}

public boolean hasMinions()
{
	if (_minionList == null || this instanceof L2MinionInstance)
		return false;
	return _minionList.hasMinions();
}

@Override
public void addDamageHate(L2Character attacker, int aggro)
{
	if (!(attacker instanceof L2MonsterInstance))
	{
		super.addDamageHate(attacker, aggro);
	}
}

@Override
public void deleteMe()
{
	if (hasMinions())
	{
		if (_minionMaintainTask != null)
			_minionMaintainTask.cancel(true);
		
		deleteSpawnedMinions();
	}
	super.deleteMe();
}

public void deleteSpawnedMinions()
{
	for(L2MinionInstance minion : getSpawnedMinions())
	{
		if (minion == null) continue;
		minion.abortAttack();
		minion.abortCast();
		minion.deleteMe();
		getSpawnedMinions().remove(minion);
	}
	_minionList.clearRespawnList();
}
}
