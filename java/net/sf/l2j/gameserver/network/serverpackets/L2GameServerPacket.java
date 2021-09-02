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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.logging.Logger;

import org.mmocore.network.SendablePacket;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.L2GameClient;

/**
 * @author KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	private static final Logger	_log			= Logger.getLogger(L2GameServerPacket.class.getName());
	L2GameClient				client			= getClient();
	protected boolean			_invisible		= false;
	private static final int[]	PAPERDOLL_ORDER	= new int[]
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_BACK,
		Inventory.PAPERDOLL_LRHAND,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT
	};
	
	/**
	 * @return True if packet originated from invisible character.
	 */
	public boolean isInvisible()
	{
		return _invisible;
	}
	
	/**
	 * Set "invisible" boolean flag in the packet.
	 * Packets from invisible characters will not be broadcasted to players.
	 * 
	 * @param b
	 */
	public void setInvisible(boolean b)
	{
		_invisible = b;
	}
	
	protected int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
	
	/**
	 * @see com.l2jserver.mmocore.network.SendablePacket#write()
	 */
	@Override
	protected void write()
	{
		try
		{
			// Announcements.getInstance().announceToAll(getClass().getSimpleName());
			// System.out.println(getClass().getSimpleName());
			// _client = getClient();
			L2GameClient client = getClient();
			L2GameClient _client = getClient();
			writeImpl();
		}
		catch (Exception e)
		{
			final String type = getType();
			if (type == "[S] 4e PartySmallWindowAll" || type == "[S] 4f PartySmallWindowAdd")
			{}
			else
			{
				_log.severe("Client: " + getClient().toString() + " - Failed writing: " + type + " - L2J Server Version: " + Config.SERVER_VERSION + " - DP Revision: " + Config.DATAPACK_VERSION);
				e.printStackTrace();
			}
		}
	}
	
	public void runImpl()
	{
		L2GameClient client = getClient();
		L2GameClient _client = getClient();
	}
	
	protected abstract void writeImpl();
	
	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public abstract String getType();
}
