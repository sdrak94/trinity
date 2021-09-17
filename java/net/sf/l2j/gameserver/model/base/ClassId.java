package net.sf.l2j.gameserver.model.base;
/**
 * This class defines all classes (ex : human fighter, darkFighter...) that a player can chose.<BR><BR>
 * 
 * Data :<BR><BR>
 * <li>id : The Identifier of the class</li>
 * <li>isMage : True if the class is a mage class</li>
 * <li>race : The race of this class</li>
 * <li>parent : The parent ClassId or null if this class is the root</li><BR><BR>
 * 
 * @version $Revision: 1.4.4.4 $ $Date: 2005/03/27 15:29:33 $
 * 
 */
public enum ClassId
{
	fighter(0x00, "Human Fighter", false, Race.Human, null),

	 warrior(0x01, "Human Warrior", false, Race.Human, fighter),
	 gladiator(0x02, "Gladiator", false, Race.Human, warrior),
	 warlord(0x03, "Warlord", false, Race.Human, warrior),
	 knight(0x04, "Human Knight", false, Race.Human, fighter),
	 paladin(0x05, "Paladin", false, Race.Human, knight),
	 darkAvenger(0x06, "Dark Avanger", false, Race.Human, knight),
	 rogue(0x07, "Rogue", false, Race.Human, fighter),
	 treasureHunter(0x08, "Treausure Hunter", false, Race.Human, rogue),
	 hawkeye(0x09, "Hawkeye", false, Race.Human, rogue),

	 mage(0x0a, "Human Mage", true, Race.Human, null),
	 wizard(0x0b, "Human Wizzard", true, Race.Human, mage),
	 sorceror(0x0c, "Sorcerror", true, Race.Human, wizard),
	 necromancer(0x0d, "Necromancer", true, Race.Human, wizard),
	 warlock(0x0e, "Warlock", true, true, Race.Human, wizard),
	 cleric(0x0f, "Cleric", true, Race.Human, mage),
	 bishop(0x10, "Bishop", true, Race.Human, cleric),
	 prophet(0x11, "Prophet", false, Race.Human, cleric),

	 elvenFighter(0x12, "Elven Fighter", false, Race.Elf, null),
	 elvenKnight(0x13, "Elven Knight", false, Race.Elf, elvenFighter),
	 templeKnight(0x14, "Temple Knight", false, Race.Elf, elvenKnight),
	 swordSinger(0x15, "Sword Singer", false, Race.Elf, elvenKnight),
	 elvenScout(0x16, "Elven Scout", false, Race.Elf, elvenFighter),
	 plainsWalker(0x17, "Plains Walker", false, Race.Elf, elvenScout),
	 silverRanger(0x18, "Silver Ranger", false, Race.Elf, elvenScout),

	 elvenMage(0x19, "Elven Mage", true, Race.Elf, null),
	 elvenWizard(0x1a, "Elven Wizard", true, Race.Elf, elvenMage),
	 spellsinger(0x1b, "Spellsinger", true, Race.Elf, elvenWizard),
	 elementalSummoner(0x1c, "Elemental Summoner", true, true, Race.Elf, elvenWizard),
	 oracle(0x1d, "Elven Oracle", true, Race.Elf, elvenMage),
	 elder(0x1e, "Elven Elder", true, Race.Elf, oracle),

	 darkFighter(0x1f, "Dark Fighter", false, Race.DarkElf, null),
	 palusKnight(0x20, "Palus Knight", false, Race.DarkElf, darkFighter),
	 shillienKnight(0x21, "Shillien Knight", false, Race.DarkElf, palusKnight),
	 bladedancer(0x22, "Bladedancer", false, Race.DarkElf, palusKnight),
	 assassin(0x23, "Assassin", false, Race.DarkElf, darkFighter),
	 abyssWalker(0x24, "Abyss Walker", false, Race.DarkElf, assassin),
	 phantomRanger(0x25, "Phantom Ranger", false, Race.DarkElf, assassin),

	 darkMage(0x26, "Dark Mage", true, Race.DarkElf, null),
	 darkWizard(0x27, "Dark Wizard", true, Race.DarkElf, darkMage),
	 spellhowler(0x28, "Spellhowler", true, Race.DarkElf, darkWizard),
	 phantomSummoner(0x29, "Phantom Summoner", true, true, Race.DarkElf, darkWizard),
	 shillienOracle(0x2a,"Shillien Oracle", true, Race.DarkElf, darkMage),
	 shillienElder(0x2b, "Shillien Elder", true, Race.DarkElf, shillienOracle),

	 orcFighter(0x2c, "Orc Fighter", false, Race.Orc, null),
	 orcRaider(0x2d, "Orc Raider", false, Race.Orc, orcFighter),
	 destroyer(0x2e, "Destroyer", false, Race.Orc, orcRaider),
	 orcMonk(0x2f, "Monk", false, Race.Orc, orcFighter),
	 tyrant(0x30, "Tyrant", false, Race.Orc, orcMonk),

