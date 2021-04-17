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
package net.sf.l2j.gameserver.model;

/**
 * This class ...
 *
 * @version $Revision: 0.0.0.1 $ $Date: 2008/03/19 15:10:30 $
 */
public final class L2TransformSkillLearn
{
	// these two build the primary key
	private final int _race_id;
	private final int _skill_id;
	private final int _item_id;
	private final int _level;

	// not needed, just for easier debug
	private final String _name;

	private final int _sp;
	private final int _min_level;

	public L2TransformSkillLearn(int race_id, int skill_id, int item_id, int level, String name, int sp, int min_level)
	{
		_race_id = race_id;
		_skill_id = skill_id;
		_item_id = item_id;
		_level = level;
		_name = name.intern();
		_sp = sp;
		_min_level = min_level;
	}

	/**
	 * @return Returns the skill_id.
	 */
	public int getId()
	{
		return _skill_id;
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns the minLevel.
	 */
	public int getMinLevel()
	{
		return _min_level;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the spCost.
	 */
	public int getSpCost()
	{
		return _sp;
	}
	public int getRace()
	{
		return _race_id;
	}
	public int getItemId()
	{
		return _item_id;
	}
}
