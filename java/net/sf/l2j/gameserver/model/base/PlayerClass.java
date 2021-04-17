package net.sf.l2j.gameserver.model.base;

import static net.sf.l2j.gameserver.model.base.ClassLevel.First;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Fourth;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Second;
import static net.sf.l2j.gameserver.model.base.ClassLevel.Third;
import static net.sf.l2j.gameserver.model.base.ClassType.Archer;
import static net.sf.l2j.gameserver.model.base.ClassType.Dagger;
import static net.sf.l2j.gameserver.model.base.ClassType.Fighter;
import static net.sf.l2j.gameserver.model.base.ClassType.Mystic;
import static net.sf.l2j.gameserver.model.base.ClassType.Priest;
import static net.sf.l2j.gameserver.model.base.ClassType.kFighter;
import static net.sf.l2j.gameserver.model.base.ClassType.sFighter;
import static net.sf.l2j.gameserver.model.base.Race.DarkElf;
import static net.sf.l2j.gameserver.model.base.Race.Dwarf;
import static net.sf.l2j.gameserver.model.base.Race.Elf;
import static net.sf.l2j.gameserver.model.base.Race.Human;
import static net.sf.l2j.gameserver.model.base.Race.Kamael;
import static net.sf.l2j.gameserver.model.base.Race.Orc;

