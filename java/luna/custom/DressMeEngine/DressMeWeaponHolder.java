package luna.custom.DressMeEngine;

import java.util.ArrayList;
import java.util.List;

import luna.custom.data.xml.AbstractHolder;


public final class DressMeWeaponHolder extends AbstractHolder
{
	private static final DressMeWeaponHolder _instance = new DressMeWeaponHolder();
	
	public static DressMeWeaponHolder getInstance()
	{
		return _instance;
	}
	
	private final List<DressMeWeaponData> _weapons = new ArrayList<>();
	
	public void addWeapon(DressMeWeaponData weapon)
	{
		_weapons.add(weapon);
	}
	
	public List<DressMeWeaponData> getAllWeapons()
	{
		return _weapons;
	}
	
	public DressMeWeaponData getWeapon(int id)
	{
		for (DressMeWeaponData weapon : _weapons)
		{
			if (weapon.getId() == id)
			{
				return weapon;
			}
		}
		
		return null;
	}
	
	public int size()
	{
		return _weapons.size();
	}
	
	public void clear()
	{
		_weapons.clear();
	}
}