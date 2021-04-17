package net.sf.l2j.gameserver.datatables;

import javolution.util.FastList;

public final class CrownTable
{
public final static int PRIDE_CLOAK = 14602;
public final static int ADEN_CLOAK = 75000;
public final static int DION_CLOAK = 75001;
public final static int GLUDIO_CLOAK = 75003;
public final static int OREN_CLOAK = 75006;
public final static int INNADRIL_CLOAK = 75005;
public final static int SCHUTTGART_CLOAK = 75008;
public final static int RUNE_CLOAK = 75007;
public final static int GIRAN_CLOAK = 75002;
public final static int GODDARD_CLOAK = 75004;
public final static int FORT_CLOAK = 75010;

private static final int[] CROWN_IDS =
{ 6841, // Crown of the lord
	6834, // Innadril
	6835, // Dion
	6836, // Goddard
	6837, // Oren
	6838, // Gludio
	6839, // Giran
	6840, // Aden
	8182, // Rune
	8183, // Schuttgart
};

public static FastList<Integer> CASTLE_CLOAKS;

public static int[] getCrownIds()
{
	return CROWN_IDS;
}

public static FastList<Integer> getCastleCloaks()
{
	if (CASTLE_CLOAKS == null)
	{
		CASTLE_CLOAKS = new FastList<Integer>();
		
		CASTLE_CLOAKS.add(PRIDE_CLOAK);
		CASTLE_CLOAKS.add(ADEN_CLOAK);
		CASTLE_CLOAKS.add(RUNE_CLOAK);
		CASTLE_CLOAKS.add(GIRAN_CLOAK);
		CASTLE_CLOAKS.add(GODDARD_CLOAK);
		CASTLE_CLOAKS.add(FORT_CLOAK);
		CASTLE_CLOAKS.add(DION_CLOAK);
		CASTLE_CLOAKS.add(GLUDIO_CLOAK);
		CASTLE_CLOAKS.add(OREN_CLOAK);
		CASTLE_CLOAKS.add(INNADRIL_CLOAK);
		CASTLE_CLOAKS.add(SCHUTTGART_CLOAK);
	}
	
	return CASTLE_CLOAKS;
}

public static int getCrownId(int castleId)
{
	switch (castleId)
	{
	case 1:// Gludio
	return 6838;
	
	case 2: // Dion
		return 6835;
		
	case 3: // Giran
		return 6839;
		
	case 4: // Oren
		return 6837;
		
	case 5: // Aden
		return 6840;
		
	case 6: // Innadril
		return 6834;
		
	case 7: // Goddard
		return 6836;
		
	case 8:// Rune
		return 8182;
		
	case 9: // Schuttgart
		return 8183;
	}
	
	return 0;
}

public static int getCloakId(int castleId, boolean isLeader)
{
	switch (castleId)
	{
	case 5: // Aden
	{
		if (isLeader)
			return ADEN_CLOAK;
		
		return PRIDE_CLOAK;
	}
	case 8:// Rune
	{
		if (isLeader)
			return RUNE_CLOAK;
		
		return PRIDE_CLOAK;
	}
	case 3:// Giran
	{
		if (isLeader)
			return GIRAN_CLOAK;
		
		break;
	}
	case 7:// Goddard
	{
		if (isLeader)
			return GODDARD_CLOAK;
		
		return PRIDE_CLOAK;
	}
	case 1:// Gludio
	{
		if (isLeader)
			return GLUDIO_CLOAK;
		
		break;
	}
	case 2:// Dion
	{
		if (isLeader)
			return DION_CLOAK;
		
		break;
	}
	case 4:// Oren
	{
		if (isLeader)
			return OREN_CLOAK;
		
		break;
	}
	case 6:// INNADRIL
	{
		if (isLeader)
			return INNADRIL_CLOAK;
		
		break;
	}
	case 9:// SCHUTTGART
	{
		if (isLeader)
			return SCHUTTGART_CLOAK;
		
		break;
	}
	}
	
	return -1;
}
}