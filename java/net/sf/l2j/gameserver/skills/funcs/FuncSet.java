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
package net.sf.l2j.gameserver.skills.funcs;

import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;



public class FuncSet extends Func {
	private final Lambda _lambda;
	public FuncSet(Stats pStat, int pOrder, Object owner, Lambda lambda) {
		super(pStat, pOrder, owner);
		_lambda = lambda;
	}
	@Override
	public void calc(Env env)
	{
		if (cond == null || cond.test(env))
			env.value = _lambda.calc(env);
	}
}
