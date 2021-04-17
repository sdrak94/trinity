package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class StrenghtChecksFeature extends AbstractFeature
{
	private int maxLevelDiff = 5;
	
	public StrenghtChecksFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("MaxLevelDiff", "The maximal acceptable level difference between players to start the match. For parties/teams, it calculates the average level of all players inside it. Put '0' to disable this config.", 1);
		
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
			maxLevelDiff = Integer.parseInt(params[0]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public int getMaxLevelDiff()
	{
		return maxLevelDiff;
	}
	
	public boolean canFight(RegistrationData player, RegistrationData opponent)
	{
		if(Math.abs(player.getAverageLevel() - opponent.getAverageLevel()) > maxLevelDiff)
			return false;
		
		return true;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.StrenghtChecks;
	}
}
