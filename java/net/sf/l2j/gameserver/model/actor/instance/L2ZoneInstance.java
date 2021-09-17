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

}
