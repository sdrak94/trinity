package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class RoundsFeature extends AbstractFeature
{
	private int roundsAmmount = 1;
	
	public RoundsFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("RoundsAmmount", "The ammount of rounds for matches started under this mode (overrides the value from general configs). The value must be > 0 otherwise this config will be ignored.", 1);
		
		if(parametersString == null)
			parametersString = "1";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			roundsAmmount = Integer.parseInt(params[0]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public int getRoundsAmmount()
	{
		return roundsAmmount;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Rounds;
	}
}
