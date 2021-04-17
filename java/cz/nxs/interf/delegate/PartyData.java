package cz.nxs.interf.delegate;

import java.util.List;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.delegate.IPartyData;
import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author hNoke
 *
 */
public class PartyData implements IPartyData
{
	private L2Party _party;
	
	public PartyData(L2Party p)
	{
		_party = p;
	}
	
	public PartyData(PlayerEventInfo leader)
	{
		leader.getOwner().setParty(new L2Party(leader.getOwner(), 1));
		_party = leader.getOwner().getParty();
	}
	
	public L2Party getParty()
	{
		return _party;
	}
	
	public boolean exists()
	{
		return _party != null;
	}
	
	@Override
	public void addPartyMember(PlayerEventInfo player)
	{
		player.getOwner().joinParty(_party);
	}
	
	@Override
	public void removePartyMember(PlayerEventInfo player)
	{
		_party.removePartyMember(player.getOwner());
	}
	
	@Override
	public PlayerEventInfo getLeader()
	{
		return _party.getLeader().getEventInfo();
	}
	
	@Override
	public PlayerEventInfo[] getPartyMembers()
	{
		List<PlayerEventInfo> players = new FastList<PlayerEventInfo>();
		
		for(L2PcInstance player : _party.getPartyMembers())
		{
			players.add(player.getEventInfo());
		}
		
		return players.toArray(new PlayerEventInfo[players.size()]);
	}
	
	@Override
	public int getMemberCount()
	{
		return _party.getMemberCount();
	}
	
	@Override
	public int getLeadersId()
	{
		return _party.getPartyLeaderOID();
	}
}
