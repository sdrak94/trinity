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
package net.sf.l2j.util;

/**
 * @author FBIagent
 */
public final class LinePointIterator
{
	// src is moved towards dst in next()
	private int _srcX;
	private int _srcY;
	private final int _dstX;
	private final int _dstY;
	private final int _dx;
	private final int _dy;
	private final int _sx;
	private final int _sy;
	private int _err;
	private int _e2;
	private boolean _first;
	
	public LinePointIterator(final int srcX, final int srcY, final int dstX, final int dstY)
	{
		_srcX = srcX;
		_srcY = srcY;
		_dstX = dstX;
		_dstY = dstY;
		_dx = Math.abs(dstX - srcX);
		_sx = srcX < dstX ? 1 : -1;
		_dy = -Math.abs(dstY - srcY);
		_sy = srcY < dstY ? 1 : -1;
		_err = _dx + _dy;
		_e2 = 0;
		_first = true;
	}
	
	public boolean next()
	{
		if (_first)
		{
			_first = false;
			return true;
		}
		else if (_srcX != _dstX || _srcY != _dstY)
		{
			_e2 = 2 * _err;
			if (_e2 > _dy)
			{
				_err += _dy;
				_srcX += _sx;
			}
			if (_e2 < _dx)
			{
				_err += _dx;
				_srcY += _sy;
			}
			return true;
		}
		return false;
	}
	
	public int x()
	{
		return _srcX;
	}
	
	public int y()
	{
		return _srcY;
	}
}
