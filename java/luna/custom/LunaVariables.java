package luna.custom;

public class LunaVariables
{
	protected static final LunaVariables	_instance							= new LunaVariables();
	boolean									_FarmEventZoneStatus				= false;
	boolean									_koreanCubicSkillsPrevented			= false;
	boolean									_koreanHeroSkillsPrevented			= false;
	boolean									_koreanRessurectionSkillsPrevented	= false;
	boolean									_koreanMarriageSkillsPrevented		= false;
	boolean									_announceEnchantMessagesToPlayers	= true;
	boolean									_enableCustomPvPZone				= false;
	int										_customPvPZoneId					= 0;
	
	/**
	 * @return the _customPvPZoneId
	 */
	public int get_customPvPZoneId()
	{
		return _customPvPZoneId;
	}

	/**
	 * @param _customPvPZoneId the _customPvPZoneId to set
	 */
	public void set_customPvPZoneId(int customPvPZoneId)
	{
		_customPvPZoneId = customPvPZoneId;
	}

	/**
	 * @return the _enableCustomPvPZone
	 */
	public boolean getCustomPvPZoneStatus()
	{
		return _enableCustomPvPZone;
	}

	/**
	 * @param _enableCustomPvPZone the _enableCustomPvPZone to set
	 */
	public void set_enableCustomPvPZone(boolean enableCustomPvPZone)
	{
		_enableCustomPvPZone = enableCustomPvPZone;
	}

	public void setFarmEventZoneStatus(boolean FarmEventZoneStatus)
	{
		_FarmEventZoneStatus = FarmEventZoneStatus;
	}
	
	public boolean getFarmEventStatus()
	{
		return _FarmEventZoneStatus;
	}
	
	
	public boolean getKoreanCubicSkillsPrevented()
	{
		return _koreanCubicSkillsPrevented;
	}
	
	public void setKoreanCubicSkillsPrevented(boolean KoreanCubicSkillsPrevented)
	{
		_koreanCubicSkillsPrevented = KoreanCubicSkillsPrevented;
	}
	
	public void setKoreanHeroSkillsPrevented(boolean KoreanHeroSkillsPrevented)
	{
		_koreanHeroSkillsPrevented = KoreanHeroSkillsPrevented;
	}
	
	public boolean getKoreanHeroSkillsPrevented()
	{
		return _koreanHeroSkillsPrevented;
	}
	
	public void setKoreanRessurectionSkillsPrevented(boolean KoreanRessurectionSkillsPrevented)
	{
		_koreanRessurectionSkillsPrevented = KoreanRessurectionSkillsPrevented;
	}
	
	public boolean getKoreanRessurectionSkillsPrevented()
	{
		return _koreanRessurectionSkillsPrevented;
	}
	
	public void setKoreanMarriageSkillsPrevented(boolean KoreanMarriageSkillsPrevented)
	{
		_koreanMarriageSkillsPrevented = KoreanMarriageSkillsPrevented;
	}
	
	public boolean getKoreanMarriageSkillsPrevented()
	{
		return _koreanMarriageSkillsPrevented;
	}
	
	public void setAnnounceEnchantMessagesToPlayers(boolean AnnounceEnchantMessagesToPlayers)
	{
		_announceEnchantMessagesToPlayers = AnnounceEnchantMessagesToPlayers;
	}
	
	public boolean getAnnounceEnchantMessagesToPlayers()
	{
		return _announceEnchantMessagesToPlayers;
	}
	public static LunaVariables getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final LunaVariables _instance = new LunaVariables();
		
		private SingletonHolder()
		{}
	}
}
