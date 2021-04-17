/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.effects;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class EffectThrowUp extends L2Effect
{
static final Logger _log = Logger.getLogger(EffectThrowUp.class.getName());

private int _x, _y, _z;

public EffectThrowUp(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	return L2EffectType.THROW_UP;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	// Get current position of the L2Character
	final int curX = getEffected().getX();
	final int curY = getEffected().getY();
	final int curZ = getEffected().getZ();
	
	// Calculate distance between effector and effected current position
	double dx = getEffector().getX() - curX;
	double dy = getEffector().getY() - curY;
	double dz = getEffector().getZ() - curZ;
	double distance = Math.sqrt(dx * dx + dy * dy);
	if (distance > 2000)
	{
		_log.info("EffectThrow was going to use invalid coordinates for characters, getEffected: "+curX+","+curY+" and getEffector: "+getEffector().getX()+","+getEffector().getY());
		return false;
	}
	int offset = Math.min((int) distance + getSkill().getFlyRadius(), 1400);
	
	double cos;
	double sin;
	
	// approximation for moving futher when z coordinates are different
	// TODO: handle Z axis movement better
	offset += Math.abs(dz);
	if (offset < 5)
		offset = 5;
	
	// If no distance
	if (distance < 1)
		return false;
	
	// Calculate movement angles needed
	sin = dy / distance;
	cos = dx / distance;
	
	// Calculate the new destination with offset included
	_x = getEffector().getX() - (int) (offset * cos);
	_y = getEffector().getY() - (int) (offset * sin);
	_z = getEffected().getZ();
	
	if (Config.GEODATA > 0)
	{
		Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), _x, _y, _z, getEffected().getInstanceId());
		_x = destiny.getX();
		_y = destiny.getY();
	}
	getEffected().startStunning();
	getEffected().broadcastPacket(new FlyToLocation(getEffected().getObjectId(), getEffected().getX(), getEffected().getY(), getEffected().getZ(), _x, _y, _z, FlyType.THROW_UP));
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	return false;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	getEffected().stopStunning(this);
	getEffected().setXYZ(_x, _y, _z);
}
}
