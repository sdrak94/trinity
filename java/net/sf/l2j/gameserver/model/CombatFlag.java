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
package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class CombatFlag
{
    //private static final Logger _log = Logger.getLogger(CombatFlag.class.getName());

    protected L2PcInstance _player = null;
    public int playerId = 0;
    private L2ItemInstance _item = null;


    private Location _location;
    public L2ItemInstance itemInstance;
    
    private int _itemId;
    
    @SuppressWarnings("unused")
    private int _heading;
    @SuppressWarnings("unused")
    private int _fortId;
    
    // =========================================================
    // Constructor
    public CombatFlag(int fort_id, int x, int y, int z, int heading, int item_id)
    {
        _fortId = fort_id;
        _location = new Location(x,y,z,heading);
        _heading = heading;
        _itemId = item_id;
    }

    public synchronized void spawnMe()
    {
        L2ItemInstance i;

        // Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
        i = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
        i.spawnMe(_location.getX(), _location.getY(),  _location.getZ());
        itemInstance = i;    
    }

    public synchronized void unSpawnMe()
    {
        if ( _player != null )
            dropIt();
        
        if ( itemInstance != null )
        {
            itemInstance.decayMe();
        }
    }
    
    public boolean activate(L2PcInstance player, L2ItemInstance item)
    {
        if (player.isMounted())
        {
        	player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
        	return false;
        }

        // Player holding it data
        _player = player;
        playerId = _player.getObjectId();
        itemInstance = null;

        // Equip with the weapon
        _item = item;
        _player.getInventory().equipItemAndRecord(_item);
        SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
        sm.addItemName(_item);
        _player.sendPacket(sm);

        // Refresh inventory
        if (!Config.FORCE_INVENTORY_UPDATE)
        {
            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(_item);
            _player.sendPacket(iu);
        }
        else _player.sendPacket(new ItemList(_player, false));
        
        // Refresh player stats
        _player.broadcastUserInfo();
        _player.setCombatFlagEquipped(true);
        return true;
    }

    public void dropIt()
    {
        // Reset player stats
        _player.setCombatFlagEquipped(false);
        int slot = _player.getInventory().getSlotFromItem(_item);
        _player.getInventory().unEquipItemInBodySlotAndRecord(slot);
        _player.destroyItem("CombatFlag", _item, null, true);
        _item = null;
        _player.broadcastUserInfo();
        _player = null;
        playerId = 0;
    }
}
