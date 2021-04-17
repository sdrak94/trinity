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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

/**
 * @author hNoke
 *
 */
public class RewardItem
{
public int id;
public int minAmmount;
public int maxAmmount;
public int chance;
public int pvpRequired;
public int levelRequired;

public RewardItem(int id1, int minAmmount1, int maxAmmount1, int chance1, int pvpRequired1, int levelRequired1)
{
	id = id1;
	minAmmount = minAmmount1;
	maxAmmount = maxAmmount1;
	chance = chance1;
	pvpRequired = pvpRequired1;
	levelRequired = levelRequired1;
}

public int getAmmount(L2PcInstance player)
{
	if(Rnd.get(0, 100) < chance && player.getLevel() >= levelRequired && player.getPvpKills() >= pvpRequired)
		return Rnd.get(minAmmount, maxAmmount);
	else
		return 0;
}
}