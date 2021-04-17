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
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.3.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestCrystallizeItem extends L2GameClientPacket
{
private static final String _C__72_REQUESTDCRYSTALLIZEITEM = "[C] 72 RequestCrystallizeItem";

private static Logger _log = Logger.getLogger(RequestCrystallizeItem.class.getName());

private int _objectId;
private long _count;

@Override
protected void readImpl()
{
	_objectId = readD();
	_count = readQ();
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
	{
		_log.fine("RequestCrystalizeItem: activeChar was null");
		return;
	}
	
	if (_count <= 0)
	{
		Util.handleIllegalPlayerAction(activeChar, "[RequestCrystallizeItem] count <= 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), Config.DEFAULT_PUNISH);
		return;
	}
	
	if (activeChar.isAccountLockedDown() || activeChar.isInJail())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	
	if (activeChar.getPrivateStoreType() != 0 || activeChar.isInCrystallize())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
		return;
	}
	
	int skillLevel = activeChar.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
	if (skillLevel <= 0)
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	PcInventory inventory = activeChar.getInventory();
	if (inventory != null)
	{
		L2ItemInstance item = inventory.getItemByObjectId(_objectId);
		if (item == null || item.isWear())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (item.isHeroItem() || item.isCastleItem())
			return;
		
		if (_count > item.getCount())
			_count = activeChar.getInventory().getItemByObjectId(_objectId).getCount();
	}
	
	L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);
	if (itemToRemove == null || itemToRemove.isWear() || itemToRemove.isShadowItem() || itemToRemove.isTimeLimitedItem())
		return;
	
	if (!itemToRemove.getItem().isCrystallizable()
			|| (itemToRemove.getItem().getCrystalCount() <= 0)
			|| (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_NONE))
	{
		_log.warning(activeChar.getName() + " (" + activeChar.getObjectId() + ") tried to crystallize " + itemToRemove.getItem().getItemId());
		return;
	}
	
	// Check if the char can crystallize items and return if false;
	boolean canCrystallize = true;
	
	switch (itemToRemove.getItem().getItemGradeSPlus())
	{
	case L2Item.CRYSTAL_C:
	{
		if (skillLevel <= 1)
			canCrystallize = false;
		break;
	}
	case L2Item.CRYSTAL_B:
	{
		if (skillLevel <= 2)
			canCrystallize = false;
		break;
	}
	case L2Item.CRYSTAL_A:
	{
		if (skillLevel <= 3)
			canCrystallize = false;
		break;
	}
	case L2Item.CRYSTAL_S:
	{
		if (skillLevel <= 4)
			canCrystallize = false;
		break;
	}
	}
	
	if (!canCrystallize)
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	activeChar.setInCrystallize(true);
	
	// unequip if needed
	if (itemToRemove.isEquipped())
	{
		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot());
		InventoryUpdate iu = new InventoryUpdate();
		for (L2ItemInstance item: unequiped)
			iu.addModifiedItem(item);
		activeChar.sendPacket(iu);
		
		SystemMessage msg;
		if (itemToRemove.getEnchantLevel() > 0)
		{
			msg = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
			msg.addNumber(itemToRemove.getEnchantLevel());
			msg.addItemName(itemToRemove);
		}
		else
		{
			msg = new SystemMessage(SystemMessageId.S1_DISARMED);
			msg.addItemName(itemToRemove);
		}
		activeChar.sendPacket(msg);
	}
	
	// remove from inventory
	L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Crystalize", _objectId, _count, activeChar, null);
	
	InventoryUpdate iu = new InventoryUpdate();
	iu.addRemovedItem(removedItem);
	activeChar.sendPacket(iu);
	
	// add crystals
	int crystalId = itemToRemove.getItem().getCrystalItemId();
	int crystalAmount = itemToRemove.getCrystalCount();
	L2ItemInstance createditem = activeChar.getInventory().addItem("Crystalize", crystalId, crystalAmount, activeChar, itemToRemove);
	
	SystemMessage sm;
	sm = new SystemMessage(SystemMessageId.S1_CRYSTALLIZED);
	sm.addItemName(removedItem);
	activeChar.sendPacket(sm);
	
	sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
	sm.addItemName(createditem);
	sm.addItemNumber(crystalAmount);
	activeChar.sendPacket(sm);
	
	activeChar.broadcastUserInfo();
	
	L2World world = L2World.getInstance();
	world.removeObject(removedItem);
	
	activeChar.setInCrystallize(false);
}

/*
 * (non-Javadoc)
 *
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__72_REQUESTDCRYSTALLIZEITEM;
}
}
