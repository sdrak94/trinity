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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.util.Rnd;

/**
 * @author Advi
 *
 */
public class ConditionGameChance extends Condition
{
private final int _chance;

public ConditionGameChance(int chance)
{
	_chance = chance;
}

/**
 * 
 * @see net.sf.l2j.gameserver.skills.conditions.Condition#testImpl(net.sf.l2j.gameserver.skills.Env)
 */
@Override
public boolean testImpl(Env env)
{
	if (env.item != null && env.item.getName().contains("Icarus"))
		{
			return Rnd.get(100) < _chance + (env.item.getEnchantLevel() >= 16 ? _chance*0.25 : env.item.getEnchantLevel() >= 14 ? _chance*0.15 : 0);
			 
		}

	if (env.item != null && env.item.getUniqueness() == 4.5 && env.item.getName().contains("Dread"))
	{
		return Rnd.get(100) < _chance + _chance * env.item.getEnchantLevel() * 0.06;
	}
	
	return Rnd.get(100) < _chance;
}

public final int getChance()
{
	return _chance;
}
}
