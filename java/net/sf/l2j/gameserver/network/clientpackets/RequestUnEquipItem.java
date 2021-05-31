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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.3.2.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestUnEquipItem extends L2GameClientPacket
{
	private static final String _C__11_REQUESTUNEQUIPITEM = "[C] 11 RequestUnequipItem";
	private static Logger _log = Logger.getLogger(RequestUnEquipItem.class.getName());

	// cd
	private int _slot;

	/**
	 * packet type id 0x11
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_slot = readD();
	}

	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
            _log.fine("request unequip slot " + _slot);

		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
		    return;

		L2ItemInstance item = activeChar.getInventory().getPaperdollItemByL2ItemId(_slot);
		if (item != null && item.isWear())
		{
			// Wear-items are not to be unequipped
			return;
		}
		// Prevent of unequiping a cursed weapon
		if (_slot == L2Item.SLOT_LR_HAND && activeChar.isCursedWeaponEquipped())
		{
			// Message ?
			return;
		}
		if (NewHuntingGrounds._started && activeChar._inEventHG)
		{
			return;
		}
		// Prevent player from unequipping items in special conditions
       	if (activeChar.isStunned() || activeChar.isSleeping()
       			|| activeChar.isParalyzed() || activeChar.isAlikeDead())
        {
            activeChar.sendMessage("Your status does not allow you to do that.");
            return;
        }
        if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
        	return;

		L2ItemInstance[] unequiped =
			activeChar.getInventory().unEquipItemInBodySlotAndRecord(_slot);

		// show the update in the inventory
		InventoryUpdate iu = new InventoryUpdate();

		for (L2ItemInstance itm: unequiped)
		{
			iu.addModifiedItem(itm);
		}

		activeChar.sendPacket(iu);

		// On retail you don't stop hitting if unequip something. REOMVED: activeChar.abortAttack();
		activeChar.broadcastUserInfo();

		// this can be 0 if the user pressed the right mousebutton twice very fast
		if (unequiped.length > 0)
		{

            SystemMessage sm = null;
            if (unequiped[0].getEnchantLevel() > 0)
            {
            	sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
            	sm.addNumber(unequiped[0].getEnchantLevel());
            	sm.addItemName(unequiped[0]);
            }
            else
            {
	            sm = new SystemMessage(SystemMessageId.S1_DISARMED);
	            sm.addItemName(unequiped[0]);
            }
            activeChar.sendPacket(sm);
            sm = null;
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__11_REQUESTUNEQUIPITEM;
	}
}
