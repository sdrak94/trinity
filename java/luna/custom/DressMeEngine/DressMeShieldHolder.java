package luna.custom.DressMeEngine;

import java.util.ArrayList;
import java.util.List;

import luna.custom.data.xml.AbstractHolder;


public final class DressMeShieldHolder extends AbstractHolder
{
	private static final DressMeShieldHolder _instance = new DressMeShieldHolder();
	
	public static DressMeShieldHolder getInstance()
	{
		return _instance;
	}
	
	private final List<DressMeShieldData> _shield = new ArrayList<>();
	
	public void addShield(DressMeShieldData shield)
	{
		_shield.add(shield);
	}
	
	public List<DressMeShieldData> getAllShields()
	{
		return _shield;
	}
	
	public DressMeShieldData getShield(int id)
	{
		for (DressMeShieldData shield : _shield)
		{
			if (shield.getId() == id)
			{
				return shield;
			}
		}
		
		return null;
	}
	
	public int size()
	{
		return _shield.size();
	}
	
	public void clear()
	{
		_shield.clear();
	}
}