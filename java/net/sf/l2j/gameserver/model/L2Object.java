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

import luna.PlayerPassport;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.ObjectKnownList;
import net.sf.l2j.gameserver.model.actor.poly.ObjectPoly;
import net.sf.l2j.gameserver.model.actor.position.ObjectPosition;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

/**
 * Mother class of all objects in the world wich ones is it possible
 * to interact (PC, NPC, Item...)<BR>
 * <BR>
 * L2Object :<BR>
 * <BR>
 * <li>L2Character</li>
 * <li>L2ItemInstance</li>
 * <li>L2Potion</li>
 */
public abstract class L2Object
{
	// =========================================================
	// Data Field
	private boolean			_isVisible;		// Object visibility
	private ObjectKnownList	_knownList;
	private String			_name;
	private int				_objectId;		// Object identifier
	private ObjectPoly		_poly;
	private ObjectPosition	_position;
	private int				_instanceId	= 0;
	
	// =========================================================
	// Constructor
	public L2Object(int objectId)
	{
		_objectId = objectId;
		initKnownList();
		initPosition();
	}
	
	// =========================================================
	// Event - Public
	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onActionShift(L2GameClient client)
	{
		client.getActiveChar().sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onForcedAttack(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public boolean isInFunEvent()
	{
		final L2PcInstance player = getActingPlayer();
		return (player != null && player.isInFunEvent());
	}
	
	/**
	 * Do Nothing.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the home location of its L2GuardInstance</li>
	 * <li>L2Attackable : Reset the Spoiled flag</li><BR>
	 * <BR>
	 */
	public void onSpawn()
	{
		//if (this instanceof L2FenceInstance || this instanceof L2FenceNexusInstance)
		//	System.out.println(this.getObjectId());
	}
	
	// =========================================================
	// Position - Should remove to fully move to L2ObjectPosition
	public final void setXYZ(int x, int y, int z)
	{
		getPosition().setXYZ(x, y, z);
	}
	
	public final void setXYZInvisible(int x, int y, int z)
	{
		getPosition().setXYZInvisible(x, y, z);
	}
	
	public final void setXYZInvisibleCS(int x, int y, int z)
	{
		getPosition().setXYZInvisibleCS(x, y, z);
	}
	
	public final int getX()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getX();
	}
	
	/**
	 * @return The id of the instance zone the object is in - id 0 is global
	 *         since everything like dropped items, mobs, players can be in a instanciated area, it must be in l2object
	 */
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * @param instanceId
	 *            The id of the instance zone the object is in - id 0 is global
	 */
	public void setInstanceId(int instanceId)
	{
		if (_instanceId == instanceId)
			return;
		if (this instanceof L2PcInstance)
		{
			if (_instanceId > 0)
			{
				final Instance instanceJustLeft = InstanceManager.getInstance().getInstance(_instanceId);
				instanceJustLeft.removePlayer(getObjectId());
				if (this instanceof L2PcInstance && instanceJustLeft.getGearLimit() >= 0)
					getActingPlayer()._gearLimit = -1;
			}
			if (instanceId > 0)
			{
				final Instance instanceJustEntered = InstanceManager.getInstance().getInstance(instanceId);
				instanceJustEntered.addPlayer(getObjectId());
				if (this instanceof L2PcInstance && instanceJustEntered.getGearLimit() >= 0)
				{
					final int gearLimit = instanceJustEntered.getGearLimit();
					getActingPlayer()._gearLimit = gearLimit;
					if (!getActingPlayer().isGM())
						getActingPlayer().checkItemRestrictionZone(gearLimit);
					String name = "";
					switch (gearLimit)
					{
						case 0:
							name = "S grade only zone";
							break;
						case 1:
							name = "S80 and below zone";
							break;
						case 2:
							name = "Vesper and below zone";
							break;
						case 3:
							name = "Titanium and below zone";
							break;
						case 4:
							name = "Dread and below zone";
							break;
						case 5:
							name = "Forbidden and below zone";
							break;
					}
					getActingPlayer().sendMessage("You have entered a " + name);
				}
			}
			if (((L2PcInstance) this).getPet() != null)
				((L2PcInstance) this).getPet().setInstanceId(instanceId);
		}
		else if (this instanceof L2Npc)
		{
			if (_instanceId > 0)
				InstanceManager.getInstance().getInstance(_instanceId).removeNpc(((L2Npc) this).getSpawn());
			if (instanceId > 0)
				
				InstanceManager.getInstance().getInstance(instanceId).addNpc(((L2Npc) this));
		}
		
		_instanceId = instanceId;
		// If we change it for visible objects, me must clear & revalidate knownlists
		if (_isVisible && _knownList != null)
		{
			if (this instanceof L2PcInstance)
			{
				// We don't want some ugly looking disappear/appear effects, so don't update
				// the knownlist here, but players usually enter instancezones through teleporting
				// and the teleport will do the revalidation for us.
			}
			else
			{
				decayMe();
				spawnMe();
			}
		}
	}
	
	public final int getY()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getY();
	}
	
