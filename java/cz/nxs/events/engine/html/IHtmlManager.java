package cz.nxs.events.engine.html;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.NpcData;

/**
 * @author hNoke
 * - this interface will be used in a future api
 */
public interface IHtmlManager
{
	public boolean showNpcHtml(PlayerEventInfo player, NpcData npc);
	public boolean onBypass(PlayerEventInfo player, String bypass);
	boolean onCbBypass(PlayerEventInfo player, String bypass);
}
