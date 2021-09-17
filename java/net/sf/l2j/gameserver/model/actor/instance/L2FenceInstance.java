package net.sf.l2j.gameserver.model.actor.instance;
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

import java.awt.Rectangle;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExColosseumFenceInfoPacket;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;

/**
 * @author Nik
 */
public final class L2FenceInstance extends L2Object
{
	public static final int					HIDDEN			= 0;
	public static final int					UNCLOSED		= 1;
	public static final int					CLOSED			= 2;
	private String							_name;
	private final int						_xMin;
	private final int						_xMax;
	private final int						_yMin;
	private final int						_yMax;
	private int								_state;
	private final int						_width;
	private final int						_length;
	private int								_height;
	private Rectangle						_shape;
	private boolean							_storedToDB		= false;
	private int								_mapRegion		= -1;
	private int								_type;
	private int								_mapId;
	private Map<Integer, L2FenceInstance>	_staticItems	= new FastMap<Integer, L2FenceInstance>();
	
	public L2FenceInstance(int objectId, String name, int state, int x, int y, int z, int width, int length, int height, int eventId)
	{
		super(objectId);
		initPosition();
		setXYZ(x, y, z);
		if ((state < 0) || (state > 2))
		{
			state = 0;
		}
		_name = name;
		_type = state;
		_mapId = eventId;
		_state = state;
		_width = width;
		_length = length;
		_height = height;
		_xMin = x - (width / 2);
		_xMax = x + (width / 2);
		_yMin = y - (length / 2);
		_yMax = y + (length / 2);
		_shape = new Rectangle(getX(), getY(), _width, _length);
	}
	
	public L2FenceInstance(String name, int x, int y, int z, int width, int length)
	{
		super(IdFactory.getInstance().getNextId());
		initPosition();
		setXYZ(x, y, z);
		_name = name;
		_state = CLOSED; // Its a better default than HIDDEN.
		_xMin = x - (width / 2);
		_xMax = x + (width / 2);
		_yMin = y - (length / 2);
		_yMax = y + (length / 2);
		_width = width;
		_length = length;
		_height = 50;
		_shape = new Rectangle(getX(), getY(), _width, _length);
	}
	
	public boolean isLocInside(Location loc)
	{
		boolean isInsideZ = (loc.getZ() >= getZ()) && (loc.getZ() <= (getZ() + _height));
		return _shape.contains(loc.getX(), loc.getY());
	}
	
	public boolean isLocOutside(Location loc)
	{
		return !isLocInside(loc);
	}
	
	/**
	 * @param x1
	 *            - obj1's X loc
	 * @param y1
	 *            - obj1's Y loc
	 * @param x2
	 *            - obj2's X loc
	 * @param y2
	 *            - obj2's Y loc
	 * @return can obj1 see obj2 through this fence (their LOS do not intersect the fence's bounds). <B>No Z</B> checks are done. <B>No Instance</B> checks are done.
	 */
	public boolean canSee(int x1, int y1, int x2, int y2)
	{
		return !_shape.intersectsLine(x1, y1, x2, y2);
	}
	
	/**
	 * @param loc1
	 *            - obj1's xyz & instanceId location
	 * @param loc2
	 *            - obj2's xyz & instanceId location
	 * @return can obj1 see obj2 through this fence (their LOS do not intersect the fence's bounds). <B>Z</B> checks are done. <B>Instance</B> checks are done.
	 */
	public boolean canSee(Location loc1, Location loc2)
	{
		if ((getState() == CLOSED))
		{
			// Those Z checks are probably not 100% accurate, rework them if it must.
			int higherZ = loc1.getZ() >= loc2.getZ() ? loc1.getZ() : loc2.getZ();
			if ((higherZ >= getZ()) && (higherZ <= (getZ() + _height)))
			{
				return canSee(loc1.getX(), loc1.getY(), loc2.getX(), loc2.getY());
			}
		}
		return true;
	}
	
	/**
	 * @param obj1
	 * @param obj2
	 * @return can obj1 see obj2 through this fence (their LOS do not intersect the fence's bounds). <B>Z</B> checks are done. <B>Instance</B> checks are done.
	 */
	public boolean canSee(L2Object obj1, L2Object obj2)
	{
		if ((getState() == CLOSED) && (obj1.getInstanceId() == obj2.getInstanceId()))
		{
			// Those Z checks are probably not 100% accurate, rework them if it must.
			int higherZ = obj1.getZ() >= obj2.getZ() ? obj1.getZ() : obj2.getZ();
			if ((higherZ >= getZ()) && (higherZ <= (getZ() + _height)))
			{
				return canSee(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
			}
		}
		return true;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new ExColosseumFenceInfoPacket(this));
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	/**
	 * Also removes the fence from FencesTable, but its not removed from DB
	 */
	public void decay()
	{
		FencesTable.getInstance().removeFence(getObjectId());
	}
	
	@Override
	public void onSpawn()
	{
		// Maybe someone for some reason decided to respawn this fence on different XYZ. RESHAPE!!!
		if (this != null)
		{
			_shape = new Rectangle(getX(), getY(), _width, _length);
			FencesTable.getInstance().addFence(this);
		}
	}
	
	/**
	 * @param height
	 *            - how high the fence is :D :D Although the client height of the fence is ~50, you can set it to as much as you want. <BR>
	 *            Setting it to lower might result in some strange LOS checks. Setting it too high might result in weird LOS checks in all the floors of TOI :P
	 */
	public void setHeight(int height)
	{
		_height = height;
	}
	
	public void setState(int state)
	{
		if ((state < 0) || (state > 2))
		{
			state = 0;
		}
		_state = state;
	}
	
	public int getState()
	{
		return _state;
	}
	
	public int getWidth()
	{
		return _width;
	}
	
	public int getLength()
	{
		return _length;
	}
	
	public int getHeight()
	{
		return _height;
	}
	
	public int getXMin()
	{
		return _xMin;
	}
	
	public int getYMin()
	{
		return _yMin;
	}
	
	public int getXMax()
	{
		return _xMax;
	}
	
	public int getYMax()
	{
		return _yMax;
	}
	
	public int getMapRegion()
	{
		return _mapRegion;
	}
	
	public void setMapRegion(int region)
	{
		_mapRegion = region;
	}
	
	public void setStoredToDB(boolean storedToDB)
	{
		_storedToDB = storedToDB;
	}
	
	public boolean isStoredToDB()
	{
		return _storedToDB;
	}
	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;
		
		if (player.getAccessLevel().isGm())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			player.sendMessage("ObjectId of targeted fence: " + getObjectId());
		}
		else
			player.sendPacket(ActionFailed.STATIC_PACKET);
	}


	
	@Override
	public boolean isFence()
	{
		return true;
	}
}