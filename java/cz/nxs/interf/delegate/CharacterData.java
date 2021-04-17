package cz.nxs.interf.delegate;

import cz.nxs.events.engine.base.Loc;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.delegate.ICharacterData;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author hNoke
 *
 */
public class CharacterData extends ObjectData implements ICharacterData
{
	protected L2Character _owner;
	
	public CharacterData(L2Character cha)
	{
		super(cha);
		_owner = cha;
	}
	
	@Override
	public L2Character getOwner()
	{
		return _owner;
	}
	
	@Override
	public double getPlanDistanceSq(int targetX, int targetY)
	{
		return _owner.getPlanDistanceSq(targetX, targetY);
	}
	
	@Override
	public Loc getLoc()
	{
		return new Loc(_owner.getX(), _owner.getY(), _owner.getZ(), _owner.getHeading());
	}
	
	@Override
	public int getObjectId()
	{
		return _owner.getObjectId();
	}
	
	@Override
	public boolean isDoor()
	{
		return _owner instanceof L2DoorInstance;
	}
	
	@Override
	public DoorData getDoorData()
	{
		return isDoor() ? new DoorData((L2DoorInstance) _owner) : null;
	}
	
	@Override
	public void startAbnormalEffect(int mask)
	{
		_owner.startAbnormalEffect(mask);
	}
	
	@Override
	public void stopAbnormalEffect(int mask)
	{
		_owner.stopAbnormalEffect(mask);
	}
	
	/** returns null if _owner is NOT L2Playable */
	@Override
	public PlayerEventInfo getEventInfo()
	{
		if(_owner instanceof L2Playable)
			return ((L2Playable)_owner).getActingPlayer().getEventInfo();
		return null;
	}
	
	@Override
	public String getName()
	{
		return _owner.getName();
	}
	
	@Override
	public void creatureSay(int channel, String charName, String text)
	{
		_owner.broadcastPacket(new CreatureSay(_owner.getObjectId(), channel, charName, text));
	}
	
	@Override
	public void doDie(CharacterData killer)
	{
		_owner.reduceCurrentHp(_owner.getCurrentHp()*2, killer.getOwner(), null);
	}
	
	@Override
	public boolean isDead()
	{
		return _owner.isDead();
	}
}
