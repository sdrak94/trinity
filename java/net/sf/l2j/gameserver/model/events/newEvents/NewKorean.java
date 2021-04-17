package net.sf.l2j.gameserver.model.events.newEvents;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.dataTables.KoreanTeamTemplate;

public class NewKorean
{
	private FastList<L2Party>	_partiesTemp	= new FastList<L2Party>();
	private FastList<L2Party>	_partiesSave	= new FastList<L2Party>();
	private FastList<L2Party>	_parties		= new FastList<L2Party>();
	private FastList<KoreanTeamTemplate>	_partiesIG		= new FastList<KoreanTeamTemplate>();
	int							partySize		= 10;
	private FastList<Integer>	_bannedClasses	= new FastList<Integer>();
	public EventState			_state;
	int							totalParties	= 0;
	
	private enum EventState
	{
		START,
		FIGHT,
		END,
		TELEPORT,
		INACTIVE
	}
	
	public void event()
	{
		if(startEvent())
		{
			
		}
	}
	public void regParty(L2PcInstance p)
	{
		if (p == null)
		{
			return;
		}
		if (p.getParty() == null)
		{
			return;
		}
		if (p.getParty().getPartyLeaderOID() != p.getObjectId())
		{
			p.sendMessage("Only the party Leader may register.");
			return;
		}
		if (p.getParty().getPartyMembers().size() != partySize)
		{
			p.sendMessage("You may not register, your party must have " + partySize + " members.");
			return;
		}
		if (!checkPartyForBannedClasses(p))
		{
			return;
		}
	}
	
	private boolean startEvent()
	{
		if (!checkPartiesBeforeStart())
		{
			return false;
		}
		if (!teleportPartiesOnStart())
		{
			return false;
		}
		
		return true;
	}
	
	
	
	
	
	// -------------------------- UTIL COMMANDS
	
	private boolean teleportPartiesOnStart()
	{
		_state = EventState.TELEPORT;
		int count = 1;
		//96 X - dif, 84 X - dif
		int xDiff = 96*count;
		int yDiff = 84*count;
		
		
		Location loc = new Location(114087+xDiff, 17525+yDiff, 10074, 57343);
		Location loc2 = new Location(115935+xDiff, 15493+yDiff, 10074, 20352);
	
		for(int i = 0; i <= _parties.size(); i++)
		{
			if(count == _parties.size() / 2)
			{
				loc = loc2;
				count = 0;
			}
			L2Party party = _parties.get(i);
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				if(ptm.isTransformed())
					ptm.untransform();
				ptm.teleToLocation(loc, false);
				ptm.setParalyzedEffect();
			}
			count ++;
			_partiesIG.add(new KoreanTeamTemplate(party.getLeader(), party));
		}
		if (_partiesIG.size() > 5)
			return true;
		else
			return false;
	}
	
	private boolean checkPartiesBeforeStart()
	{
		_partiesTemp.forEach(party ->
		{
			if (party.getMemberCount() != partySize)
			{
				_partiesTemp.remove(party);
				return;
			}
			_partiesSave.add(party);
			_parties.add(party);
			totalParties++;
		});
		if (totalParties > 5)
			return true;
		else
			return false;
	}
	
	private boolean checkPartyForBannedClasses(L2PcInstance p)
	{
		if (p == null)
		{
			return false;
		}
		if (p.getParty() == null)
		{
			p.sendMessage("You have no party");
			return false;
		}
		for (L2PcInstance player : p.getParty().getPartyMembers())
		{
			if (player.getClassId().getId() < 88)
			{
				p.sendMessage(player.getName() + "'s Class is not allowed in current Event, he must be in third class.");
				player.sendMessage("Your Class is not allowed in current Event, he must be in third class.");
				return false;
			}
			if (_bannedClasses.contains(player.getActiveClass()))
			{
				p.sendMessage(player.getName() + "'s Class is not allowed in current Event.");
				player.sendMessage("Your Class is not allowed in current Event.");
				return false;
			}
		}
		return true;
	}
}