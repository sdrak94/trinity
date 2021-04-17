/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class SellListProcure extends L2GameServerPacket
{
    private static final String _S__E9_SELLLISTPROCURE = "[S] ef SellListProcure";
    //private static Logger _log = Logger.getLogger(SellListProcure.class.getName());

    private final L2PcInstance _activeChar;
    private long _money;
    private Map<L2ItemInstance,Long> _sellList = new FastMap<L2ItemInstance,Long>();
    private List<CropProcure> _procureList = new FastList<CropProcure>();
    private int _castle;

    public SellListProcure(L2PcInstance player, int castleId)
    {
        _money = player.getAdena();
        _activeChar = player;
        _castle = castleId;
        _procureList =  CastleManager.getInstance().getCastleById(_castle).getCropProcure(0);
        for(CropProcure c : _procureList)
        {
            L2ItemInstance item = _activeChar.getInventory().getItemByItemId(c.getId());
            if(item != null && c.getAmount() > 0)
            {
                _sellList.put(item,c.getAmount());
            }
        }
    }

    @Override
	protected final void writeImpl()
    {
        writeC(0xef);
        writeQ(_money);         // money
        writeD(0x00);           // lease ?
        writeH(_sellList.size());         // list size

        for(L2ItemInstance item : _sellList.keySet())
        {
            writeH(item.getItem().getType1());
            writeD(item.getObjectId());
            writeD(item.getItemId());
            writeQ(_sellList.get(item));  // count
            writeH(item.getItem().getType2());
            writeH(0);  // unknown
            writeQ(0);  // price, u shouldnt get any adena for crops, only raw materials
        }
    }

    @Override
	public String getType()
    {
        return _S__E9_SELLLISTPROCURE;
    }
}
