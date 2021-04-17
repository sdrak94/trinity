/**
 * 
 */
package cz.nxs.events.engine.stats;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.ShowBoardData;


/**
 * @author hNoke
 *
 */
public abstract class EventStats
{
	public EventStats()
	{
		
	}
	
	public void showHtmlText(PlayerEventInfo player, String text)
	{
		if (text.length() < 4090)
		{
			ShowBoardData sb = new ShowBoardData(text, "101");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(null, "102");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(null, "103");
			sb.sendToPlayer(player);
		}
		else if (text.length() < 8180)
		{
			ShowBoardData sb = new ShowBoardData(text.substring(0, 4090), "101");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(text.substring(4090, text.length()), "102");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(null, "103");
			sb.sendToPlayer(player);
		}
		else if (text.length() < 12270)
		{
			ShowBoardData sb = new ShowBoardData(text.substring(0, 4090), "101");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(text.substring(4090, 8180), "102");
			sb.sendToPlayer(player);
			
			sb = new ShowBoardData(text.substring(8180, text.length()), "103");
			sb.sendToPlayer(player);
		}
	}
	
	public abstract void load();
	public abstract void onLogin(PlayerEventInfo player);
	public abstract void onDisconnect(PlayerEventInfo player);
	public abstract void onCommand(PlayerEventInfo player, String command);
	public abstract void statsChanged(PlayerEventInfo player);
}
