package net.sf.l2j.gameserver.skills;

import java.util.NoSuchElementException;

public enum Stats
{
//
// Base stats, for each in Calculator a slot is allocated
//
	MAX_HP("maxHp", "Maximum HP"),
	MAX_MP("maxMp", "Maximum MP"),
	MAX_CP("maxCp", "Maximum CP"),
	REGENERATE_HP_RATE("regHp", "HP Recovery"),
	REGENERATE_CP_RATE("regCp", "CP Recovery"),
	REGENERATE_MP_RATE("regMp", "MP Recovery"),
	RECHARGE_MP_RATE("gainMp", ""),
	HEAL_EFFECTIVNESS("gainHp", ""),
	HEAL_PROFICIENCY("giveHp", ""),
	HEAL_STATIC_BONUS("bonusHp", ""),

	POWER_DEFENCE("pDef", "P. Def."),
	MAGIC_DEFENCE("mDef", "M. Def."),

	POWER_DEFENCE_BEHIND("pDefBehindVuln", ""),
	MAGIC_DEFENCE_BEHIND("mDefBehindVuln", ""),
	HAMSTRING("hamstring", ""),
	POWER_ATTACK("pAtk", "P. Atk."),
	MAGIC_ATTACK("mAtk", "M. Atk."),
	POWER_ATTACK_SPEED("pAtkSpd", ""),
	MAGIC_ATTACK_SPEED("mAtkSpd", ""), // how fast a spell is casted (including animation)
	MAGIC_REUSE_RATE("mReuse", ""), // how fast spells becomes ready to reuse
	SHIELD_DEFENCE("sDef", ""),
	CRITICAL_DAMAGE("cAtk", ""),
	CRITICAL_DAMAGE_ADD("cAtkAdd", ""), // this is another type for special critical damage mods - vicious stance, crit power and crit damage SA		// it was totally bad since now...
	PVP_PHYSICAL_DMG("pvpPhysDmg", ""),
	PVP_MAGICAL_DMG("pvpMagicalDmg", ""),
	PVP_PHYS_SKILL_DMG("pvpPhysSkillsDmg", ""),
	SKILL_DAM_MULTI("skillDam", ""),

	PVP_PHYSICAL_VUL("pvpPhysVul", ""),
	PVP_MAGICAL_VUL("pvpMagicalVul", ""),
	PVP_PHYS_SKILL_VUL("pvpPhysSkillsVul", ""),

	SAME_RACE_DMG_VUL("racialDamVul", ""),
	HATED_RACE_DMG_BOOST("racialDamBoost", ""),

	EVASION_RATE("rEvas", "Dodge"),
	P_SKILL_EVASION("pSkillEvas", ""),
	P_SKILL_EVASION_REDUCTION("pSkillEvasReduce", ""),
	M_SKILL_EVASION("mSkillEvas", ""),
	M_SKILL_EVASION_REDUCTION("mSkillEvasReduce", ""),
	CRIT_DAMAGE_EVASION("critDamEvas", ""),
	SKILL_CRIT_DAMAGE_EVASION("skillCritDamEvas", ""),
	M_CRIT_DAMAGE_EVASION("mCritDamEvas", ""),
	SHIELD_RATE("rShld", ""),
	CRITICAL_RATE("rCrit", "Critical"),
	BLOW_RATE("blowRate", ""),
	LETHAL_RATE("lethalRate", ""),
	MCRITICAL_RATE("mCritRate", ""),
	EXPSP_RATE("rExp", ""),
	DROP_COUNT_RATE("rDrop", ""),
	/*ATTACK_CANCEL("cancel", ""),*/
	LETHAL_IMMUNITY("lethalImmunity", ""),

	ACCURACY_COMBAT("accCombat", "Accuracy"),
	POWER_ATTACK_RANGE("pAtkRange", ""),
	MAGIC_ATTACK_RANGE("mAtkRange", ""),
	POWER_ATTACK_ANGLE("pAtkAngle", ""),
	ATTACK_COUNT_MAX("atkCountMax", ""),

	RUN_SPEED("runSpd", ""),
	WALK_SPEED("walkSpd", ""),
	SOUL_DOUBLING("doubleSoul", ""),

