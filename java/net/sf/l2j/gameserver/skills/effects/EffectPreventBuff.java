package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public final class EffectPreventBuff extends L2Effect
{
	public EffectPreventBuff(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PREVENT_BUFF;
	}
	
	@Override
	public boolean onStart()
	{
		super.onStart();
		getEffected().setPreventedFromReceivingBuffs(true);
		if (getEffected() instanceof L2PcInstance)
			getEffected().sendMessage(getSkill().getName() + " protects you from receiving any effects.");
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().setPreventedFromReceivingBuffs(false);
		super.onExit();
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}