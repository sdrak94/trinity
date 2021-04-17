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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.AgitDecoInfo;

/**
 * A clan hall zone
 *
 * @author  durgus
 */
public class L2ClanHallZone extends L2ZoneType
{
private int _clanHallId;
private int[] _spawnLoc;

public L2ClanHallZone(int id)
{
	super(id);
	
	_spawnLoc = new int[3];
}

@Override
public void setParameter(String name, String value)
{
	if (name.equals("clanHallId"))
	{
		_clanHallId = Integer.parseInt(value);
		// Register self to the correct clan hall
		ClanHallManager.getInstance().getClanHallById(_clanHallId).setZone(this);
	}
	else if (name.equals("spawnX"))
	{
		_spawnLoc[0] = Integer.parseInt(value);
	}
	else if (name.equals("spawnY"))
	{
		_spawnLoc[1] = Integer.parseInt(value);
	}
	else if (name.equals("spawnZ"))
	{
		_spawnLoc[2] = Integer.parseInt(value);
	}
	else
		super.setParameter(name, value);
}

@Override
protected void onEnter(L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		// Set as in clan hall
		character.setInsideZone(L2Character.ZONE_CLANHALL, true);
		
		ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_clanHallId);
		if (clanHall == null)
			return;
		
		// Send decoration packet
		AgitDecoInfo deco = new AgitDecoInfo(clanHall);
		((L2PcInstance) character).sendPacket(deco);
		
	}
}

@Override
protected void onExit(L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		// Unset clanhall zone
		character.setInsideZone(L2Character.ZONE_CLANHALL, false);
		
	}
}

@Override
public void onDieInside(L2Character character)
{
}

@Override
public void onReviveInside(L2Character character)
{
}

/**
 * Removes all foreigners from the clan hall
 * @param owningClanId
 */
public void banishForeigners(int owningClanId)
{
	for (L2Character temp : _characterList.values())
	{
		if (!(temp instanceof L2PcInstance))
			continue;
		
		if (temp.isGM())
			continue;
		
		if (!temp.getActingPlayer().isCursedWeaponEquipped() && ((L2PcInstance) temp).getClanId() == owningClanId)
			continue;
		
		((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
	}
}

/**
 * Get the clan hall's spawn
 * @return
 */
public Location getSpawn()
{
	return new Location(_spawnLoc[0], _spawnLoc[1], _spawnLoc[2]);
}
}
