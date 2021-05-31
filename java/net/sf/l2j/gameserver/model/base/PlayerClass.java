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


import static net.sf.l2j.gameserver.model.base.ClassLevel.First;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Fourth;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Second;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Third;
import static net.sf.l2j.gameserver.model.base.ClassType.ARCHER;
import static net.sf.l2j.gameserver.model.base.ClassType.DAGGER;
import static net.sf.l2j.gameserver.model.base.ClassType.HEALER;
import static net.sf.l2j.gameserver.model.base.ClassType.MAGE;
import static net.sf.l2j.gameserver.model.base.ClassType.MAGE_SUPPORT;
import static net.sf.l2j.gameserver.model.base.ClassType.MELEE;
import static net.sf.l2j.gameserver.model.base.ClassType.MELEE_SUPPORT;
import static net.sf.l2j.gameserver.model.base.ClassType.OFFTANK;
import static net.sf.l2j.gameserver.model.base.ClassType.SUMMONER;
import static net.sf.l2j.gameserver.model.base.ClassType.SUPPORT;
import static net.sf.l2j.gameserver.model.base.ClassType.TANK;
import static net.sf.l2j.gameserver.model.base.Race.DarkElf;
import static net.sf.l2j.gameserver.model.base.Race.Dwarf;
import static net.sf.l2j.gameserver.model.base.Race.Elf;
import static net.sf.l2j.gameserver.model.base.Race.Human;
import static net.sf.l2j.gameserver.model.base.Race.Orc;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public enum PlayerClass
{
	Human_Fighter(Human, MELEE, First),
	Warrior(Human, MELEE, Second),
	Gladiator(Human, MELEE, Third),
	Warlord(Human, MELEE, Third),
	Human_Knight(Human, TANK, Second),
	Paladin(Human, TANK, Third),
	Dark_Avenger(Human, TANK, Third),
	Rogue(Human, MELEE, Second),
	Treasure_Hunter(Human, DAGGER, Third),
	Hawkeye(Human, ARCHER, Third),
	Human_Mystic(Human, MAGE, First),
	Human_Wizard(Human, MAGE, Second),
	Sorceror(Human, MAGE, Third),
	Necromancer(Human, MAGE, Third),
	Warlock(Human, SUMMONER, Third),
	Cleric(Human, SUPPORT, Second),
	Bishop(Human, HEALER, Third),
	Prophet(Human, SUPPORT, Third),
	Elven_Fighter(Elf, MELEE, First),
	Elven_Knight(Elf, TANK, Second),
	Temple_Knight(Elf, TANK, Third),
	Swordsinger(Elf, TANK, Third),
	Elven_Scout(Elf, MELEE, Second),
	Plainswalker(Elf, DAGGER, Third),
	Silver_Ranger(Elf, ARCHER, Third),
	Elven_Mystic(Elf, MAGE, First),
	Elven_Wizard(Elf, MAGE, Second),
	Spellsinger(Elf, MAGE, Third),
	Elemental_Summoner(Elf, SUMMONER, Third),
	Elven_Oracle(Elf, SUPPORT, Second),
	Elven_Elder(Elf, SUPPORT, Third),
	Dark_Elven_Fighter(DarkElf, MELEE, First),
	Palus_Knight(DarkElf, TANK, Second),
	Shillien_Knight(DarkElf, TANK, Third),
	Bladedancer(DarkElf, MELEE, Third),
	Assassin(DarkElf, MELEE, Second),
	Abyss_Walker(DarkElf, DAGGER, Third),
	Phantom_Ranger(DarkElf, ARCHER, Third),
	Dark_Elven_Mystic(DarkElf, MAGE, First),
	Dark_Elven_Wizard(DarkElf, MAGE, Second),
	Spellhowler(DarkElf, MAGE, Third),
	Phantom_Summoner(DarkElf, SUMMONER, Third),
	Shillien_Oracle(DarkElf, SUPPORT, Second),
	Shillien_Elder(DarkElf, SUPPORT, Third),
	Orc_Fighter(Orc, MELEE, First),
	Orc_Raider(Orc, MELEE, Second),
	Destroyer(Orc, MELEE, Third),
	Orc_Monk(Orc, MAGE, Second),
	Tyrant(Orc, MELEE, Third),
	Orc_Mystic(Orc, MAGE, First),
	Orc_Shaman(Orc, MAGE, Second),
	Overlord(Orc, MAGE_SUPPORT, Third),
	Warcryer(Orc, MELEE_SUPPORT, Third),
	Dwarven_Fighter(Dwarf, MELEE, First),
	Dwarven_Scavenger(Dwarf, MELEE, Second),
	Bounty_Hunter(Dwarf, MELEE, Third),
	Dwarven_Artisan(Dwarf, MELEE, Second),
	Warsmith(Dwarf, MELEE, Third),
	dummyEntry1(null, null, null),
	dummyEntry2(null, null, null),
	dummyEntry3(null, null, null),
	dummyEntry4(null, null, null),
	dummyEntry5(null, null, null),
	dummyEntry6(null, null, null),
	dummyEntry7(null, null, null),
	dummyEntry8(null, null, null),
	dummyEntry9(null, null, null),
	dummyEntry10(null, null, null),
	dummyEntry11(null, null, null),
	dummyEntry12(null, null, null),
	dummyEntry13(null, null, null),
	dummyEntry14(null, null, null),
	dummyEntry15(null, null, null),
	dummyEntry16(null, null, null),
	dummyEntry17(null, null, null),
	dummyEntry18(null, null, null),
	dummyEntry19(null, null, null),
	dummyEntry20(null, null, null),
	dummyEntry21(null, null, null),
	dummyEntry22(null, null, null),
	dummyEntry23(null, null, null),
	dummyEntry24(null, null, null),
	dummyEntry25(null, null, null),
	dummyEntry26(null, null, null),
	dummyEntry27(null, null, null),
	dummyEntry28(null, null, null),
	dummyEntry29(null, null, null),
	dummyEntry30(null, null, null),
	// 3rd classes
	Duelist(Human, MELEE, Fourth),
	Dreadnought(Human, MELEE, Fourth),
	Phoenix_Knight(Human, TANK, Fourth),
	Hell_Knight(Human, TANK, Fourth),
	Sagittarius(Human, ARCHER, Fourth),
	Adventurer(Human, DAGGER, Fourth),
	Archmage(Human, MAGE, Fourth),
	Soultaker(Human, MAGE, Fourth),
	Arcana_Lord(Human, SUMMONER, Fourth),
	Cardinal(Human, HEALER, Fourth),
	Hierophant(Human, MELEE, Fourth),
	Eva_Templar(Elf, TANK, Fourth),
	Sword_Muse(Elf, MELEE, Fourth),
	Wind_Rider(Elf, DAGGER, Fourth),
	Moonlight_Sentinel(Elf, ARCHER, Fourth),
	Mystic_Muse(Elf, MAGE, Fourth),
	Elemental_Master(Elf, MAGE, Fourth),
	Eva_Saint(Elf, SUPPORT, Fourth),
	Shillien_Templar(DarkElf, TANK, Fourth),
	Spectral_Dancer(DarkElf, MELEE, Fourth),
	Ghost_Hunter(DarkElf, DAGGER, Fourth),
	Ghost_Sentinel(DarkElf, ARCHER, Fourth),
	Storm_Screamer(DarkElf, MAGE, Fourth),
	Spectral_Master(DarkElf, MAGE, Fourth),
	Shillien_Saint(DarkElf, MAGE, Fourth),
	Titan(Orc, MELEE, Fourth),
	Grand_Khauatari(Orc, MELEE, Fourth),
	Dominator(Orc, MAGE_SUPPORT, Fourth),
	Doomcryer(Orc, MELEE_SUPPORT, Fourth),
	Fortune_Seeker(Dwarf, OFFTANK, Fourth),
	Maestro(Dwarf, OFFTANK, Fourth),
	
	
	dummyEntry31(null, null, null),
	dummyEntry32(null, null, null),
	dummyEntry33(null, null, null),
	dummyEntry34(null, null, null),
	
	maleSoldier(Race.Kamael, MELEE, First),
	femaleSoldier(Race.Kamael, MELEE, First),
	trooper(Race.Kamael, MELEE, Second),
	warder(Race.Kamael, MELEE, Second),
	berserker(Race.Kamael, MELEE, Third),
	maleSoulbreaker(Race.Kamael, MELEE, Third),
	femaleSoulbreaker(Race.Kamael, MELEE, Third),
	arbalester(Race.Kamael, ARCHER, Third),
	Doombringer(Race.Kamael, MELEE, Fourth),
	Soul__Hound(Race.Kamael, MELEE, Fourth),
	Soul_Hound(Race.Kamael, MELEE, Fourth),
	Trickster(Race.Kamael, ARCHER, Fourth),
	inspector(Race.Kamael, ARCHER, Third),
	Judicator(Race.Kamael, MELEE_SUPPORT, Fourth);
	
//	private static final EnumSet<PlayerClass> kamaelOnly = EnumSet.of(Soul__Hound, Soul_Hound, Trickster, Judicator); 
	
	private Race _race;
	private ClassLevel _level;
	private ClassType _type;
	private static final Set<PlayerClass> mainSubclassSet;
	// private static final Set<PlayerClass> neverSubclassed =
	// EnumSet.of(Overlord, Warsmith);
	
	private static final EnumSet<PlayerClass> _kamaelClasses = EnumSet.of(maleSoldier, femaleSoldier, trooper, warder, berserker, maleSoulbreaker, femaleSoulbreaker, arbalester, Doombringer, Soul__Hound, Soul_Hound, Trickster, inspector, Judicator);
	
	private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<>(PlayerClass.class);
	
	static
	{
		final Set<PlayerClass> subclasses = getSet(null, Fourth);
		// subclasses.removeAll(neverSubclassed);
		mainSubclassSet = subclasses;
	}
	
	PlayerClass(final Race pRace, final ClassType pType, final ClassLevel pLevel)
	{
		_race = pRace;
		_level = pLevel;
		_type = pType;
	}
	
	@Override
	public String toString()
	{
		return super.toString().replace("__", " ").replace("_", " ").replace("SUMMONER", "Summoner");
	}
	
	public final Set<PlayerClass> getAvailableSubclasses(final L2PcInstance player)
	{
		Set<PlayerClass> subclasses = null;
		if (_level == Fourth)
		{
			subclasses = EnumSet.copyOf(mainSubclassSet);
			subclasses.remove(this);
			
//			if (player.getRace() != Race.Kamael)
//				subclasses.removeAll(kamaelOnly);
//			else
				subclasses.remove(player.getSex() ? Soul__Hound : Soul_Hound);
			
			final Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);
			if (unavailableClasses != null)
				subclasses.removeAll(unavailableClasses);
		}
		return subclasses;
	}
	
	public static final EnumSet<PlayerClass> getSet(final Race race, final ClassLevel level)
	{
		final EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);
		for (final PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
			if (race == null || playerClass.isOfRace(race))
				if (level == null || playerClass.isOfLevel(level))
					allOf.add(playerClass);
		
		
		return allOf;
	}
	
	public final boolean isOfRace(final Race pRace)
	{
		return _race == pRace;
	}
	
	public final boolean isOfType(final ClassType pType)
	{
		return _type.isOfType(pType);
	}
	
	public final boolean isOfLevel(final ClassLevel pLevel)
	{
		return _level == pLevel;
	}
	
	public final ClassLevel getLevel()
	{
		return _level;
	}
	
	public ClassType getType()
	{
		return _type;
	}
}
