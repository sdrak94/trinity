package luna.custom.DressMeEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.handler.VoicedCommandHandler;



public class DressMeLoader
{
	private static final Logger _log = Logger.getLogger(DressMeLoader.class.getName());
	
	public static Map<Integer, DressMeWeaponData> SWORD;
	public static Map<Integer, DressMeWeaponData> BLUNT;
	public static Map<Integer, DressMeWeaponData> DAGGER;
	public static Map<Integer, DressMeWeaponData> BOW;
	public static Map<Integer, DressMeWeaponData> POLE;
	public static Map<Integer, DressMeWeaponData> FIST;
	public static Map<Integer, DressMeWeaponData> DUAL;
	public static Map<Integer, DressMeWeaponData> DUALFIST;
	public static Map<Integer, DressMeWeaponData> BIGSWORD;
	public static Map<Integer, DressMeWeaponData> ROD;
	public static Map<Integer, DressMeWeaponData> BIGBLUNT;
	public static Map<Integer, DressMeWeaponData> CROSSBOW;
	public static Map<Integer, DressMeWeaponData> RAPIER;
	public static Map<Integer, DressMeWeaponData> ANCIENTSWORD;
	public static Map<Integer, DressMeWeaponData> DUALDAGGER;
	
	public static Map<Integer, DressMeArmorData> LIGHT;
	public static Map<Integer, DressMeArmorData> HEAVY;
	public static Map<Integer, DressMeArmorData> ROBE;
	
	public static Map<Integer, DressMeHatData> HAIR;
	public static Map<Integer, DressMeHatData> HAIR2;
	public static Map<Integer, DressMeHatData> HAIR_FULL;

	private static final DressMeLoader _instance = new DressMeLoader();
	
	public static DressMeLoader getInstance()
	{
		return _instance;
	}
	