	 orcMage(0x31, "Orc Mage", true, Race.Orc, null),
	 orcShaman(0x32, "Orc Shaman", true, Race.Orc, orcMage),
	 overlord(0x33, "Overlord", true, Race.Orc, orcShaman),
	 warcryer(0x34, "Warcryer", true, Race.Orc, orcShaman),

	 dwarvenFighter(0x35, "Dwarven Fighter", false, Race.Dwarf, null),
	 scavenger(0x36, "Scavenger", false, Race.Dwarf, dwarvenFighter),
	 bountyHunter(0x37, "Bounty Hunter", false, Race.Dwarf, scavenger),
	 artisan(0x38, "Artisan", false, Race.Dwarf, dwarvenFighter),
	 warsmith(0x39, "Warsmith", false, Race.Dwarf, artisan),

	 /*
	  * Dummy Entries (id's already in decimal format)
	  * btw FU NCSoft for the amount of work you put me
	  * through to do this!!
	  * <START>
	  */
	 dummyEntry1(58, "dummyEntry1", false, null, null),
	 dummyEntry2(59, "dummyEntry2", false, null, null),
	 dummyEntry3(60, "dummyEntry3", false, null, null),
	 dummyEntry4(61, "dummyEntry4", false, null, null),
	 dummyEntry5(62, "dummyEntry5", false, null, null),
	 dummyEntry6(63, "dummyEntry6", false, null, null),
	 dummyEntry7(64, "dummyEntry7", false, null, null),
	 dummyEntry8(65, "dummyEntry8", false, null, null),
	 dummyEntry9(66, "dummyEntry9", false, null, null),
	 dummyEntry10(67, "dummyEntry10", false, null, null),
	 dummyEntry11(68, "dummyEntry11", false, null, null),
	 dummyEntry12(69, "dummyEntry12", false, null, null),
	 dummyEntry13(70, "dummyEntry13", false, null, null),
	 dummyEntry14(71, "dummyEntry14", false, null, null),
	 dummyEntry15(72, "dummyEntry15", false, null, null),
	 dummyEntry16(73, "dummyEntry16", false, null, null),
	 dummyEntry17(74, "dummyEntry17", false, null, null),
	 dummyEntry18(75, "dummyEntry18", false, null, null),
	 dummyEntry19(76, "dummyEntry19", false, null, null),
	 dummyEntry20(77, "dummyEntry20", false, null, null),
	 dummyEntry21(78, "dummyEntry21", false, null, null),
	 dummyEntry22(79, "dummyEntry22", false, null, null),
	 dummyEntry23(80, "dummyEntry23", false, null, null),
	 dummyEntry24(81, "dummyEntry24", false, null, null),
	 dummyEntry25(82, "dummyEntry25", false, null, null),
	 dummyEntry26(83, "dummyEntry26", false, null, null),
	 dummyEntry27(84, "dummyEntry27", false, null, null),
	 dummyEntry28(85, "dummyEntry28", false, null, null),
	 dummyEntry29(86, "dummyEntry29", false, null, null),
	 dummyEntry30(87, "dummyEntry30", false, null, null),
	 /*
	  * <END>
	  * Of Dummy entries
	  */

	 /*
	  * Now the bad boys! new class ids :)) (3rd classes)
	  */
	 duelist(0x58, "Duelist", false, Race.Human, gladiator),
	 dreadnought(0x59, "Dreadnought", false, Race.Human, warlord),
	 phoenixKnight(0x5a, "Phoenix Knight", false, Race.Human, paladin),
	 hellKnight(0x5b, "Hell Knight", false, Race.Human, darkAvenger),
	 sagittarius(0x5c, "Sagittarius", false, Race.Human, hawkeye),
	 adventurer(0x5d, "Adventurer", false, Race.Human, treasureHunter),
	 archmage(0x5e, "Archmage", true, Race.Human, sorceror),
	 soultaker(0x5f, "Soultaker", true, Race.Human, necromancer),
	 arcanaLord(0x60, "Arcana Lord", true, true, Race.Human, warlock),
	 cardinal(0x61, "Cardinal", true, Race.Human, bishop),
	 hierophant(0x62, "Hierophant", false, Race.Human, prophet),

	 evaTemplar(0x63, "Eva Templar", false, Race.Elf, templeKnight),
	 swordMuse(0x64, "Sword Muse", false, Race.Elf, swordSinger),
	 windRider(0x65, "Wind Rider", false, Race.Elf, plainsWalker),
	 moonlightSentinel(0x66, "Moonlight Sentinel", false, Race.Elf, silverRanger),
	 mysticMuse(0x67, "Mystic Muse", true, Race.Elf, spellsinger),
	 elementalMaster(0x68, "Elemental Master", true, true, Race.Elf, elementalSummoner),
	 evaSaint(0x69, "Eva Saint", true, Race.Elf, elder),

	 shillienTemplar(0x6a, "Shillien Templar", false, Race.DarkElf, shillienKnight),
	 spectralDancer(0x6b, "Spectral Dancer", false, Race.DarkElf, bladedancer),
	 ghostHunter(0x6c, "Ghost Hunter", false, Race.DarkElf, abyssWalker),
	 ghostSentinel(0x6d, "Ghost Sentinel", false, Race.DarkElf, phantomRanger),
	 stormScreamer(0x6e, "Storm Screamer", true, Race.DarkElf, spellhowler),
	 spectralMaster(0x6f, "Spectral Master", true, true, Race.DarkElf, phantomSummoner),
	 shillienSaint(0x70, "Shillien Saint", true, Race.DarkElf, shillienElder),

