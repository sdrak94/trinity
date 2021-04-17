package cz.nxs.events.engine.main.events;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.main.MainEventManager;

/**
 * @author hNoke
 *
 */
public class TreasureHuntPvp extends TreasureHunt
{
	public TreasureHuntPvp(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill });
		
		_allowPvp = true;
	}
}
