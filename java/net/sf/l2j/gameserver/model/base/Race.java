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
package net.sf.l2j.gameserver.model.base;

import net.sf.l2j.util.Rnd;

/**
 * This class defines all races that a player can choose.<BR>
 * <BR>
 */
public enum Race
{
	Human(0x00),
	Elf(0x63),
	DarkElf(0x6a),
	Orc(0x2e),
	Dwarf(0x75),
	Kamael(0x84),
	MHuman(0x10),
	MOrc(0x34),
	Dummy(0x00);
	private int baseId;
	
	Race(final int id)
	{
		baseId = id;
	}
	
	public int getBaseId()
	{
		return baseId;
	}
	
	public int realOrdinal()
	{
		switch (this)
		{
			case MHuman:
				return Human.ordinal();
			case MOrc:
				return Orc.ordinal();
			default:
				return ordinal();
		}
	}
	
	public Race real()
	{
		switch (this)
		{
			case MHuman:
				return Human;
			case MOrc:
				return Orc;
			default:
				return this;
		}
	}
	
	
	public float getCollisionHeight(int sex)
	{
		return getCollisionHeight(sex == 1);
	}
	
	public float getCollisionHeight(boolean female)
	{
		switch (this)
		{
			case Human:
				return female ? 23.5f : 23f;
			case MHuman:
				return female ? 22.5f : 22.8f;
			case Elf:
				return female ? 23f : 23f;
			case DarkElf:
				return female ? 23.5f : 24f;
			case Orc:
				return female ? 27f : 28f;
			case MOrc:
				return female ? 25.5f : 27.5f;
			case Dwarf:
				return female ? 19f : 18f;
			case Kamael:
				return female ? 22.6f : 25.2f;
		}
		return 0;
	}
	
	public float getCollisionRadius(int sex)
	{
		return getCollisionRadius(sex == 1);
	}
	
	public float getCollisionRadius(boolean female)
	{
		switch (this)
		{
			case Human:
				return female ? 8f : 9f;
			case Dwarf:
				return female ? 5f : 9f;
			case MHuman:
				return female ? 6.5f : 7.5f;
			case Elf:
				return 7.5f;
			case DarkElf:
				return female ? 7.5f : 7f;
			case MOrc:
				return female ? 8f : 7f;
			case Orc:
				return female ? 7f : 11f;
			case Kamael:
				return female ? 7f : 8f;
		}
		return 0;
	}
	
	public String getDisplayName()
	{
		switch (this)
		{
			case Human:
				return "Human Fighter";
			case Dwarf:
				return "Dwarf";
			case MHuman:
				return "Human Mystic";
			case Elf:
				return "Elf";
			case DarkElf:
				return "Dark Elf";
			case MOrc:
				return "Orc Mystic";
			case Orc:
				return "Orc Fighter";
			case Kamael:
				return "Kamael";
		}
		return "Dummy";
	}
	
	public String getGenericName()
	{
		switch (this)
		{
			case Human:
				return "human";
			case Dwarf:
				return "dwarven";
			case MHuman:
				return "hooman";
			case Elf:
				return "elf";
			case DarkElf:
				return "dark-elf";
			case MOrc:
			case Orc:
				return "orc";
			case Kamael:
				return "Kamael";
		}
		return "Dummy";
	}
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
	private static Race[] sortedValues = new Race[] {Human, MHuman, Elf, DarkElf, Orc, MOrc, Dwarf, Kamael};
	
	public static Race[] sortedValues()
	{
		return sortedValues;
	}
	
	public boolean stucks(final Race race)
	{
		return race != null && this != race && race.real() == real();
	}

	public static Race getRandomRace()
	{
		return Rnd.get(values());
	}
	
}