package cz.nxs.events.engine.mini;

import java.util.StringTokenizer;

/**
 * @author hNoke
 * - enum defining the behaviour of a door in event
 */
public enum DoorAction
{
	Open,
	Close,
	Default;
	
	public static DoorAction getAction(String note, int state)
	{
		String action = "Default";
		StringTokenizer st = new StringTokenizer(note);
		
		if(state == 1)
			action = st.nextToken();
		else if(state ==2)
		{
			st.nextToken();
			action = st.nextToken();
		}
		
		for(DoorAction d : values())
		{
			if(d.toString().equalsIgnoreCase(action))
				return d;
		}
		return Default;
	}
}
