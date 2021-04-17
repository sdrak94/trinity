package cz.nxs.l2j.delegate;

import cz.nxs.events.engine.base.Loc;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.DoorData;

/**
 * @author hNoke
 *
 */
public interface ICharacterData
{
	public String getName();
	public int getObjectId();
	
	public Loc getLoc();
	
	public double getPlanDistanceSq(int targetX, int targetY);
	
	public boolean isDoor();
	public DoorData getDoorData();
	
	public void startAbnormalEffect(int mask);
	public void stopAbnormalEffect(int mask);
	
	public void creatureSay(int channel, String charName, String text);
	public void doDie(CharacterData killer);
	public boolean isDead();
	
	/** returns null if _owner is NOT L2Playable */
	public PlayerEventInfo getEventInfo();
}
