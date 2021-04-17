package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectStoneSkin extends L2Effect
{
int _currDmg = 0;

public EffectStoneSkin(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.STONESKIN;
}

public int addDamage(int damage)
{
	_currDmg += damage;
	
	if (_currDmg >= getEffectPower())
	{
		final int dmg = (int) (_currDmg - getEffectPower());
		exit();
		return dmg;
	}
	
	return 0;
}

@Override
public boolean onActionTime()
{
	return false;
}
}
