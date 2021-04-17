package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class PetFood implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		int itemId = item.getItemId();
		switch (itemId)
		{
			case 2515: // Wolf's food
				useFood(playable, 2048, item);
				break;
			case 4038: // Hatchling's food
				useFood(playable, 2063, item);
				break;
			case 5168: // Strider's food
				useFood(playable, 2101, item);
				break;
			case 5169: // ClanHall / Castle Strider's food
				useFood(playable, 2102, item);
				break;
			case 6316: // Wyvern's food
				useFood(playable, 2180, item);
				break;
			case 7582: // Baby Pet's food
				useFood(playable, 2048, item);
				break;
			case 9668: // Great Wolf's food
				useFood(playable, 2361, item);
				break;
			case 10425: // Improved Baby Pet's food
				useFood(playable, 2361, item);
				break;
		}
	}
	
	public boolean useFood(L2Playable activeChar, int magicId, L2ItemInstance item)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, 1);
		
		if (skill != null)
		{
			if (activeChar instanceof L2PetInstance)
			{
				if (((L2PetInstance)activeChar).destroyItem("Consume", item.getObjectId(), 1, null, false))
				{
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, magicId, 1, 0, 0));
					((L2PetInstance)activeChar).setCurrentFed(((L2PetInstance)activeChar).getCurrentFed() + skill.getFeed() * Config.PET_FOOD_RATE);
					((L2PetInstance)activeChar).broadcastStatusUpdate();
					if (((L2PetInstance)activeChar).getCurrentFed() < (0.55 * ((L2PetInstance)activeChar).getPetData().getPetMaxFeed()))
						((L2PetInstance)activeChar).getOwner().sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_ATE_A_LITTLE_BUT_IS_STILL_HUNGRY));
					return true;
				}
			}
			else if (activeChar instanceof L2PcInstance)
			{
				L2PcInstance player = ((L2PcInstance)activeChar);
				int itemId = item.getItemId();
				boolean canUse = false;
				if (player.isMounted())
				{
					int petId = player.getMountNpcId();
					if (L2PetDataTable.isWolf(petId) && L2PetDataTable.isWolfFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isEvolvedWolf(petId) && L2PetDataTable.isEvolvedWolfFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isSinEater(petId) && L2PetDataTable.isSinEaterFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isHatchling(petId) && L2PetDataTable.isHatchlingFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isStrider(petId) && L2PetDataTable.isStriderFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isWyvern(petId) && L2PetDataTable.isWyvernFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isBaby(petId) && L2PetDataTable.isBabyFood(itemId))
					{
						canUse = true;
					}
					else if (L2PetDataTable.isImprovedBaby(petId) && L2PetDataTable.isImprovedBabyFood(itemId))
					{
						canUse = true;
					}
					if (canUse)
					{
						if (player.destroyItem("Consume", item.getObjectId(), 1, null, false))
						{
							player.broadcastPacket(new MagicSkillUse(activeChar, activeChar, magicId, 1, 0, 0));
							player.setCurrentFeed(player.getCurrentFeed() + skill.getFeed());
						}
						return true;
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED));
						return false;
					}
				}
				else
					player.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED));
				return false;
			}
		}
		return false;
	}
}
