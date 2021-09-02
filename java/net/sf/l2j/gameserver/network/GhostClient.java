package net.sf.l2j.gameserver.network;

import org.mmocore.network.MMOConnection;

import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public class GhostClient extends L2GameClient
{

	public static L2GamePacketHandler				packetHandler;
	
	public GhostClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
		
		
	}
	
	public static void setGamePacketHandler(L2GamePacketHandler handler)
	{
		packetHandler = handler;
	}
	
	@Override
	public void sendPacket(L2GameServerPacket gsp)
	{
		System.currentTimeMillis();
	}

	@Override
	public boolean isDetached()
	{
		return false;
	}
	
	public void closeNow()
	{
	}
	
	@Override
	public void close(L2GameServerPacket gsp)
	{}
}
