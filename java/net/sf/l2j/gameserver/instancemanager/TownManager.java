package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;

public class TownManager
{
public final static L2TownZone getClosestTown(L2Object activeObject)
{
	if (activeObject instanceof L2Playable && activeObject.getActingPlayer().getInstanceId() > 0)
		return getTown(4);
	
	switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY()))
	{

	case 0:
		return getTown(2); // Talkin Isle
	case 1:
		return getTown(3); // Elven
	case 2:
		return getTown(1); // DE
	case 3:
		return getTown(4); // Orc
	case 4:
		return getTown(6); // Dwarven
	case 5:
		return getTown(7); // Gludio
	case 6:
		return getTown(5); // Gludin
	case 7:
		return getTown(8); // Dion
	case 8:
		return getTown(9); // Giran
	case 9:
		return getTown(10); // Oren
	case 10:
		return getTown(12); // Aden
	case 11:
		return getTown(11); // HV
	case 12:
		return getTown(9); // Giran Harbour
	case 13:
		return getTown(15); // Heine
	case 14:
		return getTown(14); // Rune
	case 15:
		return getTown(13); // Goddard
	case 16:
		return getTown(17); // Schuttgart
	case 17:
		return getTown(16); // Floran
	case 18:
		return getTown(19); //Primeval Isle
    case 19:
        return getTown(20); //Kamael Village
    case 20:
        return getTown(21); //South of Wastelands Camp
    case 21:
        return getTown(22); //Fantasy Island
    case 22:
        return getTown(23); //Neutral Zone
    case 23:
        return getTown(24);//Coliseum
    case 24:
        return getTown(25);//GM Consultation service
    case 25:
        return getTown(26);//Dimensional Gap
    case 26:
        return getTown(27);//Cemetery of the Empire
    case 27:
        return getTown(28);//inside the Steel Citadel
    case 28:
        return getTown(29);//Steel Citadel Resistance
    case 29:
        return getTown(30);//Inside Kamaloka
    case 30:
        return getTown(31);//Inside Nia Kamaloka
    case 31:
        return getTown(32);//Inside Rim Kamaloka
    case 32:
        return getTown(33);//near the Keucereus clan association location
    case 33:
        return getTown(34);//inside the Seed of Infinity
    case 34:
        return getTown(35);//outside the Seed of Infinity
    case 35:
        return getTown(36);//inside Aerial Cleft
	}
	return getTown(5); // Default to gludin

	
}

public final static L2TownZone getSecondClosestTown(L2Object activeObject)
{
	
	return getTown(5); // Default to gludin
}

public final static boolean townHasCastleInSiege(int townId)
{
	//int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,0,0,6,0};
	int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,7,8,6,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	int castleIndex= castleidarray[townId] ;
	
	if ( castleIndex > 0 )
	{
		Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
		if (castle != null)
			return castle.getSiege().getIsInProgress();
	}
	return false;
}

public final static boolean townHasCastleInSiege(int x, int y)
{
	int curtown= (MapRegionTable.getInstance().getMapRegion(x, y));
	//int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,0,0,0,0};
	int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,8,7,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	//find an instance of the castle for this town.
	int castleIndex = castleidarray[curtown];
	if ( castleIndex > 0 )
	{
		Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
		if (castle != null)
			return castle.getSiege().getIsInProgress();
	}
	return false;
}

public final static L2TownZone getTown(int townId)
{
	for (L2ZoneType temp : ZoneManager.getInstance().getAllZones())
	{
		if (temp instanceof L2TownZone && ((L2TownZone) temp).getTownId() == townId)
			return (L2TownZone) temp;
	}
	return null;
}

/**
 * Returns the town at that position (if any)
 * @param x
 * @param y
 * @param z
 * @return
 */
public final static L2TownZone getTown(int x, int y, int z)
{
	for (L2ZoneType temp : ZoneManager.getInstance().getZones(x, y, z))
	{
		if (temp instanceof L2TownZone)
			return (L2TownZone) temp;
	}
	return null;
}
}
