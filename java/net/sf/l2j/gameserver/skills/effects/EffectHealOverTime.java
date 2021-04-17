package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExRegMax;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectHealOverTime extends L2Effect
{
public EffectHealOverTime(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
 */
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.HEAL_OVER_TIME;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	if (getEffected() instanceof L2PcInstance)
		getEffected().sendPacket(new ExRegMax(calc(), getTotalCount() * getPeriod(), getPeriod()));
	super.onStart();
	
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	if (getEffected().isDead())
	{
		if (getSkill().isPositive())
			return true; //special edit here to prevent toggles from disabling upon death
		else return false;
	}
	
	if (getEffected() instanceof L2DoorInstance)
		return false;
	
	double hp = getEffected().getCurrentHp();
	double maxhp = getEffected().getMaxHp();
	int healed = (int) calc();
	
	if (getEffected() instanceof L2PcInstance && getEffected().getActingPlayer().isInOlympiadMode())
		healed /= 3;
	hp += healed;
	if (hp > maxhp)
	{
		hp = maxhp;
	}
	getEffected().setCurrentHp(hp);
	StatusUpdate suhp = new StatusUpdate(getEffected().getObjectId());
	suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
	getEffected().sendPacket(suhp);
	return true;
}
}
