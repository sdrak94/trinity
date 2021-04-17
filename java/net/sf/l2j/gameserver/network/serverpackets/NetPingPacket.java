package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author vGodFather
 */
public class NetPingPacket extends L2GameServerPacket
{
	private final int _objId;
	
	public NetPingPacket(L2PcInstance cha)
	{
		_objId = cha.getObjectId();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xD9);
		writeD(_objId);
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}

}