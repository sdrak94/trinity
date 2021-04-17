package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.funcs.FuncAdd;
import net.sf.l2j.gameserver.skills.funcs.LambdaConst;

public final class Elementals
{
	private ElementalStatBoni _boni = null;
	public final static byte NONE = -1;
	public final static byte FIRE = 0;
	public final static byte WATER = 1;
	public final static byte WIND = 2;
	public final static byte EARTH = 3;
	public final static byte HOLY = 4;
	public final static byte DARK = 5;

	public final static int ENCHANT_CHANCE = Config.ENCHANT_CHANCE_ELEMENT;

	public final static int FIRST_WEAPON_BONUS = 20;
	public final static int NEXT_WEAPON_BONUS = 5;
	public final static int ARMOR_BONUS = 6;

	public final static int[] WEAPON_VALUES =
	{
		0,   // Level 1
		25,  // Level 2
		75,  // Level 3
		150, // Level 4
		175, // Level 5
		225, // Level 6
		300, // Level 7
		325, // Level 8
		375, // Level 9
		450, // Level 10
		475, // Level 11
		525, // Level 12
		600, // Level 13
		Integer.MAX_VALUE  // TODO: Higher stones
	};

	public final static int[] ARMOR_VALUES =
	{
		0,  // Level 1
		12, // Level 2
		30, // Level 3
		60, // Level 4
		72, // Level 5
		90, // Level 6
		120, // Level 7
		132, // Level 8
		150, // Level 9
		180, // Level 10
		192, // Level 11
		210, // Level 12
		240, // Level 13
		Integer.MAX_VALUE  // TODO: Higher stones
	};

	public final static int[] STONES = 
	{
		9546,
		9547,
		9549,
		9548,
		9551,
		9550
	};

	public final static int[] CRYSTALS =
	{
		9552,
		9553,
		9555,
		9554,
		9557,
		9556
	};

	public final static int[] JEWELS =
	{
		9558,
		9559,
		9561,
		9560,
		9563,
		9562
	};

	public final static int[] ENERGIES = 
	{
		9564,
		9565,
		9567,
		9566,
		9569,
		9568
	};

	private byte _element = NONE;
	private int _value = 0;

	public byte getElement()
	{
		return _element;
	}

	public void setElement(byte type)
	{
		_element = type;
		_boni.setElement(type);
	}

	public int getValue()
	{
		return _value;
	}

	public void setValue(int val)
	{
		_value = val;
		_boni.setValue(val); 
	}

	public static String getElementName(byte element)
	{
		switch(element)
		{
			case FIRE:
				return "Fire";
			case WATER:
				return "Water";
			case WIND:
				return "Wind";
			case EARTH:
				return "Earth";
			case DARK:
				return "Dark";
			case HOLY:
				return "Holy";
		}
		return "None";
	}

	public static byte getElementId(String name)
	{
		String tmp = name.toLowerCase();
		if (tmp.equals("fire"))
			return FIRE;
		if (tmp.equals("water"))
			return WATER;
		if (tmp.equals("wind"))
			return WIND;
		if (tmp.equals("earth"))
			return EARTH;
		if (tmp.equals("dark"))
			return DARK;
		if (tmp.equals("holy"))
			return HOLY;
		return NONE;
	}

	public static byte getOppositeElement(byte element)
	{
		return (byte)((element % 2 == 0) ? (element + 1) : (element - 1));
	}

	@Override
	public String toString()
	{
		return getElementName(_element) + " +" + _value;
	}

	public Elementals(byte type, int value)
	{
		_element = type;
		_value = value;
		_boni = new ElementalStatBoni(_element, _value);
	}
	
	public class ElementalStatBoni  
	{  
		private byte _elementalType;  
		private int _elementalValue;  
		private boolean _active;  
		
		public ElementalStatBoni(byte type, int value)  
		{  
			_elementalType = type;  
			_elementalValue = value;  
			_active = false;  
		}  
		
		public void applyBonus(L2PcInstance player, boolean isArmor)  
		{  
			// make sure the bonuses are not applied twice..  
			if (_active)  
				return;  
			
			switch (_elementalType)  
			{  
				case FIRE:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.FIRE_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.FIRE_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
				case WATER:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.WATER_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.WATER_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
				case WIND:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.WIND_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.WIND_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
				case EARTH:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.EARTH_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.EARTH_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
				case DARK:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.DARK_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.DARK_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
				case HOLY:  
					if (isArmor)  
						player.addStatFunc(new FuncAdd(Stats.HOLY_RES, 0x40, this, new LambdaConst(_elementalValue)));  
					else  
						player.addStatFunc(new FuncAdd(Stats.HOLY_POWER, 0x40, this, new LambdaConst(_elementalValue)));  
					break;  
			}  
			
			_active = true;  
		}  
		
		public void removeBonus(L2PcInstance player)  
		{  
			// make sure the bonuses are not removed twice  
			if (!_active)  
				return;  
			
			((L2Character) player).removeStatsOwner(this);  
			
			_active = false;  
		}  
		
		public void setValue(int val)  
		{  
			_elementalValue = val;  
		}  
		
		public void setElement(byte type)  
		{  
			_elementalType = type;  
		}  
	}  
	
	public void applyBonus(L2PcInstance player, boolean isArmor)  
	{  
		_boni.applyBonus(player, isArmor);  
	}  
	
	public void removeBonus(L2PcInstance player)  
	{  
		_boni.removeBonus(player);  
	}  
	
	public void updateBonus(L2PcInstance player, boolean isArmor)  
	{  
		_boni.removeBonus(player);  
		_boni.applyBonus(player, isArmor);  
	}  
}  