package net.sf.l2j.gameserver.model;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.templates.item.L2Item;

public final class L2ArmorSet
{
	public final int		_chest;
	public final int		_legs;
	public final int		_head;
	public final int		_gloves;
	public final int		_feet;
	public final int		_mw_legs;
	public final int		_mw_head;
	public final int		_mw_gloves;
	public final int		_mw_feet;
	public final int		_mw_chest;
	private final String[]	_skills;
	private final int		_shield;
	private final int		_mw_shield;
	private final int		_shieldSkillId;
	private final int		_enchant16Skill;
	
	public L2ArmorSet(int chest, int legs, int head, int gloves, int feet, String[] skills, int shield, int shield_skill_id, int enchant16skill, int mw_legs, int mw_head, int mw_gloves, int mw_feet, int mw_shield, int mw_chest)
	{
		_chest = chest;
		_legs = legs;
		_head = head;
		_gloves = gloves;
		_feet = feet;
		_mw_legs = mw_legs;
		_mw_head = mw_head;
		_mw_gloves = mw_gloves;
		_mw_feet = mw_feet;
		_mw_shield = mw_shield;
		_mw_chest = mw_chest;
		_skills = skills;
		_shield = shield;
		_shieldSkillId = shield_skill_id;
		switch (enchant16skill)
		{
			case 3614:
			case 3617:
			case 3620:
				enchant16skill = 3623;
				break;
			case 3615:
			case 3618:
			case 3621:
				enchant16skill = 3624;
				break;
			case 3616:
			case 3619:
			case 3622:
				enchant16skill = 3625;
				break;
		}
		_enchant16Skill = enchant16skill;
	}
	
