package luna.custom;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;




/**
 * @author L2Luna Team
 */
public class Conditions
{
	private Conditions()
	{
		// Dummy default
	}
	
	/**
	 * Check the general requirements for the player to be able to use it
	 * @param player [L2PcInstance]
	 * @return boolean
	 */
	public static boolean checkPlayerConditions(L2PcInstance player)
	{
		if (player.isGM())
		{
			return true;
		}
		
		// Restrictions added here
		if (player.isInOlympiadMode() || player.inObserverMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("Cannot use while in Olympiad.");
			return false;
		}
		
		if (player.getKarma() > 0)
		{
			player.sendMessage("Cannot use while hava karma.");
			return false;
		}
		
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in Jail.");
			return false;
		}
		
		if (player.isEnchanting())
		{
			player.sendMessage("Cannot use while Enchanting.");
			return false;
		}
		
		if (player.isAlikeDead())
		{
			player.sendMessage("Cannot use while Dead or Fake Death.");
			return false;
		}
		
		if (player.isInsideZone(L2Character.ZONE_CHAOTIC))
		{
			player.sendMessage("Cannot use in chaotic zone.");
			return false;
		}
		
		if ((player.getPvpFlag() != 0) && !player.isInsideZone(L2Character.ZONE_PEACE))
		{
			player.sendMessage("Cannot use while in pvp flag.");
			return false;
		}
		
		if (player.isInCombat() && !player.isInsideZone(L2Character.ZONE_PEACE))
		{
			player.sendMessage("Cannot use while in combat.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check the basic requirements for the player to be able to use it
	 * @param player [L2PcInstance]
	 * @return boolean
	 */
	public static boolean checkItemBufferConditions(L2PcInstance player)
	{
		if (player.isGM())
		{
			return true;
		}
		
		// Restrictions added here
		if (player.isInOlympiadMode() || player.inObserverMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("Cannot use while in Olympiad.");
			return false;
		}
		
//		if (NexusEvents.isRegistered(player) || NexusEvents.isInEvent(player))
//		{
//			player.sendMessage("Cannot use while in event.");
//			return false;
//		}
		
		if (player.getKarma() > 0)
		{
			player.sendMessage("Cannot use while hava karma.");
			return false;
		}

		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in Jail.");
			return false;
		}
		
		if (player.isEnchanting())
		{
			player.sendMessage("Cannot use while Enchanting.");
			return false;
		}
		
		if (player.isAlikeDead())
		{
			player.sendMessage("Cannot use while Dead or Fake Death.");
			return false;
		}
		
		if (player.isInsideZone(L2Character.ZONE_CHAOTIC))
		{
			player.sendMessage("Cannot use in chaotic zone.");
			return false;
		}
		
		if ((player.getPvpFlag() != 0) && !player.isInsideZone(L2Character.ZONE_PEACE))
		{
			player.sendMessage("Cannot use while in pvp flag.");
			return false;
		}
		
		if (player.isInCombat() && !player.isInsideZone(L2Character.ZONE_PEACE))
		{
			player.sendMessage("Cannot use while in combat.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check the basic requirements for the player to be able to use it
	 * @param player [L2PcInstance]
	 * @return boolean
	 */
	public static boolean checkPlayerBasicConditions(L2PcInstance player)
	{
		if (player.isGM())
		{
			return true;
		}
		
		// Restrictions added here
		if (player.isInOlympiadMode() || player.inObserverMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("Cannot use while in Olympiad.");
			return false;
		}
		
		if (player.getKarma() > 0)
		{
			player.sendMessage("Cannot use while hava karma.");
			return false;
		}
		
		if (player.isEnchanting())
		{
			player.sendMessage("Cannot use while Enchanting.");
			return false;
		}
		
		if (player.isAlikeDead())
		{
			player.sendMessage("Cannot use while Dead or Fake Death.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check the item count in inventory
	 * @param player [L2PcInstance]
	 * @param itemId
	 * @param count
	 * @return boolean
	 */
	public static boolean checkPlayerItemCount(L2PcInstance player, int itemId, int count)
	{
		if ((player.getInventory().getItemByItemId(itemId) == null) || (player.getInventory().getItemByItemId(itemId).getCount() < count))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}
		return true;
	}
	
	/**
	 * Check the item count in inventory
	 * @param player [L2PcInstance]
	 * @param itemId
	 * @param count
	 * @return boolean
	 */
	public static boolean checkPlayerItemCount(L2PcInstance player, int itemId, long count)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if(itemId == 0 || count == 0)
			return true;
		if ((player.getInventory().getItemByItemId(itemId) == null) || (player.getInventory().getItemByItemId(itemId).getCount() < count))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}
		return true;
	}

	public static boolean checkPlayerTier(L2PcInstance player, float tier, float requiredTier)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if ((player.getActiveChestArmorItem().getUniqueness() == 0) || (player.getActiveChestArmorItem().getUniqueness() < requiredTier))
		{
			player.sendMessage("Your equipment Tier is lower than the required Tier.");
			return false;
		}
		return true;
	}

	public static boolean checkPlayerArmorSet(L2PcInstance player, int requiredArmorSet)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if (requiredArmorSet == 0)
		{
			return true;
		}
		if(player.getActiveChestArmorItem().getItemId() == requiredArmorSet)
		{
			return true;
		}
		else
		return false;
	}
	public static boolean checkCloakRequiredId(L2PcInstance player, int requiredId)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if (requiredId != 0 && player.getActiveCloakItem().getItemId() != requiredId)
		{
			player.sendMessage("Your equipment is not suitable for that dressme.");
			return false;
		}
		return true;
	}
	public static boolean checkWepRequiredId(L2PcInstance player, int requiredId)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if (requiredId != 0 && player.getActiveWeaponItem().getItemId() != requiredId)
		{
			player.sendMessage("Your equipment is not suitable for that dressme.");
			return false;
		}
		return true;
	}
	public static boolean checkShieldRequiredId(L2PcInstance player, int requiredId)
	{
		if (player.isGM() || player.isBeta())
		{
			return true;
		}
		if (requiredId != 0 && player.getActiveShieldItem().getItemId() != requiredId)
		{
			player.sendMessage("Your equipment is not suitable for that dressme.");
			return false;
		}
		return true;
	}
}