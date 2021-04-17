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
public class KoreanDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs, int roundsCount, int teamsCount, int teamSize, int rejoinDelay, int timeLimit)
	{
		String text;
		
		text = "This is a team-based mini event. You need a party of exactly " + teamSize + " players (and be the party leader) to register. ";
		text += "You will fight against one enemy party in a randomly chosen map. ";
		
		text += "The fight is in the famous Korean-style - it's a set of continous 1v1 fights. If you die, you will be replaced by someone from your party. ";
		text += "The match ends when all players from one party are dead. ";
		
		text += "Your opponent will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
		
		text += "If the match doesn't end within " + timeLimit/60000 + " minutes, it will be aborted automatically. ";
		
		text += "Also, after you visit this event, you will have to wait at least " + rejoinDelay/60000 + " minutes to join this event again. ";
		
		return text;
	}
}
