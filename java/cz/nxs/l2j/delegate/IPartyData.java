/**
 * 
 */
package cz.nxs.l2j.delegate;

import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public interface IPartyData
{
	public void addPartyMember(PlayerEventInfo player);
	public void removePartyMember(PlayerEventInfo player);
	
	public PlayerEventInfo getLeader();
	public int getLeadersId();
	
	public PlayerEventInfo[] getPartyMembers();
	public int getMemberCount();
}