	STAT_STR("STR", ""),
	STAT_CON("CON", ""),
	STAT_DEX("DEX", ""),
	STAT_INT("INT", ""),
	STAT_WIT("WIT", ""),
	STAT_MEN("MEN", ""),

	BREATH("breath", ""),
	AGGRESSION("aggression", ""), // locks a mob on tank caster
	BLEED("bleed", ""), // by daggers, like poison
	POISON("poison", ""), // by magic, hp dmg over time
	STUN("stun", ""), // disable move/ATTACK for a period of time
	ROOT("root", ""), // disable movement, but not ATTACK
	MOVEMENT("movement", ""), // slowdown movement, debuff
	CONFUSION("confusion", ""), // mob changes target, opposite to aggression/hate
	SLEEP("sleep", ""), // sleep (don't move/ATTACK) until attacked
	VALAKAS("valakas", ""),
	VALAKAS_RES("valakasRes", ""),

	AGGRESSION_VULN("aggressionVuln", ""),
	BLEED_VULN("bleedVuln", ""),
	POISON_VULN("poisonVuln", ""),
	STUN_VULN("stunVuln", ""),
	PARALYZE_VULN("paralyzeVuln", ""),
	ROOT_VULN("rootVuln", ""),
	CANCEL_VULN("cancelVuln", ""), // Resistance for cancel type skills
	DERANGEMENT_VULN("derangementVuln", ""),
	DEBUFF_VULN("debuffVuln", ""),
	SLEEP_VULN("sleepVuln", ""),
	CONFUSION_VULN("confusionVuln", ""),
	MOVEMENT_VULN("movementVuln", ""),
	FIRE_RES("fireRes", ""),
	WIND_RES("windRes", ""),
	WATER_RES("waterRes", ""),
	EARTH_RES("earthRes", ""),
	HOLY_RES("holyRes", ""),
	DARK_RES("darkRes", ""),

	FIRE_POWER("firePower", ""),
	WATER_POWER("waterPower", ""),
	WIND_POWER("windPower", ""),
	EARTH_POWER("earthPower", ""),
	HOLY_POWER("holyPower", ""),
	DARK_POWER("darkPower", ""),

	BUFF_VULN("buffVuln", ""),
	CRIT_VULN("critVuln", ""), // Resistance to Crit DMG.
	MAGIC_DAMAGE_VULN("magicDamVul", ""),
	STUN_DURATION_REDUCE("stunReduce", ""),
	DEBUFF_DURATION_REDUCE("debuffReduce", ""),

	AGGRESSION_PROF("aggressionProf", ""),
	BLEED_PROF("bleedProf", ""),
	POISON_PROF("poisonProf", ""),
	STUN_PROF("stunProf", ""),
	PARALYZE_PROF("paralyzeProf", ""),
	ROOT_PROF("rootProf", ""),
	SLEEP_PROF("sleepProf", ""),
	CONFUSION_PROF("confusionProf", ""),
	PROF("movementProf", ""),
	CANCEL_PROF("cancelProf", ""),
	DERANGEMENT_PROF("derangementProf", ""),
	DEBUFF_PROF("debuffProf", ""),

	NONE_WPN_VULN("noneWpnVuln", ""), // Shields!!!
	SWORD_WPN_VULN("swordWpnVuln", ""),
	BLUNT_WPN_VULN("bluntWpnVuln", ""),
	DAGGER_WPN_VULN("daggerWpnVuln", ""),
	BOW_WPN_VULN("bowWpnVuln", ""),
	CROSSBOW_WPN_VULN("crossbowWpnVuln", ""),
	POLE_WPN_VULN("poleWpnVuln", ""),
	ETC_WPN_VULN("etcWpnVuln", ""),
	FIST_WPN_VULN("fistWpnVuln", ""),
	DUAL_WPN_VULN("dualWpnVuln", ""),
	DUALFIST_WPN_VULN("dualFistWpnVuln", ""),
	BIGSWORD_WPN_VULN("bigSwordWpnVuln", ""),
	BIGBLUNT_WPN_VULN("bigBluntWpnVuln", ""),
	DUALDAGGER_WPN_VULN("dualDaggerWpnVuln", ""),
	RAPIER_WPN_VULN("rapierWpnVuln", ""),
	ANCIENT_WPN_VULN("ancientWpnVuln", ""),
	PET_WPN_VULN("petWpnVuln", ""),

