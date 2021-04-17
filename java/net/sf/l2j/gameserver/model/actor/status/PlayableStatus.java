package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class PlayableStatus extends CharStatus
{
public PlayableStatus(L2Playable activeChar)
{
	super(activeChar);
}

@Override
public void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true, false, false, false); }
@Override
public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP)
{
	final L2Playable you = getActiveChar();
	if (you == null) return;
	if (you.isDead()) return;
	
	super.reduceHp(value, attacker, awake, isDOT, isHPConsumption, false);
	
	if (you != null && you.isDead())
	{
		if (attacker.getTarget() != null && attacker.getTarget() == you)
		{
			if (attacker instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)attacker;
				
				if (player.isGM())
				{
					return;
				}
				else
				{
					player.setIsSelectingTarget(3);
					player.setTarget(null);
				}
			}
		}
	}
}

@Override
public L2Playable getActiveChar()
{
	return (L2Playable)super.getActiveChar();
}
}