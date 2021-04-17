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

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.util.Rnd;

/**
 * This class handles following admin commands:
 * - itemcreate = show menu
 * - create_item <id> [num] = creates num items with respective id, if num is not specified, assumes 1.
 *
 * @version $Revision: 1.2.2.2.2.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminCreateItem implements IAdminCommandHandler
{

//ZGirl
private static final double MAX_ITEM_DENSITY = 0.5;
private static final int MAX_ITEM_DROP = 500;
private static final int MAX_ITEM_RADIUS = 1000;
private static final String[] ADMIN_COMMANDS =
{
	"admin_itemcreate",
	"admin_give_item",
	"admin_bind_item",
	"admin_itemrain",  //Zgirl
	"admin_create_item"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_itemcreate"))
	{
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
	else if (command.startsWith("admin_create_item"))
	{
		try
		{
			String val = command.substring(17);
			StringTokenizer st = new StringTokenizer(val);
			if (st.countTokens() == 2)
			{
				String id = st.nextToken();
				int idval = Integer.parseInt(id);
				String num = st.nextToken();
				long numval = Long.parseLong(num);
				createItem(activeChar, idval, numval);
			}
			else if (st.countTokens() == 1)
			{
				String id = st.nextToken();
				int idval = Integer.parseInt(id);
				createItem(activeChar, idval, 1);
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //itemcreate <itemId> [amount]");
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendMessage("Specify a valid number.");
		}
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
	else if (command.startsWith("admin_give_item") || command.startsWith("admin_bind_item"))
	{
		final L2Object target = activeChar.getTarget();
		
		if (target != null && target instanceof L2PcInstance)
		{
			if (target != activeChar)
			{
				if (!activeChar.getAccessLevel().allowTransaction())
				{
					activeChar.sendMessage("Transactions are disable for your Access Level");
					return false;
				}
			}
			try
			{
				String val = command.substring(15);
				StringTokenizer st = new StringTokenizer(val);
				
				if (st.countTokens() == 3)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					long numval = Long.parseLong(num);
					String enchant = st.nextToken();
					int enchantval = Integer.parseInt(enchant);
					giveItem(activeChar, (L2PcInstance)target, idval, numval, enchantval, command.startsWith("admin_bind_item"));
				}
				else if (st.countTokens() == 2)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					long numval = Long.parseLong(num);
					giveItem(activeChar, (L2PcInstance)target, idval, numval, 0, command.startsWith("admin_bind_item"));
				}
				else if (st.countTokens() == 1)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					giveItem(activeChar, (L2PcInstance)target, idval, 1, 0, command.startsWith("admin_bind_item"));
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Usage: //itemcreate <itemId> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendMessage("Specify a valid number.");
			}
		}
		else
		{
			activeChar.sendMessage("Invalid target.");
		}
		
		AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
	}
	else if (command.startsWith("admin_itemrain"))	//Zgirl
	{
		try
		{
			String val = command.substring(15);
			StringTokenizer st = new StringTokenizer(val);
			if (st.countTokens() == 3)
			{
				String id = st.nextToken();
				int idval = Integer.parseInt(id);
				String num = st.nextToken();
				int numval = Integer.parseInt(num);
				String rad = st.nextToken();
				int radval = Integer.parseInt(rad);
				itemDropArea(activeChar, idval, numval, radval);
			}
			else
			{
				activeChar.sendMessage("Usage: //itemrain <itemId> <amount> <radius>");
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //itemrain <itemId> <amount> <radius>");
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendMessage("Specify a valid number.");
		}
	}
	
	return true;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void createItem(L2PcInstance activeChar, int id, long num)
{
	L2Item template = ItemTable.getInstance().getTemplate(id);
	if (template == null)
	{
		activeChar.sendMessage("This item doesn't exist.");
		return;
	}
	if (num > 20)
	{
		if (!template.isStackable())
		{
			activeChar.sendMessage("This item does not stack - Creation aborted.");
			return;
		}
	}
	
	final String itemName = template.getName();
	
	String process = "createItem";
	
	if (id == L2Item.DONATION_TOKEN)
	{
		if (!(activeChar.getName().equalsIgnoreCase("[GM]Brado") || activeChar.getName().equalsIgnoreCase("[GM]Fate") || activeChar.getName().equalsIgnoreCase("[GM]Alfie")))
			return;
		else
			process = "donation_token";
	}
	
	activeChar.getInventory().addItem(process, id, num, activeChar, activeChar);
	
	ItemList il = new ItemList(activeChar, true);
	activeChar.sendPacket(il);
	
	activeChar.sendMessage("You have spawned " + num + " item(s) number " + id + " ("+itemName+") in your inventory.");
}
private void giveItem(L2PcInstance activeChar, L2PcInstance target, int id, long num, int enchant, boolean bind)
{
	final L2Item template = ItemTable.getInstance().getTemplate(id);
	
	if (template == null)
	{
		activeChar.sendMessage("This item doesn't exist.");
		return;
	}
	
	if (num > 30)
	{
		if (!template.isStackable())
		{
			activeChar.sendMessage("This item does not stack - Creation aborted.");
			return;
		}
	}
	
	String itemName = template.getName();
	
	String process = "giveItem";
	
	if (id == L2Item.DONATION_TOKEN)
	{
		if (!(activeChar.getName().equalsIgnoreCase("[GM]Brado") || activeChar.getName().equalsIgnoreCase("[GM]Fate") || activeChar.getName().equalsIgnoreCase("[GM]Alfie")))
			return;
		else
			process = "donation_token";
	}
	
	L2ItemInstance newItem = target.getInventory().addItem(process, id, num, target, activeChar);
	
	if (enchant > 0 && newItem.isEnchantable())
	{
		newItem.setEnchantLevel(enchant);
		itemName = "+"+enchant+" "+itemName;
	}
	
	if (!newItem.isStackable())
	{
		if (bind)
		{
			newItem.setUntradeableTimer(9999999910000L);
		}
		else
		{
			final long untTime = newItem.getUntradeableTime();
			
			if (untTime < 9999999900000L)
			{
				final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_GM_TRADE*60*60*1000);
				
				if (untTime + 3600000 < newTime)
				{
					newItem.setUntradeableTimer(newTime);
				}
			}
		}
	}
	
	ItemList il = new ItemList(target, true);
	target.sendPacket(il);
	
	activeChar.sendMessage("You have spawned " + num + " item(s) number " + id + " ("+itemName+") in "+target.getName()+"'s inventory.");
	//if (activeChar != target)
		//target.sendMessage("Admin has given you " + num + " " + itemName + " in your inventory.");
}
//ZGirl
private void itemDropArea(L2PcInstance activeChar, int itemId, int quantity, int radius)
{
	L2ItemInstance item;
	
	L2Item template = ItemTable.getInstance().getTemplate(itemId);
	
	if (template == null)
	{
		activeChar.sendMessage("This item doesn't exist.");
		return;
	}
	if (quantity < 1)
	{
		activeChar.sendMessage("Incorrect item quantity.");
		return;
	}
	if (quantity > MAX_ITEM_DROP)
	{
		activeChar.sendMessage("You can't drop more than "+MAX_ITEM_DROP+" items at a time.");
		return;
	}
	if (radius < 1)
	{
		activeChar.sendMessage("Incorrect drop radius.");
		return;
	}
	if (radius > MAX_ITEM_RADIUS)
	{
		activeChar.sendMessage("Drop radius cannot exceed "+MAX_ITEM_RADIUS+".");
		return;
	}
	if ((quantity/radius) > MAX_ITEM_DENSITY)
	{
		activeChar.sendMessage("You can't drop more than "+ MAX_ITEM_DENSITY +" items per radius unit.");
		return;
	}
	
	/* Add some validation here for special items.
	 * We don't want to accidentally drop 100 legendary enchant scrolls or titanium/dread runes
	 *
	 *            if (quantity > 100 && itemId == legendary enchant weapon scroll id)
	 *            {
	 *                  activeChar.sendMessage("You can't drop more than 100 of this item.");
	 *                  return;
	 *            }
	 */
	
	
	for (int i = 0; i < quantity; i++)
	{
		
		int signX = (Rnd.nextInt(2) == 0) ? -1 : 1;
		int signY = (Rnd.nextInt(2) == 0) ? -1 : 1;
		int randX = Rnd.nextInt(radius);
		int randY = Rnd.nextInt(radius);
		
		int _x = activeChar.getX() + signX*randX;
		int _y = activeChar.getY() + signY*randY;
		int _z = activeChar.getZ() + 50;
		
		item = activeChar.getInventory().addItem("Admin", itemId, 1, activeChar, null);
		activeChar.dropItem("Drop", item.getObjectId(), 1, _x, _y, _z, null, false);
		
	}
	activeChar.sendMessage("You have scattered " + quantity + " item(s) number " + itemId + " in a "+ radius +" radius.");
}
}
