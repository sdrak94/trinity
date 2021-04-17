package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;


public class EffectPull extends L2Effect
{
private L2Character _actor;
private L2Character _target;

public EffectPull(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.THROW_UP;
}

@Override
public boolean onStart()
{
	if (getEffected() == null || getEffector() == null)
		return false;
	
	if (getEffected() == getEffector())
		return false;
	
	if (getEffected() instanceof L2RaidBossInstance || getEffected().isRooted() || getEffected().isInvul()
			|| getSkill().getBuffDuration() == 1 || getEffected() instanceof L2TesterInstance)
	{
		_actor = getEffector();
		_target = getEffected();
	}
	else
	{
		_actor = getEffected();
		_target = getEffector();
		
		if (_actor instanceof L2PcInstance && _actor.getActingPlayer().isSitting())
			_actor.getActingPlayer().standUp();
	}
	
	_actor.setKnockedbackTimer(GameTimeController.getGameTicks() + 18);
	
	_actor.abortAttack();
	
	if (_actor.isMoving())
		_actor.broadcastPacket(new StopMove(_actor));
	else if (_actor.isCastingNow())
		_actor.abortCast();
	
	if (_actor instanceof L2Playable)
		_actor.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	
	int id, x, y, z, tx, ty, tz;
	
	id = _actor.getObjectId();
	x = _actor.getX();
	y = _actor.getY();
	z = _actor.getZ();
	tx = _target.getX();
	ty = _target.getY();
	tz = _target.getZ();
	
	if (x > tx)
		tx++;
	else if (x < tx)
		tx--;
	
	if (y > ty)
		ty++;
	else if (y < ty)
		ty--;
	
	_actor.broadcastPacket(new FlyToLocation(id, x, y, z, tx, ty, tz, FlyType.valueOf(getSkill().getFlyType())));
	_actor.getPosition().setXYZ(tx, ty, tz);
	
	final int hamstring = (int)_actor.calcStat(Stats.HAMSTRING, 0, _target, getSkill());
	
	if (hamstring > 0 && getSkill().getFlyType() != "DUMMY")
	{
		final int distance = (int) Util.calculateDistance(x, y, z, tx, ty);
		final int damage = (int) (hamstring * distance/2.5);
		if (damage > 0)
		{
			_actor.reduceCurrentHp(damage, _target, true, true, null, true);
			_target.sendMessage("You caused an additional "+damage+" damage to "+_actor.getDisplayName()+" due to Hemorrhage");
		}
	}
	
	return true;
}

@Override
public boolean onActionTime()
{
	return false;
}
}