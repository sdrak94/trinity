package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

public class L2PersonalZone extends L2ZoneType
{

public L2PersonalZone(final int id)
{
	super(id);
}

@Override
protected void onEnter(final L2Character character)
{
	
}

@Override
protected void onExit(final L2Character character)
{
	
}

@Override
public void onDieInside(final L2Character character)
{
}

@Override
public void onReviveInside(final L2Character character)
{
}
}