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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - enchant_armor
 *
 * @version $Revision: 1.3.2.1.2.10 $ $Date: 2005/08/24 21:06:06 $
 */
public class AdminEnchant implements IAdminCommandHandler
{
private static Logger _log = Logger.getLogger(AdminEnchant.class.getName());

private static final String[] ADMIN_COMMANDS =
{
	"admin_seteh",//6
	"admin_setec",//10
	"admin_seteg",//9
	"admin_setel",//11
	"admin_seteb",//12
	"admin_setew",//7
	"admin_setes",//8
	"admin_setle",//1
	"admin_setre",//2
	"admin_setlf",//4
	"admin_setrf",//5
	"admin_seten",//3
	"admin_setun",//0
	"admin_setba",//13
	"admin_setbe",
	"admin_enchant"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_enchant"))
	{
		showMainPage(activeChar);
	}
	else
	{
		int armorType = -1;
		
		if (command.startsWith("admin_seteh"))
			armorType = Inventory.PAPERDOLL_HEAD;
		else if (command.startsWith("admin_setec"))
			armorType = Inventory.PAPERDOLL_CHEST;
		else if (command.startsWith("admin_seteg"))
			armorType = Inventory.PAPERDOLL_GLOVES;
		else if (command.startsWith("admin_seteb"))
			armorType = Inventory.PAPERDOLL_FEET;
		else if (command.startsWith("admin_setel"))
			armorType = Inventory.PAPERDOLL_LEGS;
		else if (command.startsWith("admin_setew"))
			armorType = Inventory.PAPERDOLL_RHAND;
		else if (command.startsWith("admin_setes"))
			armorType = Inventory.PAPERDOLL_LHAND;
		else if (command.startsWith("admin_setle"))
			armorType = Inventory.PAPERDOLL_LEAR;
		else if (command.startsWith("admin_setre"))
			armorType = Inventory.PAPERDOLL_REAR;
		else if (command.startsWith("admin_setlf"))
			armorType = Inventory.PAPERDOLL_LFINGER;
		else if (command.startsWith("admin_setrf"))
			armorType = Inventory.PAPERDOLL_RFINGER;
		else if (command.startsWith("admin_seten"))
			armorType = Inventory.PAPERDOLL_NECK;
		else if (command.startsWith("admin_setun"))
			armorType = Inventory.PAPERDOLL_UNDER;
		else if (command.startsWith("admin_setba"))
			armorType = Inventory.PAPERDOLL_BACK;
		else if (command.startsWith("admin_setbe"))
			armorType = Inventory.PAPERDOLL_BELT;
		
		if (armorType != -1)
		{
			try
			{
				int ench = Integer.parseInt(command.substring(12));
				
				// check value
				if (ench < 0 || ench > 65535)
					activeChar.sendMessage("You must set the enchant level to be between 0-65535.");
				else
					setEnchant(activeChar, ench, armorType);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Set enchant error: " + e);
				activeChar.sendMessage("Please specify a new enchant value.");
			}
			catch (NumberFormatException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Set enchant error: " + e);
				activeChar.sendMessage("Please specify a valid new enchant value.");
			}
		}
		
		// show the enchant menu after an action
		showMainPage(activeChar);
	}
	
	return true;
}

private void setEnchant(L2PcInstance activeChar, int ench, int armorType)
{
	// get the target
	L2Object target = activeChar.getTarget();
	if (target == null)
		target = activeChar;
	L2PcInstance player = null;
	if (target instanceof L2PcInstance)
	{
		player = (L2PcInstance) target;
		
		if (player != activeChar)
		{
			if (!activeChar.getAccessLevel().allowTransaction())
			{
				activeChar.sendMessage("Transactions are disable for your Access Level");
				return;
			}
		}
	}
	else
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
		return;
	}
	
	// now we need to find the equipped weapon of the targeted character...
	int curEnchant = 0; // display purposes only
	L2ItemInstance itemInstance = null;
	
	// only attempt to enchant if there is a weapon equipped
	L2ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
	if (parmorInstance != null && parmorInstance.getLocationSlot() == armorType)
	{
		itemInstance = parmorInstance;
	}
	else
	{
		// for bows/crossbows and double handed weapons
		parmorInstance = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		if (parmorInstance != null && parmorInstance.getLocationSlot() == Inventory.PAPERDOLL_LRHAND)
			itemInstance = parmorInstance;
	}
	
	if (itemInstance != null)
	{
		curEnchant = itemInstance.getEnchantLevel();
		
		if (!player.isGM())
		{
			final long untTime = itemInstance.getUntradeableTime();
			
			if (untTime < 9999999900000L)
			{
				final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_GM_ENCHANT*60*60*1000);
				
				if (untTime + 3600000 < newTime)
				{
					itemInstance.setUntradeableTimer(newTime);
					player.sendMessage("Your "+itemInstance.getName()+" is now untradeable for "+(newTime-System.currentTimeMillis())/3600000+" hours");
				}
			}
		}
		
		// set enchant value
		player.getInventory().unEquipItemInSlotAndRecord(armorType);
		itemInstance.setEnchantLevel(ench);
		player.getInventory().equipItemAndRecord(itemInstance);
		
		// send packets
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(itemInstance);
		player.sendPacket(iu);
		player.broadcastUserInfo();
		
		// informations
		activeChar.sendMessage("Changed enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
		player.sendMessage("Admin has changed the enchantment of your " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
	}
}

public static void showMainPage(L2PcInstance activeChar)
{
	AdminHelpPage.showHelpPage(activeChar, "enchant.htm");
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}
}