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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.zone.type.L2ArenaZone;
import net.sf.l2j.gameserver.model.zone.type.L2ClanHallZone;

/**
 * This class ...
 */
public class MapRegionTable
{
private static Logger _log = Logger.getLogger(MapRegionTable.class.getName());

private final int[][] _regions = new int[16][18];

private final int[][] _pointsWithKarmas;

public static enum TeleportWhereType
{
Castle,
ClanHall,
SiegeFlag,
Town,
Fortress
}

public static MapRegionTable getInstance()
{
	return SingletonHolder._instance;
}

private MapRegionTable()
{
	int count2 = 0;
	
	//LineNumberReader lnr = null;
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT region, sec0, sec1, sec2, sec3, sec4, sec5, sec6, sec7, sec8, sec9, sec10,sec11,sec12,sec13,sec14,sec15 FROM mapregion");
		ResultSet rset = statement.executeQuery();
		int region;
		while (rset.next())
		{
			region = rset.getInt(1);
			
			for (int j = 0; j < 16; j++)
			{
				_regions[j][region] = rset.getInt(j + 2);
				count2++;
				//_log.fine(j+","+region+" -> "+rset.getInt(j+2));
			}
		}
		
		rset.close();
		statement.close();
		if (Config.DEBUG)
			_log.fine(count2 + " mapregion loaded");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error loading Map Region Table.", e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
	
	_pointsWithKarmas = new int[21][3];
	//Talking Island
	_pointsWithKarmas[0][0] = -79077;
	_pointsWithKarmas[0][1] = 240355;
	_pointsWithKarmas[0][2] = -3440;
	//Elven
	_pointsWithKarmas[1][0] = 43503;
	_pointsWithKarmas[1][1] = 40398;
	_pointsWithKarmas[1][2] = -3450;
	//DarkElven
	_pointsWithKarmas[2][0] = 1675;
	_pointsWithKarmas[2][1] = 19581;
	_pointsWithKarmas[2][2] = -3110;
	//Orc
	_pointsWithKarmas[3][0] = -44413;
	_pointsWithKarmas[3][1] = -121762;
	_pointsWithKarmas[3][2] = -235;
	//Dwalf
	_pointsWithKarmas[4][0] = 12009;
	_pointsWithKarmas[4][1] = -187319;
	_pointsWithKarmas[4][2] = -3309;
	//Gludio
	_pointsWithKarmas[5][0] = -18872;
	_pointsWithKarmas[5][1] = 126216;
	_pointsWithKarmas[5][2] = -3280;
	//Gludin
	_pointsWithKarmas[6][0] = -85915;
	_pointsWithKarmas[6][1] = 150402;
	_pointsWithKarmas[6][2] = -3060;
	//Dion
	_pointsWithKarmas[7][0] = 23652;
	_pointsWithKarmas[7][1] = 144823;
	_pointsWithKarmas[7][2] = -3330;
	//Giran
	_pointsWithKarmas[8][0] = 79125;
	_pointsWithKarmas[8][1] = 154197;
	_pointsWithKarmas[8][2] = -3490;
	//Oren
	_pointsWithKarmas[9][0] = 73840;
	_pointsWithKarmas[9][1] = 58193;
	_pointsWithKarmas[9][2] = -2730;
	//Aden
	_pointsWithKarmas[10][0] = 44413;
	_pointsWithKarmas[10][1] = 22610;
	_pointsWithKarmas[10][2] = 235;
	//Hunters
	_pointsWithKarmas[11][0] = 114137;
	_pointsWithKarmas[11][1] = 72993;
	_pointsWithKarmas[11][2] = -2445;
	//Giran
	_pointsWithKarmas[12][0] = 79125;
	_pointsWithKarmas[12][1] = 154197;
	_pointsWithKarmas[12][2] = -3490;
	// heine
	_pointsWithKarmas[13][0] = 119536;
	_pointsWithKarmas[13][1] = 218558;
	_pointsWithKarmas[13][2] = -3495;
	// Rune Castle Town
	_pointsWithKarmas[14][0] = 42931;
	_pointsWithKarmas[14][1] = -44733;
	_pointsWithKarmas[14][2] = -1326;
	// Goddard
	_pointsWithKarmas[15][0] = 147419;
	_pointsWithKarmas[15][1] = -64980;
	_pointsWithKarmas[15][2] = -3457;
	// Schuttgart
	_pointsWithKarmas[16][0] = 85184;
	_pointsWithKarmas[16][1] = -138560;
	_pointsWithKarmas[16][2] = -2256;
	// Kamael Village
	_pointsWithKarmas[19][0] = -121425;
	_pointsWithKarmas[19][1] = 59778;
	_pointsWithKarmas[19][2] = -2264;
}

public final int getMapRegion(int posX, int posY)
{
	try
	{
		return _regions[getMapRegionX(posX)][getMapRegionY(posY)];
	}
	catch (ArrayIndexOutOfBoundsException e)
	{
		// Position sent is outside MapRegionTable area.
		if (Config.DEBUG)
			_log.log(Level.WARNING, "MapRegionTable: Player outside map regions at X,Y=" + posX + "," + posY, e);
		return 0;
	}
}

public final int getMapRegionX(int posX)
{
	return (posX >> 15) + 9;// + centerTileX;
}

public final int getMapRegionY(int posY)
{
	return (posY >> 15) + 10;// + centerTileX;
}

public int getAreaCastle(L2Character activeChar)
{
	final int area = getClosestTownNumber(activeChar);
	int castle;
	switch (area)
	{
	case 0:
		castle = 2;
		break;//Talking Island Village
	case 1:
		castle = 4;
		break; //Elven Village
	case 2:
		castle = 8;
		break; //Dark Elven Village
	case 3:
		castle = 8;
		break; //Orc Village
	case 4:
		castle = 9;
		break; //Dwarven Village
	case 5:
		castle = 1;
		break; //Town of Gludio
	case 6:
		castle = 1;
		break; //Gludin Village
	case 7:
		castle = 2;
		break; //Town of Dion
	case 8:
		castle = 3;
		break; //Town of Giran
	case 9:
		castle = 4;
		break; //Town of Oren
	case 10:
		castle = 5;
		break; //Town of Aden
	case 11:
		castle = 5;
		break; //Hunters Village
	case 12:
		castle = 3;
		break; //Giran Harbor
	case 13:
		castle = 6;
		break; //Heine
	case 14:
		castle = 8;
		break; //Rune Township
	case 15:
		castle = 7;
		break; //Town of Goddard
	case 16:
		castle = 9;
		break; //Town of Shuttgart
	case 17:
		castle = 4;
		break; //Ivory Tower
	case 18:
		castle = 8;
		break; //Primeval Isle Wharf
	case 19:
		castle = 5;
		break; //Kamael Village
	case 20:
		castle = 6;
		break; //South of Wastelands Camp
	case 21:
		castle = 8;
		break; //Fantasy Island
	default:
		castle = 5;
		break; //Town of Aden
	}
	
	return castle;
}

public int getClosestTownNumber(L2Character activeChar)
{
	return getMapRegion(activeChar.getX(), activeChar.getY());
}

public String getClosestTownName(L2Character activeChar)
{
	int nearestTownId = getMapRegion(activeChar.getX(), activeChar.getY());
	String nearestTown;
	
	switch (nearestTownId)
	{
	case 0:
		nearestTown = "Talking Island Village";
		break;
	case 1:
		nearestTown = "Elven Village";
		break;
	case 2:
		nearestTown = "Dark Elven Village";
		break;
	case 3:
		nearestTown = "Orc Village";
		break;
	case 4:
		nearestTown = "Dwarven Village";
		break;
	case 5:
		nearestTown = "Town of Gludio";
		break;
	case 6:
		nearestTown = "Gludin Village";
		break;
	case 7:
		nearestTown = "Town of Dion";
		break;
	case 8:
		nearestTown = "Town of Giran";
		break;
	case 9:
		nearestTown = "Town of Oren";
		break;
	case 10:
		nearestTown = "Town of Aden";
		break;
	case 11:
		nearestTown = "Hunters Village";
		break;
	case 12:
		nearestTown = "Giran Harbor";
		break;
	case 13:
		nearestTown = "Heine";
		break;
	case 14:
		nearestTown = "Rune Township";
		break;
	case 15:
		nearestTown = "Town of Goddard";
		break;
	case 16:
		nearestTown = "Town of Schuttgart";
		break;
	case 18:
		nearestTown = "Primeval Isle";
		break;
	case 19:
		nearestTown = "Kamael Village";
		break;
	case 20:
		nearestTown = "South of Wastelands Camp";
		break;
	case 21:
		nearestTown = "Fantasy Island";
		break;
	case 22:
		nearestTown = "Neutral Zone";
		break;
	case 23:
		nearestTown = "Coliseum";
		break;
	case 24:
		nearestTown = "GM Consultation service";
		break;
	case 25:
		nearestTown = "Dimensional Gap";
		break;
	case 26:
		nearestTown = "Cemetary of the Empire";
		break;
	case 27:
		nearestTown = "Inside the Steel Citadel";
		break;
	case 28:
		nearestTown = "Steel Citadel Resistance";
		break;
	case 29:
		nearestTown = "Inside Kamaloka";
		break;
	case 30:
		nearestTown = "Inside Nia Kamaloka";
		break;
	case 31:
		nearestTown = "Inside Rim Kamaloka";
		break;
	case 32:
		nearestTown = "Keucereus clan association";
		break;
	case 33:
		nearestTown = "Inside the Seed of Infinity";
		break;
	case 34:
		nearestTown = "Outside the Seed of Infinity";
		break;
	case 35:
		nearestTown = "Aerial Cleft";
		break;
	case 50:
		nearestTown = "Primeval Isle";
		break;
	default:
		nearestTown = "Town of Aden";
		break;
		
	}
	
	return nearestTown;
}

public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere)
{
	int[] coord;
	
	if (activeChar instanceof L2PcInstance)
	{
		L2PcInstance player = ((L2PcInstance) activeChar);
		
		Castle castle = null;
		Fort fort = null;
		ClanHall clanhall = null;
		
		if (player.getClan() != null && !player.isFlyingMounted()) // flying players in gracia cant use teleports to aden continent
		{
			// If teleport to clan hall
			if (teleportWhere == TeleportWhereType.ClanHall)
			{
				clanhall = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
				if (clanhall != null)
				{
					L2ClanHallZone zone = clanhall.getZone();
					if (zone != null && !player.isFlyingMounted())
					{
						return zone.getSpawn();
					}
				}
			}
			
			// If teleport to castle
			if (teleportWhere == TeleportWhereType.Castle)
			{
				castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
				// Otherwise check if player is on castle or fortress ground and player's clan is defender
				if (castle == null)
				{
					castle = CastleManager.getInstance().getCastle(player);
					if (!(castle != null && castle.getSiege().getIsInProgress() && castle.getSiege().getDefenderClan(player.getClan()) != null))
						castle = null;
				}
				
				if (castle != null && castle.getCastleId() > 0)
				{
					coord = castle.getZone().getPortInLoc();
					return new Location(coord[0], coord[1], coord[2]);
				}
			}
			
			// If teleport to fortress
			if (teleportWhere == TeleportWhereType.Fortress)
			{
				fort = FortManager.getInstance().getFortByOwner(player.getClan());
				// Otherwise check if player is on castle or fortress ground
				// and player's clan is defender
				if (fort == null)
				{
					fort = FortManager.getInstance().getFort(player);
					if (!(fort != null && fort.getSiege().getIsInProgress() && fort.getOwnerClan() == player.getClan()))
						fort = null;
				}
				
				if (fort != null && fort.getFortId() > 0)
				{
					coord = fort.getZone().getSpawn();
					return new Location(coord[0], coord[1], coord[2]);
				}
			}
			
			// If teleport to SiegeHQ
			if (teleportWhere == TeleportWhereType.SiegeFlag)
			{
				castle = CastleManager.getInstance().getCastle(player);
				fort = FortManager.getInstance().getFort(player);
				if (castle != null)
				{
					if (castle.getSiege().getIsInProgress())
					{
						// Check if player's clan is attacker
						List<L2Npc> flags = castle.getSiege().getFlag(player.getClan());
						if (flags != null && !flags.isEmpty())
						{
							// Spawn to flag - Need more work to get player to the nearest flag
							L2Npc flag = flags.get(0);
							return new Location(flag.getX(), flag.getY(), flag.getZ());
						}
					}
					
				}
				else if (fort != null)
				{
					if (fort.getSiege().getIsInProgress())
					{
						// Check if player's clan is attacker
						List<L2Npc> flags = fort.getSiege().getFlag(player.getClan());
						if (flags != null && !flags.isEmpty())
						{
							// Spawn to flag - Need more work to get player to the nearest flag
							L2Npc flag = flags.get(0);
							return new Location(flag.getX(), flag.getY(), flag.getZ());
						}
					}
				}
			}
		}
		
		//Karma player land out of city
		if (player.getKarma() > 1)
		{
			int closest = getMapRegion(activeChar.getX(), activeChar.getY());
			if (closest >= 0 && closest < _pointsWithKarmas.length)
				return new Location(_pointsWithKarmas[closest][0], _pointsWithKarmas[closest][1], _pointsWithKarmas[closest][2]);
			else if (player.isFlyingMounted()) // prevent flying players to teleport outside of gracia
				return new Location(-186330, 242944, 2544);
			else
				return new Location(17817, 170079, -3530);
		}
		
		// Checking if in arena
		L2ArenaZone arena = ZoneManager.getInstance().getArena(player);
		
		if (arena != null)
		{
			coord = arena.getSpawnLoc();
			if (coord[0] != 0 && coord[1] != 0 && coord[2] != 0)
			{
				return new Location(coord[0], coord[1], coord[2]);
			}
		}
		
		//Checking if needed to be respawned in "far" town from the castle;
		castle = CastleManager.getInstance().getCastle(player);
		
		if (castle != null)
		{
			if (castle.getSiege().getIsInProgress())
			{
				// Check if player's clan is participating
				if ((castle.getSiege().checkIsDefender(player.getClan()) || castle.getSiege().checkIsAttacker(player.getClan()))
						&& SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				{
					coord = TownManager.getSecondClosestTown(activeChar).getSpawnLoc();
					if (coord[0] != 0 && coord[1] != 0 && coord[2] != 0)
					{
						return new Location(coord[0], coord[1], coord[2]);
					}
				}
			}
		}
		// Checking if in an instance
		if (player.getInstanceId() > 0)
		{
			Instance inst = InstanceManager.getInstance().getInstance(player.getInstanceId());
			if (inst != null)
			{
				coord = inst.getSpawnLoc();
				if (coord[0] != 0 && coord[1] != 0 && coord[2] != 0)
					return new Location(coord[0], coord[1], coord[2]);
			}
		}
	}
	
	// Get the nearest town
	// TODO: Micht: Maybe we should add some checks to prevent exception here.
	try
	{
		coord = TownManager.getClosestTown(activeChar).getSpawnLoc();
	}
	catch (Exception e)
	{
		coord = TownManager.getTown(9).getSpawnLoc();
	}
	
	return new Location(coord[0], coord[1], coord[2]);
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final MapRegionTable _instance = new MapRegionTable();
}
}
