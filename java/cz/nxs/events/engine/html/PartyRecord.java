/**
 * 
 */
package cz.nxs.events.engine.html;

import java.util.List;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.PartyData;
import javolution.util.FastList;

/**
 * @author hNoke
 *
 */
public class PartyRecord
{
	int partyId;
	PlayerEventInfo leader;
	String message;
	int membersWanted;
	long timeCreated;
	
	List<PlayerEventInfo> applicants;
	List<Integer> blacklist;
	
	PartyRecord(int id, PlayerEventInfo leader, String message, int membersWanted)
	{
		this.partyId = id;
		this.leader = leader;
		this.message = message;
		this.membersWanted = membersWanted;
		applicants = new FastList<PlayerEventInfo>();
		blacklist = new FastList<Integer>();
		
		timeCreated = System.currentTimeMillis();
	}
	
	public int getCurrentMembersCount()
	{
		return 0;
	}
	
	public PartyData getParty()
	{
		if(leader.getParty() != null)
		{
			return leader.getParty();
		}
		
		return null;
	}
	
	public boolean canBeRemoved(long current)
	{
		if(timeCreated + 3600000 <= current)
			return true;
		else return false;
	}
	
	public List<PlayerEventInfo> getApplicants()
	{
		return applicants;
	}
	
	public void addApplicant(PlayerEventInfo player)
	{
		if(blacklist.contains(player.getPlayersId()))
		{
			player.sendMessage("You've been blacklisted for this party.");
			return;
		}
		
		if(!applicants.contains(player))
			applicants.add(player);
		else
		{
			player.sendMessage("You are already on the list of applicants.");
		}
	}
	
	public void banApplicant(String name)
	{
		PlayerEventInfo player = null;
		for(PlayerEventInfo p : applicants)
		{
			if(p.getPlayersName().equals(name))
			{
				player = p;
				break;
			}
		}
		
		if(player == null)
		{
			leader.sendMessage("This player is no longer on the list.");
			return;
		}
		
		if(applicants.contains(player))
		{
			applicants.remove(player);
			blacklist.add(player.getPlayersId());
		}
	}
	
	public boolean isApplicant(PlayerEventInfo player)
	{
		return applicants.contains(player);
	}
	
	public boolean isBanned(PlayerEventInfo player)
	{
		return blacklist.contains(player.getPlayersId());
	}
	
	public void removeApplicant(String name, boolean addToParty)
	{
		PlayerEventInfo player = null;
		for(PlayerEventInfo p : applicants)
		{
			if(p.getPlayersName().equals(name))
			{
				player = p;
				break;
			}
		}
		
		if(player == null)
		{
			leader.sendMessage("This player is no longer on the list.");
			return;
		}
		
		if(applicants.contains(player))
		{
			applicants.remove(player);
			
			if(addToParty)
			{
				PartyData pt = getParty();
				if(pt == null)
				{
					pt = new PartyData(leader);
					pt.addPartyMember(player);
				}
				else
				{
					pt.addPartyMember(player);
				}
			}
		}
	}
}
