package net.sf.l2j.gameserver.network.serverpackets;

import java.util.List;

import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Henna;

public class HennaEquipList extends L2GameServerPacket
{
    private static final String _S__E2_HennaEquipList = "[S] ee HennaEquipList";

    private L2PcInstance _player;
    private List<L2Henna> _hennaEquipList;

    public HennaEquipList(L2PcInstance player)
    {
        _player = player;  
        _hennaEquipList = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId());
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0xee);
        writeQ(_player.getAdena());          //activeChar current amount of aden
        writeD(3);     //available equip slot
        writeD(_hennaEquipList.size());
        
        for (L2Henna temp: _hennaEquipList)
        {
            writeD(temp.getSymbolId()); //symbolId
            writeD(temp.getDyeId());       //itemId of dye
            writeQ(temp.getAmountDyeRequire());    //amount of dye require
            writeQ(temp.getPrice());    //amount of adena required
            writeD(1);            //meet the requirement or not
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _S__E2_HennaEquipList;
    }
}