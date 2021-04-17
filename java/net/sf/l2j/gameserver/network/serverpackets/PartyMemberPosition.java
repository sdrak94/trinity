package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class PartyMemberPosition extends L2GameServerPacket
{
	private final L2PcInstance _actor;
	
	public PartyMemberPosition(L2PcInstance actor)
	{
		_actor = actor;
	}
	
	@Override
	protected void writeImpl()
	{
		final L2Party party = _actor.getParty();
		
		if (party == null)
			return;
		
		final L2PcInstance actChar = getClient().getActiveChar();
		
		if (actChar == null)
			return;
		
		writeC(0xba);
		writeD(Math.min(9, party.getMemberCount()));
		
		int count = 0;
		
		for (L2PcInstance pm : party.getPartyMembers())
		{
			if (pm == null)
				continue;
			if (pm == actChar)
				continue;
			
			writeD(pm.getObjectId());
			writeD(pm.getX());
			writeD(pm.getY());
			writeD(pm.getZ());
			
			count++;
			
			if (count >= 8)
				return;
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] ba PartyMemberPosition";
	}
}