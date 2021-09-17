package net.sf.l2j.gameserver.network.serverpackets;

public class AlphaOpenUrl extends L2GameServerPacket
{
	private String _url;
	
	public AlphaOpenUrl(String url)
	{
		_url = url;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xff);
		writeC(0x03);
		
		writeS(_url);
	}
	
	@Override
	public String getType()
	{
		return null;
	}
}