	/**
	 * Checks if player have equiped all items from set (not checking shield)
	 * 
	 * @param player
	 *            whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(L2PcInstance player)
	{
		final Inventory inv = player.getInventory();
		final L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		int chest = 0;
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		if (chestItem != null)
			chest = chestItem.getItemId();
		if (legsItem != null)
			legs = legsItem.getItemId();
		if (headItem != null)
			head = headItem.getItemId();
		if (glovesItem != null)
			gloves = glovesItem.getItemId();
		if (feetItem != null)
			feet = feetItem.getItemId();
		return containAll(chest, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if (_chest != 0 && _chest != chest && (_mw_chest == 0 || _mw_chest != chest))
			return false;
		if (_legs != 0 && _legs != legs && (_mw_legs == 0 || _mw_legs != legs))
			return false;
		if (_head != 0 && _head != head && (_mw_head == 0 || _mw_head != head))
			return false;
		if (_gloves != 0 && _gloves != gloves && (_mw_gloves == 0 || _mw_gloves != gloves))
			return false;
		if (_feet != 0 && _feet != feet && (_mw_feet == 0 || _mw_feet != feet))
			return false;
		return true;
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return (_chest == itemId || _mw_chest == itemId);
			case Inventory.PAPERDOLL_LEGS:
				return (_legs == itemId || _mw_legs == itemId);
			case Inventory.PAPERDOLL_HEAD:
				return (_head == itemId || _mw_head == itemId);
			case Inventory.PAPERDOLL_GLOVES:
				return (_gloves == itemId || _mw_gloves == itemId);
			case Inventory.PAPERDOLL_FEET:
				return (_feet == itemId || _mw_feet == itemId);
			default:
				return false;
		}
	}
	
	public byte containItemArmorExchanger(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				if (_chest == itemId)
					return 1;
				if (_mw_chest == itemId)
					return 2;
			case Inventory.PAPERDOLL_LEGS:
				if (_legs == itemId)
					return 1;
				if (_mw_legs == itemId)
					return 2;
			case Inventory.PAPERDOLL_HEAD:
				if (_head == itemId)
					return 1;
				if (_mw_head == itemId)
					return 2;
			case Inventory.PAPERDOLL_GLOVES:
				if (_gloves == itemId)
					return 1;
				if (_mw_gloves == itemId)
					return 2;
			case Inventory.PAPERDOLL_FEET:
				if (_feet == itemId)
					return 1;
				if (_mw_feet == itemId)
					return 2;
			default:
				return 0;
		}
	}
	
	public String[] getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (shieldItem != null && (shieldItem.getItemId() == _shield || shieldItem.getItemId() == _mw_shield))
			return true;
		return false;
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield == 0)
			return false;
		return (_shield == shield_id || _mw_shield == shield_id);
	}
	
	public int getShieldSkillId()
	{
		return _shieldSkillId;
	}
	
	public int getEnchant16skillId()
	{
		return _enchant16Skill;
	}
	
	/**
	 * Checks if all parts of set are enchanted to +11 or more
	 * 
	 * @param player
	 * @return
	 */
	public boolean isEnchanted16(L2PcInstance player)
	{
		// Player don't have full set
		if (!containAll(player))
			return false;
		if (player.isInOlympiadMode())
			return false;
		final Inventory inv = player.getInventory();
		final L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		/*
		 * if (chestItem != null)
		 * {
		 * switch (chestItem.getCrystalType())
		 * {
		 * case L2Item.CRYSTAL_S84:
		 * if (chestItem.getUniqueness() >= 4.5)
		 * yeah = 8;
		 * else
		 * yeah = 12;
		 * break;
		 * case L2Item.CRYSTAL_S80:
		 * yeah = 16;
		 * break;
		 * default:
		 * yeah = 16;
		 * break;
		 * }
		 * }
		 */
		int yeah = 0;
		if (chestItem != null)
		{
			if (chestItem.getUniqueness() >= 2)
			{
				yeah = Config.ENCHANT_BONUS_TIER_2;
			}
			if (chestItem.getUniqueness() >= 2.5)
			{
				yeah = Config.ENCHANT_BONUS_TIER_2_5;
			}
			if (chestItem.getUniqueness() >= 3)
			{
				yeah = Config.ENCHANT_BONUS_TIER_3;
			}
			if (chestItem.getUniqueness() >= 4)
			{
				yeah = Config.ENCHANT_BONUS_TIER_4;
			}
			if (chestItem.getUniqueness() >= 4.5)
			{
				yeah = Config.ENCHANT_BONUS_TIER_4_5;
			}
		}
		else
			return false;
		final L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (chestItem.getEnchantLevel() < yeah)
			return false;
		if (chestItem.getUniqueness() == 2.5)
		{
			if (_legs != 0 && (legsItem == null || legsItem.getEnchantLevel() < 20))
				return false;
			if (_gloves != 0 && (glovesItem == null || glovesItem.getEnchantLevel() < 20))
				return false;
			if (_head != 0 && (headItem == null || headItem.getEnchantLevel() < 20))
				return false;
			if (_feet != 0 && (feetItem == null || feetItem.getEnchantLevel() < 20))
				return false;
		}
		else if (chestItem.getUniqueness() != 2.5)
		{
			if (_legs != 0 && (legsItem == null || legsItem.getEnchantLevel() < yeah))
				return false;
			if (_gloves != 0 && (glovesItem == null || glovesItem.getEnchantLevel() < yeah))
				return false;
			if (_head != 0 && (headItem == null || headItem.getEnchantLevel() < yeah))
				return false;
			if (_feet != 0 && (feetItem == null || feetItem.getEnchantLevel() < yeah))
				return false;
		}
		return true;
	}
	
