package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class handles warp effects, disappear and quickly turn up in a near
 * location. If geodata enabled and an object is between initial and final
 * point, flight is stopped just before colliding with object. Flight course and
 * radius are set as skill properties (flyCourse and flyRadius):
 * 
 * <li>Fly Radius means the distance between starting point and final point, it
 * must be an integer.</li> <li>Fly Course means the movement direction: imagine
 * a compass above player's head, making north player's heading. So if fly
 * course is 180, player will go backwards (good for blink, e.g.). By the way,
 * if flyCourse = 360 or 0, player will be moved in in front of him. <br>
 * <br>
 * 
 * If target is effector, put in XML self = "1". This will make _actor =
 * getEffector(). This, combined with target type, allows more complex actions
 * like flying target's backwards or player's backwards.<br>
 * <br>
 * 
 * @author House
 */
public class EffectWarp extends L2Effect
{

private int x, y, z;
private L2Character _actor;

public EffectWarp(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.WARP;
}

@Override
public boolean onStart()
{
	if (isSelfEffect())
		_actor = getEffector();
	else
		_actor = getEffected();
	
	int _radius = getSkill().getFlyRadius();
	
	double angle = Util.convertHeadingToDegree(_actor.getHeading());
	double radian = Math.toRadians(angle);
	double course = Math.toRadians(getSkill().getFlyCourse());
	
	int x1 = (int) (Math.cos(Math.PI + radian + course) * _radius);
	int y1 = (int) (Math.sin(Math.PI + radian + course) * _radius);
	
	x = _actor.getX() + x1;
	y = _actor.getY() + y1;
	z = _actor.getZ();
	
	if (Config.GEODATA > 0)
	{
		Location destiny = GeoData.getInstance().moveCheck(_actor.getX(), _actor.getY(), _actor.getZ(), x, y, z, _actor.getInstanceId());
		x = destiny.getX();
		y = destiny.getY();
		z = destiny.getZ();
	}
	
	_actor.abortAttack();
	if (_actor instanceof L2Playable)
		_actor.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	_actor.abortCast();
	
	final int id, sx, sy, sz;
	
	id = _actor.getObjectId();
	sx = _actor.getX();
	sy = _actor.getY();
	sz = _actor.getZ();
	
	for (L2PcInstance player : _actor.getKnownList().getKnownPlayers().values())
	{
		if (player != null && player != _actor)
		{
			if (player.getTarget() != null && player.getTarget() == _actor)
			{
				if (player.isAutoAttackable(_actor))
				{
					player.setIsSelectingTarget(4);
					player.setTarget(null);
					player.abortAttack();
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					player.abortCast();
				}
			}
		}
	}
	
	_actor.broadcastPacket(new FlyToLocation(id, sx, sy, sz, x, y, z, FlyType.DUMMY));
	_actor.getPosition().setXYZ(x, y, z);
	
	return true;
}

@Override
public boolean onActionTime()
{
	return false;
}
}