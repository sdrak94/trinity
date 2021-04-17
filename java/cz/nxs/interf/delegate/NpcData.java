package cz.nxs.interf.delegate;

import java.util.Collection;

import cz.nxs.l2j.delegate.ICharacterData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author hNoke
 *
 */
public class NpcData extends CharacterData implements ICharacterData
{
	private int _team;
	private boolean deleted = false;
	
	public NpcData(L2Npc npc)
	{
		super(npc);
	}
	
	public void deleteMe()
	{
		if(!deleted)
			((L2Npc)_owner).deleteMe();
		
		deleted = true;
	}
	
	public ObjectData getObjectData()
	{
		return new ObjectData(_owner);
	}
	
	public void setName(String name)
	{
		_owner.setName(name);
	}
	
	public void setTitle(String t)
	{
		_owner.setTitle(t);
	}
	
	public int getNpcId()
	{
		return ((L2Npc) _owner).getNpcId();
	}
	
	public void setEventTeam(int team)
	{
		_team = team;
	}
	
	public int getEventTeam()
	{
		return _team;
	}
	
	public void broadcastNpcInfo()
	{
		Collection<L2PcInstance> plrs = _owner.getKnownList().getKnownPlayers().values();
		{
			for (L2PcInstance player : plrs)
			{
				((L2Npc)_owner).sendInfo(player);
			}
		}
	}
	
	public void broadcastSkillUse(CharacterData owner, CharacterData target, int skillId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		
		if (skill != null)
			getOwner().broadcastPacket(new MagicSkillUse(owner.getOwner(), target.getOwner(), skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
	}

}
