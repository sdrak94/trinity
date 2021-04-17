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

import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.model.actor.FakePc;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.skills.Env;


/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConditionPlayerRace extends Condition {

public final Race _race;

public ConditionPlayerRace(Race race)
{
	_race = race;
}

@Override
public boolean testImpl(Env env)
{
	if (env.player.isAPC())
	{
		final FakePc fpc = FakePcsTable.getInstance().getFakePc(((L2Npc)env.player).getNpcId());
		
		if (fpc != null)
		{
			int race = fpc.race;
			
			if (race == 6)
				race = 0;
			else if (race == 7)
				race = 3;
			
			return race == _race.ordinal();
		}
	}
	
	if (!(env.player instanceof L2PcInstance))
		return true;
	
	return env.player.isGM() || ((L2PcInstance)env.player).getRace().getRealOrdinal() == _race.ordinal();
}
}
