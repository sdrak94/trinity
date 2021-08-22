package net.sf.l2j.gameserver.model.actor.instance;

import java.awt.Color;

import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;

public class L2ZoneInstance extends L2NpcInstance
{
	private final int _zoneRadius;
	
	private ExServerPrimitive _circle;
	
	public L2ZoneInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		
		_zoneRadius = template.getZoneRadius();
	}
	
	public void mkPrimitive(final int color, final int radius)
	{
		_circle = new ExServerPrimitive(String.valueOf(getObjectId()), getLocation());
		_circle.addCircle(color, radius, 27, 1);
		_circle.addCircle(Color.WHITE, radius, 27, 2);
		_circle.addCircle(color, radius, 27, 3);
		
		broadcastPacket(_circle);
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		super.sendInfo(activeChar);
		
		if (_circle != null)
			activeChar.sendPacket(_circle);
	}
	
	public int getZoneRadius()
	{
		return _zoneRadius;
	}
	
	public boolean isInZoneRadius(final ILocational loc)
	{
		final var dist = Util.calculateDistance(this, loc, true); 
		
		return dist < _zoneRadius;
	}
}