	REFLECT_DAMAGE_PERCENT("reflectDam", ""),
	REFLECT_ARROWS("reflectArrows", ""),
	REFLECT_SKILL_MAGIC("reflectSkillMagic", ""),
	REFLECT_SKILL_PHYSIC("reflectSkillPhysic", ""),
	VENGEANCE_SKILL_MAGIC_DAMAGE("vengeanceMdam", ""),
	VENGEANCE_SKILL_PHYSICAL_DAMAGE("vengeancePdam", ""),
	VENGEANCE_RANGED_SKILL_DAMAGE("vengeanceRangedDam", ""),

	ABSORB_DAMAGE_CHANCE("absorbDamChance", ""),
	ABSORB_DAMAGE_PERCENT("absorbDam", ""),
	ABSORB_DAMAGE_STATIC("absorbDamStatic", ""),
	ABSORB_DAMAGE_PERCENT_SKILL("absorbDamSkill", ""),

	TRANSFER_DAMAGE_PERCENT("transDam", ""),
	TRANSFER_DAMAGE_MANA("transDamMana", ""),
	ABSORB_MANA_DAMAGE_PERCENT("absorbDamMana", ""),

	STONESKIN("stoneSkin", ""),
	DISARM_VULN("disarmVuln", ""),

	PATK_PLANTS("pAtk-plants", ""),
	PATK_INSECTS("pAtk-insects", ""),
	PATK_ANIMALS("pAtk-animals", ""),
	PATK_MONSTERS("pAtk-monsters", ""),
	PATK_DEMONS("pAtk-demons", ""),
	PATK_UNDEAD("pAtk-undead", ""),
	PATK_DRAGONS("pAtk-dragons", ""),
	PATK_GIANTS("pAtk-giants", ""),
	PATK_MCREATURES("pAtk-mcreatures", ""),
	PDEF_PLANTS("pDef-plants", ""),
	PDEF_INSECTS("pDef-insects", ""),
	PDEF_ANIMALS("pDef-animals", ""),
	PDEF_MONSTERS("pDef-monsters", ""),
	PDEF_DRAGONS("pDef-dragons", ""),
	PDEF_GIANTS("pDef-giants", ""),

	ATK_REUSE("atkReuse", ""),
	P_REUSE("pReuse", ""),

	INV_LIM("inventoryLimit", ""),
	WH_LIM("whLimit", ""),
	FREIGHT_LIM("FreightLimit", ""),
	P_SELL_LIM("PrivateSellLimit", ""),
	P_BUY_LIM("PrivateBuyLimit", ""),
	REC_D_LIM("DwarfRecipeLimit", ""),
	REC_C_LIM("CommonRecipeLimit", ""),

	//C4 Stats
	PHYSICAL_MP_CONSUME_RATE("PhysicalMpConsumeRate", ""),
	MAGICAL_MP_CONSUME_RATE("MagicalMpConsumeRate", ""),
	DANCE_MP_CONSUME_RATE("DanceMpConsumeRate", ""),
	BOW_MP_CONSUME_RATE("BowMpConsumeRate", ""),
	HP_CONSUME_RATE("HpConsumeRate", ""),
	MP_CONSUME("MpConsume", ""),

	//T1 stats
	transformId("transformId", ""),
	TALISMAN_SLOTS("talisman", ""),

	//Shield Stats
	SHIELD_DEFENCE_ANGLE("shieldDefAngle", ""),

	//Skill mastery
	SKILL_MASTERY			("skillMastery", ""),

	AOE_HIT_OTHER_PERCENT    ("aoePercent", ""),
	ABSORB_CP_PERCENT ("absorbCP", ""),
	PET_NO_UNSUMMON_AFTER_OWNER_DIE ("summonslive", ""),
	MAGIC_CRITICAL_DAMAGE ("cAtkMagic", ""),
	PHYS_SKILL_CRITICAL_DAMAGE ("cAtkSkill", ""),

