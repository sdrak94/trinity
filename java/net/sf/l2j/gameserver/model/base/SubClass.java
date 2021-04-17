package net.sf.l2j.gameserver.model.base;

import net.sf.l2j.Config;

/**
 * Character Sub-Class Definition
 * <BR>
 * Used to store key information about a character's sub-class.
 *
 * @author Tempy
 */
public final class SubClass
{
	private static final byte _maxLevel = Config.MAX_SUBCLASS_LEVEL < Experience.MAX_LEVEL ? Config.MAX_SUBCLASS_LEVEL : Experience.MAX_LEVEL - 1;

	private PlayerClass _class;
	private long _exp = Experience.LEVEL[85];
	private int _sp = 0;
	private byte _level = 85;
	private int _classIndex = 1;
	
	public SubClass(int classId, long exp, int sp, byte level, int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_exp = exp;
		_sp = sp;
		_level = level;
		_classIndex = classIndex;
	}
	
	public SubClass(int classId, int classIndex)
	{
		// Used for defining a sub class using default values for XP, SP and player level.
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
	}
	
	public SubClass()
	{
		// Used for specifying ALL attributes of a sub class directly,
		// using the preset default values.
	}
	
	public PlayerClass getClassDefinition()
	{
		return _class;
	}
	
	public int getClassId()
	{
		return _class.ordinal();
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	public void setClassId(int classId)
	{
		_class = PlayerClass.values()[classId];
	}
	
	public void setExp(long expValue)
	{
		if (expValue > (Experience.LEVEL[_maxLevel + 1] - 1))
			expValue = (Experience.LEVEL[_maxLevel + 1] - 1);
		
		_exp = expValue;
	}
	
	public void setSp(int spValue)
	{
		_sp = spValue;
	}
	
	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	public void setLevel(byte levelValue)
	{
		if (levelValue > _maxLevel)
			levelValue = _maxLevel;
		else if (levelValue < 40)
			levelValue = 40;
		
		_level = levelValue;
	}
	
	public void incLevel()
	{
		if (getLevel() == _maxLevel)
			return;
		
		_level++;
		setExp(Experience.LEVEL[getLevel()]);
	}
	
	public void decLevel()
	{
		if (getLevel() == 40)
			return;
		
		_level--;
		setExp(Experience.LEVEL[getLevel()]);
	}
}