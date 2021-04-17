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
package net.sf.l2j.gameserver.model.actor.position;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.util.Point3D;

public class ObjectPosition
{
private static final Logger _log = Logger.getLogger(ObjectPosition.class.getName());

// =========================================================
// Data Field
private final L2Object _activeObject;
private int _heading    = 0;
private Point3D _worldPosition;
protected L2WorldRegion _worldRegion;         // Object localization : Used for items/chars that are seen in the world

// =========================================================
// Constructor
public ObjectPosition(L2Object activeObject)
{
	_activeObject = activeObject;
	setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
}

// =========================================================
// Method - Public
/**
 * Set the x,y,z position of the L2Object and if necessary modify its _worldRegion.<BR><BR>
 *
 * <B><U> Assert </U> :</B><BR><BR>
 * <li> _worldRegion != null</li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Update position during and after movement, or after teleport </li><BR>
 */
public final void setXYZ(int x, int y, int z)
{
	if (Config.ASSERT) assert getWorldRegion() != null;
	
	setWorldPosition(x, y ,z);
	
	try
	{
		if (L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion())
			updateWorldRegion();
	}
	catch (Exception e)
	{
		_log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
		badCoords();
	}
}
public final void setXYZTele(int x, int y, int z)
{
	if (Config.ASSERT) assert getWorldRegion() != null;
	
	setWorldPosition(x, y ,z);
	
	try
	{
		L2WorldRegion reg = L2World.getInstance().getRegion(getWorldPosition());
		
		if (reg != getWorldRegion())
			setWorldRegion(reg);
	}
	catch (Exception e)
	{
		_log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
		badCoords();
	}
}

/**
 * Called on setXYZ exception.<BR><BR>
 * <B><U> Overwritten in </U> :</B><BR><BR>
 * <li> CharPosition</li>
 * <li> PcPosition</li><BR>
 */
protected void badCoords()
{
	
}

/**
 * Set the x,y,z position of the L2Object and make it invisible.<BR><BR>
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * A L2Object is invisble if <B>_hidden</B>=true or <B>_worldregion</B>==null <BR><BR>
 *
 * <B><U> Assert </U> :</B><BR><BR>
 * <li> _worldregion==null <I>(L2Object is invisible)</I></li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Create a Door</li>
 * <li> Restore L2PcInstance</li><BR>
 */
public final void setXYZInvisible(int x, int y, int z)
{
	if (Config.ASSERT) assert getWorldRegion() == null;
	if (x > L2World.MAP_MAX_X) x = L2World.MAP_MAX_X - 5000;
	if (x < L2World.MAP_MIN_X) x = L2World.MAP_MIN_X + 5000;
	if (y > L2World.MAP_MAX_Y) y = L2World.MAP_MAX_Y - 5000;
	if (y < L2World.MAP_MIN_Y) y = L2World.MAP_MIN_Y + 5000;
	
	setWorldPosition(x, y ,z);
	getActiveObject().setIsVisible(false);
}

public final void setXYZInvisibleCS(int x, int y, int z)
{
	if (Config.ASSERT) assert getWorldRegion() == null;
	if (x > L2World.MAP_MAX_X) x = L2World.MAP_MAX_X - 5000;
	if (x < L2World.MAP_MIN_X) x = L2World.MAP_MIN_X + 5000;
	if (y > L2World.MAP_MAX_Y) y = L2World.MAP_MAX_Y - 5000;
	if (y < L2World.MAP_MIN_Y) y = L2World.MAP_MIN_Y + 5000;
	
	setWorldPosition(x, y ,z);
}

/**
 * checks if current object changed its region, if so, update referencies
 */
public void updateWorldRegion()
{
	if (!getActiveObject().isVisible()) return;
	
	L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
	if (newRegion != getWorldRegion())
	{
		getWorldRegion().removeVisibleObject(getActiveObject());
		
		setWorldRegion(newRegion);
		
		// Add the L2Oject spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
		getWorldRegion().addVisibleObject(getActiveObject());
	}
}
// =========================================================
// Method - Private

// =========================================================
// Property - Public
public L2Object getActiveObject()
{
	return _activeObject;
}

public final int getHeading() { return _heading; }
public final void setHeading(int value) { _heading = value; }

/** Return the x position of the L2Object. */
public final int getX() { return getWorldPosition().getX(); }
public final void setX(int value) { getWorldPosition().setX(value); }

/** Return the y position of the L2Object. */
public final int getY() { return getWorldPosition().getY(); }
public final void setY(int value) { getWorldPosition().setY(value); }

/** Return the z position of the L2Object. */
public final int getZ() { return getWorldPosition().getZ(); }
public final void setZ(int value) { getWorldPosition().setZ(value); }

public final Point3D getWorldPosition()
{
	if (_worldPosition == null)
	{
		_worldPosition = new Point3D(0, 0, 0);
	}
	return _worldPosition;
}
public final void setWorldPosition(int x, int y, int z)
{
	getWorldPosition().setXYZ(x,y,z);
}
public final void setWorldPosition(Point3D newPosition) { setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ()); }

public final L2WorldRegion getWorldRegion() { return _worldRegion; }
public void setWorldRegion(L2WorldRegion value)
{
	if (_worldRegion == value)
		return;
	
	if(_worldRegion != null && getActiveObject() instanceof L2Character) // confirm revalidation of old region's zones
	{
		if (value != null)
			_worldRegion.revalidateZones((L2Character)getActiveObject());    // at world region change
		else
			_worldRegion.removeFromZones((L2Character)getActiveObject());    // at world region change
	}
	
	_worldRegion = value;
}

/*public void setWorldRegionTeleport(L2WorldRegion value)
{
	if (_worldRegion == value)
		return;
	
	if(_worldRegion != null && getActiveObject() instanceof L2Character) // confirm revalidation of old region's zones
	{
		if (value != null)
		{
			System.out.println("Old region: "+_worldRegion.getName());
			System.out.println("New region: "+value.getName());
			_worldRegion.revalidateZonesTele((L2Character)getActiveObject());    // at world region change
		}
		else
			_worldRegion.removeFromZones((L2Character)getActiveObject());    // at world region change
	}
	
	_worldRegion = value;
}*/
}
