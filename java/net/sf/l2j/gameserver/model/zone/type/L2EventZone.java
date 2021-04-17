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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

/**
 * Zone where 'Build Headquarters' is not allowed.
 * 
 * @author Gnat
 */
public class L2EventZone extends L2ZoneType
{
	public L2EventZone(final int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_EVENT, true);
			if (Config.HWID_FARMZONES_CHECK)
			{
				String hwid = ((L2PcInstance) character).getHWID();
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player.getClient().isDetached())
					{
						continue;
					}
					if (!player.isInsideZone(L2Character.ZONE_EVENT))
					{
						continue;
					}
					if (player == character)
					{
						continue;
					}
					if (player.isGM() || character.isGM())
					{
						continue;
					}
					
					String plr_hwid = player.getClient().getStrixClientData().getClientHWID();
					if (plr_hwid.equalsIgnoreCase(hwid))
					{
						character.setIsPendingRevive(true);
						character.teleToLocation(83380, 148107, -3404, true);
						character.setInsideZone(L2Character.ZONE_EVENT, false);
						character.sendMessage("You have another window in the event zone.");
						break;
					}
					else
					{
						if(character.isGM())
						{
							//character.sendMessage("You have entered the event Area");
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_EVENT, false);
			if(character.isGM())
			{
				//character.sendMessage("You have left the Farm Area");
			}
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{}
	
	@Override
	public void onReviveInside(final L2Character character)
	{}
}