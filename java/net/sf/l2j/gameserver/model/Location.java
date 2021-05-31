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
package net.sf.l2j.gameserver.model;

import net.sf.l2j.util.Rnd;

/**
 * Location data transfer object.<br>
 * Contains coordinates data, heading and instance Id.
 * @author Zoey76
 */
public class Location implements IPositionable
{
	protected volatile int _x;
	protected volatile int _y;
	protected volatile int _z;
	private volatile int _heading;
	private volatile int _world;
	
	public Location()
	{
		this(0, 0, 0);
	}
	
	public Location(int x, int y, int z)
	{
		this(x, y, z, 0, 0);
	}
	
	public Location(int x, int y, int z, int heading)
	{
		this(x, y, z, heading, 0);
	}
	
	public Location(L2Object obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ(), obj.getInstanceId(), obj.getInstanceId());
	}
	
	public Location(int x, int y, int z, int heading, int world)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
		_world = world;
	}
	
	
	public Location(final int[] data)
	{
		this(data[0], data[1], data[2]);
	}
	
	public Location(ILocational loc)
	{
		this(loc.getX(), loc.getY(), loc.getZ(), 0, loc.getInstanceWorld());
	}
	
	public Location(final Location loc, int offset)
	{
		this(loc.getX() + Rnd.get(-offset, +offset), loc.getY() + Rnd.get(-offset, +offset), loc.getZ(), loc.getHeading());
	}
	
	/**
	 * Get the x coordinate.
	 * @return the x coordinate
	 */
	@Override
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Get the y coordinate.
	 * @return the y coordinate
	 */
	@Override
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Get the z coordinate.
	 * @return the z coordinate
	 */
	@Override
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Set the x, y, z coordinates.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 */
	public void setXYZ(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	public void setX(final int x)
	{
		_x = x;
	}

	public void setY(final int y)
	{
		_y = y;
	}

	public void setZ(final int z)
	{
		_z = z;
	}
	
	/**
	 * Set the x, y, z coordinates.
	 * @param loc The location.
	 */
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Get the heading.
	 * @return the heading
	 */
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Set the heading.
	 * @param heading the heading
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Get the instance Id.
	 * @return the instance Id
	 */
	@Override
	public int getInstanceWorld()
	{
		return _world;
	}
	
	/**
	 * Set the instance Id.
	 * @param instanceId the instance Id to set
	 */
	@Override
	public void setInstanceWorld(final int instanceWorld)
	{
		_world = instanceWorld;
	}
	
	@Override
	public IPositionable getLocation()
	{
		return this;
	}
	
	@Override
	public void setLocation(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
		_world = loc.getInstanceWorld();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Location))
		{
			return false;
		}
		final Location loc = (Location) obj;
		return (getX() == loc.getX()) && (getY() == loc.getY()) && (getZ() == loc.getZ()) && (getHeading() == loc.getHeading()) && (getInstanceWorld() == loc.getInstanceWorld());
	}
	
	public boolean equals(final int x, final int y, final int z)
	{
		return _x == x && _y == y && _z == z;
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] X: " + _x + " Y: " + _y + " Z: " + _z + " Heading: " + _heading + " InstanceId: " + _world;
	}
}
