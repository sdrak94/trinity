package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class TeamSizeFeature extends AbstractFeature
{
	private int teamSize = 5;
	
	public TeamSizeFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("TeamSize", "The ammount of players in one team in matches started under this mode (overrides the value from general configs). The value must be > 1 otherwise this config will be ignored.", 1);
		
		if(parametersString == null)
			parametersString = "5";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			teamSize = Integer.parseInt(params[0]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public int getTeamSize()
	{
		return teamSize;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.TeamSize;
	}
}
