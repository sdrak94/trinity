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
package net.sf.l2j.gameserver.model.zone.form;

import net.sf.l2j.gameserver.model.zone.L2ZoneForm;

/**
 * A primitive rectangular zone
 *
 *
 * @author  durgus
 */
public class ZoneCuboid extends L2ZoneForm
{
	private int _x1, _x2, _y1, _y2, _z1, _z2;
	
	public ZoneCuboid(int x1, int x2, int y1, int y2, int z1, int z2)
	{
		_x1 = x1;
		_x2 = x2;
		if (_x1 > _x2) // switch them if alignment is wrong
		{
			_x1 = x2;
			_x2 = x1;
		}
		
		_y1 = y1;
		_y2 = y2;
		if (_y1 > _y2) // switch them if alignment is wrong
		{
			_y1 = y2;
			_y2 = y1;
		}
		
		_z1 = z1;
		_z2 = z2;
		if (_z1 > _z2) // switch them if alignment is wrong
		{
			_z1 = z2;
			_z2 = z1;
		}
	}
	
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		if (x < _x1 || x > _x2 || y < _y1 || y > _y2 || z < _z1 || z > _z2)
			return false;
		return true;
	}
	
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		// Check if any point inside this rectangle
		if (isInsideZone(ax1, ay1, (_z2 - 1)))
			return true;
		if (isInsideZone(ax1, ay2, (_z2 - 1)))
			return true;
		if (isInsideZone(ax2, ay1, (_z2 - 1)))
			return true;
		if (isInsideZone(ax2, ay2, (_z2 - 1)))
			return true;
		
		// Check if any point from this rectangle is inside the other one
		if (_x1 > ax1 && _x1 < ax2 && _y1 > ay1 && _y1 < ay2)
			return true;
		if (_x1 > ax1 && _x1 < ax2 && _y2 > ay1 && _y2 < ay2)
			return true;
		if (_x2 > ax1 && _x2 < ax2 && _y1 > ay1 && _y1 < ay2)
			return true;
		if (_x2 > ax1 && _x2 < ax2 && _y2 > ay1 && _y2 < ay2)
			return true;
		
		// Horizontal lines may intersect vertical lines
		if (lineSegmentsIntersect(_x1, _y1, _x2, _y1, ax1, ay1, ax1, ay2))
			return true;
		if (lineSegmentsIntersect(_x1, _y1, _x2, _y1, ax2, ay1, ax2, ay2))
			return true;
		if (lineSegmentsIntersect(_x1, _y2, _x2, _y2, ax1, ay1, ax1, ay2))
			return true;
		if (lineSegmentsIntersect(_x1, _y2, _x2, _y2, ax2, ay1, ax2, ay2))
			return true;
		
		// Vertical lines may intersect horizontal lines
		if (lineSegmentsIntersect(_x1, _y1, _x1, _y2, ax1, ay1, ax2, ay1))
			return true;
		if (lineSegmentsIntersect(_x1, _y1, _x1, _y2, ax1, ay2, ax2, ay2))
			return true;
		if (lineSegmentsIntersect(_x2, _y1, _x2, _y2, ax1, ay1, ax2, ay1))
			return true;
		if (lineSegmentsIntersect(_x2, _y1, _x2, _y2, ax1, ay2, ax2, ay2))
			return true;
		
		return false;
	}
	
	@Override
	public double getDistanceToZone(int x, int y)
	{
		double test, shortestDist = Math.pow(_x1 - x, 2) + Math.pow(_y1 - y, 2);
		
		test = Math.pow(_x1 - x, 2) + Math.pow(_y2 - y, 2);
		if (test < shortestDist)
			shortestDist = test;
		
		test = Math.pow(_x2 - x, 2) + Math.pow(_y1 - y, 2);
		if (test < shortestDist)
			shortestDist = test;
		
		test = Math.pow(_x2 - x, 2) + Math.pow(_y2 - y, 2);
		if (test < shortestDist)
			shortestDist = test;
		
		return Math.sqrt(shortestDist);
	}
	
	/* getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new
	 * fishing algorithms, wich are now able to correctly place the hook in the water, thanks to getHighZ().
	 * getLowZ() was added, considering potential future modifications.
	 */
	@Override
	public int getLowZ()
	{
		return _z1;
	}
	
	@Override
	public int getHighZ()
	{
		return _z2;
	}
	@Override
	public void visualizeZone(final int z)
	{
		// x1->x2
		for (int x = _x1; x < _x2; x = x + STEP)
		{
			dropDebugItem(57, 1, x, _y1, z);
			dropDebugItem(57, 1, x, _y2, z);
		}
		// y1->y2
		for (int y = _y1; y < _y2; y = y + STEP)
		{
			dropDebugItem(57, 1, _x1, y, z);
			dropDebugItem(57, 1, _x2, y, z);
		}
	}
}
