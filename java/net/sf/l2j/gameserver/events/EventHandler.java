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
package net.sf.l2j.gameserver.events;

/**
 * @author Layane
 *
 */
public abstract class EventHandler
{
	private Object _owner;
	
	public EventHandler(Object owner)
	{
		_owner = owner;
	}
	
	public final Object getOwner()
	{
		return _owner;
	}
	
	@Override
	public final boolean equals(Object object)
	{
		if (object instanceof EventHandler && _owner == ((EventHandler) object)._owner)
			return true;
		return false;
	}
	
	public abstract void handler(Object trigger, IEventParams params);
	
}
