package cz.nxs.interf.delegate;

import cz.nxs.l2j.delegate.IObjectData;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceNexusInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author hNoke
 *
 */
public class ObjectData implements IObjectData
{
	protected L2Object _owner;
	
	public ObjectData(L2Object cha)
	{
		_owner = cha;
	}
	
	public L2Object getOwner()
	{
		return _owner;
	}
	
	@Override
	public int getObjectId()
	{
		return _owner.getObjectId();
	}
	
	@Override
	public boolean isPlayer()
	{
		return _owner instanceof L2PcInstance;
	}
	
	@Override
	public boolean isSummon()
	{
		return _owner instanceof L2Summon;
	}
	
	@Override
	public boolean isFence()
	{
		return _owner instanceof L2FenceInstance;
	}
	
	@Override
	public FenceData getFence()
	{
		if(!isFence()) return null;
		return new FenceData((L2FenceInstance)_owner);
	}
	
	@Override
	public NpcData getNpc()
	{
		return new NpcData((L2Npc)_owner);
	}
	
	@Override
	public boolean isNpc()
	{
		return _owner instanceof L2Npc;
	}
}
