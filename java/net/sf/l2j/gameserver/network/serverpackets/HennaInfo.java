package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Henna;


public final class HennaInfo extends L2GameServerPacket
{
	private static final String _S__E4_HennaInfo = "[S] e5 HennaInfo";

	private final L2PcInstance _activeChar;
	private final L2Henna[] _hennas = new L2Henna[3];
	private int _count;

	public HennaInfo(L2PcInstance player)
	{
		_activeChar = player;

		for (int i = 0; i < 3; i++)
		{
			L2Henna h = _activeChar.getHenna(i+1);
			if (h != null)
			{
				_hennas[_count++] = h;
			}
		}
	}


	@Override
	protected final void writeImpl()
	{

		writeC(0xe5);

		writeC(_activeChar.getHennaStatINT());	//equip INT
		writeC(_activeChar.getHennaStatSTR());	//equip STR
		writeC(_activeChar.getHennaStatCON());	//equip CON
		writeC(_activeChar.getHennaStatMEN());	//equip MEM
		writeC(_activeChar.getHennaStatDEX());	//equip DEX
		writeC(_activeChar.getHennaStatWIT());	//equip WIT

		writeD(3); // slots?

		writeD(_count); //size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(_hennas[i].getSymbolId());
		}
	}


	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__E4_HennaInfo;
	}
}
