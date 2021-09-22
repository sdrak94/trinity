package luna.custom.DressMeEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class DressMeHandler
{
	private static final Logger _log = Logger.getLogger(DressMeHandler.class.getName());
	
	public static void tryingItem(L2PcInstance player, L2ItemInstance item, int visual)
	{
		boolean isWeapon = item.isWeapon();
		
		if (player.isTrying() && isWeapon)
		{
			player.sendMessage("You are already Trying an item.");
			return;
		}
		
		int tryTime = 2500;
		item.setTryingItemId(visual);
		if (visual > 0)
		{
			if(isWeapon)
			{
				player.setIsTrying(true);
			}
			String itemName = Util.getItemName(visual);
			player.sendMessage("You are now trying " + Util.getItemName(visual));
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				if (player != null)
				{
					if (item != null)
					{
						player.sendMessage("You are no longer trying " + itemName);
						if(isWeapon)
						{
							player.setIsTrying(false);
						}
						DressMeHandler.tryingItem(player, item, 0);
						player.broadcastUserInfo();
					}
				}
			}, tryTime);
		}
		player.broadcastUserInfo();
	}
	
	public static void visuality(L2PcInstance player, L2ItemInstance item, int visual, long time)
	{
		item.setVisualItemId(visual);
		updateVisualInDb(item, visual, time);
		if (visual > 0)
		{
			player.sendMessage(item.getName() + " visual change to " + Util.getItemName(visual));
		}
		else
		{
			player.sendMessage("Visual removed from " + item.getName() + ".");
		}
		player.broadcastUserInfo();
	}
	
	public static void updateVisualInDb(L2ItemInstance item, int visual, long limitedTime)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE items SET visual_item_id=?,visual_item_limitedTime=? " + "WHERE object_id = ?"))
		{
			ps.setInt(1, visual);
			ps.setLong(2, limitedTime);
			ps.setInt(3, item.getObjectId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			if (Config.DEBUG)
			{
				_log.config("Could not update dress me item in DB: Reason: " + e.getMessage());
			}
		}
	}
	
	public static Map<Integer, DressMeWeaponData> initWeaponMap(String type, Map<Integer, DressMeWeaponData> map, L2ItemInstance slot)
	{
		if (type.equals("SWORD") && (slot.getItem().getBodyPart() != L2Item.SLOT_LR_HAND))
		{
			return map = DressMeLoader.SWORD;
		}
		else if (type.equals("BLUNT") && (slot.getItem().getBodyPart() != L2Item.SLOT_LR_HAND))
		{
			return map = DressMeLoader.BLUNT;
		}
		else if (type.equals("BIGSWORD") && (slot.getItem().getBodyPart() == L2Item.SLOT_LR_HAND))
		{
			return map = DressMeLoader.BIGSWORD;
		}
		else if (type.equals("BIGBLUNT") && (slot.getItem().getBodyPart() == L2Item.SLOT_LR_HAND))
		{
			return map = DressMeLoader.BIGBLUNT;
		}
		else if (type.equals("DAGGER"))
		{
			return map = DressMeLoader.DAGGER;
		}
		else if (type.equals("BOW"))
		{
			return map = DressMeLoader.BOW;
		}
		else if (type.contains("POLE"))
		{
			return map = DressMeLoader.POLE;
		}
		else if (type.equals("FIST"))
		{
			return map = DressMeLoader.FIST;
		}
		else if (type.equals("DUAL"))
		{
			return map = DressMeLoader.DUAL;
		}
		else if (type.equals("DUALFIST"))
		{
			return map = DressMeLoader.DUALFIST;
		}
		else if (type.equals("FISHINGROD"))
		{
			return map = DressMeLoader.ROD;
		}
		else if (type.equals("CROSSBOW"))
		{
			return map = DressMeLoader.CROSSBOW;
		}
		else if (type.equals("RAPIER"))
		{
			return map = DressMeLoader.RAPIER;
		}
		else if (type.equals("ANCIENT_SWORD"))
		{
			return map = DressMeLoader.ANCIENTSWORD;
		}
		else if (type.equals("DUAL_DAGGER"))
		{
			return map = DressMeLoader.DUALDAGGER;
		}
		else
		{
			_log.config("Dress me system: Unknown weapon type: " + type);
			return null;
		}
	}
	
	public static Map<Integer, DressMeArmorData> initArmorMap(String type, Map<Integer, DressMeArmorData> map, L2ItemInstance slot)
	{
		if (type.equals("LIGHT") || (type.equals("Light")))
		{
			return map = DressMeLoader.LIGHT;
		}
		else if (type.equals("HEAVY") || (type.equals("Heavy")))
		{
			return map = DressMeLoader.HEAVY;
		}
		else if (type.equals("ROBE") || (type.equals("Magic")))
		{
			return map = DressMeLoader.ROBE;
		}
		else
		{
			_log.config("Dress me system: Unknown armor type: " + type);
			return null;
		}
	}
	
	public static Map<Integer, DressMeHatData> initHatMap(String type, Map<Integer, DressMeHatData> map, L2ItemInstance slot)
	{
		if ((slot.getLocationSlot() == 18) && (slot.getItem().getBodyPart() != L2Item.SLOT_HAIRALL))
		{
			return map = DressMeLoader.HAIR;
		}
		else if ((slot.getLocationSlot() == 18) && (slot.getItem().getBodyPart() == L2Item.SLOT_HAIRALL))
		{
			return map = DressMeLoader.HAIR_FULL;
		}
		else if (slot.getLocationSlot() == 20)
		{
			return map = DressMeLoader.HAIR2;
		}
		else
		{
			_log.config("Dress me system: Unknown hat slot: " + slot.getLocationSlot());
			return null;
		}
	}
}