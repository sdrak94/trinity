package net.sf.l2j.gameserver.network.serverpackets;

public class ExEventMatchMessage extends L2GameServerPacket
{

	private static final String _S__0x0F_EVENT_MATCH_MESSAGE = "0x0F 0f CharCreateOk";
	private int _type;
	private String _message;
	
	/**
	 * Create an event match message.
	 * @param type 0 - gm, 1 - finish, 2 - start, 3 - game over, 4 - 1, 5 - 2, 6 - 3, 7 - 4, 8 - 5
	 * @param message to show, only when type is 0 - gm
	 */
	public ExEventMatchMessage(int type, String message)
	{
		_type = type;
		_message = message;
	}
	
	@Override
	protected void writeImpl()
	{
	    writeC(0xFE);
	    writeH(0x0F);
	    writeC(_type); // 0 - custom text, 1 - Start, 2 - Finish, 3 - Game Over, 4 - 1, 5 - 2, 6 - 3, 7 - 4, 8 - 5
	    writeS(_message);
	}
	@Override
	public String getType()
	{
		return _S__0x0F_EVENT_MATCH_MESSAGE;
	}
}