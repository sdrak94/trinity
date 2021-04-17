/**
 * 
 */
package cz.nxs.events.engine.main.globalevent;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import net.sf.l2j.gameserver.model.actor.L2Npc;

/**
 * @author hNoke
 *
 */
public abstract class GlobalEvent
{
	public abstract String getName();
	public abstract void start();
	public abstract void end();
	
	public abstract boolean canRegister(PlayerEventInfo player);
	public abstract void addPlayer(PlayerEventInfo player);
	
	public abstract void monsterDies(L2Npc npc);
	
	public abstract String getStateNameForHtml();
	
	public void announce(String message)
	{
		announce(message, false);
	}
	
	public void announce(String message, boolean special)
	{
		for(PlayerEventInfo player : CallBack.getInstance().getOut().getAllPlayers())
		{
			player.screenMessage(message, getName(), special);
		}
	}
}
