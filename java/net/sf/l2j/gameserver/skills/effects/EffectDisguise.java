package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectDisguise extends L2Effect
{
public EffectDisguise(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.DISGUISE;
}

@Override
public boolean onActionTime()
{
	return false;
}

@Override
public boolean onStart()
{
	if (getEffected() instanceof L2PcInstance)
	{
		getEffected().getActingPlayer().setDisguised(true);
		getEffected().getActingPlayer().broadcastUserInfo();
		
		if (!getEffected().getActingPlayer().getAppearance().getSex() && getEffected().getActingPlayer().isSpawned())
		{
			for (L2PcInstance nigga : getEffected().getKnownList().getKnownPlayers().values())
			{
				if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(getEffected()))
				{
					nigga.getKnownList().removeKnownObject(getEffected());
					nigga.getKnownList().addKnownObject(getEffected());
				}
			}
		}
	}
	return super.onStart();
}
@Override
public void onExit()
{
	if (getEffected() instanceof L2PcInstance)
	{
		getEffected().getActingPlayer().setDisguised(false);
		getEffected().getActingPlayer().broadcastUserInfo();
		
		if (!getEffected().getActingPlayer().getAppearance().getSex() && getEffected().getActingPlayer().isSpawned())
		{
			for (L2PcInstance nigga : getEffected().getKnownList().getKnownPlayers().values())
			{
				if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(getEffected()))
				{
					nigga.getKnownList().removeKnownObject(getEffected());
					nigga.getKnownList().addKnownObject(getEffected());
				}
			}
		}
	}
	super.onExit();
}
}
