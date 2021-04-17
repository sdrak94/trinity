package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectNeutralBuff extends L2Effect
{
public EffectNeutralBuff(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.NEUTRALBUFF;
}
@Override
public boolean onActionTime()
{
	return false;
}
}
