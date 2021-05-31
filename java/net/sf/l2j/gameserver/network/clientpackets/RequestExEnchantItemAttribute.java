package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAttributeEnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class RequestExEnchantItemAttribute extends L2GameClientPacket
{
	private static final String	D0_38_REQUEST_EX_ENCHANT_ITEM_ATTRIBUTE	= "[C] D0 38 RequestExEnchantItemAttribute";
	private int					_objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = 0;
		try
		{
			_objectId = readD();
		}
		catch (Exception e)
		{}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null || _objectId == 0)
			return;
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.setActiveEnchantAttrItem(null);
			player.sendPacket(new SystemMessage(SystemMessageId.ELEMENTAL_ENHANCE_CANCELED));
			return;
		}
		if (player.isOnline() == 0)
		{
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if (player.isAccountLockedDown())
		{
			player.sendMessage("Your account is in lockdown");
			return;
		}
		if (player.isProcessingTransaction())
		{
			player.setActiveEnchantAttrItem(null);
			player.sendMessage("Enchanting items is not allowed during a trade.");
			return;
		}
		if (player.getPrivateStoreType() != 0 || player.isInStoreMode())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_PRIVATE_STORE_OR_WORKSHOP));
			player.setActiveEnchantAttrItem(null);
			player.cancelActiveTrade();
			return;
		}
		if (player.isEnchanting())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ENCHANTMENT_ALREADY_IN_PROGRESS));
			player.setActiveEnchantAttrItem(null);
			return;
		}
		/*
		 * // Restrict enchant during a trade (bug if enchant fails) if (player.getActiveRequester()
		 * != null) { // Cancel trade player.cancelActiveTrade();
		 * player.setActiveEnchantAttrItem(null);
		 * player.sendMessage("Enchanting items is not allowed during a trade."); return; }
		 */
		final L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		final L2ItemInstance stone = player.getActiveEnchantAttrItem();
		if (item == null || stone == null)
		{
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if ((item.getItemLocation() != L2ItemInstance.ItemLocation.INVENTORY) && (item.getItemLocation() != L2ItemInstance.ItemLocation.PAPERDOLL))
		{
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if (item.isWear())
		{
			player.setActiveEnchantAttrItem(null);
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to enchant a weared Item", Config.DEFAULT_PUNISH);
			return;
		}
		// can't enchant rods, shadow items, adventurers', hero items
		if (item.getItem().getItemType() == L2WeaponType.ROD || item.isShadowItem()
		/* || item.isHeroItem() */ /* || item.isTimeLimitedItem() */ || item.isEtcItem() || item.isWear() || item.getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || item.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET || (item.getItemId() >= 7816 && item.getItemId() <= 7831) || (item.getItem().getItemType() == L2WeaponType.NONE) || (item.getItem().getItemGradeSPlus() < L2Item.CRYSTAL_D && item.getItemId() != 20325)
		/* || item.getItem().getBodyPart() == L2Item.SLOT_BACK */)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ELEMENTAL_ENHANCE_REQUIREMENT_NOT_SUFFICIENT));
			player.setActiveEnchantAttrItem(null);
			return;
		}
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.setActiveEnchantAttrItem(null);
					return;
				}
				break;
			}
			default:
			{
				player.setActiveEnchantAttrItem(null);
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to use enchant Exploit!", Config.DEFAULT_PUNISH);
				return;
			}
		}
		final int stoneId = stone.getItemId();
		final Elementals oldElement = item.getElementals();
		/*
		 * if (oldElement != null)
		 * {
		 * if (item.getItem() instanceof L2Weapon)
		 * {
		 * if (oldElement.getValue() >= 150)
		 * {
		 * player.sendMessage("Max elemental level reached");
		 * return;
		 * }
		 * }
		 * else if (oldElement.getValue() >= 60)
		 * {
		 * player.sendMessage("Max elemental level reached");
		 * return;
		 * }
		 * }
		 */
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(stoneId, item);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		byte elementToAdd = getElementFromItemId(stoneId);
		// Armors have the opposite element
		if (item.isArmor() && item.getItemId() != 14164)
			elementToAdd = Elementals.getOppositeElement(elementToAdd);
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		if (oldElement != null && oldElement.getElement() != elementToAdd && oldElement.getElement() != -2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ANOTHER_ELEMENTAL_POWER_ALREADY_ADDED));
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if (powerToAdd <= 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ELEMENTAL_ENHANCE_CANCELED));
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if (!player.destroyItem("AttrEnchant", stone, 1, player, true))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to attribute enchant with a stone he doesn't have", Config.DEFAULT_PUNISH);
			player.setActiveEnchantAttrItem(null);
			return;
		}
		if (!stone.isTradeable() && stone.getUntradeableTime() > System.currentTimeMillis())
		{
			if (item.getUntradeableTime() < stone.getUntradeableTime())
			{
				item.setUntradeableTimer(stone.getUntradeableTime());
				player.sendMessage("Your " + item.getName() + " is now untradeable for " + stone.getUntradeableTime() / 3600000 + "" + " hours due to your element stone being temporarily untradeable");
			}
		}
		int chance = Elementals.ENCHANT_CHANCE;
		switch ((int) item.getItem().getUniqueness())
		{
			case 0:
				break;
			case 1:
				break;
			case 2:
				break;
			case 3:
				chance -= 3;
				break;
			case 4:
				if (item.isDread())
					chance -= 12;
				else if (!item.isStandardShopItem())
					chance -= 14;
				else
					chance -= 6;
				break;
			case 5:
				chance -= 20;
				break;
		}
		if (!item.isStandardShopItem())
			chance -= 5;
		if (item.isHeroItem())
		{
			if (stone.getItemName().contains("Stone"))
				chance = 100;
			else
				chance = 70;
		}
		if (item.getItemId() == 14164 || item.getItemId() == 14163 || item.getItemId() == 14165 || item.getItemId() == 20325) // vesper necklace and plastic hair
		{
			if (stone.getItemName().contains("Stone"))
				chance = 10;
			else
				chance = 100;
		}
		else if (item.getItem().getBodyPart() == L2Item.SLOT_BACK)
		{
			if (stone.getItemName().contains("Stone"))
				chance = 9;
			else
				chance = 70;
		}
		if (Rnd.get(100) < chance)
		{
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				sm = new SystemMessage(SystemMessageId.ELEMENTAL_POWER_S2_SUCCESSFULLY_ADDED_TO_S1).addItemName(item).addNumber(powerToAdd);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.ELEMENTAL_POWER_S3_SUCCESSFULLY_ADDED_TO_S1_S2).addNumber(item.getEnchantLevel());
				sm.addItemName(item).addNumber(powerToAdd);
			}
			player.sendPacket(sm);
			item.setElementAttr(elementToAdd, newPower);
			if (item.isEquipped())
				item.updateElementAttrBonus(player);
			// send packets
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_ADDING_ELEMENTAL_POWER));
		}
		player.sendPacket(new ExAttributeEnchantResult(powerToAdd));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		player.setActiveEnchantAttrItem(null);
	}
	
	public byte getElementFromItemId(int itemId)
	{
		byte element = 0;
		for (int id : Elementals.STONES)
		{
			if (id == itemId)
				return element;
			element++;
		}
		element = 0;
		for (int id : Elementals.CRYSTALS)
		{
			if (id == itemId)
				return element;
			element++;
		}
		element = 0;
		for (int id : Elementals.JEWELS)
		{
			if (id == itemId)
				return element;
			element++;
		}
		element = 0;
		for (int id : Elementals.ENERGIES)
		{
			if (id == itemId)
				return element;
			element++;
		}
		return -1;
	}
	
	public int getLimit(int itemId, L2ItemInstance item)
	{
		if (item.getItemId() == 14164) // vesper necklace
		{
			return 30;
		}
		else if (item.getItem().getBodyPart() == L2Item.SLOT_BACK) // cloaks
		{
			return 30;
		}
		else if (item.getItemId() == 14163 || item.getItemId() == 14165) // vesper earring and ring
		{
			return 15;
		}
		for (int id : Elementals.STONES)
		{
			if (id == itemId)
			{
				if (item.isWeapon())
					return Elementals.WEAPON_VALUES[3];
				return Elementals.ARMOR_VALUES[3];
			}
		}
		for (int id : Elementals.CRYSTALS)
		{
			if (id == itemId)
			{
				if (item.isWeapon())
					return Elementals.WEAPON_VALUES[6];
				return Elementals.ARMOR_VALUES[6];
			}
		}
		for (int id : Elementals.JEWELS)
		{
			if (id == itemId)
			{
				if (item.isWeapon())
					return Elementals.WEAPON_VALUES[9];
				return Elementals.ARMOR_VALUES[9];
			}
		}
		for (int id : Elementals.ENERGIES)
		{
			if (id == itemId)
			{
				if (item.isWeapon())
					return Elementals.WEAPON_VALUES[10]; // Should be 12
				return Elementals.ARMOR_VALUES[10]; //
			}
		}
		return 0;
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, L2ItemInstance item)
	{
		if (item.getItemId() == 14164) // vesper necklace
		{
			return 2;
		}
		else if (item.getItemId() == 14163 || item.getItemId() == 14165) // vesper earring and ring
		{
			return 1;
		}
		else if (item.getItem().getBodyPart() == L2Item.SLOT_BACK)
		{
			return 2;
		}
		boolean stone = false, crystal = false;
		// boolean jewel = false, energy = false;
		for (int id : Elementals.STONES)
		{
			if (id == stoneId)
			{
				stone = true;
				break;
			}
		}
		if (!stone)
		{
			for (int id : Elementals.CRYSTALS)
			{
				if (id == stoneId)
				{
					crystal = true;
					break;
				}
			}
			if (!crystal)
			{
				for (int id : Elementals.JEWELS)
				{
					if (id == stoneId)
					{
						// jewel = true;
						break;
					}
				}
				// if (!jewel)
				// energy = true;
			}
		}
		if (stone || crystal)
		{
			if (item.isWeapon())
			{
				if (oldValue == 0)
					return Elementals.FIRST_WEAPON_BONUS;
				else
					return Elementals.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
				return Elementals.ARMOR_BONUS;
		}
		// Others not implemented
		return 0;
	}
	
	@Override
	public String getType()
	{
		return D0_38_REQUEST_EX_ENCHANT_ITEM_ATTRIBUTE;
	}
}
