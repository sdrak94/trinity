package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class LevelFeature extends AbstractFeature
{
	private int minLevel = 1;
	private int maxLevel = 85;
	
	public LevelFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("MinLevel", "The min level required to participate this event mode.", 1);
		addConfig("MaxLevel", "The max level to participate this event mode.", 1);
		
		if(parametersString == null)
			parametersString = "1,85";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			minLevel = Integer.parseInt(params[0]);
			maxLevel = Integer.parseInt(params[1]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public int getMinLevel()
	{
		return minLevel;
	}
	
	public int getMaxLevel()
	{
		return maxLevel;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		if(player.getLevel() < minLevel)
			return false;
		
		if(player.getLevel() > maxLevel)
			return false;
		
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Level;
	}
}
