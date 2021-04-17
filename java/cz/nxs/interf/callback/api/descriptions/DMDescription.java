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
public class DMDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs)
	{
		String text;
		text = "This is a free-for-all event, don't expect any help from teammates. Gain score by killing your opponents";
		
		text += " and if you die, you will be resurrected within " + getInt(configs, "resDelay") + " seconds. ";
		
		if(getBoolean(configs, "waweRespawn"))
			text += "Also, wawe-spawn system ensures that all dead players are spawned in the same moment (but in different spots). ";
		
		if(getBoolean(configs, "antifeedProtection"))
			text += "This event has a protection, which completely changes the appearance of all players and temporary removes their title and clan/ally crests. ";
		
		if(getInt(configs, "killsForReward") > 0)
			text += "In the end, you need at least " + getInt(configs, "killsForReward") + " kills to receive a reward.";
		return text;
	}
}
