/**
 * 
 */
package cz.nxs.l2j.handler;

import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public interface NexusAdminCommand
{
	public abstract boolean useAdminCommand(String command, PlayerEventInfo pi);
}