	BACK_ANGLE_INCREASE ("backAngle", ""),
	KNOCKBACK_CHANCE ("knockback", ""),
	KNOCKBACK_DISTANCE_ADD ("knockbackDistAdd", ""),
	KNOCKBACK_TYPE ("knockbackType", ""),
	PVM_DAMAGE ("pvmDam", ""),
	PVM_DAMAGE_VUL ("pvmVuln", ""),
	CRIT_MAX_ADD ("critMax", ""),
	CRIT_MAGIC_MAX_ADD ("mCritMax", ""),
	BLOCK_RATE_MAX ("rShldMax", ""),
	RESIST_REMOVE_TARGET ("conc", ""),
	ROBE_DAM_MUL ("damRobe", ""),
	HEAVY_DAM_MUL("damHeavy", ""),
	LIGHT_DAM_MUL("damLight", ""),
	CAST_SPEED_MAX_ADD ("mAtkSpdMaxAdd", ""),
	SKILL_CRITICAL_CHANCE_INCREASE ("skillCritAdd", ""),

	SPELL_CANCEL_RES ("spellCancelRes", ""),
	SPELL_CANCEL_ADD ("spellCancelAdd", ""),
	ATTACK_CANCEL_ADD ("attackCancelAdd", ""),

	MIN_LAND_RATE ("minLandRate", ""),
	MAX_LAND_RATE ("maxLandRate", ""),

	IMPROVED_EVASION ("improvedEvasion", ""),

	MORE_DEBUFF ("moreDebuff", ""),
	LESS_DEBUFF ("lessDebuff", ""),

	PERF_BLOCK_ADD("rShldPerfAdd", ""),
	FORCE_DAM("forceDam", ""),
	CHARGE_MAX_ADD("chargeMax", ""),
	CHARGE_REDUCE("reduceCharge", ""),
	SOUL_MAX("soulMax", ""),
	POWER_ATTACK_SPEED_MAX_ADD("pAtkSpdMax", ""),

	CRITICAL_DMG_ADD_BLEEDING ("cAtkBleed", ""),
	IGNORE_ELEMENTALS ("negateEle", ""),
	IGNORE_SHIELD ("negateShld", ""),
	IGNORE_AUTOTARGET_SKILL ("negateAutoTarget", ""),
	IGNORE_AUTOTARGET_ATTACK ("negateAtkTarget", ""),

	RUSH_DIST_ADD ("rushDistAdd", ""),
	DMG_ADD("addDmg", ""),
	DMG_REMOVE("delDmg", ""),
	DMG_REMOVE_SHIELD("delDmgShld", ""),
	KNOCKBACK_RESIST ("knockbackRes", ""),

	TANK_ARCHERY     ("tankBow", ""),
	TANK_SPELLS     ("tankSpell", ""),
	RANGED_PIERCING		("rangedPiercing", ""),
	CHAIN_SHOT			("doubleShot", ""),
	RANGE_DMG_DIST_BOOST	("distDmgBoost", ""),
	RANGE_DMG_DIST_BOOST_SKILL	("distDmgBoostSkill", ""),
	SKILL_RADIUS_BOOST	("skillRadiusBoost", ""),
	DRAIN_PERC_BOOST	("drainBoost", ""),
	SKILL_POWER_BOOST ("skillPowerBoost", ""),

	RES_DISABLE ("disableRes", ""),
	RES_UNDISABLE ("undisableRes", ""),

	SKILL_AREA_ANGLE_MOD ("skillAreaAngleMod", ""),
	SKILL_TARGET_TYPE_CHANGE ("skillTargetTypeChange", ""),
	SKILL_RETRY_CHANGE ("skillRetryChange", ""),
	SKILL_REUSE_CHANGE ("skillReuseChange", ""),
	SKILL_HITTIME_CHANGE ("skillHitTimeChange", ""),
	EFFECT_DURATION_CHANGE ("effectDurationChange", ""),
	EFFECT_POWER_BOOST ("effectPowerBoost", ""),
	UNTARGETABLE ("untargetable", ""),

