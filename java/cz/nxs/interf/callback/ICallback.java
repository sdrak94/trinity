/**
 * 
 */
package cz.nxs.interf.callback;

import java.util.Collection;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public interface ICallback
{
	public void eventStarts(int instance, EventType event, Collection<? extends EventTeam> teams);
	public void playerKills(EventType event, PlayerEventInfo player, PlayerEventInfo target);
	public void playerScores(EventType event, PlayerEventInfo player, int count);
	public void playerFlagScores(EventType event, PlayerEventInfo player);
	public void playerKillsVip(EventType event, PlayerEventInfo player, PlayerEventInfo vip);
	public void eventEnded(int instance, EventType event, Collection<? extends EventTeam> teams);
}
