/* This program is free software: you can redistribute it and/or modify it under
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

package net.sf.l2j.gameserver;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Fort;

/**
 *
 * Vice - 2008
 * Class managing periodical events with castle
 *
 */
public class FortUpdater implements Runnable
{
	protected static Logger _log = Logger.getLogger(FortUpdater.class.getName());
	private L2Clan _clan;
	private Fort _fort;
	@SuppressWarnings("unused")
	private int _runCount;
	
	public FortUpdater(Fort fort, L2Clan clan, int runCount)
	{
		_fort = fort;
		_clan = clan;
		_runCount = runCount;
	}
	
	public void run()
	{
		try
		{
			_runCount++;
			if (_fort.getOwnerClan() == null || _fort.getOwnerClan() != _clan)
				return;
			
			_fort.setBloodOathReward(_fort.getBloodOathReward() + Config.FS_BLOOD_OATH_COUNT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}