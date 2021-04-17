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

import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class RequestRecipeShopMakeItem extends L2GameClientPacket
{
private static final String _C__AF_REQUESTRECIPESHOPMAKEITEM = "[C] B6 RequestRecipeShopMakeItem";
//private static Logger _log = Logger.getLogger(RequestSellItem.class.getName());

private int _id;
private int _recipeId;
@SuppressWarnings("unused")
private long _unknow;

@Override
protected void readImpl()
{
	_id = readD();
	_recipeId = readD();
	_unknow = readQ();
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	if (activeChar.isAccountLockedDown())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	L2PcInstance manufacturer = (L2PcInstance)L2World.getInstance().findObject(_id);
	if (manufacturer == null)
		return;
	
	if (manufacturer.getInstanceId() != activeChar.getInstanceId() && activeChar.getInstanceId() != -1)
		return;
	
	if (activeChar.getPrivateStoreType() != 0)
	{
		activeChar.sendMessage("Cannot make items while trading");
		return;
	}
	if (manufacturer.getPrivateStoreType() != 5)
	{
		//activeChar.sendMessage("Cannot make items while trading");
		return;
	}
	
	if (activeChar.isInCraftMode() || manufacturer.isInCraftMode())
	{
		activeChar.sendMessage("Currently in Craft Mode");
		return;
	}
	if (manufacturer.isInDuel() || activeChar.isInDuel())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_CRAFT_DURING_COMBAT));
		return;
	}
	if (Util.checkIfInRange(150, activeChar, manufacturer, true))
		RecipeController.getInstance().requestManufactureItem(manufacturer, _recipeId, activeChar);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__AF_REQUESTRECIPESHOPMAKEITEM;
}

}
