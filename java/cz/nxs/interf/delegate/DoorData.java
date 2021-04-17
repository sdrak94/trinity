package cz.nxs.interf.delegate;

import cz.nxs.l2j.delegate.IDoorData;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;

/**
 * @author hNoke
 *
 */
public class DoorData extends CharacterData implements IDoorData
{
	protected L2DoorInstance _owner;
	
	public DoorData(L2DoorInstance d)
	{
		super(d);
		_owner = d;
	}
	
	@Override
	public L2DoorInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public int getDoorId()
	{
		return _owner.getDoorId();
	}
	
	@Override
	public boolean isOpened()
	{
		return _owner.getOpen();
	}
	
	@Override
	public void openMe()
	{
		_owner.openMe();
	}
	
	@Override
	public void closeMe()
	{
		_owner.closeMe();
	}
}
