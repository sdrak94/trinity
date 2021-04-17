package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowBaseAttributeCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.util.Rnd;

public class RequestExRemoveItemAttribute extends L2GameClientPacket
{
private static String _C__D0_23_REQUESTEXREMOVEITEMATTRIBUTE = "[C] D0:23 RequestExRemoveItemAttribute";

private int _objectId;

public RequestExRemoveItemAttribute()
{
}

@Override
public void readImpl()
{
	_objectId = readD();
}

@Override
public void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	if (activeChar.isAccountLockedDown())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	
	final L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_objectId);
	
	if (targetItem == null)
		return;
	
	if (targetItem.getElementals() == null || targetItem.isHeroItem())
		return;
	
	final int eleType = targetItem.getElementals().getElement();
	final int elePower = targetItem.getElementals().getValue();
	
	if (targetItem.isEquipped())
		targetItem.getElementals().removeBonus(activeChar);
	
	targetItem.clearElementAttr();
	
	InventoryUpdate iu = new InventoryUpdate();
	iu.addModifiedItem(targetItem);
	activeChar.sendPacket(iu);
	SystemMessage sm;
	
	if (eleType >= 0 && elePower > 6)
	{
		final int stoneId;
		
		switch (eleType)
		{
		case 0:
			stoneId = 9546;  //fire stone
			break;
		case 1:
			stoneId = 9547;  //water stone
			break;
		case 2:
			stoneId = 9549;  //wind stone
			break;
		case 3:
			stoneId = 9548;  //Earth stone
			break;
		case 4:
			stoneId = 9551;  //divine stone
			break;
		case 5:
			stoneId = 9550;  //dark stone
			break;
		default:
			return;
		}
		
		int returnedAmount = 0;
		final int type2 = targetItem.getItem().getType2();
		
		switch (type2)
		{
		case L2Item.TYPE2_WEAPON:
		{
			if (elePower > 20)
			{
				returnedAmount = Math.min(Math.max((elePower - 20)/10, 1), 11);
				
				if (returnedAmount >= 6)
				{
					if (Rnd.get(9) == 0)
						returnedAmount += 6;
				}
			}
		}
		break;
		case L2Item.TYPE2_SHIELD_ARMOR:
		case L2Item.TYPE2_ACCESSORY:
		{
			if (elePower > 6)
			{
				returnedAmount = Math.min(Math.max((elePower - 6)/10, 1), 11);
				
				if (returnedAmount >= 3)
				{
					if (Rnd.get(9) == 0)
						returnedAmount += 3;
				}
			}
		}
		break;
		default:
			return;
		}
		
		if (returnedAmount > 0)
		{
			assert (returnedAmount < 50);
			
			activeChar.sendMessage("Removing the attribute from your "+targetItem.getName()+" has gleaned you some elemental stones back");
			activeChar.addItem("Remove Attribute", stoneId, returnedAmount, targetItem, true);
		}
	}
	
	if (targetItem.getEnchantLevel() > 0)
	{
		sm = new SystemMessage(SystemMessageId.S1_S2_ELEMENTAL_POWER_REMOVED);
		sm.addNumber(targetItem.getEnchantLevel());
		sm.addItemName(targetItem);
	}
	else
	{
		sm = new SystemMessage(SystemMessageId.S1_ELEMENTAL_POWER_REMOVED);
		sm.addItemName(targetItem);
	}
	
	activeChar.sendPacket(sm);
	activeChar.getInventory().reloadEquippedItems();
	activeChar.broadcastUserInfo();
	activeChar.sendPacket(new ExShowBaseAttributeCancelWindow(activeChar));
	return;
}

@Override
public String getType()
{
	return _C__D0_23_REQUESTEXREMOVEITEMATTRIBUTE;
}
}