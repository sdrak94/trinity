/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.util.ArrayList;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author KenM
 */
public abstract class L2Transformation implements Cloneable, Runnable
{
	private final int			_id;
	private final int			_graphicalId;
	private double				_collisionRadius;
	private double				_collisionHeight;
	private final boolean		_isStance;
	private ArrayList<Integer>	_allowedSkills		= new ArrayList<>();
	
	public static final int		TRANSFORM_ZARICHE	= 301;
	public static final int		TRANSFORM_AKAMANAH	= 302;
	
	private L2PcInstance		_player;
	
	/**
	 * @param id
	 *            Internal id that server will use to associate this transformation
	 * @param graphicalId
	 *            Client visible transformation id
	 * @param collisionRadius
	 *            Collision Radius of the player while transformed
	 * @param collisionHeight
	 *            Collision Height of the player while transformed
	 */
	public L2Transformation(int id, int graphicalId, double collisionRadius, double collisionHeight)
	{
		_id = id;
		_graphicalId = graphicalId;
		_collisionRadius = collisionRadius;
		_collisionHeight = collisionHeight;
		_isStance = false;
	}
	
	/**
	 * @param id
	 *            Internal id(will be used also as client graphical id) that server will use to associate this transformation
	 * @param collisionRadius
	 *            Collision Radius of the player while transformed
	 * @param collisionHeight
	 *            Collision Height of the player while transformed
	 */
	public L2Transformation(int id, double collisionRadius, double collisionHeight)
	{
		this(id, id, collisionRadius, collisionHeight);
	}
	
	/**
	 * @param id
	 *            Internal id(will be used also as client graphical id) that server will use to associate this transformation
	 *            Used for stances
	 */
	public L2Transformation(int id)
	{
		_id = id;
		_graphicalId = id;
		_isStance = true;
		_allowedSkills = new ArrayList<Integer>();
	}
	
	public void addAllowedSkills(int skillId)
	{
		_allowedSkills.add(skillId);
	}
	public ArrayList<Integer> getAllowedSkills()
	{
		return _allowedSkills;
	}
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return Returns the graphicalId.
	 */
	public int getGraphicalId()
	{
		return _graphicalId;
	}
	
	/**
	 * Return true if this is a stance (vanguard/inquisitor)
	 * 
	 * @return
	 */
	public boolean isStance()
	{
		return _isStance;
	}
	
	/**
	 * @return Returns the collisionRadius.
	 */
	public double getCollisionRadius()
	{
		if (isStance())
			return _player.getCollisionRadius();
		return _collisionRadius;
	}
	
	/**
	 * @return Returns the collisionHeight.
	 */
	public double getCollisionHeight()
	{
		if (isStance())
			return _player.getCollisionHeight();
		return _collisionHeight;
	}
	
	// Scriptable Events
	public abstract void onTransform();
	
	public abstract void onUntransform();
	
	/**
	 * @param player
	 *            The player to set.
	 */
	private void setPlayer(L2PcInstance player)
	{
		_player = player;
	}
	
	/**
	 * @return Returns the player.
	 */
	public L2PcInstance getPlayer()
	{
		return _player;
	}
	
	public void start()
	{
		resume();
	}
	
	public void resume()
	{
		getPlayer().transform(this);
	}
	
	public void run()
	{
		stop();
	}
	
	public void stop()
	{
		getPlayer().stopTransformation(null);
	}
	
	public L2Transformation createTransformationForPlayer(L2PcInstance player)
	{
		try
		{
			L2Transformation transformation = (L2Transformation) clone();
			transformation.setPlayer(player);
			return transformation;
		}
		catch (CloneNotSupportedException e)
		{
			// should never happen
			return null;
		}
	}
	
	// Override if necessary
	public void onLevelUp()
	{}
	
	/**
	 * Returns true if transformation can do melee attack
	 */
	public boolean canDoMeleeAttack()
	{
		return true;
	}
	
	/**
	 * Returns true if transformation can start follow target when trying to cast an skill out of range
	 */
	public boolean canStartFollowToCast()
	{
		return true;
	}
}
