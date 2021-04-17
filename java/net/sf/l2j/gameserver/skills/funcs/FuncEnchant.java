package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class FuncEnchant extends Func
{
public FuncEnchant(Stats pStat, int pOrder, Object owner, Lambda lambda)
{
	super(pStat, pOrder, owner);
}

@SuppressWarnings("incomplete-switch")
@Override
public void calc(Env env)
{
	if (cond != null && !cond.test(env))
		return;
	
	final L2ItemInstance item = (L2ItemInstance)funcOwner;
	
	int enchant = item.getEnchantLevel();
	
	int overenchant = 0;
	double superenchant = 0;
	
	if (enchant > 3)
	{
		if (enchant > 8 && item.isWeapon() && (item.isDread() || item.isCorrupted()))
		{
			double unique = Math.max(0.8, item.getItem().getUniqueness());
			
			
			if (item.isStandardShopItem())
			{
				superenchant = Math.pow(enchant - 8, Math.pow(1.04, unique));
			}
		}
		if (enchant > 10 && !(item.isWeapon() && (item.isDread() || item.isCorrupted())))
		{
			double unique = Math.max(0.8, item.getItem().getUniqueness());
			
			if (item.getCrystalType() == L2Item.CRYSTAL_S)
			{
				if (item.getItem().getUniqueness() == 1.5) //masterwork S
				{
					unique = 1.146;
				}
			}
			
			if (item.isStandardShopItem())
			{
				superenchant = Math.pow(enchant - 10, Math.pow(1.04, unique));
			}
			else
			{
				if (item.isRaidbossItem())
					unique -= 0.52;
				
				superenchant = Math.pow(enchant - 10, Math.pow(1.11, unique*1.24));
			}
			
			if (item.isJewelry())
			{
				if (unique >= 3)
					superenchant /= 1.45;
				else
					superenchant /= 1.13;
			}
			else if (item.isArmor())
			{
				 if (unique == 3)
					superenchant /= 1.09;
				else if (unique >= 2 && unique < 3)
					superenchant /= 1.075;
			}
			else if (item.isWeapon())
			{
				if (item.getName().contains("Vesper"))
					superenchant /= 1.04;
				else if (item.getName().contains("Dynasty"))
					superenchant /= 1.04;
			}
			
			overenchant = 7;
		}
		else
		{
			overenchant = enchant - 3;
		}
		
		enchant = 3;
	}
	
	if (env.player != null && env.player instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance) env.player;
		
		if (player.isInOlympiadMode() && Config.ALT_OLY_ENCHANT_LIMIT >= 0)
		{
			overenchant = Config.ALT_OLY_ENCHANT_LIMIT - 3;
			enchant = 3;
			superenchant = 0;
		}
		else if (player.isInFunEvent())
		{
			if (item.getCrystalType() <= L2Item.CRYSTAL_S && item.getUniqueness() < 3)
			{
				enchant = 3;
				overenchant = 7;
				
				if (item.getUniqueness() <= 1)
					superenchant = 15;
				else
					superenchant = 12;
				
				if (item.isWeapon())
					superenchant += 4;
			}
		}
		else if (player.isInSgradeZone())
		{
			if (item.getUniqueness() == 3 && !item.isStandardShopItem() && !item.isRaidbossItem())
			{
				if (item.getCrystalType() == L2Item.CRYSTAL_S)
				{
					superenchant = Math.min(superenchant, Math.pow(5, Math.pow(1.15, 3.5)));
				}
				else
				{
					superenchant = 0;
				}
			}
			else if (item.getUniqueness() == 2.5) //icarus
			{
				superenchant = Math.min(superenchant, Math.pow(8, Math.pow(1.05, 2.5)));
			}
			else if (item.getUniqueness() == 2) //dynasty
			{
				superenchant = Math.min(superenchant, Math.pow(10, Math.pow(1.05, 2)));
			}
		}
	}
	
	if (enchant <= 0)
		return;
	
	if (stat == Stats.MAGIC_DEFENCE)
	{
		env.value += enchant + 3 * overenchant + 4.8 * superenchant;
		return;
	}
	if (stat == Stats.POWER_DEFENCE)
	{
		if (item.getItemType() == L2ArmorType.MAGIC || item.getItemType() == L2ArmorType.SIGIL || item.getItem().getName().contains("Robe"))
			env.value += enchant + 2 * overenchant + 3.2 * superenchant;
		else
			env.value += enchant + 3 * overenchant + 4.8 * superenchant;
		
		return;
	}
	if (stat == Stats.MAGIC_ATTACK)
	{
		if (!item.isStandardShopItem())
			superenchant = Math.pow(superenchant, (14/(double)15));
		
		if (item.isWeapon())
		{
			final L2WeaponType type = (L2WeaponType)item.getItemType();
			
			switch (type)
			{
			case BOW:
			case CROSSBOW:
				env.value += 5 * enchant + 10 * overenchant + 15.98 * superenchant;
				return;
			case BIGSWORD:
			case ANCIENT_SWORD:
			case BIGBLUNT:
			case DUAL:
			case DUALFIST:
			case DUAL_DAGGER:
				env.value += 5.32 * enchant + 10.64 * overenchant + 17 * superenchant;
				return;
			}
		}
		
		env.value += 4 * enchant + 8 * overenchant + 12.8 * superenchant;
		return;
	}
	if (stat == Stats.RUN_SPEED || stat == Stats.VENGEANCE_SKILL_MAGIC_DAMAGE || stat == Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE || stat == Stats.TRANSFER_DAMAGE_PERCENT || stat == Stats.TRANSFER_DAMAGE_MANA)
	{
		if (overenchant > 500)
			overenchant = 500;
		
		env.value += 1 * enchant + 1 * overenchant;
		return;
	}
	if (stat == Stats.REFLECT_DAMAGE_PERCENT || stat == Stats.REFLECT_SKILL_MAGIC || stat == Stats.REFLECT_SKILL_PHYSIC)
	{
		env.value += 1 * enchant + 1 * overenchant;
		return;
	}
	if (stat == Stats.SHIELD_RATE)
	{
		env.value += 1.5 * enchant + 1.3 * overenchant;
		return;
	}
	if (stat == Stats.POWER_ATTACK_ANGLE || stat == Stats.SHIELD_DEFENCE_ANGLE || stat == Stats.CRIT_MAX_ADD || stat == Stats.MAGIC_ATTACK_RANGE)
	{
		env.value += 10 * enchant + 10 * overenchant;
		return;
	}
	if (stat == Stats.CRITICAL_DMG_ADD_BLEEDING)
	{
		env.value += 20 * enchant + 20 * overenchant;
		return;
	}
	if (stat == Stats.POWER_ATTACK_RANGE)
	{
		if (overenchant > 150)
			overenchant = 150;
		
		if (item.isWeapon())
		{
			if (item.getItemType() == L2WeaponType.BOW || item.getItemType() == L2WeaponType.CROSSBOW)
			{
				env.value += 15 * enchant + 15 * overenchant;
				return;
			}
			else if (item.getItemType() == L2WeaponType.POLE)
			{
				env.value += 3 * enchant + 3 * overenchant;
				return;
			}
		}
		
		env.value += 2 * enchant + 2 * overenchant;
		return;
	}
	if (stat == Stats.CRITICAL_RATE || stat == Stats.MCRITICAL_RATE)
	{
		env.value += (7 * enchant + 5.5 * overenchant);
		return;
	}
	if (stat == Stats.MAX_CP)
	{
		if (overenchant > 100)
			overenchant = 100;
		
		env.value *= 1 + (0.013 * enchant + 0.013 * overenchant);
		return;
	}
	if (stat == Stats.MAX_HP || stat == Stats.MAX_MP || stat == Stats.SKILL_DAM_MULTI || stat == Stats.SHIELD_DEFENCE)
	{
		if (overenchant > 100)
			overenchant = 100;
		
		env.value *= 1 + (0.012 * enchant + 0.012 * overenchant);
		return;
	}
	if (stat == Stats.FORCE_DAM)
	{
		env.value *= 1 + (0.015 * enchant + 0.015 * overenchant);
		return;
	}
	if (stat == Stats.POWER_ATTACK_SPEED || stat == Stats.MAGIC_ATTACK_SPEED)
	{
		if (overenchant > 100)
			overenchant = 100;
		
		env.value *= 1 + (0.0038 * enchant + 0.0038 * overenchant);
		return;
	}
	if (stat == Stats.CRITICAL_DAMAGE || stat == Stats.MAGIC_CRITICAL_DAMAGE)
	{
		if (overenchant > 100)
			overenchant = 100;
		
		env.value *= 1 + (0.009 * enchant + 0.009 * overenchant);
		return;
	}
	if (stat == Stats.HEAL_EFFECTIVNESS || stat == Stats.HEAL_PROFICIENCY)
	{
		env.value *= 1 + (0.015 * enchant + 0.015 * overenchant);
		return;
	}
	if (stat == Stats.EVASION_RATE || stat == Stats.ACCURACY_COMBAT || stat == Stats.ABSORB_CP_PERCENT)
	{
		env.value += 0.7 * enchant + 0.66 * overenchant;
		return;
	}
	if (stat == Stats.ABSORB_DAMAGE_PERCENT)
	{
		env.value += 0.6 * enchant + 0.6 * overenchant;
		return;
	}
	if (stat == Stats.HOLY_RES || stat == Stats.DARK_RES || stat == Stats.FIRE_RES || stat == Stats.WATER_RES ||
			stat == Stats.WIND_RES || stat == Stats.EARTH_RES)
	{
		env.value += 4 * enchant + 3.5 * overenchant;
		return;
	}
	if (stat == Stats.HOLY_POWER || stat == Stats.DARK_POWER || stat == Stats.FIRE_POWER || stat == Stats.WATER_POWER ||
			stat == Stats.WIND_POWER || stat == Stats.EARTH_POWER)
	{
		env.value += 6 * enchant + 5 * overenchant;
		return;
	}
	if (stat == Stats.P_SKILL_EVASION || stat == Stats.SKILL_CRITICAL_CHANCE_INCREASE || stat == Stats.CRIT_DAMAGE_EVASION || stat == Stats.M_SKILL_EVASION)
	{
		env.value += 0.85 * enchant + 0.85 * overenchant;
		return;
	}
	if (stat == Stats.CRIT_DAMAGE_EVASION)
	{
		env.value += 0.92 * enchant + 0.92 * overenchant;
		return;
	}
	if (stat == Stats.P_REUSE || stat == Stats.MAGIC_REUSE_RATE || stat == Stats.CRIT_VULN || stat == Stats.MAGIC_DAMAGE_VULN || stat == Stats.ATK_REUSE || stat == Stats.MP_CONSUME)
	{
		env.value -= (0.01 * enchant + 0.01 * overenchant);
		return;
	}
	if (stat == Stats.STAT_STR || stat == Stats.STAT_DEX || stat == Stats.STAT_CON || stat == Stats.STAT_INT || stat == Stats.STAT_WIT || stat == Stats.STAT_MEN || stat == Stats.SKILL_MASTERY)
	{
		env.value += 0.25 * enchant + 0.25 * overenchant;
		return;
	}
	if (stat == Stats.PERF_BLOCK_ADD)
	{
		env.value += 0.33 * enchant + 0.33 * overenchant;
		return;
	}
	if (stat == Stats.ATTACK_COUNT_MAX)
	{
		env.value += 0.15 * enchant + 0.15 * overenchant;
		return;
	}
	
	if (item.isWeapon())
	{
		final L2WeaponType type = (L2WeaponType)item.getItemType();

		double bow_default = 10;
		double cross_default = 8.49;
		double bigb_default = 6.5;
		double dual_default = 6;
		double default_val = 5;
		
		double over_bow_default = 17;
		double over_cross_default = 14.43;
		double over_bigb_default = 11.92;
		double over_dual_default = 11;
		double over_default = 10;
		
		double super_bow_default = 26.5;
		double super_cross_default = 22.92;
		double super_bigb_default = 19.07;
		double super_dual_default = 17.6;
		double super_default = 16;

		if (item.isTit())
		{
			bow_default += Config.titanium_default_bow_default;
			cross_default += Config.titanium_default_cross_default;
			bigb_default += Config.titanium_default_bigb_default;
			dual_default += Config.titanium_default_dual_default;
			default_val += Config.titanium_default_val;
			
			over_bow_default += Config.titanium_over_bow_default;
			over_cross_default += Config.titanium_over_cross_default;
			over_bigb_default += Config.titanium_over_bigb_default;
			over_dual_default += Config.titanium_over_dual_default;
			over_default += Config.titanium_over_default;
			
			super_bow_default += Config.titanium_super_bow_default;
			super_cross_default += Config.titanium_super_cross_default;
			super_bigb_default += Config.titanium_super_bigb_default;
			super_dual_default += Config.titanium_super_dual_default;
			super_default += Config.titanium_super_default;
		}
		if (item.isDread())
		{
			bow_default += Config.dread_default_bow_default;
			cross_default += Config.dread_default_cross_default;
			bigb_default += Config.dread_default_bigb_default;
			dual_default += Config.dread_default_dual_default;
			default_val += Config.dread_default_val;
			
			over_bow_default += Config.dread_over_bow_default;
			over_cross_default += Config.dread_over_cross_default;
			over_bigb_default += Config.dread_over_bigb_default;
			over_dual_default += Config.dread_over_dual_default;
			over_default += Config.dread_over_default;
			
			super_bow_default += Config.dread_super_bow_default;
			super_cross_default += Config.dread_super_cross_default;
			super_bigb_default += Config.dread_super_bigb_default;
			super_dual_default += Config.dread_super_dual_default;
			super_default += Config.dread_super_default;
		}
		if (item.isCorrupted())
		{
			bow_default += Config.corrupted_default_bow_default;
			cross_default += Config.corrupted_default_cross_default;
			bigb_default += Config.corrupted_default_bigb_default;
			dual_default += Config.corrupted_default_dual_default;
			default_val += Config.corrupted_default_val;
			
			over_bow_default += Config.corrupted_over_bow_default;
			over_cross_default += Config.corrupted_over_cross_default;
			over_bigb_default += Config.corrupted_over_bigb_default;
			over_dual_default += Config.corrupted_over_dual_default;
			over_default += Config.corrupted_over_default;
			
			super_bow_default += Config.corrupted_super_bow_default;
			super_cross_default += Config.corrupted_super_cross_default;
			super_bigb_default += Config.corrupted_super_bigb_default;
			super_dual_default += Config.corrupted_super_dual_default;
			super_default += Config.corrupted_super_default;
		}
		
		switch (type)
		{
		case BOW:
			env.value += bow_default * enchant + over_bow_default * overenchant + super_bow_default * superenchant;
			break;
		case CROSSBOW:
			env.value += cross_default * enchant + over_cross_default * overenchant + super_cross_default * superenchant;
			break;
		case BIGSWORD:
		case BIGBLUNT:
			env.value += bigb_default * enchant + over_bigb_default * overenchant + super_bigb_default * superenchant;
			break;
		case ANCIENT_SWORD:
		case DUAL:
		case DUALFIST:
		case DUAL_DAGGER:
			env.value += dual_default * enchant + over_dual_default * overenchant + super_dual_default * superenchant;
			break;
		default:
			env.value += default_val * enchant + over_default * overenchant + super_default * superenchant;
		}
	}
}
}
