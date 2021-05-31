package net.sf.l2j.gameserver.model.base;

 /**
 	@author xdem
 */
public enum ClassType
{
	DD,
	MELEE(DD),
	MAGE(DD),
	ARCHER(DD),
	TANK(DD,MELEE),
	OFFTANK(DD, TANK, MELEE),
	DAGGER(DD, MELEE),
	SUMMONER(DD, MAGE),
	SUPPORT(MAGE),
	MAGE_SUPPORT(DD, MAGE, SUPPORT),
	MELEE_SUPPORT(DD, SUPPORT, MELEE),
	HEALER(SUPPORT, MAGE);
	
	private final boolean[] extendsMap = new boolean[ordinal()];
	
	private ClassType(ClassType ... classTypes)
	{
		for (ClassType classType : classTypes)
			extendsMap[classType.ordinal()] = true;
	}
	
	public boolean isOfType(ClassType type)
	{
		if (this == type)
			return true;
		final int typeOrdinal = type.ordinal();
		if (ordinal() < typeOrdinal)
			return false;
		return extendsMap[typeOrdinal];
	}
}
