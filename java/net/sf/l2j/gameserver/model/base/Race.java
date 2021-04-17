package net.sf.l2j.gameserver.model.base;


public enum Race
{
Human,
Elf,
DarkElf,
Orc,
Dwarf,
Kamael,
HumanMystic,
OrcMystic;

public final static Race getRaceByName(String name)
{
	for (Race race : Race.values())
	{
		if (race.name().equalsIgnoreCase(name))
			return race;
	}
	
	return Race.Human;
}

public final int getRealOrdinal()
{
	int ord = ordinal();
	
	if (ord == 6)
		ord = 0;
	else if (ord == 7)
		ord = 3;
	
	return ord;
}
}
