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
public class CTFDescription extends EventDescription
{
	@Override
	public String getDescription(Map<String, ConfigModel> configs)
	{
		String text;
		text = "There are " + getInt(configs, "teamsCount") + " teams; in order to score you need to steal enemy team's flag and bring it back your team's base (to the flag holder). ";
		
		if(getInt(configs, "flagReturnTime") > -1)
			text += "If you hold the flag and don't manage to score within " + getInt(configs, "flagReturnTime")/1000 + " seconds, the flag will be returned back to enemy's flag holder. ";
				
		if(getBoolean(configs, "waweRespawn"))
			text += "Dead players are resurrected by an advanced wawe-spawn engine each " + getInt(configs, "resDelay") + " seconds.";
		else
			text += "If you die, you will be resurrected in " + getInt(configs, "resDelay") + " seconds. ";
		
		if(getBoolean(configs, "createParties"))
			text += "The event automatically creates parties on start.";
		
		return text;
	}
}