	INC_PHYSDAM_CHARGES ("pAtk_charge", ""),
	INC_PHYSDAM_SOULS ("pAtk_soul", ""),

	INC_DAM_MP ("moreDamMP", ""),
	INC_DAM_HP ("moreDamHP", ""),
	INC_DAM_CP ("moreDamCP", ""),

	EVASION_ABSOLUTE("rEvasAbs", ""),
	EVADE_AOE_HIT("evadeAOEHit", ""),
	EVADE_AOE_SPELL("evadeAOESpell", ""),

	BOW_AOE_RADIUS("bowAOERadius", ""),
	BOW_AOE_DMG_MOD("bowAOEDmgMod", ""),

	PDEF_IGNORE("pDefIgnore", ""),
	MDEF_IGNORE("mDefIgnore", ""),

	PDEF_REDUCE("reducePdef", ""),
	MDEF_REDUCE("reduceMdef", ""),

	PDAM_MAX("maxPdam", ""),
	MDAM_MAX("maxMdam", ""),

	DAMAGE_STRAIGHT_TO_HP("deleteLife", ""),
	SEE_INVISIBLE("seeInvisible", ""),

	RESIST_WEAPON_S("resistSGradeWeapons", ""),
	RESIST_WEAPON_S80("resistS80GradeWeapons", ""),
	RESIST_WEAPON_S84("resistS84GradeWeapons", ""),

	RESIST_WEAPON_NORMAL_S("resistNormalWeapons", ""),
	RESIST_WEAPON_DYNASTY("resistDynastyWeapons", ""),
	RESIST_WEAPON_ICARUS("resistIcarusWeapons", ""),
	RESIST_WEAPON_VESPER("resistVesperWeapons", ""),
	RESIST_WEAPON_TITANIUM("resistTitaniumWeapons", ""),
	RESIST_WEAPON_DREAD("resistDreadWeapons", ""),

	OVERPOWER("overpower", ""),
	OVERDRIVE("overdrive", ""),
	REFLECT_RES("reflectRes", ""),
	LIONHEART("lionheart", ""), //debuff receive ressist
	PHAZE_MOVEMENT("phazeMove", ""),
	DEMONIC_MOVEMENT("demonicMove", ""),
	EXTRA_ATTACK("extraHit", ""),

	// vitality
	VITALITY_CONSUME_RATE("vitalityConsumeRate", ""),
	//summon boosts
	SUMMON_HP_BOOST("summonHPBoost", ""),
	SUMMON_PATK_BOOST("summonPAtkBoost", ""),
	SUMMON_MATK_BOOST("summonMAtkBoost", "");	

public static final int NUM_STATS = values().length;

private String _value;
private String _name;

public String getValue()
{
	return _value;
}
public String getName()
{
return _name;

}
private Stats(String s, String n)
{
	_value = s;
	_name = n;
}

public static Stats valueOfXml(String name)
{
	name = name.intern();
	for (Stats s : values())
	{
		if (s.getValue().equalsIgnoreCase(name))
			return s;
	}
	
	throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
}

public static Stats valueOfString(String name)
{
	for (Stats s : values())
	{
		if (s.getValue().equalsIgnoreCase(name))
			return s;
	}
	
	return null;
}

public String getHtmlDesc()
{
	//TODO
	return toString();
}

public enum Color
{
	Default("FFFFFF"),
	FighterStat("CB9E96"), // dark red
	MagicStat("8FB4D1"), // light blue
	BalancedStat("ABB6AB"), // light grey
	Special("DEAE83"), // orange
	MaxHP("FF5555"), // red
	MaxCP("F2D94A"), // yellow
	MaxMP("5555FF"), // darker blue
	Resists("4D723D"), // green
	Proficiency("773D8D"), // purple
	PvPDmg("796559"), // brown
	PvEDmg("696969"), // grey
	Rare("FFF793"), // yellow 
	Summon("E17D1A"); // orange
	
	public String code;
	Color(String c)
	{
		code = c;
	}
}

public Color getColor()
{
	//TODO
	return Color.Default;
}

}