	public boolean isEnchantedForVitalityGlow(L2PcInstance player)
	{
		if (!containAll(player))
			return false;
		if (player.isInOlympiadMode())
			return false;
		final Inventory inv = player.getInventory();
		final L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final int yeah;
		if (chestItem != null)
		{
			yeah = chestItem.getItem().getClutchEnchantLevel();
		}
		else
			return false;
		final L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (chestItem.getEnchantLevel() < yeah)
			return false;
		if (chestItem.getUniqueness() == 2.5)
		{
			if (_legs != 0 && (legsItem == null || legsItem.getEnchantLevel() < 20))
				return false;
			if (_gloves != 0 && (glovesItem == null || glovesItem.getEnchantLevel() < 20))
				return false;
			if (_head != 0 && (headItem == null || headItem.getEnchantLevel() < 20))
				return false;
			if (_feet != 0 && (feetItem == null || feetItem.getEnchantLevel() < 20))
				return false;
		}
		else if (chestItem.getUniqueness() != 2.5)
		{
			if (_legs != 0 && (legsItem == null || legsItem.getEnchantLevel() < yeah))
				return false;
			if (_gloves != 0 && (glovesItem == null || glovesItem.getEnchantLevel() < yeah))
				return false;
			if (_head != 0 && (headItem == null || headItem.getEnchantLevel() < yeah))
				return false;
			if (_feet != 0 && (feetItem == null || feetItem.getEnchantLevel() < yeah))
				return false;
		}
		return true;
	}
	
	public boolean isEnchantedForEpicSkill(L2PcInstance player) // epic skills
	{
		// Player don't have full set
		if (!containAll(player) || player.isInSgradeZone())
			return false;
		if (player.isInOlympiadMode())
			return false;
		final Inventory inv = player.getInventory();
		final L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chestItem == null)
			return false;
		final int yeah;
		switch (chestItem.getCrystalType())
		{
			case L2Item.CRYSTAL_S84:
				return false;
			case L2Item.CRYSTAL_S80:
				if (chestItem.getUniqueness() == 2.5)
					yeah = 20;
				else
					yeah = 21;
				break;
			default:
				yeah = 25;
				break;
		}
		final L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if (chestItem.getEnchantLevel() < yeah)
			return false;
		if (_legs != 0 && (legsItem == null || legsItem.getEnchantLevel() < yeah))
			return false;
		if (_gloves != 0 && (glovesItem == null || glovesItem.getEnchantLevel() < yeah))
			return false;
		if (_head != 0 && (headItem == null || headItem.getEnchantLevel() < yeah))
			return false;
		if (_feet != 0 && (feetItem == null || feetItem.getEnchantLevel() < yeah))
			return false;
		return true;
	}
	
	public static int getHighEnchantSkillId(L2PcInstance player)
	{
		final Inventory inv = player.getInventory();
		final L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chestItem == null)
			return 0;
		switch (chestItem.getCrystalType())
		{
			case L2Item.CRYSTAL_S80:
			{
				if (player.isWearingHeavyArmor())
					return 3611;
				if (player.isWearingLightArmor())
					return 3612;
				if (player.isWearingMagicArmor())
					return 3613;
				break;
			}
			case L2Item.CRYSTAL_S:
			{
				if (player.isWearingHeavyArmor())
					return 3614;
				if (player.isWearingLightArmor())
					return 3615;
				if (player.isWearingMagicArmor())
					return 3616;
				break;
			}
		}
		return 0;
	}
	
	public int getItemIdBySlot(Integer slot, boolean masterwork)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				if (masterwork)
					return _mw_chest;
				else
					return _chest;
			case Inventory.PAPERDOLL_LEGS:
				if (masterwork)
					return _mw_legs;
				else
					return _legs;
			case Inventory.PAPERDOLL_HEAD:
				if (masterwork)
					return _mw_head;
				else
					return _head;
			case Inventory.PAPERDOLL_GLOVES:
				if (masterwork)
					return _mw_gloves;
				else
					return _gloves;
			case Inventory.PAPERDOLL_FEET:
				if (masterwork)
					return _mw_feet;
				else
					return _feet;
			default:
				return 0;
		}
	}
	
	public int[] getAllBaseParts()
	{
		return new int[]
		{
			_head, _chest, _legs, _gloves, _feet
		};
	}
}