import java.util.EnumSet;
import java.util.Set;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public enum PlayerClass {
humanFighter(Human, Fighter, First), Warrior(Human, Fighter, Second), Gladiator(Human, sFighter,
		Third), Warlord(Human, sFighter, Third), humanKnight(Human, Fighter, Second), Paladin(Human,
				sFighter, Third), DarkAvenger(Human, sFighter, Third), Rogue(Human, Fighter, Second), TreasureHunter(
						Human, Dagger, Third), Hawkeye(Human, Archer, Third), humanMystic(Human, Mystic, First), humanWizard(
								Human, Mystic, Second), Sorceror(Human, Mystic, Third), Necromancer(Human, Mystic, Third), Warlock(
										Human, Mystic, Third), Cleric(Human, Priest, Second), Bishop(Human, Priest, Third), Prophet(
												Human, Priest, Third),
												
												ElvenFighter(Elf, Fighter, First), ElvenKnight(Elf, Fighter, Second), TempleKnight(
														Elf, sFighter, Third), Swordsinger(Elf, sFighter, Third), ElvenScout(Elf,
																Fighter, Second), Plainswalker(Elf, Dagger, Third), SilverRanger(Elf, Archer,
																		Third), ElvenMystic(Elf, Mystic, First), ElvenWizard(Elf, Mystic, Second), Spellsinger(
																				Elf, Mystic, Third), ElementalSummoner(Elf, Mystic, Third), ElvenOracle(Elf,
																						Priest, Second), ElvenElder(Elf, Priest, Third),
																						
																						DarkElvenFighter(DarkElf, Fighter, First), PalusKnight(DarkElf, Fighter, Second), ShillienKnight(
																								DarkElf, sFighter, Third), Bladedancer(DarkElf, sFighter, Third), Assassin(DarkElf, Fighter,
																										Second), AbyssWalker(DarkElf, Dagger, Third), PhantomRanger(DarkElf, Archer, Third), DarkElvenMystic(
																												DarkElf, Mystic, First), DarkElvenWizard(DarkElf, Mystic, Second), Spellhowler(DarkElf,
																														Mystic, Third), PhantomSummoner(DarkElf, Mystic, Third), ShillienOracle(DarkElf, Priest,
																																Second), ShillienElder(DarkElf, Priest, Third),
																																
																																orcFighter(Orc, Fighter, First), orcRaider(Orc, Fighter, Second), Destroyer(Orc, sFighter, Third), orcMonk(
																																		Orc, Fighter, Second), Tyrant(Orc, Fighter, Third), orcMystic(Orc, Mystic, First), orcShaman(
																																				Orc, Mystic, Second), Overlord(Orc, Mystic, Third), Warcryer(Orc, Mystic, Third),
																																				
																																				DwarvenFighter(Dwarf, Fighter, First), DwarvenScavenger(Dwarf, Fighter, Second), BountyHunter(Dwarf,
																																						sFighter, Third), DwarvenArtisan(Dwarf, Fighter, Second), Warsmith(Dwarf, sFighter, Third),
																																						
																																						dummyEntry1(null, null, null), dummyEntry2(null, null, null), dummyEntry3(null, null, null), dummyEntry4(
																																								null, null, null), dummyEntry5(null, null, null), dummyEntry6(null, null, null), dummyEntry7(
																																										null, null, null), dummyEntry8(null, null, null), dummyEntry9(null, null, null), dummyEntry10(
																																												null, null, null), dummyEntry11(null, null, null), dummyEntry12(null, null, null), dummyEntry13(
																																														null, null, null), dummyEntry14(null, null, null), dummyEntry15(null, null, null), dummyEntry16(
																																																null, null, null), dummyEntry17(null, null, null), dummyEntry18(null, null, null), dummyEntry19(
																																																		null, null, null), dummyEntry20(null, null, null), dummyEntry21(null, null, null), dummyEntry22(
																																																				null, null, null), dummyEntry23(null, null, null), dummyEntry24(null, null, null), dummyEntry25(
																																																						null, null, null), dummyEntry26(null, null, null), dummyEntry27(null, null, null), dummyEntry28(
																																																								null, null, null), dummyEntry29(null, null, null), dummyEntry30(null, null, null), dummyEntry31(
																																																										null, null, null), dummyEntry32(null, null, null), dummyEntry33(null, null, null), dummyEntry34(
																																																												null, null, null),
																																																												
																																																												/*
																																																												 * (3rd classes)
																																																												 */
																																																												duelist(Human, Fighter, Fourth), dreadnought(Human, Fighter, Fourth), phoenixKnight(Human, Fighter,
																																																														Fourth), hellKnight(Human, Fighter, Fourth), sagittarius(Human, Archer, Fourth), adventurer(
																																																																Human, Dagger, Fourth), archmage(Human, Mystic, Fourth), soultaker(Human, Mystic, Fourth), arcanaLord(
																																																																		Human, Mystic, Fourth), cardinal(Human, Priest, Fourth), hierophant(Human, Priest, Fourth),
																																																																		
																																																																		evaTemplar(Elf, Fighter, Fourth), swordMuse(Elf, Fighter, Fourth), windRider(Elf,
																																																																				Dagger, Fourth), moonlightSentinel(Elf, Archer, Fourth), mysticMuse(Elf, Mystic,
																																																																						Fourth), elementalMaster(Elf, Mystic, Fourth), evaSaint(Elf, Priest, Fourth),
																																																																						
																																																																						shillienTemplar(DarkElf, Fighter, Fourth), spectralDancer(DarkElf, Fighter, Fourth), ghostHunter(
																																																																								DarkElf, Dagger, Fourth), ghostSentinel(DarkElf, Archer, Fourth), stormScreamer(DarkElf,
																																																																										Mystic, Fourth), spectralMaster(DarkElf, Mystic, Fourth), shillienSaint(DarkElf, Priest,
																																																																												Fourth),
																																																																												
																																																																												titan(Orc, Fighter, Fourth), grandKhauatari(Orc, Fighter, Fourth), dominator(Orc, Mystic, Fourth), doomcryer(
																																																																														Orc, Mystic, Fourth),
																																																																														
																																																																														fortuneSeeker(Dwarf, Fighter, Fourth), maestro(Dwarf, Fighter, Fourth),
																																																																														
																																																																														maleSoldier(Kamael, Fighter, First), femaleSoldier(Kamael, Fighter, First),
																																																																														Dragoon(Kamael, Fighter, Second), Warder(Kamael, Fighter, Second),
																																																																														Berserker(Kamael, kFighter, Third), MaleSoulbreaker(Kamael, Fighter, Third),
																																																																														FemaleSoulbreaker(Kamael, Fighter, Third), Arbalester(Kamael, Fighter, Third),
																																																																														doombringer(Kamael, kFighter, Fourth), maleSoulhound(Kamael,  Fighter, Fourth),
																																																																														femaleSoulhound(Kamael, Fighter, Fourth), trickster(Kamael, Fighter, Fourth),
																																																																														Inspector(Kamael, Fighter, Third), judicator(Kamael, Fighter, Fourth);

private Race _race;
private ClassLevel _level;
private ClassType _type;

private static final Set<PlayerClass> mainSubclassSet;
/*    private static final Set<PlayerClass> neverSubclassed = EnumSet.of(Overlord, Warsmith);

    private static final Set<PlayerClass> subclasseSet1 = EnumSet.of(DarkAvenger, Paladin, TempleKnight, ShillienKnight);
    private static final Set<PlayerClass> subclasseSet2 = EnumSet.of(TreasureHunter, AbyssWalker, Plainswalker);
    private static final Set<PlayerClass> subclasseSet3 = EnumSet.of(Hawkeye, SilverRanger, PhantomRanger);
    private static final Set<PlayerClass> subclasseSet4 = EnumSet.of(Warlock, ElementalSummoner, PhantomSummoner);
    private static final Set<PlayerClass> subclasseSet5 = EnumSet.of(Sorceror, Spellsinger, Spellhowler);

    private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap
           = new EnumMap<PlayerClass, Set<PlayerClass>>(PlayerClass.class);*/

static
{
	Set<PlayerClass> subclasses = getSet(null, Third);
	/*        subclasses.removeAll(neverSubclassed);*/
	
	mainSubclassSet = subclasses;
	
	/*        subclassSetMap.put(DarkAvenger, subclasseSet1);
        subclassSetMap.put(Paladin, subclasseSet1);
        subclassSetMap.put(TempleKnight, subclasseSet1);
        subclassSetMap.put(ShillienKnight, subclasseSet1);

        subclassSetMap.put(TreasureHunter, subclasseSet2);
        subclassSetMap.put(AbyssWalker, subclasseSet2);
        subclassSetMap.put(Plainswalker, subclasseSet2);

        subclassSetMap.put(Hawkeye, subclasseSet3);
        subclassSetMap.put(SilverRanger, subclasseSet3);
        subclassSetMap.put(PhantomRanger, subclasseSet3);

        subclassSetMap.put(Warlock, subclasseSet4);
        subclassSetMap.put(ElementalSummoner, subclasseSet4);
        subclassSetMap.put(PhantomSummoner, subclasseSet4);

        subclassSetMap.put(Sorceror, subclasseSet5);
        subclassSetMap.put(Spellsinger, subclasseSet5);
        subclassSetMap.put(Spellhowler, subclasseSet5);*/
}

PlayerClass(Race pRace, ClassType pType, ClassLevel pLevel)
{
	_race = pRace;
	_level = pLevel;
	_type = pType;
}

public final Set<PlayerClass> getAvailableSubclasses(L2PcInstance player)
{
	Set<PlayerClass> subclasses = null;
	
	if (_level != Third) return null;
	
	if (player.getRace() != Kamael)
	{
		subclasses = EnumSet.copyOf(mainSubclassSet);
		
		// Already done in mainSubclassSet
		//subclasses.removeAll(neverSubclassed);
		
		subclasses.remove(this);
		
		/*            switch (_race)
            {
                case Elf:
                    subclasses.removeAll(getSet(DarkElf, Third));
                    break;
                case DarkElf:
                    subclasses.removeAll(getSet(Elf, Third));
                    break;
            }*/
		
		subclasses.removeAll(getSet(Kamael, Third));
		subclasses.addAll(getSet(kFighter));
		
		/*            Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);

            if (unavailableClasses != null)
            {
                subclasses.removeAll(unavailableClasses);
            }*/
	}
	else
	{
		subclasses = getSet(Kamael,Third);
		subclasses.addAll(getSet(Archer));
		subclasses.addAll(getSet(Mystic));
		subclasses.addAll(getSet(Priest));
		subclasses.addAll(getSet(Dagger));
		subclasses.addAll(getSet(sFighter));
		subclasses.remove(this);
		//Check sex, male can't subclass female and vice versa
		/*        	if (player.getAppearance().getSex())
        		subclasses.removeAll(EnumSet.of(maleSoulbreaker));
        	else
        		subclasses.removeAll(EnumSet.of(femaleSoulbreaker));*/
		
		/*            byte subOverLevel75 = 0;
            
            for (SubClass sc : player.getSubClasses().values())
            {
                if (sc.getLevel() >= 75)
                    subOverLevel75++;
            }
            if (subOverLevel75 < 2)
                subclasses.removeAll(EnumSet.of(inspector));*/
	}
	
	return subclasses;
}

public static final EnumSet<PlayerClass> getSet(Race race, ClassLevel level)
{
	EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);
	
	for (PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
	{
		if (race == null || playerClass.isOfRace(race))
		{
			if (level == null || playerClass.isOfLevel(level))
			{
				allOf.add(playerClass);
			}
		}
	}
	
	return allOf;
}

public static final EnumSet<PlayerClass> getSet(ClassType type)
{
	EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);
	
	for (PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
	{
		if (playerClass.isOfType(type) && playerClass.isOfLevel(Third))
		{
			allOf.add(playerClass);
		}
	}
	
	return allOf;
}

public final boolean isOfRace(Race pRace)
{
	return _race == pRace;
}

public final boolean isOfType(ClassType pType)
{
	return _type == pType;
}

public final boolean isOfLevel(ClassLevel pLevel)
{
	return _level == pLevel;
}
public final ClassLevel getLevel()
{
	return _level;
}
}
