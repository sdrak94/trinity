package net.sf.l2j.gameserver.model.events.dataTables;

import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class KoreanTeamTemplate
{
	L2PcInstance _partyLeader;
	L2Party 	_party;

	public KoreanTeamTemplate (L2PcInstance partyLeader, L2Party party)
	{
		_partyLeader = partyLeader;
		_party = party;
	}
	
	public L2PcInstance getLeader()
	{
		return _partyLeader;
	}
	
	public L2Party getParty()
	{
		return _party;
	}
}
