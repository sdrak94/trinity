package net.sf.l2j.gameserver.network.serverpackets;

public class AlphaOverlay extends L2GameServerPacket
{

	private final String _message;
	private final boolean _open;
	
	public enum ScreePosition
	{
		
	}
	
	public AlphaOverlay(String message, boolean open)
	{
		_message = message;
		_open = open;
	}
	
	
	@Override
	protected void writeImpl()
	{
		writeC(0xff);
		writeH(0x05);
		
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
