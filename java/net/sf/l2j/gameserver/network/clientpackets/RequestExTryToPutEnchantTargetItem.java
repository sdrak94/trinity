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

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 *
 * @author  KenM
 */
public class RequestExTryToPutEnchantTargetItem extends AbstractEnchantPacket
{

private int _objectId = 0;

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
 */
@Override
public String getType()
{
	return "[C] D0:4F RequestExTryToPutEnchantTargetItem";
}

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
 */
@Override
protected void readImpl()
{
	_objectId = readD();
}

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
 */
@Override
protected void runImpl()
{
	if (_objectId == 0)
		return;
	
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar != null)
	{
		if (activeChar.isEnchanting())
			return;
		
		if (activeChar.isAccountLockedDown())
		{
			activeChar.sendMessage("Your account is in lockdown");
			activeChar.setActiveEnchantItem(null);
			return;
		}
		
		L2ItemInstance item = (L2ItemInstance) L2World.getInstance().findObject(_objectId);
		if (item == null)
			return;
		L2ItemInstance scroll = activeChar.getActiveEnchantItem();
		
		if (scroll == null)
			return;
		
		// template for scroll
		EnchantScroll scrollTemplate = getEnchantScroll(scroll);
		
		if (!scrollTemplate.isValid(item) || !isEnchantable(item) || item.getOwnerId() != activeChar.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.DOES_NOT_FIT_SCROLL_CONDITIONS));
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
			return;
		}
		activeChar.setIsEnchanting(true);
		activeChar.setActiveEnchantTimestamp(System.currentTimeMillis());
		activeChar.sendPacket(new ExPutEnchantTargetItemResult(_objectId));
		
		final String penalty;
		
		switch (scrollTemplate.determineFateOfItemIfFail(item))
		{
		case ITEM_DESTROYED:
			penalty = "ITEM DESTROYED";
			break;
		case ENCHANT_TO_4_OR_0:
			if (item.getEnchantLevel() >= 4)
				penalty = "enchant set to +4";
			else
				penalty = "enchant set to +0";
			break;
		case ENCHANT_MINUS_ONE_OR_NEXT_LEVEL:
			if (item.getEnchantLevel() >= 10)
			{
				if (item.getEnchantLevel() > 11)
					penalty = "enchant -1";
				else
					penalty = "enchant set to +10";
			}
			else if (item.getEnchantLevel() >= 3)
			{
				if (item.getEnchantLevel() > 4)
					penalty = "enchant -1";
				else
					penalty = "enchant set to +3";
			}
			else
			{
				if (item.getEnchantLevel() > 1)
					penalty = "enchant -1";
				else
					penalty = "enchant set to +0";
			}
			break;
		case REMAIN_SAME_ENCHANT:
			penalty = "none";
			break;
		case ENCHANT_TO_10_OR_6_OR_3_OR_0:
			if (item.getUniqueness() <= 3 && item.getEnchantLevel() >= 14)
				penalty = "enchant set to +14";
			else if (item.getUniqueness() <= 3 && item.getEnchantLevel() >= 12)
				penalty = "enchant set to +12";
			else if (item.getEnchantLevel() >= 10)
					penalty = "enchant set to +10";
			else if (item.getEnchantLevel() >= 6)
					penalty = "enchant set to +6";
			else if (item.getEnchantLevel() >= 3)
					penalty = "enchant set to +3";
			else
					penalty = "enchant set to +0";
			break;
		case ENCHANT_TO_7_OR_3_OR_0:
			if (item.getEnchantLevel() >= 7)
				penalty = "enchant set to +7";
			else if (item.getEnchantLevel() >= 3)
				penalty = "enchant set to +3";
			else
				penalty = "enchant set to +0";
			break;
		case RETURNS_TO_0:
			penalty = "enchant set to +0";
			break;
		default :
			penalty = "none";
			break;
		}
		
		final int chance = Math.min(scrollTemplate.getChance(item, null), 100);
		
		if (chance >= 100)
			activeChar.sendMessage("Success chance: "+chance+"% - Max enchant: +"+scrollTemplate.getMaxEnchantLevel(item));
		else
			activeChar.sendMessage("Success chance: "+chance+"% - Failure penalty: "+penalty+" - Max enchant: +"+scrollTemplate.getMaxEnchantLevel(item));
	}
}
}
