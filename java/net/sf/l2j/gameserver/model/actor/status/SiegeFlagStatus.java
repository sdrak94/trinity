package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;

public class SiegeFlagStatus extends NpcStatus
{
public SiegeFlagStatus(L2SiegeFlagInstance activeChar)
{
	super(activeChar);
}

@Override
public void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

@Override
public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHpConsumption, boolean bypassCP)
{
	if (getActiveChar().isAdvancedHeadquarter())
		value /= 2.;
	
	super.reduceHp(value, attacker, awake, isDOT, isHpConsumption, false);
}

@Override
public L2SiegeFlagInstance getActiveChar()
{
	return (L2SiegeFlagInstance)super.getActiveChar();
}
}