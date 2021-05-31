/*
 * Copyright (C) 2004-2014 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 * Object world location storage interface.
 * @author xban1x
 */
public interface ILocational
{
	/**
	 * Gets the X coordinate of this object.
	 * @return the X coordinate
	 */
	int getX();
	
	/**
	 * Gets the Y coordinate of this object.
	 * @return the current Y coordinate
	 */
	int getY();
	
	/**
	 * Gets the Z coordinate of this object.
	 * @return the current Z coordinate
	 */
	int getZ();
	
	/**
	 * Gets the heading of this object.
	 * @return the current heading
	 */
	int getHeading();
	
	/**
	 * Gets the instance zone ID of this object.
	 * @return the ID of the instance zone this object is currently in (0 - not in any instance)
	 */
	int getInstanceWorld();
	
	/**
	 * Gets this object's location.
	 * @return a {@link ILocational} object containing the current position of this object
	 */
	ILocational getLocation();
	
	/**
	 * @param to
	 * @return the heading to the target specified
	 */
	default int calculateHeadingTo(ILocational to)
	{
		return Util.calculateHeadingFrom(getX(), getY(), to.getX(), to.getY());
	}
	
	/**
	 * @param target
	 * @return {@code true} if this location is in front of the target location based on the game's concept of position.
	 */
	default boolean isInFrontOf(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.FRONT == Position.getPosition(this, target);
	}
	
	/**
	 * @param target
	 * @return {@code true} if this location is in one of the sides of the target location based on the game's concept of position.
	 */
	default boolean isOnSideOf(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.SIDE == Position.getPosition(this, target);
	}
	
	/**
	 * @param target
	 * @return {@code true} if this location is behind the target location based on the game's concept of position.
	 */
	default boolean isBehind(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.BACK == Position.getPosition(this, target);
	}
	
	public default Location randomize(int min, int max)
	{
		final int randX = Rnd.get(min, max) * (Rnd.nextBoolean() ? 1 : -1);
		final int randY = Rnd.get(min, max) * (Rnd.nextBoolean() ? 1 : -1);
		return new Location(getX() + randX, getY() + randY, getZ());
	}
	
	/**
	 * Return the squared distance between the current position of the L2Character
	 * and the given object.
	 * 
	 * @param object L2Object
	 * @return the squared distance
	 */
	public default double getDistanceSq(final L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}

	/**
	 * Return the squared distance between the current position of the L2Character
	 * and the given x, y, z.
	 * 
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @return the squared distance
	 */
	public default double getDistanceSq(final int x, final int y, final int z)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Return the squared plan distance between the current position of the
	 * L2Character and the given x, y, z.<BR>
	 * (check only x and y, not z)
	 * 
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @return the squared plan distance
	 */
	public default double getPlanDistanceSq(final int x, final int y)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		return dx * dx + dy * dy;
	}

	/**
	 * Check if this object is inside the given radius around the given object.
	 * Warning: doesn't cover collision radius!
	 * 
	 * @param object      the target
	 * @param radius      the radius around the target
	 * @param checkZ      should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public default boolean isInsideRadius(final ILocational object, final int radius, final boolean checkZ, final boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}

	/**
	 * Check if this object is inside the given plan radius around the given point.
	 * Warning: doesn't cover collision radius!
	 * 
	 * @param x           X position of the target
	 * @param y           Y position of the target
	 * @param radius      the radius around the target
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public default boolean isInsideRadius(final int x, final int y, final int radius, final boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}
	
	public default boolean isInsideRadius(final ILocational loc, final int radius, final boolean strictCheck)
	{
		return isInsideRadius(loc.getX(), loc.getY(), 0, radius, false, strictCheck);
	}

	/**
	 * Check if this object is inside the given radius around the given point.
	 * 
	 * @param x           X position of the target
	 * @param y           Y position of the target
	 * @param z           Z position of the target
	 * @param radius      the radius around the target
	 * @param checkZ      should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public default boolean isInsideRadius(final int x, final int y, final int z, final int radius, final boolean checkZ, final boolean strictCheck)
	{
		final double dx = x - getX();
		final double dy = y - getY();
		final double dz = z - getZ();
		if (strictCheck)
		{
			if (checkZ)
				return dx * dx + dy * dy + dz * dz < radius * radius;
			return dx * dx + dy * dy < radius * radius;
		}
		if (checkZ)
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		return dx * dx + dy * dy <= radius * radius;
	}
}