	 titan(0x71, "Titan", false, Race.Orc, destroyer),
	 grandKhauatari(0x72, "Grand Khavatari", false, Race.Orc, tyrant),
	 dominator(0x73, "Dominator", true, Race.Orc, overlord),
	 doomcryer(0x74, "Doomcryer", true, Race.Orc, warcryer),

	 fortuneSeeker(0x75, "Fortune Seeker", false, Race.Dwarf, bountyHunter),
	 maestro(0x76,"Maestro", false, Race.Dwarf, warsmith),

	 dummyEntry31(119, "dummyEntry31", false, null, null),
	 dummyEntry32(120, "dummyEntry32", false, null, null),
	 dummyEntry33(121, "dummyEntry33", false, null, null),
	 dummyEntry34(122, "dummyEntry34", false, null, null),


	 maleSoldier(0x7B, "Male Soldier", false, Race.Kamael, null),
	 femaleSoldier(0x7C, "Female Soldier", false, Race.Kamael, null),
	 dragoon(0x7D, "Trooper", false, Race.Kamael, maleSoldier),
	 warder(0x7E, "Warder", false, Race.Kamael, femaleSoldier),
	 berserker(0x7F, "Berserker", false, Race.Kamael, dragoon),
	 maleSoulbreaker(0x80, "Male Soulbreaker", false, Race.Kamael, dragoon),
	 femaleSoulbreaker(0x81, "Female Soulbreaker", false, Race.Kamael, warder),
	 arbalester(0x82, "Arbalester", false, Race.Kamael, warder),
	 doombringer(0x83, "Doombringer", false, Race.Kamael, berserker),
	 maleSoulhound(0x84," Male Soulhound", false, Race.Kamael, maleSoulbreaker),
	 femaleSoulhound(0x85, "Female Soulhound", false, Race.Kamael, femaleSoulbreaker),
	 trickster(0x86, "Trickster", false, Race.Kamael, arbalester),
	 inspector(0x87, "Inspector", false, Race.Kamael, dragoon),
	 judicator(0x88, "Judicator", false, Race.Kamael, inspector);
	 
/** The Identifier of the Class */
private final int     _id;

private final String _name;

/** True if the class is a mage class */
private final boolean _isMage;

/** True if the class is a summoner class */
private final boolean _isSummoner;

/** The Race object of the class */
private final Race    _race;

/** The parent ClassId or null if this class is a root */
private final ClassId _parent;


/**
 * Constructor of ClassId.<BR><BR>
 */
private ClassId(int id, String name,  boolean isMage, Race race, ClassId parent)
{
	_id = id;
	_name = name;
	_isMage = isMage;
	_isSummoner = false;
	_race = race;
	_parent = parent;
}


/**
 * Constructor of ClassId.<BR><BR>
 */
private ClassId(int pId, String pName, boolean pIsMage, boolean pIsSummoner, Race pRace, ClassId pParent)
{
	_id = pId;
	_name = pName;
	_isMage = pIsMage;
	_isSummoner = pIsSummoner;
	_race = pRace;
	_parent = pParent;
}

/**
 * Return the Identifier of the Class.<BR><BR>
 */
public final int getId()
{
	return _id;
}

/**
 * Return the display name of the Class.<BR>
 * <BR>
 */
public final String getName()
{
	return _name;
}
/**
 * Return True if the class is a mage class.<BR><BR>
 */
public final boolean isMage()
{
	return _isMage;
}

/**
 * Return True if the class is a summoner class.<BR><BR>
 */
public final boolean isSummoner()
{
	return _isSummoner;
}

/**
 * Return the Race object of the class.<BR><BR>
 */
public final Race getRace()
{
	return _race;
}

/**
 * Return True if this Class is a child of the selected ClassId.<BR><BR>
 * 
 * @param cid The parent ClassId to check
 * 
 */
public final boolean childOf(ClassId cid)
{
	if (_parent == null)
		return false;
	
	if (_parent == cid)
		return true;
	
	return _parent.childOf(cid);
	
}

/**
 * Return True if this Class is equal to the selected ClassId or a child of the selected ClassId.<BR><BR>
 * 
 * @param cid The parent ClassId to check
 * 
 */
public final boolean equalsOrChildOf(ClassId cid)
{
	return this == cid || childOf(cid);
}


/**
 * Return the child level of this Class (0=root, 1=child leve 1...).<BR><BR>
 * 
 * @param cid The parent ClassId to check
 * 
 */
public final int level()
{
	if (_id != 0x87 && _parent == null)
		return 0;
	if (_id == 0x87)
		return 2;
	if (_id == 0x88)
		return 3;
	return 1 + _parent.level();
}

/**
 * Return its parent ClassId<BR><BR>
 * 
 */
public final ClassId getParent()
{
	return _parent;
}
}
