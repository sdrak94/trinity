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
public class TvTDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs)
	{
		String text;
		text = getInt(configs, "teamsCount") + " teams fighting against each other. ";
		text += "Gain score by killing your opponents";
		
		if(getInt(configs, "killsForReward") > 0)
		{
			text += " (at least " + getInt(configs, "killsForReward") + " kill(s) is required to receive a reward)";
		}
		
		if(getBoolean(configs, "waweRespawn"))
			text += " and dead players are resurrected by an advanced wawe-spawn engine each " + getInt(configs, "resDelay") + " seconds";
		else
			text += " and if you die, you will be resurrected in " + getInt(configs, "resDelay") + " seconds";
		
		
		if(getBoolean(configs, "createParties"))
			text += ". The event automatically creates parties on start";
		
		text += ".";
		return text;
	}
}