	public static void load()
	{
		DressMeArmorParser.getInstance().load();
		DressMeCloakParser.getInstance().load();
		DressMeHatParser.getInstance().load();
		DressMeShieldParser.getInstance().load();
		DressMeWeaponParser.getInstance().load();
		
		
		SWORD = new HashMap<>();
		BLUNT = new HashMap<>();
		DAGGER = new HashMap<>();
		BOW = new HashMap<>();
		POLE = new HashMap<>();
		FIST = new HashMap<>();
		DUAL = new HashMap<>();
		DUALFIST = new HashMap<>();
		BIGSWORD = new HashMap<>();
		ROD = new HashMap<>();
		BIGBLUNT = new HashMap<>();
		CROSSBOW = new HashMap<>();
		RAPIER = new HashMap<>();
		ANCIENTSWORD = new HashMap<>();
		DUALDAGGER = new HashMap<>();
		
		LIGHT = new HashMap<>();
		HEAVY = new HashMap<>();
		ROBE = new HashMap<>();
		
		HAIR = new HashMap<>();
		HAIR2 = new HashMap<>();
		HAIR_FULL = new HashMap<>();
		
		parseWeapon();
		parseArmor();
		parseHat();
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new DressMeVCmd());
	}
	public void reload()
	{
		DressMeArmorHolder.getInstance().clear();
		DressMeCloakHolder.getInstance().clear();
		DressMeHatHolder.getInstance().clear();
		DressMeWeaponHolder.getInstance().clear();
	    load();
	}
	
	private static int parseWeapon()
	{
		int Sword = 1, Blunt = 1, Dagger = 1, Bow = 1, Pole = 1, Fist = 1, DualSword = 1, DualFist = 1, BigSword = 1, Rod = 1, BigBlunt = 1, Crossbow = 1, Rapier = 1, AncientSword = 1, DualDagger = 1;
		
		for (DressMeWeaponData weapon : DressMeWeaponHolder.getInstance().getAllWeapons())
		{
			if (weapon.getType().equals("Sword"))
			{
				SWORD.put(Sword, weapon);
				Sword++;
			}
			else if (weapon.getType().equals("Blunt"))
			{
				BLUNT.put(Blunt, weapon);
				Blunt++;
			}
			else if (weapon.getType().equals("Big Sword"))
			{
				BIGSWORD.put(BigSword, weapon);
				BigSword++;
			}
			else if (weapon.getType().equals("Big Blunt"))
			{
				BIGBLUNT.put(BigBlunt, weapon);
				BigBlunt++;
			}
			else if (weapon.getType().equals("Dagger"))
			{
				DAGGER.put(Dagger, weapon);
				Dagger++;
			}
			else if (weapon.getType().equals("Bow"))
			{
				BOW.put(Bow, weapon);
				Bow++;
			}
			else if (weapon.getType().equals("Pole"))
			{
				POLE.put(Pole, weapon);
				Pole++;
			}
			else if (weapon.getType().equals("Dual Fist"))
			{
				DUALFIST.put(Fist, weapon);
				Fist++;
			}
			else if (weapon.getType().equals("Dual Sword"))
			{
				DUAL.put(DualSword, weapon);
				DualSword++;
			}
			else if (weapon.getType().equals("Dual Fist"))
			{
				DUALFIST.put(DualFist, weapon);
				DualFist++;
			}
			else if (weapon.getType().equals("Rod"))
			{
				ROD.put(Rod, weapon);
				Rod++;
			}
			else if (weapon.getType().equals("Crossbow"))
			{
				CROSSBOW.put(Crossbow, weapon);
				Crossbow++;
			}
			else if (weapon.getType().equals("Rapier"))
			{
				RAPIER.put(Rapier, weapon);
				Rapier++;
			}
			else if (weapon.getType().equals("Ancient"))
			{
				ANCIENTSWORD.put(AncientSword, weapon);
				AncientSword++;
			}
			else if (weapon.getType().equals("Dual Dagger"))
			{
				DUALDAGGER.put(DualDagger, weapon);
				DualDagger++;
			}
			else
			{
				_log.config("Dress me system: Can't find type: " + weapon.getType());
			}
		}
		
		_log.config("Dress me system: Loaded " + (Sword - 1) + " Sword(s).");
		_log.config("Dress me system: Loaded " + (Blunt - 1) + " Blunt(s).");
		_log.config("Dress me system: Loaded " + (Dagger - 1) + " Dagger(s).");
		_log.config("Dress me system: Loaded " + (Bow - 1) + " Bow(s).");
		_log.config("Dress me system: Loaded " + (Pole - 1) + " Pole(s).");
		_log.config("Dress me system: Loaded " + (Fist - 1) + " Fist(s).");
		_log.config("Dress me system: Loaded " + (DualSword - 1) + " Dual Sword(s).");
		_log.config("Dress me system: Loaded " + (DualFist - 1) + " Dual Fist(s).");
		_log.config("Dress me system: Loaded " + (BigSword - 1) + " Big Sword(s).");
		_log.config("Dress me system: Loaded " + (Rod - 1) + " Rod(s).");
		_log.config("Dress me system: Loaded " + (BigBlunt - 1) + " Big Blunt(s).");
		_log.config("Dress me system: Loaded " + (Crossbow - 1) + " Crossbow(s).");
		_log.config("Dress me system: Loaded " + (Rapier - 1) + " Rapier(s).");
		_log.config("Dress me system: Loaded " + (AncientSword - 1) + " Ancient Sword(s).");
		_log.config("Dress me system: Loaded " + (DualDagger - 1) + " Dual Dagger(s).");
		
		return 0;
	}
	
	private static int parseArmor()
	{
		int light = 1, heavy = 1, robe = 1;
		
		for (DressMeArmorData armor : DressMeArmorHolder.getInstance().getAllDress())
		{
			if (armor.getType().equals("LIGHT"))
			{
				LIGHT.put(light, armor);
				light++;
			}
			else if (armor.getType().equals("HEAVY"))
			{
				HEAVY.put(heavy, armor);
				heavy++;
			}
			else if (armor.getType().equals("ROBE"))
			{
				ROBE.put(robe, armor);
				robe++;
			}
			else
			{
				_log.config("Dress me system: Can't find type: " + armor.getType());
			}
		}
		
		_log.config("Dress me system: Loaded " + (light - 1) + " Light Armor(s).");
		_log.config("Dress me system: Loaded " + (heavy - 1) + " Heavy Armor(s).");
		_log.config("Dress me system: Loaded " + (robe - 1) + " Robe Armor(s).");
		
		return 0;
	}
	
	private static int parseHat()
	{
		int hair = 1, hair2 = 1, full_hair = 1;
		
		for (DressMeHatData hat : DressMeHatHolder.getInstance().getAllHats())
		{
			if (hat.getSlot() == 1)
			{
				HAIR.put(hair, hat);
				hair++;
			}
			else if (hat.getSlot() == 2)
			{
				HAIR2.put(hair2, hat);
				hair2++;
			}
			else if (hat.getSlot() == 3)
			{
				HAIR_FULL.put(full_hair, hat);
				full_hair++;
			}
			else
			{
				_log.config("Dress me system: Can't find slot: " + hat.getSlot());
			}
		}
		
		_log.config("Dress me system: Loaded " + (hair - 1) + " Hair(s).");
		_log.config("Dress me system: Loaded " + (hair2 - 1) + " Hair2(s).");
		_log.config("Dress me system: Loaded " + (full_hair - 1) + " Full Hair(s).");
		
		return 0;
	}
}