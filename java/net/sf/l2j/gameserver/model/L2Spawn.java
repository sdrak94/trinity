/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.Territory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type.
 * <B><U> Concept</U> :</B><BR>
 * <BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
 * <BR>
 *
 * @author Nightmare
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Spawn
{
	protected static final Logger	_log	= Logger.getLogger(L2Spawn.class.getName());
	
	private long _diedDate;
	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private L2NpcTemplate			_template;
	/** The Identifier of this spawn in the spawn table */
	protected int					_id;
	/** The identifier of the location area where L2NpcInstance can be spwaned */
	private int						_location;
	/** The maximum number of L2NpcInstance that can manage this L2Spawn */
	private int						_maximumCount;
	/** The current number of L2NpcInstance managed by this L2Spawn */
	private int						_currentCount;
	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	protected int					_scheduledCount;
	/** The X position of the spwan point */
	private int						_locX;
	protected int					_curX;
	
	public final void setCurX(int curX)
	{
		_curX = curX;
	}
	
	protected int _curY;
	
	public final void setCurY(int curY)
	{
		_curY = curY;
	}
	
	protected int _curZ;
	
	public final void setCurZ(int curZ)
	{
		_curZ = curZ;
	}
	
	/** The Y position of the spwan point */
	private int				_locY;
	/** The Z position of the spwan point */
	private int				_locZ;
	/** randomization */
	private int				_randomX	= 0, _randomY = 0;
	/** The heading of L2NpcInstance when they are spawned */
	private int				_heading;
	/** The delay between a L2NpcInstance remove and its re-spawn */
	private int				_respawnDelay;
	/** Minimum delay RaidBoss */
	private int				_respawnMinDelay;
	/** Maximum delay RaidBoss */
	private int				_respawnMaxDelay;
	private int				_instanceId	= 0;
	/** The generic constructor of L2NpcInstance managed by this L2Spawn */
	private Constructor<?>	_constructor;
	/** If True a L2NpcInstance is respawned each time that another is killed */
	private boolean			_doRespawn;
	
	public final boolean isRespawning()
	{
		return _doRespawn;
	}
	
	/** If true then spawn is custom */
	private boolean						_customSpawn;
	private L2Npc						_lastSpawn;
	private static List<SpawnListener>	_spawnListeners	= new FastList<SpawnListener>();
	public int							_spawnType		= 0;
	/* public ScheduledFuture<?> _respawnTask; */
	
	public int getSpawnType()
	{
		return _spawnType;
	}
	
	private int _periodOfDay = 0;
	
	public final int getPeriodOfDay()
	{
		return _periodOfDay;
	}
	
	public final void setPeriodOfDay(int periodOfDay)
	{
		_periodOfDay = periodOfDay;
	}
	
	public void setSpawnType(int spawnType)
	{
		_spawnType = spawnType;
	}
	
	public int _minPopRequiredToSpawn = 0;
	
	public final int getMinPopRequiredToSpawn()
	{
		return _minPopRequiredToSpawn;
	}
	
	public final void setMinPopRequiredToSpawn(int minPop)
	{
		_minPopRequiredToSpawn = minPop;
	}
	
	/** The task launching the function doSpawn() */
	class SpawnTask implements Runnable
	{
		// L2NpcInstance _instance;
		// int _objId;
		private final L2Npc _oldNpc;
		
		public SpawnTask(/* int objid */L2Npc pOldNpc)
		{
			// _objId= objid;
			_oldNpc = pOldNpc;
		}
		
		public void run()
		{
			try
			{
				// doSpawn();
				respawnNpc(_oldNpc);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
			_scheduledCount--;
		}
	}
	
	/**
	 * Constructor of L2Spawn.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
	 * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * This link is stored in <B>_template</B><BR>
	 * <BR>
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
	 * This link is stored in <B>_spawn</B> of the L2NpcInstance<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Spawn</li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 *
	 * @param mobTemplate
	 *            The L2NpcTemplate to link to this L2Spawn
	 */
	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		if (_template == null)
			return;
		// The Name of the L2NpcInstance type managed by this L2Spawn
		String implementationName = _template.type; // implementing class name
		if (mobTemplate.npcId == 30995)
			implementationName = "L2RaceManager";
		// if (mobTemplate.npcId == 8050)
		if ((mobTemplate.npcId >= 31046) && (mobTemplate.npcId <= 31053))
			implementationName = "L2SymbolMaker";
		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		Class<?>[] parameters =
		{
			int.class, Class.forName("net.sf.l2j.gameserver.templates.chars.L2NpcTemplate")
		};
		_constructor = Class.forName("net.sf.l2j.gameserver.model.actor.instance." + implementationName + "Instance").getConstructor(parameters);
	}
	
	/**
	 * Return the maximum number of L2NpcInstance that this L2Spawn can manage.<BR>
	 * <BR>
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * Return the Identifier of this L2Spwan (used as key in the SpawnTable).<BR>
	 * <BR>
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Return the Identifier of the location area where L2NpcInstance can be spwaned.<BR>
	 * <BR>
	 */
	public int getLocation()
	{
		return _location;
	}
	
	/**
	 * Return the X position of the spwan point.<BR>
	 * <BR>
	 */
	public int getLocx()
	{
		return _locX;
	}
	
	/**
	 * Return the Y position of the spwan point.<BR>
	 * <BR>
	 */
	public int getLocy()
	{
		return _locY;
	}
	
	/**
	 * Return the Z position of the spwan point.<BR>
	 * <BR>
	 */
	public int getLocz()
	{
		return _locZ;
	}
	
	public int getCurX()
	{
		return _curX;
	}
	
	/**
	 * Return the Y position of the spwan point.<BR>
	 * <BR>
	 */
	public int getCurY()
	{
		return _curY;
	}
	
	/**
	 * Return the Z position of the spwan point.<BR>
	 * <BR>
	 */
	public int getCurZ()
	{
		return _curZ;
	}
	
	public int getRandomX()
	{
		return _randomX;
	}
	
	public int getRandomY()
	{
		return _randomY;
	}
	
	/**
	 * Return the Itdentifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getNpcid()
	{
		return _template.npcId;
	}
	
	/**
	 * Return the heading of L2NpcInstance when they are spawned.<BR>
	 * <BR>
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Return the delay between a L2NpcInstance remove and its re-spawn in miliseconds.<BR>
	 * <BR>
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	
	/**
	 * Return Min RaidBoss Spawn delay.<BR>
	 * <BR>
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * Return Max RaidBoss Spawn delay.<BR>
	 * <BR>
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.<BR>
	 * <BR>
	 */
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Set the Identifier of this L2Spwan (used as key in the SpawnTable).<BR>
	 * <BR>
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spwaned.<BR>
	 * <BR>
	 */
	public void setLocation(int location)
	{
		_location = location;
	}
	
	/**
	 * Set Minimum Respawn Delay.<BR>
	 * <BR>
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Set Maximum Respawn Delay.<BR>
	 * <BR>
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Set the X position of the spwan point.<BR>
	 * <BR>
	 */
	public void setLocx(int locx)
	{
		_locX = locx;
	}
	
	/**
	 * Set the Y position of the spwan point.<BR>
	 * <BR>
	 */
	public void setLocy(int locy)
	{
		_locY = locy;
	}
	
	/**
	 * Set the Z position of the spwan point.<BR>
	 * <BR>
	 */
	public void setLocz(int locz)
	{
		_locZ = locz;
	}
	
	public void setRandomX(int randomX)
	{
		_randomX = randomX;
	}
	
	public void setRandomY(int randomY)
	{
		_randomY = randomY;
	}
	
	/**
	 * Set the heading of L2NpcInstance when they are spawned.<BR>
	 * <BR>
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Set the spawn as custom.<BR>
	 */
	public void setCustom(boolean custom)
	{
		_customSpawn = custom;
	}
	
	/**
	 * Return type of spawn.<BR>
	 * <BR>
	 */
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn</li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag</li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn</li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR>
	 * <BR>
	 */
	public void decreaseCount(/* int npcId */L2Npc oldNpc)
	{
		// sanity check
		if (_currentCount <= 0)
		{
			if (_currentCount < 0)
			{
				_log.warning(getId() + "'s _currentCount is less than 0: " + _currentCount);
			}
			return;
		}
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn && _scheduledCount + _currentCount < _maximumCount)
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;
			int delay = getRespawnDelay();
			final double pplOnline = L2World.getInstance().getAllPlayersCount();
			boolean doit = false;
			if (pplOnline >= 250)
				doit = true;
			else if (pplOnline > 210 && oldNpc.getElite() == 0 && oldNpc.getRare() == 0)
				doit = true;
			if (doit)
			{
				final double serverFullness = pplOnline / Config.MAXIMUM_ONLINE_USERS;
				final double percentOf37 = serverFullness / 0.28;
				double respawnDelayMulti = 1 / percentOf37;
				if (respawnDelayMulti > 0.8)
					respawnDelayMulti = 0.8;
				else if (respawnDelayMulti < 0.35)
					respawnDelayMulti = 0.35;
				delay *= respawnDelayMulti;
				if (delay < 15000)
					delay = 15000;
			}
			if (oldNpc.getRare() > 0) // rare npc
			{
				final int halfDelay = delay / 2;
				delay += Rnd.get(-halfDelay, halfDelay * 2);
				if (delay < 120000)
				{
					delay = 120000;
					if (getRespawnDelay() >= 2700000)
						_log.warning("Rare NPC: " + oldNpc.getName() + " has a respawn delay of less than 120 seconds!!!!!!");
				}
			}
			/*
			 * if (_respawnTask != null)
			 * _respawnTask.cancel(true);
			 * _respawnTask =
			 */ ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), delay);
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to True.<BR>
	 * <BR>
	 *
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;
		return _currentCount;
	}
	
	/**
	 * Create a L2NpcInstance in this L2Spawn.<BR>
	 * <BR>
	 */
	public L2Npc spawnOne(boolean val)
	{
		return doSpawn(val);
	}
	
	/**
	 * Set _doRespawn to False to stop respawn in thios L2Spawn.<BR>
	 * <BR>
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR>
	 * <BR>
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	public L2Npc doSpawn()
	{
		return doSpawn(false);
	}
	
	/**
	 * Create the L2NpcInstance, add it to the world and lauch its OnSpawn action.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
	 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR>
	 * <BR>
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR>
	 * <BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier</li>
	 * <li>Call the constructor of the L2NpcInstance</li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn</li>
	 * <li>Set the position of the L2NpcInstance</li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max</li>
	 * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1)</li>
	 * <li>Link the L2NpcInstance to this L2Spawn</li>
	 * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world</li>
	 * <li>Lauch the action OnSpawn fo the L2NpcInstance</li><BR>
	 * <BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn</li><BR>
	 * <BR>
	 */
	public L2Npc doSpawn(boolean isSummonSpawn)
	{
		L2Npc mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion or L2Decoy spawn
			if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion") || _template.type.equalsIgnoreCase("L2FlyMinion") || _template.type.equalsIgnoreCase("L2Decoy") || _template.type.equalsIgnoreCase("L2Trap") || _template.type.equalsIgnoreCase("L2EffectPoint"))
			{
				_currentCount++;
				return mob;
			}
			// Get L2NpcInstance Init parameters and its generate an Identifier
			Object[] parameters =
			{
				IdFactory.getInstance().getNextId(), _template
			};
			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance,
			// L2FeedableBeastInstance, L2TamedBeastInstance, L2FolkInstance or L2TvTEventNpcInstance)
			Object tmp = _constructor.newInstance(parameters);
			((L2Object) tmp).setInstanceId(_instanceId); // Must be done before object is spawned into visible world
			if (isSummonSpawn && tmp instanceof L2Character)
				((L2Character) tmp).setShowSummonAnimation(isSummonSpawn);
			// Check if the Instance is a L2NpcInstance
			if (!(tmp instanceof L2Npc))
				return mob;
			mob = (L2Npc) tmp;
			return intializeNpcInstance(mob);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPC " + _template.npcId + " class not found");
			e.printStackTrace();
		}
		return mob;
	}
	
	/**
	 * @param mob
	 * @return
	 */
	private L2Npc intializeNpcInstance(L2Npc mob)
	{
		int newlocx = 0, newlocy = 0, newlocz = 0;
		boolean spawnedAsRandom = false;
		if (getSpawnType() == 1)
		{
			FastList<L2PcInstance> players = new FastList<L2PcInstance>();
			for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				if (player != null && player.isOnline() == 1 && !player.isAlikeDead() && player.getInstanceId() == 0 && !player.isInOlympiadMode() && !player.isGM() && !player.isInvisible() && !player.isInDuel() && !player.isInJail())
				{
					if (player.isInsideZone(L2Character.ZONE_PEACE) || (player._inEventDM && DM._started) || (player._inEventDM && NewDM._started))
						continue;
					players.add(player);
				}
			}
			if (players.size() > 10)
			{
				final L2PcInstance pickedPlayer = players.get(Rnd.get(players.size()));
				if (pickedPlayer != null)
				{
					newlocx = pickedPlayer.getX() + Rnd.get(-60, 60);
					newlocy = pickedPlayer.getY() + Rnd.get(-60, 60);
					newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, pickedPlayer.getZ() - 20, pickedPlayer.getZ() + 20, _id);
					pickedPlayer.sendPacket(new ExShowScreenMessage("Watch out! Something spawned besides you!", 6000));
					_log.config("Spawning " + mob.getName() + " as a random spawn beside player " + pickedPlayer.getName());
					spawnedAsRandom = true;
					if (mob instanceof L2Attackable)
						((L2Attackable) mob).addDamageHate(pickedPlayer, 11000);
				}
			}
			if (!spawnedAsRandom)
			{
				_log.config("NOT Spawning " + mob.getName() + " as a random spawn");
			}
			startDoDieTimer(getRespawnDelay() + Rnd.get(60000));
		}
		if (!spawnedAsRandom)
		{
			// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
			if (getLocx() == 0 && getLocy() == 0)
			{
				if (getLocation() == 0)
					return mob;
				// Calculate the random position in the location area
				int p[] = Territory.getInstance().getRandomPoint(getLocation());
				// Set the calculated position of the L2NpcInstance
				newlocx = p[0];
				newlocy = p[1];
				newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, p[2], p[3], _id);
			}
			else
			{
				// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
				newlocx = getLocx();
				newlocy = getLocy();
				if (getRandomX() > 0)
				{
					newlocx += Rnd.get(-getRandomX(), getRandomX());
				}
				if (getRandomY() > 0)
				{
					newlocy += Rnd.get(-getRandomY(), getRandomY());
				}
				if (Config.GEODATA > 0)
					newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, getLocz(), getLocz(), _id);
				else
					newlocz = getLocz();
			}
		}
		_curX = newlocx;
		_curY = newlocy;
		_curZ = newlocz;
		for (L2Effect f : mob.getAllEffects())
		{
			if (f != null)
				mob.removeEffect(f);
		}
		mob.setIsDead(false);
		// Reset decay info
		mob.setDecayed(false);
		// Set the HP and MP of the L2NpcInstance to the max
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
		// Set the heading of the L2NpcInstance (random heading if not defined)
		if (getHeading() == -1)
		{
			mob.setHeading(Rnd.nextInt(61794));
		}
		else
		{
			mob.setHeading(getHeading());
		}
		// Link the L2NpcInstance to this L2Spawn
		mob.setSpawn(this);
		// mob.loadDropHTML();
		// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
		mob.spawnMe(newlocx, newlocy, newlocz);
		L2Spawn.notifyNpcSpawned(mob);
		_lastSpawn = mob;
		if (Config.DEBUG)
			_log.finest("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");
		// Increase the current number of L2NpcInstance managed by this L2Spawn
		_currentCount++;
		return mob;
	}
	
	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}
	
	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}
	
	public static void notifyNpcSpawned(L2Npc npc)
	{
		synchronized (_spawnListeners)
		{
			for (SpawnListener listener : _spawnListeners)
			{
				listener.npcSpawned(npc);
			}
		}
	}
	
	/**
	 * @param i
	 *            delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 0)
			_log.warning("respawn delay is negative for spawnId:" + _id);
		if (i < 10)
			i = 10;
		_respawnDelay = i * 1000;
	}
	
	public L2Npc getLastSpawn()
	{
		return _lastSpawn;
	}
	
	/**
	 * @param oldNpc
	 */
	public void respawnNpc(L2Npc oldNpc)
	{
		oldNpc.refreshID();
		intializeNpcInstance(oldNpc);
	}
	
	public L2NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	public void startUnspawnTimer(int seconds)
	{
		if (seconds < 2)
		{
			_log.severe("L O L startunspawntimer in l2spawn has seconds < 2!!!!!!!");
			return;
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				try
				{
					getLastSpawn().deleteMe();
					stopRespawn();
				}
				catch (Exception e)
				{}
			}
		}, seconds * 1000);
	}
	
	public void startDoDieTimer(int miliseconds)
	{
		if (miliseconds < 2000)
		{
			_log.severe("L O L startDODIEtimer in l2spawn has seconds < 2!!!!!!!");
			return;
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				try
				{
					if (!getLastSpawn().isInCombat())
						getLastSpawn().doDie(null);
				}
				catch (Exception e)
				{}
			}
		}, miliseconds);
	}
	
	public void setDeathDate(final long date)
	{
		_diedDate = date;
	}
	
	public long getDeathDate()
	{
		return _diedDate;
	}
}
