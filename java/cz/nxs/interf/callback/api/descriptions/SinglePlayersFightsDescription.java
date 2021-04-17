/**
 * 
 */
package cz.nxs.interf.callback.api.descriptions;

import java.util.Map;

import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.description.EventDescription;

/**
 * @author hNoke
 *
 */
public class SinglePlayersFightsDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs, int roundsCount, int teamsCount, int teamSize, int rejoinDelay, int timeLimit)
	{
		String text;
		text = "This is a free-for-all mini event. ";
		text += "You will fight against " + (teamsCount-1) + " enemy player" + (teamsCount > 2 ? "s" : "") + " in a randomly chosen map. ";
		
		if(roundsCount > 1)
		{
			text += "Each match has " + roundsCount + " rounds, the winner of round (the player, who kills all his opponents) receives  1 score. ";
			text += "The player, who has the biggest score in the end of all rounds, wins the match. ";
		}
		else
		{
			text += "This event has only one round. If you die, the event ends for you. ";
			text += "The winner of the match is the player, who kills all his opponents. ";
		}
		
		text += "Your opponents will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
		
		text += "If the match doesn't end within " + timeLimit/60000 + " minutes, it will be aborted automatically. ";
		
		text += "Also, after you visit this event, you will have to wait at least " + rejoinDelay/60000 + " minutes to join this event again. ";
		return text;
	}
}
