package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;


public final class RequestHennaList extends L2GameClientPacket
{
    private static final String _C__BA_RequestHennaList = "[C] ba RequestHennaList";

    // This is just a trigger packet...
    @SuppressWarnings("unused")
    private int _unknown;

    @Override
	protected void readImpl()
    {
        _unknown = readD(); // ??
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) return;

        activeChar.sendPacket(new HennaEquipList(activeChar));
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _C__BA_RequestHennaList;
    }
}