package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.datatables.EnchantHPBonusData;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;

public class FuncEnchantHp extends Func
{
public FuncEnchantHp(Stats pStat, int pOrder, Object owner, Lambda lambda)
{
	super(pStat, pOrder, owner);
}

@Override
public void calc(Env env)
{
	if (cond != null && !cond.test(env))
		return;
	
	if (env.player instanceof L2PcInstance && env.player.getActingPlayer().isInOlympiadMode())
		return;
	
	final L2ItemInstance item = (L2ItemInstance) funcOwner;
	
	int lvl = 6;
	
	if (item.getUniqueness() == 4)
	{
		lvl = 4;
	}
	else if (item.getUniqueness() == 4.5)
	{
		lvl = 1;
	}
	
	if (item.getEnchantLevel() > lvl)
		env.value += EnchantHPBonusData.getInstance().getHPBonus(item);
}
}
