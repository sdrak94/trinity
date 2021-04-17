package cz.nxs.playervalue;

import java.util.List;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.playervalue.criteria.GearScore;
import cz.nxs.playervalue.criteria.ICriteria;
import cz.nxs.playervalue.criteria.PlayerClass;
import cz.nxs.playervalue.criteria.PlayerLevel;
import cz.nxs.playervalue.criteria.PlayerSkills;
import javolution.util.FastList;

/**
 * @author hNoke
 *
 */
public class PlayerValueEngine
{
	private List<ICriteria> criterias = new FastList<ICriteria>();
	
	public PlayerValueEngine()
	{
		load();
		NexusLoader.debug("Nexus Engine: Loaded PlayerValue engine.");
	}
	
	private void load()
	{
		criterias.add(GearScore.getInstance());
		criterias.add(PlayerClass.getInstance());
		criterias.add(PlayerLevel.getInstance());
		criterias.add(PlayerSkills.getInstance());
	}
	
	public void addCriteria(ICriteria c)
	{
		criterias.add(c);
	}
	
	public int getPlayerValue(PlayerEventInfo player)
	{
		if(!EventConfig.getInstance().getGlobalConfigBoolean("GearScore", "enableGearScore"))
			return 0;
		
		int value = 0;
		for(ICriteria i : criterias)
		{
			value += i.getPoints(player);
		}
		return value;
	}
	
	public static PlayerValueEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PlayerValueEngine _instance = new PlayerValueEngine();
	}
}
