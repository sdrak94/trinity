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
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

/**
 * Zone where 'Build Headquarters' is not allowed.
 * 
 * @author Gnat
 */
public class L2PrimevalZone extends L2ZoneType
{
	public L2PrimevalZone(final int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (character.getInstanceId() == 0)
			{
				character.setInsideZone(L2Character.ZONE_PRIMEVAL, true);
				L2PcInstance player = character.getActingPlayer();

				player.sendMessage("You have entered Primeval Isle");
				if (!player.isInPI())
				{
					player.setIsInPI(true);
					if (!player.isGM())
					{
						player.updatePvPFlag(1);
						player.sendMessage("PvP Flag status updated");
						try
						{
							if (player.isCursedWeaponEquipped())
								CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
						}
						catch (Exception e)
						{}
						//player.setDisguised(true);
						player.broadcastUserInfo();
						if (Config.HWID_FARMZONES_CHECK)
						{
							String hwid = ((L2PcInstance) character).getHWID();
							for (L2PcInstance player1 : L2World.getInstance().getAllPlayers().values())
							{

								if (!(player1 instanceof Ghost) && player1.getClient().isDetached())
								{
									continue;
								}
								if (!(player1.isInsideZone(L2Character.ZONE_FARM) || player1.isInsideZone(L2Character.ZONE_RAID) || player1.isInsideZone(L2Character.ZONE_CHAOTIC) || player1.isInsideZone(L2Character.ZONE_EVENT) || player1.isInsideZone(L2Character.ZONE_PRIMEVAL) || player1.isInPI() || player1.isInHuntersVillage()))
								{
									continue;
								}
								if (player1 == character)
								{
									continue;
								}
								if (player1.isGM() || character.isGM())
								{
									continue;
								}
								String plr_hwid = player1.getHWID();
								if (plr_hwid.equalsIgnoreCase(hwid))
								{
									character.setIsPendingRevive(true);
									character.getActingPlayer().setIsInPI(false);
									character.getActingPlayer().stopPvPFlag();
									character.getActingPlayer().broadcastUserInfo();
									character.getActingPlayer().sendMessage("You have left Primeval Isle.");
									character.sendMessage("You have another window in a hwid restricted zone.");
									character.teleToLocation(83380, 148107, -3404, true);
									break;
								}
								else
								{
									if (character.isGM())
									{
										character.sendMessage("You have entered the Primeval Isle PvP Area");
									}
								}
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
		if (character instanceof L2PcInstance && character.getActingPlayer().isInPI())
		{
			character.getActingPlayer().broadcastUserInfo();
			character.getActingPlayer().setInPvPCustomEventZone(false);
			character.getActingPlayer().setIsInPI(false);
			character.getActingPlayer().stopPvPFlag();
			character.getActingPlayer().sendMessage("You have left Primeval Isle");
		}
		character.setInsideZone(L2Character.ZONE_PRIMEVAL, false);
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{}
	
	@Override
	public void onReviveInside(final L2Character character)
	{}
}
