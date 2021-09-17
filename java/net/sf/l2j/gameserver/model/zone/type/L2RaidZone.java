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

import ghosts.model.Ghost;
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
public class L2RaidZone extends L2ZoneType
{
	public L2RaidZone(final int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character.getInstanceId() == 0)
		{
			if (character instanceof L2PcInstance)
			{
				character.setInsideZone(L2Character.ZONE_RAID, true);
				if (character.isGM())
				{
					character.sendMessage("You have entered the Raid Area");
				}
				if (character.getKnownList().getKnownRaidsInRadius(7000).isEmpty() && character.getActingPlayer().getInstanceId() == 0)
				{
					if (character.isGM())
					{
						character.sendMessage("started onEnter startkick");
					}
					character.getActingPlayer().startKickFromRaidTask();
				}
				if (Config.HWID_FARMZONES_CHECK)
				{
					String hwid = ((L2PcInstance) character).getHWID();
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{

						if (!(player instanceof Ghost) && player.getClient().isDetached())
						{
							continue;
						}
						if (!(player.isInsideZone(L2Character.ZONE_FARM) || player.isInsideZone(L2Character.ZONE_RAID)))
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
						String plr_hwid = player.getClient().getFullHwid();
						if (plr_hwid.equalsIgnoreCase(hwid))
						{
							player.stopKickFromRaidTask();
							character.setIsPendingRevive(true);
							character.teleToLocation(83380, 148107, -3404, true);
							character.setInsideZone(L2Character.ZONE_RAID, false);
							// character.setInsideZone(L2Character.ZONE_CHAOTIC, false);
							character.sendMessage("You have another window in a hwid restricted zone.");
							break;
						}
						else
						{
							if (character.isGM())
							{
								character.sendMessage("You have entered the Raid Area");
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character.getInstanceId() == 0)
		{
			if (character instanceof L2PcInstance)
			{
				if (character.getActingPlayer().getInstanceId() == 0)
				{
					character.getActingPlayer().stopKickFromRaidTask();
				}
				character.setInsideZone(L2Character.ZONE_RAID, false);
				// character.setInsideZone(L2Character.ZONE_PVP, false);
				// character.sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				if (character.isGM())
				{
					character.sendMessage("You have left the Raid Area");
				}
			}
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
		if (character.getInstanceId() == 0)
		{
			if (character instanceof L2PcInstance)
			{
				if (character.getActingPlayer().getInstanceId() == 0)
				{
					character.getActingPlayer().stopKickFromRaidTask();
					if (character.isGM())
					{
						character.sendMessage("stopped onDieInside startkick");
					}
					if (character.getKnownList().getKnownRaidsInRadius(7000).isEmpty())
					{
						if (character.isGM())
						{
							character.sendMessage("started onDieInside startkick");
						}
						character.getActingPlayer().startKickFromRaidTask();
					}
				}
			}
		}
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
		if (character.getInstanceId() == 0)
		{
			if (character instanceof L2PcInstance)
			{
				if (character.getActingPlayer().getInstanceId() == 0)
				{
					character.getActingPlayer().stopKickFromRaidTask();
					if (character.isGM())
					{
						character.sendMessage("stopped onReviveInside startkick");
					}
					if (character.getKnownList().getKnownRaidsInRadius(7000).isEmpty())
					{
						if (character.isGM())
						{
							character.sendMessage("started onReviveInside startkick");
						}
						character.getActingPlayer().startKickFromRaidTask();
					}
				}
			}
		}
	}
}
