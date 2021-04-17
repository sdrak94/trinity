package luna.custom.DressMeEngine;

import java.util.ArrayList;
import java.util.List;

import luna.custom.data.xml.AbstractHolder;


public final class DressMeArmorHolder extends AbstractHolder
{
	private static final DressMeArmorHolder _instance = new DressMeArmorHolder();
	
	public static DressMeArmorHolder getInstance()
	{
		return _instance;
	}
	
	private final List<DressMeArmorData> _dress = new ArrayList<>();
	
	public void addDress(DressMeArmorData armorset)
	{
		_dress.add(armorset);
	}
	
	public List<DressMeArmorData> getAllDress()
	{
		return _dress;
	}
	
	public DressMeArmorData getArmor(int id)
	{
		for (DressMeArmorData dress : _dress)
		{
			if (dress.getId() == id)
			{
				return dress;
			}
		}
		
		return null;
	}
	
	public int size()
	{
		return _dress.size();
	}
	
	public void clear()
	{
		_dress.clear();
	}
}