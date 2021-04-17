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
public class PartyFightsDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs, int roundsCount, int teamsCount, int teamSize, int rejoinDelay, int timeLimit)
	{
		String text;
		text = "This is a team-based mini event. You need a party of exactly " + teamSize + " players (and be the party leader) to register. ";
		text += "You will fight against " + (teamsCount-1) + " enemy part" + (teamsCount > 2 ? "ies" : "y") + " in a randomly chosen map. ";
		
		if(roundsCount > 1)
		{
			text += "Each match has " + roundsCount + " rounds, the winner of round (the party, who kills all it's opponents) receives  1 score. ";
			text += "The party, who has the biggest score in the end of all rounds, wins the match. ";
		}
		else
		{
			text += "This match has only one round. If you die, you can get revived only by your party-mate. ";
			text += "The winner of the match is the party, who kills all it's opponents. ";
		}
		
		text += "Your opponent(s) will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
		
		text += "If the match doesn't end within " + timeLimit/60000 + " minutes, it will be aborted automatically. ";
		
		text += "Also, after you visit this event, you will have to wait at least " + rejoinDelay/60000 + " minutes to join this event again. ";
		return text;
	}
}