	public final int getZ()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getZ();
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Remove a L2Object from the world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Object from the world</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion != null <I>(L2Object is visible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Delete NPC/PC or Unsummon</li><BR>
	 * <BR>
	 */
	public final void decayMe()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null;
		L2WorldRegion reg = getPosition().getWorldRegion();
		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}
		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Remove the L2Object from the world
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
		if (Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance().removeObject(this);
	}
	
	public final void decayMeSummon()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null;
		L2WorldRegion reg = getPosition().getWorldRegion();
		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}
		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Remove the L2Object from the world
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
		// L2World.getInstance().removePet(this.getActingPlayer().getObjectId());
	}
	
	public final void decayMeTeleport(int x, int y, int z)
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null;
		L2World.getInstance().removeVisibleObject(this, getPosition().getWorldRegion());
	}
	
	public void refreshID()
	{
		L2World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}
	
	public Location getLoc()
	{
		return new Location(getX(), getY(), getZ(), 0);
	}
	
	/**
	 * Init the position of a L2Object spawn and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2Object spawn and update its _worldregion</li>
	 * <li>Add the L2Object spawn in the _allobjects of L2World</li>
	 * <li>Add the L2Object spawn to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2Object spawn in the world as a <B>visible</B> object</li><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Create Door</li>
	 * <li>Spawn : Monster, Minion, CTs, Summon...</li><BR>
	 */
	public final void spawnMe()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() == null && getPosition().getWorldPosition().getX() != 0 && getPosition().getWorldPosition().getY() != 0 && getPosition().getWorldPosition().getZ() != 0;
		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			// Add the L2Object spawn in the _allobjects of L2World
			L2World.getInstance().storeObject(this);
			// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getPosition().getWorldRegion().addVisibleObject(this);
		}
		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
		onSpawn();
	}
	
	public final void spawnMeNoOnSpawn()
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() == null && getPosition().getWorldPosition().getX() != 0 && getPosition().getWorldPosition().getY() != 0 && getPosition().getWorldPosition().getZ() != 0;
		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			// Add the L2Object spawn in the _allobjects of L2World
			L2World.getInstance().storeObject(this);
			// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getPosition().getWorldRegion().addVisibleObject(this);
		}
		// this can synchronize on others instancies, so it's out of
		// synchronized, to avoid deadlocks
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
	}
	
	public final void spawnMe(int x, int y, int z)
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() == null;
		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			if (x > L2World.MAP_MAX_X)
				x = L2World.MAP_MAX_X - 5000;
			else if (x < L2World.MAP_MIN_X)
				x = L2World.MAP_MIN_X + 5000;
			if (y > L2World.MAP_MAX_Y)
				y = L2World.MAP_MAX_Y - 5000;
			else if (y < L2World.MAP_MIN_Y)
				y = L2World.MAP_MIN_Y + 5000;
			getPosition().setWorldPosition(x, y, z);
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			// Add the L2Object spawn in the _allobjects of L2World
		}
		if (this instanceof L2Npc)
		{
			final L2Npc npc = (L2Npc) this;
			npc.setCollisionHeight(npc.getCollisionHeight());
			npc.setCollisionRadius(npc.getCollisionRadius());
		}
		L2World.getInstance().storeObject(this);
		// these can synchronize on others instancies, so they're out of
		// synchronized, to avoid deadlocks
		// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
		getPosition().getWorldRegion().addVisibleObject(this);
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
		onSpawn();
	}
	
	public final void spawnMeTeleport(int x, int y, int z)
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() == null;
		if (this instanceof L2Npc)
		{
			final L2Npc npc = (L2Npc) this;
			npc.setCollisionHeight(npc.getCollisionHeight());
			npc.setCollisionRadius(npc.getCollisionRadius());
		}
		// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
		getPosition().getWorldRegion().addVisibleObject(this);
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
		onSpawn();
	}
	
	public void toggleVisible()
	{
		if (isVisible())
			decayMe();
		else
			spawnMe();
	}
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	public boolean isAttackable()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(L2Character attacker);
	
	public boolean isMarker()
	{
		return false;
	}
	
	/**
	 * Return the visibilty state of the L2Object. <BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Object is visble if <B>__IsVisible</B>=true and <B>_worldregion</B>!=null <BR>
	 * <BR>
	 */
	public final boolean isVisible()
	{
		// return getPosition().getWorldRegion() != null && _IsVisible;
		return getPosition().getWorldRegion() != null;
	}
	
	public final void setIsVisible(boolean value)
	{
		_isVisible = value;
		if (!_isVisible)
			getPosition().setWorldRegion(null);
	}
	
	public final boolean isSpawned()
	{
		return _isVisible;
	}
	
	public ObjectKnownList getKnownList()
	{
		return _knownList;
	}
	
	/**
	 * Initializes the KnownList of the L2Object,
	 * is overwritten in classes that require a different knownlist Type.
	 * Removes the need for instanceof checks.
	 */
	public void initKnownList()
	{
		_knownList = new ObjectKnownList(this);
	}
	
	public final void setKnownList(ObjectKnownList value)
	{
		_knownList = value;
	}
	
	public final String getName()
	{
		return _name;
	}
	public final void setName(String value)
	{
		_name = value;
	}
	
	public final int getObjectId()
	{
		return _objectId;
	}
	
	public final ObjectPoly getPoly()
	{
		if (_poly == null)
			_poly = new ObjectPoly(this);
		return _poly;
	}
	
	public ObjectPosition getPosition()
	{
		return _position;
	}
	
	/**
	 * Initializes the Position class of the L2Object,
	 * is overwritten in classes that require a different position Type.
	 * Removes the need for instanceof checks.
	 */
	public void initPosition()
	{
		_position = new ObjectPosition(this);
	}
	
	public final void setObjectPosition(ObjectPosition value)
	{
		_position = value;
	}
	
	/**
	 * returns reference to region this object is in
	 */
	public L2WorldRegion getWorldRegion()
	{
		return getPosition().getWorldRegion();
	}
	
	public L2PcInstance getActingPlayer()
	{
		return null;
	}
	
	/**
	 * Sends the Server->Client info packet for the object.<br>
	 * <br>
	 * Is Overridden in:
	 * <li>L2AirShipInstance</li>
	 * <li>L2BoatInstance</li>
	 * <li>L2DoorInstance</li>
	 * <li>L2PcInstance</li>
	 * <li>L2StaticObjectInstance</li>
	 * <li>L2Decoy</li>
	 * <li>L2Npc</li>
	 * <li>L2Summon</li>
	 * <li>L2Trap</li>
	 * <li>L2ItemInstance</li>
	 */
	public void sendInfo(L2PcInstance activeChar)
	{}
	
	@Override
	public String toString()
	{
		return "" + getObjectId();
	}
	
	public double getHpPercent()
	{
		return 100;
	}
	public PlayerPassport getPassport()
	{
		return null;
	}

	public boolean isFence()
	{
		return false;
	}
}