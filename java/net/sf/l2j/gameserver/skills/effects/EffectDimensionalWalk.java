package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectDimensionalWalk extends L2Effect
{
final public static int DIMENSION_INSTANCE_ID = 2;
public int originalInstance = 0;
public EffectDimensionalWalk(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public boolean onStart()
{
	if (getEffected() instanceof L2PcInstance)
	{
		final L2PcInstance effected = (L2PcInstance)getEffected();
		
		if (effected.getInstanceId() == DIMENSION_INSTANCE_ID)
			return false;
		
		super.onStart();
		
		originalInstance = effected.getInstanceId();
		
		effected.setInstanceId(DIMENSION_INSTANCE_ID);
		effected.teleToLocation(effected.getX(), effected.getY(), effected.getZ(), effected.getHeading(), false);
	}
	
	return true;
}

@Override
public void onExit()
{
	if (getEffected() instanceof L2PcInstance)
	{
		final L2PcInstance effected = (L2PcInstance)getEffected();
		
		super.onExit();
		
		if (effected.getPet() != null)
			effected.getPet().updateAbnormalEffect();
		
		effected.setInstanceId(originalInstance);
		effected.setForceNoSpawnProtection(true);
		effected.teleToLocation(effected.getX(), effected.getY(), effected.getZ(), effected.getHeading(), false);
	}
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.DIMENSIONALWALK;
}

@Override
public boolean onActionTime()
{
	return false;
}
}