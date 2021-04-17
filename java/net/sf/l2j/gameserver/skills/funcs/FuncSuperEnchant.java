package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class FuncSuperEnchant extends Func
{
public FuncSuperEnchant(Stats pStat, int pOrder, Object owner, Lambda lambda)
{
	super(pStat, pOrder, owner);
}

@Override
public void calc(Env env)
{
	if (cond != null && !cond.test(env))
		return;
	
	final L2ItemInstance item = (L2ItemInstance)funcOwner;
	
	int enchant = item.getEnchantLevel();
	
	if (enchant <= 0 || enchant < item.getSuperEnchantLevel())
		return;
	
	if (env.player != null && env.player instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance) env.player;
		
		if (player.isInOlympiadMode() && Config.ALT_OLY_ENCHANT_LIMIT >= 0)
		{
			return;
		}
		else if (player._inEventDM && (DM._started || NewDM._started))
		{
			return;
		}
	}
	
	enchant -= item.getSuperEnchantLevel();
	
	if (stat == Stats.MAGIC_DEFENCE)
	{
		env.value += enchant;
		return;
	}
	
	if (stat == Stats.POWER_DEFENCE)
	{
		if (item.getItemType() == L2ArmorType.MAGIC || item.getItemType() == L2ArmorType.SIGIL || item.getItem().getName().contains("Robe"))
			env.value += enchant;
		else
			env.value += enchant ;
		
		return;
	}
	
	if (stat == Stats.MAGIC_ATTACK)
	{
		env.value += 4 * enchant ;
		return;
	}
	
	if (stat == Stats.RUN_SPEED || stat == Stats.VENGEANCE_SKILL_MAGIC_DAMAGE || stat == Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE || stat == Stats.TRANSFER_DAMAGE_PERCENT)
	{
		env.value += 1 * enchant ;
		return;
	}
	if (stat == Stats.REFLECT_DAMAGE_PERCENT || stat == Stats.REFLECT_SKILL_MAGIC || stat == Stats.REFLECT_SKILL_PHYSIC)
	{
		env.value += 1 * enchant ;
		return;
	}
	if (stat == Stats.SHIELD_RATE)
	{
		env.value += 1.5 * enchant ;
		return;
	}
	if (stat == Stats.POWER_ATTACK_ANGLE || stat == Stats.SHIELD_DEFENCE_ANGLE || stat == Stats.CRIT_MAX_ADD || stat == Stats.MAGIC_ATTACK_RANGE)
	{
		env.value += 10 * enchant ;
		return;
	}
	if (stat == Stats.CRITICAL_DMG_ADD_BLEEDING)
	{
		env.value += 20 * enchant ;
		return;
	}
	if (stat == Stats.POWER_ATTACK_RANGE)
	{
		if (item.isWeapon())
		{
			if (item.getItemType() == L2WeaponType.BOW || item.getItemType() == L2WeaponType.CROSSBOW)
			{
				env.value += 15 * enchant ;
				return;
			}
			else if (item.getItemType() == L2WeaponType.POLE)
			{
				env.value += 3 * enchant;
				return;
			}
		}
		
		env.value += 2 * enchant;
		return;
	}
	if (stat == Stats.CRITICAL_RATE || stat == Stats.MCRITICAL_RATE)
	{
		env.value += (7 * enchant);
		return;
	}
	if (stat == Stats.MAX_CP)
	{
		
		env.value *= 1 + (0.017 * enchant );
		return;
	}
	if (stat == Stats.MAX_HP || stat == Stats.MAX_MP || stat == Stats.SKILL_DAM_MULTI || stat == Stats.SHIELD_DEFENCE)
	{
		
		env.value *= 1 + (0.015 * enchant );
		return;
	}
	if (stat == Stats.FORCE_DAM)
	{
		env.value *= 1 + (0.015 * enchant );
		return;
	}
	if (stat == Stats.POWER_ATTACK_SPEED || stat == Stats.MAGIC_ATTACK_SPEED || stat == Stats.CRITICAL_DAMAGE || stat == Stats.MAGIC_CRITICAL_DAMAGE)
	{
		
		env.value *= 1 + (0.01 * enchant );
		return;
	}
	if (stat == Stats.HEAL_EFFECTIVNESS || stat == Stats.HEAL_PROFICIENCY)
	{
		env.value *= 1 + (0.015 * enchant );
		return;
	}
	if (stat == Stats.EVASION_RATE || stat == Stats.ACCURACY_COMBAT || stat == Stats.ABSORB_CP_PERCENT)
	{
		env.value += 0.7 * enchant;
		return;
	}
	if (stat == Stats.ABSORB_DAMAGE_PERCENT)
	{
		env.value += 0.6 * enchant ;
		return;
	}
	if (stat == Stats.HOLY_RES || stat == Stats.DARK_RES || stat == Stats.FIRE_RES || stat == Stats.WATER_RES ||
			stat == Stats.WIND_RES || stat == Stats.EARTH_RES)
	{
		env.value += 4 * enchant ;
		return;
	}
	if (stat == Stats.HOLY_POWER || stat == Stats.DARK_POWER || stat == Stats.FIRE_POWER || stat == Stats.WATER_POWER ||
			stat == Stats.WIND_POWER || stat == Stats.EARTH_POWER)
	{
		env.value += 6 * enchant ;
		return;
	}
	if (stat == Stats.P_SKILL_EVASION || stat == Stats.SKILL_CRITICAL_CHANCE_INCREASE || stat == Stats.CRIT_DAMAGE_EVASION || stat == Stats.M_SKILL_EVASION)
	{
		env.value += 0.85 * enchant ;
		return;
	}
	if (stat == Stats.CRIT_DAMAGE_EVASION)
	{
		env.value += 0.92 * enchant ;
		return;
	}
	if (stat == Stats.P_REUSE || stat == Stats.MAGIC_REUSE_RATE || stat == Stats.CRIT_VULN || stat == Stats.MAGIC_DAMAGE_VULN || stat == Stats.ATK_REUSE || stat == Stats.MP_CONSUME)
	{
		env.value -= (0.01 * enchant );
		return;
	}
	if (stat == Stats.STAT_STR || stat == Stats.STAT_DEX || stat == Stats.STAT_CON || stat == Stats.STAT_INT || stat == Stats.STAT_WIT || stat == Stats.STAT_MEN || stat == Stats.SKILL_MASTERY)
	{
		env.value += 0.25 * enchant ;
		return;
	}
	if (stat == Stats.PERF_BLOCK_ADD)
	{
		env.value += 0.33 * enchant ;
		return;
	}
	if (stat == Stats.ATTACK_COUNT_MAX)
	{
		env.value += 0.15 * enchant;
		return;
	}
	
	if (item.isWeapon())
	{
		final L2WeaponType type = (L2WeaponType)item.getItemType();
		
		switch (type)
		{
		case BOW:
		case CROSSBOW:
			env.value += 10 * enchant ;
			break;
		case BIGSWORD:
		case ANCIENT_SWORD:
		case BIGBLUNT:
		case DUAL:
		case DUALFIST:
		case DUAL_DAGGER:
			env.value += 6 * enchant ;
			break;
		default:
			env.value += 5 * enchant ;
		}
	}
}
}
