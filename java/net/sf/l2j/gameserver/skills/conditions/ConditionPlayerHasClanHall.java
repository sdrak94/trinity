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

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author MrPoke
 *
 */
public final class ConditionPlayerHasClanHall extends Condition
{
	
	private final FastList<Integer> _clanHall;
	
	public ConditionPlayerHasClanHall(FastList<Integer> clanHall)
	{
		_clanHall = clanHall;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.skills.conditions.Condition#testImpl(net.sf.l2j.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player instanceof L2PcInstance))
			return false;
		
		L2Clan clan = ((L2PcInstance)env.player).getClan();
		if (clan == null)
			return (_clanHall.size() == 1 && _clanHall.getFirst() == 0);
		
		// All Clan Hall
		if (_clanHall.size() == 1 && _clanHall.getFirst() == -1)
			return clan.getHasHideout() > 0;
		
		return _clanHall.contains(clan.getHasHideout());
	}
}