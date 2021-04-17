package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Rnd;

public class EffectCleanse extends L2Effect
{
	public EffectCleanse(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		super(env, template, ignoreBoost);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CLEANSE;
	}
	
	@Override
	public boolean onStart()
	{
		if (Rnd.get(100) > 20)
		{
			for (L2Effect e : getEffected().getAllEffects())
			{
				if (e != null && e.getSkill().isDebuff())
					e.exit();
			}
		}
		
		return false;
	}
	
	@Override
	public void onExit()
	{
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}